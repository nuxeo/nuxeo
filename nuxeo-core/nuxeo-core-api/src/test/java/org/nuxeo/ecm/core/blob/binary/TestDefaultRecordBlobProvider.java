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
package org.nuxeo.ecm.core.blob.binary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalConfig;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features(TransactionalFeature.class)
@TransactionalConfig(autoStart = false)
public class TestDefaultRecordBlobProvider {

    protected static final String ID = "12345";

    protected static final String HELLO_WORLD = "hello world";

    protected static final String LOREM_IPSUM = "lorem ipsum";

    protected static final String MIME_TYPE = "text/plain";

    protected static final String ENCODING = "ascii";

    protected static final String ENCODING_UTF8 = "UTF-8";

    protected static final String FILENAME = "hello.txt";

    protected static final String FILENAME_LOREM = "lorem.txt";

    protected static final String DIGEST = "1000deadbeef";

    protected static final String CONTENT_XPATH = "content";

    protected static final int JOIN_TIMEOUT = 5_000; // 5s, increase this when debugging

    protected DefaultRecordBlobProvider bp;

    /**
     * Helper to build a Blob and get the corresponding BlobInfo to use to re-read it from a BlobProvider.
     */
    public static class BlobAndBlobInfo {

        public final Blob blob;

        public final BlobInfo blobInfo;

        public BlobAndBlobInfo(Blob blob, BlobInfo blobInfo) {
            this.blob = blob;
            this.blobInfo = blobInfo;
        }

        public static BlobAndBlobInfo of(String string, String mimeType, String encoding, String filename) {
            Blob blob = new StringBlob(string, mimeType, encoding, filename);
            try {
                blob.setDigest(DigestUtils.md5Hex(blob.getByteArray()));
            } catch (IOException e) {
                throw new NuxeoException(e);
            }
            BlobInfo blobInfo = new BlobInfo();
            blobInfo.mimeType = blob.getMimeType();
            blobInfo.encoding = blob.getEncoding();
            blobInfo.filename = blob.getFilename();
            blobInfo.length = Long.valueOf(blob.getLength());
            blobInfo.digest = blob.getDigest();
            return new BlobAndBlobInfo(blob, blobInfo);
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

    @Before
    public void before() throws Exception {
        bp = new DefaultRecordBlobProvider();
        bp.initialize("repo", Collections.emptyMap());
    }

    @After
    public void after() {
        File dir = bp.getStorageDir();
        bp.close();
        assertTrue(FileUtils.deleteQuietly(dir));
    }

    protected int countStorageFiles() {
        return countFiles(bp.getStorageDir());
    }

    protected int countTmpFiles() {
        return countFiles(bp.getTmpDir());
    }

    protected static int countFiles(File dir) {
        int n = 0;
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                n += countFiles(f);
            } else {
                n++;
            }
        }
        return n;
    }

    @Test
    public void testWriteErrors() throws Exception {
        Blob blob = new StringBlob(HELLO_WORLD, MIME_TYPE, ENCODING, FILENAME);

        // must use main blob
        try {
            bp.writeBlob(blob, ID, "files/0/files:file");
            fail();
        } catch (NuxeoException e) {
            assertEquals("Cannot store blob at xpath 'files/0/files:file' in record blob provider: repo",
                    e.getMessage());
        }

        // must have a doc id
        try {
            bp.writeBlob(blob, "", CONTENT_XPATH);
            fail();
        } catch (NuxeoException e) {
            assertEquals("Missing id", e.getMessage());
        }
    }

    @Test
    public void testReadErrors() throws Exception {
        // unknown file
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = "nosuchfile";
        try {
            bp.readBlob(blobInfo);
            fail();
        } catch (IOException e) {
            assertEquals("Nonexistent file for key: nosuchfile", e.getMessage());
        }
    }

