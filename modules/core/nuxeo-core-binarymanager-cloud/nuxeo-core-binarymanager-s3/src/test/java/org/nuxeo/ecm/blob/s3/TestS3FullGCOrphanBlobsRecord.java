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
import static org.nuxeo.ecm.core.action.GarbageCollectOrphanBlobsAction.RESULT_DELETED_SIZE_KEY;
import static org.nuxeo.ecm.core.action.GarbageCollectOrphanBlobsAction.RESULT_TOTAL_SIZE_KEY;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;

import org.junit.Test;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.KeyStrategy;
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
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-blob-provider-s3-record.xml")
public class TestS3FullGCOrphanBlobsRecord extends AbstractTestFullGCOrphanBlobs {

    @Override
    public int getNbFiles() {
        return 1;
    }

    @Test
    public void testGCBlobsAction() {
        DocumentModelList docList = session.query("SELECT * From Document");
        assertEquals(1, docList.size());
        DocumentModel doc = docList.get(0);
        ManagedBlob blob = (ManagedBlob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        S3BlobProvider blobProvider = (S3BlobProvider) Framework.getService(BlobManager.class).getBlobProvider(blob);
        assertTrue(blobProvider.getKeyStrategy() instanceof KeyStrategyDocId);
        String blobKey = blob.getKey();
        assertTrue(blobProvider.store.exists(doc.getId()));

        // We are testing record provider
        // blob versioning is enabled and blob key has the ${docId}@{versionId} pattern
        assertTrue(blobKey.startsWith(blobProvider.blobProviderId + ":" + doc.getId() + KeyStrategy.VER_SEP));

        BulkStatus status = triggerAndWaitGC(false);
        assertNotNull(status);
        assertEquals(COMPLETED, status.getState());
        assertEquals(false, status.hasError());

        // nothing was deleted
        assertTrue(blobProvider.store.exists(doc.getId()));

        assertEquals(1, status.getProcessed());
        assertEquals(0, status.getErrorCount());
        assertEquals(1, status.getTotal());
        assertEquals(1, status.getSkipCount());
        assertEquals(0, ((Number) status.getResult().get(RESULT_DELETED_SIZE_KEY)).longValue());
        assertEquals(sizeOfBinaries, ((Number) status.getResult().get(RESULT_TOTAL_SIZE_KEY)).longValue());
    }

}
