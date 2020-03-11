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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.blob.BlobStore.OptionalOrUnknown;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

@RunWith(FeaturesRunner.class)
@Features(BlobManagerFeature.class)
public abstract class TestAbstractBlobStore {

    protected static final String XPATH = "content";

    protected static final String ID1 = "id1";

    protected static final String ID2 = "id2";

    protected static final String ID3 = "id3";

    protected static final String FOO = "foo";

    protected static final String BAR = "bar";

    protected static final String FOO_MD5 = "acbd18db4cc2f85cedef654fccc4a4d8";

    protected static final String BAR_MD5 = "37b51d194a7513e45b56f6524f2d51f2";

    @Inject
    protected BlobManager blobManager;

    protected BlobProvider bp;

    protected BlobStore bs;

    protected Path tmpFile;

    @Before
    public void setUp() throws IOException {
        bp = blobManager.getBlobProvider("test");
        bs = ((BlobStoreBlobProvider) bp).store;
        clearBlobStore();
        tmpFile = Files.createTempFile("tmp_", ".tmp");
    }

    @After
    public void tearDown() throws IOException {
        closeBlobStore();
        Files.deleteIfExists(tmpFile);
    }

    public void clearBlobStore() throws IOException {
        clearBlobStore(bs);
    }

    protected void clearBlobStore(BlobStore blobStore) throws IOException {
        if (blobStore instanceof TransactionalBlobStore) {
            clearBlobStore((TransactionalBlobStore) blobStore);
        } else if (blobStore instanceof CachingBlobStore) {
            clearBlobStore((CachingBlobStore) blobStore);
        } else if (blobStore instanceof LocalBlobStore) {
            clearBlobStore((LocalBlobStore) blobStore);
        } else if (blobStore instanceof InMemoryBlobStore) {
            clearBlobStore((InMemoryBlobStore) blobStore);
        }
    }

    protected void clearBlobStore(TransactionalBlobStore blobStore) throws IOException {
        clearBlobStore(blobStore.store);
        clearBlobStore(blobStore.transientStore);
    }

    protected void clearBlobStore(CachingBlobStore blobStore) throws IOException {
        clearBlobStore(blobStore.store);
        Path dir = blobStore.cacheDir;
        FileUtils.cleanDirectory(dir.toFile());
    }

    protected void clearBlobStore(LocalBlobStore blobStore) throws IOException {
        Path dir = blobStore.pathStrategy.dir;
        FileUtils.cleanDirectory(dir.toFile());
    }

    protected void clearBlobStore(InMemoryBlobStore blobStore) {
        blobStore.map.clear();
    }

    public void closeBlobStore() throws IOException {
        clearBlobStore();
    }

    /**
     * If this is true, then GC won't delete some files that are younger than a time threshold.
     */
    public boolean hasGCTimeThreshold() {
        return false;
    }

    public void waitForGCTimeThreshold() {
        // no wait by default
    }

    // we can't check the size on files encrypted when stored
    public boolean checkSizeOfGCedFiles() {
        return true;
    }

    public boolean useDeDuplication() {
        return bs.getKeyStrategy().useDeDuplication();
    }

    protected BlobContext blobContext(String id, String value) {
        return new BlobContext(value == null ? null : new StringBlob(value), id, XPATH);
    }

    protected void assertBlob(String key, String value) throws IOException {
        assertBlob(bs, key, value);
    }

    protected void assertBlob(BlobStore bs, String key, String value) throws IOException {
        // check readBlobTo
        assertTrue(bs.readBlob(key, tmpFile));
        assertEquals(value, new String(Files.readAllBytes(tmpFile), UTF_8));
        // check file
        OptionalOrUnknown<Path> fileOpt = bs.getFile(key);
        if (fileOpt.isKnown()) {
            assertTrue(fileOpt.isPresent());
            assertEquals(value, new String(Files.readAllBytes(fileOpt.get()), UTF_8));
        }
        // check stream
        OptionalOrUnknown<InputStream> streamOpt = bs.getStream(key);
        if (streamOpt.isKnown()) {
            assertTrue(streamOpt.isPresent());
            try (InputStream stream = streamOpt.get()) {
                assertEquals(value, IOUtils.toString(stream, UTF_8));
            }
        }
    }

    protected void assertKey(String expected, String actual) {
        if (!useDeDuplication()) {
            if (bs.hasVersioning()) {
                assertTrue(actual + " does not start with " + expected, actual.startsWith(expected + '@'));
            } else {
                assertEquals(expected, actual);
            }
        }
    }

    protected void assertNoBlob(String key) throws IOException {
        assertNoBlob(bs, key);
    }

    protected void assertNoBlob(BlobStore bs, String key) throws IOException {
        // check readBlobTo
        assertFalse(bs.readBlob(key, tmpFile));
        OptionalOrUnknown<Path> fileOpt = bs.getFile(key);
        if (fileOpt.isKnown()) {
            assertFalse(fileOpt.isPresent());
        }
        OptionalOrUnknown<InputStream> streamOpt = bs.getStream(key);
        if (streamOpt.isKnown()) {
            assertFalse(streamOpt.isPresent());
        }
    }

    @Test
    public void testMissing() throws IOException {
        assertNoBlob(ID1);
    }

