/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.jaxrs.test;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.client.apache4.ApacheHttpClient4Handler;

/**
 * Helper for using the Jersey 1 {@link Client}.
 *
 * @since 9.3
 */
public final class JerseyClientHelper {

    public static final int DEFAULT_CONNECTION_TIMEOUT = 60 * 1000; // 60 seconds

    /**
     * Default instance of a Jersey 1 {@link Client} relying on the Apache HTTP client,
     * {@link #DEFAULT_CONNECTION_TIMEOUT} for the connection timeout parameters and no authentication.
     */
    public static final Client DEFAULT_CLIENT = clientBuilder().build();

    private JerseyClientHelper() {
        // Helper class
    }

    /**
     * Allows to build a custom instance of a Jersey 1 {@link Client} relying on the Apache HTTP client.
     * <p>
     * For instance, you can set the connection timeout and credentials for basic authentication by calling:
     *
     * <pre>
     * {@code
     *     Client client = JerseyClientHelper.clientBuilder()
     *                                       .setConnectTimeout(5000)
     *                                       .setCredentials("joe", "password")
     *                                       .build();
     * }
     *
     * <pre>
     */
    public static ApacheHttpClientBuilder clientBuilder() {
        return new ApacheHttpClientBuilder();
    }

    public static class ApacheHttpClientBuilder {

        protected int socketTimeout;

        protected int connectTimeout;

        protected int connectionRequestTimeout;

        protected boolean redirectsEnabled;

        protected String username;

        protected String password;

        protected ApacheHttpClientBuilder() {
            this.socketTimeout = DEFAULT_CONNECTION_TIMEOUT;
            this.connectTimeout = DEFAULT_CONNECTION_TIMEOUT;
            this.connectionRequestTimeout = DEFAULT_CONNECTION_TIMEOUT;
            this.redirectsEnabled = true;
        }

        public ApacheHttpClientBuilder setSocketTimeout(final int socketTimeout) {
            this.socketTimeout = socketTimeout;
            return this;
        }

        public ApacheHttpClientBuilder setConnectTimeout(final int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public ApacheHttpClientBuilder setConnectionRequestTimeout(final int connectionRequestTimeout) {
            this.connectionRequestTimeout = connectionRequestTimeout;
            return this;
        }

        public ApacheHttpClientBuilder setRedirectsEnabled(final boolean redirectsEnabled) {
            this.redirectsEnabled = redirectsEnabled;
            return this;
        }

        public ApacheHttpClientBuilder setCredentials(final String username, final String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        public Client build() {
            RequestConfig requestConfig = RequestConfig.custom()
                                                       .setSocketTimeout(socketTimeout)
                                                       .setConnectTimeout(connectTimeout)
                                                       .setConnectionRequestTimeout(connectionRequestTimeout)
                                                       .setRedirectsEnabled(redirectsEnabled)
                                                       .build();
            HttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(requestConfig).build();
            Client client = new Client(new ApacheHttpClient4Handler(httpClient, new BasicCookieStore(), true));
            if (username != null && password != null) {
                client.addFilter(new HTTPBasicAuthFilter(username, password));
            }
            return client;
        }

    }

}
