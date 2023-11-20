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
package org.nuxeo.ecm.platform.rendition.work;

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
import org.nuxeo.ecm.platform.rendition.Rendition;
import org.nuxeo.ecm.platform.rendition.service.RenditionService;
import org.nuxeo.runtime.api.Framework;

/**
 * Generates a document rendition on worker node and cache the result in a transient store.
 *
 * @since 2021.41
 */
public class RenditionWork extends AbstractWork {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LogManager.getLogger(RenditionWork.class);

    public static final String STORE_NAME = "RenditionCache";

    protected final String documentId;

    protected final String renditionName;

    protected final String sourceId;

    @Override
    public String getCategory() {
        return "renditionBuilder";
    }

    public RenditionWork(DocumentModel doc, String renditionName) {
        super();
        if (doc == null) {
            throw new DocumentNotFoundException("Document not found");
        }
        this.documentId = doc.getId();
        this.sourceId = getSourceId(doc);
        this.renditionName = renditionName;
        this.id = sourceId + ":" + renditionName;
    }

    @Override
    public String getTitle() {
        return "Rendition Work: " + getId();
    }

    @Override
    public boolean isIdempotent() {
        // If the result exists, there is nothing to do
        return getStore().isCompleted(getId());
    }

    public Blob getRendition(Duration timeout) throws TimeoutException {
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
                throw new TimeoutException("No rendition within duration " + timeout.getSeconds() + "s.");
            }
        }
        List<Blob> blobs = ts.getBlobs(getId());
        if (blobs != null && !blobs.isEmpty()) {
            return blobs.get(0);
        } else {
            String errorMessage = (String) ts.getParameter(getId(), "error");
            if (errorMessage != null) {
                throw new NuxeoException(errorMessage);
            }
        }
        return null;
    }

    @Override
    public void work() {
        log.debug("Building a rendition for doc: {}, source: {}, rendition: {}", documentId, sourceId, renditionName);
        openSystemSession();
        DocumentModel doc;
        try {
            doc = session.getDocument(new IdRef(documentId));
        } catch (DocumentNotFoundException e) {
            inError("Cannot perform a rendition on a deleted document: " + documentId);
            return;
        }
        RenditionService renditionService = Framework.getService(RenditionService.class);
        Rendition rendition;
        try {
            rendition = renditionService.getRendition(doc, renditionName);
        } catch (NuxeoException e) {
            inError("Invalid rendition: " + e.getMessage());
            return;
        }
        if (rendition == null) {
            inError("No rendition: " + renditionName + " for doc: " + doc);
            return;
        }
        TransientStore ts = getStore();
        ts.setCompleted(getId(), false);
        try {
            Blob result = rendition.getBlob();
            if (result != null) {
                ts.putBlobs(getId(), Collections.singletonList(result));
            }
        } catch (NuxeoException e) {
            inError("Fail to apply rendition " + e.getMessage());
            return;
        }
        ts.setCompleted(getId(), true);
        log.debug("Rendition completed {}", this::getId);
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
        try {
            if (bh != null) {
                Blob blob = bh.getBlob();
                if (blob != null && blob.getDigest() != null) {
                    return blob.getDigest();
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot get blob for doc: " + doc, e);
        }
        // no blob, or StringBlob built on other props like note:note
        return doc.getId() + ":" + doc.getChangeToken();
    }

    protected void submitWork() {
        WorkManager workManager = Framework.getService(WorkManager.class);
        workManager.schedule(this, false);
        getStore().putParameter(getId(), "doc", documentId);
    }

}
