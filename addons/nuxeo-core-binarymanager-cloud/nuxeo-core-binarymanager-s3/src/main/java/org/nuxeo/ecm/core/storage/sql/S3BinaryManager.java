/*
 * (C) Copyright 2011-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Mathieu Guillaume
 *     Florent Guillaume
 *     Luís Duarte
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.DISABLE_PROXY_PROPERTY;
import static org.nuxeo.ecm.blob.s3.S3BlobStoreConfiguration.MULTIPART_CLEANUP_DISABLED_PROPERTY;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.blob.AbstractBinaryGarbageCollector;
import org.nuxeo.ecm.blob.AbstractCloudBinaryManager;
import org.nuxeo.ecm.blob.s3.S3ManagedTransfer;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.FileStorage;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.aws.AWSConfigurationService;
import org.nuxeo.runtime.aws.NuxeoAWSRegionProvider;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Builder;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3EncryptionClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.EncryptedPutObjectRequest;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.S3VersionSummary;
import com.amazonaws.services.s3.model.SSEAwsKeyManagementParams;
import com.amazonaws.services.s3.model.StaticEncryptionMaterialsProvider;
import com.amazonaws.services.s3.model.VersionListing;
import com.amazonaws.services.s3.transfer.Copy;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.TransferManagerBuilder;
import com.amazonaws.services.s3.transfer.Upload;

/**
 * A Binary Manager that stores binaries as S3 BLOBs
 * <p>
 * The BLOBs are cached locally on first access for efficiency.
 * <p>
 * Because the BLOB length can be accessed independently of the binary stream, it is also cached in a simple text file
 * if accessed before the stream.
 */
public class S3BinaryManager extends AbstractCloudBinaryManager implements S3ManagedTransfer {

    private static final Log log = LogFactory.getLog(S3BinaryManager.class);

    public static final String SYSTEM_PROPERTY_PREFIX = "nuxeo.s3storage";

    public static final String BUCKET_NAME_PROPERTY = "bucket";

    public static final String BUCKET_PREFIX_PROPERTY = "bucket_prefix";

    public static final String BUCKET_REGION_PROPERTY = "region";

    public static final String AWS_ID_PROPERTY = "awsid";

    public static final String AWS_SECRET_PROPERTY = "awssecret";

    /**
     * @since 10.10
     */
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

    /**
     * @since 10.3
     */
    public static final String PATHSTYLEACCESS_PROPERTY = "pathstyleaccess";

    /** @since 11.1 */
    public static final String ACCELERATE_MODE_PROPERTY = "accelerateMode";

    public static final String DIRECTDOWNLOAD_PROPERTY_COMPAT = "downloadfroms3";

    public static final String DIRECTDOWNLOAD_EXPIRE_PROPERTY_COMPAT = "downloadfroms3.expire";

    public static final String DELIMITER = "/";

    /** @deprecated since 11.1, now unused */
    @Deprecated
    private static final Pattern MD5_RE = Pattern.compile("[0-9a-f]{32}");

    protected String bucketName;

    protected String bucketNamePrefix;

    protected AWSCredentialsProvider awsCredentialsProvider;

    protected ClientConfiguration clientConfiguration;

    protected EncryptionMaterials encryptionMaterials;

    protected boolean isEncrypted;

    protected CryptoConfiguration cryptoConfiguration;

    protected boolean useServerSideEncryption;

    protected String serverSideKMSKeyID;

    protected AmazonS3 amazonS3;

    protected TransferManager transferManager;

    @Override
    public void close() {
        // this also shuts down the AmazonS3Client
        transferManager.shutdownNow();
        super.close();
    }

