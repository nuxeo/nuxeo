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
package org.nuxeo.ecm.blob.azure;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.nuxeo.ecm.blob.AbstractTestBlobStoreRetention;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;

/**
 * Requires Azure Blob Retention enabled.
 *
 * @since 2025.11
 */
@Features(AzureBlobProviderFeature.class)
@Deploy("org.nuxeo.ecm.core.storage.binarymanager.azure.test:OSGI-INF/test-azure-record.xml")
public class TestAzureStorageRetention
        extends AbstractTestBlobStoreRetention<AzureBlobStoreConfiguration, AzureBlobKey> {

    @Override
    protected Duration getRetentionDelay() {
        // Azure does not take into account ms in retention dates
        // Let's make sure we bump the seconds unit in time stamps
        return Duration.ofSeconds(2);
    }

    @Override
    protected void assertNoRetention() {
        var ip = getCloudKey().blobClient().getProperties().getImmutabilityPolicy();
        assertNotNull(ip);
        assertNull(ip.getExpiryTime());
        assertNull(ip.getPolicyMode());
    }

    @Override
    protected void assertObjectHasLegalHold() {
        assertTrue(getCloudKey().blobClient().getProperties().hasLegalHold());
    }

    @Override
    protected void assertObjectHasNotLegalHold() {
        assertFalse(getCloudKey().blobClient().getProperties().hasLegalHold());
    }

    @Override
    protected void assertRetention(Instant retainUntil) {
        var ip = getCloudKey().blobClient().getProperties().getImmutabilityPolicy();
        assertNotNull(ip);
        assertEquals(getConfig().retentionMode, ip.getPolicyMode());
        assertNotNull(ip.getExpiryTime());
        // Azure does not take into account ms in retention dates
        var expected = retainUntil.truncatedTo(ChronoUnit.SECONDS);
        assertEquals(expected, ip.getExpiryTime().toInstant());
    }

    @Override
    protected AzureBlobStoreConfiguration getConfig() {
        return ((AzureBlobProvider) Framework.getService(BlobManager.class).getBlobProvider("test")).config;
    }

    @Override
    protected AzureBlobKey getCloudKey() {
        ManagedBlob blob = (ManagedBlob) doc.getPropertyValue("file:content");
        assertNotNull(blob);
        String key = blob.getKey().substring("test:".length());
        return new AzureBlobKey(getConfig(), key);
    }

    @Override
    protected boolean isRetentionExpired() {
        var ip = getCloudKey().blobClient().getProperties().getImmutabilityPolicy();
        if (ip == null) {
            return true;
        }
        var expiryTime = ip.getExpiryTime();
        if (expiryTime == null) {
            return true;
        }
        return Instant.now().isAfter(expiryTime.toInstant());
    }

    @Override
    protected void removeLegalHold() {
        getCloudKey().blobClient().setLegalHold(false);
    }
}
