/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.blob;

import java.time.Instant;

/**
 * Status associated to a blob in storage.
 *
 * @since 11.1
 */
public class BlobStatus {

    protected String storageClass;

    protected boolean downloadable = true;

    protected Instant downloadableUntil;

    protected boolean ongoingRestore;

    public BlobStatus withStorageClass(String storageClass) {
        this.storageClass = storageClass;
        return this;
    }

    public BlobStatus withDownloadable(boolean downloadable) {
        this.downloadable = downloadable;
        return this;
    }

    public BlobStatus withDownloadableUntil(Instant downloadableUntil) {
        this.downloadableUntil = downloadableUntil;
        return this;
    }

    public BlobStatus withOngoingRestore(boolean ongoingRestore) {
        this.ongoingRestore = ongoingRestore;
        return this;
    }

    /**
     * The storage class, or {@code null} for the standard storage class.
     */
    public String getStorageClass() {
        return storageClass;
    }

    /**
     * Whether the blob can be immediately downloaded.
     */
    public boolean isDownloadable() {
        return downloadable;
    }

    /**
     * If the blob can be download for a limited time, until when.
     * <p>
     * Returns {@code null} if the blob is always downloadable, or is not immediately downloadable.
     */
    public Instant getDownloadableUntil() {
        return downloadableUntil;
    }

    public boolean isOngoingRestore() {
        return ongoingRestore;
    }
}