    /**
     * Aborts uploads that crashed and are older than 1 day.
     *
     * @since 7.2
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
        int oneDay = 1000 * 60 * 60 * 24;
        try {
            log.debug("Starting cleanup of old multipart uploads for bucket: " + bucketName);
            transferManager.abortMultipartUploads(bucketName, new Date(System.currentTimeMillis() - oneDay));
            log.debug("Cleanup done for bucket: " + bucketName);
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 400 || e.getStatusCode() == 404) {
                log.error("Your cloud provider does not support aborting old uploads");
                return;
            }
            throw new NuxeoException("Failed to abort old uploads", e);
        }
    }

    @Override
    protected void setupCloudClient() throws IOException {
        // Get settings from the configuration
        bucketName = getProperty(BUCKET_NAME_PROPERTY);
        // as bucket prefix is optional we don't want to use the fallback mechanism
        bucketNamePrefix = StringUtils.defaultString(properties.get(BUCKET_PREFIX_PROPERTY));
        String bucketRegion = getProperty(BUCKET_REGION_PROPERTY);
        if (isBlank(bucketRegion)) {
            bucketRegion = NuxeoAWSRegionProvider.getInstance().getRegion();
        }
        String awsID = getProperty(AWS_ID_PROPERTY);
        String awsSecret = getProperty(AWS_SECRET_PROPERTY);
        String awsToken = getProperty(AWS_SESSION_TOKEN_PROPERTY);

        boolean proxyDisabled = Framework.isBooleanPropertyTrue(DISABLE_PROXY_PROPERTY);
        String proxyHost = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_HOST);
        String proxyPort = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_PORT);
        String proxyLogin = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_LOGIN);
        String proxyPassword = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_PASSWORD);

        int maxConnections = getIntProperty(CONNECTION_MAX_PROPERTY);
        int maxErrorRetry = getIntProperty(CONNECTION_RETRY_PROPERTY);
        int connectionTimeout = getIntProperty(CONNECTION_TIMEOUT_PROPERTY);
        int socketTimeout = getIntProperty(SOCKET_TIMEOUT_PROPERTY);

        String keystoreFile = getProperty(KEYSTORE_FILE_PROPERTY);
        String keystorePass = getProperty(KEYSTORE_PASS_PROPERTY);
        String privkeyAlias = getProperty(PRIVKEY_ALIAS_PROPERTY);
        String privkeyPass = getProperty(PRIVKEY_PASS_PROPERTY);
        String endpoint = getProperty(ENDPOINT_PROPERTY);
        boolean accelerateModeEnabled = getBooleanProperty(ACCELERATE_MODE_PROPERTY);
        boolean pathStyleAccessEnabled = getBooleanProperty(PATHSTYLEACCESS_PROPERTY);
        String sseprop = getProperty(SERVERSIDE_ENCRYPTION_PROPERTY);
        if (isNotBlank(sseprop)) {
            useServerSideEncryption = Boolean.parseBoolean(sseprop);
            serverSideKMSKeyID = getProperty(SERVERSIDE_ENCRYPTION_KMS_KEY_PROPERTY);
        }

        if (isBlank(bucketName)) {
            throw new RuntimeException("Missing conf: " + BUCKET_NAME_PROPERTY);
        }

        if (!isBlank(bucketNamePrefix) && !bucketNamePrefix.endsWith(DELIMITER)) {
            log.debug(String.format("%s %s S3 bucket prefix should end with '/' : added automatically.",
                    BUCKET_PREFIX_PROPERTY, bucketNamePrefix));
            bucketNamePrefix += DELIMITER;
        }
        if (isNotBlank(namespace)) {
            // use namespace as an additional prefix
            bucketNamePrefix += namespace;
            if (!bucketNamePrefix.endsWith(DELIMITER)) {
                bucketNamePrefix += DELIMITER;
            }
        }

        // set up credentials
        awsCredentialsProvider = S3Utils.getAWSCredentialsProvider(awsID, awsSecret, awsToken);

        // set up client configuration
        clientConfiguration = new ClientConfiguration();
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

        AWSConfigurationService service = Framework.getService(AWSConfigurationService.class);
        if (service != null) {
            service.configureSSL(clientConfiguration);
        }

        // set up encryption
        encryptionMaterials = null;
        if (isNotBlank(keystoreFile)) {
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
                throw new RuntimeException("S3 Crypto configuration incomplete");
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
                    throw new RuntimeException("Alias " + privkeyAlias + " is missing or not a key alias");
                }
                PrivateKey privKey = (PrivateKey) keystore.getKey(privkeyAlias, privkeyPass.toCharArray());
                Certificate cert = keystore.getCertificate(privkeyAlias);
                PublicKey pubKey = cert.getPublicKey();
                KeyPair keypair = new KeyPair(pubKey, privKey);
                // Get encryptionMaterials from keypair
                encryptionMaterials = new EncryptionMaterials(keypair);
                cryptoConfiguration = new CryptoConfiguration();
            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException("Could not read keystore: " + keystoreFile + ", alias: " + privkeyAlias, e);
            }
        }
        isEncrypted = encryptionMaterials != null;

        AmazonS3Builder<?, ?> s3Builder;
        // Try to create bucket if it doesn't exist
        if (!isEncrypted) {
            s3Builder = AmazonS3ClientBuilder.standard()
                                             .withCredentials(awsCredentialsProvider)
                                             .withClientConfiguration(clientConfiguration);

        } else {
            s3Builder = AmazonS3EncryptionClientBuilder.standard()
                                                       .withClientConfiguration(clientConfiguration)
                                                       .withCryptoConfiguration(cryptoConfiguration)
                                                       .withCredentials(awsCredentialsProvider)
                                                       .withEncryptionMaterials(new StaticEncryptionMaterialsProvider(
                                                               encryptionMaterials));
        }
        if (pathStyleAccessEnabled) {
            log.debug("Path-style access enabled");
            s3Builder.enablePathStyleAccess();
        }
        if (isNotBlank(endpoint)) {
            s3Builder = s3Builder.withEndpointConfiguration(new EndpointConfiguration(endpoint, bucketRegion));
        } else {
            s3Builder = s3Builder.withRegion(bucketRegion);
        }
        s3Builder.setAccelerateModeEnabled(accelerateModeEnabled);

        amazonS3 = s3Builder.build();

        try {
            if (!amazonS3.doesBucketExist(bucketName)) {
                amazonS3.createBucket(bucketName);
                amazonS3.setBucketAcl(bucketName, CannedAccessControlList.Private);
            }
        } catch (AmazonClientException e) {
            throw new IOException(e);
        }

        // compat for NXP-17895, using "downloadfroms3", to be removed
        // these two fields have already been initialized by the base class initialize()
        // using standard property "directdownload"
        String dd = getProperty(DIRECTDOWNLOAD_PROPERTY_COMPAT);
        if (dd != null) {
            directDownload = Boolean.parseBoolean(dd);
        }
        int dde = getIntProperty(DIRECTDOWNLOAD_EXPIRE_PROPERTY_COMPAT);
        if (dde >= 0) {
            directDownloadExpire = dde;
        }

        transferManager = TransferManagerBuilder.standard().withS3Client(amazonS3).build();
        abortOldUploads();
    }

    @Override
    public TransferManager getTransferManager() {
        return transferManager;
    }

    protected void removeBinary(String digest) {
        amazonS3.deleteObject(bucketName, bucketNamePrefix + digest);
    }

    /**
     * INTERNAL (TESTS). Clears the binary manager of all its data.
     *
     * @since 11.5
     */
    public void clear() {
        ObjectListing list = null;
        do {
            if (list == null) {
                // use delimiter to avoid useless listing of objects in "subdirectories"
                ListObjectsRequest listObjectsRequest = //
                        new ListObjectsRequest().withBucketName(bucketName)
                                                .withPrefix(bucketNamePrefix)
                                                .withDelimiter("/");
                list = amazonS3.listObjects(listObjectsRequest);
            } else {
                list = amazonS3.listNextBatchOfObjects(list);
            }
            for (S3ObjectSummary summary : list.getObjectSummaries()) {
                amazonS3.deleteObject(bucketName, summary.getKey());
            }
        } while (list.isTruncated());
        VersionListing vlist = null;
        do {
            if (vlist == null) {
                ListVersionsRequest listVersionsRequest = //
                        new ListVersionsRequest().withBucketName(bucketName)
                                                 .withPrefix(bucketNamePrefix)
                                                 .withDelimiter("/");
                vlist = amazonS3.listVersions(listVersionsRequest);
            } else {
                vlist = amazonS3.listNextBatchOfVersions(vlist);

            }
            for (S3VersionSummary vsummary : vlist.getVersionSummaries()) {
                amazonS3.deleteVersion(bucketName, vsummary.getKey(), vsummary.getVersionId());
            }
        } while (vlist.isTruncated());
    }

