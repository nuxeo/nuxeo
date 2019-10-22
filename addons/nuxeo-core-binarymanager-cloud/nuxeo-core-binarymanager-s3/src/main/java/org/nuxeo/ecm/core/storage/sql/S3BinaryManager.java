/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Mathieu Guillaume
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.blob.AbstractBinaryGarbageCollector;
import org.nuxeo.ecm.blob.AbstractCloudBinaryManager;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.blob.BlobManager.BlobInfo;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.core.blob.binary.BinaryBlobProvider;
import org.nuxeo.ecm.core.blob.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.blob.binary.FileStorage;
import org.nuxeo.ecm.core.model.Document;
import org.nuxeo.runtime.api.Framework;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import com.amazonaws.services.s3.internal.ServiceUtils;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.EncryptedPutObjectRequest;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.StaticEncryptionMaterialsProvider;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import com.google.common.base.MoreObjects;

/**
 * A Binary Manager that stores binaries as S3 BLOBs
 * <p>
 * The BLOBs are cached locally on first access for efficiency.
 * <p>
 * Because the BLOB length can be accessed independently of the binary stream, it is also cached in a simple text file
 * if accessed before the stream.
 */
public class S3BinaryManager extends AbstractCloudBinaryManager {

    private static final String MD5 = "MD5"; // must be MD5 for Etag

    @Override
    protected String getDefaultDigestAlgorithm() {
        return MD5;
    }

    private static final Log log = LogFactory.getLog(S3BinaryManager.class);

    public static final String SYSTEM_PROPERTY_PREFIX = "nuxeo.s3storage";

    public static final String BUCKET_NAME_PROPERTY = "bucket";

    public static final String BUCKET_PREFIX_PROPERTY = "bucket_prefix";

    public static final String BUCKET_REGION_PROPERTY = "region";

    public static final String DEFAULT_BUCKET_REGION = null; // US East

    public static final String AWS_ID_PROPERTY = "awsid";

    public static final String AWS_ID_ENV = "AWS_ACCESS_KEY_ID";

    public static final String AWS_SECRET_PROPERTY = "awssecret";

    public static final String AWS_SECRET_ENV = "AWS_SECRET_ACCESS_KEY";

    /** Use a proxy or not for an internal server S3 compatible */
    public static final String CONNECTION_PROXY = "nuxeo.s3storage.connection.proxy";

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

    public static final String PRIVKEY_ALIAS_PROPERTY = "crypt.key.alias";

    public static final String PRIVKEY_PASS_PROPERTY = "crypt.key.password";

    public static final String ENDPOINT_PROPERTY = "endpoint";

    public static final String DIRECTDOWNLOAD_PROPERTY_COMPAT = "downloadfroms3";

    public static final String DIRECTDOWNLOAD_EXPIRE_PROPERTY_COMPAT = "downloadfroms3.expire";

    private static final Pattern MD5_RE = Pattern.compile("[0-9a-f]{32}");

    protected String bucketName;

    protected String bucketNamePrefix;

    protected AWSCredentialsProvider awsCredentialsProvider;

    protected ClientConfiguration clientConfiguration;

    protected EncryptionMaterials encryptionMaterials;

    protected boolean isEncrypted;

    protected CryptoConfiguration cryptoConfiguration;

    protected boolean userServerSideEncryption;

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
    protected void abortOldUploads() throws IOException {
        int oneDay = 1000 * 60 * 60 * 24;
        try {
            transferManager.abortMultipartUploads(bucketName, new Date(System.currentTimeMillis() - oneDay));
        } catch (AmazonClientException e) {
            throw new IOException("Failed to abort old uploads", e);
        }
    }

