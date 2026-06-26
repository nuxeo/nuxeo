/*
 * (C) Copyright 2019-2025 Nuxeo (http://nuxeo.com/) and others.
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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.ALLOW_BYTE_RANGE;
import static org.nuxeo.ecm.core.blob.BlobProviderDescriptor.RECORD;
import static org.nuxeo.ecm.core.model.BaseSession.isRetentionStrictMode;
import static software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode.COMPLIANCE;
import static software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode.GOVERNANCE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.ByteSize;
import org.nuxeo.ecm.blob.CloudBlobStoreConfiguration;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.PathStrategy;
import org.nuxeo.ecm.core.blob.PathStrategyFlat;
import org.nuxeo.ecm.core.blob.PathStrategySubDirs;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.aws.AWSConfigurationService;
import org.nuxeo.runtime.aws.NuxeoAWSRegionProvider;
import org.nuxeo.runtime.services.config.ConfigurationService;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3CrtAsyncClientBuilder;
import software.amazon.awssdk.services.s3.crt.S3CrtHttpConfiguration;
import software.amazon.awssdk.services.s3.model.BucketVersioningStatus;
import software.amazon.awssdk.services.s3.model.DefaultRetention;
import software.amazon.awssdk.services.s3.model.GetBucketVersioningResponse;
import software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationRequest;
import software.amazon.awssdk.services.s3.model.GetObjectLockConfigurationResponse;
import software.amazon.awssdk.services.s3.model.ObjectLockConfiguration;
import software.amazon.awssdk.services.s3.model.ObjectLockEnabled;
import software.amazon.awssdk.services.s3.model.ObjectLockRetentionMode;
import software.amazon.awssdk.services.s3.model.ObjectLockRule;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.StorageClass;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.utils.ThreadFactoryBuilder;
import software.amazon.encryption.s3.S3AsyncEncryptionClient;
import software.amazon.encryption.s3.S3EncryptionClient;
import software.amazon.encryption.s3.materials.KmsKeyring;

/**
 * Blob storage configuration in S3.
 *
 * @since 11.1
 */
public class S3BlobStoreConfiguration extends CloudBlobStoreConfiguration {

    private static final Logger log = LogManager.getLogger(S3BlobStoreConfiguration.class);

    public static final String SYSTEM_PROPERTY_PREFIX = "nuxeo.s3storage";

    public static final String BUCKET_NAME_PROPERTY = "bucket";

    public static final String BUCKET_PREFIX_PROPERTY = "bucket_prefix";

    public static final String BUCKET_SUB_DIRS_DEPTH_PROPERTY = "subDirsDepth";

    public static final String BUCKET_REGION_PROPERTY = "region";

    public static final String AWS_ID_PROPERTY = "awsid";

    public static final String AWS_SECRET_PROPERTY = "awssecret";

    public static final String AWS_SESSION_TOKEN_PROPERTY = "awstoken";

    public static final String CONCURRENCY_MAX_PROPERTY = "concurrency.max";

    public static final String TARGET_THROUGHPUT_IN_GBPS_PROPERTY = "targetThroughputInGbps";

    /** AWS ClientConfiguration default 50 */
    public static final String CONNECTION_MAX_PROPERTY = "connection.max";

    /** AWS ClientConfiguration default 3 (with exponential backoff) */
    public static final String CONNECTION_RETRY_PROPERTY = "connection.retry";

    /** AWS ClientConfiguration default 50*1000 = 50s */
    public static final String CONNECTION_TIMEOUT_PROPERTY = "connection.timeout";

    /** AWS ClientConfiguration default 50*1000 = 50s */
    public static final String SOCKET_TIMEOUT_PROPERTY = "socket.timeout";

    public static final String KEYSTORE_FILE_PROPERTY = "crypt.keystore.file";

    public static final String KEYSTORE_PASS_PROPERTY = "crypt.keystore.password";

    /**
     * @since 2023.18
     */
    public static final String KEYSTORE_LEGACY_MODE_PROPERTY = "crypt.keystore.legacymode";

    /**
     * @since 2025.0
     */
    public static final String KMS_LEGACY_MODE_PROPERTY = "crypt.kms.legacymode";

    public static final String SERVERSIDE_ENCRYPTION_PROPERTY = "crypt.serverside";

    public static final String SERVERSIDE_ENCRYPTION_KMS_KEY_PROPERTY = "crypt.kms.key";