    @Override
    protected String getSystemPropertyPrefix() {
        return SYSTEM_PROPERTY_PREFIX;
    }

    @Override
    protected BinaryGarbageCollector instantiateGarbageCollector() {
        return new S3BinaryGarbageCollector(this);
    }

    @Override
    public void removeBinaries(Collection<String> digests) {
        digests.forEach(this::removeBinary);
    }

    /** @return object length, or -1 if missing */
    public long lengthOfBlob(String digest) {
        String bucketKey = bucketNamePrefix + digest;
        try {
            ObjectMetadata metadata = amazonS3.getObjectMetadata(bucketName, bucketKey);
            return metadata.getContentLength();
        } catch (AmazonServiceException e) {
            if (isMissingKey(e)) {
                return -1;
            }
            throw e;
        }
    }

    protected static boolean isMissingKey(AmazonClientException e) {
        if (e instanceof AmazonServiceException) {
            AmazonServiceException ase = (AmazonServiceException) e;
            return (ase.getStatusCode() == 404) || "NoSuchKey".equals(ase.getErrorCode())
                    || "Not Found".equals(e.getMessage());
        }
        return false;
    }

    /** @deprecated since 11.1, now unused */
    @Deprecated
    public static boolean isMD5(String digest) {
        return MD5_RE.matcher(digest).matches();
    }