    @Override
    protected void setupCloudClient() throws IOException {
        // Get settings from the configuration
        bucketName = getProperty(BUCKET_NAME_PROPERTY);
        bucketNamePrefix = MoreObjects.firstNonNull(getProperty(BUCKET_PREFIX_PROPERTY), StringUtils.EMPTY);
        String bucketRegion = getProperty(BUCKET_REGION_PROPERTY);
        if (isBlank(bucketRegion)) {
            bucketRegion = DEFAULT_BUCKET_REGION;
        }
        String awsID = getProperty(AWS_ID_PROPERTY);
        String awsSecret = getProperty(AWS_SECRET_PROPERTY);

        String proxyHost = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_HOST);
        String proxyPort = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_PORT);
        String proxyLogin = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_LOGIN);
        String proxyPassword = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_PASSWORD);

        int maxConnections = getIntProperty(CONNECTION_MAX_PROPERTY);
        int maxErrorRetry = getIntProperty(CONNECTION_RETRY_PROPERTY);
        int connectionTimeout = getIntProperty(CONNECTION_TIMEOUT_PROPERTY);
        int socketTimeout = getIntProperty(SOCKET_TIMEOUT_PROPERTY);
        Boolean connectionProxy = Boolean.parseBoolean(Framework.getProperty(CONNECTION_PROXY));

        String keystoreFile = getProperty(KEYSTORE_FILE_PROPERTY);
        String keystorePass = getProperty(KEYSTORE_PASS_PROPERTY);
        String privkeyAlias = getProperty(PRIVKEY_ALIAS_PROPERTY);
        String privkeyPass = getProperty(PRIVKEY_PASS_PROPERTY);
        String endpoint = getProperty(ENDPOINT_PROPERTY);
        String sseprop = getProperty(SERVERSIDE_ENCRYPTION_PROPERTY);
        if (isNotBlank(sseprop)) {
            userServerSideEncryption = Boolean.parseBoolean(sseprop);
        }

        // Fallback on default env keys for ID and secret
        if (isBlank(awsID)) {
            awsID = System.getenv(AWS_ID_ENV);
        }
        if (isBlank(awsSecret)) {
            awsSecret = System.getenv(AWS_SECRET_ENV);
        }

        if (isBlank(bucketName)) {
            throw new RuntimeException("Missing conf: " + BUCKET_NAME_PROPERTY);
        }

        if (!isBlank(bucketNamePrefix) && !bucketNamePrefix.endsWith("/")) {
            log.warn(String.format("%s %s S3 bucket prefix should end by '/' " + ": added automatically.",
                    BUCKET_PREFIX_PROPERTY, bucketNamePrefix));
            bucketNamePrefix += "/";
        }
        // set up credentials
        if (isBlank(awsID) || isBlank(awsSecret)) {
            awsCredentialsProvider = new InstanceProfileCredentialsProvider();
            try {
                awsCredentialsProvider.getCredentials();
            } catch (AmazonClientException e) {
                throw new RuntimeException("Missing AWS credentials and no instance role found");
            }
        } else {
            awsCredentialsProvider = new BasicAWSCredentialsProvider(awsID, awsSecret);
        }

        // set up client configuration
        clientConfiguration = new ClientConfiguration();
        if (connectionProxy && isNotBlank(proxyHost)) {
            clientConfiguration.setProxyHost(proxyHost);
        }
        if (connectionProxy && isNotBlank(proxyPort)) {
            clientConfiguration.setProxyPort(Integer.parseInt(proxyPort));
        }
        if (connectionProxy && isNotBlank(proxyLogin)) {
            clientConfiguration.setProxyUsername(proxyLogin);
        }
        if (connectionProxy && proxyPassword != null) { // could be blank
            clientConfiguration.setProxyPassword(proxyPassword);
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
                FileInputStream ksStream = new FileInputStream(ksFile);
                KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
                keystore.load(ksStream, keystorePass.toCharArray());
                ksStream.close();
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

        // Try to create bucket if it doesn't exist
        if (!isEncrypted) {
            amazonS3 = new AmazonS3Client(awsCredentialsProvider, clientConfiguration);
        } else {
            amazonS3 = new AmazonS3EncryptionClient(awsCredentialsProvider, new StaticEncryptionMaterialsProvider(
                    encryptionMaterials), clientConfiguration, cryptoConfiguration);
        }
        if (isNotBlank(endpoint)) {
            amazonS3.setEndpoint(endpoint);
        }

        // Set region explicitely for regions that reguire Version 4 signature
        ArrayList<String> V4_ONLY_REGIONS = new ArrayList<String>();
        V4_ONLY_REGIONS.add("eu-central-1");
        V4_ONLY_REGIONS.add("ap-northeast-2");
        if (V4_ONLY_REGIONS.contains(bucketRegion)) {
            amazonS3.setRegion(Region.getRegion(Regions.fromName(bucketRegion)));
        }

        try {
            if (!amazonS3.doesBucketExist(bucketName)) {
                amazonS3.createBucket(bucketName, bucketRegion);
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

        transferManager = new TransferManager(amazonS3);
        abortOldUploads();
    }

    protected void removeBinary(String digest) {
        amazonS3.deleteObject(bucketName, bucketNamePrefix + digest);
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

    protected static boolean isMissingKey(AmazonClientException e) {
        if (e instanceof AmazonServiceException) {
            AmazonServiceException ase = (AmazonServiceException) e;
            return (ase.getStatusCode() == 404) || "NoSuchKey".equals(ase.getErrorCode())
                    || "Not Found".equals(e.getMessage());
        }
        return false;
    }

    public static boolean isMD5(String digest) {
        return MD5_RE.matcher(digest).matches();
    }

    @Override
    protected FileStorage getFileStorage() {
        return new S3FileStorage();
    }

    public class S3FileStorage implements FileStorage {

        @Override
        public void storeFile(String digest, File file) throws IOException {
            long t0 = 0;
            if (log.isDebugEnabled()) {
                t0 = System.currentTimeMillis();
                log.debug("storing blob " + digest + " to S3");
            }
            String etag;
            String key = bucketNamePrefix + digest;
            try {
                ObjectMetadata metadata = amazonS3.getObjectMetadata(bucketName, key);
                etag = metadata.getETag();
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
                    if (userServerSideEncryption) {
                        ObjectMetadata objectMetadata = new ObjectMetadata();
                        objectMetadata.setSSEAlgorithm(ObjectMetadata.AES_256_SERVER_SIDE_ENCRYPTION);
                        request.setMetadata(objectMetadata);
                    }
                } else {
                    request = new EncryptedPutObjectRequest(bucketName, key, file);
                }
                Upload upload = transferManager.upload(request);
                try {
                    UploadResult result = upload.waitForUploadResult();
                    etag = result.getETag();
                } catch (AmazonClientException ee) {
                    throw new IOException(ee);
                } catch (InterruptedException ee) {
                    // reset interrupted status
                    Thread.currentThread().interrupt();
                    // continue interrupt
                    throw new RuntimeException(ee);
                } finally {
                    if (log.isDebugEnabled()) {
                        long dtms = System.currentTimeMillis() - t0;
                        log.debug("stored blob " + digest + " to S3 in " + dtms + "ms");
                    }
                }
            }
            // check transfer went ok
            if (!isEncrypted && !etag.equals(digest) && !ServiceUtils.isMultipartUploadETag(etag)) {
                // When the blob is not encrypted by S3, the MD5 remotely
                // computed by S3 and passed as a Etag should match the locally
                // computed MD5 digest.
                // This check cannot be done when encryption is enabled unless
                // we could replicate that encryption locally just for that
                // purpose which would add further load and complexity on the
                // client.
                throw new IOException("Invalid ETag in S3, ETag=" + etag + " digest=" + digest);
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

                ObjectMetadata metadata = amazonS3.getObject(
                        new GetObjectRequest(bucketName, bucketNamePrefix + digest), file);
                // check ETag
                String etag = metadata.getETag();
                if (!isEncrypted && !etag.equals(digest) && !ServiceUtils.isMultipartUploadETag(etag)) {
                    log.error("Invalid ETag in S3, ETag=" + etag + " digest=" + digest);
                    return false;
                }
                return true;
            } catch (AmazonClientException e) {
                if (!isMissingKey(e)) {
                    throw new IOException(e);
                }
                return false;
            } finally {
                if (log.isDebugEnabled()) {
                    long dtms = System.currentTimeMillis() - t0;
                    log.debug("fetched blob " + digest + " from S3 in " + dtms + "ms");
                }
            }

        }

        @Override
        public Long fetchLength(String digest) throws IOException {
            long t0 = 0;
            if (log.isDebugEnabled()) {
                t0 = System.currentTimeMillis();
                log.debug("fetching blob length " + digest + " from S3");
            }
            try {
                ObjectMetadata metadata = amazonS3.getObjectMetadata(bucketName, bucketNamePrefix + digest);
                // check ETag
                String etag = metadata.getETag();
                if (!isEncrypted && !etag.equals(digest) && !ServiceUtils.isMultipartUploadETag(etag)) {
                    log.error("Invalid ETag in S3, ETag=" + etag + " digest=" + digest);
                    return null;
                }
                return Long.valueOf(metadata.getContentLength());
            } catch (AmazonClientException e) {
                if (!isMissingKey(e)) {
                    throw new IOException(e);
                }
                return null;
            } finally {
                if (log.isDebugEnabled()) {
                    long dtms = System.currentTimeMillis() - t0;
                    log.debug("fetched blob length " + digest + " from S3 in " + dtms + "ms");
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
        public Set<String> getUnmarkedBlobs() {
            // list S3 objects in the bucket
            // record those not marked
            Set<String> unmarked = new HashSet<>();
            ObjectListing list = null;
            do {
                if (list == null) {
                    list = binaryManager.amazonS3.listObjects(binaryManager.bucketName, binaryManager.bucketNamePrefix);
                } else {
                    list = binaryManager.amazonS3.listNextBatchOfObjects(list);
                }
                int prefixLength = binaryManager.bucketNamePrefix.length();
                for (S3ObjectSummary summary : list.getObjectSummaries()) {
                    String digest = summary.getKey().substring(prefixLength);
                    if (!isMD5(digest)) {
                        // ignore files that cannot be MD5 digests for
                        // safety
                        continue;
                    }
                    long length = summary.getSize();
                    if (marked.contains(digest)) {
                        status.numBinaries++;
                        status.sizeBinaries += length;
                    } else {
                        status.numBinariesGC++;
                        status.sizeBinariesGC += length;
                        // record file to delete
                        unmarked.add(digest);
                        marked.remove(digest); // optimize memory
                    }
                }
            } while (list.isTruncated());

            return unmarked;
        }
    }

    // ******************** BlobProvider ********************

    @Override
    public Blob readBlob(BlobInfo blobInfo) throws IOException {
        // just delegate to avoid copy/pasting code
        return new BinaryBlobProvider(this).readBlob(blobInfo);
    }

    @Override
    public String writeBlob(Blob blob, Document doc) throws IOException {
        // just delegate to avoid copy/pasting code
        return new BinaryBlobProvider(this).writeBlob(blob, doc);
    }

    @Override
    protected boolean isDirectDownload() {
        return directDownload;
    }

    @Override
    protected URI getRemoteUri(String digest, ManagedBlob blob, HttpServletRequest servletRequest) throws IOException {
        String key = bucketNamePrefix + digest;
        Date expiration = new Date();
        expiration.setTime(expiration.getTime() + directDownloadExpire * 1000);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.GET);
        request.addRequestParameter("response-content-type", getContentTypeHeader(blob));
        request.addRequestParameter("response-content-disposition", getContentDispositionHeader(blob, servletRequest));
        request.setExpiration(expiration);
        URL url = amazonS3.generatePresignedUrl(request);
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

}
