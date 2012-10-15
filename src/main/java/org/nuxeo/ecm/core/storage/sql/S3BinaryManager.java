/*
 * (C) Copyright 2011-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.nuxeo.common.file.FileCache;
import org.nuxeo.common.file.LRUFileCache;
import org.nuxeo.common.utils.SizeUtils;
import org.nuxeo.runtime.api.Framework;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3EncryptionClient;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;

/**
 * A Binary Manager that stores binaries as S3 BLOBs
 * <p>
 * The BLOBs are cached locally on first access for efficiency.
 * <p>
 * Because the BLOB length can be accessed independently of the binary stream,
 * it is also cached in a simple text file if accessed before the stream.
 */
public class S3BinaryManager extends AbstractBinaryManager {

    private static final Log log = LogFactory.getLog(S3BinaryManager.class);

    public static final String BUCKET_NAME_KEY = "nuxeo.s3storage.bucket";

    public static final String BUCKET_REGION_KEY = "nuxeo.s3storage.region";

    public static final String DEFAULT_BUCKET_REGION = null; // US East

    public static final String AWS_ID_KEY = "nuxeo.s3storage.awsid";

    public static final String AWS_ID_ENV_KEY = "AWS_ACCESS_KEY_ID";

    public static final String AWS_SECRET_KEY = "nuxeo.s3storage.awssecret";

    public static final String AWS_SECRET_ENV_KEY = "AWS_SECRET_ACCESS_KEY";

    public static final String CACHE_SIZE_KEY = "nuxeo.s3storage.cachesize";

    public static final String DEFAULT_CACHE_SIZE = "100 MB";

    public static final String KEYSTORE_FILE_KEY = "nuxeo.s3storage.crypt.keystore.file";

    public static final String KEYSTORE_PASS_KEY = "nuxeo.s3storage.crypt.keystore.password";

    public static final String PRIVKEY_ALIAS_KEY = "nuxeo.s3storage.crypt.key.alias";

    public static final String PRIVKEY_PASS_KEY = "nuxeo.s3storage.crypt.key.password";

    // TODO define these constants globally somewhere
    public static final String PROXY_HOST_KEY = "nuxeo.http.proxy.host";

    public static final String PROXY_PORT_KEY = "nuxeo.http.proxy.port";

    public static final String PROXY_LOGIN_KEY = "nuxeo.http.proxy.login";

    public static final String PROXY_PASSWORD_KEY = "nuxeo.http.proxy.password";

    private static final String MD5 = "MD5"; // must be MD5 for Etag

    private static final Pattern MD5_RE = Pattern.compile("[0-9a-f]{32}");

    protected String bucketName;

    protected BasicAWSCredentials awsCredentials;

    protected ClientConfiguration clientConfiguration;

    protected EncryptionMaterials encryptionMaterials;

    protected CryptoConfiguration cryptoConfiguration;

    protected String repositoryName;

    protected FileCache fileCache;

    protected AmazonS3 amazonS3;

