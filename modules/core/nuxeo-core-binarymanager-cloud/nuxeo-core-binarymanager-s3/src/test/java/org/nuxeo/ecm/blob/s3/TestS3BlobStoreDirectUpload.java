/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.blob.s3;

import static org.junit.Assert.assertTrue;

import javax.inject.Inject;

import org.junit.Test;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * Tests S3DirectBatchHandler with S3BlobProvider.
 *
 * @since 11.5
 */
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.s3.tests:OSGI-INF/test-blob-provider-s3-directupload.xml")
public class TestS3BlobStoreDirectUpload extends TestS3DirectUploadAbstract {

    @Inject
    protected BlobManager blobManager;

    @Test
    public void testImplementation() {
        BlobProvider blobProvider = blobManager.getBlobProvider("s3DUBlobProviderSource");
        assertTrue(blobProvider instanceof S3BlobProvider);
    }

}