    /**
     * Used in the healthCheck; the transferManager should be initialized and the bucket accessible
     *
     * @since 9.3
     */
    public boolean canAccessBucket() {
        return transferManager != null && transferManager.getAmazonS3Client().doesBucketExist(bucketName);
    }

    @Override
    protected FileStorage getFileStorage() {
        return new S3FileStorage();
    }

    /**
     * Gets the AWSCredentialsProvider.
     *
     * @since 10.2
     */
    public AWSCredentialsProvider getAwsCredentialsProvider() {
        return awsCredentialsProvider;
    }

    /**
     * Gets AmazonS3.
     *
     * @since 10.2
     */
    public AmazonS3 getAmazonS3() {
        return amazonS3;
    }

    /**
     * Gets the bucket name.
     *
     * @since 11.1
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Gets the bucket prefix.
     *
     * @since 11.1
     */
    public String getBucketPrefix() {
        return bucketNamePrefix;
    }

    @Override
    public String writeBlob(Blob blob) throws IOException {
        // Attempt to do S3 Copy if the Source Blob provider is also S3
        if (blob instanceof ManagedBlob) {
            ManagedBlob managedBlob = (ManagedBlob) blob;
            BlobProvider blobProvider = Framework.getService(BlobManager.class)
                                                 .getBlobProvider(managedBlob.getProviderId());
            if (blobProvider instanceof S3BinaryManager) {
                S3BinaryManager sourceBlobProvider = (S3BinaryManager) blobProvider;
                if (getDigestAlgorithm().equals(sourceBlobProvider.getDigestAlgorithm())) {
                    String key = managedBlob.getKey();
                    int colon = key.indexOf(':');
                    if (colon >= 0) {
                        key = key.substring(colon + 1);
                    }
                    if (isValidDigest(key)) {
                        String digest = copyBlob(sourceBlobProvider, key);
                        if (digest != null) {
                            return digest;
                        }
                    }
                }
            }
        }
        return super.writeBlob(blob);
    }

