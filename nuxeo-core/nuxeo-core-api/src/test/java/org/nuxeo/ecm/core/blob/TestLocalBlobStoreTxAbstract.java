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
package org.nuxeo.ecm.core.blob;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalConfig;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(TransactionalFeature.class)
@TransactionalConfig(autoStart = false)
public abstract class TestLocalBlobStoreTxAbstract extends TestAbstractBlobStore {

    protected static final int JOIN_TIMEOUT = 5_000; // 5s, increase this when debugging

    protected Path dir;

    protected Path tmp;

    @Override
    public void setUp() throws IOException {
        super.setUp();
        TransactionalBlobStore transactionalBlobStore = (TransactionalBlobStore) bs;
        dir = ((LocalBlobStore) transactionalBlobStore.store).pathStrategy.dir;
        tmp = ((LocalBlobStore) transactionalBlobStore.transientStore).pathStrategy.dir;
    }

    protected static void setUncaughtException(Thread thread, List<Throwable> exc) {
        thread.setUncaughtExceptionHandler((t, e) -> exc.add(e));
    }

    protected long countStorageFiles() throws IOException {
        return countFiles(dir);
    }

    protected long countTmpFiles() throws IOException {
        return countFiles(tmp);
    }

    protected static long countFiles(Path dir) throws IOException {
        try (Stream<Path> ps = Files.walk(dir)) {
            return ps.filter(Files::isRegularFile).count(); // NOSONAR (squid:S3725)
        }
    }

    protected static void assertBlobEquals(Blob blob1, Blob blob2) throws IOException {
        assertEquals(blob1.getString(), blob2.getString());
        assertEquals(blob1.getMimeType(), blob2.getMimeType());
        assertEquals(blob1.getEncoding(), blob2.getEncoding());
        assertEquals(blob1.getFilename(), blob2.getFilename());
        assertEquals(blob1.getLength(), blob2.getLength());
        assertEquals(blob1.getDigest(), blob2.getDigest());
    }

    protected static void assertEmptyBlobStream(Blob blob) throws IOException {
        try (InputStream stream = blob.getStream()) {
            assertEquals("", IOUtils.toString(stream, StandardCharsets.ISO_8859_1));
        }
    }

