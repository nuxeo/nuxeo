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
package org.nuxeo.ecm.blob.azure;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.blob.CloudBlobStoreConfiguration;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;

/**
 * Blob storage configuration in Azure Storage.
 *
 * @since 2023.6
 */
public class AzureBlobStoreConfiguration extends CloudBlobStoreConfiguration {

    protected static final String AZURE_STORAGE_ACCESS_KEY_ENV_VAR = "AZURE_STORAGE_ACCESS_KEY";

    protected static final String AZURE_STORAGE_ACCOUNT_ENV_VAR = "AZURE_STORAGE_ACCOUNT";

    protected static final Logger log = LogManager.getLogger(AzureBlobStoreConfiguration.class);

    public static final String ACCOUNT_KEY_PROPERTY = "account.key";

    public static final String ACCOUNT_NAME_PROPERTY = "account.name";

    public static final String AZURE_CDN_PROPERTY = "cdn.host";

    public static final String CONTAINER_PROPERTY = "container";

    public static final String PREFIX_PROPERTY = "prefix";

    public static final String SYSTEM_PROPERTY_PREFIX = "nuxeo.storage.azure";

    public static final String DELIMITER = "/";

    // Upload properties

    /**
     * @since 2025.10
     */
    public static final long BLOCK_SIZE_DEFAULT = (long) (4 * 1024 * 1024); // 4 MiB

    /**
     * @since 2025.10
     */
    public static final String BLOCK_SIZE_PROPERTY = "upload.blockSize";

    /**
     * @since 2025.10
     */
    public static final int MAX_CONCURRENCY_DEFAULT = 2;

    /**
     * @since 2025.10
     */
    public static final String MAX_CONCURRENCY_PROPERTY = "upload.maxConcurrency";

    /**
     * @since 2025.10
     */
    public static final long MAX_SINGLE_UPLOAD_SIZE_DEFAULT = (long) 8 * 1024 * 1024; // 8 MiB

    /**
     * @since 2025.10
     */
    public static final String MAX_SINGLE_UPLOAD_SIZE_PROPERTY = "upload.maxSingleUploadSize";

    /**
     * @since 2025.10
     */
    public static final Duration UPLOAD_TIMEOUT_DEFAULT = Duration.ofHours(2);

    /**
     * @since 2025.10
     */
    public static final String UPLOAD_TIMEOUT_PROPERTY = "upload.timeout";

    protected final long blockSize;

    protected final int maxConcurrency;

    protected final long maxSingleUploadSize;

    protected final Duration uploadTimeout;
    // End upload properties

    protected final String cdnHost;

    protected final String containerName;

    protected String prefix;

    protected final BlobContainerClient client;

    public AzureBlobStoreConfiguration(Map<String, String> properties) throws IOException {
        super(SYSTEM_PROPERTY_PREFIX, properties);
        if (StringUtils.isBlank(properties.get(ACCOUNT_KEY_PROPERTY))) {
            properties.put(ACCOUNT_NAME_PROPERTY, System.getenv(AZURE_STORAGE_ACCOUNT_ENV_VAR));
            properties.put(ACCOUNT_KEY_PROPERTY, System.getenv(AZURE_STORAGE_ACCESS_KEY_ENV_VAR));
        }
        cdnHost = getProperty(AZURE_CDN_PROPERTY);
        containerName = getProperty(CONTAINER_PROPERTY);
        String accountName = getProperty(ACCOUNT_NAME_PROPERTY);
        // accountName and containerName are conf properties, not user inputs, no need to sanitize
        String endpoint = String.format("https://%s.blob.core.windows.net/%s", accountName, containerName);
        client = new BlobContainerClientBuilder().endpoint(endpoint)
                                                 .credential(new StorageSharedKeyCredential(accountName,
                                                         getProperty(ACCOUNT_KEY_PROPERTY)))
                                                 .buildClient();
        client.createIfNotExists();
        prefix = StringUtils.defaultIfBlank(properties.get(PREFIX_PROPERTY), "");
        String delimiter = DELIMITER;
        if (StringUtils.isNotBlank(prefix) && !prefix.endsWith(delimiter)) {
            log.warn("Azure container prefix ({}): {} should end with '{}': added automatically.", PREFIX_PROPERTY,
                    prefix, delimiter);
            prefix += delimiter;
        }
        if (StringUtils.isNotBlank(namespace)) {
            // use namespace as an additional prefix
            prefix += namespace;
            if (!prefix.endsWith(delimiter)) {
                prefix += delimiter;
            }
        }
        blockSize = getOptionalLongProperty(BLOCK_SIZE_PROPERTY).orElse(BLOCK_SIZE_DEFAULT);
        maxConcurrency = getOptionalIntegerProperty(MAX_CONCURRENCY_PROPERTY).orElse(MAX_CONCURRENCY_DEFAULT);
        maxSingleUploadSize = getOptionalLongProperty(MAX_SINGLE_UPLOAD_SIZE_PROPERTY).orElse(
                MAX_SINGLE_UPLOAD_SIZE_DEFAULT);
        uploadTimeout = getOptionalDurationProperty(UPLOAD_TIMEOUT_PROPERTY).orElse(UPLOAD_TIMEOUT_DEFAULT);
    }

}
