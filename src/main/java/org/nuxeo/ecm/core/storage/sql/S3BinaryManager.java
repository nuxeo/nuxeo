/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.core.storage.sql;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.file.FileCache;
import org.nuxeo.common.file.LRUFileCache;
import org.nuxeo.common.utils.SizeUtils;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CryptoConfiguration;
import com.amazonaws.services.s3.model.EncryptionMaterials;
import com.amazonaws.services.s3.model.S3Object;


/**
 * A Binary Manager that stores binaries as S3 BLOBs
 * <p>
 * The blob keys is still computed
 */
public class S3BinaryManager extends DefaultBinaryManager {

    private static final Log log = LogFactory.getLog(S3BinaryManager.class);

    public static final String BUCKET_NAME_KEY = "nuxeo.s3storage.bucket";

    public static final String BUCKET_REGION_KEY = "nuxeo.s3storage.region";
    public static final String DEFAULT_BUCKET_REGION = null; // US East

    public static final String AWS_ID_KEY = "nuxeo.s3storage.awsid";
    public static final String AWS_SECRET_KEY = "nuxeo.s3storage.awssecret";

    public static final String CACHE_SIZE_KEY = "nuxeo.s3storage.cachesize";
    public static final String DEFAULT_CACHE_SIZE = "100M";

    public static final String KEYSTORE_FILE_KEY = "nuxeo.s3storage.crypt.keystore.file";
    public static final String KEYSTORE_PASS_KEY = "nuxeo.s3storage.crypt.keystore.password";
    public static final String PRIVKEY_ALIAS_KEY = "nuxeo.s3storage.crypt.key.alias";
    public static final String PRIVKEY_PASS_KEY = "nuxeo.s3storage.crypt.key.password";

    // Those are probably defined somewhere else, but I didn't see them!
    public static final String PROXY_HOST_KEY = "nuxeo.http.proxy.host";
    public static final String PROXY_PORT_KEY = "nuxeo.http.proxy.port";
    public static final String PROXY_LOGIN_KEY = "nuxeo.http.proxy.login";
    public static final String PROXY_PASSWORD_KEY = "nuxeo.http.proxy.password";

    protected String bucketName;
    protected BasicAWSCredentials awsCredentials;
    protected ClientConfiguration clientConfiguration;
    protected EncryptionMaterials encryptionMaterials;
    protected CryptoConfiguration cryptoConfiguration;

    protected String repositoryName;
    protected FileCache fileCache;

