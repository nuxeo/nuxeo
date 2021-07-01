/*
 * (C) Copyright 2011-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.blob.AbstractCloudBinaryManager;
import org.nuxeo.ecm.blob.s3.S3TestHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.impl.blob.ByteArrayBlob;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.binary.Binary;
import org.nuxeo.ecm.core.blob.binary.LazyBinary;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

/**
 * ***** NOTE THAT THE TESTS WILL REMOVE ALL FILES IN THE BUCKET!!! *****
 * <p>
 * This test must be run with at least the following system properties set:
 * <ul>
 * <li>nuxeo.s3storage.awsid (or AWS_ACCESS_KEY_ID environment variable)</li>
 * <li>nuxeo.s3storage.awssecret (or AWS_SECRET_ACCESS_KEY environment variable)</li>
 * </ul>
 * <p>
 * ***** NOTE THAT THE TESTS WILL REMOVE ALL FILES IN THE BUCKET!!! *****
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, MockitoFeature.class })
public class TestS3BinaryManager extends AbstractS3BinaryTest<S3BinaryManager> {

    @Mock
    @RuntimeService
    protected BlobManager blobManager;

    protected S3BinaryManager binaryManager2;

    protected S3BinaryManager binaryManager3;

    @BeforeClass
    public static void beforeClass() {
        PROPERTIES = S3TestHelper.getProperties();
    }

    @Before
    public void doBefore() throws IOException {
        binaryManager2 = getBinaryManager2();
        binaryManager3 = getBinaryManager3();
        when(blobManager.getBlobProvider("repo")).thenReturn(binaryManager);
        when(blobManager.getBlobProvider("repo2")).thenReturn(binaryManager2);
        when(blobManager.getBlobProvider("repo3")).thenReturn(binaryManager3);
    }

    @After
    public void tearDown() throws IOException {
        removeObjects();
    }

    @Override
    public boolean isStorageSizeSameAsOriginalSize() {
        return !binaryManager.isEncrypted;
    }

    @Test
    public void testS3BinaryManagerOverwrite() throws IOException {
        // store binary
        Binary binary = binaryManager.getBinary(Blobs.createBlob(CONTENT));
        assertNotNull(binary);
        assertEquals(CONTENT, toString(binary.getStream()));
        assertNull(Framework.getProperty("cachedBinary"));

        // store the same content again
        Binary binary2 = binaryManager.getBinary(Blobs.createBlob(CONTENT));
        assertNotNull(binary2);
        assertEquals(CONTENT, toString(binary.getStream()));
        // check that S3 bucked was not called for no valid reason
        assertEquals(binary2.getDigest(), Framework.getProperty("cachedBinary"));
    }

    @Test
    public void testS3MaxConnections() throws IOException {
        PROPERTIES.put(S3BinaryManager.CONNECTION_MAX_PROPERTY, "1");
        PROPERTIES.put(S3BinaryManager.CONNECTION_RETRY_PROPERTY, "0");
        PROPERTIES.put(S3BinaryManager.CONNECTION_TIMEOUT_PROPERTY, "5000"); // 5s
        try {
            binaryManager = new S3BinaryManager();
            binaryManager.initialize("repo", PROPERTIES);
            doTestS3MaxConnections();
        } finally {
            PROPERTIES.remove(S3BinaryManager.CONNECTION_MAX_PROPERTY);
            PROPERTIES.remove(S3BinaryManager.CONNECTION_RETRY_PROPERTY);
            PROPERTIES.remove(S3BinaryManager.CONNECTION_TIMEOUT_PROPERTY);
        }
    }

    protected void doTestS3MaxConnections() throws IOException {
        // store binary
        binaryManager.getBinary(Blobs.createBlob(CONTENT));

        String key = binaryManager.bucketNamePrefix + CONTENT_MD5;
        S3Object o = binaryManager.amazonS3.getObject(binaryManager.bucketName, key);
        try {
            binaryManager.amazonS3.getObject(binaryManager.bucketName, key);
            fail("Should throw AmazonClientException");
        } catch (AmazonClientException e) {
            Throwable c = e.getCause();
            assertTrue(c.getClass().getName(), c instanceof ConnectionPoolTimeoutException);
        }
        // abort reading stream to avoid WARN about incomplete read since AWS SDK 1.11.99
        o.getObjectContent().abort();
        o.close();
    }

    @Test
    public void testFixupDigestOnWrite() throws IOException {
        // write blob with no digest
        Blob blob = Blobs.createBlob(CONTENT);
        String key = binaryManager.writeBlob(blob);
        assertEquals(CONTENT_MD5, key);
        // digest was fixed up
        assertEquals("MD5", blob.getDigestAlgorithm());
        assertEquals(CONTENT_MD5, blob.getDigest());

        // write blob with temporary digest
        blob = Blobs.createBlob(CONTENT);
        blob.setDigest("notadigest-0");
        key = binaryManager.writeBlob(blob);
        assertEquals(CONTENT_MD5, key);
        // digest was fixed up
        assertEquals("MD5", blob.getDigestAlgorithm());
        assertEquals(CONTENT_MD5, blob.getDigest());

        // write blob with custom digest
        String digest = "rL0Y20zC+Fzt72VPzMSk2A==";
        blob = Blobs.createBlob(CONTENT);
        blob.setDigest(digest);
        key = binaryManager.writeBlob(blob);
        assertEquals(CONTENT_MD5, key);
        // digest was not fixed up
        assertNull(blob.getDigestAlgorithm());
        assertEquals(digest, blob.getDigest());
    }

    @Test
    public void testFixupDigestOnRead() throws IOException {
        // write blob
        String key = binaryManager.writeBlob(Blobs.createBlob(CONTENT));
        assertEquals(CONTENT_MD5, key);

        // read blob
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = CONTENT_MD5;
        Blob blob = binaryManager.readBlob(blobInfo);
        // digest was fixed up
        assertEquals("MD5", blob.getDigestAlgorithm());
        assertEquals(CONTENT_MD5, blob.getDigest());

        // read blob with temporary digest in database
        blobInfo.digest = "notadigest-0";
        blob = binaryManager.readBlob(blobInfo);
        // digest was fixed up
        assertEquals("MD5", blob.getDigestAlgorithm());
        assertEquals(CONTENT_MD5, blob.getDigest());

        // read blob with custom digest in database
        String digest = "rL0Y20zC+Fzt72VPzMSk2A==";
        blobInfo.digest = digest;
        blob = binaryManager.readBlob(blobInfo);
        // digest was not fixed up
        assertNull(blob.getDigestAlgorithm());
        assertEquals(digest, blob.getDigest());
    }

    @Test
    public void testCopy() throws IOException {
        // put blob in first binary manager
        Binary binary = binaryManager.getBinary(Blobs.createBlob(CONTENT));
        binary = binaryManager.getBinary(CONTENT_MD5);
        try (InputStream stream = binary.getStream()) {
            assertNotNull(stream);
            assertEquals(CONTENT, toString(stream));
        }
        // check it's not visible in second binary manager
        Binary binary2 = binaryManager2.getBinary(CONTENT_MD5);
        try (InputStream stream2 = binary2.getStream()) {
            assertNull(stream2);
        }
        // do copy into second binary manager
        BlobInfo blobInfo = new BlobInfo();
        blobInfo.key = CONTENT_MD5;
        Blob blob = binaryManager.readBlob(blobInfo);
        binaryManager2.writeBlob(blob);
        // check it's now been copied into second binary manager
        binary2 = binaryManager2.getBinary(CONTENT_MD5);
        try (InputStream stream2 = binary2.getStream()) {
            assertNotNull(stream2);
            assertEquals(CONTENT, toString(stream2));
        }
        // and it's still in the first binary manager
        // disable cache to fetch the binary from the S3 bucket
        binaryManager.fileCache.clear();
        binary = binaryManager.getBinary(CONTENT_MD5);
        try (InputStream stream = binary.getStream()) {
            assertNotNull(stream);
            assertEquals(CONTENT, toString(stream));
        }
    }

    @Override
    @Test
    public void testBinaryManagerGC() throws IOException {
        if (binaryManager.bucketNamePrefix.isEmpty()) {
            // no additional test if no bucket name prefix
            super.testBinaryManagerGC();
            return;
        }

        // create a md5-looking extra file at the root
        String digest = "12345678901234567890123456789012";
        try (InputStream in = new ByteArrayInputStream(new byte[] { '0' })) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(1);
            if (binaryManager.useServerSideEncryption) {
                metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
            }
            binaryManager.amazonS3.putObject(binaryManager.bucketName, digest, in, metadata);
        }
        // create a md5-looking extra file in a "subdirectory" of the bucket prefix
        String digest2 = binaryManager.bucketNamePrefix + "subfolder/12345678901234567890123456789999";
        try (InputStream in = new ByteArrayInputStream(new byte[] { '0' })) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(1);
            if (binaryManager.useServerSideEncryption) {
                metadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
            }
            binaryManager.amazonS3.putObject(binaryManager.bucketName, digest2, in, metadata);
        }
        // check that the files are here
        assertEquals(new HashSet<>(Arrays.asList(digest, digest2)), listAllObjects());

        // run base test with the bucket name prefix
        super.testBinaryManagerGC();

        // check that the extra files are still here
        Set<String> res = listAllObjects();
        assertTrue(res.contains(digest));
        assertTrue(res.contains(digest2));
    }

    @Test
    public void test1KB() throws IOException {
        test(1024);
    }

    @Test
    public void test1MB() throws IOException {
        test(1024 * 1024);
    }

    @Test
    public void test5MB() throws IOException {
        test(5 * 1024 * 1024);
    }

    @Test
    public void test20MB() throws IOException {
        test(20 * 1024 * 1024);
    }

    protected void test(int size) throws IOException {
        Blob blob = new ByteArrayBlob(generateRandomBytes(size));
        binaryManager.writeBlob(blob);
    }

    protected byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        return bytes;
    }

    @Test
    public void testDifferentDigestAlgorithm() throws IOException {
        Binary binary = binaryManager3.getBinary(CONTENT4_SHA256);
        assertTrue(binary instanceof LazyBinary);
        if (binary.getStream() != null) {
            // the tests have already been run
            // make sure we delete it from the bucket first
            removeObject(CONTENT4_SHA256);
            binaryManager3.fileCache.clear();
        }

        // store binary
        binary = binaryManager3.getBinary(Blobs.createBlob(CONTENT4));
        assertNotNull(binary);

        // clear cache
        binaryManager3.fileCache.clear();

        // get binary
        binary = binaryManager3.getBinary(CONTENT4_SHA256);
        assertNotNull(binary);
        assertTrue(binary instanceof LazyBinary);
        assertEquals(CONTENT4, toString(binary.getStream()));
    }

    @Override
    protected S3BinaryManager getBinaryManager() throws IOException {
        S3BinaryManager binaryManager = new S3BinaryManager();
        binaryManager.initialize("repo", PROPERTIES);
        return binaryManager;
    }

    /** Other binary manager storing in a subfolder of the main one. */
    protected S3BinaryManager getBinaryManager2() throws IOException {
        Map<String, String> properties2 = new HashMap<>(PROPERTIES);
        String prefix = properties2.get(S3BinaryManager.BUCKET_PREFIX_PROPERTY);
        properties2.put(S3BinaryManager.BUCKET_PREFIX_PROPERTY, prefix + "transient/");
        S3BinaryManager binaryManager = new S3BinaryManager();
        binaryManager.initialize("repo2", properties2);
        return binaryManager;
    }

    /** Other binary manager with a different digest algorithm. */
    protected S3BinaryManager getBinaryManager3() throws IOException {
        Map<String, String> properties3 = new HashMap<>(PROPERTIES);
        String prefix = properties3.get(S3BinaryManager.BUCKET_PREFIX_PROPERTY);
        properties3.put(S3BinaryManager.BUCKET_PREFIX_PROPERTY, prefix + "withsha/");
        properties3.put(AbstractCloudBinaryManager.DIGEST_ALGORITHM_PROPERTY, "SHA-256");
        S3BinaryManager binaryManager = new S3BinaryManager();
        binaryManager.initialize("repo3", properties3);
        return binaryManager;
    }

}
