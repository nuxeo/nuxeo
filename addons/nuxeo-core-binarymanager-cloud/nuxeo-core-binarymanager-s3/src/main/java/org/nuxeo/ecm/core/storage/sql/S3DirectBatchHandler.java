/*
 * (C) Copyright 2011-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Luís Duarte
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.nuxeo.ecm.core.storage.sql.S3Utils.NON_MULTIPART_COPY_MAX_SIZE;

import java.io.Serializable;
import java.util.Map;
import java.util.regex.Pattern;

import org.nuxeo.ecm.automation.server.jaxrs.batch.Batch;
import org.nuxeo.ecm.automation.server.jaxrs.batch.handler.AbstractBatchHandler;
import org.nuxeo.ecm.automation.server.jaxrs.batch.handler.BatchFileInfo;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.binary.Binary;
import org.nuxeo.ecm.core.blob.binary.BinaryBlob;
import org.nuxeo.ecm.core.blob.binary.LazyBinary;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetFederationTokenRequest;

/**
 * Batch Handler allowing direct S3 upload.
 *
 * @since 10.1
 */
public class S3DirectBatchHandler extends AbstractBatchHandler {

    protected static final Pattern REGEX_MULTIPART_ETAG = Pattern.compile("-\\d+$");

    protected static final Pattern REGEX_BUCKET_PATH_PLACE_HOLDER = Pattern.compile("\\{\\{bucketPath}}");

    // properties passed at initialization time from extension point

    public static final String POLICY_TEMPLATE_PROPERTY = "policyTemplate";

    // keys in the batch properties, returned to the client

    public static final String INFO_AWS_SECRET_KEY_ID = "awsSecretKeyId";

    public static final String INFO_AWS_SECRET_ACCESS_KEY = "awsSecretAccessKey";

    public static final String INFO_AWS_SESSION_TOKEN = "awsSessionToken";

    public static final String INFO_BUCKET = "bucket";

    public static final String INFO_BASE_KEY = "baseKey";

    public static final String INFO_EXPIRATION = "expiration";

    public static final String INFO_AWS_REGION = "region";

    public static final String INFO_USE_S3_ACCELERATE = "useS3Accelerate";

    protected AWSSecurityTokenService stsClient;

    protected int expiration;

    protected String policy;

    private NuxeoS3Client s3Client;

    @Override
    protected void initialize(Map<String, String> properties) {
        super.initialize(properties);

        s3Client = NuxeoS3Client.builder(S3BinaryManager.SYSTEM_PROPERTY_PREFIX, properties).build();
        expiration = Integer.parseInt(defaultIfEmpty(properties.get(INFO_EXPIRATION), "0"));
        policy = properties.get(POLICY_TEMPLATE_PROPERTY);
        stsClient = s3Client.getSTSClient();

    }

    @Override
    public Batch getBatch(String batchId) {
        Map<String, Serializable> parameters = getBatchParameters(batchId);
        if (parameters == null) {
            return null;
        }

        // create the batch
        Batch batch = new Batch(batchId, parameters, getName(), getTransientStore());

        String name = batchId.substring(0, Math.min(32, batchId.length()));

        GetFederationTokenRequest request = new GetFederationTokenRequest().withPolicy(policy).withName(name);

        if (expiration > 0) {
            request.setDurationSeconds(expiration);
        }

        Credentials credentials = getSTSCredentials(request);

        Map<String, Object> properties = batch.getProperties();
        properties.put(INFO_AWS_SECRET_KEY_ID, credentials.getAccessKeyId());
        properties.put(INFO_AWS_SECRET_ACCESS_KEY, credentials.getSecretAccessKey());
        properties.put(INFO_AWS_SESSION_TOKEN, credentials.getSessionToken());
        properties.put(INFO_BUCKET, s3Client.getBucketName());
        properties.put(INFO_BASE_KEY, s3Client.getBucketNamePrefix());
        properties.put(INFO_EXPIRATION, credentials.getExpiration().toInstant().toEpochMilli());
        properties.put(INFO_AWS_REGION, s3Client.getAmazonS3().getRegionName());
        properties.put(INFO_USE_S3_ACCELERATE, s3Client.isAccelerateModeEnabled());

        return batch;
    }

    protected Credentials getSTSCredentials(GetFederationTokenRequest request) {
        return stsClient.getFederationToken(request).getCredentials();
    }

    @Override
    public boolean completeUpload(String batchId, String fileIndex, BatchFileInfo fileInfo) {
        String fileKey = fileInfo.getKey();
        AmazonS3 amazonS3 = s3Client.getAmazonS3();

        ObjectMetadata metadata = amazonS3.getObjectMetadata(s3Client.getBucketName(), fileKey);
        String etag = metadata.getETag();
        if (isEmpty(etag)) {
            return false;
        }
        String mimeType = metadata.getContentType();
        String encoding = metadata.getContentEncoding();

        String bucket = s3Client.getBucketName();
        String bucketPrefix = s3Client.getBucketNamePrefix();

        String newFileKey = bucketPrefix + etag;
        ObjectMetadata newMetadata;
        if (metadata.getContentLength() > lowerThresholdToUseMultipartCopy()) {
            newMetadata = S3Utils.copyFileMultipart(amazonS3, metadata, bucket, fileKey, bucket, newFileKey, true);
        } else {
            newMetadata = S3Utils.copyFile(amazonS3, metadata, bucket, fileKey, bucket, newFileKey, true);
            boolean isMultipartUpload = REGEX_MULTIPART_ETAG.matcher(etag).find();
            if (isMultipartUpload) {
                etag = newMetadata.getETag();
                String previousFileKey = newFileKey;
                newFileKey = bucketPrefix + etag;
                newMetadata = S3Utils.copyFile(amazonS3, metadata, bucket, previousFileKey, bucket, newFileKey, true);
            }
        }

        String filename = fileInfo.getFilename();
        long length = newMetadata.getContentLength();
        String digest = newMetadata.getContentMD5() != null ? newMetadata.getContentMD5() : etag;
        String blobProviderId = transientStoreName; // TODO decouple this
        Binary binary = new LazyBinary(digest, blobProviderId, null);
        Blob blob = new BinaryBlob(binary, digest, filename, mimeType, encoding, digest, length);
        Batch batch = getBatch(batchId);
        batch.addFile(fileIndex, blob, filename, mimeType);

        return true;
    }

    protected long lowerThresholdToUseMultipartCopy() {
        return NON_MULTIPART_COPY_MAX_SIZE;
    }

}
