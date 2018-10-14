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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.restAPI;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

@RunWith(FeaturesRunner.class)
@Features(RestletFeature.class)
@ServletContainer(port = AbstractRestletTest.PORT)
public abstract class AbstractRestletTest {

    protected static final int PORT = 18090;

    protected static final String USERNAME = "admin";

    protected static final String PASSWORD = "pass";

    protected static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    protected HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

    protected void executeRequest(String path, String expectedContent) throws IOException, ClientProtocolException {
        executeRequest(path, HttpGet::new, expectedContent);
    }

    protected void executeRequest(String path, Function<String, HttpUriRequest> requestBuilder, String expectedContent)
            throws IOException, ClientProtocolException {
        String content = executeRequest(path, requestBuilder, SC_OK, "application/xml;charset=UTF-8");
        assertEquals(expectedContent, content);
    }

    protected void executeRequestNoContent(String path) throws IOException, ClientProtocolException {
        String uri = getUri(path);
        HttpGet request = new HttpGet(uri);
        setAuthorization(request);
        try (CloseableHttpClient httpClient = httpClientBuilder.build();
                CloseableHttpResponse response = httpClient.execute(request)) {
        }
    }

    protected String executeRequest(String path) throws IOException, ClientProtocolException {
        return executeRequest(path, HttpGet::new, SC_OK, "application/xml;charset=UTF-8");
    }

    protected String executeRequest(String path, Function<String, HttpUriRequest> requestBuilder, int expectedStatus,
            String expectedContentType) throws IOException, ClientProtocolException {
        return executeRequest(path, requestBuilder, expectedStatus, expectedContentType, null);
    }

    protected String executeRequest(String path, Function<String, HttpUriRequest> requestBuilder, int expectedStatus,
            String expectedContentType, String expectedContentDisposition) throws IOException, ClientProtocolException {
        String uri = getUri(path);
        HttpUriRequest request = requestBuilder.apply(uri);
        setAuthorization(request);
        try (CloseableHttpClient httpClient = httpClientBuilder.build();
                CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals(expectedStatus, response.getStatusLine().getStatusCode());
            assertEquals(expectedContentType, response.getFirstHeader("Content-Type").getValue());
            if (expectedContentDisposition != null) {
                assertEquals(expectedContentDisposition, response.getFirstHeader("Content-Disposition").getValue());
            }
            try (InputStream is = response.getEntity().getContent()) {
                return IOUtils.toString(is, UTF_8);
            }
        }
    }

    protected String getUri(String path) {
        return "http://localhost:" + PORT + "/restAPI" + path;
    }

    protected void setAuthorization(HttpUriRequest request) {
        setAuthorization(request, USERNAME);
    }

    protected void setAuthorization(HttpUriRequest request, String username) {
        String auth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + PASSWORD).getBytes(UTF_8));
        request.setHeader("Authorization", auth);
    }

}
