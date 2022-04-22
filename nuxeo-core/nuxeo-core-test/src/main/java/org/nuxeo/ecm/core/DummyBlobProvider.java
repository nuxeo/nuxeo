/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.AbstractBlobProvider;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobStatus;
import org.nuxeo.ecm.core.blob.BlobUpdateContext;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.SimpleManagedBlob;
import org.nuxeo.ecm.core.blob.apps.AppLink;

/**
 * Dummy storage in memory.
 */
public class DummyBlobProvider extends AbstractBlobProvider {

    public final static long RESTORE_DELAY_MILLISECONDS = 1000;

    protected Map<String, byte[]> blobs;

    /** @since 11.1 **/
    protected Map<String, BlobStatus> blobsStatus;

    protected AtomicLong counter;

    @Override
    public void initialize(String blobProviderId, Map<String, String> properties) throws IOException {
        super.initialize(blobProviderId, properties);
        blobs = new HashMap<>();
        blobsStatus = new HashMap<>();
        counter = new AtomicLong();
    }

    @Override
    public void close() {
        blobs.clear();
    }

    @Override
    public Blob readBlob(BlobInfo blobInfo) {
        return new SimpleManagedBlob(blobProviderId, blobInfo) {
            private static final long serialVersionUID = 1L;

            @Override
            public InputStream getStream() throws IOException {
                if (!getStatus(this).isDownloadable()) {
                    throw new IOException(String.format("Blob %s is not downloadable", key));
                }
                int colon = key.indexOf(':');
                String k = colon < 0 ? key : key.substring(colon + 1);
                byte[] bytes = blobs.get(k);
                return new ByteArrayInputStream(bytes);
            }
        };
    }

    @Override
    public String writeBlob(Blob blob) throws IOException {
        byte[] bytes;
        try (InputStream in = blob.getStream()) {
            bytes = IOUtils.toByteArray(in);
        }
        String k = String.valueOf(counter.incrementAndGet());
        blobs.put(k, bytes);
        return k;
    }

    public List<AppLink> getAppLinks(String user, ManagedBlob blob) {
        AppLink link = new AppLink();
        link.setAppName("dummyApp");
        link.setIcon("dummyIcon");
        link.setLink("dummyLink");
        return Arrays.asList(link);
    }

    /** @since 11.1 **/
    @Override
    public BlobStatus getStatus(ManagedBlob blob) throws IOException {
        return blobsStatus.getOrDefault(getBlobKey(blob), super.getStatus(blob));
    }

    @Override
    public void updateBlob(BlobUpdateContext blobUpdateContext) throws IOException {
        if (blobUpdateContext != null) {
            BlobStatus status = blobsStatus.getOrDefault(blobUpdateContext.key, new BlobStatus());
            if (blobUpdateContext.coldStorageClass != null) {
                status.withDownloadable(!blobUpdateContext.coldStorageClass.inColdStorage);
            }
            if (blobUpdateContext.restoreForDuration != null) {
                status.withOngoingRestore(true);
                // retrieval will occur in a short delay
                ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
                Runnable restoreHandler = new Runnable() {

                    @Override
                    public void run() {
                        status.withDownloadable(true);
                        status.withOngoingRestore(false);
                        status.withDownloadableUntil(Instant.now().plus(blobUpdateContext.restoreForDuration.duration));
                        blobsStatus.put(blobUpdateContext.key, status);
                    }
                };
                executorService.schedule(restoreHandler, RESTORE_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS);
            }
            blobsStatus.put(blobUpdateContext.key, status);
        }
    }

    /** @since 11.1 **/
    public void addStatus(ManagedBlob blob, BlobStatus status) {
        blobsStatus.put(getBlobKey(blob), status);
    }

    /** @since 11.1 **/
    protected String getBlobKey(ManagedBlob blob) {
        int colon = blob.getKey().indexOf(':');
        return colon < 0 ? blob.getKey() : blob.getKey().substring(colon + 1);
    }

}
