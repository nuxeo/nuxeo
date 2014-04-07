/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

/**
 * Tests the {@link TokenAuthenticationServlet}.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.7
 */
@RunWith(FeaturesRunner.class)
@Features(TokenAuthenticationJettyFeature.class)
public class TestTokenAuthenticationServlet {

    @Test
    public void testServlet() throws Exception {

        HttpClient httpClient = new HttpClient();

        HttpMethod getMethod = null;
        try {
            // ------------ Test bad authentication ----------------
            getMethod = new GetMethod(
                    "http://localhost:18080/authentication/token?applicationName=myFavoriteApp&deviceId=dead-beaf-cafe-babe&permission=rw");
            int status = executeGetMethod(httpClient, getMethod,
                    "Administrator", "badPassword");
            // Receives 404 because of redirection to error page
            assertEquals(404, status);

            // ------------ Test omitting required parameters ----------------
            // Token acquisition
            getMethod = new GetMethod(
                    "http://localhost:18080/authentication/token?applicationName=myFavoriteApp");
            status = executeGetMethod(httpClient, getMethod, "Administrator",
                    "Administrator");
            assertEquals(400, status);

            // Token revocation
            getMethod = new GetMethod(
                    "http://localhost:18080/authentication/token?applicationName=myFavoriteApp&revoke=true");
            status = executeGetMethod(httpClient, getMethod, "Administrator",
                    "Administrator");
            assertEquals(400, status);

            // ------------ Test acquiring token ----------------
            String queryParams = URIUtil.encodeQuery("applicationName=Nuxeo Drive&deviceId=dead-beaf-cafe-babe&permission=rw");
            URI uri = new URI("http", null, "localhost", 18080,
                    "/authentication/token", queryParams, null);
            getMethod = new GetMethod(uri.toString());
            // Acquire new token
            status = executeGetMethod(httpClient, getMethod, "Administrator",
                    "Administrator");
            assertEquals(201, status);
            String token = getMethod.getResponseBodyAsString();
            assertNotNull(token);
            assertNotNull(getTokenAuthenticationService().getUserName(token));
            assertEquals(
                    1,
                    getTokenAuthenticationService().getTokenBindings(
                            "Administrator").size());

            // Acquire existing token
            status = httpClient.executeMethod(getMethod);
            assertEquals(201, status);
            String existingToken = getMethod.getResponseBodyAsString();
            assertEquals(token, existingToken);

            // ------------ Test revoking token ----------------
            // Non existing token, should do nothing
            getMethod = new GetMethod(
                    "http://localhost:18080/authentication/token?applicationName=nonExistingApp&deviceId=dead-beaf-cafe-babe&revoke=true");
            status = executeGetMethod(httpClient, getMethod, "Administrator",
                    "Administrator");
            assertEquals(400, status);
            String response = getMethod.getResponseBodyAsString();
            assertEquals(
                    String.format(
                            "No token found for userName %s, applicationName %s and deviceId %s; nothing to do.",
                            "Administrator", "nonExistingApp",
                            "dead-beaf-cafe-babe"), response);

            // Existing token
            queryParams = URIUtil.encodeQuery("applicationName=Nuxeo Drive&deviceId=dead-beaf-cafe-babe&revoke=true");
            uri = new URI("http", null, "localhost", 18080,
                    "/authentication/token", queryParams, null);
            getMethod = new GetMethod(uri.toString());
            status = executeGetMethod(httpClient, getMethod, "Administrator",
                    "Administrator");
            assertEquals(202, status);
            response = getMethod.getResponseBodyAsString();
            assertEquals(
                    String.format(
                            "Token revoked for userName %s, applicationName %s and deviceId %s.",
                            "Administrator", "Nuxeo Drive",
                            "dead-beaf-cafe-babe"), response);
            assertNull(getTokenAuthenticationService().getUserName(token));
            assertTrue(getTokenAuthenticationService().getTokenBindings(
                    "Administrator").isEmpty());
        } finally {
            getMethod.releaseConnection();
        }
    }

    /**
     * Executes the specified HTTP method on the specified HTTP client with a
     * basic authentication header given the specified credentials.
     */
    protected final int executeGetMethod(HttpClient httpClient,
            HttpMethod httpMethod, String userName, String password)
            throws HttpException, IOException {

        String authString = userName + ":" + password;
        String basicAuthHeader = "Basic "
                + new String(Base64.encodeBase64(authString.getBytes()));
        httpMethod.setRequestHeader("Authorization", basicAuthHeader);
        return httpClient.executeMethod(httpMethod);
    }

    protected TokenAuthenticationService getTokenAuthenticationService() {
        return Framework.getLocalService(TokenAuthenticationService.class);
    }
}