    /**
     * @since 2023.17
     */
    public static final String CLIENTSIDE_ENCRYPTION_KMS_KEY_PROPERTY = "crypt.kms.clientside.key";

    /**
     * @since 2023.17
     */
    public static final String CLIENTSIDE_ENCRYPTION_KMS_REGION_PROPERTY = "crypt.kms.clientside.region";

    public static final String PRIVKEY_ALIAS_PROPERTY = "crypt.key.alias";

    public static final String PRIVKEY_PASS_PROPERTY = "crypt.key.password";

    public static final String ENDPOINT_PROPERTY = "endpoint";

    public static final String PATHSTYLEACCESS_PROPERTY = "pathstyleaccess";

    public static final String ACCELERATE_MODE_PROPERTY = "accelerateMode";

    public static final String DIRECTDOWNLOAD_PROPERTY_COMPAT = "downloadfroms3";

    public static final String DIRECTDOWNLOAD_EXPIRE_PROPERTY_COMPAT = "downloadfroms3.expire";

    public static final String METADATA_ADD_USERNAME_PROPERTY = "metadata.addusername";

    /**
     * Disable automatic abort of old multipart uploads at startup time.
     *
     * @since 11.1
     * @deprecated since 2025.0, unused
     */
    @Deprecated(since = "2025.0")
    public static final String MULTIPART_CLEANUP_DISABLED_PROPERTY = "multipart.cleanup.disabled";

    public static final String DELIMITER = "/";

    /**
     * The deprecated configuration property to define the multipart copy part size, for backward compatibility.
     *
     * @since 2021.11
     * @deprecated since 2025.0, merged with {@link #MINIMUM_UPLOAD_PART_SIZE_PROPERTY}
     */
    @Deprecated(since = "2025.0")
    public static final String MULTIPART_COPY_PART_SIZE_CONFIGURATION_PROPERTY = "nuxeo.s3.multipart.copy.part.size";

    /**
     * The Framework property to define the multipart copy part size.
     *
     * @deprecated since 2025.0, merged with {@link #MINIMUM_UPLOAD_PART_SIZE_PROPERTY}
     */
    @Deprecated(since = "2025.0")
    public static final String MULTIPART_COPY_PART_SIZE_PROPERTY = "multipart.copy.part.size";

    /**
     * The default value for the multipart copy part size.
     *
     * @since 2021.11
     * @deprecated since 2025.0, merged with {@link #MINIMUM_UPLOAD_PART_SIZE_DEFAULT}
     */
    @Deprecated(since = "2025.0")
    public static final long MULTIPART_COPY_PART_SIZE_DEFAULT = ByteSize.ofMebibytes(5).bytes();

    /**
     * The Framework property to define the multipart copy threshold.
     *
     * @since 2021.11
     * @deprecated since 2025.0, merged with {@link #MULTIPART_UPLOAD_THRESHOLD_PROPERTY}
     */
    @Deprecated(since = "2025.0")
    public static final String MULTIPART_COPY_THRESHOLD_PROPERTY = "multipart.copy.threshold";

    /**
     * The default value for the multipart copy threshold.
     * <p>
     * AWS SDK default.
     *
     * @since 2021.11
     * @deprecated since 2025.0, merged with {@link #MULTIPART_UPLOAD_THRESHOLD_DEFAULT}
     */
    @Deprecated(since = "2025.0")
    public static final long MULTIPART_COPY_THRESHOLD_DEFAULT = ByteSize.ofGibibytes(5).bytes();

    /**
     * The Framework property to define the multipart upload threshold.
     *
     * @since 2021.11
     */
    public static final String MULTIPART_UPLOAD_THRESHOLD_PROPERTY = "multipart.upload.threshold";

    /**
     * The default value for the multipart upload threshold.
     * <p>
     * AWS SDK default.
     *
     * @since 2021.11
     */
    public static final long MULTIPART_UPLOAD_THRESHOLD_DEFAULT = ByteSize.ofMebibytes(16).bytes();

    /**
     * The Framework property to define the minimum upload part size.
     *
     * @since 2021.11
     */
    public static final String MINIMUM_UPLOAD_PART_SIZE_PROPERTY = "minimum.upload.part.size";

    /**
     * The default value for the minimum upload part size.
     * <p>
     * AWS SDK default.
     *
     * @since 2021.11
     */
    public static final long MINIMUM_UPLOAD_PART_SIZE_DEFAULT = ByteSize.ofMebibytes(5).bytes();

