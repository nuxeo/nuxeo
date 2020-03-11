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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpStatus.SC_OK;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.AbstractHttpMessage;
import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * @since 11.1
 */
public class HttpAutomationSession {

    // must not be closed here, done by whoever constructed us
    protected final CloseableHttpClient client;

    protected final String baseURL;

    protected boolean async;

    protected String username;

    protected String password;

    protected Map<String, String> authHeaders;

    public HttpAutomationSession(CloseableHttpClient client, String baseURL) {
        this.client = client;
        this.baseURL = baseURL;
    }

    public void setAsync(boolean async) {
        this.async = async;
    }

    public HttpAutomationRequest newRequest() {
        return new HttpAutomationRequest(this, null);
    }

    public HttpAutomationRequest newRequest(String operationId) {
        return new HttpAutomationRequest(this, operationId);
    }

    protected void addAuthentication(AbstractHttpMessage request) {
        if (username != null && password != null) {
            String info = username + ":" + password;
            String authorization = "Basic " + Base64.encodeBase64String(info.getBytes(UTF_8));
            request.setHeader(AUTHORIZATION, authorization);
        } else if (authHeaders != null) {
            authHeaders.forEach(request::setHeader);
        } else {
            throw new NuxeoException("Missing auth");
        }
    }

    public void login(String username, String password) throws IOException {
        this.username = username;
        this.password = password;
        login(SC_OK);
    }

    public String login(Map<String, String> authHeaders) throws IOException {
        return login(authHeaders, SC_OK);
    }

    public String login(Map<String, String> authHeaders, int expectedStatusCode) throws IOException {
        this.authHeaders = authHeaders;
        return login(expectedStatusCode);
    }

    protected String login(int expectedStatusCode) throws IOException {
        return newRequest().login(expectedStatusCode);
    }

    /**
     * Converts a property list into a string representation suitable for server-side interpretation.
     */
    public String propertyMapToString(Map<String, Object> map) {
        StringBuilder properties = new StringBuilder();
        map.forEach((k, v) -> properties.append(k).append('=').append(v).append('\n'));
        return properties.toString();
    }

}
