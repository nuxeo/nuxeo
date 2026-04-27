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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.blob;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.ByteSize;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.blob.BlobStore.OptionalOrUnknown;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.BinaryManagerStatus;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Integration tests for {@link KeyStrategyDigest} with a size threshold and an {@link InMemoryBlobProvider}.
 * <p>
 * The blob provider is configured with {@code digest.maxSize=10}, so blobs of 10 bytes or fewer get MD5 digest keys,
 * and blobs larger than 10 bytes get UUIDv7 keys.
 *
 * @since 2025.19
 */
@RunWith(FeaturesRunner.class)
@Features(BlobManagerFeature.class)
@Deploy("org.nuxeo.ecm.core.api.tests:OSGI-INF/test-blob-provider-inmemory-digest-threshold.xml")
public class TestInMemoryBlobStoreDigestThreshold {

    protected static final String XPATH = "content";

    protected static final String SMALL = "foo"; // 3 bytes, below threshold

    protected static final String SMALL_MD5 = "acbd18db4cc2f85cedef654fccc4a4d8";

    protected static final String LARGE = "this content is larger than 10 bytes"; // 36 bytes, above threshold

    @Rule
    public final TemporaryFolder tempFolder = new TemporaryFolder();

    @Inject
    protected BlobManager blobManager;

    protected BlobProvider bp;

    protected BlobStore bs;

    protected Path tmpFile;

    @Before
    public void setUp() throws IOException {
        bp = blobManager.getBlobProvider("test");
        bs = ((BlobStoreBlobProvider) bp).store;
        tmpFile = tempFolder.newFile("read.tmp").toPath();
    }

    @Test
    public void testKeyStrategyIsDigestThreshold() {
        KeyStrategy ks = bs.getKeyStrategy();
        assertTrue("Expected KeyStrategyDigest with threshold but got: " + ks.getClass().getSimpleName(),
                ks instanceof KeyStrategyDigest);
        var ksd = (KeyStrategyDigest) ks;
        assertEquals(ByteSize.ofBytes(10), ksd.maxSize);
        assertEquals("MD5", ksd.digestAlgorithm);
    }

    @Test
    public void testSmallBlobGetsMD5Key() throws IOException {
        var blob = new StringBlob(SMALL);
        String key = bp.writeBlob(blob);
        assertEquals(SMALL_MD5, key);
        // digest was fixed up
        assertEquals("MD5", blob.getDigestAlgorithm());
        assertEquals(SMALL_MD5, blob.getDigest());
        // content is readable
        assertBlobContent(key, SMALL);
    }

    @Test
    public void testSmallBlobDedup() throws IOException {
        String key1 = bp.writeBlob(new StringBlob(SMALL));
        String key2 = bp.writeBlob(new StringBlob(SMALL));
        assertEquals("Small blobs with same content should have same key (dedup)", key1, key2);
    }

    @Test
    public void testLargeBlobGetsUUIDKey() throws IOException {
        var blob = new StringBlob(LARGE);
        String key = bp.writeBlob(blob);
        assertNotEquals(SMALL_MD5, key);
        assertTrue("Expected UUIDv7 key but got: " + key, KeyStrategyDigest.isUUIDv7(key));
        // no digest algorithm for UUID keys
        assertNull(blob.getDigestAlgorithm());
        // content is readable
        assertBlobContent(key, LARGE);
    }

    @Test
    public void testLargeBlobNoDedup() throws IOException {
        String key1 = bp.writeBlob(new StringBlob(LARGE));
        String key2 = bp.writeBlob(new StringBlob(LARGE));
        assertNotEquals("Large blobs should get unique UUIDv7 keys (no dedup)", key1, key2);
        // both are readable
        assertBlobContent(key1, LARGE);
        assertBlobContent(key2, LARGE);
    }

