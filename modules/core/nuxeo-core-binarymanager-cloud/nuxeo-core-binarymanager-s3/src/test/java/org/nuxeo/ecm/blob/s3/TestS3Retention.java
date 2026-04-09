/*
 * (C) Copyright 2024 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume RENARD
 */
package org.nuxeo.ecm.blob.s3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static software.amazon.awssdk.services.s3.model.ObjectLockLegalHoldStatus.OFF;
import static software.amazon.awssdk.services.s3.model.ObjectLockLegalHoldStatus.ON;

import java.io.IOException;
import java.time.Instant;

import org.nuxeo.ecm.blob.AbstractTestBlobStoreRetention;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ObjectLockLegalHoldStatus;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Requires S3 Object Lock enabled.
 *
 * @since 2025.0
 */
@Features(S3BlobProviderFeature.class)
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-blob-provider-s3-record.xml")
public class TestS3Retention extends AbstractTestBlobStoreRetention<S3BlobStoreConfiguration, S3BlobKey> {

    @Override
    protected S3BlobKey getCloudKey() {
        ManagedBlob blob = (ManagedBlob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        String key = blob.getKey().substring("test:".length());
        return new S3BlobKey(getConfig(), key);
    }

    @Override
    protected boolean isRetentionExpired() throws IOException {
        try {
            var retainUntil = headObject().objectLockRetainUntilDate();
            return retainUntil == null || Instant.now().isAfter(retainUntil);
        } catch (S3Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void removeLegalHold() throws IOException {
        try {
            getConfig().amazonS3.putObjectLegalHold(pb -> pb.bucket(getConfig().bucketName)
                                                            .key(getCloudKey().bucketKey())
                                                            .versionId(getCloudKey().versionId())
                                                            .legalHold(b -> b.status(OFF)));
        } catch (S3Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void assertNoRetention() {
        var headObject = headObject();
        assertNull("Object has Object Lock mode set", headObject.objectLockMode());
        assertNull("Object has Object Lock retain until date set", headObject.objectLockRetainUntilDate());
    }

    @Override
    protected void assertObjectHasLegalHold() {
        assertObjectLegalHold(ON);
    }

    @Override
    protected void assertObjectHasNotLegalHold() {
        assertObjectLegalHold(OFF);
    }

    @Override
    protected void assertRetention(Instant retainUntil) {
        assertEquals(retainUntil, headObject().objectLockRetainUntilDate());
    }

    @Override
    protected S3BlobStoreConfiguration getConfig() {
        return ((S3BlobProvider) Framework.getService(BlobManager.class).getBlobProvider("test")).config;
    }

    protected void assertObjectLegalHold(ObjectLockLegalHoldStatus expectedStatus) {
        assertEquals(expectedStatus, headObject().objectLockLegalHoldStatus());
    }

    protected HeadObjectResponse headObject() {
        return getConfig().amazonS3.headObject(b -> b.bucket(getConfig().bucketName)
                                                     .key(getCloudKey().bucketKey())
                                                     .versionId(getCloudKey().versionId()));
    }

}
