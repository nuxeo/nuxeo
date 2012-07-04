/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.PostCommitEventListener;
import org.nuxeo.ecm.core.event.ReconnectedEventBundle;
import org.nuxeo.ecm.core.storage.sql.FulltextParser;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.ModelFulltext;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.runtime.api.Framework;

/**
 * Listener that does fulltext extraction from the blobs of documents whose ids
 * have been recorded in the bundle's events.
 *
 * @author Florent Guillaume
 * @author Stephane Lacoin
 */
public class BinaryTextListener implements PostCommitEventListener {

    private static final Log log = LogFactory.getLog(BinaryTextListener.class);

    public static final String EVENT_NAME = "event_storage_binaries_doc";

    private static final String ANY2TEXT = "any2text";

    protected final ConversionService conversionService;

    public BinaryTextListener() throws ClientException {
        try {
            conversionService = Framework.getService(ConversionService.class);
        } catch (Exception e) {
            throw new ClientException(e);
        }
        if (conversionService == null) {
            throw new ClientException("No conversion service");
        }
    }

    @Override
    public void handleEvent(EventBundle eventBundle) throws ClientException {
        if (! eventBundle.containsEventName(EVENT_NAME)) {
            return;
        }
        if (!(eventBundle instanceof ReconnectedEventBundle)) {
            log.error("Incorrect event bundle type: " + eventBundle);
            return;
        }
        CoreSession session = null;
        ModelFulltext fulltextInfo = null;
        FulltextParser fulltextParser = null;
        Set<Serializable> ids = new HashSet<Serializable>();
        for (Event event : eventBundle) {
            if (!event.getName().equals(EVENT_NAME)) {
                continue;
            }
            EventContext eventContext = event.getContext();
            fulltextInfo = getFulltextInfoFromEventContext(eventContext);
            ids.addAll(getIdsFromEventContext(eventContext));
            fulltextParser = getFulltextParserFromEventContext(eventContext);
            CoreSession s = eventContext.getCoreSession();
            if (session == null) {
                session = s;
            } else if (session != s) {
                // cannot happen given current ReconnectedEventBundleImpl
                throw new ClientException(
                        "Several CoreSessions in one EventBundle");
            }
        }
        if (session == null) {
            if (ids.isEmpty()) {
                return;
            }
            throw new ClientException("Null CoreSession");
        }

        // we have all the info from the bundle, now do the extraction
        boolean save = false;
        BlobsExtractor extractor = new BlobsExtractor();
        for (Serializable id : ids) {
            IdRef docRef = new IdRef((String) id);
            // if the runtime has shutdown (normally because tests are finished)
            // this can happen, see NXP-4009
            if (session.getPrincipal() == null) {
                continue;
            }
            if (!session.exists(docRef)) {
                // doc is gone
                continue;
            }
            DocumentModel indexedDoc = session.getDocument(docRef);

            if (indexedDoc.isProxy()) {
                // proxies don't have any fulltext attached, it's
                // the target document that carries it
                continue;
            }

            if (Boolean.FALSE.equals(fulltextInfo.isFulltextIndexable(indexedDoc.getType()))) {
                continue;
            }

            // Iterate on each index to set the binaryText column
            for (String indexName : fulltextInfo.indexNames) {
                if (!fulltextInfo.indexesAllBinary.contains(indexName)
                        && fulltextInfo.propPathsByIndexBinary.get(indexName) == null) {
                    // nothing to do: index not configured for blob
                    continue;
                }
                extractor.setExtractorProperties(
                        fulltextInfo.propPathsByIndexBinary.get(indexName),
                        fulltextInfo.propPathsExcludedByIndexBinary.get(indexName),
                        fulltextInfo.indexesAllBinary.contains(indexName));
                List<Blob> blobs = extractor.getBlobs(indexedDoc);
                String text = blobsToText(blobs, (String) id);
                fulltextParser.setStrings(new ArrayList<String>());
                fulltextParser.parse(text, null);
                text = StringUtils.join(fulltextParser.getStrings(), " ");
                String impactedQuery =
                    String.format("SELECT * from Document where ecm:fulltextJobId = '%s'",
                            indexedDoc.getId());
                DocumentModelList impactedDocs = session.query(impactedQuery);
                for (DocumentModel impactedDoc : impactedDocs) {
                    try {
                        DocumentRef ref = impactedDoc.getRef();
                        session.setDocumentSystemProp(ref,
                                SQLDocument.FULLTEXT_JOBID_SYS_PROP,
                                null);
                        session.setDocumentSystemProp(ref,
                                SQLDocument.BINARY_TEXT_SYS_PROP + getFulltextIndexSuffix(indexName),
                                text);
                    } catch (DocumentException e) {
                        log.error("Couldn't set fulltext on: " + id, e);
                        continue;
                    }
                }
            }

            save = true;
        }
        if (save) {
            session.save();
        }
    }

    @SuppressWarnings("unchecked")
    protected Set<Serializable> getIdsFromEventContext(EventContext eventContext) {
        return (Set<Serializable>) eventContext.getArguments()[0];
    }

    protected ModelFulltext getFulltextInfoFromEventContext(
            EventContext eventContext) {
        return (ModelFulltext) eventContext.getArguments()[1];
    }

    protected FulltextParser getFulltextParserFromEventContext(
            EventContext eventContext) {
        // test length for compat
        if (eventContext.getArguments().length > 2) {
            @SuppressWarnings("unchecked")
            Class<? extends FulltextParser> fulltextParserClass = (Class<? extends FulltextParser>) eventContext.getArguments()[2];
            try {
                return fulltextParserClass.newInstance();
            } catch (InstantiationException e) {
                log.error("Failed to instanciate " + fulltextParserClass.getCanonicalName(), e);
            } catch (IllegalAccessException e) {
                log.error(e);
            }
        }
        return new FulltextParser();
    }

    protected String blobsToText(List<Blob> blobs, String docId) {
        List<String> strings = new LinkedList<String>();
        for (Blob blob : blobs) {
            try {
                SimpleBlobHolder bh = new SimpleBlobHolder(blob);
                BlobHolder result = conversionService.convert(ANY2TEXT, bh,
                        null);
                if (result == null) {
                    continue;
                }
                blob = result.getBlob();
                if (blob == null) {
                    continue;
                }
                String string = new String(blob.getByteArray(), "UTF-8");
                // strip '\0 chars from text
                if (string.indexOf('\0') >= 0) {
                    string = string.replace("\0", " ");
                }
                strings.add(string);
            } catch (Exception e) {
                String msg = "Could not extract fulltext of file '"
                        + blob.getFilename() + "' for document: " + docId;
                log.warn(msg);
                log.debug(msg, e);
                continue;
            }
        }
        return StringUtils.join(strings, " ");
    }

    public String getFulltextIndexSuffix(String indexName) {
        return indexName.equals(Model.FULLTEXT_DEFAULT_INDEX) ? "" : '_' + indexName;
    }

}
