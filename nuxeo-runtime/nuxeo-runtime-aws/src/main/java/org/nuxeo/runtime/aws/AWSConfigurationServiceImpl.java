/*
 * (C) Copyright 2018-2021 Nuxeo (http://nuxeo.com/) and others.
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
 *     Remi Cattiau
 *     Florent Guillaume
 */
package org.nuxeo.runtime.aws;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.nuxeo.runtime.aws.AWSConfigurationDescriptor.DEFAULT_CONFIG_ID;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.nuxeo.runtime.model.DefaultComponent;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;

/**
 * Implementation of the service providing AWS configuration.
 * <p>
 * This service does a simple lookup in provided Nuxeo configuration. Instead of this service, you should probably use
 * {@link NuxeoAWSCredentialsProvider} and {@link NuxeoAWSRegionProvider} because they fall back to the default AWS SDK
 * lookup behavior if no Nuxeo configuration is available.
 *
 * @since 10.3
 * @see NuxeoAWSCredentialsProvider
 * @see NuxeoAWSRegionProvider
 */
public class AWSConfigurationServiceImpl extends DefaultComponent implements AWSConfigurationService {

    public static final String XP_CONFIGURATION = "configuration";

    @Override
    public AWSCredentials getAWSCredentials(String id) {
        AWSConfigurationDescriptor descriptor = getDescriptor(XP_CONFIGURATION, defaultIfBlank(id, DEFAULT_CONFIG_ID));
        if (descriptor != null) {
            String accessKeyId = descriptor.getAccessKeyId();
            String secretKey = descriptor.getSecretKey();
            String sessionToken = descriptor.getSessionToken();
            if (isNotBlank(accessKeyId) && isNotBlank(secretKey)) {
                if (isNotBlank(sessionToken)) {
                    return new BasicSessionCredentials(accessKeyId, secretKey, sessionToken);
                } else {
                    return new BasicAWSCredentials(accessKeyId, secretKey);
                }
            }
        }
        return null;
    }

    @Override
    public String getAWSRegion(String id) {
        AWSConfigurationDescriptor descriptor = getDescriptor(XP_CONFIGURATION, defaultIfBlank(id, DEFAULT_CONFIG_ID));
        if (descriptor != null) {
            String region = descriptor.getRegion();
            if (isNotBlank(region)) {
                return region;
            }
        }
        return null;
    }

    /**
     * Configures a client configuration with a custom socket factory.
     *
     * @since 2021.10
     */
    @Override
    public void configureSSL(String id, ClientConfiguration config) {
        SSLContext sslContext = getSSLContext(getDescriptor(XP_CONFIGURATION, defaultIfBlank(id, DEFAULT_CONFIG_ID)));
        if (sslContext != null) {
            SSLConnectionSocketFactory factory = new SSLConnectionSocketFactory(sslContext);
            config.getApacheHttpClientConfig().setSslSocketFactory(factory);
        }
    }

    protected SSLContext getSSLContext(AWSConfigurationDescriptor descriptor) {
        if (descriptor == null) {
            return null;
        }
        try {
            KeyStore trustStore = loadKeyStore(descriptor.trustStorePath, descriptor.trustStorePassword,
                    descriptor.trustStoreType);
            KeyStore keyStore = loadKeyStore(descriptor.keyStorePath, descriptor.keyStorePassword,
                    descriptor.keyStoreType);
            if (trustStore == null && keyStore == null) {
                return null;
            }

            SSLContextBuilder sslContextBuilder = SSLContexts.custom();
            if (trustStore != null) {
                sslContextBuilder.loadTrustMaterial(trustStore, null);
            }
            if (keyStore != null) {
                sslContextBuilder.loadKeyMaterial(keyStore,
                        isBlank(descriptor.keyStorePassword) ? null : descriptor.keyStorePassword.toCharArray());
            }
            return sslContextBuilder.build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Cannot setup SSL context", e);
        }
    }

    protected KeyStore loadKeyStore(String path, String password, String type)
            throws GeneralSecurityException, IOException {
        if (isBlank(path)) {
            return null;
        }
        String keyStoreType = defaultIfBlank(type, KeyStore.getDefaultType());
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        char[] passwordChars = isBlank(password) ? null : password.toCharArray();
        try (InputStream is = Files.newInputStream(Paths.get(path))) {
            keyStore.load(is, passwordChars);
        }
        return keyStore;
    }

}
