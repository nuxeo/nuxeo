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
package org.nuxeo.ecm.automation.core.operations.blob;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.work.BlobListZipWork;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.AsyncBlob;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkManager.Scheduling;
import org.nuxeo.runtime.api.Framework;

/**
 * Asynchronous Bulk Download Operation.
 *
 * @since 9.3
 */
@Operation(id = BulkDownload.ID, category = Constants.CAT_BLOB, label = "Bulk Download", description = "Prepare a Zip of a list of documents which is build asynchrously. Produced Zip will be available in the TransientStore with the key returned by the JSON.")
public class BulkDownload {

    public static final String ID = "Blob.BulkDownload";

    @Context
    protected CoreSession session;

    @Param(name = "filename", required = false)
    protected String fileName;

    @OperationMethod
    public Blob run(DocumentModelList docs) {
        TransientStoreService tss = Framework.getService(TransientStoreService.class);
        TransientStore ts = tss.getStore(DownloadService.TRANSIENT_STORE_STORE_NAME);
        if (ts == null) {
            throw new NuxeoException("Unable to find download Transient Store");
        }
        String key = UUID.randomUUID().toString();
        List<String> docIds = docs.stream().map(DocumentModel::getId).collect(Collectors.toList());
        Work work = new BlobListZipWork(key, session.getPrincipal().getName(), fileName, docIds,
                DownloadService.TRANSIENT_STORE_STORE_NAME);
        ts.setCompleted(key, false);
        Blob blob = new AsyncBlob(key);
        ts.putBlobs(key, Collections.singletonList(blob));
        Framework.getService(WorkManager.class).schedule(work, Scheduling.IF_NOT_SCHEDULED);
        return blob;
    }

}
