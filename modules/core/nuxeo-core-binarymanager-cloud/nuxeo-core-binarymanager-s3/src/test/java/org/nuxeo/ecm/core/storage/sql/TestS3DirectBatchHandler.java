/*
 * (C) Copyright 2018-2020 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.storage.sql;

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
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.server.jaxrs.batch.Batch;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchHandler;
import org.nuxeo.ecm.automation.server.jaxrs.batch.BatchManager;
import org.nuxeo.ecm.automation.server.jaxrs.batch.handler.BatchFileInfo;
import org.nuxeo.ecm.automation.test.AutomationServerFeature;
import org.nuxeo.ecm.blob.s3.S3BlobProvider;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.SdkBaseException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;

/**
 * Tests S3DirectBatchHandler.
 *
 * @since 10.2
 */
@RunWith(FeaturesRunner.class)
@Features(AutomationServerFeature.class)
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3")
public class TestS3DirectBatchHandler {

    public static final int MULTIPART_THRESHOLD = 5 * 1024 * 1024; // 5MB AWS minimum value

    public static final String S3DIRECT_PREFIX = SYSTEM_PROPERTY_PREFIX + ".transient.";

    protected static String envId;

    protected static String envSecret;

    protected static String envToken;

    @Inject
    public BatchManager batchManager;

