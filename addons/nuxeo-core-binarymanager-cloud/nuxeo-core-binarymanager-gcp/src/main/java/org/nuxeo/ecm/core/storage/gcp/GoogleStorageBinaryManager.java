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
 *     Nuxeo
 */
package org.nuxeo.ecm.core.storage.gcp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.blob.AbstractBinaryGarbageCollector;
import org.nuxeo.ecm.blob.AbstractCloudBinaryManager;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.FileStorage;

import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobField;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;

/**
 * A Binary Manager that stores binaries as Google Storage BLOBs
 * <p>
 * The BLOBs are cached locally on first access for efficiency.
 * <p>
 * Because the BLOB length can be accessed independently of the binary stream, it is also cached in a simple text file
 * if accessed before the stream.
 *
 * @since 10.10
 */
public class GoogleStorageBinaryManager extends AbstractCloudBinaryManager {

    private static final Log log = LogFactory.getLog(GoogleStorageBinaryManager.class);

    public static final String BUCKET_NAME_PROPERTY = "bucket";

    public static final String PROJECT_ID_PROPERTY = "project";

    public static final String GOOGLE_APPLICATION_CREDENTIALS = "credentials";

    public static final String GOOGLE_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    public static final String GOOGLE_STORAGE_SCOPE = "https://www.googleapis.com/auth/devstorage.full_control";

    public static final String SYSTEM_PROPERTY_PREFIX = "nuxeo.gcp";

    protected String bucketName;

    protected Bucket bucket;

    protected Storage storage;

    @Override
    protected void setupCloudClient() {
        try {
            String projectId = getProperty(PROJECT_ID_PROPERTY);

            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(getProperty(GOOGLE_APPLICATION_CREDENTIALS).getBytes()))
                                                             .createScoped(GOOGLE_PLATFORM_SCOPE, GOOGLE_STORAGE_SCOPE);
            credentials.refreshIfExpired();

            storage = StorageOptions.newBuilder()
                                    .setCredentials(GoogleCredentials.create(credentials.refreshAccessToken()))
                                    .setProjectId(projectId)
                                    .build()
                                    .getService();
            bucketName = getProperty(BUCKET_NAME_PROPERTY);
            bucket = this.getOrCreateBucket(bucketName);
        } catch (IOException | StorageException e) {
            throw new NuxeoException(e);
        }
    }

    /**
     * Get or create a bucket with the given bucketName.
     *
     * @param bucketName
     * @return the bucket instance.
     */
    public Bucket getOrCreateBucket(String bucketName) {
        try {
            Bucket bucket = storage.get(bucketName);
            if (bucket == null) {
                log.debug(String.format("Creating a new bucket %s", bucketName));
                return storage.create(BucketInfo.of(bucketName));
            }
            return bucket;
        } catch (StorageException e) {
            throw new NuxeoException(e);
        }
    }

    /**
     * Delete a bucket (and all its blobs) with the given bucketName.
     *
     * @param bucketName
     * @return boolean if bucket has been deleted or not.
     */
    public boolean deleteBucket(String bucketName) {
        try {
            Bucket bucket = storage.get(bucketName);
            for (Blob blob : storage.list(bucketName).iterateAll()) {
                blob.delete();
            }
            return bucket.exists() && storage.delete(bucketName);
        } catch (StorageException e) {
            throw new NuxeoException(e);
        }
    }

    public Bucket getBucket() {
        return bucket;
    }

    @Override
    protected FileStorage getFileStorage() {
        return new GCPFileStorage();
    }

    public class GCPFileStorage implements FileStorage {

        @Override
        public void storeFile(String key, File file) {
            try {
                bucket.create(key, new FileInputStream(file));
            } catch (StorageException | IOException e) {
                throw new NuxeoException(e);
            }
        }

        @Override
        public boolean fetchFile(String key, File file) {
            try {
                Blob blob = bucket.get(key);
                if (blob != null) {
                    blob.downloadTo(file.toPath());
                    return true;
                }
                return false;
            } catch (StorageException e) {
                throw new NuxeoException(e);
            }
        }
    }

    @Override
    protected String getSystemPropertyPrefix() {
        return SYSTEM_PROPERTY_PREFIX;
    }

    /**
     * Garbage collector for GCP binaries that stores the marked (in use) binaries in memory.
     */
    public static class GoogleStorageBinaryGarbageCollector
            extends AbstractBinaryGarbageCollector<GoogleStorageBinaryManager> {

        protected GoogleStorageBinaryGarbageCollector(GoogleStorageBinaryManager binaryManager) {
            super(binaryManager);
        }

        @Override
        public String getId() {
            return "gcs:" + binaryManager.bucketName;
        }

        protected String getDigest(Blob blob) {
            return blob.getBlobId().getName();
        }

        @Override
        public Set<String> getUnmarkedBlobs() {
            try {
                Set<String> unmarked = new HashSet<>();
                Page<Blob> blobs = binaryManager.getBucket().list(BlobListOption.fields(BlobField.ID, BlobField.SIZE));
                do {
                    for (Blob blob : blobs.iterateAll()) {
                        if (marked.contains(getDigest(blob))) {
                            status.numBinaries++;
                            status.sizeBinaries += blob.getSize();
                        } else {
                            status.numBinariesGC++;
                            status.sizeBinariesGC += blob.getSize();
                            unmarked.add(getDigest(blob));
                            marked.remove(getDigest(blob));
                        }
                    }
                    blobs = blobs.getNextPage();
                } while (blobs != null);
                return unmarked;
            } catch (StorageException e) {
                throw new NuxeoException(e);
            }
        }
    }

    @Override
    protected BinaryGarbageCollector instantiateGarbageCollector() {
        return new GoogleStorageBinaryGarbageCollector(this);
    }

    @Override
    public void removeBinaries(Collection<String> digests) {
        digests.forEach(this::removeBinary);
    }

    protected void removeBinary(String digest) {
        try {
            storage.delete(BlobId.of(bucket.getName(), digest));
        } catch (StorageException e) {
            throw new NuxeoException(e);
        }
    }

}