    @Test
    public void testCRUDInTransaction() {
        TransactionHelper.runInTransaction(() -> {
            try {
                testCRUD();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Test
    public void testTransaction() throws IOException {
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // start transaction
        assertTrue(TransactionHelper.startTransaction());

        // write a blob
        String key1 = bs.writeBlob(blobContext(ID1, FOO));
        assertEquals(useDeDuplication() ? FOO_MD5 : ID1, key1);

        // it's not in the storage yet
        assertEquals(0, countStorageFiles());
        assertEquals(1, countTmpFiles());

        // update the blob (actually writes another blob)
        String key2 = bs.writeBlob(blobContext(ID1, BAR));
        assertEquals(useDeDuplication() ? BAR_MD5 : ID1, key2);

        // still not in storage
        assertEquals(0, countStorageFiles());
        // when not doing deduplication, we only keep one tmp file per key
        assertEquals(useDeDuplication() ? 2 : 1, countTmpFiles());

        // read the blob back
        assertBlob(key2, BAR);

        // commit
        TransactionHelper.commitOrRollbackTransaction();

        // blobs are now in permanent storage
        assertEquals(useDeDuplication() ? 2 : 1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blob can still be read
        assertBlob(key2, BAR);

        // -----

        // new transaction
        assertTrue(TransactionHelper.startTransaction());

        // delete the blob
        bs.deleteBlob(key2);

        // still in storage until commit
        assertEquals(useDeDuplication() ? 2 : 1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // cannot read the blob anymore
        assertNoBlob(key2);

        // commit
        TransactionHelper.commitOrRollbackTransaction();

        // blob is now deleted from storage
        // (if deduplication first blob is still here, will need to be GCed)
        assertEquals(useDeDuplication() ? 1 : 0, countStorageFiles());
        assertEquals(0, countTmpFiles());
    }

    @Test
    public void testTransactionRollbackAfterCreation() throws IOException {
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // start transaction
        assertTrue(TransactionHelper.startTransaction());

        // write a blob
        String key = bs.writeBlob(blobContext(ID1, FOO));
        assertEquals(useDeDuplication() ? FOO_MD5 : ID1, key);

        // it's not in the storage yet
        assertEquals(0, countStorageFiles());
        assertEquals(1, countTmpFiles());

        // we can read it back
        assertBlob(key, FOO);

        // rollback
        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();

        // blob is removed from all storage
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blob cannot be read
        assertNoBlob(key);
    }

    // note: this test doesn't make much sense as we're using deduplication
    // so there's no real notion of an "update" of a blob
    @Test
    public void testTransactionRollbackAfterUpdate() throws IOException, InterruptedException {
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // write a blob
        String key1 = bs.writeBlob(blobContext(ID1, FOO));
        assertEquals(useDeDuplication() ? FOO_MD5 : ID1, key1);
        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // we can read it back
        assertBlob(key1, FOO);

        // start transaction
        assertTrue(TransactionHelper.startTransaction());

        // update the blob (actually writes another blob)
        String key2 = bs.writeBlob(blobContext(ID1, BAR));
        assertEquals(useDeDuplication() ? BAR_MD5 : ID1, key2);
        assertEquals(1, countStorageFiles());
        assertEquals(1, countTmpFiles());

        // read the new blob
        assertBlob(key2, BAR);

        // outside transaction we read the old one
        Runnable checkBlob = () -> {
            try {
                assertBlob(key1, FOO); // old one
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
        };
        Thread thread = new Thread(checkBlob);
        thread.start();
        thread.join(JOIN_TIMEOUT);

        // rollback
        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();

        // we still read the old blob
        assertBlob(key1, FOO);
        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());
    }

    @Test
    public void testTransactionRollbackAfterDelete() throws IOException, InterruptedException {
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // write a blob
        String key = bs.writeBlob(blobContext(ID1, FOO));
        assertEquals(useDeDuplication() ? FOO_MD5 : ID1, key);
        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // we can read it back
        assertBlob(key, FOO);

        // start transaction
        assertTrue(TransactionHelper.startTransaction());

        // delete the blob
        bs.deleteBlob(key);
        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blob cannot be read
        assertNoBlob(key);

        // outside transaction we still read the blob
        Runnable checkBlob = () -> {
            try {
                assertBlob(key, FOO);
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
        };
        Thread thread = new Thread(checkBlob);
        thread.start();
        thread.join(JOIN_TIMEOUT);

        // rollback
        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();

        // we still read the blob
        assertBlob(key, FOO);
        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());
    }

    protected void blobWriter(int i, CyclicBarrier barrier) {
        TransactionHelper.runInTransaction(() -> {
            try {
                barrier.await();
                // write blob
                try {
                    bs.writeBlob(blobContext(ID1, FOO + i));
                } finally {
                    barrier.await();
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (Exception | AssertionError e) {
                throw new RuntimeException(e);
            }
        });
    }

    protected void blobDeleter(CyclicBarrier barrier) {
        TransactionHelper.runInTransaction(() -> {
            try {
                barrier.await();
                // delete blob
                bs.deleteBlob(blobContext(ID1, null));
            } catch (RuntimeException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (Exception | AssertionError e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testTransactionConcurrencyCreateCreate() throws IOException, InterruptedException {
        assumeFalse("Concurrency test does not make sense with deduplication", useDeDuplication());
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // create the blob concurrently
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Throwable> exc = new CopyOnWriteArrayList<>();
        Thread t1 = new Thread(() -> blobWriter(1, barrier));
        Thread t2 = new Thread(() -> blobWriter(2, barrier));
        setUncaughtException(t1, exc);
        setUncaughtException(t2, exc);
        t1.start();
        t2.start();
        t1.join(JOIN_TIMEOUT);
        t2.join(JOIN_TIMEOUT);

        // one thread had an error
        if (exc.size() == 2) {
            exc.get(0).addSuppressed(exc.get(1));
            throw new NuxeoException(exc.get(0));
        }
        assertEquals("One thread should throw ConcurrentUpdateException", 1, exc.size());
        Throwable t = exc.get(0);
        if (t instanceof ConcurrentUpdateException) {
            assertEquals(ID1, t.getMessage());
        } else {
            throw new NuxeoException(t);
        }

        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blob can be read
        assertTrue(bs.readBlob(ID1, tmpFile));
        String value = new String(Files.readAllBytes(tmpFile), UTF_8);
        assertTrue(value, value.startsWith(FOO));
    }

    @Test
    public void testTransactionConcurrencyUpdateUpdate() throws IOException, InterruptedException {
        assumeFalse("Concurrency test does not make sense with deduplication", useDeDuplication());
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // write a blob
        String key = bs.writeBlob(blobContext(ID1, FOO));
        assertEquals(ID1, key);

        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // update the blob concurrently
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Throwable> exc = new CopyOnWriteArrayList<>();
        Thread t1 = new Thread(() -> blobWriter(1, barrier));
        Thread t2 = new Thread(() -> blobWriter(2, barrier));
        setUncaughtException(t1, exc);
        setUncaughtException(t2, exc);
        t1.start();
        t2.start();
        t1.join(JOIN_TIMEOUT);
        t2.join(JOIN_TIMEOUT);

        // one thread had an error
        if (exc.size() == 2) {
            exc.get(0).addSuppressed(exc.get(1));
            throw new NuxeoException(exc.get(0));
        }
        assertEquals("One thread should throw ConcurrentUpdateException", 1, exc.size());
        Throwable t = exc.get(0);
        if (t instanceof ConcurrentUpdateException) {
            assertEquals(ID1, t.getMessage());
        } else {
            throw new NuxeoException(t);
        }

        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blob can be read
        assertTrue(bs.readBlob(ID1, tmpFile));
        String value = new String(Files.readAllBytes(tmpFile), UTF_8);
        assertTrue(value, value.startsWith(FOO));
    }

    @Test
    public void testTransactionConcurrencyDeleteDelete() throws IOException, InterruptedException {
        assumeFalse("Concurrency test does not make sense with deduplication", useDeDuplication());
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // write a blob
        String key = bs.writeBlob(blobContext(ID1, FOO));
        assertEquals(ID1, key);

        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // delete the blob concurrently
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Throwable> exc = new CopyOnWriteArrayList<>();
        Thread t1 = new Thread(() -> blobDeleter(barrier));
        Thread t2 = new Thread(() -> blobDeleter(barrier));
        setUncaughtException(t1, exc);
        setUncaughtException(t2, exc);
        t1.start();
        t2.start();
        t1.join(JOIN_TIMEOUT);
        t2.join(JOIN_TIMEOUT);

        // one thread had an error
        if (exc.size() == 2) {
            exc.get(0).addSuppressed(exc.get(1));
            throw new NuxeoException(exc.get(0));
        }
        assertEquals("One thread should throw ConcurrentUpdateException", 1, exc.size());
        Throwable t = exc.get(0);
        if (t instanceof ConcurrentUpdateException) {
            assertEquals(ID1, t.getMessage());
        } else {
            throw new NuxeoException(t);
        }

        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blob was deleted
        assertNoBlob(ID1);
    }

    @Test
    public void testTransactionConcurrencyUpdateWinsOverDelete() throws IOException, InterruptedException {
        assumeFalse("Concurrency test does not make sense with deduplication", useDeDuplication());
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // write a blob
        String key = bs.writeBlob(blobContext(ID1, FOO));
        assertEquals(ID1, key);

        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // update and delete the blob concurrently
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Throwable> exc = new CopyOnWriteArrayList<>();
        Thread t1 = new Thread(() -> TransactionHelper.runInTransaction(() -> {
            try {
                barrier.await(); // A
                // write blob first
                String k = bs.writeBlob(blobContext(ID1, "foo2"));
                barrier.await(); // B
                assertEquals(ID1, k);
            } catch (RuntimeException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (Exception | AssertionError e) {
                throw new RuntimeException(e);
            }
        }));
        Thread t2 = new Thread(() -> TransactionHelper.runInTransaction(() -> {
            try {
                barrier.await(); // A
                barrier.await(); // B
                // delete blob
                bs.deleteBlob(blobContext(ID1, null));
            } catch (RuntimeException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (Exception | AssertionError e) {
                throw new RuntimeException(e);
            }
        }));
        setUncaughtException(t1, exc);
        setUncaughtException(t2, exc);
        t1.start();
        t2.start();
        t1.join(JOIN_TIMEOUT);
        t2.join(JOIN_TIMEOUT);

        // one thread had an error
        if (exc.size() == 2) {
            exc.get(0).addSuppressed(exc.get(1));
            throw new NuxeoException(exc.get(0));
        }
        assertEquals("One thread should throw ConcurrentUpdateException", 1, exc.size());
        Throwable t = exc.get(0);
        if (t instanceof ConcurrentUpdateException) {
            assertEquals(ID1, t.getMessage());
        } else {
            throw new NuxeoException(t);
        }

        // update won
        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blob can be read
        assertBlob(ID1, "foo2");
    }

    @Test
    public void testTransactionConcurrencyDeleteWinsOverUpdate() throws IOException, InterruptedException {
        assumeFalse("Concurrency test does not make sense with deduplication", useDeDuplication());
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // write a blob
        String key = bs.writeBlob(blobContext(ID1, FOO));
        assertEquals(ID1, key);

        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // update and delete the blob concurrently
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Throwable> exc = new CopyOnWriteArrayList<>();
        Thread t1 = new Thread(() -> TransactionHelper.runInTransaction(() -> {
            try {
                barrier.await(); // A
                barrier.await(); // B
                // write blob
                bs.writeBlob(blobContext(ID1, "foo2"));
                fail("should get ConcurrentUpdateException");
            } catch (RuntimeException e) {
                barrier.reset();
                throw e;
            } catch (InterruptedException e) {
                barrier.reset();
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (Exception | AssertionError e) {
                barrier.reset();
                throw new RuntimeException(e);
            }
        }));
        Thread t2 = new Thread(() -> TransactionHelper.runInTransaction(() -> {
            try {
                barrier.await(); // A
                // delete blob first
                bs.deleteBlob(blobContext(ID1, null));
                barrier.await(); // B
                try {
                    barrier.await(); // C
                    fail("should get BrokenBarrierException");
                } catch (BrokenBarrierException bbe) {
                    // expected, from reset() on exception in first thread
                }
            } catch (RuntimeException e) {
                barrier.reset();
                throw e;
            } catch (InterruptedException e) {
                barrier.reset();
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (Exception | AssertionError e) {
                barrier.reset();
                throw new RuntimeException(e);
            }
        }));
        setUncaughtException(t1, exc);
        setUncaughtException(t2, exc);
        t1.start();
        t2.start();
        t1.join(JOIN_TIMEOUT);
        t2.join(JOIN_TIMEOUT);

        // one thread had an error
        if (exc.size() == 2) {
            exc.get(0).addSuppressed(exc.get(1));
            throw new NuxeoException(exc.get(0));
        }
        assertEquals("One thread should throw ConcurrentUpdateException", 1, exc.size());
        Throwable t = exc.get(0);
        if (t instanceof ConcurrentUpdateException) {
            assertEquals(ID1, t.getMessage());
        } else {
            throw new NuxeoException(t);
        }

        // delete won
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blob was deleted
        assertNoBlob(ID1);
    }

}
