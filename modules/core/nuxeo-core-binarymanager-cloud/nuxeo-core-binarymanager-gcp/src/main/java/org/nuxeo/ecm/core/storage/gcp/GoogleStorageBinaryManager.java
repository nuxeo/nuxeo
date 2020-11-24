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

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.blob.AbstractBinaryGarbageCollector;
import org.nuxeo.ecm.blob.AbstractCloudBinaryManager;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.FileStorage;

import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobField;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageOptions;

/**
 * A Binary Manager that stores binaries as Google Storage BLOBs
 * <p>
 * The BLOBs are cached locally on first access for efficiency.
 * <p>
 * Because the BLOB length can be accessed independently of the binary stream, it is also cached in a simple text file
 * if accessed before the stream. Related to GCP credentials, here are the options:
 * <ul>
 * <li>nuxeo.gcp.credentials=/path/to/file.json</li>
 * <li>nuxeo.gcp.credentials=file.json (located in nxserver/config)</li>
 * <li>If nothing is set, Nuxeo will look into 'gcp-credentials.json' file by default (located in nxserver/config)</li>
 * </ul>
 *
 * @since 10.10-HF12
 */
public class GoogleStorageBinaryManager extends AbstractCloudBinaryManager {

    private static final Logger log = LogManager.getLogger(GoogleStorageBinaryManager.class);

    public static final String BUCKET_NAME_PROPERTY = "storage.bucket";

    public static final String BUCKET_PREFIX_PROPERTY = "storage.bucket_prefix";

    /** @since 11.4 */
    public static final String UPLOAD_CHUNK_SIZE_PROPERTY = "storage.upload.chunk.size";

    /**
     * Default is taken from {@link com.google.cloud.BaseWriteChannel}.
     *
     * @since 11.4
     */
    public static final int DEFAULT_UPLOAD_CHUNK_SIZE = 2048 * 1024; // 2 MB

    public static final String PROJECT_ID_PROPERTY = "project";

    public static final String GOOGLE_APPLICATION_CREDENTIALS = "credentials";

    public static final String GOOGLE_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    public static final String GOOGLE_STORAGE_SCOPE = "https://www.googleapis.com/auth/devstorage.full_control";

    public static final String SYSTEM_PROPERTY_PREFIX = "nuxeo.gcp";

    private static final Pattern MD5_RE = Pattern.compile("[0-9a-f]{32}");

    public static final String DELIMITER = "/";

    public static final String GCP_JSON_FILE = "gcp-credentials.json";

    protected String bucketName;

    protected String bucketPrefix;

    protected Bucket bucket;

    protected Storage storage;

    /** @since 11.4 */
    protected int chunkSize;

