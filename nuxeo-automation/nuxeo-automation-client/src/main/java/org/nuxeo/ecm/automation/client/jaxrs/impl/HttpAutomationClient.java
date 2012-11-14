/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 *     ataillefer
 */
package org.nuxeo.ecm.automation.client.jaxrs.impl;

import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.nuxeo.ecm.automation.client.adapters.DocumentServiceFactory;
import org.nuxeo.ecm.automation.client.jaxrs.spi.AsyncAutomationClient;
import org.nuxeo.ecm.automation.client.jaxrs.spi.Connector;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class HttpAutomationClient extends AsyncAutomationClient {

    protected DefaultHttpClient http;

    protected long httpConnectionTimeout;

    /**
     * Instantiates a new {@link HttpAutomationClient} with no timeout for the
     * HTTP connection.
     */
    public HttpAutomationClient(String url) {
        this(url, 0);
    }

    /**
     * Instantiates a new {@link HttpAutomationClient} with a timeout for the
     * HTTP connection.
     *
     * @since 5.7
     */
    public HttpAutomationClient(String url, long httpConnectionTimeout) {
        super(url);
        http = new DefaultHttpClient();
        this.httpConnectionTimeout = httpConnectionTimeout;
        // http.setCookieSpecs(null);
        // http.setCookieStore(null);
        registerAdapter(new DocumentServiceFactory());
    }

    public void setProxy(String host, int port) {
        // httpclient.getCredentialsProvider().setCredentials(
        // new AuthScope(PROXY, PROXY_PORT),
        // new UsernamePasswordCredentials("username", "password"));

        http.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
                new HttpHost(host, port));
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
}
