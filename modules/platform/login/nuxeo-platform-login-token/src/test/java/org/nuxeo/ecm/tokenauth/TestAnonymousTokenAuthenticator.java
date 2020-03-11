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

import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;

import java.util.Map;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.EmbeddedAutomationServerFeature;
import org.nuxeo.ecm.automation.test.HttpAutomationClient;
import org.nuxeo.ecm.automation.test.HttpAutomationSession;
import org.nuxeo.ecm.platform.ui.web.auth.token.TokenAuthenticator;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

/**
 * Tests the {@link TokenAuthenticator} in the case of an anonymous user.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ TokenAuthenticationServiceFeature.class, EmbeddedAutomationServerFeature.class })
@Deploy("org.nuxeo.ecm.platform.login.token.test:OSGI-INF/test-token-authentication-anonymous-contrib.xml")
public class TestAnonymousTokenAuthenticator {

    protected static final String TOKEN_HEADER = "X-Authentication-Token";

    @Inject
    protected HotDeployer deployer;

    @Inject
    protected TransactionalFeature transactionalFeature;

    @Inject
    protected TokenAuthenticationService tokenAuthenticationService;

    @Inject
    protected HttpAutomationClient automationClient;

    @Test
    public void testAuthenticatorAsAnonymous() throws Exception {

        // token for anonymous
        String token = tokenAuthenticationService.acquireToken("Guest", "myApp", "myDevice", "My Device", "rw");
        transactionalFeature.nextTransaction();

        // Check automation call with anonymous user not allowed
        HttpAutomationSession session = automationClient.getSession();
        session.login(Map.of(TOKEN_HEADER, token), SC_UNAUTHORIZED);

        // Check automation call with anonymous user allowed
        deployer.deploy(
                "org.nuxeo.ecm.platform.login.token.test:OSGI-INF/test-token-authentication-allow-anonymous-token-contrib.xml");

        assertEquals("Guest", session.login(Map.of(TOKEN_HEADER, token)));

    }

}