    /**
     * The Framework property to define the transfer manager thread pool size.
     *
     * @since 2021.14
     */
    public static final String TRANSFER_MANAGER_THREAD_POOL_SIZE_PROPERTY = "transfer.manager.thread.pool.size";

    /**
     * The default value for the transfer manager thread pool size.
     *
     * @since 2021.14
     */
    public static final int TRANSFER_MANAGER_THREAD_POOL_SIZE_DEFAULT = 10;

    /**
     * Framework property to disable usage of the proxy environment variables ({@code nuxeo.http.proxy.*}) for the
     * connection to the S3 endpoint.
     *
     * @since 11.1
     */
    public static final String DISABLE_PROXY_PROPERTY = "nuxeo.s3.proxy.disabled";

    /** @since 2021.15 */
    public static final String USER_AGENT_PREFIX_PROPERTY = "userAgentPrefix";

    /** @since 2021.15 */
    public static final String USER_AGENT_SUFFIX_PROPERTY = "userAgentSuffix";

    public static final ObjectLockRetentionMode DEFAULT_RETENTION_MODE = ObjectLockRetentionMode.GOVERNANCE;

    /** @since 2025.8 */
    public static final String STORAGE_CLASS_PROPERTY = "storageClass";

    /** @since 2025.8 */
    public static final List<StorageClass> SUPPORTED_STORAGE_CLASS = List.of(StorageClass.STANDARD,
            StorageClass.INTELLIGENT_TIERING, StorageClass.GLACIER_IR, StorageClass.STANDARD_IA,
            StorageClass.ONEZONE_IA);

    public final CloudFrontConfiguration cloudFront;

    public S3Client amazonS3;

    public S3AsyncClient amazonS3Async;

    public final S3TransferManager transferManager;

    public final String bucketName;

    public final String bucketPrefix;

    public final boolean useServerSideEncryption;

    public final String serverSideKMSKeyID;

    protected boolean useClientSideEncryption;

    public final boolean metadataAddUsername;

    /**
     * Is Object Lock feature enabled at s3 level.
     *
     * @since 2021.13
     * @deprecated since 2025.8, use {@link CloudBlobStoreConfiguration#retentionEnabled} instead
     */
    @Deprecated(since = "2025.8", forRemoval = true)
    public final boolean s3RetentionEnabled;

    /**
     * The default storage class in s3.
     *
     * @since 2025.8
     */
    public final StorageClass storageClass;

    /**
     * The retention mode to use when setting the retention on an object.
     */
    public final ObjectLockRetentionMode retentionMode;

    /**
     * The path strategy according to the sub directory depth configuration.
     *
     * @since 2023.7
     */
    protected final PathStrategy pathStrategy;

    /**
     * Is backslash "/" the path separator.
     *
     * @since 2023.7
     */
    protected final boolean pathSeparatorIsBackslash;

    /**
     * @since 2025.0
     */
    protected AwsCredentialsProvider awsCredentialsProvider;

    /**
     * @since 2025.0
     */
    protected Region region;

    /**
     * @since 2025.0
     */
    protected URI endpointOverride;

    protected ByteSize minimumPartSize;

    protected ByteSize multipartUploadThreshold;

    protected KeyPair clientSideEncryptionKeyPair;

    protected int maxConnections;

    protected int maxConcurrency;

    protected double targetThroughputInGbps;

    protected int connectionTimeout;

    protected int socketTimeout;

    protected int maxErrorRetry;

    protected String userAgentPrefix;

    protected String userAgentSuffix;

    protected final boolean isBucketVersioningEnabled;

