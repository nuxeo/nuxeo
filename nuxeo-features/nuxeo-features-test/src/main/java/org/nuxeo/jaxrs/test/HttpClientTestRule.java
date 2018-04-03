/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.jaxrs.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.google.common.base.Splitter;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * @since 10.1
 */
public class HttpClientTestRule implements TestRule {

    public static final String ADMINISTRATOR = "Administrator";

    private final String url;

    private final String username;

    private final String password;

    private final String accept;

    private final Map<String, String> headers;

    protected Client client;

    protected WebResource service;

    private HttpClientTestRule(Builder builder) {
        this.url = builder.url;
        this.username = builder.username;
        this.password = builder.password;
        this.accept = builder.accept;
        this.headers = builder.headers;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                starting();
                try {
                    base.evaluate();
                } finally {
                    finished();
                }
            }
        };
    }

    protected void starting() {
        client = JerseyClientHelper.clientBuilder().setCredentials(username, password).build();
        service = client.resource(url);
    }

    protected void finished() {
        client.destroy();
    }

    public CloseableClientResponse get(String path) {
        return execute(path, builder -> builder.get(ClientResponse.class));
    }

    public CloseableClientResponse post(String path, Object data) {
        return execute(path, builder -> builder.post(ClientResponse.class, data));
    }

    public CloseableClientResponse put(String path, Object data) {
        return execute(path, builder -> builder.put(ClientResponse.class, data));
    }

    public CloseableClientResponse delete(String path) {
        return execute(path, builder -> builder.delete(ClientResponse.class));
    }

    protected CloseableClientResponse execute(String path, Function<WebResource.Builder, ClientResponse> invoker) {
        // extract queryParams from path
        Map<String, String> queryParams = Collections.emptyMap();
        int interrogationIdx = path.indexOf('?');
        if (interrogationIdx >= 0) {
            queryParams = Splitter.on('&').withKeyValueSeparator('=').split(path.substring(interrogationIdx + 1));
            path = path.substring(0, interrogationIdx);
        }
        WebResource webResource = service.path(path);
        for (Entry<String, String> entry : queryParams.entrySet()) {
            webResource = webResource.queryParam(entry.getKey(), entry.getValue());
        }
        WebResource.Builder builder = webResource.accept(accept);
        headers.forEach(builder::header);
        return invoker.andThen(CloseableClientResponse::of).apply(builder);
    }

    /**
     * The http client test rule builder. This builder is used to pass default parameters to client and requests.
     */
    public static class Builder {

        private String url;

        private String username;

        private String password;

        private String accept;

        private Map<String, String> headers;

        public Builder() {
            this.url = System.getProperty("nuxeoURL", "http://localhost:8080/nuxeo").replaceAll("/$", "");
            this.username = null;
            this.password = null;
            this.accept = null;
            this.headers = new HashMap<>();
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder adminCredentials() {
            return credentials(ADMINISTRATOR, ADMINISTRATOR);
        }

        public Builder credentials(String username, String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        public Builder accept(String accept) {
            this.accept = accept;
            return this;
        }

        public Builder header(String key, String value) {
            headers.put(key, value);
            return this;
        }

        public HttpClientTestRule build() {
            return new HttpClientTestRule(this);
        }

    }

}
