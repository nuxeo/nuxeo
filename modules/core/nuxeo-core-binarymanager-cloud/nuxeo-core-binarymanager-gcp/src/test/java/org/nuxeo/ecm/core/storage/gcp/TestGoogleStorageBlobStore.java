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
package org.nuxeo.ecm.core.storage.gcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.IOException;

import org.junit.Test;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.ecm.core.blob.InMemoryBlobStore;
import org.nuxeo.ecm.core.blob.KeyStrategyDigest;
import org.nuxeo.ecm.core.blob.TestAbstractBlobStore;
import org.nuxeo.runtime.test.runner.Features;

/**
 * @since 2023.5
 */
@Features(GoogleStorageBlobProviderFeature.class)
public class TestGoogleStorageBlobStore extends TestAbstractBlobStore {

    // copy/move from another GoogleStorageBlobStore has a different, optimized code path

    @Test
    public void testCopyIsOptimized() {
        BlobProvider otherbp = blobManager.getBlobProvider("other");
        BlobStore otherGSStore = ((BlobStoreBlobProvider) otherbp).store; // no need for unwrap
        assertTrue(bs.copyBlobIsOptimized(otherGSStore));
        InMemoryBlobStore otherStore = new InMemoryBlobStore("mem", new KeyStrategyDigest("MD5"));
        assertFalse(bs.copyBlobIsOptimized(otherStore));
    }

    @Test
    public void testCopyFromGSBlobStore() throws IOException {
        testCopyOrMoveFromGSBlobStore(false);
    }

    @Test
    public void testMoveFromGSBlobStore() throws IOException {
        testCopyOrMoveFromGSBlobStore(true);
    }

    protected void testCopyOrMoveFromGSBlobStore(boolean atomicMove) throws IOException {
        // we don't test the unimplemented copyBlob API, as it's only called from commit or during caching
        assumeFalse("low-level copy/move not tested in transactional blob store", bp.isTransactional());

        BlobProvider otherbp = blobManager.getBlobProvider("other");
        BlobStore sourceStore = ((BlobStoreBlobProvider) otherbp).store;
        String key1 = useDeDuplication() ? FOO_MD5 : ID1;
        String key2 = useDeDuplication() ? key1 : ID2;
        assertNull(bs.copyOrMoveBlob(key2, sourceStore, key1, atomicMove));
        assertEquals(key1, sourceStore.writeBlob(blobContext(ID1, FOO)));
        String key3 = bs.copyOrMoveBlob(key2, sourceStore, key1, atomicMove);
        assertEquals(key2, key3);
        assertBlob(bs, key2, FOO);
        if (atomicMove) {
            assertNoBlob(sourceStore, key1);
        } else {
            assertBlob(sourceStore, key1, FOO);
        }
    }
}
