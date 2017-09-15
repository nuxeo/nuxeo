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

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.operations.blob.CreateZip;
import org.nuxeo.ecm.automation.core.util.BlobList;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.AsyncBlob;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.transientstore.work.TransientStoreWork;
import org.nuxeo.runtime.api.Framework;

/**
 * Work to zip a list of blob and store the produced zip into the TransientStore.
 *
 * @since 9.3
 */
public class BlobListZipWork extends TransientStoreWork {

    public static final String CATEGORY = "blobListZip";

    public static final String CACHE_NAME = "blobListZip";

    private static final long serialVersionUID = 1L;

    protected List<Blob> blobList;

    protected String filename;

    protected final String key;

    protected final String storeName;

    public BlobListZipWork(String transientStoreKey, String originatingUsername, String filename, List<Blob> blobList) {
        this(transientStoreKey, originatingUsername, filename, blobList, filename);
    }

    public BlobListZipWork(String transientStoreKey, String originatingUsername, String filename, List<Blob> blobList,
            String storeName) {
        this.key = transientStoreKey;
        this.blobList = blobList;
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
        AutomationService as = Framework.getLocalService(AutomationService.class);
        try (OperationContext oc = new OperationContext(session)) {
            Blob blob;
            if (blobList.size() == 1) {
                blob = blobList.get(0);
            } else {
                oc.push("filename", StringUtils.isNotBlank(this.filename) ? this.filename : this.id);
                oc.setInput(new BlobList(blobList));
                blob = (Blob) as.run(oc, CreateZip.ID);
            }
            updateAndCompleteStoreEntry(Collections.singletonList(blob));
        } catch (OperationException e) {
            TransientStore ts = getTransientStore();
            ts.putParameter(key, DownloadService.TRANSIENT_STORE_PARAM_ERROR, e.getMessage());
            throw new NuxeoException("Exception while zipping blob list", e);
        }

    }

}
