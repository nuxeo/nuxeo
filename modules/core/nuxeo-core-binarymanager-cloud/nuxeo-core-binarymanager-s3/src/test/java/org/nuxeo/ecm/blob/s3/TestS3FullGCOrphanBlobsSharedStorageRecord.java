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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.blob.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.action.GarbageCollectOrphanBlobsAction.RESULT_DELETED_SIZE_KEY;
import static org.nuxeo.ecm.core.action.GarbageCollectOrphanBlobsAction.RESULT_TOTAL_SIZE_KEY;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;

import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.KeyStrategy;
import org.nuxeo.ecm.core.blob.KeyStrategyDigest;
import org.nuxeo.ecm.core.blob.KeyStrategyDocId;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.bulk.AbstractTestFullGCOrphanBlobs;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 2023.5
 */
@Features({ CoreFeature.class, S3BlobProviderFeature.class })
public class TestS3FullGCOrphanBlobsSharedStorageRecord extends AbstractTestFullGCOrphanBlobs {

    protected DocumentModel doc1;

    protected DocumentModel doc2;

    @Override
    public int getNbFiles() {
        return 2;
    }

    @Before
    @Override
    public void setup() {
        assumeTrue("MongoDB feature only", coreFeature.getStorageConfiguration().isDBS());
        sizeOfBinaries = 0L;
        doc1 = session.createDocumentModel("/", "doc1", "File");
        Blob blob1 = Blobs.createBlob(CONTENT + 1);
        sizeOfBinaries += blob1.getLength();
        doc1.setPropertyValue("file:content", (Serializable) blob1);
        doc1 = session.createDocument(doc1);
        // 1 blob in store
        session.makeRecord(doc1.getRef());
        // 2 blobs in store because makeRecord copy to record provider with key strategy doc id
        sizeOfBinaries += blob1.getLength();
        doc1 = session.getDocument(doc1.getRef());

        doc2 = session.createDocumentModel("/", "doc2", "File");
        Blob blob2 = Blobs.createBlob(CONTENT + 2);
        sizeOfBinaries += blob2.getLength();
        doc2.setPropertyValue("file:content", (Serializable) blob2);
        doc2 = session.createDocument(doc2);
        // 3 blobs in store

        coreFeature.waitForAsyncCompletion();
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-blob-provider-s3-shared-storage-record.xml")
    public void testGCBlobsAction() {
        ManagedBlob blob1 = (ManagedBlob) doc1.getPropertyValue("file:content");
        long blobSize1 = blob1.getLength();
        assertNotNull(blob1);
        S3BlobProvider blobProvider1 = (S3BlobProvider) Framework.getService(BlobManager.class).getBlobProvider(blob1);
        assertTrue(blobProvider1.getKeyStrategy() instanceof KeyStrategyDocId);
        String blobKey1 = blob1.getKey();
        assertTrue(blobProvider1.store.exists(doc1.getId()));
        ManagedBlob blob2 = (ManagedBlob) doc2.getPropertyValue("file:content");
        assertNotNull(blob2);
        S3BlobProvider blobProvider2 = (S3BlobProvider) Framework.getService(BlobManager.class).getBlobProvider(blob2);
        assertTrue(blobProvider2.getKeyStrategy() instanceof KeyStrategyDigest);
        String blobKey2 = blob2.getKey().substring(blob2.getKey().indexOf(":") + 1);;
        assertTrue(blobProvider2.store.exists(blobKey2));

        // We are testing record provider
        // blob versioning is enabled and blob key has the ${docId}@{versionId} pattern
        assertTrue(blobKey1.startsWith(blobProvider1.blobProviderId + ":" + doc1.getId() + KeyStrategy.VER_SEP));

        BulkStatus status = triggerAndWaitGC(false);
        assertNotNull(status);
        assertEquals(COMPLETED, status.getState());
        assertEquals(false, status.hasError());

        // nothing was deleted
        assertTrue(blobProvider1.store.exists(doc1.getId()));
        assertTrue(blobProvider2.store.exists(blobKey2));

        // 3 from scrolling default (test) provider, 3 from scrolling other record provider
        assertEquals(3 * 2, status.getProcessed());
        assertEquals(0, status.getErrorCount());
        assertEquals(3 * 2, status.getTotal());
        // the blob form the default is orphaned and was deleted
        assertEquals(5, status.getSkipCount());
        assertEquals(blobSize1, ((Number) status.getResult().get(RESULT_DELETED_SIZE_KEY)).longValue());
        assertEquals(sizeOfBinaries * 2, ((Number) status.getResult().get(RESULT_TOTAL_SIZE_KEY)).longValue());
    }

}
