/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
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

import javax.inject.Inject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.ecm.tokenauth.servlet.TokenAuthenticationServlet;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeHarness;

/**
 * Tests the {@link TokenAuthenticationServlet} in the case of an anonymous user.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features(TokenAuthenticationJettyFeature.class)
@Deploy("org.nuxeo.ecm.platform.login.token.test:OSGI-INF/test-token-authentication-anonymous-contrib.xml")
public class TestAnonymousTokenAuthenticationServlet {

    @Inject
    protected RuntimeHarness harness;

    @Inject
    protected TokenAuthenticationService tokenAuthenticationService;

    @Test
    public void testServletAsAnonymous() throws Exception {

        HttpClient httpClient = new HttpClient();

        HttpMethod getMethod = null;
        try {
            // ------------ Test anonymous user not allowed ----------------
            getMethod = new GetMethod(
                    "http://localhost:18080/authentication/token?applicationName=myFavoriteApp&deviceId=dead-beaf-cafe-babe&permission=rw");
            int status = httpClient.executeMethod(getMethod);
            assertEquals(401, status);

            // ------------ Test anonymous user allowed ----------------
            harness.deployContrib("org.nuxeo.ecm.platform.login.token.test",
                    "OSGI-INF/test-token-authentication-allow-anonymous-token-contrib.xml");

            status = httpClient.executeMethod(getMethod);
            assertEquals(201, status);
            String token = getMethod.getResponseBodyAsString();
            assertNotNull(token);
            assertNotNull(tokenAuthenticationService.getUserName(token));
            assertEquals(1, tokenAuthenticationService.getTokenBindings("Guest").size());

            harness.undeployContrib("org.nuxeo.ecm.platform.login.token.test",
                    "OSGI-INF/test-token-authentication-allow-anonymous-token-contrib.xml");
        } finally {
            getMethod.releaseConnection();
        }
    }

}