    @Test
    public void testCRUD() throws Exception {
        assertEquals(0, countStorageFiles());

        BlobAndBlobInfo bbi = BlobAndBlobInfo.of(HELLO_WORLD, MIME_TYPE, ENCODING, FILENAME);

        // write a blob
        String key = bp.writeBlob(bbi.blob, ID, CONTENT_XPATH);
        assertEquals(ID, key);
        assertEquals(1, countStorageFiles());

        // read the blob
        bbi.blobInfo.key = key;
        Blob blobr = bp.readBlob(bbi.blobInfo);
        assertBlobEquals(bbi.blob, blobr);

        // update the blob
        BlobAndBlobInfo bbi2 = BlobAndBlobInfo.of(LOREM_IPSUM, MIME_TYPE, ENCODING_UTF8, FILENAME_LOREM);
        String key2 = bp.writeBlob(bbi2.blob, ID, CONTENT_XPATH);
        assertEquals(ID, key2);
        assertEquals(1, countStorageFiles());

        // read the new blob
        bbi2.blobInfo.key = key;
        Blob blob2r = bp.readBlob(bbi2.blobInfo);
        assertBlobEquals(bbi2.blob, blob2r);

        // delete the blob
        bp.deleteBlob(ID, CONTENT_XPATH);
        assertEquals(0, countStorageFiles());

        // cannot read the blob anymore
        try {
            bp.readBlob(bbi2.blobInfo);
            fail();
        } catch (IOException e) {
            assertEquals("Nonexistent file for key: " + ID, e.getMessage());
        }
    }