    @Test
    public void testCRUD() throws IOException {
        // no blob initially
        assertNoBlob(ID1);

        // store blob
        String key1 = bs.writeBlob(blobContext(ID1, FOO));
        assertKey(ID1, key1);
        // check content
        assertBlob(key1, FOO);

        // replace
        String key2 = bs.writeBlob(blobContext(ID1, BAR));
        assertKey(ID1, key2);
        // check content
        assertBlob(key2, BAR);

        if (!useDeDuplication()) {
            // delete
            bs.deleteBlob(blobContext(key2, null));
            // check deleted
            assertNoBlob(key2);
        }
    }

    @Test
    public void testWriteFromSameBlobProvier() throws IOException {
        // store blob
        String key1 = bs.writeBlob(blobContext(ID1, FOO));
        assertKey(ID1, key1);

        // construct an actual Blob for it
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = "test:" + key1;
        Blob blob = new SimpleManagedBlob(blobInfo);

        // this write should use an optimized copy path
        String key2 = bs.writeBlob(new BlobContext(blob, ID2, XPATH));
        assertKey(ID2, key2);
    }

    @Test
    public void testCopy() throws IOException {
        testCopyOrMove(false);
    }

    @Test
    public void testMove() throws IOException {
        testCopyOrMove(true);
    }

    protected void testCopyOrMove(boolean atomicMove) throws IOException {
        // we don't test the unimplemented copyBlob API, as it's only called from commit or during caching
        assumeFalse("low-level copy/move not tested in transactional blob store", bp.isTransactional());

        // stream available for source
        BlobStore bs2 = new InMemoryBlobStore("mem", new KeyStrategyDigest("MD5"));
        testCopyOrMove(bs2, atomicMove);

        // no stream for the source, no local file
        BlobStore bs3 = new InMemoryBlobStore("mem", new KeyStrategyDigest("MD5"), true, false);
        testCopyOrMove(bs3, atomicMove);

        // no stream for the source, but has local file
        BlobStore bs4 = new InMemoryBlobStore("mem", new KeyStrategyDigest("MD5"), true, true);
        testCopyOrMove(bs4, atomicMove);
    }

    protected void testCopyOrMove(BlobStore sourceStore, boolean atomicMove) throws IOException {
        assertFalse(bs.copyBlob(ID2, sourceStore, ID1, atomicMove));
        assertFalse(bs.copyBlob(ID2, sourceStore, FOO_MD5, atomicMove));
        String key1 = sourceStore.writeBlob(blobContext(ID1, FOO));
        assertEquals(FOO_MD5, key1);
        assertTrue(bs.copyBlob(ID2, sourceStore, key1, atomicMove));
        if (!useDeDuplication()) {
            assertBlob(ID2, FOO);
        }
        if (atomicMove) {
            assertNoBlob(sourceStore, key1);
        } else {
            assertBlob(sourceStore, key1, FOO);
        }
    }

    @Test
    public void testGC() throws IOException {
        // doesn't bring anything over the LocalBlobStore GC test;  avoid additional setup for this
        assumeFalse("GC not tested in transactional blob store", bp.isTransactional());

        // store blob
        String key1 = bs.writeBlob(blobContext(ID1, FOO));
        assertKey(ID1, key1);

        // other blob we'll GC
        String key2 = bs.writeBlob(blobContext(ID2, "barbaz"));
        assertKey(ID2, key2);

        long addNum = 0;
        long addSize = 0;
        String key3 = null;
        if (hasGCTimeThreshold()) {
            // sleep before GC to pass time threshold (in some implementations)
            waitForGCTimeThreshold();
            // create another binary after time threshold, it won't be GCed
            key3 = bs.writeBlob(blobContext(ID3, "abcde"));
            assertKey(ID3, key3);
            addNum = 1;
            addSize = 5;
        }

        // GC in non-delete mode
        BinaryGarbageCollector gc = bs.getBinaryGarbageCollector();
        assertFalse(gc.isInProgress());
        gc.start();
        assertTrue(gc.isInProgress());
        gc.mark(key1);
        assertTrue(gc.isInProgress());
        gc.stop(false);
        assertFalse(gc.isInProgress());
        BinaryManagerStatus status = gc.getStatus();
        assertEquals(1 + addNum, status.numBinaries);
        assertEquals(1, status.numBinariesGC);
        if (checkSizeOfGCedFiles()) {
            assertEquals(3 + addSize, status.sizeBinaries);
            assertEquals(6, status.sizeBinariesGC);
        }

        // check content
        assertBlob(key1, FOO);
        assertBlob(key2, "barbaz");

        // real GC
        assertFalse(gc.isInProgress());
        gc.start();
        assertTrue(gc.isInProgress());
        gc.mark(key1);
        assertTrue(gc.isInProgress());
        gc.stop(true); // delete=true
        assertFalse(gc.isInProgress());
        status = gc.getStatus();
        assertEquals(1 + addNum, status.numBinaries);
        assertEquals(1, status.numBinariesGC);
        if (checkSizeOfGCedFiles()) {
            assertEquals(3 + addSize, status.sizeBinaries);
            assertEquals(6, status.sizeBinariesGC);
        }
        // check content, one blob gone
        assertBlob(key1, FOO);
        assertNoBlob(key2);
        // if time threshold, other blob is still here
        if (hasGCTimeThreshold()) {
            assertBlob(key3, "abcde");
        }
    }

}
