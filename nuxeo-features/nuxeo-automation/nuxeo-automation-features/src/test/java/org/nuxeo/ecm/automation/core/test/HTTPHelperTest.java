/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Ricardo Dias
 */
package org.nuxeo.ecm.automation.core.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockserver.integration.ClientAndProxy.startClientAndProxy;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.integration.ClientAndProxy;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;
import org.mockserver.model.BinaryBody;
import org.mockserver.model.Body;
import org.mockserver.model.Delay;
import org.mockserver.model.Header;
import org.mockserver.model.JsonBody;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.context.ContextHelper;
import org.nuxeo.ecm.automation.context.ContextService;
import org.nuxeo.ecm.automation.core.scripting.Scripting;
import org.nuxeo.ecm.automation.features.HTTPHelper;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy({ "org.nuxeo.ecm.automation.core", "org.nuxeo.ecm.automation.features" })
public class HTTPHelperTest {

    @Inject
    CoreSession session;

    @Inject
    ContextService ctxService;

    OperationContext ctx;

    protected ClientAndProxy proxy;

    protected ClientAndServer mockServer;

    protected final static String SERVER_HOST = "localhost";

    protected final static String SERVER_PATH = "/ws/path/";

    protected final static int SERVER_PORT = 1080;

    protected final static String SERVER_URL = "http://" + SERVER_HOST + ":" + String.valueOf(SERVER_PORT)
            + SERVER_PATH;

    protected final static String IMAGE_FILENAME = "sample.jpeg";

    private final JsonBody DEFAULT_HTTP_RESPONSE = new JsonBody("{\"message\": \"Default answer to requests.\"}");

    @Before
    public void setUp() throws Exception {
        mockServer = startClientAndServer(1080);
        proxy = startClientAndProxy(1090);

        ctx = new OperationContext(session);

        // assert that the helper is available
        Map<String, ContextHelper> contextHelperList = ctxService.getHelperFunctions();
        Object httpHelper = contextHelperList.get("HTTP");
        assertNotNull(httpHelper);
        assertTrue(httpHelper instanceof HTTPHelper);
    }

    @After
    public void tearDown() throws Exception {
        proxy.stop();
        mockServer.stop();
    }

    @Test
    public void testHTTPHelperGet() {
        createMockServer("GET", SERVER_PATH, DEFAULT_HTTP_RESPONSE);
        try {
            String expr = String.format(
                    "HTTP.get(\'%s\', {'auth' : { 'method' : 'basic', 'username' : 'test', 'password' : 'test' }})",
                    SERVER_URL);
            Blob resultBlob = (Blob) Scripting.newExpression(expr).eval(ctx);
            String result = IOUtils.toString(resultBlob.getStream(), "utf-8");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonResult = mapper.readValue(result, Map.class);

            String message = jsonResult.get("message");
            assertEquals("Default answer to requests.", message);
        } catch (IOException exception) {
            fail("Problem parsing the result. " + exception);
        }
    }

    @Test
    public void testHTTPHelperGetWithParams() {
        createMockServer("GET", SERVER_PATH, DEFAULT_HTTP_RESPONSE);
        try {
            String expr = String.format(
                    "HTTP.get(\'%s\', " + "{'auth' : { 'method' : 'basic', 'username' : 'test', 'password' : 'test' }, "
                            + "'params' : { 'param1' : [ 'value1' ] , 'param2' : [ 'value2' ] }})",
                    SERVER_URL);
            Blob resultBlob = (Blob) Scripting.newExpression(expr).eval(ctx);
            String result = IOUtils.toString(resultBlob.getStream(), "utf-8");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonResult = mapper.readValue(result, Map.class);

            String message = jsonResult.get("message");
            assertEquals("Default answer to requests.", message);
        } catch (IOException exception) {
            fail("Problem parsing the result. " + exception.getMessage());
        }
    }

    @Test
    public void testHTTPHelperGetWithHeaders() {
        createMockServer("GET", SERVER_PATH, DEFAULT_HTTP_RESPONSE);
        try {
            String expr = String.format(
                    "HTTP.get(\'%s\', " + "{'auth' : { 'method' : 'basic', 'username' : 'test', 'password' : 'test' }, "
                            + "'headers' : { 'Accept' : 'application/json' , 'User-Agent' : 'Mozilla/5.0' }})",
                    SERVER_URL);
            Blob resultBlob = (Blob) Scripting.newExpression(expr).eval(ctx);
            String result = IOUtils.toString(resultBlob.getStream(), "utf-8");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonResult = mapper.readValue(result, Map.class);

            String message = jsonResult.get("message");
            assertEquals("Default answer to requests.", message);
        } catch (IOException exception) {
            fail("Problem parsing the result. " + exception.getMessage());
        }
    }

