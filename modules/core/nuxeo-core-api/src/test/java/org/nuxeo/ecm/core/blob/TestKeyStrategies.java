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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.output.NullOutputStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.ByteSize;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.blob.KeyStrategy.WriteObserver;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestKeyStrategies {

    protected static final String ABC = "abc";

    protected static final String ABC_MD5 = "900150983cd24fb0d6963f7d28e17f72";

    @Test
    public void testKeyStrategyDigest() throws IOException {
        KeyStrategy ks = new KeyStrategyDigest("MD5");

        assertTrue(ks.useDeDuplication());
        assertNull(ks.getDigestFromKey("deadbeef"));

        // write observer / key computer
        BlobContext blobContext = new BlobContext(null, "foo", "bar");
        BlobWriteContext c = ks.getBlobWriteContext(blobContext);
        WriteObserver writeObserver = c.writeObserver;
        @SuppressWarnings("resource")
        OutputStream out = writeObserver.wrap(NullOutputStream.INSTANCE);
        out.write(ABC.getBytes(UTF_8), 0, 3);
        out.flush();
        writeObserver.done();
        assertEquals(ABC_MD5, c.getKey());

        // equals
        KeyStrategy ks2 = new KeyStrategyDigest("MD5");
        KeyStrategy ks3 = new KeyStrategyDigest("SHA-256");
        assertEquals(ks, ks);
        assertEquals(ks, ks2);
        assertNotEquals(ks, ks3);
        assertNotEquals(ks, "foobar");
    }

    @Test
    public void testKeyStrategyDigestIsValidDigest() {
        KeyStrategyDigest ks = new KeyStrategyDigest("MD5");
        assertEquals("MD5", ks.digestAlgorithm);
        assertEquals("[0-9a-f]{32}", ks.digestPattern.toString());
        assertFalse(ks.isValidDigest("dead"));
        assertFalse(ks.isValidDigest("d41d8cd98f00b204e9800998ecf8427e-0"));
        assertTrue(ks.isValidDigest("d41d8cd98f00b204e9800998ecf8427e"));
        ks = new KeyStrategyDigest("SHA-256");
        assertEquals("SHA-256", ks.digestAlgorithm);
        assertEquals("[0-9a-f]{64}", ks.digestPattern.toString());
        assertFalse(ks.isValidDigest("dead"));
        assertTrue(ks.isValidDigest("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"));
        ks = new KeyStrategyDigest("SHA-512");
        assertEquals("SHA-512", ks.digestAlgorithm);
        assertEquals("[0-9a-f]{128}", ks.digestPattern.toString());
        assertFalse(ks.isValidDigest("dead"));
        assertTrue(ks.isValidDigest(
                "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e"));
    }

    @Test
    public void testKeyStrategyDocId() {
        KeyStrategy ks = KeyStrategyDocId.instance();

        assertFalse(ks.useDeDuplication());
        assertNull(ks.getDigestFromKey("deadbeef"));

        // write observer / key computer
        BlobContext blobContext = new BlobContext(null, "docid1", "content");
        BlobWriteContext c = ks.getBlobWriteContext(blobContext);
        assertNull(c.writeObserver);
        assertEquals("docid1", c.getKey());

        // equals
        KeyStrategy ks2 = new KeyStrategyDocId();
        assertEquals(ks, ks);
        assertEquals(ks, ks2);
        assertNotEquals(ks, "foobar");

        blobContext = new BlobContext(null, "docid1", "files/0/file");
        c = ks.getBlobWriteContext(blobContext);
        assertNull(c.writeObserver);
        assertEquals("docid1-files-0-file", c.getKey());

        blobContext = new BlobContext(null, "docid1", "files:files/0/file");
        c = ks.getBlobWriteContext(blobContext);
        assertNull(c.writeObserver);
        assertEquals("docid1-files_files-0-file", c.getKey());
    }

    @Test
    public void testKeyStrategyDigestThresholdBelowThreshold() throws IOException {
        var ks = new KeyStrategyDigest("MD5", ByteSize.ofKibibytes(1));

        assertTrue(ks.useDeDuplication());

        // blob below threshold -> digest key via write observer
        var blob = Blobs.createBlob(ABC);
        var blobContext = new BlobContext(blob, "doc1", "content");
        var c = ks.getBlobWriteContext(blobContext);
        assertNotNull(c.writeObserver);
        @SuppressWarnings("resource")
        OutputStream out = c.writeObserver.wrap(NullOutputStream.INSTANCE);
        out.write(ABC.getBytes(UTF_8), 0, 3);
        out.flush();
        c.writeObserver.done();
        assertEquals(ABC_MD5, c.getKey());
    }

    @Test
    public void testKeyStrategyDigestThresholdAboveThreshold() {
        var ks = new KeyStrategyDigest("MD5", ByteSize.ofBytes(2));

        // blob above threshold -> UUIDv7 key, no write observer
        var blob = Blobs.createBlob(ABC); // 3 bytes > 2
        var blobContext = new BlobContext(blob, "doc1", "content");
        var c = ks.getBlobWriteContext(blobContext);
        assertNull(c.writeObserver);
        String key = c.getKey();
        assertNotNull(key);
        assertTrue("Expected UUIDv7 key but got: " + key, KeyStrategyDigest.isUUIDv7(key));
    }

    @Test
    public void testKeyStrategyDigestThresholdUnknownSize() throws IOException {
        var ks = new KeyStrategyDigest("MD5", ByteSize.ofBytes(2));

        // blob with unknown size (-1) -> falls back to digest strategy even though content is above threshold
        var blob = new StringBlob(ABC) {
            @Override
            public long getLength() {
                return -1;
            }
        };
        var blobContext = new BlobContext(blob, "doc1", "content");
        var c = ks.getBlobWriteContext(blobContext);
        // digest strategy provides a write observer (falls back to digest since size is unknown)
        assertNotNull(c.writeObserver);
    }

    @Test
    public void testKeyStrategyDigestThresholdIsValidKey() {
        var ks = new KeyStrategyDigest("MD5", ByteSize.ofKibibytes(1));

        // MD5 digest key is valid
        assertTrue(ks.isValidKey(ABC_MD5));

        // UUIDv7 key is valid
        assertTrue(ks.isValidKey("01936e40-d6e0-7a3e-a2b1-c4d5e6f7a8b9"));

        // random string is not valid
        assertFalse(ks.isValidKey("deadbeef"));

        // temp key is not valid
        assertFalse(ks.isValidKey("1234567890123456789-0"));

        // UUIDv4 (KeyStrategyDocId) is not valid
        assertFalse(ks.isValidKey("12051767-a926-425c-a7e0-dcdf02c0bc04"));
    }

    @Test
    public void testKeyStrategyDigestThresholdGetDigestFromKey() {
        var ks = new KeyStrategyDigest("MD5", ByteSize.ofKibibytes(1));

        // digest key returns digest
        assertEquals(ABC_MD5, ks.getDigestFromKey(ABC_MD5));

        // UUID key returns null (no content-based digest)
        assertNull(ks.getDigestFromKey("01936e40-d6e0-7a3e-a2b1-c4d5e6f7a8b9"));
    }

    @Test
    public void testKeyStrategyDigestThresholdEquals() {
        var ks1 = new KeyStrategyDigest("MD5", ByteSize.ofKibibytes(1));
        var ks2 = new KeyStrategyDigest("MD5", ByteSize.ofKibibytes(1));
        var ks3 = new KeyStrategyDigest("MD5", ByteSize.ofKibibytes(2));
        var ks4 = new KeyStrategyDigest("SHA-256", ByteSize.ofKibibytes(1));
        var ks5 = new KeyStrategyDigest("MD5"); // no threshold

        assertEquals(ks1, ks2);
        assertNotEquals(ks1, ks3); // different maxSize
        assertNotEquals(ks1, ks4); // different algorithm
        assertNotEquals(ks1, ks5); // bounded vs unlimited maxSize
        assertNotEquals(ks1, "foobar");
    }

    @Test
    public void testIsUUIDv7() {
        // valid UUIDv7 (version=7, variant=2)
        assertTrue(KeyStrategyDigest.isUUIDv7("01936e40-d6e0-7a3e-a2b1-c4d5e6f7a8b9"));

        // UUIDv4 (version=4, variant=2) should not match
        assertFalse(KeyStrategyDigest.isUUIDv7("12051767-a926-425c-a7e0-dcdf02c0bc04"));

        // not a UUID
        assertFalse(KeyStrategyDigest.isUUIDv7("not-a-uuid"));
        assertFalse(KeyStrategyDigest.isUUIDv7(null));

        // MD5 digest
        assertFalse(KeyStrategyDigest.isUUIDv7(ABC_MD5));
    }

}
