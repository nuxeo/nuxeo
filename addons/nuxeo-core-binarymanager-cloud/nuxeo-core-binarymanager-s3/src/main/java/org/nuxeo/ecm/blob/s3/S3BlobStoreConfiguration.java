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

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.DISABLE_PROXY_PROPERTY;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.blob.CloudBlobStoreConfiguration;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.storage.sql.S3Utils;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.aws.NuxeoAWSRegionProvider;
import org.nuxeo.runtime.services.config.ConfigurationService;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Builder;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.StaticEncryptionMaterialsProvider;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;

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

    public static final String BUCKET_REGION_PROPERTY = "region";

    public static final String AWS_ID_PROPERTY = "awsid";

    public static final String AWS_SECRET_PROPERTY = "awssecret";

    public static final String AWS_SESSION_TOKEN_PROPERTY = "awstoken";

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

    public static final String SERVERSIDE_ENCRYPTION_PROPERTY = "crypt.serverside";

    public static final String SERVERSIDE_ENCRYPTION_KMS_KEY_PROPERTY = "crypt.kms.key";

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
     */
    public static final String MULTIPART_CLEANUP_DISABLED_PROPERTY = "multipart.cleanup.disabled";

    public static final String DELIMITER = "/";

    /**
     * The configuration property to define the multipart copy part size.
     */
    public static final String MULTIPART_COPY_PART_SIZE_PROPERTY = "nuxeo.s3.multipart.copy.part.size";

    /**
     * Framework property to disable usage of the proxy environment variables ({@code nuxeo.http.proxy.*}) for the
     * connection to the S3 endpoint.
     *
     * @since 11.1
     */
    public static final String DISABLE_PROXY_PROPERTY = "nuxeo.s3.proxy.disabled";

    public final CloudFrontConfiguration cloudFront;

    public final AmazonS3 amazonS3;

    public final TransferManager transferManager;

    public final String bucketName;

    public final String bucketPrefix;

    public final boolean useServerSideEncryption;

    public final String serverSideKMSKeyID;

    public final boolean useClientSideEncryption;

    public final boolean metadataAddUsername;

    public S3BlobStoreConfiguration(Map<String, String> properties) throws IOException {
        super(SYSTEM_PROPERTY_PREFIX, properties);
        cloudFront = new CloudFrontConfiguration(SYSTEM_PROPERTY_PREFIX, properties);

        bucketName = getBucketName();
        bucketPrefix = getBucketPrefix();

        String sseprop = getProperty(SERVERSIDE_ENCRYPTION_PROPERTY);
        if (isNotBlank(sseprop)) {
            useServerSideEncryption = Boolean.parseBoolean(sseprop);
            serverSideKMSKeyID = getProperty(SERVERSIDE_ENCRYPTION_KMS_KEY_PROPERTY);
        } else {
            useServerSideEncryption = false;
            serverSideKMSKeyID = null;
        }

        AWSCredentialsProvider awsCredentialsProvider = getAWSCredentialsProvider();
        ClientConfiguration clientConfiguration = getClientConfiguration();
        EncryptionMaterials encryptionMaterials = getEncryptionMaterials();
        useClientSideEncryption = encryptionMaterials != null;

        AmazonS3Builder<?, ?> s3Builder;
        if (useClientSideEncryption) {
            CryptoConfiguration cryptoConfiguration = new CryptoConfiguration();
            s3Builder = AmazonS3EncryptionClientBuilder.standard()
                                                       .withCredentials(awsCredentialsProvider)
                                                       .withClientConfiguration(clientConfiguration)
                                                       .withCryptoConfiguration(cryptoConfiguration)
                                                       .withEncryptionMaterials(new StaticEncryptionMaterialsProvider(
                                                               encryptionMaterials));
        } else {
            s3Builder = AmazonS3ClientBuilder.standard()
                                             .withCredentials(awsCredentialsProvider)
                                             .withClientConfiguration(clientConfiguration);
        }

        configurePathStyleAccess(s3Builder);
        configureRegionOrEndpoint(s3Builder);
        configureAccelerateMode(s3Builder);

        amazonS3 = getAmazonS3(s3Builder);

        transferManager = createTransferManager();

        abortOldUploads();

        metadataAddUsername = getBooleanProperty(METADATA_ADD_USERNAME_PROPERTY);
    }

    /**
     * Returns a copy of the S3BlobStoreConfiguration with a different namespace.
     */
    public S3BlobStoreConfiguration withNamespace(String ns) throws IOException {
        return new S3BlobStoreConfiguration(propertiesWithNamespace(ns));
    }

    public void close() {
        transferManager.shutdownNow();
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

    protected String getBucketPrefix() {
        // bucket prefix is optional so we don't want to use the fallback mechanism to system properties,
        // as there may be a globally defined bucket prefix for another blob provider
        String value = properties.get(BUCKET_PREFIX_PROPERTY);
        if (isBlank(value)) {
            value = "";
        } else if (!value.endsWith(DELIMITER)) {
            log.warn(String.format("%s %s S3 bucket prefix should end with '/': added automatically.",
                    BUCKET_PREFIX_PROPERTY, value));
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

    protected AWSCredentialsProvider getAWSCredentialsProvider() {
        String awsID = getProperty(AWS_ID_PROPERTY);
        String awsSecret = getProperty(AWS_SECRET_PROPERTY);
        String awsToken = getProperty(AWS_SESSION_TOKEN_PROPERTY);
        return S3Utils.getAWSCredentialsProvider(awsID, awsSecret, awsToken);
    }

    protected ClientConfiguration getClientConfiguration() {
        boolean proxyDisabled = Framework.isBooleanPropertyTrue(DISABLE_PROXY_PROPERTY);
        String proxyHost = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_HOST);
        String proxyPort = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_PORT);
        String proxyLogin = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_LOGIN);
        String proxyPassword = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_PASSWORD);
        int maxConnections = getIntProperty(CONNECTION_MAX_PROPERTY);
        int maxErrorRetry = getIntProperty(CONNECTION_RETRY_PROPERTY);
        int connectionTimeout = getIntProperty(CONNECTION_TIMEOUT_PROPERTY);
        int socketTimeout = getIntProperty(SOCKET_TIMEOUT_PROPERTY);
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        if (!proxyDisabled) {
            if (isNotBlank(proxyHost)) {
                clientConfiguration.setProxyHost(proxyHost);
            }
            if (isNotBlank(proxyPort)) {
                clientConfiguration.setProxyPort(Integer.parseInt(proxyPort));
            }
            if (isNotBlank(proxyLogin)) {
                clientConfiguration.setProxyUsername(proxyLogin);
            }
            if (proxyPassword != null) { // could be blank
                clientConfiguration.setProxyPassword(proxyPassword);
            }
        }
        if (maxConnections > 0) {
            clientConfiguration.setMaxConnections(maxConnections);
        }
        if (maxErrorRetry >= 0) { // 0 is allowed
            clientConfiguration.setMaxErrorRetry(maxErrorRetry);
        }
        if (connectionTimeout >= 0) { // 0 is allowed
            clientConfiguration.setConnectionTimeout(connectionTimeout);
        }
        if (socketTimeout >= 0) { // 0 is allowed
            clientConfiguration.setSocketTimeout(socketTimeout);
        }
        return clientConfiguration;
    }

    protected EncryptionMaterials getEncryptionMaterials() {
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
            KeyPair keypair = new KeyPair(pubKey, privKey);
            return new EncryptionMaterials(keypair);
        } catch (IOException | GeneralSecurityException e) {
            throw new NuxeoException("Could not read keystore: " + keystoreFile + ", alias: " + privkeyAlias, e);
        }
    }

    protected void configurePathStyleAccess(AmazonS3Builder<?, ?> s3Builder) {
        boolean pathStyleAccessEnabled = getBooleanProperty(PATHSTYLEACCESS_PROPERTY);
        if (pathStyleAccessEnabled) {
            log.debug("Path-style access enabled");
            s3Builder.enablePathStyleAccess();
        }
    }

    protected void configureRegionOrEndpoint(AmazonS3Builder<?, ?> s3Builder) {
        String bucketRegion = getProperty(BUCKET_REGION_PROPERTY);
        if (isBlank(bucketRegion)) {
            bucketRegion = NuxeoAWSRegionProvider.getInstance().getRegion();
        }
        String endpoint = getProperty(ENDPOINT_PROPERTY);
        if (isNotBlank(endpoint)) {
            s3Builder.withEndpointConfiguration(new EndpointConfiguration(endpoint, bucketRegion));
        } else {
            s3Builder.withRegion(bucketRegion);
        }
    }

    protected void configureAccelerateMode(AmazonS3Builder<?, ?> s3Builder) {
        boolean accelerateModeEnabled = getBooleanProperty(ACCELERATE_MODE_PROPERTY);
        if (accelerateModeEnabled) {
            log.debug("Accelerate mode enabled");
            s3Builder.enableAccelerateMode();
        }
    }

    protected AmazonS3 getAmazonS3(AmazonS3Builder<?, ?> s3Builder) {
        return s3Builder.build();
    }

    protected TransferManager createTransferManager() {
        long minimumUploadPartSize = 5L * 1024 * 1024; // AWS SDK default = 5 MB
        long multipartUploadThreshold = 16L * 1024 * 1024; // AWS SDK default = 16 MB
        long multipartCopyThreshold = 5L * 1024 * 1024 * 1024; // AWS SDK default = 5 GB
        long multipartCopyPartSize = 100L * 1024 * 1024; // AWS SDK default = 100 MB
        ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
        if (configurationService != null) {
            multipartCopyPartSize = configurationService.getLong(MULTIPART_COPY_PART_SIZE_PROPERTY,
                    multipartCopyPartSize);
        }
        return TransferManagerBuilder.standard()
                                     .withS3Client(amazonS3)
                                     .withMinimumUploadPartSize(Long.valueOf(minimumUploadPartSize))
                                     .withMultipartUploadThreshold(Long.valueOf(multipartUploadThreshold))
                                     .withMultipartCopyThreshold(Long.valueOf(multipartCopyThreshold))
                                     .withMultipartCopyPartSize(Long.valueOf(multipartCopyPartSize))
                                     .build();
    }

    /**
     * Aborts uploads that crashed and are older than 1 day.
     */
    protected void abortOldUploads() {
        if (getBooleanProperty(MULTIPART_CLEANUP_DISABLED_PROPERTY)) {
            log.debug("Cleanup of old multipart uploads is disabled");
            return;
        }
        // Async to avoid issues with transferManager.abortMultipartUploads taking a very long time.
        // See NXP-28571.
        new Thread(this::abortOldMultipartUploadsInternal, "Nuxeo-S3-abortOldMultipartUploads-" + bucketName).start();
    }

    // executed in a separate thread
    protected void abortOldMultipartUploadsInternal() {
        long oneDay = Duration.ofDays(1).toMillis();
        try {
            log.debug("Starting cleanup of old multipart uploads for bucket: {}", bucketName);
            Date oneDayAgo = new Date(System.currentTimeMillis() - oneDay);
            transferManager.abortMultipartUploads(bucketName, oneDayAgo);
            log.debug("Cleanup done for bucket: {}", bucketName);
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == 400 || e.getStatusCode() == 404) {
                log.warn("Aborting old uploads is not supported by this provider");
                return;
            }
            throw new NuxeoException("Failed to abort old uploads", e);
        }
    }

}
