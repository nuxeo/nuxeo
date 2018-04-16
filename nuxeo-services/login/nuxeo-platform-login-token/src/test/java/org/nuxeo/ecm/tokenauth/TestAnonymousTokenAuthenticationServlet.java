/*
 * (C) Copyright 2014-2017 Nuxeo (http://nuxeo.com/) and others.
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
import static org.nuxeo.ecm.tokenauth.servlet.TokenAuthenticationServlet.APPLICATION_NAME_PARAM;
import static org.nuxeo.ecm.tokenauth.servlet.TokenAuthenticationServlet.DEVICE_ID_PARAM;
import static org.nuxeo.ecm.tokenauth.servlet.TokenAuthenticationServlet.PERMISSION_PARAM;

import java.net.URI;

import javax.inject.Inject;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.ecm.tokenauth.servlet.TokenAuthenticationServlet;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;

/**
 * Tests the {@link TokenAuthenticationServlet} in the case of an anonymous user.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features(TokenAuthenticationServletContainerFeature.class)
@Deploy("org.nuxeo.ecm.platform.login.token.test:OSGI-INF/test-token-authentication-anonymous-contrib.xml")
public class TestAnonymousTokenAuthenticationServlet {

    @Inject
    protected TokenAuthenticationService tokenAuthenticationService;

    @Inject
    protected HotDeployer deployer;

    @Test
    public void testServletAsAnonymous() throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // ------------ Test anonymous user not allowed ----------------
            String baseURL = "http://localhost:18080/authentication/token";
            URI uri = new URIBuilder(baseURL).addParameter(APPLICATION_NAME_PARAM, "myFavoriteApp") //
                                             .addParameter(DEVICE_ID_PARAM, "dead-beaf-cafe-babe")
                                             .addParameter(PERMISSION_PARAM, "rw")
                                             .build();
            HttpGet get = new HttpGet(uri);
            try (CloseableHttpResponse response = httpClient.execute(get)) {
                assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getStatusLine().getStatusCode());
            }

            // ------------ Test anonymous user allowed ----------------
            deployer.deploy(
                    "org.nuxeo.ecm.platform.login.token.test:OSGI-INF/test-token-authentication-allow-anonymous-token-contrib.xml");

            String token;
            try (CloseableHttpResponse response = httpClient.execute(get)) {
                assertEquals(HttpStatus.SC_CREATED, response.getStatusLine().getStatusCode());
                token = EntityUtils.toString(response.getEntity());
            }
            assertNotNull(token);
            assertNotNull(tokenAuthenticationService.getUserName(token));
            assertEquals(1, tokenAuthenticationService.getTokenBindings("Guest").size());
        }
    }

}