    @Override
    public void initialize(RepositoryDescriptor repositoryDescriptor)
            throws IOException {
        repositoryName = repositoryDescriptor.name;
        descriptor = new BinaryManagerDescriptor();
        descriptor.digest = getDigest();
        log.info("Repository '" + repositoryDescriptor.name + "' using "
                + this.getClass().getSimpleName());

        // Get settings from the configuration
        bucketName = org.nuxeo.runtime.api.Framework.getProperty(BUCKET_NAME_KEY, null);
        String bucketRegion = org.nuxeo.runtime.api.Framework.getProperty(BUCKET_REGION_KEY, DEFAULT_BUCKET_REGION);
        String awsID = org.nuxeo.runtime.api.Framework.getProperty(AWS_ID_KEY, null);
        String awsSecret = org.nuxeo.runtime.api.Framework.getProperty(AWS_SECRET_KEY, null);

        String proxyHost = org.nuxeo.runtime.api.Framework.getProperty(PROXY_HOST_KEY, null);
        String proxyPort = org.nuxeo.runtime.api.Framework.getProperty(PROXY_PORT_KEY, null);
        String proxyLogin = org.nuxeo.runtime.api.Framework.getProperty(PROXY_LOGIN_KEY, null);
        String proxyPassword = org.nuxeo.runtime.api.Framework.getProperty(PROXY_PASSWORD_KEY, null);

        String cacheSizeStr = org.nuxeo.runtime.api.Framework.getProperty(CACHE_SIZE_KEY, DEFAULT_CACHE_SIZE);

        String keystoreFile = org.nuxeo.runtime.api.Framework.getProperty(KEYSTORE_FILE_KEY, null);
        String keystorePass = org.nuxeo.runtime.api.Framework.getProperty(KEYSTORE_PASS_KEY, null);
        String privkeyAlias = org.nuxeo.runtime.api.Framework.getProperty(PRIVKEY_ALIAS_KEY, null);
        String privkeyPass = org.nuxeo.runtime.api.Framework.getProperty(PRIVKEY_PASS_KEY, null);

        if (bucketName == null) {
            throw new RuntimeException("Missing " + BUCKET_NAME_KEY + "in nuxeo.conf");
        }
        if (awsID == null) {
            throw new RuntimeException("Missing " + AWS_ID_KEY + "in nuxeo.conf");
        }
        if (awsSecret == null) {
            throw new RuntimeException("Missing " + AWS_SECRET_KEY + "in nuxeo.conf");
        }

        // Setup credentials
        awsCredentials = new BasicAWSCredentials(awsID, awsSecret);

        // Setup client configuration
        clientConfiguration = new ClientConfiguration();
        if (proxyHost != null) {
            clientConfiguration.setProxyHost(proxyHost);
        }
        if (proxyPort != null) {
            clientConfiguration.setProxyPort(Integer.parseInt(proxyPort));
        }
        if (proxyLogin != null) {
            clientConfiguration.setProxyUsername(proxyLogin);
        }
        if (proxyPassword != null) {
            clientConfiguration.setProxyPassword(proxyPassword);
        }

        // Setup encryption
        encryptionMaterials = null;
        if (keystoreFile != null) {
            boolean confok = true;
            if (keystorePass == null) {
                log.error("Keystore password missing");
                confok = false;
            }
            if (privkeyAlias == null) {
                log.error("Key alias missing");
                confok = false;
            }
            if (privkeyPass == null) {
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
                PrivateKey privKey = (PrivateKey)keystore.getKey(privkeyAlias, privkeyPass.toCharArray());
                Certificate cert = keystore.getCertificate(privkeyAlias);
                PublicKey pubKey = cert.getPublicKey();
                KeyPair keypair = new KeyPair(pubKey, privKey);
                // Get encryptionMaterials from keypair
                encryptionMaterials = new EncryptionMaterials(keypair);
                cryptoConfiguration = new CryptoConfiguration();
            } catch (IOException e) {
                throw new RuntimeException("Could not read keystore: " + keystoreFile);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Could not verify keystore integrity: " + keystoreFile);
            } catch (CertificateException e) {
                throw new RuntimeException("Could not read keystore certificates for " + keystoreFile);
            } catch (KeyStoreException e) {
                throw new RuntimeException(e);
            } catch (UnrecoverableKeyException e) {
                throw new RuntimeException("Wrong password for key alias " + privkeyAlias);
            }
        }

        // Try to create bucket if it doesn't exist
        AmazonS3Client s3client = getS3Client();
        try {
            if (!s3client.doesBucketExist(bucketName)) {
                s3client.createBucket(bucketName, bucketRegion);
                s3client.setBucketAcl(bucketName, CannedAccessControlList.Private);
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
    }

    protected AmazonS3Client getS3Client() {
        AmazonS3Client client;
        if (encryptionMaterials == null) {
            client = new AmazonS3Client(awsCredentials, clientConfiguration);
        } else {
            client = new com.amazonaws.services.s3.AmazonS3EncryptionClient(awsCredentials, encryptionMaterials, clientConfiguration, cryptoConfiguration);
        }
        return client;
    }

    /**
     * Gets the message digest to use to hash binaries.
     */
    protected String getDigest() {
        return DEFAULT_DIGEST;
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

        // Register the file in the file cache
        File file = fileCache.putFile(digest, tmp);

        // Store the blob in the S3 bucket
        AmazonS3Client s3client = getS3Client();

        boolean objectExists = true;
        try {
            s3client.getObjectMetadata(bucketName, digest);
        } catch (AmazonServiceException e) {
            objectExists = false;
        } catch (AmazonClientException e) {
            objectExists = false;
        }

        if (!objectExists) {
            try {
                s3client.putObject(bucketName, digest, file);
            } catch (AmazonServiceException e) {
                throw new IOException(e);
            } catch (AmazonClientException e) {
                throw new IOException(e);
            }
        }

        return new Binary(file, digest, repositoryName);
    }

    @Override
    public Binary getBinary(String digest) {
        // Check in the cache
        File file = fileCache.getFile(digest);
        if (file == null) {
            // Fetch from S3 and store it in the cache
            AmazonS3Client s3client = getS3Client();
            try {
                S3Object s3object = s3client.getObject(bucketName, digest);
                file = fileCache.putFile(digest, s3object.getObjectContent());
            } catch (AmazonServiceException e) {
                throw new RuntimeException(e);
            } catch (AmazonClientException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new Binary(file, digest, repositoryName);
    }

}
