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

import java.io.IOException;
import java.net.URI;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.URIUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
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
@Features(TokenAuthenticationJettyFeature.class)
public class TestTokenAuthenticationServlet {

    protected void nextTransaction() {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }

    @Test
    public void testServlet() throws Exception {

        HttpClient httpClient = new HttpClient();

        HttpMethod getMethod = null;
        try {
            // ------------ Test bad authentication ----------------
            getMethod = new GetMethod(
                    "http://localhost:18080/authentication/token?applicationName=myFavoriteApp&deviceId=dead-beaf-cafe-babe&permission=rw");
            int status = executeGetMethod(httpClient, getMethod, "Administrator", "badPassword");
            // Receives 404 because of redirection to error page
            assertEquals(404, status);

            // ------------ Test omitting required parameters ----------------
            // Token acquisition
            getMethod = new GetMethod("http://localhost:18080/authentication/token?applicationName=myFavoriteApp");
            status = executeGetMethod(httpClient, getMethod, "Administrator", "Administrator");
            assertEquals(400, status);

            // Token revocation
            getMethod = new GetMethod(
                    "http://localhost:18080/authentication/token?applicationName=myFavoriteApp&revoke=true");
            status = executeGetMethod(httpClient, getMethod, "Administrator", "Administrator");
            assertEquals(400, status);

            // ------------ Test acquiring token ----------------
            String queryParams = URIUtil.encodeQuery("applicationName=Nuxeo Drive&deviceId=dead-beaf-cafe-babe&permission=rw");
            URI uri = new URI("http", null, "localhost", 18080, "/authentication/token", queryParams, null);
            getMethod = new GetMethod(uri.toString());
            // Acquire new token
            status = executeGetMethod(httpClient, getMethod, "Administrator", "Administrator");
            assertEquals(201, status);
            String token = getMethod.getResponseBodyAsString();
            assertNotNull(token);
            assertNotNull(getTokenAuthenticationService().getUserName(token));
            assertEquals(1, getTokenAuthenticationService().getTokenBindings("Administrator").size());

            // Acquire existing token
            status = httpClient.executeMethod(getMethod);
            assertEquals(201, status);
            String existingToken = getMethod.getResponseBodyAsString();
            assertEquals(token, existingToken);

            // ------------ Test revoking token ----------------
            // Non existing token, should do nothing
            getMethod = new GetMethod(
                    "http://localhost:18080/authentication/token?applicationName=nonExistingApp&deviceId=dead-beaf-cafe-babe&revoke=true");
            status = executeGetMethod(httpClient, getMethod, "Administrator", "Administrator");
            assertEquals(400, status);
            String response = getMethod.getResponseBodyAsString();
            assertEquals(String.format(
                    "No token found for userName %s, applicationName %s and deviceId %s; nothing to do.",
                    "Administrator", "nonExistingApp", "dead-beaf-cafe-babe"), response);

            // Existing token
            queryParams = URIUtil.encodeQuery("applicationName=Nuxeo Drive&deviceId=dead-beaf-cafe-babe&revoke=true");
            uri = new URI("http", null, "localhost", 18080, "/authentication/token", queryParams, null);
            getMethod = new GetMethod(uri.toString());
            status = executeGetMethod(httpClient, getMethod, "Administrator", "Administrator");
            assertEquals(202, status);
            response = getMethod.getResponseBodyAsString();
            assertEquals(String.format("Token revoked for userName %s, applicationName %s and deviceId %s.",
                    "Administrator", "Nuxeo Drive", "dead-beaf-cafe-babe"), response);
            nextTransaction(); // see committed changes
            assertNull(getTokenAuthenticationService().getUserName(token));
            assertTrue(getTokenAuthenticationService().getTokenBindings("Administrator").isEmpty());
        } finally {
            getMethod.releaseConnection();
        }
    }

    /**
     * Executes the specified HTTP method on the specified HTTP client with a basic authentication header given the
     * specified credentials.
     */
    protected final int executeGetMethod(HttpClient httpClient, HttpMethod httpMethod, String userName, String password)
            throws HttpException, IOException {

        String authString = userName + ":" + password;
        String basicAuthHeader = "Basic " + new String(Base64.encodeBase64(authString.getBytes()));
        httpMethod.setRequestHeader("Authorization", basicAuthHeader);
        return httpClient.executeMethod(httpMethod);
    }

    protected TokenAuthenticationService getTokenAuthenticationService() {
        return Framework.getLocalService(TokenAuthenticationService.class);
    }
}