    public S3BlobStoreConfiguration(Map<String, String> properties) throws IOException {
        super(SYSTEM_PROPERTY_PREFIX, properties);
        cloudFront = new CloudFrontConfiguration(SYSTEM_PROPERTY_PREFIX, properties);

        bucketName = getBucketName();
        bucketPrefix = getBucketPrefix();

        var sseprop = getProperty(SERVERSIDE_ENCRYPTION_PROPERTY);
        if (isNotBlank(sseprop)) {
            useServerSideEncryption = Boolean.parseBoolean(sseprop);
            serverSideKMSKeyID = getProperty(SERVERSIDE_ENCRYPTION_KMS_KEY_PROPERTY);
        } else {
            useServerSideEncryption = false;
            serverSideKMSKeyID = null;
        }
        minimumPartSize = getOptionalByteSizeProperty(MINIMUM_UPLOAD_PART_SIZE_PROPERTY).orElseGet(
                () -> new ByteSize(MINIMUM_UPLOAD_PART_SIZE_DEFAULT));
        multipartUploadThreshold = getOptionalByteSizeProperty(MULTIPART_UPLOAD_THRESHOLD_PROPERTY).orElseGet(
                () -> new ByteSize(MULTIPART_UPLOAD_THRESHOLD_DEFAULT));
        maxConcurrency = getIntProperty(CONCURRENCY_MAX_PROPERTY);
        targetThroughputInGbps = getLongProperty(TARGET_THROUGHPUT_IN_GBPS_PROPERTY);
        maxConnections = getIntProperty(CONNECTION_MAX_PROPERTY);
        connectionTimeout = getIntProperty(CONNECTION_TIMEOUT_PROPERTY);
        socketTimeout = getIntProperty(SOCKET_TIMEOUT_PROPERTY);
        awsCredentialsProvider = initAwsCredentialsProvider();
        region = getBucketRegion();
        endpointOverride = initEnpoint();
        metadataAddUsername = getBooleanProperty(METADATA_ADD_USERNAME_PROPERTY);
        maxErrorRetry = getIntProperty(CONNECTION_RETRY_PROPERTY);
        userAgentPrefix = getProperty(USER_AGENT_PREFIX_PROPERTY);
        userAgentSuffix = getProperty(USER_AGENT_SUFFIX_PROPERTY);
        Path p = Paths.get(bucketPrefix);
        int subDirsDepth = getSubDirsDepth();
        if (subDirsDepth == 0) {
            // pathStrategy is not used when subDirsDepth=0 because a bucketPrefix could be in the key - NXP-30632
            pathStrategy = new PathStrategyFlat(p);
        } else {
            pathStrategy = new PathStrategySubDirs(p, subDirsDepth);
        }
        pathSeparatorIsBackslash = FileSystems.getDefault().getSeparator().equals("\\");

        amazonS3 = createSyncClient();
        amazonS3Async = createASyncClient();
        encryptClients();
        if (Boolean.parseBoolean(properties.get(RECORD))) {
            retentionMode = computeBucketRetentionMode();
            retentionEnabled = retentionMode != null;
            if (!retentionEnabled) {
                log.warn("Blob provider is configured for records but retention is not enabled on s3 bucket {}",
                        bucketName);
            } else {
                log.info("Computed bucket {}'s retention mode: {}", bucketName, retentionMode);
            }
        } else {
            retentionMode = null;
            retentionEnabled = false;
        }
        // For compat
        s3RetentionEnabled = retentionEnabled;

        isBucketVersioningEnabled = computeVersioningEnabled();

        var storageClassProperty = getProperty(STORAGE_CLASS_PROPERTY);
        if (isNotBlank(storageClassProperty)) {
            storageClass = StorageClass.fromValue(storageClassProperty.toUpperCase());
            if (!SUPPORTED_STORAGE_CLASS.contains(storageClass)) {
                throw new IllegalArgumentException("Unsupported S3 Storage Class: %s".formatted(storageClassProperty));
            }
        } else {
            storageClass = StorageClass.STANDARD;
        }
        log.info("Object will be stored with S3 {} storage class", storageClass);
        transferManager = createTransferManager();
    }

    @Override
    protected boolean isVersioningEnabled() {
        return isBucketVersioningEnabled;
    }

    protected boolean computeVersioningEnabled() {
        try {
            GetBucketVersioningResponse response = amazonS3.getBucketVersioning(b -> b.bucket(bucketName));
            // if versioning is suspended, created objects won't have versions
            return BucketVersioningStatus.ENABLED.equals(response.status());
        } catch (SdkServiceException e) {
            if (e.statusCode() == HttpStatus.SC_NOT_IMPLEMENTED) {
                // minio does not implement versioning
                log.warn("Versioning not implemented for bucket: {}: {}", () -> bucketName, e::getMessage);
                log.debug(e, e);
                return false;
            }
            throw e;
        }
    }

