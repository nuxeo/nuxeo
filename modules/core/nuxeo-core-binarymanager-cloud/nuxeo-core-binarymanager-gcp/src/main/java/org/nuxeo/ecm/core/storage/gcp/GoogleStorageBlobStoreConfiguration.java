/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.gcp;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.ALLOW_BYTE_RANGE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.blob.CloudBlobStoreConfiguration;
import org.nuxeo.ecm.core.api.NuxeoException;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.BucketInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

/**
 * Blob storage configuration in Google Storage.
 *
 * @since 2023.5
 */
public class GoogleStorageBlobStoreConfiguration extends CloudBlobStoreConfiguration {

    protected static final Logger log = LogManager.getLogger(GoogleStorageBlobStoreConfiguration.class);

    public static final String BUCKET_NAME_PROPERTY = "bucket";

    public static final String BUCKET_PREFIX_PROPERTY = "bucket_prefix";

    public static final String UPLOAD_CHUNK_SIZE_PROPERTY = "storage.upload.chunk.size";

    /**
     * Default is taken from {@link com.google.cloud.BaseWriteChannel}.
     */
    public static final int DEFAULT_UPLOAD_CHUNK_SIZE = 2048 * 1024; // 2 MB

    public static final String SYSTEM_PROPERTY_PREFIX = "nuxeo.gcp";

    public static final String GOOGLE_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

    public static final String GOOGLE_STORAGE_SCOPE = "https://www.googleapis.com/auth/devstorage.full_control";

    public static final String GOOGLE_APPLICATION_CREDENTIALS = "credentials";

    public static final String GCP_JSON_FILE = "gcp-credentials.json";

    public static final String DELIMITER = "/";

    public static final String PROJECT_ID_PROPERTY = "project";

    protected final Storage storage;

    protected final String bucketName;

    protected final String bucketPrefix;

    protected final Bucket bucket;

    protected final boolean allowByteRange;

    protected final int chunkSize;

    public GoogleStorageBlobStoreConfiguration(Map<String, String> properties) throws IOException {
        super(SYSTEM_PROPERTY_PREFIX, properties);
        String projectId = getProperty(PROJECT_ID_PROPERTY);
        Path credentialsPath = Path.of(getProperty(GOOGLE_APPLICATION_CREDENTIALS, GCP_JSON_FILE));
        if (!credentialsPath.isAbsolute()) {
            credentialsPath = Environment.getDefault().getConfig().toPath().resolve(credentialsPath);
        }
        GoogleCredentials credentials;
        try {
            credentials = GoogleCredentials.fromStream(Files.newInputStream(credentialsPath))
                                           .createScoped(GOOGLE_PLATFORM_SCOPE, GOOGLE_STORAGE_SCOPE);
            credentials.refreshIfExpired();
        } catch (IOException e) {
            throw new NuxeoException(e);
        }

        storage = StorageOptions.newBuilder().setCredentials(credentials).setProjectId(projectId).build().getService();
        bucketName = getProperty(BUCKET_NAME_PROPERTY);
        Bucket b = storage.get(bucketName);
        if (b == null) {
            log.debug("Creating a new bucket: {}", bucketName);
            b = storage.create(BucketInfo.of(bucketName));
        }
        bucket = b;
        chunkSize = getIntProperty(UPLOAD_CHUNK_SIZE_PROPERTY, DEFAULT_UPLOAD_CHUNK_SIZE);
        allowByteRange = getBooleanProperty(ALLOW_BYTE_RANGE);

        String bp = getProperty(BUCKET_PREFIX_PROPERTY, EMPTY);
        if (!isBlank(bp) && !bp.endsWith(DELIMITER)) {
            log.warn("Google bucket prefix ({}): {} should end with '{}': added automatically.", BUCKET_PREFIX_PROPERTY,
                    bp, DELIMITER);
            bp += DELIMITER;
        }
        if (isNotBlank(namespace)) {
            // use namespace as an additional prefix
            bp += namespace;
            if (!bp.endsWith(DELIMITER)) {
                bp += DELIMITER;
            }
        }
        bucketPrefix = bp;
    }

}
