/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard
 */
package org.nuxeo.ecm.blob.s3;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static software.amazon.encryption.s3.S3EncryptionClientUtilities.DEFAULT_INSTRUCTION_FILE_SUFFIX;

import java.io.IOException;
import java.util.Objects;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Test;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.ecm.core.blob.TestAbstractBlobStore;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.RandomBug;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.encryption.s3.S3EncryptionClientException;

/**
 * @since 2025.0
 */
@Features(S3BlobProviderFeature.class)
public abstract class TestAbstractS3BlobStoreClientEncryption extends TestAbstractBlobStore {

    @Override
    public boolean checkSizeOfGCedFiles() {
        // cannot check file size with client-side encryption
        return false;
    }

    @Test
    public void testFlags() {
        assertFalse(bp.isTransactional());
        assertFalse(bp.isRecordMode());
        assertTrue(bs.getKeyStrategy().useDeDuplication());
        assertTrue(((S3BlobProvider) bp).config.useClientSideEncryption);
        BlobProvider srcProvider = blobManager.getBlobProvider("other");
        BlobStore srcStore = ((BlobStoreBlobProvider) srcProvider).store; // no need for unwrap
        assertFalse(bs.copyBlobIsOptimized(srcStore));
    }

    /**
     * Checks that an instruction file is not honoured on read - NXP-33694.
     *
     * @since 2025.24
     */
    @Test
    public void testInstructionFileIsRejected() {
        var config = ((S3BlobProvider) bp).config;
        String key = "instructionFileIsRejected";
        String bucketKey = config.bucketKey(key);
        // not config.createSyncClient(), whose http client the SDK never closes
        try (S3Client plainClient = S3Client.builder()
                                            .region(config.region)
                                            .credentialsProvider(config.awsCredentialsProvider)
                                            .endpointOverride(config.endpointOverride)
                                            .build()) {
            plainClient.putObject(b -> b.bucket(config.bucketName).key(bucketKey),
                    RequestBody.fromString("not an encrypted content"));
            plainClient.putObject(b -> b.bucket(config.bucketName).key(bucketKey + DEFAULT_INSTRUCTION_FILE_SUFFIX),
                    RequestBody.fromString("{}"));
        }
        var e = assertThrows(S3EncryptionClientException.class, () -> bs.readBlob(key, tmpFile));
        assertTrue(ExceptionUtils.getStackTrace(e),
                ExceptionUtils.getThrowableList(e)
                              .stream()
                              .map(Throwable::getMessage)
                              .filter(Objects::nonNull)
                              .anyMatch(m -> m.contains("Instruction File has been disabled")));
    }

    @Test
    @Override
    @RandomBug.Repeat(issue = "NXP-32924", onFailure = 10, onSuccess = 30)
    public void testCopy() throws IOException {
        super.testCopy();
    }

    @Test
    @Override
    @RandomBug.Repeat(issue = "NXP-32924", onFailure = 10, onSuccess = 30)
    public void testMove() throws IOException {
        super.testMove();
    }
}
