/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
 *   bdelbosc
 */
package org.nuxeo.ecm.platform.preview.work;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Generates a document preview on worker node and cache the result in a transient store.
 *
 * @since 2021.41
 */
public class PreviewWork extends AbstractWork {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LogManager.getLogger(PreviewWork.class);

    public static final String STORE_NAME = "RenditionCache";

    protected final String documentId;

    protected final String xpath;

    protected final boolean blobPostProcessing;

    protected final String sourceId;

    public PreviewWork(DocumentModel doc, String xpath, boolean blobPostProcessing) {
        super();
        if (doc == null) {
            throw new DocumentNotFoundException("Document not found");
        }
        this.documentId = doc.getId();
        this.sourceId = getSourceId(doc);
        this.xpath = xpath;
        this.blobPostProcessing = blobPostProcessing;
        this.id = sourceId + ":" + xpath + ":preview:" + blobPostProcessing;
    }

    @Override
    public String getCategory() {
        return "renditionBuilder";
    }

    @Override
    public String getTitle() {
        return "Preview Work: " + getId();
    }

    @Override
    public boolean isIdempotent() {
        // If the result exists, there is nothing to do
        return getStore().isCompleted(getId());
    }

    public List<Blob> getPreview(Duration timeout) throws TimeoutException {
        TransientStore ts = getStore();
        long end = System.currentTimeMillis() + timeout.toMillis();
        if (!ts.exists(getId())) {
            submitWork();
        }
        while (!ts.isCompleted(getId())) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
            if (System.currentTimeMillis() > end) {
                throw new TimeoutException("No preview within duration " + timeout.getSeconds() + "s.");
            }
        }
        List<Blob> blobs = ts.getBlobs(getId());
        if (blobs == null || blobs.isEmpty()) {
            String errorMessage = (String) ts.getParameter(getId(), "error");
            if (errorMessage != null) {
                throw new NuxeoException(errorMessage);
            }
            return Collections.emptyList();
        }
        return blobs;
    }

    @Override
    public void work() {
        log.debug("Building a preview for doc: {}, source: {}, xpath: {}", documentId, sourceId, xpath);
        openSystemSession();
        DocumentModel doc;
        try {
            doc = session.getDocument(new IdRef(documentId));
        } catch (DocumentNotFoundException e) {
            inError("Cannot perform preview on a deleted document: " + documentId);
            return;
        }
        TransientStore ts = getStore();
        ts.setCompleted(getId(), false);
        try {
            List<Blob> result;
            if (xpath == null) {
                HtmlPreviewAdapter preview = doc.getAdapter(HtmlPreviewAdapter.class);
                result = preview.getFilePreviewBlobs(blobPostProcessing);
            } else {
                HtmlPreviewAdapter preview = PreviewHelper.getBlobPreviewAdapter(doc);
                result = preview.getFilePreviewBlobs(xpath, blobPostProcessing);
            }
            if (result == null || result.isEmpty()) {
                log.debug("No preview for doc: {}", doc);
                ts.putBlobs(getId(), Collections.emptyList());
            } else {
                ts.putBlobs(getId(), Collections.singletonList(result.get(0)));
            }
        } catch (PreviewException e) {
            log.warn("Preview failure for doc: {} {}", doc, e.getMessage());
            ts.putBlobs(getId(), Collections.emptyList());
        } catch (NuxeoException e) {
            inError("Fail to build preview: " + e.getMessage());
            return;
        }
        ts.setCompleted(getId(), true);
        log.debug("Preview completed {}", this::getId);
    }

    protected void inError(String message) {
        log.warn(message);
        TransientStore ts = getStore();
        ts.putParameter(getId(), "error", message);
        ts.setCompleted(getId(), true);
        ts.release(getId());
    }

    protected static TransientStore getStore() {
        TransientStoreService transientStoreService = Framework.getService(TransientStoreService.class);
        return transientStoreService.getStore(STORE_NAME);
    }

    /**
     * Returns the identifier of the object to render, could be the main blob or a doc.
     */
    protected String getSourceId(DocumentModel doc) {
        BlobHolder bh = doc.getAdapter(BlobHolder.class);
        if (bh == null) {
            throw new IllegalArgumentException("No blob holder for doc: " + doc);
        }
        try {
            Blob blob = bh.getBlob();
            if (blob != null && blob.getDigest() != null) {
                return blob.getDigest();
            }
            // no blob, or StringBlob built on other props like note:note
            return doc.getId() + ":" + doc.getChangeToken();
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot get blob for doc: " + doc, e);
        }
    }

    protected void submitWork() {
        WorkManager workManager = Framework.getService(WorkManager.class);
        workManager.schedule(this, false);
        getStore().putParameter(getId(), "doc", documentId);
    }

}
