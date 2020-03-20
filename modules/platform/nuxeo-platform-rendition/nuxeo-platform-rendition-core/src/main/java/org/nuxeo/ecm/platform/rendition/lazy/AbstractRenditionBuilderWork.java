/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
package org.nuxeo.ecm.platform.rendition.lazy;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.transientstore.work.TransientStoreWork;
import org.nuxeo.ecm.platform.rendition.extension.RenditionProvider;
import org.nuxeo.ecm.platform.rendition.impl.LazyRendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionDefinition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 7.2
 */
public abstract class AbstractRenditionBuilderWork extends TransientStoreWork {

    private static final long serialVersionUID = 1L;

    protected final String key;

    protected final DocumentRef docRef;

    protected final String renditionName;

    private static final Logger log = LogManager.getLogger(AbstractRenditionBuilderWork.class);

    public static final String CATEGORY = "renditionBuilder";

    public AbstractRenditionBuilderWork(String key, DocumentModel doc, RenditionDefinition def) {
        super();
        this.key = key;
        docRef = doc.getRef();
        repositoryName = doc.getRepositoryName();
        renditionName = def.getName();
        setOriginatingUsername(doc.getPrincipal().getName());
        this.id = buildId(doc, def);
    }

    protected String buildId(DocumentModel doc, RenditionDefinition def) {
        StringBuilder sb = new StringBuilder("rendition:");
        sb.append(doc.getId());
        String variant = def.getProvider().getVariant(doc, def);
        if (variant != null) {
            sb.append("::");
            sb.append(variant);
        }
        sb.append("::");
        sb.append(def.getName());
        return sb.toString();
    }

    @Override
    public String getTitle() {
        return "Lazy Rendition for " + renditionName + " on " + docRef.toString() + " on behalf of "
                + originatingUsername;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    protected String getTransientStoreName() {
        return AbstractLazyCachableRenditionProvider.CACHE_NAME;
    }

    @Override
    public boolean isIdempotent() {
        // The same rendering can be executed multiple times because the result is transient.
        return false;
    }

    @Override
    public boolean isCoalescing() {
        // The same rendering has no reason to be executed more than once if schedulled multiple times
        // The last schedulled work will build the wanted rendition
        return true;
    }

    @Override
    public void work() {
        log.debug("Starting work: {} with id: {} for transient store key: {} and document: {}",
                getClass().getSimpleName(), id, key, docRef);
        openUserSession();
        DocumentModel doc = session.getDocument(docRef);

        RenditionService rs = Framework.getService(RenditionService.class);
        RenditionDefinition def = rs.getAvailableRenditionDefinition(doc, renditionName);

        log.debug("Starting rendition computation.");
        List<Blob> blobs = doComputeRendition(session, doc, def);
        updateAndCompleteStoreEntry(getSourceDocumentModificationDate(doc), blobs);
    }

    @Override
    public void cleanUp(boolean ok, Exception e) {
        try {
            if (!ok) {
                storeAnErrorRendition();
            }
        } finally {
            super.cleanUp(ok, e);
        }
    }

    protected void storeAnErrorRendition() {
        if (session == null) {
            log.info("No session, unable to clean work: {} with id: {} for transient store key: {} and document: {}",
                    getClass().getSimpleName(), id, key, docRef);
            return;
        }
        DocumentModel doc = session.getDocument(docRef);
        String sourceDocumentModificationDate = getSourceDocumentModificationDate(doc);
        List<Blob> blobs = new ArrayList<>();
        StringBlob emptyBlob = new StringBlob("");
        emptyBlob.setFilename("error");
        emptyBlob.setMimeType("text/plain;" + LazyRendition.ERROR_MARKER);
        blobs.add(emptyBlob);
        updateAndCompleteStoreEntry(sourceDocumentModificationDate, blobs);
    }

    void updateAndCompleteStoreEntry(String sourceDocumentModificationDate, List<Blob> blobs) {
        log.debug("Updating and completing transient store entry with key: {} (workId: {}, document: {})", key, id,
                docRef);
        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore(getTransientStoreName());

        log.debug("BEGIN - Putting blobs for transient store entry with key: {}", key);
        ts.putBlobs(key, blobs);
        log.debug("END - Putting blobs for transient store entry with key: {}", key);

        if (sourceDocumentModificationDate != null) {
            log.debug(
                    "Updating source document modification date parameter to: {} for transient store entry with key: {}",
                    sourceDocumentModificationDate, key);
            ts.putParameter(key, AbstractLazyCachableRenditionProvider.SOURCE_DOCUMENT_MODIFICATION_DATE_KEY,
                    sourceDocumentModificationDate);
        }
        ts.setCompleted(key, true);
    }

    protected String getSourceDocumentModificationDate(DocumentModel doc) {
        RenditionService rs = Framework.getService(RenditionService.class);
        RenditionDefinition definition = rs.getAvailableRenditionDefinition(doc, renditionName);
        RenditionProvider provider = definition.getProvider();
        if (provider instanceof AbstractLazyCachableRenditionProvider) {
            return ((AbstractLazyCachableRenditionProvider) provider).getSourceDocumentModificationDate(doc,
                    definition);
        }
        return null;
    }

    /**
     * Does the actual Rendition Computation: this code will be called from inside an Asynchronous Work
     */
    protected abstract List<Blob> doComputeRendition(CoreSession session, DocumentModel doc, RenditionDefinition def);

}