    @Override
    public void initialize(RepositoryDescriptor repositoryDescriptor)
            throws IOException {
        repositoryName = repositoryDescriptor.name;
        descriptor = new BinaryManagerDescriptor();
        descriptor.digest = MD5; // matches ETag computation
        log.info("Repository '" + repositoryDescriptor.name + "' using "
                + getClass().getSimpleName());

        // Get settings from the configuration
        bucketName = Framework.getProperty(BUCKET_NAME_KEY);
        String bucketRegion = Framework.getProperty(BUCKET_REGION_KEY);
        if (isBlank(bucketRegion)) {
            bucketRegion = DEFAULT_BUCKET_REGION;
        }
        String awsID = Framework.getProperty(AWS_ID_KEY);
        String awsSecret = Framework.getProperty(AWS_SECRET_KEY);

        String proxyHost = Framework.getProperty(PROXY_HOST_KEY);
        String proxyPort = Framework.getProperty(PROXY_PORT_KEY);
        String proxyLogin = Framework.getProperty(PROXY_LOGIN_KEY);
        String proxyPassword = Framework.getProperty(PROXY_PASSWORD_KEY);

        String cacheSizeStr = Framework.getProperty(CACHE_SIZE_KEY);
        if (isBlank(cacheSizeStr)) {
            cacheSizeStr = DEFAULT_CACHE_SIZE;
        }

        String keystoreFile = Framework.getProperty(KEYSTORE_FILE_KEY);
        String keystorePass = Framework.getProperty(KEYSTORE_PASS_KEY);
        String privkeyAlias = Framework.getProperty(PRIVKEY_ALIAS_KEY);
        String privkeyPass = Framework.getProperty(PRIVKEY_PASS_KEY);

        // Fallback on default env keys for ID and secret
        if (isBlank(awsID)) {
            awsID = System.getenv(AWS_ID_ENV_KEY);
        }
        if (isBlank(awsSecret)) {
            awsSecret = System.getenv(AWS_SECRET_ENV_KEY);
        }
        if (isBlank(bucketName)) {
            throw new RuntimeException("Missing conf: " + BUCKET_NAME_KEY);
        }
        if (isBlank(awsID)) {
            throw new RuntimeException("Missing conf: " + AWS_ID_KEY);
        }
        if (isBlank(awsSecret)) {
            throw new RuntimeException("Missing conf: " + AWS_SECRET_KEY);
        }

        // set up credentials
        awsCredentials = new BasicAWSCredentials(awsID, awsSecret);

        // set up client configuration
        clientConfiguration = new ClientConfiguration();
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
                    throw new RuntimeException("Alias " + privkeyAlias
                            + " is missing or not a key alias");
                }
                PrivateKey privKey = (PrivateKey) keystore.getKey(privkeyAlias,
                        privkeyPass.toCharArray());
                Certificate cert = keystore.getCertificate(privkeyAlias);
                PublicKey pubKey = cert.getPublicKey();
                KeyPair keypair = new KeyPair(pubKey, privKey);
                // Get encryptionMaterials from keypair
                encryptionMaterials = new EncryptionMaterials(keypair);
                cryptoConfiguration = new CryptoConfiguration();
            } catch (Exception e) {
                throw new RuntimeException("Could not read keystore: "
                        + keystoreFile + ", alias: " + privkeyAlias, e);
            }
        }

        // Try to create bucket if it doesn't exist
        if (encryptionMaterials == null) {
            amazonS3 = new AmazonS3Client(awsCredentials, clientConfiguration);
        } else {
            amazonS3 = new AmazonS3EncryptionClient(awsCredentials,
                    encryptionMaterials, clientConfiguration,
                    cryptoConfiguration);
        }
        try {
            log.trace(String.format(
                    "Checking the existence of bucket '%s' in region '%s'",
                    bucketName, bucketRegion));
            if (!amazonS3.doesBucketExist(bucketName)) {
                log.debug(String.format(
                        "Creating missing bucket '%s' in region '%s'",
                        bucketName, bucketRegion));
                amazonS3.createBucket(bucketName, bucketRegion);
                amazonS3.setBucketAcl(bucketName,
                        CannedAccessControlList.Private);
            }
        } catch (AmazonServiceException e) {
            throw new IOException(e);
        } catch (AmazonClientException e) {
            throw new IOException(e);
        }

        // Create file cache
        File dir = File.createTempFile("nxbincache.", "", null);
        dir.delete();
        dir.mkdir();
        dir.deleteOnExit();
        long cacheSize = SizeUtils.parseSizeInBytes(cacheSizeStr);
        fileCache = new LRUFileCache(dir, cacheSize);
        log.info("Using binary cache directory: " + dir.getPath() + " size: "
                + cacheSizeStr);

        createGarbageCollector();
    }

    protected void createGarbageCollector() {
        garbageCollector = new S3BinaryGarbageCollector(this);
    }

    @Override
    public Binary getBinary(InputStream in) throws IOException {
        // Write the input stream to a temporary file, while computing a digest
        File tmp = fileCache.getTempFile();
        OutputStream out = new FileOutputStream(tmp);
        String digest;
        try {
            digest = storeAndDigest(in, out);
        } finally {
            in.close();
            out.close();
        }

        // Store the blob in the S3 bucket if not already there
        String etag;
        try {
            log.trace(String.format(
                    "Looking up existence of blob with digest '%s' in bucket '%s'",
                    digest, bucketName));
            ObjectMetadata metadata = amazonS3.getObjectMetadata(bucketName,
                    digest);
            etag = metadata.getETag();
        } catch (AmazonClientException e) {
            if (!isMissingKey(e)) {
                throw new IOException(e);
            }
            // no data, store the blob
            try {
                log.trace(String.format(
                        "Uploading blob with digest '%s' in bucket '%s'",
                        digest, bucketName));
                Split split = SimonManager.getStopwatch(
                        this.getClass().getName() + ".upload").start();
                PutObjectResult result = amazonS3.putObject(bucketName, digest,
                        tmp);
                Double duration = split.stop() / 1e9;
                log.debug(String.format(
                        "Uploaded blob with digest '%s' in bucket '%s' in %fs",
                        digest, bucketName, duration));
                log.trace(split.getStopwatch());
                etag = result.getETag();
            } catch (AmazonClientException ee) {
                throw new IOException(ee);
            }
        }
        // check transfer went ok
        if (!(amazonS3 instanceof AmazonS3EncryptionClient)
                && !etag.equals(digest)) {
            // When the blob is not encrypted by S3, the MD5 remotely computed
            // by S3 and passed as a Etag should match the locally computed MD5
            // digest.
            // This check cannot be done when encryption is enabled unless we
            // could replicate that encryption locally just for that purpose
            // which would add further load and complexity on the client.
            throw new IOException("Invalid ETag in S3, ETag=" + etag
                    + " digest=" + digest);
        }

        // Register the file in the file cache if all went well
        File file = fileCache.putFile(digest, tmp);

        return new Binary(file, digest, repositoryName);
    }

    @Override
    public Binary getBinary(String digest) {
        // Check in the cache
        File file = fileCache.getFile(digest);
        if (file == null) {
            return new S3LazyBinary(digest, fileCache, amazonS3, bucketName);
        } else {
            return new Binary(file, digest, repositoryName);
        }
    }

    protected void removeBinary(String digest) {
        log.trace(String.format(
                "Deleting blob with digest '%s' in bucket '%s'", digest,
                bucketName));
        amazonS3.deleteObject(bucketName, digest);
    }

    protected static boolean isMissingKey(AmazonClientException e) {
        if (e instanceof AmazonServiceException) {
            AmazonServiceException ase = (AmazonServiceException) e;
            return "NoSuchKey".equals(ase.getErrorCode())
                    || "Not Found".equals(e.getMessage());
        }
        return false;
    }

    public static boolean isMD5(String digest) {
        return MD5_RE.matcher(digest).matches();
    }

    public static class S3LazyBinary extends LazyBinary {

        private static final long serialVersionUID = 1L;

        protected final AmazonS3 amazonS3;

        protected final String bucketName;

        public S3LazyBinary(String digest, FileCache fileCache,
                AmazonS3 amazonS3, String bucketName) {
            super(digest, fileCache);
            this.amazonS3 = amazonS3;
            this.bucketName = bucketName;
        }

        @Override
        protected boolean fetchFile(File tmp) {
            try {
                log.trace(String.format(
                        "Fetching blob with digest '%s' in bucket '%s'",
                        digest, bucketName));
                Split split = SimonManager.getStopwatch(
                        this.getClass().getName() + ".download").start();
                ObjectMetadata metadata = amazonS3.getObject(
                        new GetObjectRequest(bucketName, digest), tmp);
                Double duration = split.stop() / 1e9;
                log.debug(String.format(
                        "Downloaded blob with digest '%s' in bucket '%s' in %fs",
                        digest, bucketName, duration));
                log.trace(split.getStopwatch());
                // check ETag
                String etag = metadata.getETag();
                if (!(amazonS3 instanceof AmazonS3EncryptionClient)
                        && !etag.equals(digest)) {
                    log.error("Invalid ETag in S3, ETag=" + etag + " digest="
                            + digest);
                    return false;
                }
                return true;
            } catch (AmazonClientException e) {
                if (!isMissingKey(e)) {
                    log.error("Unknown binary: " + digest, e);
                }
                return false;
            }
        }

        @Override
        protected Long fetchLength() {
            try {
                log.trace(String.format(
                        "Looking up length of blob with digest '%s' in bucket '%s'",
                        digest, bucketName));
                ObjectMetadata metadata = amazonS3.getObjectMetadata(
                        bucketName, digest);
                // check ETag
                String etag = metadata.getETag();
                if (!(amazonS3 instanceof AmazonS3EncryptionClient)
                        && !etag.equals(digest)) {
                    log.error("Invalid ETag in S3, ETag=" + etag + " digest="
                            + digest);
                    return null;
                }
                return Long.valueOf(metadata.getContentLength());
            } catch (AmazonClientException e) {
                if (!isMissingKey(e)) {
                    log.error("Unknown binary: " + digest, e);
                }
                return null;
            }
        }
    }

    /**
     * Garbage collector for S3 binaries that stores the marked (in use)
     * binaries in memory.
     */
    public static class S3BinaryGarbageCollector implements
            BinaryGarbageCollector {

        protected final S3BinaryManager binaryManager;

        protected volatile long startTime;

        protected BinaryManagerStatus status;

        protected Set<String> marked;

        public S3BinaryGarbageCollector(S3BinaryManager binaryManager) {
            this.binaryManager = binaryManager;
        }

        @Override
        public String getId() {
            return "s3:" + binaryManager.bucketName;
        }

        @Override
        public BinaryManagerStatus getStatus() {
            return status;
        }

        @Override
        public boolean isInProgress() {
            // volatile as this is designed to be called from another thread
            return startTime != 0;
        }

        @Override
        public void start() {
            if (startTime != 0) {
                throw new RuntimeException("Alread started");
            }
            startTime = System.currentTimeMillis();
            status = new BinaryManagerStatus();
            marked = new HashSet<String>();
            log.trace(String.format(
                    "Starting Garbage Collection on bucket '%s'",
                    binaryManager.bucketName));
        }

        @Override
        public void mark(String digest) {
            marked.add(digest);
        }

        @Override
        public void stop(boolean delete) {
            if (startTime == 0) {
                throw new RuntimeException("Not started");
            }

            try {
                // list S3 objects in the bucket
                // record those not marked
                Set<String> unmarked = new HashSet<String>();
                ObjectListing list = null;
                do {
                    if (list == null) {
                        list = binaryManager.amazonS3.listObjects(binaryManager.bucketName);
                    } else {
                        list = binaryManager.amazonS3.listNextBatchOfObjects(list);
                    }
                    for (S3ObjectSummary summary : list.getObjectSummaries()) {
                        String digest = summary.getKey();
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
                marked = null; // help GC

                // delete unmarked objects
                if (delete) {
                    for (String digest : unmarked) {
                        binaryManager.removeBinary(digest);
                    }
                }

            } catch (AmazonClientException e) {
                throw new RuntimeException(e);
            }

            status.gcDuration = System.currentTimeMillis() - startTime;
            startTime = 0;
            log.debug(String.format(
                    "Collected %d blobs with total size %d bytes in %fs in bucket '%s'",
                    status.numBinariesGC, status.sizeBinariesGC,
                    status.gcDuration / 1000., binaryManager.bucketName));
        }
    }

}
