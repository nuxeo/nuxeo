/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.client;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.elasticsearch.api.ESClient;
import org.nuxeo.elasticsearch.api.ESClientFactory;
import org.nuxeo.elasticsearch.config.ElasticSearchClientConfig;
import org.nuxeo.elasticsearch.config.ElasticSearchEmbeddedServerConfig;
import org.nuxeo.elasticsearch.core.ElasticSearchEmbeddedNode;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 9.3
 */
public class ESRestClientFactory implements ESClientFactory {
    private static final Log log = LogFactory.getLog(ESRestClientFactory.class);

    public static final String DEFAULT_CONNECT_TIMEOUT_MS = "5000";

    public static final String DEFAULT_SOCKET_TIMEOUT_MS = "20000";

    public static final String CONNECTION_TIMEOUT_MS_OPT = "connection.timeout.ms";

    public static final String SOCKET_TIMEOUT_MS_OPT = "socket.timeout.ms";

    public static final String AUTH_USER_OPT = "username";

    public static final String AUTH_PASSWORD_OPT = "password";

    /** @since 10.3 */
    public static final String TRUST_STORE_PATH_OPT = "trustStorePath";

    /** @since 10.3 */
    public static final String TRUST_STORE_PASSWORD_OPT = "trustStorePassword";

    /** @since 10.3 */
    public static final String TRUST_STORE_TYPE_OPT = "trustStoreType";

    /** @since 10.3 */
    public static final String KEY_STORE_PATH_OPT = "keyStorePath";

    /** @since 10.3 */
    public static final String KEY_STORE_PASSWORD_OPT = "keyStorePassword";

    /** @since 10.3 */
    public static final String KEY_STORE_TYPE_OPT = "keyStoreType";

    /** @deprecated since 10.3, misnamed, use {@link #TRUST_STORE_PATH_OPT} instead */
    @Deprecated
    public static final String DEPRECATED_TRUST_STORE_PATH_OPT = "keystore.path";

    /** @deprecated since 10.3, misnamed, use {@link #TRUST_STORE_PATH_OPT} instead */
    @Deprecated
    public static final String DEPRECATED_TRUST_STORE_PASSWORD_OPT = "keystore.password";

    /**
     * @since 9.10-HF01
     * @deprecated since 10.3, misnamed, use {@link #TRUST_STORE_PATH_OPT} instead
     */
    @Deprecated
    public static final String DEPRECATED_TRUST_STORE_TYPE_OPT = "keystore.type";

    @Override
    public ESClient create(ElasticSearchEmbeddedNode node, ElasticSearchClientConfig config) {
        if (node != null) {
            return createLocalRestClient(node.getConfig());
        }
        return createRestClient(config);
    }

    protected ESClient createLocalRestClient(ElasticSearchEmbeddedServerConfig serverConfig) {
        if (!serverConfig.httpEnabled()) {
            throw new IllegalArgumentException(
                    "Embedded configuration has no HTTP port enable, use TransportClient instead of Rest");
        }
        RestClientBuilder lowLevelRestClientBuilder = RestClient.builder(
                new HttpHost("localhost", Integer.parseInt(serverConfig.getHttpPort())));
        RestHighLevelClient client = new RestHighLevelClient(lowLevelRestClientBuilder); // NOSONAR (factory)
        // checkConnection(client);
        return new ESRestClient(client.getLowLevelClient(), client);
    }

    protected ESClient createRestClient(ElasticSearchClientConfig config) {
        String addressList = config.getOption("addressList", "");
        if (addressList.isEmpty()) {
            throw new IllegalArgumentException("No addressList option provided cannot connect RestClient");
        }
        String[] hosts = addressList.split(",");
        HttpHost[] httpHosts = new HttpHost[hosts.length];
        int i = 0;
        for (String host : hosts) {
            httpHosts[i++] = HttpHost.create(host);
        }
        RestClientBuilder builder = RestClient.builder(httpHosts)
                                              .setRequestConfigCallback(
                                                      requestConfigBuilder -> requestConfigBuilder.setConnectTimeout(
                                                              getConnectTimeoutMs(config)).setSocketTimeout(
                                                                      getSocketTimeoutMs(config)))
                                              .setMaxRetryTimeoutMillis(getConnectTimeoutMs(config));
        addClientCallback(config, builder);
        RestHighLevelClient client = new RestHighLevelClient(builder);  // NOSONAR (factory)
        // checkConnection(client);
        return new ESRestClient(client.getLowLevelClient(), client);
    }

    private void addClientCallback(ElasticSearchClientConfig config, RestClientBuilder builder) {
        BasicCredentialsProvider credentialProvider = getCredentialProvider(config);
        SSLContext sslContext = getSslContext(config);
        if (sslContext == null && credentialProvider == null) {
            return;
        }
        builder.setHttpClientConfigCallback(httpClientBuilder -> {
            httpClientBuilder.setSSLContext(sslContext);
            httpClientBuilder.setDefaultCredentialsProvider(credentialProvider);
            return httpClientBuilder;
        });
    }

