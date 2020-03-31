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
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.client.MockServerClient;
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
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features(PlatformFeature.class)
@Deploy("org.nuxeo.ecm.automation.core")
@Deploy("org.nuxeo.ecm.automation.features")
public class HTTPHelperTest {

    private static final Logger log = LogManager.getLogger(HTTPHelperTest.class);

    @Inject
    protected CoreSession session;

    @Inject
    protected ContextService ctxService;

    protected OperationContext ctx;

    protected ClientAndServer mockServer;

    protected int serverPort;

    protected String serverURL;

    protected static final int RETRIES = 1000;

    protected static final String SERVER_HOST = "localhost";

    protected static final String SERVER_PATH = "/ws/path/";

    protected static final String IMAGE_FILENAME = "sample.jpeg";

    protected static final JsonBody DEFAULT_HTTP_RESPONSE = new JsonBody(
            "{\"message\": \"Default answer to requests.\"}");

    @Before
    public void setUp() {
        serverPort = getFreePort();
        serverURL = "http://" + SERVER_HOST + ":" + serverPort + SERVER_PATH;
        mockServer = startClientAndServer(serverPort);

        ctx = new OperationContext(session);

        // assert that the helper is available
        Map<String, ContextHelper> contextHelperList = ctxService.getHelperFunctions();
        Object httpHelper = contextHelperList.get("HTTP");
        assertNotNull(httpHelper);
        assertTrue(httpHelper instanceof HTTPHelper);
    }

    @After
    public void tearDown() {
        mockServer.stop();
    }

    @Test
    public void testHTTPHelperGet() {
        try (var mockServerClient = createMockServer("GET", SERVER_PATH, DEFAULT_HTTP_RESPONSE)) {
            String expr = String.format(
                    "HTTP.get(\'%s\', {'auth' : { 'method' : 'basic', 'username' : 'test', 'password' : 'test' }})",
                    serverURL);
            Blob resultBlob = (Blob) Scripting.newExpression(expr).eval(ctx);
            String result = IOUtils.toString(resultBlob.getStream(), "utf-8"); // NOSONAR
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonResult = mapper.readValue(result, new TypeReference<Map<String, String>>() {
            });

            String message = jsonResult.get("message"); // NOSONAR
            assertEquals("Default answer to requests.", message); // NOSONAR
        } catch (IOException exception) {
            fail("Problem parsing the result. " + exception); // NOSONAR
        }
    }

    @Test
    public void testHTTPHelperGetWithParams() {
        try (var mockServerClient = createMockServer("GET", SERVER_PATH, DEFAULT_HTTP_RESPONSE)) {
            String expr = String.format(
                    "HTTP.get(\'%s\', " + "{'auth' : { 'method' : 'basic', 'username' : 'test', 'password' : 'test' }, "
                            + "'params' : { 'param1' : [ 'value1' ] , 'param2' : [ 'value2' ] }})",
                    serverURL);
            Blob resultBlob = (Blob) Scripting.newExpression(expr).eval(ctx);
            String result = IOUtils.toString(resultBlob.getStream(), "utf-8");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonResult = mapper.readValue(result, new TypeReference<Map<String, String>>() {
            });

            String message = jsonResult.get("message");
            assertEquals("Default answer to requests.", message);
        } catch (IOException exception) {
            fail("Problem parsing the result. " + exception.getMessage());
        }
    }

    @Test
    public void testHTTPHelperGetWithHeaders() {
        try (var mockServerClient = createMockServer("GET", SERVER_PATH, DEFAULT_HTTP_RESPONSE)) {
            String expr = String.format(
                    "HTTP.get(\'%s\', " + "{'auth' : { 'method' : 'basic', 'username' : 'test', 'password' : 'test' }, "
                            + "'headers' : { 'Accept' : 'application/json' , 'User-Agent' : 'Mozilla/5.0' }})",
                    serverURL);
            Blob resultBlob = (Blob) Scripting.newExpression(expr).eval(ctx);
            String result = IOUtils.toString(resultBlob.getStream(), "utf-8");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonResult = mapper.readValue(result, new TypeReference<Map<String, String>>() {
            });

            String message = jsonResult.get("message");
            assertEquals("Default answer to requests.", message);
        } catch (IOException exception) {
            fail("Problem parsing the result. " + exception.getMessage());
        }
    }

    @Test
    public void testHTTPHelperPost() {
        try (var mockServerClient = createMockServer("POST", SERVER_PATH, DEFAULT_HTTP_RESPONSE)) {
            String expr = String.format(
                    "HTTP.post(\'%s\', 'Test', {'auth' : { 'method' : 'basic', 'username' : 'test', 'password' : 'test' }})",
                    serverURL);
            Blob resultBlob = (Blob) Scripting.newExpression(expr).eval(ctx);
            String result = IOUtils.toString(resultBlob.getStream(), "utf-8");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonResult = mapper.readValue(result, new TypeReference<Map<String, String>>() {
            });

            String message = jsonResult.get("message");
            assertEquals("Default answer to requests.", message);
        } catch (IOException exception) {
            fail("Problem parsing the result. " + exception);
        }
    }

