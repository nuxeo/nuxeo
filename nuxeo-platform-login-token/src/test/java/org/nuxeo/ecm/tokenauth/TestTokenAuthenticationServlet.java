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

import java.net.URI;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.URIUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.tokenauth.servlet.TokenAuthenticationServlet;
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

        // Test omitting required parameters
        HttpMethod httpMethod = new GetMethod(
                "http://localhost:18080/authentication/token?userName=joe");
        try {
            int status = httpClient.executeMethod(httpMethod);
            assertEquals(404, status);
        } finally {
            httpMethod.releaseConnection();
        }

        // Test acquiring token
        String queryParams = URIUtil.encodeQuery("userName=joe&applicationName=myFavoriteApp&deviceName=Ubuntu box 64 bits&permission=rw");
        URI uri = new URI("http", null, "localhost", 18080,
                "/authentication/token", queryParams, null);
        httpMethod = new GetMethod(uri.toString());
        try {
            // Acquire new token
            int status = httpClient.executeMethod(httpMethod);
            assertEquals(200, status);
            String token = httpMethod.getResponseBodyAsString();
            assertNotNull(token);

            // Acquire existing token
            status = httpClient.executeMethod(httpMethod);
            assertEquals(200, status);
            String existingToken = httpMethod.getResponseBodyAsString();
            assertEquals(token, existingToken);
        } finally {
            httpMethod.releaseConnection();
        }
    }

}
