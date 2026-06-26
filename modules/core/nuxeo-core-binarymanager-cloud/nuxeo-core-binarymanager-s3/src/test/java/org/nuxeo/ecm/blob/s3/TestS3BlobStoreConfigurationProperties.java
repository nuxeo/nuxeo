/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.MINIMUM_UPLOAD_PART_SIZE_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.MULTIPART_UPLOAD_THRESHOLD_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.SYSTEM_PROPERTY_PREFIX;

import jakarta.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.ByteSize;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManagerFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.WithFrameworkProperty;

/**
 * @since 2025.22
 */
@RunWith(FeaturesRunner.class)
@Features({ BlobManagerFeature.class, S3BlobProviderFeature.class })
@WithFrameworkProperty(name = SYSTEM_PROPERTY_PREFIX + "." + MULTIPART_UPLOAD_THRESHOLD_PROPERTY, value = "32MiB")
@WithFrameworkProperty(name = SYSTEM_PROPERTY_PREFIX + "." + MINIMUM_UPLOAD_PART_SIZE_PROPERTY, value = "10MiB")
public class TestS3BlobStoreConfigurationProperties {

    @Inject
    protected BlobManager blobManager;

    @Test
    public void testPropertiesLoaded() {
        var config = ((S3BlobProvider) blobManager.getBlobProvider("test")).config;
        assertEquals(MULTIPART_UPLOAD_THRESHOLD_PROPERTY + " should be 32 MiB", ByteSize.ofMebibytes(32).bytes(),
                config.multipartUploadThreshold.bytes());
        assertEquals(MINIMUM_UPLOAD_PART_SIZE_PROPERTY + " should be 10 MiB", ByteSize.ofMebibytes(10).bytes(),
                config.minimumPartSize.bytes());
    }

}
