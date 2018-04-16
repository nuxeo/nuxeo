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
 *     Florent Guillaume
 */
package org.nuxeo.test.runner.feature;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.test.runner.ServletContainerFeature;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import sun.net.www.http.HttpClient;

/**
 * Tests that {@link ServletContainerFeature#disableSunHttpClientRetryPostProp} properly disables the
 * {@code retryPostProp} property of {@link sun.net.www.http.HttpClient}.
 *
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class RetryPostTest {

    protected static final int PORT = 18090;

    protected static final int CLIENT_TIMEOUT = 5 * 60 * 1000; // 5 minutes

    protected WebResource webResource;

    protected DummyHttpServer dummyServer;

    protected Thread thread;

    /**
     * Dummy http server which, on first run, closes the connection abruptly, so that the client receives an
     * IOException.
     */
    protected static class DummyHttpServer implements Runnable {

        protected final int port;

        protected ServerSocket serverSocket;

        protected volatile boolean firstRun;

        public DummyHttpServer(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                this.serverSocket = serverSocket;
                serverSocket.setSoTimeout(1000);
                for (;;) {
                    try (Socket clientSocket = serverSocket.accept();
                            PrintWriter out = getWriter(clientSocket);
                            BufferedReader in = getReader(clientSocket)) {
                        for (;;) {
                            String line = in.readLine();
                            if (line == null) {
                                return;
                            }
                            if (line.startsWith("POST ")) {
                                if (firstRun) {
                                    firstRun = false;
                                    // break the connection now to make the client receive an error
                                    out.close();
                                    break;
                                }
                            }
                            if (line.isEmpty()) {
                                // end of headers, send a small body
                                out.print("HTTP/1.1 200 OK\r\nContent-Length: 4\r\n\r\nbody");
                                out.flush();
                                break;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                if (e.getMessage().equals("Socket closed")) {
                    return;
                }
                throw new RuntimeException(e);
            }
        }

        protected void end() {
            try {
                serverSocket.close(); // will make accept() throw SocketException "Socket closed"
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        protected BufferedReader getReader(Socket socket) throws IOException {
            return new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
        }

        protected PrintWriter getWriter(Socket socket) throws IOException {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8));
            return new PrintWriter(bw, true);
        }
    }

    @Before
    public void initClient() {
        dummyServer = new DummyHttpServer(PORT);
        thread = new Thread(dummyServer);
        thread.start();

        Client client = Client.create();
        client.setConnectTimeout(CLIENT_TIMEOUT);
        client.setReadTimeout(CLIENT_TIMEOUT);
        webResource = client.resource("http://localhost:18090").path("testRetryPost");
    }

    @After
    public void after() throws Exception {
        dummyServer.end();
        thread.join(1000);

        // after the test, switch back to the default in Java 8
        enableSunHttpClientRetryPostProp();
    }

    @Test
    public void testRetryPostDisabled() throws Exception {
        disableSunHttpClientRetryPostProp();
        dummyServer.firstRun = true;

        // POST retry is disabled by default by the ServletContainerFeature, the call should fail
        try {
            webResource.post(ClientResponse.class).close();
            fail("A SocketException should have been thrown since retryPostProp is disabled by the ServletContainerFeature");
        } catch (ClientHandlerException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof SocketException);
            assertEquals("Unexpected end of file from server", cause.getMessage());
        }
    }

    @Test
    public void testRetryPostEnabled() throws Exception {
        enableSunHttpClientRetryPostProp();
        dummyServer.firstRun = true;

        // POST retry is enabled, the call should succeed
        ClientResponse response = null;
        try {
            response = webResource.post(ClientResponse.class);
            assertEquals(200, response.getStatus());
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    /**
     * This is the method that we're actually testing.
     */
    protected void disableSunHttpClientRetryPostProp() throws ReflectiveOperationException {
        ServletContainerFeature.disableSunHttpClientRetryPostProp();
    }

    protected void enableSunHttpClientRetryPostProp() throws ReflectiveOperationException {
        Field field = HttpClient.class.getDeclaredField("retryPostProp");
        field.setAccessible(true);
        field.setBoolean(null, true);
    }

}
