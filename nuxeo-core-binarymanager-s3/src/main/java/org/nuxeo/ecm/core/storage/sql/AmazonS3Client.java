/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.aws.NuxeoAWSRegionProvider;

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
import com.google.common.base.MoreObjects;

/**
 * @since 10.2
 */
public class AmazonS3Client {

    public static final String BUCKET_NAME_PROPERTY = "bucket";

    public static final String BUCKET_PREFIX_PROPERTY = "bucket_prefix";

    public static final String BUCKET_REGION_PROPERTY = "region";

    public static final String AWS_ID_PROPERTY = "awsid";

    public static final String AWS_SECRET_PROPERTY = "awssecret";

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

    private final AmazonS3 amazonS3;

    private final String bucketName;

    private final String bucketNamePrefix;

    private final boolean useServerSideEncryption;

    private final String serverSideKMSKeyID;

    private final AWSCredentialsProvider awsCredentialsProvider;

    /**
     * @param build
     * @param endpoint
     * @param bucketRegion
     * @param bucketNamePrefix
     * @param bucketName
     * @param awsCredentialsProvider
     */
    private AmazonS3Client(AmazonS3 amazonS3, String bucketName, String bucketNamePrefix,
            AWSCredentialsProvider awsCredentialsProvider, boolean useServerSideEncryption, String serverSideKMSKeyID) {
        this.amazonS3 = amazonS3;

        this.bucketName = bucketName;
        this.bucketNamePrefix = bucketNamePrefix;
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.useServerSideEncryption = useServerSideEncryption;
        this.serverSideKMSKeyID = serverSideKMSKeyID;

    }

    public String getBucketName() {
        return bucketName;
    }

    public String getBucketNamePrefix() {
        return bucketNamePrefix;
    }

    public boolean isUseServerSideEncryption() {
        return useServerSideEncryption;
    }

    public String getServerSideKMSKeyID() {
        return serverSideKMSKeyID;
    }

    public static class Builder {

        private static final Log log = LogFactory.getLog(AmazonS3Client.Builder.class);

        private final Map<String, String> properties;

        private final String systemPropertyPrefix;

        protected boolean useServerSideEncryption;

        protected String serverSideKMSKeyID;

        /**
         * @param systemPropertyPrefix
         * @param properties2
         */
        public Builder(String systemPropertyPrefix, Map<String, String> properties) {
            this.systemPropertyPrefix = systemPropertyPrefix;
            this.properties = properties;
        }

        public AmazonS3Client build() {
            String bucketName = getProperty(BUCKET_NAME_PROPERTY);
            // as bucket prefix is optional we don't want to use the fallback mechanism
            String bucketNamePrefix = StringUtils.defaultString(properties.get(BUCKET_PREFIX_PROPERTY));
            // Get settings from the configuration
            String bucketRegion = getProperty(BUCKET_REGION_PROPERTY);
            if (isBlank(bucketRegion)) {
                bucketRegion = NuxeoAWSRegionProvider.getInstance().getRegion();
            }

            String endpoint = getProperty(ENDPOINT_PROPERTY);
            String sseprop = getProperty(SERVERSIDE_ENCRYPTION_PROPERTY);
            if (isNotBlank(sseprop)) {
                useServerSideEncryption = Boolean.parseBoolean(sseprop);
                serverSideKMSKeyID = getProperty(SERVERSIDE_ENCRYPTION_KMS_KEY_PROPERTY);
            }

            if (isBlank(bucketName)) {
                throw new RuntimeException("Missing conf: " + BUCKET_NAME_PROPERTY);
            }

            if (!isBlank(bucketNamePrefix) && !bucketNamePrefix.endsWith("/")) {
                log.warn(String.format("%s %s S3 bucket prefix should end by '/' " + ": added automatically.",
                        BUCKET_PREFIX_PROPERTY, bucketNamePrefix));
                bucketNamePrefix += "/";
            }

            boolean pathStyleAccessEnabled = getBooleanProperty(PATHSTYLEACCESS_PROPERTY);

            AmazonS3Builder<?,?> s3Builder = getAWSClientBuilder();

            AWSCredentialsProvider awsCredentialsProvider = buildAWSCredentials();
            s3Builder.withCredentials(awsCredentialsProvider).withClientConfiguration(buildClientConfiguration());

            if (pathStyleAccessEnabled) {
                log.debug("Path-style access enabled");
                s3Builder.enablePathStyleAccess();
            }

            if (isNotBlank(endpoint)) {
                s3Builder = s3Builder.withEndpointConfiguration(new EndpointConfiguration(endpoint, bucketRegion));
            } else {
                s3Builder = s3Builder.withRegion(bucketRegion);
            }

            return new AmazonS3Client(s3Builder.build(), bucketName, bucketNamePrefix, awsCredentialsProvider,
                    useServerSideEncryption, serverSideKMSKeyID);

        }