    @Test
    public void testTransaction() throws Exception {
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // start transaction
        assertTrue(TransactionHelper.startTransaction());

        // write a blob
        BlobAndBlobInfo bbi = BlobAndBlobInfo.of(HELLO_WORLD, MIME_TYPE, ENCODING, FILENAME);
        String key = bp.writeBlob(bbi.blob, ID, CONTENT_XPATH);
        assertEquals(ID, key);

        // it's not in the storage yet
        assertEquals(0, countStorageFiles());
        assertEquals(1, countTmpFiles());

        // update the blob
        BlobAndBlobInfo bbi2 = BlobAndBlobInfo.of(LOREM_IPSUM, MIME_TYPE, ENCODING_UTF8, FILENAME_LOREM);
        String key2 = bp.writeBlob(bbi2.blob, ID, CONTENT_XPATH);
        assertEquals(ID, key2);

        // still not in storage
        assertEquals(0, countStorageFiles());
        assertEquals(1, countTmpFiles());

        // read the blob back
        bbi2.blobInfo.key = key;
        Blob blobr = bp.readBlob(bbi2.blobInfo);
        assertBlobEquals(bbi2.blob, blobr);

        // commit
        TransactionHelper.commitOrRollbackTransaction();

        // blob is now in permanent storage
        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blob can still be read
        Blob blob2 = bp.readBlob(bbi2.blobInfo);
        assertBlobEquals(bbi2.blob, blob2);

        // -----

        // new transaction
        assertTrue(TransactionHelper.startTransaction());

        // delete the blob
        bp.deleteBlob(key, CONTENT_XPATH);

        // still in storage until commit
        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // cannot read the blob anymore
        try {
            bp.readBlob(bbi2.blobInfo);
            fail();
        } catch (IOException e) {
            assertEquals("Nonexistent file for key: " + ID, e.getMessage());
        }

        // commit
        TransactionHelper.commitOrRollbackTransaction();

        // blob is now deleted from storage
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());
    }

    @Test
    public void testTransactionSeveralBlobs() throws Exception {
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        final int n = 20;

        // start transaction
        assertTrue(TransactionHelper.startTransaction());

        // write blobs
        for (int i = 0; i < n; i++) {
            Blob blob = new StringBlob(HELLO_WORLD, MIME_TYPE, ENCODING, FILENAME);
            String key = bp.writeBlob(blob, "docid" + i, CONTENT_XPATH);
            assertEquals("docid" + i, key);
        }

        // not in the storage yet
        assertEquals(0, countStorageFiles());
        assertEquals(n, countTmpFiles());

        // commit
        TransactionHelper.commitOrRollbackTransaction();

        // blobs are now in permanent storage
        assertEquals(n, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blobs can still be read
        for (int i = 0; i < n; i++) {
            BlobInfo blobInfo = new BlobInfo();
            blobInfo.key = "docid" + i;
            blobInfo.mimeType = MIME_TYPE;
            blobInfo.encoding = ENCODING;
            blobInfo.filename = FILENAME;
            blobInfo.digest = DIGEST;
            Blob blob = bp.readBlob(blobInfo);
            assertEquals(HELLO_WORLD, blob.getString());
        }
    }

    @Test
    public void testTransactionRollbackAfterCreation() throws Exception {
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // start transaction
        assertTrue(TransactionHelper.startTransaction());

        // write a blob
        BlobAndBlobInfo bbi = BlobAndBlobInfo.of(HELLO_WORLD, MIME_TYPE, ENCODING, FILENAME);
        String key = bp.writeBlob(bbi.blob, ID, CONTENT_XPATH);
        assertEquals(ID, key);

        // it's not in the storage yet
        assertEquals(0, countStorageFiles());
        assertEquals(1, countTmpFiles());

        // read the blob back
        bbi.blobInfo.key = key;
        Blob blob2 = bp.readBlob(bbi.blobInfo);
        assertBlobEquals(bbi.blob, blob2);

        // rollback
        TransactionHelper.setTransactionRollbackOnly();
        TransactionHelper.commitOrRollbackTransaction();

        // blob is removed from all storage
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blob cannot be read
        try {
            bp.readBlob(bbi.blobInfo);
            fail();
        } catch (IOException e) {
            assertEquals("Nonexistent file for key: " + ID, e.getMessage());
        }
    }

    @Test
    public void testTransactionRollbackAfterUpdate() throws Exception {
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // write a blob
        BlobAndBlobInfo bbi = BlobAndBlobInfo.of(HELLO_WORLD, MIME_TYPE, ENCODING, FILENAME);
        String key = bp.writeBlob(bbi.blob, ID, CONTENT_XPATH);
        assertEquals(ID, key);
        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // we can read it back
        bbi.blobInfo.key = key;
        Blob blobr = bp.readBlob(bbi.blobInfo);
        assertBlobEquals(bbi.blob, blobr);

        // start transaction
        assertTrue(TransactionHelper.startTransaction());

        // update the blob
        BlobAndBlobInfo bbi2 = BlobAndBlobInfo.of(LOREM_IPSUM, MIME_TYPE, ENCODING_UTF8, FILENAME_LOREM);
        String key2 = bp.writeBlob(bbi2.blob, ID, CONTENT_XPATH);
        assertEquals(ID, key2);
        assertEquals(1, countStorageFiles());
        assertEquals(1, countTmpFiles());

        // read the new blob
        bbi2.blobInfo.key = key;
        Blob blob2r = bp.readBlob(bbi2.blobInfo);
        assertBlobEquals(bbi2.blob, blob2r);

        // outside transaction we read the old one
        Runnable checkBlob = () -> {
            try {
                Blob blob2rtx = bp.readBlob(bbi2.blobInfo);
                assertEquals(HELLO_WORLD, blob2rtx.getString()); // old one
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
        blobr = bp.readBlob(bbi.blobInfo);
        assertBlobEquals(bbi.blob, blobr);
        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());
    }

    @Test
    public void testTransactionRollbackAfterDelete() throws Exception {
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // write a blob
        BlobAndBlobInfo bbi = BlobAndBlobInfo.of(HELLO_WORLD, MIME_TYPE, ENCODING, FILENAME);
        String key = bp.writeBlob(bbi.blob, ID, CONTENT_XPATH);
        assertEquals(ID, key);
        assertEquals(1, countStorageFiles());

        // we can read it back
        bbi.blobInfo.key = key;
        Blob blobr = bp.readBlob(bbi.blobInfo);
        assertBlobEquals(bbi.blob, blobr);

        // start transaction
        assertTrue(TransactionHelper.startTransaction());

        // delete the blob
        bp.deleteBlob(ID, CONTENT_XPATH);
        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blob cannot be read
        try {
            bp.readBlob(bbi.blobInfo);
            fail();
        } catch (IOException e) {
            assertEquals("Nonexistent file for key: " + ID, e.getMessage());
        }

        // outside transaction we still read the blob
        Runnable checkBlob = () -> {
            try {
                Blob blobrtx = bp.readBlob(bbi.blobInfo);
                assertBlobEquals(bbi.blob, blobrtx);
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
        blobr = bp.readBlob(bbi.blobInfo);
        assertBlobEquals(bbi.blob, blobr);
        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());
    }

    protected void blobWriter(int i, CyclicBarrier barrier, List<Throwable> exc) {
        TransactionHelper.runInTransaction(() -> {
            try {
                barrier.await();
                // write blob
                Blob blob = new StringBlob(HELLO_WORLD + "-" + i, MIME_TYPE, ENCODING, FILENAME);
                String key = bp.writeBlob(blob, ID, CONTENT_XPATH);
                assertEquals(ID, key);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                exc.add(e);
            } catch (Exception | AssertionError e) {
                exc.add(e);
            }
        });
    }

    protected void blobDeleter(int i, CyclicBarrier barrier, List<Throwable> exc) {
        TransactionHelper.runInTransaction(() -> {
            try {
                barrier.await();
                // delete blob
                bp.deleteBlob(ID, CONTENT_XPATH);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                exc.add(e);
            } catch (Exception | AssertionError e) {
                exc.add(e);
            }
        });
    }

    @Test
    public void testTransactionConcurrencyCreateCreate() throws Throwable {
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // create the blob concurrently
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Throwable> exc = new CopyOnWriteArrayList<>();
        Thread t1 = new Thread(() -> blobWriter(1, barrier, exc));
        Thread t2 = new Thread(() -> blobWriter(2, barrier, exc));
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
            assertEquals(ID, t.getMessage());
        } else {
            throw new NuxeoException(t);
        }

        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blob can be read
        BlobAndBlobInfo bbi = BlobAndBlobInfo.of("", MIME_TYPE, ENCODING, FILENAME);
        bbi.blobInfo.key = ID;
        Blob blob = bp.readBlob(bbi.blobInfo);
        assertTrue(blob.getString(), blob.getString().startsWith(HELLO_WORLD));
    }

    @Test
    public void testTransactionConcurrencyUpdateUpdate() throws Throwable {
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // write a blob
        BlobAndBlobInfo bbi = BlobAndBlobInfo.of(HELLO_WORLD, MIME_TYPE, ENCODING, FILENAME);
        String key = bp.writeBlob(bbi.blob, ID, CONTENT_XPATH);
        assertEquals(ID, key);

        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // update the blob concurrently
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Throwable> exc = new CopyOnWriteArrayList<>();
        Thread t1 = new Thread(() -> blobWriter(1, barrier, exc));
        Thread t2 = new Thread(() -> blobWriter(2, barrier, exc));
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
            assertEquals(ID, t.getMessage());
        } else {
            throw new NuxeoException(t);
        }

        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blob can be read
        bbi.blobInfo.key = key;
        Blob blob = bp.readBlob(bbi.blobInfo);
        assertTrue(blob.getString(), blob.getString().startsWith(HELLO_WORLD + "-"));
    }

    @Test
    public void testTransactionConcurrencyDeleteDelete() throws Throwable {
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // write a blob
        BlobAndBlobInfo bbi = BlobAndBlobInfo.of(HELLO_WORLD, MIME_TYPE, ENCODING, FILENAME);
        String key = bp.writeBlob(bbi.blob, ID, CONTENT_XPATH);
        assertEquals(ID, key);

        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // delete the blob concurrently
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Throwable> exc = new CopyOnWriteArrayList<>();
        Thread t1 = new Thread(() -> blobDeleter(1, barrier, exc));
        Thread t2 = new Thread(() -> blobDeleter(2, barrier, exc));
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
            assertEquals(ID, t.getMessage());
        } else {
            throw new NuxeoException(t);
        }

        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blob was deleted
        bbi.blobInfo.key = key;
        try {
            bp.readBlob(bbi.blobInfo);
        } catch (IOException e) {
            // delete won
            assertEquals("Nonexistent file for key: " + ID, e.getMessage());
        }
    }

    @Test
    public void testTransactionConcurrencyUpdateWinsOverDelete() throws Throwable {
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // write a blob
        BlobAndBlobInfo bbi = BlobAndBlobInfo.of(HELLO_WORLD, MIME_TYPE, ENCODING, FILENAME);
        String key = bp.writeBlob(bbi.blob, ID, CONTENT_XPATH);
        assertEquals(ID, key);

        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // update and delete the blob concurrently
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Throwable> exc = new CopyOnWriteArrayList<>();
        Thread t1 = new Thread(() -> {
            TransactionHelper.runInTransaction(() -> {
                try {
                    barrier.await(); // A
                    // write blob first
                    Blob blob = new StringBlob(HELLO_WORLD + "-1", MIME_TYPE, ENCODING, FILENAME);
                    String k = bp.writeBlob(blob, ID, CONTENT_XPATH);
                    barrier.await(); // B
                    assertEquals(ID, k);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exc.add(e);
                } catch (Exception | AssertionError e) {
                    exc.add(e);
                }
            });
        });
        Thread t2 = new Thread(() -> {
            TransactionHelper.runInTransaction(() -> {
                try {
                    barrier.await(); // A
                    barrier.await(); // B
                    // delete blob
                    bp.deleteBlob(ID, CONTENT_XPATH);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exc.add(e);
                } catch (Exception | AssertionError e) {
                    exc.add(e);
                }
            });
        });
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
            assertEquals(ID, t.getMessage());
        } else {
            throw new NuxeoException(t);
        }

        // update won
        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blob can be read
        bbi.blobInfo.key = key;
        Blob blob = bp.readBlob(bbi.blobInfo);
        assertTrue(blob.getString(), blob.getString().startsWith(HELLO_WORLD + "-1"));
    }

    @Test
    public void testTransactionConcurrencyDeleteWinsOverUpdate() throws Throwable {
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // write a blob
        BlobAndBlobInfo bbi = BlobAndBlobInfo.of(HELLO_WORLD, MIME_TYPE, ENCODING, FILENAME);
        String key = bp.writeBlob(bbi.blob, ID, CONTENT_XPATH);
        assertEquals(ID, key);

        assertEquals(1, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // update and delete the blob concurrently
        CyclicBarrier barrier = new CyclicBarrier(2);
        List<Throwable> exc = new CopyOnWriteArrayList<>();
        Thread t1 = new Thread(() -> {
            TransactionHelper.runInTransaction(() -> {
                try {
                    barrier.await(); // A
                    barrier.await(); // B
                    // write blob
                    Blob blob = new StringBlob(HELLO_WORLD + "-1", MIME_TYPE, ENCODING, FILENAME);
                    String k = bp.writeBlob(blob, ID, CONTENT_XPATH);
                    assertEquals(ID, k);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exc.add(e);
                } catch (Exception | AssertionError e) {
                    exc.add(e);
                }
            });
        });
        Thread t2 = new Thread(() -> {
            TransactionHelper.runInTransaction(() -> {
                try {
                    barrier.await(); // A
                    // delete blob first
                    bp.deleteBlob(ID, CONTENT_XPATH);
                    barrier.await(); // B
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    exc.add(e);
                } catch (Exception | AssertionError e) {
                    exc.add(e);
                }
            });
        });
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
            assertEquals(ID, t.getMessage());
        } else {
            throw new NuxeoException(t);
        }

        // delete won
        assertEquals(0, countStorageFiles());
        assertEquals(0, countTmpFiles());

        // blob was deleted
        bbi.blobInfo.key = key;
        try {
            bp.readBlob(bbi.blobInfo);
        } catch (IOException e) {
            // delete won
            assertEquals("Nonexistent file for key: " + ID, e.getMessage());
        }
    }

}
