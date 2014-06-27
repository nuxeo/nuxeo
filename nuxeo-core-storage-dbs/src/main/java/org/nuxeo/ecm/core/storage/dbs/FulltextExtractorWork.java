/*
 * Copyright (c) 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.dbs;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.storage.dbs.FulltextUpdaterWork.IndexAndText;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Work task that does fulltext extraction from the blobs of the given document.
 * <p>
 * The extracted fulltext is then passed to the single-threaded
 * {@link FulltextUpdaterWork}.
 *
 * @since 5.9.5
 */
public class FulltextExtractorWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(FulltextExtractorWork.class);

    private static final String ANY2TEXT = "any2text";

    protected static final String CATEGORY = "fulltextExtractor";

    protected static final String TITLE = "fulltextExtractor";

    protected transient FulltextConfiguration fulltextConfig;

    protected transient FulltextParser fulltextParser;

    public FulltextExtractorWork(String repositoryName, String docId) {
        super(repositoryName + ':' + docId + ":fulltextExtractor");
        setDocument(repositoryName, docId);
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    public void work() throws Exception {
        initSession();
        // if the runtime has shutdown (normally because tests are finished)
        // this can happen, see NXP-4009
        if (session.getPrincipal() == null) {
            return;
        }

        fulltextConfig = getFulltextConfig();
        initFulltextParser();

        setStatus("Extracting");
        setProgress(Progress.PROGRESS_0_PC);
        extractBinaryText();
        setProgress(Progress.PROGRESS_100_PC);
        setStatus("Done");
    }

    protected void extractBinaryText() throws ClientException {
        IdRef docRef = new IdRef(docId);
        if (!session.exists(docRef)) {
            // doc is gone
            return;
        }
        DocumentModel doc = session.getDocument(docRef);
        if (!fulltextConfig.isFulltextIndexable(doc.getType())) {
            // excluded by config
            return;
        }

        // Iterate on each index to set the binaryText column
        BlobsExtractor extractor = new BlobsExtractor();
        List<IndexAndText> indexesAndText = new LinkedList<IndexAndText>();
        for (String indexName : fulltextConfig.indexNames) {
            if (!fulltextConfig.indexesAllBinary.contains(indexName)
                    && fulltextConfig.propPathsByIndexBinary.get(indexName) == null) {
                // nothing to do: index not configured for blob
                continue;
            }
            extractor.setExtractorProperties(
                    fulltextConfig.propPathsByIndexBinary.get(indexName),
                    fulltextConfig.propPathsExcludedByIndexBinary.get(indexName),
                    fulltextConfig.indexesAllBinary.contains(indexName));
            List<Blob> blobs = extractor.getBlobs(doc);
            String text = blobsToText(blobs, docId);
            fulltextParser.setStrings(new ArrayList<String>());
            fulltextParser.parse(text, null);
            text = StringUtils.join(fulltextParser.getStrings(), " ");
            indexesAndText.add(new IndexAndText(indexName, text));
        }
        if (!indexesAndText.isEmpty()) {
            Work work = new FulltextUpdaterWork(repositoryName, docId, false,
                    true, indexesAndText);
            WorkManager workManager = Framework.getLocalService(WorkManager.class);
            workManager.schedule(work, true);
        }
    }

    @Override
    public void cleanUp(boolean ok, Exception e) {
        super.cleanUp(ok, e);
        fulltextConfig = null;
        fulltextParser = null;
    }

    protected FulltextConfiguration getFulltextConfig() {
        // TODO get from extension point
        // XXX hardcoded config for now
        FulltextConfiguration config = new FulltextConfiguration();

        return config;
    }

    protected void initFulltextParser() {
        fulltextParser = new FulltextParser();
        // TODO make fulltext parser class configurable (like in VCS)
    }

    protected String blobsToText(List<Blob> blobs, String docId) {
        List<String> strings = new LinkedList<String>();
        for (Blob blob : blobs) {
            try {
                SimpleBlobHolder bh = new SimpleBlobHolder(blob);
                BlobHolder result = convert(bh);
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
                        + blob.getFilename() + "' for document: " + docId
                        + ": " + e;
                log.warn(msg);
                log.debug(msg, e);
                continue;
            }
        }
        return StringUtils.join(strings, " ");
    }

    protected BlobHolder convert(BlobHolder blobHolder)
            throws ConversionException {
        ConversionService conversionService = Framework.getLocalService(ConversionService.class);
        if (conversionService == null) {
            log.debug("No ConversionService available");
            return null;
        }
        return conversionService.convert(ANY2TEXT, blobHolder, null);
    }

}
