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
package org.nuxeo.ecm.core.storage.gcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.time.Instant;

import org.nuxeo.ecm.blob.AbstractTestBlobStoreRetention;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;

/**
 * Requires GS Object Retention enabled.
 *
 * @since 2025.8
 */
@Features(GoogleStorageBlobProviderFeature.class)
@Deploy("org.nuxeo.ecm.core.storage.gcp.tests:OSGI-INF/test-google-storage-record.xml")
public class TestGoogleStorageRetention
        extends AbstractTestBlobStoreRetention<GoogleStorageBlobStoreConfiguration, GoogleStorageBlobKey> {

    @Override
    protected void assertNoRetention() {
        assertNull(getBlob().getRetention());
    }

    @Override
    protected void assertObjectHasLegalHold() {
        assertTrue(getBlob().getTemporaryHold());
    }

    @Override
    protected void assertObjectHasNotLegalHold() {
        var hold = getBlob().getTemporaryHold();
        assertTrue(hold == null || !hold);
    }

    @Override
    protected void assertRetention(Instant retainUntil) {
        Blob blob = getBlob();
        assertNotNull(blob.getRetention());
        assertEquals(getConfig().retentionMode, blob.getRetention().getMode());
        assertNotNull(blob.getRetention().getRetainUntilTime());
        assertEquals(retainUntil, blob.getRetention().getRetainUntilTime().toInstant());
    }

    @Override
    protected GoogleStorageBlobKey getCloudKey() {
        ManagedBlob blob = (ManagedBlob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        String key = blob.getKey().substring("test:".length());
        return new GoogleStorageBlobKey(getConfig(), key);
    }

    @Override
    protected GoogleStorageBlobStoreConfiguration getConfig() {
        return ((GoogleStorageBlobProvider) Framework.getService(BlobManager.class).getBlobProvider("test")).config;
    }

    @Override
    protected boolean isRetentionExpired() throws IOException {
        try {
            Blob blob = getBlob();
            if (blob.getRetention() == null) {
                return true;
            }
            return Instant.now().isAfter(blob.getRetention().getRetainUntilTime().toInstant());
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void removeLegalHold() throws IOException {
        try {
            getConfig().storage.get(getCloudKey().blobId()).toBuilder().setTemporaryHold(false).build().update();
        } catch (StorageException e) {
            throw new IOException(e);
        }
    }

    protected Blob getBlob() {
        return getConfig().storage.get(getCloudKey().blobId(),
                Storage.BlobGetOption.fields(Storage.BlobField.values()));
    }

}
