/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Test;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalConfig;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@Features(TransactionalFeature.class)
@TransactionalConfig(autoStart = false)
public abstract class TestInMemoryBlobStoreTxAbstract extends TestAbstractBlobStore {

    protected int countStorageFiles() {
        InMemoryBlobStore baseStore = (InMemoryBlobStore) ((TransactionalBlobStore) bs).store;
        return baseStore.map.size();
    }

    protected int countTmpFiles() {
        InMemoryBlobStore transientStore = (InMemoryBlobStore) ((TransactionalBlobStore) bs).transientStore;
        return transientStore.map.size();
    }

    protected void assertVersioningCountFiles(int expected) {
        if (hasVersioning()) {
            assertEquals(expected, countStorageFiles());
            // tmp = storage when versioning, so no need to check twice
        }
    }

    protected void assertNoVersioningCountFiles(int expectedStorage, int expectedTmp) {
        if (!hasVersioning()) {
            assertEquals(expectedStorage, countStorageFiles());
            assertEquals(expectedTmp, countTmpFiles());
        }
    }

    protected void assertLegalHold(String key, boolean expectedLegalHold) {
        InMemoryBlobStore baseStore = (InMemoryBlobStore) ((TransactionalBlobStore) bs).store;
        boolean legalHold = Boolean.TRUE.equals(baseStore.legalHold.get(key));
        assertEquals(expectedLegalHold, legalHold);
    }

    @Test
    public void testTransaction() throws IOException {
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // start transaction
        assertTrue(TransactionHelper.startTransaction());

        // write a blob
        String key1 = bs.writeBlob(blobContext(ID1, FOO));
        assertKey(ID1, key1);

        // it's not in the storage yet
        assertNoVersioningCountFiles(0, 1);
        assertVersioningCountFiles(1);

        // read the blob back
        assertBlob(key1, FOO);

        // nothing changed in storage
        assertNoVersioningCountFiles(0, 1);
        assertVersioningCountFiles(1);

        // can still read the blob back
        assertBlob(key1, FOO);

        // update the blob (actually writes another blob)
        String key2 = bs.writeBlob(blobContext(ID1, BAR));
        assertKey(ID1, key2);

        // still not in storage
        // when not doing deduplication, we only keep one tmp file per key
        assertNoVersioningCountFiles(0, useDeDuplication() ? 2 : 1);
        assertVersioningCountFiles(2);

        // read the blob back
        assertBlob(key2, BAR);

        // commit
        TransactionHelper.commitOrRollbackTransaction();

        // blobs are now in permanent storage
        assertNoVersioningCountFiles(useDeDuplication() ? 2 : 1, 0);
        assertVersioningCountFiles(2);

        // blob can still be read
        assertBlob(key2, BAR);

        // -----

        // new transaction
        assertTrue(TransactionHelper.startTransaction());

        // delete the blob
        bs.deleteBlob(key2);

        // still in storage until commit
        assertNoVersioningCountFiles(useDeDuplication() ? 2 : 1, 0);
        assertVersioningCountFiles(2);

        // cannot read the blob anymore
        assertNoBlob(key2);

        // commit
        TransactionHelper.commitOrRollbackTransaction();

        // blob is now deleted from storage
        // (if deduplication first blob is still here, will need to be GCed)
        assertNoVersioningCountFiles(useDeDuplication() ? 1 : 0, 0);
        assertVersioningCountFiles(1);
    }

    @Test
    public void testTransactionWriteBlobProperties() throws IOException {
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // ----- tx doing blob write + properties change

        // start transaction
        assertTrue(TransactionHelper.startTransaction());

        // write a blob
        String key1 = bs.writeBlob(blobContext(ID1, FOO));
        assertKey(ID1, key1);

        // update blob properties (through TransactionalBlobStore)
        BlobUpdateContext blobUpdateContext = new BlobUpdateContext(key1).withUpdateLegalHold(true);
        bs.writeBlobProperties(blobUpdateContext);

        // nothing changed in store
        assertLegalHold(key1, false);

        // commit
        TransactionHelper.commitOrRollbackTransaction();

        // blob properties have been updated
        assertLegalHold(key1, true);

        // ----- tx doing just blob write

        // new transaction
        assertTrue(TransactionHelper.startTransaction());

        // write a blob
        String key2 = bs.writeBlob(blobContext(ID2, BAR));
        assertKey(ID2, key2);

        // commit
        TransactionHelper.commitOrRollbackTransaction();

        // ----- tx doing just properties change

        // new transaction
        assertTrue(TransactionHelper.startTransaction());

        // update blob properties
        blobUpdateContext = new BlobUpdateContext(key2).withUpdateLegalHold(true);
        bs.writeBlobProperties(blobUpdateContext);

        // nothing changed in store
        assertLegalHold(key2, false);

        // commit
        TransactionHelper.commitOrRollbackTransaction();

        // blob properties have been updated
        assertLegalHold(key2, true);
    }

}