    /**
     * Copies a blob. Returns {@code null} if the copy was not possible.
     *
     * @param sourceBlobProvider the source blob provider
     * @param digest the source blob key
     * @return the copied blob key, or {@code null} if the copy was not possible
     * @throws IOException
     * @since 10.1
     */
    protected String copyBlob(S3BinaryManager sourceBlobProvider, String digest) throws IOException {
        String sourceBucketName = sourceBlobProvider.bucketName;
        String sourceKey = sourceBlobProvider.bucketNamePrefix + digest;
        String key = bucketNamePrefix + digest;
        long t0 = 0;
        if (log.isDebugEnabled()) {
            t0 = System.currentTimeMillis();
            log.debug("copying blob " + sourceKey + " to " + key);
        }

        try {
            amazonS3.getObjectMetadata(bucketName, key);
            if (log.isDebugEnabled()) {
                log.debug("blob " + key + " is already in S3");
            }
            return digest;
        } catch (AmazonServiceException e) {
            if (!isMissingKey(e)) {
                throw new IOException(e);
            }
            // object does not exist, just continue
        }

        // not already present -> copy the blob
        ObjectMetadata sourceMetadata;
        try {
            sourceMetadata = amazonS3.getObjectMetadata(sourceBucketName, sourceKey);
        } catch (AmazonServiceException e) {
            throw new NuxeoException("Source blob does not exists: s3://" + sourceBucketName + "/" + sourceKey, e);
        }
        long length = sourceMetadata.getContentLength();
        try {
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(sourceBucketName, sourceKey, bucketName, key);
            if (useServerSideEncryption) {
                if (isNotBlank(serverSideKMSKeyID)) { // TODO
                    log.warn("S3 copy not supported with KMS, falling back to regular copy");
                    return null;
                }
                // SSE-S3
                ObjectMetadata newObjectMetadata = new ObjectMetadata();
                newObjectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
                copyObjectRequest.setNewObjectMetadata(newObjectMetadata);
            }
            Copy copy = transferManager.copy(copyObjectRequest);
            try {
                copy.waitForCompletion();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NuxeoException(e);
            }
            if (log.isDebugEnabled()) {
                long dtms = System.currentTimeMillis() - t0;
                log.debug("copied blob " + sourceKey + " to " + key + " in " + dtms + "ms");
            }
            return digest;
        } catch (AmazonServiceException e) {
            String message = "S3 copy not supported from s3://" + sourceBucketName + "/" + sourceKey + " to s3://"
                    + bucketName + "/" + key + " (" + length + " bytes)";
            log.warn(message + ", falling back to regular copy: " + e.getMessage());
            log.debug(message, e);
            return null;
        }
    }

    public class S3FileStorage implements FileStorage {

