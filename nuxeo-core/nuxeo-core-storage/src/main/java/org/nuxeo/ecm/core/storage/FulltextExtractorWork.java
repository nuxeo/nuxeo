/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage;

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
import org.nuxeo.ecm.core.storage.FulltextUpdaterWork.IndexAndText;
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
 * <p>
 * This base abstract class must be subclassed in order to implement the proper
 * {@link #initFulltextConfigurationAndParser} depending on the storage.
 *
 * @since 5.7
 */
public abstract class FulltextExtractorWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(FulltextExtractorWork.class);

    protected static final String ANY2TEXT = "any2text";

    protected static final String CATEGORY = "fulltextExtractor";

    protected static final String TITLE = "fulltextExtractor";

    protected final boolean excludeProxies;

    protected transient FulltextConfiguration fulltextConfiguration;

    protected transient FulltextParser fulltextParser;

    public FulltextExtractorWork(String repositoryName, String docId,
            String id, boolean excludeProxies) {
        super(id);
        setDocument(repositoryName, docId);
        this.excludeProxies = excludeProxies;
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

        initFulltextConfigurationAndParser();

        setStatus("Extracting");
        setProgress(Progress.PROGRESS_0_PC);
        extractBinaryText();
        setProgress(Progress.PROGRESS_100_PC);
        setStatus("Done");
    }

    /**
     * Initializes the fulltext configuration and parser.
     *
     * @since 5.9.5
     */
    public abstract void initFulltextConfigurationAndParser();

    protected void extractBinaryText() throws ClientException {
        IdRef docRef = new IdRef(docId);
        if (!session.exists(docRef)) {
            // doc is gone
            return;
        }
        DocumentModel doc = session.getDocument(docRef);
        if (excludeProxies && doc.isProxy()) {
            // VCS proxies don't have any fulltext attached, it's
            // the target document that carries it
            return;
        }
        if (!fulltextConfiguration.isFulltextIndexable(doc.getType())) {
            // excluded by config
            return;
        }

        // Iterate on each index to set the binaryText column
        BlobsExtractor extractor = new BlobsExtractor();
        List<IndexAndText> indexesAndText = new LinkedList<IndexAndText>();
        for (String indexName : fulltextConfiguration.indexNames) {
            if (!fulltextConfiguration.indexesAllBinary.contains(indexName)
                    && fulltextConfiguration.propPathsByIndexBinary.get(indexName) == null) {
                // nothing to do: index not configured for blob
                continue;
            }
            extractor.setExtractorProperties(
                    fulltextConfiguration.propPathsByIndexBinary.get(indexName),
                    fulltextConfiguration.propPathsExcludedByIndexBinary.get(indexName),
                    fulltextConfiguration.indexesAllBinary.contains(indexName));
            List<Blob> blobs = extractor.getBlobs(doc);
            String text = blobsToText(blobs, docId);
            text = fulltextParser.parse(text, null);
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
        fulltextConfiguration = null;
        fulltextParser = null;
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