    @Test
    public void testHTTPHelperPut() {
        try (var mockServerClient = createMockServer("PUT", SERVER_PATH, DEFAULT_HTTP_RESPONSE)) {
            String expr = String.format(
                    "HTTP.put(\'%s\', 'Test', {'auth' : { 'method' : 'basic', 'username' : 'test', 'password' : 'test' }})",
                    serverURL);
            Blob resultBlob = (Blob) Scripting.newExpression(expr).eval(ctx);
            String result = IOUtils.toString(resultBlob.getStream(), "utf-8");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonResult = mapper.readValue(result, new TypeReference<Map<String, String>>() {
            });

            String message = jsonResult.get("message");
            assertEquals("Default answer to requests.", message);
        } catch (IOException exception) {
            fail("Problem parsing the result. " + exception);
        }
    }

    @Test
    public void testHTTPHelperDelete() {
        try (var mockServerClient = createMockServer("DELETE", SERVER_PATH, DEFAULT_HTTP_RESPONSE)) {
            String expr = String.format(
                    "HTTP.delete(\'%s\', 'Test', {'auth' : { 'method' : 'basic', 'username' : 'test', 'password' : 'test' }})",
                    serverURL);
            Blob resultBlob = (Blob) Scripting.newExpression(expr).eval(ctx);
            String result = IOUtils.toString(resultBlob.getStream(), "utf-8");
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> jsonResult = mapper.readValue(result, new TypeReference<Map<String, String>>() {
            });

            String message = jsonResult.get("message");
            assertEquals("Default answer to requests.", message);
        } catch (IOException exception) {
            fail("Problem parsing the result. " + exception);
        }
    }

    @Test
    public void testHTTPHelperGetDownloadFile() {
        @SuppressWarnings("rawtypes")
        Body responseBody = null;
        try {
            File file = org.nuxeo.common.utils.FileUtils.getResourceFileFromContext("test-data/sample.jpeg");
            byte[] answer = FileUtils.readFileToByteArray(file);
            responseBody = new BinaryBody(answer);
        } catch (IOException e) {
            fail("Error reading the image file." + e.getMessage());
        }

        try (var mockServerClient = createMockServer("GET", SERVER_PATH + IMAGE_FILENAME, responseBody)) {
            String expr = String.format(
                    "HTTP.get(\'%s\', {'auth' : { 'method' : 'basic', 'username' : 'test', 'password' : 'test' }})",
                    serverURL + IMAGE_FILENAME);
            Blob httpResult = (Blob) Scripting.newExpression(expr).eval(ctx);
            assertTrue(httpResult.getLength() > 0);
            assertEquals(httpResult.getFilename(), IMAGE_FILENAME);
        }
    }

    /**
     * Create a mock server that answers http requests with a certain set of headers and a body answer.
     *
     * @param method the HTTP request (GET, POST, PUT, DELETE)
     * @param path the path of the requests
     * @param answer the body of the http response
     */
    public MockServerClient createMockServer(String method, String path, @SuppressWarnings("rawtypes") Body answer) {
        List<Header> requestHeaders = new ArrayList<>();
        requestHeaders.add(new Header(HttpHeaders.AUTHORIZATION, "Basic dGVzdDp0ZXN0"));

        List<Header> responseHeaders = new ArrayList<>();
        responseHeaders.add(new Header(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8"));
        responseHeaders.add(new Header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400"));

        MockServerClient mockServerClient = new MockServerClient(SERVER_HOST, serverPort);
        mockServerClient.when(request().withHeaders(requestHeaders).withMethod(method).withPath(path), Times.exactly(2))
                        .respond(response().withStatusCode(200)
                                           .withHeaders(responseHeaders)
                                           .withBody(answer.getRawBytes())
                                           .withDelay(new Delay(TimeUnit.SECONDS, 1)));
        return mockServerClient;
    }

    /**
     * Try to find a free port on which a socket will be listening.
     *
     * @return a free port number if any
     * @throws NuxeoException if we cannot find a free port
     * @since 11.1
     */
    protected int getFreePort() {
        int retryCount = 0;
        while (retryCount < RETRIES) {
            try (ServerSocket socket = new ServerSocket(0)) {
                socket.setReuseAddress(true);
                return socket.getLocalPort();
            } catch (IOException e) {
                retryCount++;
                log.trace("Failed to allocate port on retry {}", retryCount, e);
            }
        }
        throw new NuxeoException(String.format("Unable to find free port after %d retries", retryCount));
    }

}
