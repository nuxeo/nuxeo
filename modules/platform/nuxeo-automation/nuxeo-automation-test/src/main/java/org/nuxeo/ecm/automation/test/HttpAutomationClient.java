/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.automation.test;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * @since 11.1
 */
public class HttpAutomationClient {

    protected static final Duration TIMEOUT = Duration.ofSeconds(60);

    protected final CloseableHttpClient client;

    protected final Supplier<String> baseUrlSupplier;

    public HttpAutomationClient(Supplier<String> baseUrlSupplier) {
        this.baseUrlSupplier = baseUrlSupplier;
        int timeoutMillis = (int) TIMEOUT.toMillis();
        RequestConfig requestConfig = RequestConfig.custom()
                                                   .setConnectTimeout(timeoutMillis)
                                                   .setConnectionRequestTimeout(timeoutMillis)
                                                   .setSocketTimeout(timeoutMillis)
                                                   .build();
        client = HttpClientBuilder.create() //
                                  .setDefaultRequestConfig(requestConfig)
                                  .build();
    }

    public void shutdown() {
        try {
            client.close();
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    public HttpAutomationSession getSession() throws IOException {
        String url = baseUrlSupplier.get();
        return new HttpAutomationSession(client, url);
    }

    public HttpAutomationSession getSession(String username, String password) throws IOException {
        HttpAutomationSession session = getSession();
        session.login(username, password);
        return session;
    }

}