    protected S3Client createSyncClient() {
        ApacheHttpClient.Builder apacheBuilder = ApacheHttpClient.builder();
        if (maxConnections > 0) {
            apacheBuilder.maxConnections(maxConnections);
        }
        if (connectionTimeout >= 0) { // 0 is allowed
            var timeout = Duration.ofMillis(connectionTimeout);
            apacheBuilder.connectionTimeout(timeout);
        }
        if (socketTimeout >= 0) { // 0 is allowed
            apacheBuilder.socketTimeout(Duration.ofMillis(socketTimeout));
        }
        AWSConfigurationService service = Framework.getService(AWSConfigurationService.class);
        if (service != null) {
            if (Framework.isBooleanPropertyFalse(DISABLE_PROXY_PROPERTY)) {
                service.configureProxy(apacheBuilder);
            }
            service.configureSSL(apacheBuilder);
        }
        return S3Client.builder()
                       .region(region)
                       .credentialsProvider(awsCredentialsProvider)
                       .forcePathStyle(getBooleanProperty(PATHSTYLEACCESS_PROPERTY))
                       .accelerate(getBooleanProperty(ACCELERATE_MODE_PROPERTY))
                       .httpClient(apacheBuilder.build())
                       .endpointOverride(endpointOverride)
                       .overrideConfiguration(ocb -> {
                           if (isNotBlank(userAgentPrefix)) {
                               ocb.putAdvancedOption(SdkAdvancedClientOption.USER_AGENT_PREFIX, userAgentPrefix);
                           }
                           if (isNotBlank(userAgentSuffix)) {
                               ocb.putAdvancedOption(SdkAdvancedClientOption.USER_AGENT_SUFFIX, userAgentSuffix);
                           }
                           if (maxErrorRetry >= 0) { // 0 is allowed
                               ocb.retryStrategy(b -> b.maxAttempts(maxErrorRetry));
                           }
                       })
                       .build();
    }

    protected S3AsyncClient createASyncClient() {
        S3CrtAsyncClientBuilder s3AsyncClientBuilder = S3AsyncClient.crtBuilder(); // NOSONAR
        s3AsyncClientBuilder.region(region)
                            .credentialsProvider(awsCredentialsProvider)
                            .minimumPartSizeInBytes(minimumPartSize.bytes())
                            .thresholdInBytes(multipartUploadThreshold.bytes())
                            .forcePathStyle(getBooleanProperty(PATHSTYLEACCESS_PROPERTY))
                            .accelerate(getBooleanProperty(ACCELERATE_MODE_PROPERTY))
                            .endpointOverride(endpointOverride)
                            .httpConfiguration(b -> {
                                if (connectionTimeout >= 0) { // 0 is allowed
                                    var timeout = Duration.ofMillis(connectionTimeout);
                                    b.connectionTimeout(timeout);
                                }
                                AWSConfigurationService service = Framework.getService(AWSConfigurationService.class);
                                if (service != null && Framework.isBooleanPropertyFalse(DISABLE_PROXY_PROPERTY)) {
                                    configureProxy(b);
                                }
                            });
        if (maxConcurrency > 0) {
            s3AsyncClientBuilder.maxConcurrency(maxConcurrency);
        }
        if (targetThroughputInGbps > 0) {
            s3AsyncClientBuilder.targetThroughputInGbps(targetThroughputInGbps);
        }
        return s3AsyncClientBuilder.build();
    }