    protected BasicCredentialsProvider getCredentialProvider(ElasticSearchClientConfig config) {
        if (StringUtils.isBlank(config.getOption(AUTH_USER_OPT))) {
            return null;
        }
        String user = config.getOption(AUTH_USER_OPT);
        String password = config.getOption(AUTH_PASSWORD_OPT);
        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
        return credentialsProvider;
    }

    protected SSLContext getSslContext(ElasticSearchClientConfig config) {
        checkDeprecatedProperties();
        String trustStorePath = StringUtils.defaultIfBlank(config.getOption(TRUST_STORE_PATH_OPT),
                config.getOption(DEPRECATED_TRUST_STORE_PATH_OPT));
        String trustStorePassword = StringUtils.defaultIfBlank(config.getOption(TRUST_STORE_PASSWORD_OPT),
                config.getOption(DEPRECATED_TRUST_STORE_PASSWORD_OPT));
        String trustStoreType = StringUtils.defaultIfBlank(config.getOption(TRUST_STORE_TYPE_OPT),
                config.getOption(DEPRECATED_TRUST_STORE_TYPE_OPT));
        String keyStorePath = config.getOption(KEY_STORE_PATH_OPT);
        String keyStorePassword = config.getOption(KEY_STORE_PASSWORD_OPT);
        String keyStoreType = config.getOption(KEY_STORE_TYPE_OPT);
        try {
            KeyStore trustStore = loadKeyStore(trustStorePath, trustStorePassword, trustStoreType);
            KeyStore keyStore = loadKeyStore(keyStorePath, keyStorePassword, keyStoreType);
            if (trustStore == null && keyStore == null) {
                return null;
            }
            SSLContextBuilder sslContextBuilder = SSLContexts.custom();
            if (trustStore != null) {
                sslContextBuilder.loadTrustMaterial(trustStore, null);
            }
            if (keyStore != null) {
                sslContextBuilder.loadKeyMaterial(keyStore, null);
            }
            return sslContextBuilder.build();
        } catch (GeneralSecurityException | IOException e) {
            throw new NuxeoException("Cannot setup SSL for RestClient: " + config, e);
        }
    }

    // deprecated and new system properties, used in warnings only
    // actual values are used in templates and accessed through the options map of ElasticSearchClientConfig

    protected static final String DEPRECATED_ES_TRUST_STORE_PATH_PROP = "elasticsearch.restClient.keystorePath";

    protected static final String DEPRECATED_ES_TRUST_STORE_PASSWORD_PROP = "elasticsearch.restClient.keystorePassword";

    protected static final String DEPRECATED_ES_TRUST_STORE_TYPE_PROP = "elasticsearch.restClient.keystoreType";

    protected static final String ES_TRUST_STORE_PATH_PROP = "elasticsearch.restClient.truststore.path";

    protected static final String ES_TRUST_STORE_PASSWORD_PROP = "elasticsearch.restClient.truststore.password";

    protected static final String ES_TRUST_STORE_TYPE_PROP = "elasticsearch.restClient.truststore.type";

    protected void checkDeprecatedProperties() {
        checkDeprecatedProperty(DEPRECATED_ES_TRUST_STORE_PATH_PROP, ES_TRUST_STORE_PATH_PROP);
        checkDeprecatedProperty(DEPRECATED_ES_TRUST_STORE_PASSWORD_PROP, ES_TRUST_STORE_PASSWORD_PROP);
        checkDeprecatedProperty(DEPRECATED_ES_TRUST_STORE_TYPE_PROP, ES_TRUST_STORE_TYPE_PROP);
    }

    protected void checkDeprecatedProperty(String oldProp, String newProp) {
        if (Framework.getRuntime() == null) {
            // unit tests
            return;
        }
        if (StringUtils.isNotBlank(Framework.getProperty(oldProp))) {
            log.warn("Configuration property " + oldProp + " is deprecated, use " + newProp + " instead");
        }
    }

    protected KeyStore loadKeyStore(String path, String password, String type)
            throws GeneralSecurityException, IOException {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        String keyStoreType = StringUtils.defaultIfBlank(type, KeyStore.getDefaultType());
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        char[] passwordChars = StringUtils.isBlank(password) ? null : password.toCharArray();
        try (InputStream is = Files.newInputStream(Paths.get(path))) {
            keyStore.load(is, passwordChars);
        }
        return keyStore;
    }

    protected int getConnectTimeoutMs(ElasticSearchClientConfig config) {
        return Integer.parseInt(config.getOption(CONNECTION_TIMEOUT_MS_OPT, DEFAULT_CONNECT_TIMEOUT_MS));
    }

    protected int getSocketTimeoutMs(ElasticSearchClientConfig config) {
        return Integer.parseInt(config.getOption(SOCKET_TIMEOUT_MS_OPT, DEFAULT_SOCKET_TIMEOUT_MS));
    }

    protected void checkConnection(RestHighLevelClient client) {
        boolean ping = false;
        try {
            ping = client.ping();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        if (!ping) {
            throw new IllegalStateException("Fail to ping rest node");
        }
    }
}