    @Test
    public void testHTTPHelperPost() {
        createMockServer("POST", SERVER_PATH, DEFAULT_HTTP_RESPONSE);
        try {
            String expr = String.format(
                    "HTTP.post(\'%s\', 'Test', {'auth' : { 'method' : 'basic', 'username' : 'test', 'password' : 'test' }})",
                    SERVER_URL);
            Blob resultBlob = (Blob) Scripting.newExpression(expr).eval(ctx);
            String result = IOUtils.toString(resultBlob.getStream(), "utf-8");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonResult = mapper.readValue(result, Map.class);

            String message = jsonResult.get("message");
            assertEquals("Default answer to requests.", message);
        } catch (IOException exception) {
            fail("Problem parsing the result. " + exception);
        }
    }

    @Test
    public void testHTTPHelperPut() {
        createMockServer("PUT", SERVER_PATH, DEFAULT_HTTP_RESPONSE);
        try {
            String expr = String.format(
                    "HTTP.put(\'%s\', 'Test', {'auth' : { 'method' : 'basic', 'username' : 'test', 'password' : 'test' }})",
                    SERVER_URL);
            Blob resultBlob = (Blob) Scripting.newExpression(expr).eval(ctx);
            String result = IOUtils.toString(resultBlob.getStream(), "utf-8");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonResult = mapper.readValue(result, Map.class);

            String message = jsonResult.get("message");
            assertEquals("Default answer to requests.", message);
        } catch (IOException exception) {
            fail("Problem parsing the result. " + exception);
        }
    }

    @Test
    public void testHTTPHelperDelete() {
        createMockServer("DELETE", SERVER_PATH, DEFAULT_HTTP_RESPONSE);
        try {
            String expr = String.format(
                    "HTTP.delete(\'%s\', 'Test', {'auth' : { 'method' : 'basic', 'username' : 'test', 'password' : 'test' }})",
                    SERVER_URL);
            Blob resultBlob = (Blob) Scripting.newExpression(expr).eval(ctx);
            String result = IOUtils.toString(resultBlob.getStream(), "utf-8");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonResult = mapper.readValue(result, Map.class);

            String message = jsonResult.get("message");
            assertEquals("Default answer to requests.", message);
        } catch (IOException exception) {
            fail("Problem parsing the result. " + exception);
        }
    }

    @Test
    public void testHTTPHelperGetDownloadFile() {
        try {
            File file = org.nuxeo.common.utils.FileUtils.getResourceFileFromContext("test-data/sample.jpeg");
            byte[] answer = FileUtils.readFileToByteArray(file);
            Body responseBody = new BinaryBody(answer);
            createMockServer("GET", SERVER_PATH + IMAGE_FILENAME, responseBody);
        } catch (IOException e) {
            fail("Error reading the image file." + e.getMessage());
        }

        String expr = String.format(
                "HTTP.get(\'%s\', {'auth' : { 'method' : 'basic', 'username' : 'test', 'password' : 'test' }})",
                SERVER_URL + IMAGE_FILENAME);
        Blob httpResult = (Blob) Scripting.newExpression(expr).eval(ctx);
        assertTrue(httpResult.getLength() > 0);
        assertEquals(httpResult.getFilename(), IMAGE_FILENAME);
    }

    /**
     * Create a mock server that answers http requests with a certain set of headers and a body answer.
     *
     * @param method the HTTP request (GET, POST, PUT, DELETE)
     * @param path the path of the requests
     * @param answer the body of the http response
     */
    public void createMockServer(String method, String path, Body answer) {
        List<Header> requestHeaders = new ArrayList<>();
        requestHeaders.add(new Header(HttpHeaders.AUTHORIZATION, "Basic dGVzdDp0ZXN0"));

        List<Header> responseHeaders = new ArrayList<>();
        responseHeaders.add(new Header(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8"));
        responseHeaders.add(new Header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400"));

        new MockServerClient(SERVER_HOST, SERVER_PORT)
                  .when(request().withHeaders(requestHeaders)
                                 .withMethod(method)
                                 .withPath(path),
                          Times.exactly(2))
                  .respond(response().withStatusCode(200)
                                     .withHeaders(responseHeaders)
                                     .withBody(answer)
                                     .withDelay(new Delay(TimeUnit.SECONDS, 1)));
    }
}
