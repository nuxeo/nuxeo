/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *     ataillefer
 */
package org.nuxeo.ecm.automation.client.jaxrs.impl;

import java.util.function.Supplier;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.nuxeo.ecm.automation.client.adapters.AsyncSessionFactory;
import org.nuxeo.ecm.automation.client.adapters.BusinessServiceFactory;
import org.nuxeo.ecm.automation.client.adapters.DocumentServiceFactory;
import org.nuxeo.ecm.automation.client.jaxrs.spi.AsyncAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Connector;
import org.nuxeo.ecm.automation.client.rest.api.RestClient;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class HttpAutomationClient extends AsyncAutomationClient {

    protected HttpClient http;

    protected int httpConnectionTimeout;

    /**
     * Instantiates a new {@link HttpAutomationClient} with no timeout for the HTTP connection and the default timeout
     * for the wait of the asynchronous thread pool termination: 2 seconds.
     */
    public HttpAutomationClient(String url) {
        this(url, 0);
    }

    /**
     * Instantiates a new {@link HttpAutomationClient} with the given timeout in milliseconds for the HTTP connection
     * and the default timeout for the wait of the asynchronous thread pool termination: 2 seconds.
     *
     * @since 5.7
     */
    public HttpAutomationClient(String url, int httpConnectionTimeout) {
        super(url);
        init(httpConnectionTimeout);
    }

    /**
     * Instantiates a new {@link HttpAutomationClient} with the given timeout in milliseconds for the HTTP connection
     * and the default timeout for the wait of the asynchronous thread pool termination: 2 seconds.
     *
     * @since 10.10
     */
    public HttpAutomationClient(Supplier<String> urlSupplier, int httpConnectionTimeout) {
        super(urlSupplier);
        init(httpConnectionTimeout);
    }

    /**
     * Instantiates a new {@link HttpAutomationClient} with the given timeout in milliseconds for the HTTP connection
     * and the given timeout in milliseconds for the wait of the asynchronous thread pool termination.
     *
     * @since 5.7
     */
    public HttpAutomationClient(String url, int httpConnectionTimeout, long asyncAwaitTerminationTimeout) {
        super(url, asyncAwaitTerminationTimeout);
        init(httpConnectionTimeout);
    }

    private void init(int httpConnectionTimeout) {
        http = new DefaultHttpClient(new PoolingClientConnectionManager());
        this.httpConnectionTimeout = httpConnectionTimeout;
        // http.setCookieSpecs(null);
        // http.setCookieStore(null);
        registerAdapter(new DocumentServiceFactory());
        registerAdapter(new BusinessServiceFactory());
        registerAdapter(new AsyncSessionFactory());
    }

    public void setProxy(String host, int port) {
        // httpclient.getCredentialsProvider().setCredentials(
        // new AuthScope(PROXY, PROXY_PORT),
        // new UsernamePasswordCredentials("username", "password"));

        http.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, new HttpHost(host, port));
    }

    public HttpClient http() {
        return http;
    }

    @Override
    public synchronized void shutdown() {
        super.shutdown();
        http.getConnectionManager().shutdown();
        http = null;
    }

    @Override
    protected Connector newConnector() {
        return new HttpConnector(http, httpConnectionTimeout);
    }

    /**
     * Returns the {@link RestClient} associated to this
     * {@link org.nuxeo.ecm.automation.client.jaxrs.impl.HttpAutomationClient}.
     *
     * @since 5.8
     */
    public RestClient getRestClient() {
        return new RestClient(this);
    }
}
