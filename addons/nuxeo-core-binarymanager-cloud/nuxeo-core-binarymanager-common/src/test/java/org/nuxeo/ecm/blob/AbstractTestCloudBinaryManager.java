/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.blob;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.binary.Binary;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManager;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.ecm.core.blob.binary.CachingBinaryManager;
import org.nuxeo.ecm.core.blob.binary.LazyBinary;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 7.10
 */
public abstract class AbstractTestCloudBinaryManager<T extends CachingBinaryManager> {

    private static final Log log = LogFactory.getLog(AbstractTestCloudBinaryManager.class);

    protected abstract T getBinaryManager() throws IOException;

    protected abstract Set<String> listObjects();

    protected static final String CONTENT = "this is a file au caf\u00e9";

    protected static final String CONTENT_MD5 = "d25ea4f4642073b7f218024d397dbaef";

    protected static final String CONTENT2 = "abc";

    protected static final String CONTENT2_MD5 = "900150983cd24fb0d6963f7d28e17f72";

    protected static final String CONTENT3 = "defg";

    protected static final String CONTENT3_MD5 = "025e4da7edac35ede583f5e8d51aa7ec";

    protected T binaryManager;

    protected void removeObjects() throws IOException {
        getBinaryManager().removeBinaries(listObjects());
    }

    protected void removeObject(String digest) throws IOException {
        getBinaryManager().removeBinaries(Collections.singleton(digest));
    }

    @Before
    public void setUp() throws IOException {
        binaryManager = getBinaryManager();
        removeObjects();
    }

    /** Overridden when encrypted storage has bigger binaries than the original. */
    public boolean isStorageSizeSameAsOriginalSize() {
        return true;
    }

    @Test
    public void testStoreFile() throws Exception {
        Binary binary = binaryManager.getBinary(CONTENT_MD5);
        assertTrue(binary instanceof LazyBinary);
        if (binary.getStream() != null) {
            // the tests have already been run
            // make sure we delete it from the bucket first
            removeObject(CONTENT_MD5);
            // XXX binaryManager.removeBinary(CONTENT_MD5);
            binaryManager.fileCache.clear();
        }

        // store binary
        byte[] bytes = CONTENT.getBytes("UTF-8");
        binary = binaryManager.getBinary(Blobs.createBlob(CONTENT));
        assertNotNull(binary);

        // get binary (from cache)
        binary = binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        assertEquals(CONTENT, toString(binary.getStream()));

        // get binary (clean cache)
        binaryManager.fileCache.clear();
        binary = binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        assertTrue(binary instanceof LazyBinary);
        assertEquals(CONTENT, toString(binary.getStream()));
        // refetch, now in cache
        binary = binaryManager.getBinary(CONTENT_MD5);
        assertTrue(binary instanceof LazyBinary);
        assertEquals(CONTENT, toString(binary.getStream()));

        // get binary (clean cache), fetch length first
        binaryManager.fileCache.clear();
        binary = binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        assertTrue(binary instanceof LazyBinary);
        assertEquals(CONTENT, toString(binary.getStream()));
    }

    @Test
    public void testAsBlobProvider() throws Exception {
        // to acquire the BinaryGarbageCollector, the BlobManagerComponent only has a BlobProvider
        if (binaryManager instanceof BlobProvider) {
            BinaryManager bm = ((BlobProvider) binaryManager).getBinaryManager();
            assertNotNull(bm);
            assertEquals(binaryManager, bm);
        }
    }

    /**
     * NOTE THAT THIS TEST WILL REMOVE ALL FILES IN THE BUCKET!!!
     */
    @Test
    public void testBinaryManagerGC() throws Exception {
        Binary binary = binaryManager.getBinary(CONTENT_MD5);
        assertTrue(binary instanceof LazyBinary);

        // store binary
        byte[] bytes = CONTENT.getBytes("UTF-8");
        binary = binaryManager.getBinary(Blobs.createBlob(CONTENT));
        assertNotNull(binary);
        assertEquals(Collections.singleton(CONTENT_MD5), listObjects());

        // get binary
        binary = binaryManager.getBinary(CONTENT_MD5);
        assertNotNull(binary);
        assertEquals(CONTENT, toString(binary.getStream()));

        // another binary we'll GC
        binaryManager.getBinary(Blobs.createBlob(CONTENT2));

        // another binary we'll keep
        binaryManager.getBinary(Blobs.createBlob(CONTENT3));

        assertEquals(new HashSet<>(Arrays.asList(CONTENT_MD5, CONTENT2_MD5, CONTENT3_MD5)), listObjects());

        // GC in non-delete mode
        BinaryGarbageCollector gc = binaryManager.getGarbageCollector();
        assertFalse(gc.isInProgress());
        gc.start();
        assertTrue(gc.isInProgress());
        gc.mark(CONTENT_MD5);
        gc.mark(CONTENT3_MD5);
        assertTrue(gc.isInProgress());
        gc.stop(false);
        assertFalse(gc.isInProgress());
        BinaryManagerStatus status = gc.getStatus();
        assertEquals(2, status.numBinaries);
        if (isStorageSizeSameAsOriginalSize()) {
            assertEquals(bytes.length + CONTENT3.length(), status.sizeBinaries);
        }
        assertEquals(1, status.numBinariesGC);
        if (isStorageSizeSameAsOriginalSize()) {
            assertEquals(CONTENT2.length(), status.sizeBinariesGC);
        }
        assertEquals(new HashSet<>(Arrays.asList(CONTENT_MD5, CONTENT2_MD5, CONTENT3_MD5)), listObjects());

        // real GC
        gc = binaryManager.getGarbageCollector();
        gc.start();
        gc.mark(CONTENT_MD5);
        gc.mark(CONTENT3_MD5);
        gc.stop(true);
        status = gc.getStatus();
        assertEquals(2, status.numBinaries);
        if (isStorageSizeSameAsOriginalSize()) {
            assertEquals(bytes.length + CONTENT3.length(), status.sizeBinaries);
        }
        assertEquals(1, status.numBinariesGC);
        if (isStorageSizeSameAsOriginalSize()) {
            assertEquals(CONTENT2.length(), status.sizeBinariesGC);
        }
        assertEquals(new HashSet<>(Arrays.asList(CONTENT_MD5, CONTENT3_MD5)), listObjects());

        // another GC after not marking content3
        gc = binaryManager.getGarbageCollector();
        gc.start();
        gc.mark(CONTENT_MD5);
        gc.stop(true);
        status = gc.getStatus();
        assertEquals(1, status.numBinaries);
        if (isStorageSizeSameAsOriginalSize()) {
            assertEquals(bytes.length, status.sizeBinaries);
        }
        assertEquals(1, status.numBinariesGC);
        if (isStorageSizeSameAsOriginalSize()) {
            assertEquals(CONTENT3.length(), status.sizeBinariesGC);
        }
        assertEquals(Collections.singleton(CONTENT_MD5), listObjects());
    }

    protected static String toString(InputStream stream) throws IOException {
        return IOUtils.toString(stream, "UTF-8");
    }

}
