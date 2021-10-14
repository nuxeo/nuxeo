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

import static java.lang.Math.min;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration;
import org.nuxeo.runtime.aws.NuxeoAWSCredentialsProvider;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyPartRequest;
import com.amazonaws.services.s3.model.CopyPartResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;

/**
 * AWS S3 utilities.
 *
 * @since 10.1
 */
public class S3Utils {

    /** The maximum size of a file that can be copied without using multipart: 5 GB */
    public static final long NON_MULTIPART_COPY_MAX_SIZE = 5L * 1024 * 1024 * 1024;

    /**
     * The default multipart copy part size. This default is used only if the configuration service is not available or
     * if the configuration property {@value #MULTIPART_COPY_PART_SIZE_PROPERTY} is not defined.
     *
     * @since 11.1
     * @deprecated since 2021.11, use {@link S3BlobStoreConfiguration#MULTIPART_COPY_PART_SIZE_DEFAULT} instead.
     */
    @Deprecated
    public static final long MULTIPART_COPY_PART_SIZE_DEFAULT = 5L * 1024 * 1024; // 5 MB

    /**
     * @deprecated since 11.1, use {@link #MULTIPART_COPY_PART_SIZE_DEFAULT} instead
     */
    @Deprecated
    public static final long PART_SIZE = MULTIPART_COPY_PART_SIZE_DEFAULT;

    /**
     * The configuration property to define the multipart copy part size.
     *
     * @since 11.1
     * @deprecated since 2021.11, use {@link S3BlobStoreConfiguration#MULTIPART_COPY_PART_SIZE_PROPERTY} instead.
     */
    @Deprecated
    public static final String MULTIPART_COPY_PART_SIZE_PROPERTY = "nuxeo.s3.multipart.copy.part.size";

    private S3Utils() {
        // utility class
    }

    /**
     * Represents an operation that accepts a slice number and a slice begin and end position.
     */
    @FunctionalInterface
    public static interface SliceConsumer {
        /**
         * Performs this operation on the arguments.
         *
         * @param num the slice number, starting at 0
         * @param begin the begin position
         * @param end the end position + 1
         */
        public void accept(int num, long begin, long end);
    }

    /**
     * Calls the consumer on all slices.
     *
     * @param slice the slice size
     * @param length the total length
     * @param consumer the slice consumer
     */
    public static void processSlices(long slice, long length, SliceConsumer consumer) {
        if (slice <= 0) {
            throw new IllegalArgumentException("Invalid slice length: " + slice);
        }
        long begin = 0;
        for (int num = 0; begin < length; num++) {
            long end = min(begin + slice, length);
            consumer.accept(num, begin, end);
            begin += slice;
        }
    }

    /**
     * Copies a file, using multipart upload if needed.
     *
     * @param amazonS3 the S3 client
     * @param objectMetadata the metadata of the object being copied
     * @param sourceBucket the source bucket
     * @param sourceKey the source key
     * @param targetBucket the target bucket
     * @param targetKey the target key
     * @param targetSSEAlgorithm the target SSE Algorithm to use, or {@code null}
     * @param deleteSource whether to delete the source object if the copy is successful
     * @since 11.1
     */
    public static ObjectMetadata copyFile(AmazonS3 amazonS3, ObjectMetadata objectMetadata, String sourceBucket,
            String sourceKey, String targetBucket, String targetKey, String targetSSEAlgorithm, boolean deleteSource) {
        if (objectMetadata.getContentLength() > NON_MULTIPART_COPY_MAX_SIZE) {
            return copyFileMultipart(amazonS3, objectMetadata, sourceBucket, sourceKey, targetBucket, targetKey, targetSSEAlgorithm, deleteSource);
        } else {
            return copyFileNonMultipart(amazonS3, objectMetadata, sourceBucket, sourceKey, targetBucket, targetKey, targetSSEAlgorithm, deleteSource);
        }
    }

    /**
     * Copies a file using multipart upload.
     *
     * @param amazonS3 the S3 client
     * @param objectMetadata the metadata of the object being copied
     * @param sourceBucket the source bucket
     * @param sourceKey the source key
     * @param targetBucket the target bucket
     * @param targetKey the target key
     * @param deleteSource whether to delete the source object if the copy is successful
     * @deprecated since 11.1, use
     *             {@link #copyFileMultipart(AmazonS3, ObjectMetadata, String, String, String, String, String, boolean)}
     *             instead
     */
    @Deprecated
    public static ObjectMetadata copyFileMultipart(AmazonS3 amazonS3, ObjectMetadata objectMetadata,
            String sourceBucket, String sourceKey, String targetBucket, String targetKey, boolean deleteSource) {
        return copyFileMultipart(amazonS3, objectMetadata, sourceBucket, sourceKey, targetBucket, targetKey, null,
                deleteSource);
    }

