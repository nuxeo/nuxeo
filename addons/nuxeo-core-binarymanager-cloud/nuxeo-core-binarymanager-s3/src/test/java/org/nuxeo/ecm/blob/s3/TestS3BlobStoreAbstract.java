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

import static com.amazonaws.SDKGlobalConfiguration.ACCESS_KEY_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.ALTERNATE_ACCESS_KEY_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.ALTERNATE_SECRET_KEY_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.AWS_REGION_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.AWS_SESSION_TOKEN_ENV_VAR;
import static com.amazonaws.SDKGlobalConfiguration.SECRET_KEY_ENV_VAR;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNoneBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.AWS_ID_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.AWS_SECRET_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.AWS_SESSION_TOKEN_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.BUCKET_NAME_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.BUCKET_PREFIX_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.BUCKET_REGION_PROPERTY;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.BlobStore;
import org.nuxeo.ecm.core.blob.BlobStoreBlobProvider;
import org.nuxeo.ecm.core.blob.InMemoryBlobStore;
import org.nuxeo.ecm.core.blob.KeyStrategyDigest;
import org.nuxeo.ecm.core.blob.TestAbstractBlobStore;

public abstract class TestS3BlobStoreAbstract extends TestAbstractBlobStore {

    @BeforeClass
    public static void beforeClass() {
        getProperties().forEach(TestS3BlobStoreAbstract::setProperty);
    }

    public static void setProperty(String key, String value) {
        System.getProperties().put(S3BlobStoreConfiguration.SYSTEM_PROPERTY_PREFIX + '.' + key, value);
    }

    public static Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>();

        String envId = defaultIfBlank(System.getenv(ACCESS_KEY_ENV_VAR), System.getenv(ALTERNATE_ACCESS_KEY_ENV_VAR));
        String envSecret = defaultIfBlank(System.getenv(SECRET_KEY_ENV_VAR),
                System.getenv(ALTERNATE_SECRET_KEY_ENV_VAR));
        String envSessionToken = defaultIfBlank(System.getenv(AWS_SESSION_TOKEN_ENV_VAR), "");
        String envRegion = defaultIfBlank(System.getenv(AWS_REGION_ENV_VAR), "");

        String bucketName = "nuxeo-test-changeme";
        String bucketPrefix = "testfolder/";

        assumeTrue("AWS Credentials not set in the environment variables", isNoneBlank(envId, envSecret));

        properties.put(AWS_ID_PROPERTY, envId);
        properties.put(AWS_SECRET_PROPERTY, envSecret);
        properties.put(AWS_SESSION_TOKEN_PROPERTY, envSessionToken);
        properties.put(BUCKET_REGION_PROPERTY, envRegion);
        properties.put(BUCKET_NAME_PROPERTY, bucketName);
        properties.put(BUCKET_PREFIX_PROPERTY, bucketPrefix);
        return properties;
    }

    // remove all objects, including versions
    protected void clearBucket() {
        S3BlobStore s3BlobStore = (S3BlobStore) bs.unwrap();
        s3BlobStore.clearBucket();
    }

    @Override
    public void clearBlobStore() throws IOException {
        clearBucket();
        super.clearBlobStore();
    }

    // copy/move from another S3BlobStore has an different, optimized code path

    @Test
    public void testCopyIsOptimized() {
        BlobProvider otherbp = blobManager.getBlobProvider("other");
        BlobStore otherS3Store = ((BlobStoreBlobProvider) otherbp).store; // no need for unwrap
        assertTrue(bs.copyBlobIsOptimized(otherS3Store));
        InMemoryBlobStore otherStore = new InMemoryBlobStore("mem", new KeyStrategyDigest("MD5"));
        assertFalse(bs.copyBlobIsOptimized(otherStore));
    }

    @Test
    public void testCopyFromS3BlobStore() throws IOException {
        testCopyOrMoveFromS3BlobStore(false);
    }

    @Test
    public void testMoveFromS3BlobStore() throws IOException {
        testCopyOrMoveFromS3BlobStore(true);
    }

    protected void testCopyOrMoveFromS3BlobStore(boolean atomicMove) throws IOException {
        // we don't test the unimplemented copyBlob API, as it's only called from commit or during caching
        assumeFalse("low-level copy/move not tested in transactional blob store", bp.isTransactional());

        BlobProvider otherbp = blobManager.getBlobProvider("other");
        BlobStore sourceStore = ((BlobStoreBlobProvider) otherbp).store;
        String key1 = useDeDuplication() ? FOO_MD5 : ID1;
        String key2 = useDeDuplication() ? key1 : ID2;
        assertFalse(bs.copyBlob(key2, sourceStore, key1, atomicMove));
        assertEquals(key1, sourceStore.writeBlob(blobContext(ID1, FOO)));
        assertTrue(bs.copyBlob(key2, sourceStore, key1, atomicMove));
        assertBlob(bs, key2, FOO);
        if (atomicMove) {
            assertNoBlob(sourceStore, key1);
        } else {
            assertBlob(sourceStore, key1, FOO);
        }
    }

}
