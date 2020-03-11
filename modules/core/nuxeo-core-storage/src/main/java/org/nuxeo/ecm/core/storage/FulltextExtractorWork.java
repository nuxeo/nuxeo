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
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.text.StringEscapeUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.convert.api.ConversionException;
import org.nuxeo.ecm.core.convert.api.ConversionService;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.repository.RepositoryService;
import org.nuxeo.ecm.core.utils.BlobsExtractor;
import org.nuxeo.ecm.core.utils.StringsExtractor;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;

import net.htmlparser.jericho.Source;

/**
 * Work task that does fulltext extraction from the string properties and the blobs of the given document, saving them
 * into the fulltext table.
 *
 * @since 5.7 for the original implementation
 * @since 10.3 the extraction and update are done in the same Work
 */
public class FulltextExtractorWork extends AbstractWork {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(FulltextExtractorWork.class);

    public static final String SYSPROP_FULLTEXT_SIMPLE = "fulltextSimple";

    public static final String SYSPROP_FULLTEXT_BINARY = "fulltextBinary";

    public static final String SYSPROP_FULLTEXT_JOBID = "fulltextJobId";

    public static final String FULLTEXT_DEFAULT_INDEX = "default";

    protected static final String CATEGORY = "fulltextExtractor";

    protected static final String TITLE = "Fulltext Extractor";

    protected static final String ANY2TEXT_CONVERTER = "any2text";

    protected static final int HTML_MAGIC_OFFSET = 8192;

    protected static final String TEXT_HTML = "text/html";

    protected transient FulltextConfiguration fulltextConfiguration;

    protected transient DocumentModel document;

    protected transient List<DocumentRef> docsToUpdate;

    /** If true, update the simple text from the document. */
    protected final boolean updateSimpleText;

    /** If true, update the binary text from the document. */
    protected final boolean updateBinaryText;

    protected final boolean useJobId;

    public FulltextExtractorWork(String repositoryName, String docId, boolean updateSimpleText,
            boolean updateBinaryText, boolean useJobId) {
        super(); // random id, for unique job
        setDocument(repositoryName, docId);
        this.updateSimpleText = updateSimpleText;
        this.updateBinaryText = updateBinaryText;
        this.useJobId = useJobId;
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
        return 1;
    }

    @Override
    public void work() {
        openSystemSession();
        // if the runtime has shut down (normally because tests are finished)
        // this can happen, see NXP-4009
        if (session.getPrincipal() == null) {
            return;
        }
        DocumentRef docRef = new IdRef(docId);
        if (!session.exists(docRef)) {
            return;
        }
        document = session.getDocument(docRef);
        findDocsToUpdate();
        if (docsToUpdate.isEmpty()) {
            return;
        }
        initFulltextConfiguration();

        setStatus("Extracting");
        setProgress(Progress.PROGRESS_0_PC);
        extractAndUpdate();
        setStatus("Saving");
        session.save();
        setProgress(Progress.PROGRESS_100_PC);
        setStatus("Done");
    }

    protected void initFulltextConfiguration() {
        RepositoryService repositoryService = Framework.getService(RepositoryService.class);
        Repository repository = repositoryService.getRepository(repositoryName);
        fulltextConfiguration = repository.getFulltextConfiguration();
    }

    protected void findDocsToUpdate() {
        if (useJobId) {
            // find which docs will receive the extracted text (there may be more than one if the original
            // doc was copied between the time it was saved and this listener being asynchronously executed)
            String query = String.format(
                    "SELECT ecm:uuid FROM Document WHERE ecm:fulltextJobId = '%s' AND ecm:isProxy = 0", docId);
            docsToUpdate = new ArrayList<>();
            try (IterableQueryResult it = session.queryAndFetch(query, NXQL.NXQL)) {
                for (Map<String, Serializable> map : it) {
                    docsToUpdate.add(new IdRef((String) map.get(NXQL.ECM_UUID)));
                }
            }
        } else {
            docsToUpdate = Collections.singletonList(document.getRef());
        }
    }

    protected void extractAndUpdate() {
        // update all docs
        if (updateSimpleText) {
            extractAndUpdateSimpleText();
        }
        if (updateBinaryText) {
            extractAndUpdateBinaryText();
        }
        // reset job id
        for (DocumentRef docRef : docsToUpdate) {
            session.setDocumentSystemProp(docRef, SYSPROP_FULLTEXT_JOBID, null);
        }
    }

