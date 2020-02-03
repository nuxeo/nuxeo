/*
 * (C) Copyright 2019 Nuxeo SA (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.BUCKET_NAME_PROPERTY;
import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.SYSTEM_PROPERTY_PREFIX;

import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;

/**
 * Tests S3DirectBatchHandler with Server Side Encryption activated on a bucket with policy denying requests without SSE
 * header.<br>
 * <br>
 * The policy used is the one described in the following AWS doc page :<br>
 * <br>
 * https://docs.aws.amazon.com/AmazonS3/latest/dev/UsingServerSideEncryption.html
 *
 * @since 11.1
 */
public class TestS3DirectBatchHandlerWithSSEAndPolicy extends TestS3DirectBatchHandlerWithSSE {

    @BeforeClass
    public static void beforeClass() {
        TestS3DirectBatchHandlerWithSSE.beforeClass();
        // use a s3 bucket with encryption enforcement policy
        String envBucketName = StringUtils.defaultIfBlank(System.getenv("AWS_BUCKET_POLICY_NAME_NUXEO_TEST"), "nuxeo-s3-directupload-policy");
        System.setProperty(SYSTEM_PROPERTY_PREFIX + "." + BUCKET_NAME_PROPERTY, envBucketName);
    }
}
