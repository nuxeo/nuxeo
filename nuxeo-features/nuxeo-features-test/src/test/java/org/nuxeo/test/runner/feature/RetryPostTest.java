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
 */
package org.nuxeo.test.runner.feature;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.net.SocketException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.JettyFeature;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import sun.net.www.http.HttpClient;

/**
 * Tests that the {@link JettyFeature} disables the {@code retryPostProp} property of
 * {@link sun.net.www.http.HttpClient}.
 *
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ WebEngineFeature.class, JettyFeature.class })
@Deploy("org.nuxeo.features.test.tests")
@Jetty(port = 18090)
public class RetryPostTest {

    private static final int CLIENT_TIMEOUT = 5 * 60 * 1000; // 5 minutes

    protected WebResource webResource;

    @Before
    public void initClient() {
        Client client = Client.create();
        client.setConnectTimeout(CLIENT_TIMEOUT);
        client.setReadTimeout(CLIENT_TIMEOUT);
        webResource = client.resource("http://localhost:18090").path("testRetryPost");
    }

    @Test
    public void testRetryPostDisabled() throws Exception {
        sendInitialGetRequest();

        // POST retry is disabled by default by the JettyFeature, the call should fail
        try {
            webResource.post(ClientResponse.class).close();
            fail("A SocketException should have been thrown since retryPostProp is disabled by the JettyFeature");
        } catch (ClientHandlerException e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof SocketException);
            assertEquals("Unexpected end of file from server", cause.getMessage());
        }
    }

    @Test
    public void testRetryPostEnabled() throws Exception {
        // Enable POST retry
        enableSunHttpClientRetryPostProp();

        sendInitialGetRequest();

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
     * Calls a GET request sending an invalid HTTP response, which doesn't prevent from reading the response but enables
     * the {@code failedOnce} flag in {@link HttpClient} for the next POST request.
     */
    protected void sendInitialGetRequest() {
        ClientResponse response = webResource.get(ClientResponse.class);
        assertEquals(200, response.getStatus());
        assertEquals(RetryPostTestObject.RETRY_POST_TEST_CONTENT, response.getEntity(String.class));
    }

    protected void enableSunHttpClientRetryPostProp()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        Field field = HttpClient.class.getDeclaredField("retryPostProp");
        field.setAccessible(true);
        field.setBoolean(null, true);
    }

}