    @Override
    protected void setupCloudClient() {
        try {
            String projectId = getProperty(PROJECT_ID_PROPERTY);

            File googleCredentials = new File(getProperty(GOOGLE_APPLICATION_CREDENTIALS));
            String credentialsPath = googleCredentials.isFile() ? googleCredentials.getAbsolutePath()
                    : new File(Environment.getDefault().getConfig(),
                            getProperty(GOOGLE_APPLICATION_CREDENTIALS, GCP_JSON_FILE)).getAbsolutePath();

            GoogleCredentials credentials = GoogleCredentials.fromStream(
                    new ByteArrayInputStream(Files.readAllBytes(Paths.get(credentialsPath))))
                                                             .createScoped(GOOGLE_PLATFORM_SCOPE, GOOGLE_STORAGE_SCOPE);
            credentials.refreshIfExpired();

            storage = StorageOptions.newBuilder()
                                    .setCredentials(credentials)
                                    .setProjectId(projectId)
                                    .build()
                                    .getService();
            bucketName = getProperty(BUCKET_NAME_PROPERTY);
            bucketPrefix = getProperty(BUCKET_PREFIX_PROPERTY, EMPTY);
            bucket = getOrCreateBucket(bucketName);
            chunkSize = getIntProperty(UPLOAD_CHUNK_SIZE_PROPERTY, DEFAULT_UPLOAD_CHUNK_SIZE);

            if (!isBlank(bucketPrefix) && !bucketPrefix.endsWith(DELIMITER)) {
                log.warn("Google bucket prefix ({}): {} should end with '/': added automatically.",
                        BUCKET_PREFIX_PROPERTY, bucketPrefix);
                bucketPrefix += DELIMITER;
            }
            if (isNotBlank(namespace)) {
                // use namespace as an additional prefix
                bucketPrefix += namespace;
                if (!bucketPrefix.endsWith(DELIMITER)) {
                    bucketPrefix += DELIMITER;
                }
            }
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    /**
     * Gets or creates a bucket with the given {@code bucketName}.
     *
     * @return the bucket instance.
     */
    public Bucket getOrCreateBucket(String bucketName) {
        Bucket bucket = storage.get(bucketName);
        if (bucket == null) {
            log.debug("Creating a new bucket: {}", bucketName);
            return storage.create(BucketInfo.of(bucketName));
        }
        return bucket;
    }

    /**
     * Deletes a bucket (and all its blobs) with the given {@code bucketName}.
     *
     * @return boolean if bucket has been deleted or not.
     */
    public boolean deleteBucket(String bucketName) {
        Bucket bucket = storage.get(bucketName);
        for (Blob blob : storage.list(bucketName).iterateAll()) {
            blob.delete();
        }
        return bucket.exists() && storage.delete(bucketName);
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
        public void storeFile(String digest, File file) {
            long t0 = System.currentTimeMillis();
            log.debug("Storing blob with digest: {} to GCS", digest);
            String key = bucketPrefix + digest;
            // try to get the blob's metadata to check if it exists
            if (bucket.get(key) == null) {
                try (var is = new BufferedInputStream(new FileInputStream(file));
                        var writer = storage.writer(BlobInfo.newBuilder(bucketName, key).build())) {
                    int bufferLength;
                    byte[] buffer = new byte[chunkSize];
                    writer.setChunkSize(chunkSize);
                    while ((bufferLength = IOUtils.read(is, buffer)) > 0) {
                        writer.write(ByteBuffer.wrap(buffer, 0, bufferLength));
                    }
                } catch (IOException e) {
                    throw new NuxeoException(e);
                }
                log.debug("Stored blob with digest: {} to GCS in {}ms", digest, System.currentTimeMillis() - t0);
            } else {
                log.debug("Blob with digest: {} is already in GCS", digest);
            }
        }

        @Override
        public boolean fetchFile(String key, File file) {
            Blob blob = bucket.get(bucketPrefix + key);
            if (blob != null) {
                blob.downloadTo(file.toPath());
                return true;
            }
            return false;
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

        @Override
        public Set<String> getUnmarkedBlobs() {
            Set<String> unmarked = new HashSet<>();
            Page<Blob> blobs = binaryManager.getBucket()
                                            .list(BlobListOption.fields(BlobField.ID, BlobField.SIZE),
                                                    BlobListOption.prefix(binaryManager.bucketPrefix));
            do {
                int prefixLength = binaryManager.bucketPrefix.length();
                for (Blob blob : blobs.iterateAll()) {
                    String digest = blob.getName().substring(prefixLength);
                    if (!isMD5(digest)) {
                        // ignore files that cannot be MD5 digests for
                        // safety
                        continue;
                    }
                    if (marked.contains(digest)) {
                        status.numBinaries++;
                        status.sizeBinaries += blob.getSize();
                    } else {
                        status.numBinariesGC++;
                        status.sizeBinariesGC += blob.getSize();
                        unmarked.add(digest);
                        marked.remove(digest);
                    }
                }
                blobs = blobs.getNextPage();
            } while (blobs != null);
            return unmarked;
        }
    }

    protected static boolean isMD5(String digest) {
        return MD5_RE.matcher(digest).matches();
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
        storage.delete(BlobId.of(bucket.getName(), bucketPrefix + digest));
    }

}
