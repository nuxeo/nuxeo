/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.tokenauth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.tokenauth.servlet.TokenAuthenticationServlet.APPLICATION_NAME_PARAM;
import static org.nuxeo.ecm.tokenauth.servlet.TokenAuthenticationServlet.DEVICE_ID_PARAM;
import static org.nuxeo.ecm.tokenauth.servlet.TokenAuthenticationServlet.PERMISSION_PARAM;
import static org.nuxeo.ecm.tokenauth.servlet.TokenAuthenticationServlet.REVOKE_PARAM;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.ecm.tokenauth.servlet.TokenAuthenticationServlet;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Tests the {@link TokenAuthenticationServlet}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(TokenAuthenticationServletContainerFeature.class)
public class TestTokenAuthenticationServlet {

    protected void nextTransaction() {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    @Test
    public void testServlet() throws Exception {
        String baseURL = "http://localhost:18080/authentication/token";
        String applicationName = "Nuxeo Drive Café"; // name with space and non-ascii char
        String deviceId = "dead-beaf-cafe-babe";
        String userName = "Administrator";
        String password = "Administrator";
        URI uri;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // ------------ Test bad authentication ----------------
            uri = new URIBuilder(baseURL).addParameter(APPLICATION_NAME_PARAM, applicationName) //
                                         .addParameter(DEVICE_ID_PARAM, deviceId)
                                         .addParameter(PERMISSION_PARAM, "rw")
                                         .build();
            try (CloseableHttpResponse response = executeGetMethod(httpClient, uri, userName, "badPassword")) {
                // Receives 401 because of because of unauthorized user
                assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
            }

            // ------------ Test omitting required parameters ----------------
            // Token acquisition
            uri = new URIBuilder(baseURL).addParameter(APPLICATION_NAME_PARAM, applicationName) //
                                         .build();
            try (CloseableHttpResponse response = executeGetMethod(httpClient, uri, userName, password)) {
                assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
            }

            // Token revocation
            uri = new URIBuilder(baseURL).addParameter(APPLICATION_NAME_PARAM, applicationName) //
                                         .addParameter(REVOKE_PARAM, "true")
                                         .build();
            try (CloseableHttpResponse response = executeGetMethod(httpClient, uri, userName, password)) {
                assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
            }

            // ------------ Test acquiring token ----------------
            uri = new URIBuilder(baseURL).addParameter(APPLICATION_NAME_PARAM, applicationName) //
                                         .addParameter(DEVICE_ID_PARAM, deviceId)
                                         .addParameter(PERMISSION_PARAM, "rw")
                                         .build();
            // Acquire new token
            String token;
            try (CloseableHttpResponse response = executeGetMethod(httpClient, uri, userName, password)) {
                assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
                token = EntityUtils.toString(response.getEntity());
            }
            assertNotNull(token);
            assertNotNull(getTokenAuthenticationService().getUserName(token));
            assertEquals(1, getTokenAuthenticationService().getTokenBindings(userName).size());
            DocumentModel tokenDoc = getTokenAuthenticationService().getTokenBindings(userName).get(0);
            assertEquals(applicationName, tokenDoc.getPropertyValue("authtoken:applicationName"));

            // Acquire existing token
            String existingToken;
            try (CloseableHttpResponse response = httpClient.execute(new HttpGet(uri))) {
                assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
                existingToken = EntityUtils.toString(response.getEntity());
            }
            assertEquals(token, existingToken);

            // ------------ Test revoking token ----------------
            // Non existing token, should do nothing
            String nonExistingApp = "nonExistingApp";
            uri = new URIBuilder(baseURL).addParameter(APPLICATION_NAME_PARAM, nonExistingApp) //
                                         .addParameter(DEVICE_ID_PARAM, deviceId)
                                         .addParameter(REVOKE_PARAM, "true")
                                         .build();
            String content;
            try (CloseableHttpResponse response = executeGetMethod(httpClient, uri, userName, password)) {
                assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatusLine().getStatusCode());
                content = EntityUtils.toString(response.getEntity());
            }
            assertEquals(
                    String.format("No token found for userName %s, applicationName %s and deviceId %s; nothing to do.",
                            userName, nonExistingApp, deviceId),
                    content);

            // Existing token
            uri = new URIBuilder(baseURL).addParameter(APPLICATION_NAME_PARAM, applicationName) //
                                         .addParameter(DEVICE_ID_PARAM, deviceId)
                                         .addParameter(REVOKE_PARAM, "true")
                                         .build();
            try (CloseableHttpResponse response = executeGetMethod(httpClient, uri, userName, password)) {
                assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
                content = EntityUtils.toString(response.getEntity());
            }
            assertEquals(String.format("Token revoked for userName %s, applicationName %s and deviceId %s.", userName,
                    applicationName, deviceId), content);

            nextTransaction(); // see committed changes
            assertNull(getTokenAuthenticationService().getUserName(token));
            assertTrue(getTokenAuthenticationService().getTokenBindings(userName).isEmpty());
        }
    }

    /**
     * Executes a GET on the specified URI with the specified HTTP client with a basic authentication header given the
     * specified credentials.
     */
    protected final CloseableHttpResponse executeGetMethod(CloseableHttpClient httpClient, URI uri, String userName,
            String password) throws IOException {
        HttpGet get = new HttpGet(uri);
        String authString = userName + ":" + password;
        String basicAuthHeader = "Basic " + new String(Base64.encodeBase64(authString.getBytes()));
        get.setHeader("Authorization", basicAuthHeader);
        return httpClient.execute(get);
    }

    protected TokenAuthenticationService getTokenAuthenticationService() {
        return Framework.getService(TokenAuthenticationService.class);
    }
}
