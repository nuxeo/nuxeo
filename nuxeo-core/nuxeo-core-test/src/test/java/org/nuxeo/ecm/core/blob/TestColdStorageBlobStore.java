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
package org.nuxeo.ecm.core.blob;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Duration;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.DummyBlobProvider;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 2021.19
 */
@RunWith(FeaturesRunner.class)
@Features(BlobManagerFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.core.test.tests:OSGI-INF/test-coldStorage-blob-provider.xml")
public class TestColdStorageBlobStore {

    protected static final String XPATH = "content";

    protected static final String ID = "id";

    @Inject
    protected BlobManager blobManager;

    protected BlobProvider bp;

    protected BlobInfo blobInfo;

    @Before
    public void setUp() throws IOException {
        bp = blobManager.getBlobProvider("coldStorage");
        bp.writeBlob(new BlobContext(Blobs.createBlob("foo"), ID, XPATH));
        blobInfo = new BlobInfo();
        blobInfo.key = ID;
    }

    @Test
    public void testFlags() {
        assertTrue(bp.isTransactional());
        assertFalse(bp.isRecordMode());
        assertTrue(bp.isColdStorageMode());
    }

    @Test
    public void testBlobStoreStatus() throws IOException, InterruptedException {
        // read blob status
        BlobStatus status = getBlobStatus();
        assertTrue(status.isDownloadable());
        assertFalse(status.isOngoingRestore());

        // Send to cold storage
        BlobUpdateContext blobUpdateCtx = new BlobUpdateContext(ID).withColdStorageClass(true);
        bp.updateBlob(blobUpdateCtx);
        status = getBlobStatus();
        assertFalse(status.isDownloadable());
        assertFalse(status.isOngoingRestore());

        // Restore from cold storage
        blobUpdateCtx = new BlobUpdateContext(ID).withRestoreForDuration(Duration.ofDays(5));
        bp.updateBlob(blobUpdateCtx);
        status = getBlobStatus();
        assertFalse(status.isDownloadable());
        assertTrue(status.isOngoingRestore());

        // Wait for restoration delay
        Thread.sleep(DummyBlobProvider.RESTORE_DELAY_MILLISECONDS + 200);
        status = getBlobStatus();
        assertTrue(status.isDownloadable());
        assertFalse(status.isOngoingRestore());
    }

    protected BlobStatus getBlobStatus() throws IOException {
        Blob blob = bp.readBlob(blobInfo);
        return bp.getStatus((ManagedBlob) blob);
    }
}