    /**
     * Copies a file using multipart upload.
     *
     * @param amazonS3 the S3 client
     * @param objectMetadata the metadata of the object being copied
     * @param sourceBucket the source bucket
     * @param sourceKey the source key
     * @param targetBucket the target bucket
     * @param targetKey the target key
     * @param targetSSEAlgorithm the target SSE Algorithm to use, or {@code null}
     * @param deleteSource whether to delete the source object if the copy is successful
     * @since 11.1
     */
    public static ObjectMetadata copyFileMultipart(AmazonS3 amazonS3, ObjectMetadata objectMetadata,
            String sourceBucket, String sourceKey, String targetBucket, String targetKey, String targetSSEAlgorithm,
            boolean deleteSource) {
        InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(sourceBucket,
                targetKey);

        // server-side encryption
        if (targetSSEAlgorithm != null) {
            ObjectMetadata newObjectMetadata = new ObjectMetadata();
            newObjectMetadata.setSSEAlgorithm(targetSSEAlgorithm);
            initiateMultipartUploadRequest.setObjectMetadata(newObjectMetadata);
        }

        InitiateMultipartUploadResult initiateMultipartUploadResult = amazonS3.initiateMultipartUpload(
                initiateMultipartUploadRequest);

        String uploadId = initiateMultipartUploadResult.getUploadId();
        long objectSize = objectMetadata.getContentLength();
        List<CopyPartResult> copyResponses = new ArrayList<>();

        SliceConsumer partCopy = (num, begin, end) -> {
            CopyPartRequest copyRequest = new CopyPartRequest().withSourceBucketName(sourceBucket)
                                                               .withSourceKey(sourceKey)
                                                               .withDestinationBucketName(targetBucket)
                                                               .withDestinationKey(targetKey)
                                                               .withFirstByte(begin)
                                                               .withLastByte(end - 1)
                                                               .withUploadId(uploadId)
                                                               .withPartNumber(num + 1);
            copyResponses.add(amazonS3.copyPart(copyRequest));
        };
        long partSize = S3BlobStoreConfiguration.getMultipartCopyPartSize();
        processSlices(partSize, objectSize, partCopy);

        CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(targetBucket, targetKey,
                uploadId, responsesToETags(copyResponses));
        amazonS3.completeMultipartUpload(completeRequest);
        if (deleteSource) {
            amazonS3.deleteObject(sourceBucket, sourceKey);
        }
        return amazonS3.getObjectMetadata(targetBucket, targetKey);
    }

    protected static List<PartETag> responsesToETags(List<CopyPartResult> responses) {
        return responses.stream().map(response -> new PartETag(response.getPartNumber(), response.getETag())).collect(
                Collectors.toList());
    }

    /**
     * Copies a file without using multipart upload.
     *
     * @param amazonS3 the S3 client
     * @param objectMetadata the metadata of the object being copied
     * @param sourceBucket the source bucket
     * @param sourceKey the source key
     * @param targetBucket the target bucket
     * @param targetKey the target key
     * @param deleteSource whether to delete the source object if the copy is successful
     * @deprecated since 11.1, use {@link #copyFileNonMultipart} instead
     */
    @Deprecated
    public static ObjectMetadata copyFile(AmazonS3 amazonS3, ObjectMetadata objectMetadata, String sourceBucket,
            String sourceKey, String targetBucket, String targetKey, boolean deleteSource) {
        return copyFileNonMultipart(amazonS3, objectMetadata, sourceBucket, sourceKey, targetBucket, targetKey, null, deleteSource);
    }

    /**
     * Copies a file without using multipart upload.
     *
     * @param amazonS3 the S3 client
     * @param objectMetadata the metadata of the object being copied
     * @param sourceBucket the source bucket
     * @param sourceKey the source key
     * @param targetBucket the target bucket
     * @param targetKey the target key
     * @param targetSSEAlgorithm the target SSE Algorithm to use, or {@code null}
     * @param deleteSource whether to delete the source object if the copy is successful
     * @since 11.1
     */
    public static ObjectMetadata copyFileNonMultipart(AmazonS3 amazonS3, ObjectMetadata objectMetadata, String sourceBucket,
            String sourceKey, String targetBucket, String targetKey, String targetSSEAlgorithm, boolean deleteSource) {
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(sourceBucket, sourceKey, targetBucket, targetKey);
        // server-side encryption
        if (targetSSEAlgorithm != null) {
            ObjectMetadata newObjectMetadata = new ObjectMetadata();
            newObjectMetadata.setSSEAlgorithm(targetSSEAlgorithm);
            copyObjectRequest.setNewObjectMetadata(newObjectMetadata);
        }
        amazonS3.copyObject(copyObjectRequest);
        if (deleteSource) {
            amazonS3.deleteObject(sourceBucket, sourceKey);
        }
        return amazonS3.getObjectMetadata(targetBucket, targetKey);
    }

    /**
     * Gets the credentials providers for the given AWS key and secret.
     *
     * @param accessKeyId the AWS access key id
     * @param secretKey the secret key
     * @param sessionToken the session token (optional)
     * @since 10.10
     */
    public static AWSCredentialsProvider getAWSCredentialsProvider(String accessKeyId, String secretKey,
            String sessionToken) {
        if (isNotBlank(accessKeyId) && isNotBlank(secretKey)) {
            // explicit values from service-specific Nuxeo configuration
            if (isNotBlank(sessionToken)) {
                return new AWSStaticCredentialsProvider(
                        new BasicSessionCredentials(accessKeyId, secretKey, sessionToken));
            } else {
                return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretKey));
            }
        }
        return NuxeoAWSCredentialsProvider.getInstance();
    }

}
