/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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

import static org.junit.Assert.assertEquals;
import static org.nuxeo.ecm.core.blob.KeyStrategy.VER_SEP;

import org.nuxeo.ecm.core.blob.TestAbstractBlobStoreWithOptimizedCopy;
import org.nuxeo.runtime.test.runner.Features;

import com.amazonaws.services.s3.model.GetObjectMetadataRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;

@Features(S3BlobProviderFeature.class)
public abstract class TestS3BlobStoreAbstract extends TestAbstractBlobStoreWithOptimizedCopy {

    protected void assertStorageClass(String blobKey) {
        S3BlobStoreConfiguration config = ((S3BlobProvider) bp).config;
        var key = config.bucketKey(blobKey);
        String objectKey;
        String versionId;
        int seppos = key.indexOf(VER_SEP);
        if (seppos < 0) {
            objectKey = key;
            versionId = null;
        } else {
            objectKey = key.substring(0, seppos);
            versionId = key.substring(seppos + 1);
        }
        GetObjectMetadataRequest request = new GetObjectMetadataRequest(config.bucketName, objectKey, versionId);
        ObjectMetadata metadata;
        metadata = config.amazonS3.getObjectMetadata(request);
        assertEquals(expectedStorageClass(), metadata.getStorageClass());
    }

    protected String expectedStorageClass() {
        return null; // storage class is null for StorageClass.Standard;
    }

}
