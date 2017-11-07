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

import java.io.IOException;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.nuxeo.ecm.core.api.blobholder.BlobHolderAdapterService;
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
@Operation(id = BulkDownload.ID, category = Constants.CAT_BLOB, label = "Bulk Downlaod", description = "Prepare a Zip of a list of documents which is build asynchrously. Produced Zip will be available in the TransientStore with the key returned by the JSON.")
public class BulkDownload {

    private static final Log log = LogFactory.getLog(BulkDownload.class);

    public static final String ID = "Blob.BulkDownload";

    public static final String WORKERID_KEY = "workerid";

    @Context
    protected CoreSession session;

    @Context
    BlobHolderAdapterService blobHolderAdapterService;

    @Param(name = "filename", required = false)
    protected String fileName;

    protected String buildTransientStoreKey(DocumentModelList docs) {
        StringBuffer sb = new StringBuffer();
        for (DocumentModel doc : docs) {
            sb.append(doc.getId());
            sb.append("::");
            Calendar modif = (Calendar) doc.getPropertyValue("dc:modified");
            if (modif != null) {
                long millis = modif.getTimeInMillis();
                // the date may have been rounded by the storage layer, normalize it to the second
                millis -= millis % 1000;
                sb.append(millis);
                sb.append("::");
            }
        }
        // Rendered documents might differ according to current user
        sb.append(session.getPrincipal().getName());
        return DigestUtils.md5Hex(sb.toString());
    }

    @OperationMethod
    public Blob run(DocumentModelList docs) throws IOException {
        // build the key
        String key = buildTransientStoreKey(docs);
        TransientStoreService tss = Framework.getService(TransientStoreService.class);

        TransientStore ts = tss.getStore(DownloadService.TRANSIENT_STORE_STORE_NAME);
        if (ts == null) {
            throw new NuxeoException("Unable to find download Transient Store");
        }
        List<Blob> blobs = null;
        if (!ts.exists(key)) {
            log.trace("No async download already initialized");
            Work work = new BlobListZipWork(key, session.getPrincipal().getName(), this.fileName,
                    docs.stream().map(DocumentModel::getId).collect(Collectors.toList()),
                    DownloadService.TRANSIENT_STORE_STORE_NAME);
            ts.setCompleted(key, false);
            ts.putParameter(key, WORKERID_KEY, work.getId());
            blobs = Collections.singletonList(new AsyncBlob(key));
            ts.putBlobs(key, blobs);
            Framework.getService(WorkManager.class).schedule(work, Scheduling.IF_NOT_SCHEDULED);
            return blobs.get(0);
        } else {
            log.trace("Async download already initialized");
            blobs = ts.getBlobs(key);
            if (ts.isCompleted(key)) {
                if (blobs != null && blobs.size() == 1) {
                    Blob blob = blobs.get(0);
                    ts.release(key);
                    return blob;
                } else {
                    ts.release(key);
                    throw new NuxeoException("Cannot retrieve blob");
                }

            } else {
                Work work = new BlobListZipWork(key, session.getPrincipal().getName(), this.fileName,
                        docs.stream().map(DocumentModel::getId).collect(Collectors.toList()),
                        DownloadService.TRANSIENT_STORE_STORE_NAME);
                WorkManager wm = Framework.getService(WorkManager.class);
                wm.schedule(work, Scheduling.IF_NOT_SCHEDULED);
                return new AsyncBlob(key);
            }
        }
    }

}
