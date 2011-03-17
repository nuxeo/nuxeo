/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.NamingException;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;


import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.file.FileCache;
import org.nuxeo.common.file.LRUFileCache;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.runtime.api.Framework;


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
    public static final String DEFAULT_CACHE_SIZE = "10M";

    // Those are probably defined somewhere else, but I didn't see them!
    public static final String PROXY_HOST_KEY = "nuxeo.http.proxy.host";
    public static final String PROXY_PORT_KEY = "nuxeo.http.proxy.port";
    public static final String PROXY_LOGIN_KEY = "nuxeo.http.proxy.login";
    public static final String PROXY_PASSWORD_KEY = "nuxeo.http.proxy.password";

    protected String bucketName;
    protected BasicAWSCredentials awsCredentials;
    protected ClientConfiguration clientConfiguration;

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
        long cacheSize = parseSizeInBytes(cacheSizeStr);
        fileCache = new LRUFileCache(dir, cacheSize);
        log.info("Using binary cache directory: " + dir.getPath() + " size: "
                + cacheSizeStr);
    }

    protected AmazonS3Client getS3Client() {
        // TODO: add encryption support
        return new AmazonS3Client(awsCredentials,clientConfiguration);
    }

    protected long parseSizeInBytes(String string) {
        String digits = string;
        if (digits.length() == 0) {
            throw new RuntimeException("Invalid empty size");
        }
        char unit = digits.charAt(digits.length() - 1);
        if (unit == 'b' || unit == 'B') {
            digits = digits.substring(0, digits.length() - 1);
            if (digits.length() == 0) {
                throw new RuntimeException("Invalid size: '" + string + "'");
            }
            unit = digits.charAt(digits.length() - 1);
        }
        long mul;
        switch (unit) {
        case 'k':
        case 'K':
            mul = 1024;
            break;
        case 'm':
        case 'M':
            mul = 1024 * 1024;
            break;
        case 'g':
        case 'G':
            mul = 1024 * 1024 * 1024;
            break;
        default:
            if (!Character.isDigit(unit)) {
                throw new RuntimeException("Invalid size: '" + string + "'");
            }
            mul = 1;
        }
        if (mul != 1) {
            digits = digits.substring(0, digits.length() - 1);
            if (digits.length() == 0) {
                throw new RuntimeException("Invalid size: '" + string + "'");
            }
        }
        try {
            return Long.parseLong(digits) * mul;
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid size: '" + string + "'");
        }
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
