/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.blob.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.ecm.core.blob.InMemoryBlobStore;
import org.nuxeo.ecm.core.blob.KeyStrategyDigest;
import org.nuxeo.ecm.core.blob.TestAbstractBlobStore;

public abstract class TestS3BlobStoreAbstract extends TestAbstractBlobStore {

    protected static boolean propertiesSet;

    @BeforeClass
    public static void beforeClass() {
        S3TestHelper.getProperties().forEach(S3TestHelper::setProperty);
        propertiesSet = true;
    }

    @AfterClass
    public static void afterClass() {
        if (propertiesSet) {
            S3TestHelper.getProperties().keySet().forEach(S3TestHelper::removeProperty);
            propertiesSet = false;
        }
    }

    @After
    public void tearDown() throws IOException {
        super.tearDown();

        BlobProvider otherbp = blobManager.getBlobProvider("other");
        BlobStore otherbs = ((BlobStoreBlobProvider) otherbp).store;
        otherbs.clear();
    }

    // copy/move from another S3BlobStore has an different, optimized code path

    @Test
    public void testCopyIsOptimized() {
        BlobProvider otherbp = blobManager.getBlobProvider("other");
        BlobStore otherS3Store = ((BlobStoreBlobProvider) otherbp).store; // no need for unwrap
        assertTrue(bs.copyBlobIsOptimized(otherS3Store));
        InMemoryBlobStore otherStore = new InMemoryBlobStore("mem", new KeyStrategyDigest("MD5"));
        assertFalse(bs.copyBlobIsOptimized(otherStore));
    }

    @Test
    public void testCopyFromS3BlobStore() throws IOException {
        testCopyOrMoveFromS3BlobStore(false);
    }

    @Test
    public void testMoveFromS3BlobStore() throws IOException {
        testCopyOrMoveFromS3BlobStore(true);
    }

    protected void testCopyOrMoveFromS3BlobStore(boolean atomicMove) throws IOException {
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
