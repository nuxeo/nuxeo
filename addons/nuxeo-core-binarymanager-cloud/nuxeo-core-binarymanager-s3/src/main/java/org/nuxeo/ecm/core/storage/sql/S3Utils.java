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
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.nuxeo.ecm.core.api.NuxeoException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
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

    /** The size of the parts that we use for multipart copy. */
    public static final long PART_SIZE = 5L * 1024 * 1024; // 5 MB

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
     * Copies a file using multipart upload.
     *
     * @param amazonS3 the S3 client
     * @param objectMetadata the metadata of the object being copied
     * @param sourceBucket the source bucket
     * @param sourceKey the source key
     * @param targetBucket the target bucket
     * @param targetKey the target key
     * @param deleteSource whether to delete the source object if the copy is successful
     */
    public static ObjectMetadata copyFileMultipart(AmazonS3 amazonS3, ObjectMetadata objectMetadata,
            String sourceBucket, String sourceKey, String targetBucket, String targetKey, boolean deleteSource) {
        InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(sourceBucket,
                targetKey);
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
        processSlices(PART_SIZE, objectSize, partCopy);

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
     */
    public static ObjectMetadata copyFile(AmazonS3 amazonS3, ObjectMetadata objectMetadata, String sourceBucket,
            String sourceKey, String targetBucket, String targetKey, boolean deleteSource) {
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(sourceBucket, sourceKey, targetBucket, targetKey);
        amazonS3.copyObject(copyObjectRequest);
        if (deleteSource) {
            amazonS3.deleteObject(sourceBucket, sourceKey);
        }
        return amazonS3.getObjectMetadata(targetBucket, targetKey);
    }

    /**
     * Gets the credentials providers for the given AWS key and secret.
     *
     * @param awsSecretKeyId the AWS key id
     * @param awsSecretAccessKey the secret
     */
    public static AWSCredentialsProvider getAWSCredentialsProvider(String awsSecretKeyId, String awsSecretAccessKey) {
        AWSCredentialsProvider awsCredentialsProvider;
        if (isBlank(awsSecretKeyId) || isBlank(awsSecretAccessKey)) {
            awsCredentialsProvider = InstanceProfileCredentialsProvider.getInstance();
            try {
                awsCredentialsProvider.getCredentials();
            } catch (AmazonClientException e) {
                throw new NuxeoException("Missing AWS credentials and no instance role found", e);
            }
        } else {
            awsCredentialsProvider = new AWSStaticCredentialsProvider(
                    new BasicAWSCredentials(awsSecretKeyId, awsSecretAccessKey));
        }
        return awsCredentialsProvider;
    }

}
