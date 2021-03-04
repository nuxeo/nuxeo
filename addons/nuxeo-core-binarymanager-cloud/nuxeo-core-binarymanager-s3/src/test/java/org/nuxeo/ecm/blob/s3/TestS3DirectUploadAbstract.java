/*
 * (C) Copyright 2018-2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 *     Mickaël Schoentgen
 *     Florent Guillaume
 */
package org.nuxeo.ecm.blob.s3;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.AWS_ID_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.AWS_SECRET_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.AWS_SESSION_TOKEN_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.BUCKET_NAME_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.SYSTEM_PROPERTY_PREFIX;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.server.jaxrs.batch.Batch;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchHandler;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.automation.server.jaxrs.batch.handler.BatchFileInfo;
import org.nuxeo.ecm.automation.test.AutomationServerFeature;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.ecm.core.storage.sql.S3BinaryManager;
import org.nuxeo.ecm.core.storage.sql.S3DirectBatchHandler;
import org.nuxeo.ecm.core.transientstore.keyvalueblob.KeyValueBlobTransientStore;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RunnerFeature;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

/**
 * Tests S3DirectBatchHandler.
 *
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features({ TestS3DirectUploadAbstract.SetPropertiesFeature.class, AutomationServerFeature.class })
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3")
public abstract class TestS3DirectUploadAbstract {

    public static final int MULTIPART_THRESHOLD = 5 * 1024 * 1024; // 5MB AWS minimum value

    public static final String S3DIRECT_PREFIX = SYSTEM_PROPERTY_PREFIX + ".transient.";

    protected static String envId;

    protected static String envSecret;

    protected static String envToken;

    @Inject
    public BlobManager blobManager;

    @Inject
    public BatchManager batchManager;

    /*
     * Direct Upload extension points contain variables that need to be set before extension points are read, thus this
     * local Feature.
     */
    public static class SetPropertiesFeature implements RunnerFeature {
        @Override
        public void start(FeaturesRunner runner) {
            setProperties();
        }
    }

    public static void setProperties() {
        Map<String, String> properties = S3TestHelper.getProperties();
        properties.forEach(S3TestHelper::setProperty);

        envId = properties.get(AWS_ID_PROPERTY);
        envSecret = properties.get(AWS_SECRET_PROPERTY);
        envToken = properties.get(AWS_SESSION_TOKEN_PROPERTY);
        String bucketName2 = "nuxeo-test-changeme-2";

        assumeTrue("AWS Credentials not set in the environment variables", StringUtils.isNoneBlank(envId, envSecret));

        // BatchHander config
        System.setProperty(S3DIRECT_PREFIX + AWS_ID_PROPERTY, envId);
        System.setProperty(S3DIRECT_PREFIX + AWS_SECRET_PROPERTY, envSecret);
        System.setProperty(S3DIRECT_PREFIX + AWS_SESSION_TOKEN_PROPERTY, envToken);
        System.setProperty(S3DIRECT_PREFIX + BUCKET_NAME_PROPERTY, bucketName2);
    }

    @After
    public void tearDown() {
        S3DirectBatchHandler handler = (S3DirectBatchHandler) batchManager.getHandler("s3");
        BlobProvider dubp = blobManager.getBlobProvider(handler.blobProviderId);
        clearBlobProvider(dubp);
        KeyValueBlobTransientStore ts = (KeyValueBlobTransientStore) handler.getTransientStore();
        clearBlobProvider(ts.getBlobProvider());
    }

    protected void clearBlobProvider(BlobProvider blobProvider) {
        if (blobProvider instanceof S3BinaryManager) {
            ((S3BinaryManager) blobProvider).clear();
        } else {
            ((BlobStoreBlobProvider) blobProvider).store.clear();
        }
    }

    @Test
    public void testFails() {
        // create and initialize batch
        BatchHandler handler = batchManager.getHandler("s3");
        // complete upload with invalid key
        try {
            handler.completeUpload(null, null, new BatchFileInfo("invalid key", null, null, 10, null));
            fail("should throw 404");
        } catch (AmazonS3Exception e) {
            assertTrue(e.getMessage().contains("404"));
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-s3directupload-fail-contrib.xml")
    public void testFailsOnCopyToTransientStore() {
        try {
            test("s3fail", 1024);
            fail("should fail on putBlobs");
        } catch (NuxeoException e) {
            assertEquals("putBlobs failed", e.getMessage());
        }
    }

    @Test
    public void testSmall() {
        test("s3", 1024);
    }

    @Test
    public void testMultipart() {
        test("s3", MULTIPART_THRESHOLD * 2);
    }

    @Test
    public void testTokenRenewal() {
        // Test that refreshing tokens actually returns back valid and different tokens.
        S3DirectBatchHandler handler = (S3DirectBatchHandler) batchManager.getHandler("s3");
        Batch newBatch = handler.newBatch(null);
        Batch batch = handler.getBatch(newBatch.getKey());

        // Save current details
        Map<String, Object> properties = batch.getProperties();
        String secretKeyId = (String) properties.get(S3DirectBatchHandler.INFO_AWS_SECRET_KEY_ID);
        String secretAccessKey = (String) properties.get(S3DirectBatchHandler.INFO_AWS_SECRET_ACCESS_KEY);
        String sessionToken = (String) properties.get(S3DirectBatchHandler.INFO_AWS_SESSION_TOKEN);
        Long expiration = (Long) properties.get(S3DirectBatchHandler.INFO_EXPIRATION);

        // Renew tokens
        properties = handler.refreshToken(newBatch.getKey());
        String newSecretKeyId = (String) properties.get(S3DirectBatchHandler.INFO_AWS_SECRET_KEY_ID);
        String newSecretAccessKey = (String) properties.get(S3DirectBatchHandler.INFO_AWS_SECRET_ACCESS_KEY);
        String newSessionToken = (String) properties.get(S3DirectBatchHandler.INFO_AWS_SESSION_TOKEN);
        Long newExpiration = (Long) properties.get(S3DirectBatchHandler.INFO_EXPIRATION);

        // Checks
        assertNotEquals(secretKeyId, newSecretKeyId);
        assertNotEquals(secretAccessKey, newSecretAccessKey);
        assertNotEquals(sessionToken, newSessionToken);
        assertTrue(expiration <= newExpiration);
    }

    @Test
    public void testTokenRenewalNullBatchId() {
        // Test that refreshing tokens does not work for an invalid batch ID.
        S3DirectBatchHandler handler = (S3DirectBatchHandler) batchManager.getHandler("s3");
        try {
            handler.refreshToken(null);
            fail("should not allow null batch ID");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().contains("required batch ID"));
        }
    }

    protected void test(String handlerName, int size) {
        test(handlerName, size, true);
        tearDown();
        test(handlerName, size, false);
    }

    protected void test(String handlerName, int size, boolean keyLookingLikeADigest) {
        String key;
        if (keyLookingLikeADigest) {
            key = "01234567890123456789012345678901"; // same size as MD5
        } else {
            key = "key-" + System.nanoTime(); // with "-" to denote temporary digest
        }
        // generate unique key and and random content of give size
        String name = "name" + System.nanoTime();
        byte[] content = generateRandomBytes(size);
        String expectedDigest = DigestUtils.md5Hex(content);

        // create and initialize batch
        S3DirectBatchHandler handler = (S3DirectBatchHandler) batchManager.getHandler(handlerName);
        Batch newBatch = handler.newBatch(null);
        Batch batch = handler.getBatch(newBatch.getKey());

        Map<String, Object> properties = batch.getProperties();
        String bucketName = (String) properties.get(S3DirectBatchHandler.INFO_BUCKET);
        String bucketPrefix = (String) properties.get(S3DirectBatchHandler.INFO_BASE_KEY);

        String prefixedKey = bucketPrefix + key;

        // client side upload
        clientSideUpload(properties, content, key);

        // create privileged client
        properties.put(S3DirectBatchHandler.INFO_AWS_SECRET_KEY_ID, envId);
        properties.put(S3DirectBatchHandler.INFO_AWS_SECRET_ACCESS_KEY, envSecret);
        properties.put(S3DirectBatchHandler.INFO_AWS_SESSION_TOKEN, envToken);
        AmazonS3 priviledgedS3Client = createS3Client(properties);

        // check the initial upload has been successful
        assertNotNull(priviledgedS3Client.getObject(bucketName, prefixedKey));
        BatchFileInfo info = new BatchFileInfo(prefixedKey, name, "text/plain", content.length, null);
        assertTrue(handler.completeUpload(batch.getKey(), key, info));

        // check content
        Blob blob = handler.getBatch(batch.getKey()).getBlob(key);
        try (InputStream stream = blob.getStream()) {
            byte[] bytes = IOUtils.toByteArray(stream);
            assertArrayEquals(content, bytes);
            assertEquals(expectedDigest, blob.getDigest());
            assertEquals("MD5", blob.getDigestAlgorithm());
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    protected void clientSideUpload(Map<String, Object> properties, byte[] content, String key) {

        // upload the content with our arbitrary key
        AmazonS3 s3Client = createS3Client(properties);
        TransferManager tm = TransferManagerBuilder.standard()
                                                   .withMultipartUploadThreshold(MULTIPART_THRESHOLD * 1L)
                                                   .withS3Client(s3Client)
                                                   .build();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        metadata.setContentLength(content.length);

        String bucket = (String) properties.get(S3DirectBatchHandler.INFO_BUCKET);
        String prefix = (String) properties.get(S3DirectBatchHandler.INFO_BASE_KEY);

        Upload upload = tm.upload(new PutObjectRequest(bucket, prefix + key, new ByteArrayInputStream(content), metadata));
        try {
            upload.waitForUploadResult();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(e);
        }
    }

    protected AmazonS3 createS3Client(Map<String, Object> properties) {
        Boolean accelerated = (Boolean) properties.get(S3DirectBatchHandler.INFO_USE_S3_ACCELERATE);
        String awsSessionToken = (String) properties.get(S3DirectBatchHandler.INFO_AWS_SESSION_TOKEN);
        String awsSecretKeyId = (String) properties.get(S3DirectBatchHandler.INFO_AWS_SECRET_KEY_ID);
        String awsSecretAccessKey = (String) properties.get(S3DirectBatchHandler.INFO_AWS_SECRET_ACCESS_KEY);
        AWSCredentials credentials = awsSessionToken == null
                ? new BasicAWSCredentials(awsSecretKeyId, awsSecretAccessKey)
                        : new BasicSessionCredentials(awsSecretKeyId, awsSecretAccessKey, awsSessionToken);
                return AmazonS3ClientBuilder.standard()
                        .withAccelerateModeEnabled(accelerated)
                        .withCredentials(new AWSStaticCredentialsProvider(credentials))
                        .withRegion((String) properties.get(S3DirectBatchHandler.INFO_AWS_REGION))
                        .build();
    }

    protected byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new Random().nextBytes(bytes);
        return bytes;
    }

}