    @BeforeClass
    public static void beforeClass() {
        envId = StringUtils.defaultIfBlank(System.getenv(SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR),
                System.getenv(SDKGlobalConfiguration.ALTERNATE_ACCESS_KEY_ENV_VAR));
        envSecret = StringUtils.defaultIfBlank(System.getenv(SDKGlobalConfiguration.SECRET_KEY_ENV_VAR),
                System.getenv(SDKGlobalConfiguration.ALTERNATE_SECRET_KEY_ENV_VAR));
        envToken = StringUtils.defaultIfBlank(System.getenv(SDKGlobalConfiguration.AWS_SESSION_TOKEN_ENV_VAR), "");
        assumeTrue("AWS Credentials not set in the environment variables", StringUtils.isNoneBlank(envId, envSecret));
        System.setProperty(S3DIRECT_PREFIX + AWS_ID_PROPERTY, envId);
        System.setProperty(S3DIRECT_PREFIX + AWS_SECRET_PROPERTY, envSecret);
        System.setProperty(S3DIRECT_PREFIX + AWS_SESSION_TOKEN_PROPERTY, envToken);
        System.setProperty(S3DIRECT_PREFIX + BUCKET_NAME_PROPERTY, "nuxeo-s3-directupload-transient");
        System.setProperty(SYSTEM_PROPERTY_PREFIX + "." + BUCKET_NAME_PROPERTY, "nuxeo-s3-directupload");
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-s3directupload-contrib.xml")
    public void testFails() throws SdkBaseException, InterruptedException {
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
    @Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-s3directupload-contrib.xml")
    @Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-s3directupload-fail-contrib.xml")
    public void testFailsOnCopyToTransientStore() throws SdkBaseException, InterruptedException {
        try {
            test("s3fail", 1024);
            fail("should fail on putBlobs");
        } catch (NuxeoException e) {
            assertEquals("putBlobs failed", e.getMessage());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-s3directupload-contrib.xml")
    public void testWithoutMultipart() throws SdkBaseException, InterruptedException {
        test("s3", 1024);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-s3directupload-contrib.xml")
    public void testClientSideMultipartUpload() throws SdkBaseException, InterruptedException {
        // MULTIPART_THRESHOLD is the limit for the client-side multipart upload
        // MULTIPART_THRESHOLD + 1 is the limit for the server-side multipart copy
        test("s3", MULTIPART_THRESHOLD + 1);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-s3directupload-contrib.xml")
    public void testServerSideMultipartCopy() throws SdkBaseException, InterruptedException {
        // MULTIPART_THRESHOLD is the limit for the client-side multipart upload
        // MULTIPART_THRESHOLD + 1 is the limit for the server-side multipart copy
        test("s3", MULTIPART_THRESHOLD + 2);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-s3directupload-contrib.xml")
    public void test20MB() throws SdkBaseException, InterruptedException {
        test("s3", 20 * 1024 * 1024);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-s3directupload-contrib.xml")
    public void testTokenRenewal() throws SdkBaseException, InterruptedException {
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
    @Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-s3directupload-contrib.xml")
    public void testTokenRenewalNullBatchId() throws InterruptedException {
        // Test that refreshing tokens does not work for an invalid batch ID.
        S3DirectBatchHandler handler = (S3DirectBatchHandler) batchManager.getHandler("s3");
        try {
            handler.refreshToken(null);
            fail("should not allow null batch ID");
        } catch (NullPointerException e) {
            assertTrue(e.getMessage().contains("required batch ID"));
        }
    }

    public void test(String handlerName, int size) throws SdkBaseException, InterruptedException {
        // generate unique key and and random content of give size
        String key = "key" + System.nanoTime();
        String name = "name" + System.nanoTime();
        byte[] content = generateRandomBytes(size);

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

        // create priviledged client
        properties.put(S3DirectBatchHandler.INFO_AWS_SECRET_KEY_ID, envId);
        properties.put(S3DirectBatchHandler.INFO_AWS_SECRET_ACCESS_KEY, envSecret);
        properties.put(S3DirectBatchHandler.INFO_AWS_SESSION_TOKEN, envToken);
        AmazonS3 priviledgedS3Client = createS3Client(properties);

        // check the initial upload has been succesfull
        assertNotNull(priviledgedS3Client.getObject(bucketName, prefixedKey));
        BatchFileInfo info = new BatchFileInfo(prefixedKey, name, "text/plain", content.length, null);
        assertTrue(handler.completeUpload(batch.getKey(), key, info));

        // check the object has been renamed to its etag
        try {
            priviledgedS3Client.getObject(bucketName, prefixedKey);
            fail("should throw 404");
        } catch (AmazonS3Exception e) {
            assertTrue(e.getMessage().contains("404"));
        }

        ManagedBlob managedBlob = (ManagedBlob) handler.getBatch(batch.getKey()).getBlob(key);
        BlobProvider blobProvider = Framework.getService(BlobManager.class)
                                             .getBlobProvider(managedBlob.getProviderId());

        // check the s3 object has been renamed to correct etag
        String managedBucketName;
        String managedBucketPrefix;
        if (blobProvider instanceof S3BinaryManager) {
            S3BinaryManager s3BinaryManager = (S3BinaryManager) blobProvider;
            managedBucketName = s3BinaryManager.getBucketName();
            managedBucketPrefix = s3BinaryManager.getBucketPrefix();
        } else { // S3BlobProvider
            S3BlobProvider s3BlobProvider = (S3BlobProvider) blobProvider;
            managedBucketName = s3BlobProvider.config.bucketName;
            managedBucketPrefix = s3BlobProvider.config.bucketPrefix;
        }
        assertNotNull(
                priviledgedS3Client.getObject(managedBucketName, managedBucketPrefix + managedBlob.getDigest()));

        // cleanup
        removeAllFiles(priviledgedS3Client, bucketName, bucketPrefix);
        removeAllFiles(priviledgedS3Client, managedBucketName, managedBucketPrefix);
    }

    protected void removeAllFiles(AmazonS3 s3, String bucketName, String prefix) {
        ListObjectsRequest listObjectsRequest = new ListObjectsRequest().withBucketName(bucketName).withPrefix(prefix);
        ObjectListing objectListing = s3.listObjects(listObjectsRequest);
        while (true) {
            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
                s3.deleteObject(bucketName, objectSummary.getKey());
            }
            if (objectListing.isTruncated()) {
                objectListing = s3.listNextBatchOfObjects(objectListing);
            } else {
                break;
            }
        }
    }

    protected void clientSideUpload(Map<String, Object> properties, byte[] content, String key)
            throws AmazonServiceException, AmazonClientException, InterruptedException {

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

        tm.upload(new PutObjectRequest(bucket, prefix + key, new ByteArrayInputStream(content), metadata))
        .waitForUploadResult();
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