        protected ClientConfiguration buildClientConfiguration() {
            // set up client configuration
            ClientConfiguration clientConfiguration = new ClientConfiguration();
            setupProxy(clientConfiguration);
            configureConnection(clientConfiguration);
            customConfiguration(clientConfiguration);
            return clientConfiguration;
        }

        protected AWSCredentialsProvider buildAWSCredentials() {
            // set up credentials
            String awsID = getProperty(AWS_ID_PROPERTY);
            String awsSecret = getProperty(AWS_SECRET_PROPERTY);

            AWSCredentialsProvider awsCredentialsProvider = S3Utils.getAWSCredentialsProvider(awsID, awsSecret);
            return awsCredentialsProvider;
        }

        /**
         * @param clientConfiguration
         * @since 10.2
         */
        protected void customConfiguration(ClientConfiguration clientConfiguration) {

        }

        /**
         * @return
         * @since 10.2
         */
        protected AmazonS3Builder<?,?> getAWSClientBuilder() {

            String keystoreFile = getProperty(KEYSTORE_FILE_PROPERTY);

            if (isNotBlank(keystoreFile)) {

                KeyPair keypair = getKeyPair(keystoreFile);
                EncryptionMaterials encryptionMaterials = new EncryptionMaterials(keypair);

                return AmazonS3EncryptionClientBuilder.standard()
                                                      .withCryptoConfiguration(new CryptoConfiguration())
                                                      .withEncryptionMaterials(new StaticEncryptionMaterialsProvider(
                                                              encryptionMaterials));

            } else {
                return AmazonS3ClientBuilder.standard();
            }
        }

        protected KeyPair getKeyPair(String keystoreFile) {
            String privkeyAlias = getProperty(PRIVKEY_ALIAS_PROPERTY);
            try {
                String keystorePass = getProperty(KEYSTORE_PASS_PROPERTY);
                String privkeyPass = getProperty(PRIVKEY_PASS_PROPERTY);

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
                return keypair;
            } catch (IOException | GeneralSecurityException e) {
                throw new RuntimeException("Could not read keystore: " + keystoreFile + ", alias: " + privkeyAlias, e);
            }
        }

        /**
         * @param clientConfiguration2
         * @since 10.2
         */
        protected void configureConnection(ClientConfiguration clientConfiguration) {

            int maxConnections = getIntProperty(CONNECTION_MAX_PROPERTY);
            int maxErrorRetry = getIntProperty(CONNECTION_RETRY_PROPERTY);
            int connectionTimeout = getIntProperty(CONNECTION_TIMEOUT_PROPERTY);
            int socketTimeout = getIntProperty(SOCKET_TIMEOUT_PROPERTY);

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
        }

        /**
         * @param clientConfiguration2
         * @since 10.2
         */
        protected void setupProxy(ClientConfiguration clientConfiguration) {

            String proxyHost = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_HOST);
            String proxyPort = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_PORT);
            String proxyLogin = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_LOGIN);
            String proxyPassword = Framework.getProperty(Environment.NUXEO_HTTP_PROXY_PASSWORD);

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

        protected boolean getBooleanProperty(String key) {
            return Boolean.parseBoolean(getProperty(key));
        }

        protected String getProperty(String propertyName) {
            return getProperty(propertyName, null);
        }

        protected String getProperty(String propertyName, String defaultValue) {
            String propValue = properties.get(propertyName);
            if (isNotBlank(propValue)) {
                return propValue;
            }
            propValue = Framework.getProperty(getSystemPropertyName(propertyName));
            if (isNotBlank(propValue)) {
                return propValue;
            }
            return defaultValue;
        }

        /**
         * Gets an integer property, or -1 if undefined.
         */
        protected int getIntProperty(String key) {
            String s = getProperty(key);
            int value = -1;
            if (!isBlank(s)) {
                try {
                    value = Integer.parseInt(s.trim());
                } catch (NumberFormatException e) {
                    log.error("Cannot parse " + key + ": " + s);
                }
            }
            return value;
        }

        public String getSystemPropertyName(String propertyName) {
            return systemPropertyPrefix + "." + propertyName;
        }

    }

    public AmazonS3 getAmazonS3() {
        return amazonS3;
    }

    public static AmazonS3Client.Builder builder(String systemPropertyPrefix, Map<String, String> properties) {
        return new Builder(systemPropertyPrefix, properties);
    }

    public AWSCredentialsProvider getAwsCredentialsProvider() {
        return awsCredentialsProvider;
    }

}