    /**
     * Enriches the given {@link S3CrtHttpConfiguration.Builder} with default proxy configuration.
     *
     * @param builder the http client builder
     * @since 2025.0
     */
    protected void configureProxy(S3CrtHttpConfiguration.Builder builder) {
        String proxyHost = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_HOST).toLowerCase();
        String proxyPort = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_PORT);
        String proxyLogin = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_LOGIN);
        String proxyPassword = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_PASSWORD);
        builder.proxyConfiguration(b -> {
            if (isNotBlank(proxyHost)) {
                b.host(proxyHost);
            }
            if (isNotBlank(proxyPort)) {
                b.port(Integer.parseInt(proxyPort));
            }
            if (isNotBlank(proxyLogin)) {
                b.username(proxyLogin);
            }
            if (proxyPassword != null) { // could be blank
                b.password(proxyPassword);
            }
        });
    }

    protected void encryptClients() {
        clientSideEncryptionKeyPair = getKeyPair();
        boolean useKeyStoreClientSideEncryption = clientSideEncryptionKeyPair != null;
        var clientSideKMSKeyId = getProperty(CLIENTSIDE_ENCRYPTION_KMS_KEY_PROPERTY);
        boolean useKMSClientSideEncryption = isNotBlank(clientSideKMSKeyId);
        useClientSideEncryption = useKeyStoreClientSideEncryption || useKMSClientSideEncryption;
        if (useClientSideEncryption) {
            var allowByteRange = getBooleanProperty(ALLOW_BYTE_RANGE);
            S3EncryptionClient.Builder syncb = S3EncryptionClient.builder() // NOSONAR
                                                                 .wrappedClient(amazonS3)
                                                                 .wrappedAsyncClient(amazonS3Async)
                                                                 // to download more than 64MB object
                                                                 .enableDelayedAuthenticationMode(true);
            S3AsyncEncryptionClient.Builder asyncb = S3AsyncEncryptionClient.builder()
                                                                            .wrappedClient(amazonS3Async)
                                                                            // to download more than 64MB object
                                                                            .enableDelayedAuthenticationMode(true);
            if (useKeyStoreClientSideEncryption) {
                var legacyMode = getBooleanProperty(KEYSTORE_LEGACY_MODE_PROPERTY);
                log.info("Client-side encryption enabled with local key store, legacy mode enabled: {}", legacyMode);
                syncb.rsaKeyPair(clientSideEncryptionKeyPair)
                     // for enabling legacy key wrapping modes
                     .enableLegacyWrappingAlgorithms(legacyMode)
                     // for enabling legacy content decryption modes
                     .enableLegacyUnauthenticatedModes(allowByteRange || legacyMode);
                asyncb.rsaKeyPair(clientSideEncryptionKeyPair)
                      // for enabling legacy key wrapping modes
                      .enableLegacyWrappingAlgorithms(legacyMode)
                      // for enabling legacy content decryption modes
                      .enableLegacyUnauthenticatedModes(allowByteRange || legacyMode);
            } else { // KMS client-side encryption
                var legacyMode = getBooleanProperty(KMS_LEGACY_MODE_PROPERTY);
                log.info("Client-side encryption enabled with KMS key id: {}, legacy mode enabled: {}",
                        clientSideKMSKeyId, legacyMode);
                Region kmsRegion;
                var customKMSRegion = getProperty(CLIENTSIDE_ENCRYPTION_KMS_REGION_PROPERTY);
                if (isNotBlank(customKMSRegion)) {
                    kmsRegion = Region.of(customKMSRegion);
                } else {
                    // If crypt.kms.clientside.region not specified, fallback on the bucket region
                    kmsRegion = region;
                }
                final KmsKeyring kmsKeyring = KmsKeyring.builder()
                                                        .wrappingKeyId(clientSideKMSKeyId)
                                                        .kmsClient(KmsClient.builder().region(kmsRegion).build()) // NOSONAR
                                                        .build();
                syncb.keyring(kmsKeyring)
                     // for enabling legacy key wrapping modes
                     .enableLegacyWrappingAlgorithms(legacyMode)
                     // for enabling legacy content decryption modes
                     .enableLegacyUnauthenticatedModes(allowByteRange || legacyMode);
                asyncb.keyring(kmsKeyring)
                      // for enabling legacy key wrapping modes
                      .enableLegacyWrappingAlgorithms(legacyMode)
                      // for enabling legacy content decryption modes
                      .enableLegacyUnauthenticatedModes(allowByteRange || legacyMode);
            }
            amazonS3 = syncb.build();
            amazonS3Async = asyncb.build();
        }
    }

    /**
     * Gets the s3 bucket's object lock default mode {@code ObjectLockRetentionMode#GOVERNANCE} or
     * {@code ObjectLockRetentionMode#COMPLIANCE}.
     * <p>
     * If undefined, the retention mode of the Nuxeo platform is returned.
     * <p>
     * Returns null if object lock is not enabled on the bucket.
     *
     * @since 2023.0
     */
    protected ObjectLockRetentionMode computeBucketRetentionMode() {
        GetObjectLockConfigurationRequest request = GetObjectLockConfigurationRequest.builder()
                                                                                     .bucket(bucketName)
                                                                                     .build();
        GetObjectLockConfigurationResponse response;
        ObjectLockConfiguration olc;
        try {
            response = amazonS3.getObjectLockConfiguration(request);
            olc = response.objectLockConfiguration();
            if (olc == null || !ObjectLockEnabled.ENABLED.toString().equals(olc.objectLockEnabledAsString())) {
                return null;
            }
            return Optional.ofNullable(olc.rule())
                           .map(ObjectLockRule::defaultRetention)
                           .map(DefaultRetention::mode)
                           .orElse(isRetentionStrictMode() ? COMPLIANCE : GOVERNANCE);
        } catch (S3Exception e) {
            if (e.statusCode() == HttpStatus.SC_NOT_FOUND) {
                return null;
            }
            throw new NuxeoException(e);
        }
    }

    /**
     * Returns a copy of the S3BlobStoreConfiguration with a different namespace.
     */
    public S3BlobStoreConfiguration withNamespace(String ns) throws IOException {
        return new S3BlobStoreConfiguration(propertiesWithNamespace(ns));
    }

    public void close() {
        amazonS3.close();
        amazonS3Async.close();
        transferManager.close();
    }

    /**
     * @since 2021.11
     * @deprecated since 2025.0, unused
     */
    @Deprecated(since = "2025.0", forRemoval = true)
    public static long getMultipartCopyPartSize() {
        // backward compatibility with configuration service property
        ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
        if (configurationService != null) {
            Optional<Long> optional = configurationService.getLong(MULTIPART_COPY_PART_SIZE_CONFIGURATION_PROPERTY);
            if (optional.isPresent()) {
                return optional.get();
            }
        }
        // nuxeo.conf property
        return getLongProperty(SYSTEM_PROPERTY_PREFIX + "." + MULTIPART_COPY_PART_SIZE_PROPERTY,
                MULTIPART_COPY_PART_SIZE_DEFAULT);
    }

    /**
     * @since 2021.11
     * @deprecated since 2025.11, use {@link #getOptionalLongProperty(String)} instead
     */
    @Deprecated(since = "2025.11", forRemoval = true)
    public static long getLongProperty(String key, long defaultValue) {
        var value = Framework.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            log.error("Invalid framework property: {}={}, expecting a long value.", key, value, e);
            return defaultValue;
        }
    }

    @Override
    protected boolean parseDirectDownload() {
        String directDownloadCompat = getProperty(DIRECTDOWNLOAD_PROPERTY_COMPAT);
        if (directDownloadCompat != null) {
            return Boolean.parseBoolean(directDownloadCompat);
        } else {
            return super.parseDirectDownload();
        }
    }

    @Override
    protected long parseDirectDownloadExpire() {
        int directDownloadExpireCompat = getIntProperty(DIRECTDOWNLOAD_EXPIRE_PROPERTY_COMPAT);
        if (directDownloadExpireCompat >= 0) {
            return directDownloadExpireCompat;
        } else {
            return super.parseDirectDownloadExpire();
        }
    }

    protected String getBucketName() {
        String bn = getProperty(BUCKET_NAME_PROPERTY);
        if (isBlank(bn)) {
            throw new NuxeoException("Missing configuration: " + BUCKET_NAME_PROPERTY);
        }
        return bn;
    }

    /**
     * Gets the bucket key for the given blob key taking into account the bucket prefix and the path strategy (sub
     * directory depth).
     *
     * @since 2023.7
     */
    protected String bucketKey(String key) {
        // this allows to retrieve blobs created with a bucketPrefix in the key - NXP-30632
        // this is a workaround for incorrectly written keys
        if (getSubDirsDepth() == 0) {
            return bucketPrefix + key;
        }
        String path = pathStrategy.getPathForKey(key).toString();
        if (pathSeparatorIsBackslash) {
            // correct for our abuse of Path under Windows
            path = path.replace("\\", DELIMITER);
        }
        return path;
    }

    protected String getBucketPrefix() {
        // bucket prefix is optional so we don't want to use the fallback mechanism to system properties,
        // as there may be a globally defined bucket prefix for another blob provider
        String value = properties.get(BUCKET_PREFIX_PROPERTY);
        if (isBlank(value)) {
            value = "";
        } else if (!value.endsWith(DELIMITER)) {
            log.debug("{} {} S3 bucket prefix should end with '/': added automatically.", BUCKET_PREFIX_PROPERTY,
                    value);
            value += DELIMITER;
        }
        if (isNotBlank(namespace)) {
            // use namespace as an additional prefix
            value += namespace;
            if (!value.endsWith(DELIMITER)) {
                value += DELIMITER;
            }
        }
        return value;
    }

    protected int getSubDirsDepth() {
        int d = getIntProperty(BUCKET_SUB_DIRS_DEPTH_PROPERTY);
        if (d < 0) {
            d = 0;
        }
        return d;
    }

    protected AwsCredentialsProvider initAwsCredentialsProvider() {
        String awsID = getProperty(AWS_ID_PROPERTY);
        String awsSecret = getProperty(AWS_SECRET_PROPERTY);
        String awsToken = getProperty(AWS_SESSION_TOKEN_PROPERTY);
        return S3Utils.getAwsCredentialsProvider(awsID, awsSecret, awsToken);
    }

    protected S3CrtHttpConfiguration getHttpConfiguration() {
        S3CrtHttpConfiguration.Builder builder = S3CrtHttpConfiguration.builder();
        builder.connectionTimeout(null);
        return builder.build();
    }

    protected KeyPair getKeyPair() {
        String keystoreFile = getProperty(KEYSTORE_FILE_PROPERTY);
        String keystorePass = getProperty(KEYSTORE_PASS_PROPERTY);
        String privkeyAlias = getProperty(PRIVKEY_ALIAS_PROPERTY);
        String privkeyPass = getProperty(PRIVKEY_PASS_PROPERTY);
        if (isBlank(keystoreFile)) {
            return null;
        }
        boolean confok = true;
        if (keystorePass == null) { // could be blank
            log.error("Keystore password missing");
            confok = false;
        }
        if (isBlank(privkeyAlias)) {
            log.error("Key alias missing");
            confok = false;
        }
        if (privkeyPass == null) { // could be blank
            log.error("Key password missing");
            confok = false;
        }
        if (!confok) {
            throw new NuxeoException("S3 Crypto configuration incomplete");
        }
        try {
            // Open keystore
            File ksFile = new File(keystoreFile);
            KeyStore keystore;
            try (FileInputStream ksStream = new FileInputStream(ksFile)) {
                keystore = KeyStore.getInstance(KeyStore.getDefaultType());
                keystore.load(ksStream, keystorePass.toCharArray());
            }
            // Get keypair for alias
            if (!keystore.isKeyEntry(privkeyAlias)) {
                throw new NuxeoException("Alias " + privkeyAlias + " is missing or not a key alias");
            }
            PrivateKey privKey = (PrivateKey) keystore.getKey(privkeyAlias, privkeyPass.toCharArray());
            Certificate cert = keystore.getCertificate(privkeyAlias);
            PublicKey pubKey = cert.getPublicKey();
            return new KeyPair(pubKey, privKey);
        } catch (IOException | GeneralSecurityException e) {
            throw new NuxeoException("Could not read keystore: " + keystoreFile + ", alias: " + privkeyAlias, e);
        }
    }

    protected Region getBucketRegion() {
        String bucketRegionProperty = getProperty(BUCKET_REGION_PROPERTY);
        if (isNotBlank(bucketRegionProperty)) {
            return Region.of(bucketRegionProperty);
        } else {
            return NuxeoAWSRegionProvider.getInstance().getRegion();
        }
    }

    protected URI initEnpoint() {
        String endpoint = getProperty(ENDPOINT_PROPERTY);
        if (isNotBlank(endpoint)) {
            try {
                return new URI(endpoint);
            } catch (URISyntaxException e) {
                throw new NuxeoException(e);
            }
        }
        return null;
    }

    protected S3TransferManager createTransferManager() {
        int threadPoolSize = getOptionalIntegerProperty(TRANSFER_MANAGER_THREAD_POOL_SIZE_PROPERTY).orElse(
                TRANSFER_MANAGER_THREAD_POOL_SIZE_DEFAULT);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(0, threadPoolSize, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1_000),
                new ThreadFactoryBuilder().threadNamePrefix("s3-transfer-manager-worker").build());
        executor.allowCoreThreadTimeOut(true);
        S3TransferManager.Builder tmBuilder = S3TransferManager.builder().s3Client(amazonS3Async);
        tmBuilder.executor(executor);
        return tmBuilder.build();
    }

    /** @deprecated since 11.4, unused */
    @Deprecated(since = "11.4")
    protected ObjectLockRetentionMode getRetentionMode() {
        return retentionMode;
    }

    /** @deprecated since 2023.0, unused */
    @Deprecated(since = "2023.0")
    protected boolean isS3RetentionEnabled() {
        return retentionEnabled;
    }

}
