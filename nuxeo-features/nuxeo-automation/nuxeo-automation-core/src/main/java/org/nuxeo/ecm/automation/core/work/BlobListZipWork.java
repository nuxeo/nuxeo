/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.work;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.AsyncBlob;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.transientstore.work.TransientStoreWork;
import org.nuxeo.ecm.core.utils.BlobUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Work to zip a list of document default blob and store the produced zip into the TransientStore.
 *
 * @since 9.3
 */
public class BlobListZipWork extends TransientStoreWork {

    private static final Log log = LogFactory.getLog(BlobListZipWork.class);

    public static final String CATEGORY = "blobListZip";

    public static final String CACHE_NAME = "blobListZip";

    private static final long serialVersionUID = 1L;

    protected List<String> docIds;

    protected String filename;

    protected final String key;

    protected final String storeName;

    public BlobListZipWork(String transientStoreKey, String originatingUsername, String filename, List<String> docIds) {
        this(transientStoreKey, originatingUsername, filename, docIds, filename);
    }

    public BlobListZipWork(String transientStoreKey, String originatingUsername, String filename, List<String> docIds,
            String storeName) {
        this.key = transientStoreKey;
        this.docIds = docIds;
        this.originatingUsername = originatingUsername;
        this.id = "BlobListZipWork-" + this.key + "-" + this.originatingUsername;
        this.storeName = storeName;
        if (StringUtils.isNotBlank(filename)) {
            this.filename = filename.toLowerCase().endsWith(".zip") ? filename : filename + ".zip";
        }

    }

    @Override
    public void cleanUp(boolean ok, Exception e) {
        super.cleanUp(ok, e);
        if (ok) {
            return;
        }
        List<Blob> blobs = Collections.singletonList(new AsyncBlob(key));
        TransientStore ts = getTransientStore();
        ts.putParameter(key, DownloadService.TRANSIENT_STORE_PARAM_ERROR, e.getMessage());
        updateAndCompleteStoreEntry(blobs);
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getTitle() {
        return id;
    }

    public TransientStore getTransientStore() {
        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        return tss.getStore(storeName != null ? storeName : CACHE_NAME);
    }

    protected void updateAndCompleteStoreEntry(List<Blob> blobs) {
        TransientStore ts = getTransientStore();
        if (!ts.exists(key)) {
            throw new NuxeoException("Zip TransientStore entry can not be null");
        }
        ts.putBlobs(key, blobs);
        ts.putParameter(key, DownloadService.TRANSIENT_STORE_PARAM_PROGRESS, 100);
        ts.setCompleted(key, true);
    }

    @Override
    public void work() {
        openUserSession();
        List<Blob> blobList = new ArrayList<>();
        DownloadService downloadService = Framework.getService(DownloadService.class);
        for (String docId : docIds) {
            DocumentRef docRef = new IdRef(docId);
            if (!session.exists(docRef)) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Cannot retrieve document '%s', probably deleted in the meanwhile", docId));
                }
                continue;
            }
            DocumentModel doc = session.getDocument(docRef);
            Blob blob = downloadService.resolveBlob(doc);
            if (blob == null) {
                log.trace("Not able to resolve blob");
                continue;
            } else if (!downloadService.checkPermission(doc, null, blob, "download", Collections.emptyMap())) {
                if (log.isDebugEnabled()) {
                    log.debug(
                            String.format("Not allowed to bulk download blob for document %s", doc.getPathAsString()));
                }
                continue;
            }
            blobList.add(blob);
        }
        if (blobList.isEmpty()) {
            log.debug("No blob to be zipped");
            updateAndCompleteStoreEntry(Collections.emptyList());
            return;
        }
        Blob blob;
        String finalFilename = StringUtils.isNotBlank(this.filename) ? this.filename : this.id;
        try {
            blob = BlobUtils.zip(blobList, finalFilename);
        } catch (IOException e) {
            TransientStore ts = getTransientStore();
            ts.putParameter(key, DownloadService.TRANSIENT_STORE_PARAM_ERROR, e.getMessage());
            throw new NuxeoException("Exception while zipping blob list", e);
        }
        updateAndCompleteStoreEntry(Collections.singletonList(blob));
    }

}