    protected void extractAndUpdateSimpleText() {
        if (fulltextConfiguration.fulltextSearchDisabled) {
            // if fulltext search is disabled, we don't extract simple text at all
            return;
        }
        for (String indexName : fulltextConfiguration.indexNames) {
            if (!fulltextConfiguration.indexesAllSimple.contains(indexName)
                    && fulltextConfiguration.propPathsByIndexSimple.get(indexName) == null) {
                // nothing to do: index not configured for simple text
                continue;
            }
            Set<String> includedPaths = fulltextConfiguration.indexesAllSimple.contains(indexName) ? null
                    : fulltextConfiguration.propPathsByIndexSimple.get(indexName);
            Set<String> excludedPaths = fulltextConfiguration.propPathsExcludedByIndexSimple.get(indexName);
            // get string properties
            List<String> strings = new StringsExtractor().findStrings(document, includedPaths, excludedPaths);
            // transform to text (remove HTML and entities)
            // we do this here rather than in the indexing backend (Elasticsearch) because it's more efficient here
            // add space at beginning and end for simulated phrase search using LIKE "% foo bar %"
            String text = strings.stream().map(this::stringToText).collect(Collectors.joining(" ", " ", " "));
            // limit size
            text = limitStringSize(text, fulltextConfiguration.fulltextFieldSizeLimit);
            String property = getFulltextPropertyName(SYSPROP_FULLTEXT_SIMPLE, indexName);
            for (DocumentRef docRef : docsToUpdate) {
                session.setDocumentSystemProp(docRef, property, text);
            }
        }
    }

    protected void extractAndUpdateBinaryText() {
        // we extract binary text even if fulltext search is disabled,
        // because it is still used to inject into external indexers like Elasticsearch
        BlobsExtractor blobsExtractor = new BlobsExtractor();
        Map<Blob, String> blobsText = new IdentityHashMap<>();
        for (String indexName : fulltextConfiguration.indexNames) {
            if (!fulltextConfiguration.indexesAllBinary.contains(indexName)
                    && fulltextConfiguration.propPathsByIndexBinary.get(indexName) == null) {
                // nothing to do: index not configured for blob
                continue;
            }
            // get original text from all blobs
            blobsExtractor.setExtractorProperties(fulltextConfiguration.propPathsByIndexBinary.get(indexName),
                    fulltextConfiguration.propPathsExcludedByIndexBinary.get(indexName),
                    fulltextConfiguration.indexesAllBinary.contains(indexName));
            List<String> strings = new ArrayList<>();
            for (Blob blob : blobsExtractor.getBlobs(document)) {
                String string = blobsText.computeIfAbsent(blob, this::blobToText);
                strings.add(string);
            }
            // add space at beginning and end for simulated phrase search using LIKE "% foo bar %"
            String text = " " + String.join(" ", strings) + " ";
            text = limitStringSize(text, fulltextConfiguration.fulltextFieldSizeLimit);
            String property = getFulltextPropertyName(SYSPROP_FULLTEXT_BINARY, indexName);
            for (DocumentRef docRef : docsToUpdate) {
                session.setDocumentSystemProp(docRef, property, text);
            }
        }
    }

    protected String stringToText(String string) {
        string = removeHtml(string);
        string = removeEntities(string);
        return string;
    }

    protected String removeHtml(String string) {
        // quick HTML detection on the initial part of the string
        String initial = string.substring(0, Math.min(string.length(), HTML_MAGIC_OFFSET)).toLowerCase();
        if (initial.startsWith("<!doctype html") || initial.contains("<html")) {
            // convert using Jericho HTML Parser
            string = new Source(string).getRenderer()
                                       .setIncludeHyperlinkURLs(false)
                                       .setDecorateFontStyles(false)
                                       .toString();
        }
        return string;
    }

    protected String removeEntities(String string) {
        if (string.indexOf('&') >= 0) {
            string = StringEscapeUtils.unescapeHtml4(string);
        }
        return string;
    }

    /**
     * Converts the blob to text by calling a converter.
     */
    protected String blobToText(Blob blob) {
        try {
            ConversionService conversionService = Framework.getService(ConversionService.class);
            if (conversionService == null) {
                log.debug("No ConversionService available");
                return "";
            }
            BlobHolder blobHolder = conversionService.convert(ANY2TEXT_CONVERTER, new SimpleBlobHolder(blob), null);
            if (blobHolder == null) {
                return "";
            }
            Blob resultBlob = blobHolder.getBlob();
            if (resultBlob == null) {
                return "";
            }
            String string = resultBlob.getString();
            // strip '\0 chars from text
            if (string.indexOf('\0') >= 0) {
                string = string.replace("\0", " ");
            }
            return string;
        } catch (ConversionException | IOException e) {
            String msg = "Could not extract fulltext of file '" + blob.getFilename() + "' for document: " + docId + ": "
                    + e;
            log.warn(msg);
            log.debug(msg, e);
            return "";
        }
    }

    @SuppressWarnings("boxing")
    protected String limitStringSize(String string, int maxSize) {
        if (maxSize != 0 && string.length() > maxSize) {
            if (log.isDebugEnabled()) {
                log.debug(String.format("Fulltext extract of length: %s for document: %s truncated to length: %s",
                        string.length(), docId, maxSize));
            }
            string = string.substring(0, maxSize);
        }
        return string;
    }

    protected String getFulltextPropertyName(String name, String indexName) {
        if (!FULLTEXT_DEFAULT_INDEX.equals(indexName)) {
            name += '_' + indexName;
        }
        return name;
    }

}
