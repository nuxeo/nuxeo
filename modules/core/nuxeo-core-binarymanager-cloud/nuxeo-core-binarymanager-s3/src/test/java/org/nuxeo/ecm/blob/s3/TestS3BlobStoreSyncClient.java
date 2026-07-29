/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.UUID;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.test.runner.Deploy;

@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-blob-provider-s3-sync-client.xml")
public class TestS3BlobStoreSyncClient extends TestS3BlobStoreAbstract {

    @Test
    public void testSyncUploadRejectsBlobAboveLimit() throws IOException {
        var sparseFile = Files.createTempFile("nuxeo-s3-sync-upload-oversize-", ".bin");
        try {
            // sparse allocation: setLength reports the logical size via Files.size() without allocating disk blocks
            var size = S3BlobStore.MAX_SYNC_UPLOAD_SIZE.bytes() + 1;
            try (var raf = new RandomAccessFile(sparseFile.toFile(), "rw")) {
                raf.setLength(size);
            }
            assertEquals(size, Files.size(sparseFile));

            var s3Store = (S3BlobStore) bs.unwrap();
            String key = "nuxeo-test-oversize-" + UUID.randomUUID();
            var e = assertThrows(NuxeoException.class, () -> s3Store.writeFile(key, sparseFile, null, "test"));
            assertEquals(e.getStatusCode(), HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
        } finally {
            Files.deleteIfExists(sparseFile);
        }
    }

}