        @Override
        public void storeFile(String digest, File file) throws IOException {
            long t0 = 0;
            if (log.isDebugEnabled()) {
                t0 = System.currentTimeMillis();
                log.debug("storing blob " + digest + " to S3");
            }
            String key = bucketNamePrefix + digest;
            try {
                amazonS3.getObjectMetadata(bucketName, key);
                if (log.isDebugEnabled()) {
                    log.debug("blob " + digest + " is already in S3");
                }
            } catch (AmazonClientException e) {
                if (!isMissingKey(e)) {
                    throw new IOException(e);
                }
                // not already present -> store the blob
                PutObjectRequest request;
                if (!isEncrypted) {
                    request = new PutObjectRequest(bucketName, key, file);
                    if (useServerSideEncryption) {
                        ObjectMetadata objectMetadata = new ObjectMetadata();
                        if (isNotBlank(serverSideKMSKeyID)) {
                            SSEAwsKeyManagementParams keyManagementParams = new SSEAwsKeyManagementParams(
                                    serverSideKMSKeyID);
                            request = request.withSSEAwsKeyManagementParams(keyManagementParams);
                        } else {
                            objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
                        }
                        request.setMetadata(objectMetadata);
                    }
                } else {
                    request = new EncryptedPutObjectRequest(bucketName, key, file);
                }
                Upload upload = transferManager.upload(request);
                try {
                    upload.waitForUploadResult();
                } catch (AmazonClientException ee) {
                    throw new IOException(ee);
                } catch (InterruptedException ee) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ee);
                } finally {
                    if (log.isDebugEnabled()) {
                        long dtms = System.currentTimeMillis() - t0;
                        log.debug("stored blob " + digest + " to S3 in " + dtms + "ms");
                    }
                }
            }
        }

        @Override
        public boolean fetchFile(String digest, File file) throws IOException {
            long t0 = 0;
            if (log.isDebugEnabled()) {
                t0 = System.currentTimeMillis();
                log.debug("fetching blob " + digest + " from S3");
            }
            try {
                Download download = transferManager.download(
                        new GetObjectRequest(bucketName, bucketNamePrefix + digest), file);
                download.waitForCompletion();
                return true;
            } catch (AmazonClientException e) {
                if (!isMissingKey(e)) {
                    throw new IOException(e);
                }
                return false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } finally {
                if (log.isDebugEnabled()) {
                    long dtms = System.currentTimeMillis() - t0;
                    log.debug("fetched blob " + digest + " from S3 in " + dtms + "ms");
                }
            }

        }
    }

    /**
     * Garbage collector for S3 binaries that stores the marked (in use) binaries in memory.
     */
    public static class S3BinaryGarbageCollector extends AbstractBinaryGarbageCollector<S3BinaryManager> {

        protected S3BinaryGarbageCollector(S3BinaryManager binaryManager) {
            super(binaryManager);
        }

        @Override
        public String getId() {
            return "s3:" + binaryManager.bucketName;
        }

        @Override
        public void computeToDelete() {
            // list S3 objects in the bucket
            toDelete = new HashSet<>();
            ObjectListing list = null;
            do {
                if (list == null) {
                    // use delimiter to avoid useless listing of objects in "subdirectories"
                    ListObjectsRequest listObjectsRequest = new ListObjectsRequest(binaryManager.bucketName,
                            binaryManager.bucketNamePrefix, null, DELIMITER, null);
                    list = binaryManager.amazonS3.listObjects(listObjectsRequest);
                } else {
                    list = binaryManager.amazonS3.listNextBatchOfObjects(list);
                }
                int prefixLength = binaryManager.bucketNamePrefix.length();
                for (S3ObjectSummary summary : list.getObjectSummaries()) {
                    String digest = summary.getKey().substring(prefixLength);
                    if (!binaryManager.isValidDigest(digest)) {
                        // ignore files that cannot be digests, for safety
                        continue;
                    }
                    long length = summary.getSize();
                    status.sizeBinaries += length;
                    status.numBinaries++;
                    toDelete.add(digest);
                }
            } while (list.isTruncated());
        }

        @Override
        protected void removeUnmarkedBlobsAndUpdateStatus(boolean delete) {
            for (String digest : toDelete) {
                long length = binaryManager.lengthOfBlob(digest);
                if (length < 0) {
                    // shouldn't happen except if blob concurrently removed
                    continue;
                }
                status.sizeBinariesGC += length;
                status.numBinariesGC++;
                status.sizeBinaries -= length;
                status.numBinaries--;
                if (delete) {
                    binaryManager.removeBinary(digest);
                }
            }
        }
    }

    // ******************** BlobProvider ********************

    @Override
    protected URI getRemoteUri(String digest, ManagedBlob blob, HttpServletRequest servletRequest) throws IOException {
        String key = bucketNamePrefix + digest;
        Date expiration = new Date();
        expiration.setTime(expiration.getTime() + directDownloadExpire * 1000);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.GET);
        request.addRequestParameter("response-content-type", getContentTypeHeader(blob));
        request.addRequestParameter("response-content-disposition", getContentDispositionHeader(blob, null));
        request.setExpiration(expiration);
        URL url = amazonS3.generatePresignedUrl(request);
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

}
