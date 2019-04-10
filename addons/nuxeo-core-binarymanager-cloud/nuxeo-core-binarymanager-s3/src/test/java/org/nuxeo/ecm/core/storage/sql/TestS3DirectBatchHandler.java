/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

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
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.amazonaws.SDKGlobalConfiguration;
import com.amazonaws.SdkBaseException;
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

@RunWith(FeaturesRunner.class)
@Features(AutomationServerFeature.class)
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3")
public class TestS3DirectBatchHandler {

    public static final int MULTIPART_THRESHOLD = 5 * 1024 * 1024; // 5MB AWS minimum value

    public static final String S3DIRECT_PREIX = "nuxeo.s3storage.transient.";

    private static String envId;

    private static String envSecret;

    @Inject
    public BatchManager batchManager;

    @BeforeClass
    public static void beforeClass() {
        envId = StringUtils.defaultIfBlank(System.getenv(SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR),
                System.getenv(SDKGlobalConfiguration.ALTERNATE_ACCESS_KEY_ENV_VAR));
        envSecret = StringUtils.defaultIfBlank(System.getenv(SDKGlobalConfiguration.SECRET_KEY_ENV_VAR),
                System.getenv(SDKGlobalConfiguration.ALTERNATE_SECRET_KEY_ENV_VAR));
        assumeTrue("AWS Credentials not set in the environment variables", StringUtils.isNoneBlank(envId, envSecret));
        System.setProperty(S3DIRECT_PREIX + S3BinaryManager.AWS_ID_PROPERTY, envId);
        System.setProperty(S3DIRECT_PREIX + S3BinaryManager.AWS_SECRET_PROPERTY, envSecret);
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
    public void testWithoutMultipart() throws SdkBaseException, InterruptedException {
        test(1024);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-s3directupload-contrib.xml")
    public void testClientSideMultipartUpload() throws SdkBaseException, InterruptedException {
        // MULTIPART_THRESHOLD is the limit for the client-side multipart upload
        // MULTIPART_THRESHOLD + 1 is the limit for the server-side multipart copy
        test(MULTIPART_THRESHOLD + 1);
    }

    @Test
    @Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-s3directupload-contrib.xml")
    public void testServerSideMultipartCopy() throws SdkBaseException, InterruptedException {
        // MULTIPART_THRESHOLD is the limit for the client-side multipart upload
        // MULTIPART_THRESHOLD + 1 is the limit for the server-side multipart copy
        test(MULTIPART_THRESHOLD + 2);
    }

    public void test(int size) throws SdkBaseException, InterruptedException {
        // generate unique key and and random content of give size
        String key = "key" + System.nanoTime();
        String name = "name" + System.nanoTime();
        byte[] content = generateRandomBytes(size);

        // create and initialize batch
        BatchHandler handler = batchManager.getHandler("s3");
        Batch newBatch = handler.newBatch(null);
        Batch batch = handler.getBatch(newBatch.getKey());

        // upload the content with our arbitrary key
        Map<String, Object> properties = batch.getProperties();
        AmazonS3 s3Client = createS3Client(properties);
        TransferManager tm = TransferManagerBuilder.standard()
                                                   .withMultipartUploadThreshold(MULTIPART_THRESHOLD * 1L)
                                                   .withS3Client(s3Client)
                                                   .build();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        metadata.setContentLength(content.length);

        String bucket = (String) batch.getProperties().get(S3DirectBatchHandler.INFO_BUCKET);
        String prefix = (String) batch.getProperties().get(S3DirectBatchHandler.INFO_BASE_KEY);

        String prefixedKey = prefix + key;

        tm.upload(new PutObjectRequest(bucket, prefixedKey, new ByteArrayInputStream(content), metadata))
          .waitForUploadResult();

        // create priviledged client
        properties.put(S3DirectBatchHandler.INFO_AWS_SECRET_KEY_ID, envId);
        properties.put(S3DirectBatchHandler.INFO_AWS_SECRET_ACCESS_KEY, envSecret);
        properties.remove(S3DirectBatchHandler.INFO_AWS_SESSION_TOKEN);
        AmazonS3 priviledgedS3Client = createS3Client(properties);

        // check the initial upload has been succesfull
        assertNotNull(priviledgedS3Client.getObject(bucket, prefixedKey));
        BatchFileInfo info = new BatchFileInfo(prefixedKey, name, "text/plain", content.length, null);
        assertTrue(handler.completeUpload(batch.getKey(), key, info));

        // check the object has been renamed to its etag
        try {
            priviledgedS3Client.getObject(bucket, prefixedKey);
            fail("should throw 404");
        } catch (AmazonS3Exception e) {
            assertTrue(e.getMessage().contains("404"));
        }

        Blob transientBlob = handler.getBatch(batch.getKey()).getBlob(key);
        String digest = transientBlob.getDigest();
        String lastEtag = digest.substring(digest.lastIndexOf(":") + 1);

        // check the s3 object has been renamed to correct etag
        assertNotNull(priviledgedS3Client.getObject(bucket, prefix + lastEtag));

        // cleanup
        priviledgedS3Client.deleteObject(bucket, lastEtag);
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
