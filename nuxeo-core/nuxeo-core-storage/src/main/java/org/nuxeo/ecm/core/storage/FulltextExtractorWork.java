/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 *     Stephane Lacoin
 */
package org.nuxeo.ecm.core.storage;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.impl.DocumentLocationImpl;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.api.repository.FulltextParser;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.storage.FulltextUpdaterWork.IndexAndText;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Work task that does fulltext extraction from the blobs of the given document.
 * <p>
 * The extracted fulltext is then passed to the single-threaded {@link FulltextUpdaterWork}.
 *
 * @since 5.7
 */
public class FulltextExtractorWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(FulltextExtractorWork.class);

    protected static final String ANY2TEXT = "any2text";

    protected static final String CATEGORY = "fulltextExtractor";

    protected static final String TITLE = "fulltextExtractor";

    protected final boolean excludeProxies;

    protected transient FulltextConfiguration fulltextConfiguration;

    protected transient FulltextParser fulltextParser;

    public FulltextExtractorWork(String repositoryName, String docId, boolean excludeProxies) {
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
    public int getRetryCount() {
        // even read-only threads may encounter concurrent update exceptions
        // when trying to read a previously deleted complex property
        // due to read committed semantics, cf NXP-17384
        return 1;
    }

    @Override
    public void work() {
        openSystemSession();
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
    public void initFulltextConfigurationAndParser() {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        Repository repository = repositoryService.getRepository(repositoryName);
        fulltextConfiguration = repository.getFulltextConfiguration();
        Class<? extends FulltextParser> fulltextParserClass = fulltextConfiguration.fulltextParserClass;
        fulltextParser = new DefaultFulltextParser();
        if (fulltextParserClass != null) {
            try {
                fulltextParser = fulltextConfiguration.fulltextParserClass.getConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                log.error("Failed to instantiate " + fulltextConfiguration.fulltextParserClass.getCanonicalName(), e);
            }
        }
    }

    protected void extractBinaryText() {
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
        DocumentLocation docLocation = new DocumentLocationImpl(doc);
        List<IndexAndText> indexesAndText = new LinkedList<>();
        for (String indexName : fulltextConfiguration.indexNames) {
            if (!fulltextConfiguration.indexesAllBinary.contains(indexName)
                    && fulltextConfiguration.propPathsByIndexBinary.get(indexName) == null) {
                // nothing to do: index not configured for blob
                continue;
            }
            extractor.setExtractorProperties(fulltextConfiguration.propPathsByIndexBinary.get(indexName),
                    fulltextConfiguration.propPathsExcludedByIndexBinary.get(indexName),
                    fulltextConfiguration.indexesAllBinary.contains(indexName));
            List<Blob> blobs = extractor.getBlobs(doc);
            StringBlob stringBlob = blobsToStringBlob(blobs, docId);
            String text = fulltextParser.parse(stringBlob.getString(), null, stringBlob.getMimeType(), docLocation);
            int fullTextFieldSizeLimit = fulltextConfiguration.fulltextFieldSizeLimit;
            if (fullTextFieldSizeLimit != 0 && text.length() > fullTextFieldSizeLimit) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Fulltext extract of length: %s for indexName: %s of document: %s truncated to length: %s",
                            text.length(), indexName, docId, fullTextFieldSizeLimit));
                }
                text = text.substring(0, fullTextFieldSizeLimit);
            }
            indexesAndText.add(new IndexAndText(indexName, text));
        }
        if (!indexesAndText.isEmpty()) {
            Work work = new FulltextUpdaterWork(repositoryName, docId, false, true, indexesAndText);
            if (!fulltextConfiguration.fulltextSearchDisabled) {
                WorkManager workManager = Framework.getService(WorkManager.class);
                workManager.schedule(work, true);
            } else {
                ((FulltextUpdaterWork) work).updateWithSession(session);
            }
        }

    }

    @Override
    public void cleanUp(boolean ok, Exception e) {
        super.cleanUp(ok, e);
        fulltextConfiguration = null;
        fulltextParser = null;
    }

    protected StringBlob blobsToStringBlob(List<Blob> blobs, String docId) {
        String mimeType = null;
        List<String> strings = new LinkedList<>();
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
                if (StringUtils.isEmpty(mimeType) && StringUtils.isNotEmpty(blob.getMimeType())) {
                    mimeType = blob.getMimeType();
                }
                String string = new String(blob.getByteArray(), "UTF-8");
                // strip '\0 chars from text
                if (string.indexOf('\0') >= 0) {
                    string = string.replace("\0", " ");
                }
                strings.add(string);
            } catch (ConversionException | IOException e) {
                String msg = "Could not extract fulltext of file '" + blob.getFilename() + "' for document: " + docId
                        + ": " + e;
                log.warn(msg);
                log.debug(msg, e);
                continue;
            }
        }
        return new StringBlob(StringUtils.join(strings, " "), mimeType);
    }

    protected BlobHolder convert(BlobHolder blobHolder) throws ConversionException {
        ConversionService conversionService = Framework.getService(ConversionService.class);
        if (conversionService == null) {
            log.debug("No ConversionService available");
            return null;
        }
        return conversionService.convert(ANY2TEXT, blobHolder, null);
    }

}