    @Test
    public void testFixupDigestOnReadSmallBlob() throws IOException {
        // write small blob
        String key = bp.writeBlob(new StringBlob(SMALL));
        assertEquals(SMALL_MD5, key);

        // read back
        var blobInfo = new BlobInfo();
        blobInfo.key = SMALL_MD5;
        var blob = bp.readBlob(blobInfo);
        assertEquals("MD5", blob.getDigestAlgorithm());
        assertEquals(SMALL_MD5, blob.getDigest());
    }

    @Test
    public void testFixupDigestOnReadLargeBlob() throws IOException {
        // write large blob
        String key = bp.writeBlob(new StringBlob(LARGE));
        assertTrue(KeyStrategyDigest.isUUIDv7(key));

        // read back with UUID key
        var blobInfo = new BlobInfo();
        blobInfo.key = key;
        var blob = bp.readBlob(blobInfo);
        // no digest algorithm for UUID keys
        assertNull(blob.getDigestAlgorithm());
    }

    @Test
    public void testGCCollectsBothKeyFormats() throws IOException {
        // store a small blob (MD5 key)
        var smallCtx = new BlobContext(new StringBlob(SMALL), "doc1", XPATH);
        String smallKey = bs.writeBlob(smallCtx);
        assertEquals(SMALL_MD5, smallKey);

        // store a large blob (UUIDv7 key) — this one we'll GC
        var largeCtx = new BlobContext(new StringBlob(LARGE), "doc2", XPATH);
        String largeKey = bs.writeBlob(largeCtx);
        assertTrue(KeyStrategyDigest.isUUIDv7(largeKey));

        // GC: mark only the small blob, large blob should be collected
        BinaryGarbageCollector gc = bs.getBinaryGarbageCollector();
        gc.start();
        gc.mark(smallKey);
        gc.stop(true);
        BinaryManagerStatus status = gc.getStatus();
        assertEquals(1, status.numBinaries); // small blob kept
        assertEquals(1, status.numBinariesGC); // large blob collected

        // small blob still accessible
        assertBlobContent(smallKey, SMALL);
        // large blob gone
        assertFalse(bs.readBlob(largeKey, tmpFile));
    }

    @Test
    public void testGCKeepsMarkedUUIDBlob() throws IOException {
        // store a large blob (UUIDv7 key)
        var largeCtx = new BlobContext(new StringBlob(LARGE), "doc1", XPATH);
        String largeKey = bs.writeBlob(largeCtx);
        assertTrue(KeyStrategyDigest.isUUIDv7(largeKey));

        // GC: mark the UUID blob, should not be collected
        BinaryGarbageCollector gc = bs.getBinaryGarbageCollector();
        gc.start();
        gc.mark(largeKey);
        gc.stop(true);
        BinaryManagerStatus status = gc.getStatus();
        assertEquals(1, status.numBinaries);
        assertEquals(0, status.numBinariesGC);

        // blob still accessible
        assertBlobContent(largeKey, LARGE);
    }

    @Test
    public void testIsValidKeyAcceptsBothFormats() {
        var bsbp = (BlobStoreBlobProvider) bp;
        assertTrue(bsbp.isValidKey(SMALL_MD5));
        assertTrue(bsbp.isValidKey("01936e40-d6e0-7a3e-a2b1-c4d5e6f7a8b9"));
        assertFalse(bsbp.isValidKey("not-a-valid-key"));
        assertFalse(bsbp.isValidKey("1234567890123456789-0"));
    }

    protected void assertBlobContent(String key, String expected) throws IOException {
        assertTrue(bs.readBlob(key, tmpFile));
        assertEquals(expected, new String(Files.readAllBytes(tmpFile), UTF_8));
        OptionalOrUnknown<InputStream> streamOpt = bs.getStream(key);
        if (streamOpt.isKnown()) {
            assertTrue(streamOpt.isPresent());
            try (InputStream stream = streamOpt.get()) {
                assertEquals(expected, IOUtils.toString(stream, UTF_8));
            }
        }
    }

}
