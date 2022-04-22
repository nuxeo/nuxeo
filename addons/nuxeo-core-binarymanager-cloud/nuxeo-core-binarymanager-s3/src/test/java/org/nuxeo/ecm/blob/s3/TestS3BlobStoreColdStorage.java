/*
 * (C) Copyright 2022 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume RENARD
 */
package org.nuxeo.ecm.blob.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Duration;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.blob.BlobContext;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.BlobStatus;
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.ecm.core.blob.BlobUpdateContext;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalConfig;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.amazonaws.services.s3.model.StorageClass;

/**
 * @since 2021.19
 */
@Features({ S3BlobProviderFeature.class, TransactionalFeature.class })
@RunWith(FeaturesRunner.class)
@TransactionalConfig(autoStart = false)
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-blob-provider-s3-coldstorage.xml")
public class TestS3BlobStoreColdStorage {

    protected static final String XPATH = "content";

    @Inject
    protected BlobManager blobManager;

    protected BlobProvider bp;

    protected BlobStore bs;

    @Before
    public void setUp() throws IOException {
        bp = blobManager.getBlobProvider("test");
        bs = ((BlobStoreBlobProvider) bp).store;
    }

    @Test
    public void testFlags() {
        assertTrue(bp.isTransactional());
        assertFalse(bp.isRecordMode());
        assertTrue(bp.isColdStorageMode());
    }

    @Test
    public void testSendToColdStorageTransactional() throws IOException {
        assertTrue(TransactionHelper.startTransaction());
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = bp.writeBlob(new BlobContext(Blobs.createBlob("foo"), "id", XPATH));

        // check blob status before transaction is committed
        BlobStatus status = getBlobStatus(blobInfo);
        assertFalse(status.isDownloadable());
        assertNull(status.getStorageClass());
        assertFalse(status.isOngoingRestore());

        // commit transaction and check status
        TransactionHelper.commitOrRollbackTransaction();
        status = getBlobStatus(blobInfo);
        assertTrue(status.isDownloadable());
        assertNull(status.getStorageClass());
        assertFalse(status.isOngoingRestore());

        // Send to cold storage
        assertTrue(TransactionHelper.startTransaction());
        BlobUpdateContext blobUpdateCtx = new BlobUpdateContext(blobInfo.key).withColdStorageClass(true);
        bp.updateBlob(blobUpdateCtx);

        // check blob status before transaction is committed
        status = getBlobStatus(blobInfo);
        assertTrue(status.isDownloadable());
        assertNull(status.getStorageClass());
        assertFalse(status.isOngoingRestore());

        // commit transaction and check status
        TransactionHelper.commitOrRollbackTransaction();
        status = getBlobStatus(blobInfo);
        assertFalse(status.isDownloadable());
        assertEquals(StorageClass.Glacier.toString(), status.getStorageClass());
        assertFalse(status.isOngoingRestore());

        // Restore from cold storage
        assertTrue(TransactionHelper.startTransaction());
        blobUpdateCtx = new BlobUpdateContext(blobInfo.key).withRestoreForDuration(Duration.ofDays(5));
        bp.updateBlob(blobUpdateCtx);

        // check blob status before transaction is committed
        status = getBlobStatus(blobInfo);
        assertFalse(status.isDownloadable());
        assertEquals(StorageClass.Glacier.toString(), status.getStorageClass());
        assertFalse(status.isOngoingRestore());

        // commit transaction and check status
        TransactionHelper.commitOrRollbackTransaction();
        status = getBlobStatus(blobInfo);
        assertFalse(status.isDownloadable());
        assertEquals(StorageClass.Glacier.toString(), status.getStorageClass());
        assertTrue(status.isOngoingRestore());
    }

    protected BlobStatus getBlobStatus(BlobInfo blobInfo) throws IOException {
        Blob blob = bp.readBlob(blobInfo);
        return bp.getStatus((ManagedBlob) blob);
    }

}
