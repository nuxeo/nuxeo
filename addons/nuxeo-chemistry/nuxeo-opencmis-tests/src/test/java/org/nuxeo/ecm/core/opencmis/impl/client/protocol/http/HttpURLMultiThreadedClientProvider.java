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
 *     Stephane Lacoin (aka matic)
 */
package org.nuxeo.ecm.core.opencmis.impl.client.protocol.http;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class HttpURLMultiThreadedClientProvider implements HttpURLClientProvider {

    protected CloseableHttpClient client;

    protected CookieStore cookieStore;

    public HttpURLMultiThreadedClientProvider() {
        @SuppressWarnings("resource")
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(200);
        connectionManager.setDefaultMaxPerRoute(20);
        LaxRedirectStrategy redirectStrategy = new LaxRedirectStrategy();
        cookieStore = new BasicCookieStore();
        client = HttpClients.custom() //
                            .setConnectionManager(connectionManager)
                            .setRedirectStrategy(redirectStrategy)
                            .setDefaultCookieStore(cookieStore)
                            .build();
    }

    @Override
    public CloseableHttpClient getClient() {
        return client;
    }

    @Override
    public void setClient(CloseableHttpClient client) {
        this.client = client;
    }

    @Override
    public CookieStore getCookieStore() {
        return cookieStore;
    }

}
