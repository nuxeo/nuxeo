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

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.blob.s3.S3BlobProviderFeature.PREFIX_TEST;
import static org.nuxeo.ecm.core.storage.sql.S3BinaryManager.BUCKET_NAME_PROPERTY;

import org.junit.BeforeClass;

/**
 * Tests TestS3BinaryManager with Server Side Encryption activated on a bucket with policy denying requests without SSE
 * header. <br>
 * <br>
 * see {@link TestS3BinaryManagerWithSSE2AndPolicy} for details about the policy.
 *
 * @since 11.1
 */
public class TestS3BinaryManagerWithSSE2AndPolicy extends TestS3BinaryManager {

    public static final String POLICY = "policy";

    @BeforeClass
    public static void beforeClass() {
        TestS3BinaryManagerWithSSE.beforeClass();
        // use a s3 bucket with encryption enforcement policy
        String bucketName = System.getProperty(String.format("%s%s.%s", PREFIX_TEST, POLICY, BUCKET_NAME_PROPERTY));
        assumeTrue("AWS bucket with policy not set in the environment variables", isNotBlank(bucketName));
        properties.put(BUCKET_NAME_PROPERTY, bucketName);
    }

}
