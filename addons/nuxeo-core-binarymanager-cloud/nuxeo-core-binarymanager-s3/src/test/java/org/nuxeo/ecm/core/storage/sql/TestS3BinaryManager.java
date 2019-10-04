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
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.binary.Binary;
import org.nuxeo.ecm.core.blob.binary.LazyBinary;
import org.nuxeo.ecm.blob.AbstractTestCloudBinaryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * ***** NOTE THAT THE TESTS WILL REMOVE ALL FILES IN THE BUCKET!!! *****
 * <p>
 * This test must be run with at least the following system properties set:
 * <ul>
 * <li>nuxeo.s3storage.bucket</li>
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
        PROPERTIES = new HashMap<>();
        // this also checks in system properties for the configuration
        String bucketName = Framework.getProperty("nuxeo.s3storage.bucket");
        if (bucketName == null) {
            // NOTE THAT THE TESTS WILL REMOVE ALL FILES IN THE BUCKET!!!
            // ********** NEVER COMMIT THE SECRET KEYS !!! **********
            bucketName = "CHANGETHIS";
            String region = "CHANGETHIS";
            String idKey = "CHANGETHIS";
            String secretKey = "CHANGETHIS";
            // ********** NEVER COMMIT THE SECRET KEYS !!! **********
            PROPERTIES.put(S3BinaryManager.BUCKET_NAME_PROPERTY, bucketName);
            PROPERTIES.put(S3BinaryManager.BUCKET_PREFIX_PROPERTY, "testfolder/");
            PROPERTIES.put(S3BinaryManager.AWS_ID_PROPERTY, idKey);
            PROPERTIES.put(S3BinaryManager.AWS_SECRET_PROPERTY , secretKey);
            PROPERTIES.put(S3BinaryManager.BUCKET_REGION_PROPERTY, region);
            boolean useKeyStore = false;
            if (useKeyStore) {
                // keytool -genkeypair -keystore /tmp/keystore.ks -alias unittest -storepass unittest -keypass unittest
                // -dname "CN=AWS S3 Key, O=example, DC=com" -keyalg RSA
                String keyStoreFile = "/tmp/keystore.ks";
                String keyStorePassword = "unittest";
                String privKeyAlias = "unittest";
                String privKeyPassword = "unittest";
                PROPERTIES.put(S3BinaryManager.KEYSTORE_FILE_PROPERTY , keyStoreFile);
                PROPERTIES.put(S3BinaryManager.KEYSTORE_PASS_PROPERTY , keyStorePassword);
                PROPERTIES.put(S3BinaryManager.PRIVKEY_ALIAS_PROPERTY , privKeyAlias);
                PROPERTIES.put(S3BinaryManager.PRIVKEY_PASS_PROPERTY , privKeyPassword);
            }
        }
        boolean disabled = bucketName.equals("CHANGETHIS");
        assumeTrue("No AWS credentials configured", !disabled);
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
    public void tearDown() throws Exception {
        removeObjects();
    }

    @Override
    public boolean isStorageSizeSameAsOriginalSize() {
        return !binaryManager.isEncrypted;
    }

    @Test
    public void testS3BinaryManagerOverwrite() throws Exception {
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
    public void testS3MaxConnections() throws Exception {
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

    protected void doTestS3MaxConnections() throws Exception {
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
        o.close();
    }

    @Override
    @Test
    public void testStoreFile() throws Exception {
        // Run normal test
        super.testStoreFile();
        // Run corruption test
        String key = binaryManager.bucketNamePrefix + CONTENT_MD5;
        binaryManager.amazonS3.putObject(binaryManager.bucketName, key, "Georges Abitbol");
        binaryManager.fileCache.clear();
        Boolean exceptionOccured = false;
        try {
            binaryManager.getBinary(CONTENT_MD5).getStream();
        } catch (RuntimeException e) {
            // Should not be wrapped in a RuntimeException as it declare the IOException
            if (e.getCause() instanceof IOException) {
                exceptionOccured = true;
            }
        }
        assertTrue("IOException should occured as content is corrupted", exceptionOccured);
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
    public void testBinaryManagerGC() throws Exception {
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
            binaryManager.amazonS3.putObject(binaryManager.bucketName, digest, in, metadata);
        }
        assertEquals(Collections.singleton(digest), listAllObjects());

        // run base test with the bucket name prefix
        super.testBinaryManagerGC();

        // check that the extra file is still here
        Set<String> res = listAllObjects();
        assertTrue(res.contains(digest));
    }

    @Test
    public void testDifferentDigestAlgorithm() throws Exception {
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
        S3BinaryManager binaryManager2 = new S3BinaryManager();
        binaryManager2.initialize("repo2", properties2);
        return binaryManager2;
    }

    /** Other binary manager with a different digest algorithm. */
    protected S3BinaryManager getBinaryManager3() throws IOException {
        Map<String, String> properties3 = new HashMap<>(PROPERTIES);
        String prefix = properties3.get(S3BinaryManager.BUCKET_PREFIX_PROPERTY);
        properties3.put(S3BinaryManager.BUCKET_PREFIX_PROPERTY, prefix + "withsha/");
        properties3.put(S3BinaryManager.DIGEST_ALGORITHM_PROPERTY, "SHA-256");
        S3BinaryManager binaryManager3 = new S3BinaryManager();
        binaryManager3.initialize("repo3", properties3);
        return binaryManager3;
    }

}
