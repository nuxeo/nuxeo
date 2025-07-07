/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <guillaume.renard@hyland.com>
 */
package org.nuxeo.duoweb.authentication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.nuxeo.duoweb.authentication.DuoFactorsAuthenticator.NX_USER_FIRST_FACTOR_CHECKED;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.PASSWORD_KEY;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.USERNAME_KEY;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.web.common.MockHttpServletRequest;
import org.nuxeo.ecm.platform.web.common.MockHttpServletResponse;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.duosecurity.Client;
import com.duosecurity.exception.DuoException;
import com.duosecurity.model.AuthResult;
import com.duosecurity.model.Token;

/**
 * @since 2025.5
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, MockitoFeature.class })
@Deploy("org.nuxeo.runtime.kv")
@Deploy("org.nuxeo.ecm.platform.login")
@Deploy("org.nuxeo.ecm.platform.web.common")
@Deploy("org.nuxeo.duoweb.authentication")
@Deploy("org.nuxeo.duoweb.authentication.test:OSGI-INF/duo-authentication-test-config.xml")
public class TestDuoFactorsAuthentication {

    protected final String DUO_USER = "DuoUser";

    protected final String BASE_URL = "http://localhost:8080/nuxeo/";

    @Mock
    @RuntimeService
    protected UserManager userManager;

    protected DuoFactorsAuthenticator duoAuthPlugin;

    protected Client duoClient;

    @Before
    public void setup() throws DuoException {
        NuxeoPrincipal casUserPrincipal = new UserPrincipal(DUO_USER, null, false, false);
        when(userManager.getPrincipal(DUO_USER)).thenReturn(casUserPrincipal);
        when(userManager.checkUsernamePassword(eq(DUO_USER), anyString())).thenReturn(true);

        PluggableAuthenticationService authService = Framework.getService(PluggableAuthenticationService.class);
        duoAuthPlugin = (DuoFactorsAuthenticator) authService.getPlugin("test_duo");
        duoAuthPlugin = spy(duoAuthPlugin);
        duoClient = spy(duoAuthPlugin.getClient());
        doReturn(duoClient).when(duoAuthPlugin).getClient();
    }

    @Test
    public void testDuoFactorSuccessful() throws IOException, DuoException {
        // 1st pass authentication
        // plugin handles auth
        var request = MockHttpServletRequest.init("GET", "http://localhost:8080/nuxeo/").mock();
        var handleLoginPrompt = duoAuthPlugin.handleLoginPrompt(request, MockHttpServletResponse.init().mock(),
                BASE_URL);
        assertTrue(handleLoginPrompt);
        // cannot completely retrieve entity yet
        request = MockHttpServletRequest.init("POST", "http://localhost:8080/nuxeo/")
                                        .whenGetParameterThenReturn(USERNAME_KEY, DUO_USER)
                                        .whenGetParameterThenReturn(PASSWORD_KEY, "yolo")
                                        .mock();
        var handleRetrieveIdentity = duoAuthPlugin.handleRetrieveIdentity(request,
                MockHttpServletResponse.init().mock());
        assertNull(handleRetrieveIdentity);
        // need to redirect to remote duo auth
        assertEquals(DUO_USER, request.getSession(false).getAttribute(NX_USER_FIRST_FACTOR_CHECKED));

        // 2nd pass authentication
        request = MockHttpServletRequest.init("GET", "http://localhost:8080/nuxeo/")
                                        .whenGetSessionAttributeThenReturn(NX_USER_FIRST_FACTOR_CHECKED, DUO_USER)
                                        .mock();
        var response = MockHttpServletResponse.init().mock();
        // plugin handles auth and ask 2nd factor by redirecting to duo
        handleLoginPrompt = duoAuthPlugin.handleLoginPrompt(request, response, BASE_URL);
        assertTrue(handleLoginPrompt);
        verify(response).sendRedirect(argThat(url -> {
            assertNotNull(url);
            try {
                URL redirectURl = URI.create(url).toURL();
                assertEquals("dummy-duo.nuxeo.com", redirectURl.getHost());
            } catch (MalformedURLException e) {
                fail("Malformed URL");
            }
            return true;
        }));
        var state = duoAuthPlugin.getKeyValueStore()
                                 .keyStream()
                                 .findFirst()
                                 .orElseThrow(() -> new AssertionError("State not found"));

        // callback to validate duo auth
        doReturn(getToken(true)).when(duoClient)
                                .exchangeAuthorizationCodeFor2FAResult(eq("dummy_duo_code"), eq(DUO_USER));
        request = MockHttpServletRequest.init("GET", "http://localhost:8080/nuxeo/")
                                        .whenGetParameterThenReturn("state", state)
                                        .whenGetParameterThenReturn("duo_code", "dummy_duo_code")
                                        .mock();
        handleRetrieveIdentity = duoAuthPlugin.handleRetrieveIdentity(request, MockHttpServletResponse.init().mock());
        assertNotNull(handleRetrieveIdentity);
        assertEquals(DUO_USER, handleRetrieveIdentity.getUserName());
    }

    @Test
    public void testDuoFactorFailureOnForgedCode() throws DuoException {
        var request = MockHttpServletRequest.init("GET", "http://localhost:8080/nuxeo/")
                                            .whenGetSessionAttributeThenReturn(NX_USER_FIRST_FACTOR_CHECKED, DUO_USER)
                                            .mock();
        var handleLoginPrompt = duoAuthPlugin.handleLoginPrompt(request, MockHttpServletResponse.init().mock(),
                BASE_URL);
        assertTrue(handleLoginPrompt);
        var state = duoAuthPlugin.getKeyValueStore()
                                 .keyStream()
                                 .findFirst()
                                 .orElseThrow(() -> new AssertionError("State not found"));

        doReturn(getToken(false)).when(duoClient)
                                 .exchangeAuthorizationCodeFor2FAResult(eq("forged_duo_code"), eq(DUO_USER));
        request = MockHttpServletRequest.init("GET", "http://localhost:8080/nuxeo/")
                                        .whenGetParameterThenReturn("state", state)
                                        .whenGetParameterThenReturn("duo_code", "forged_duo_code")
                                        .mock();
        var handleRetrieveIdentity = duoAuthPlugin.handleRetrieveIdentity(request,
                MockHttpServletResponse.init().mock());
        assertNull(handleRetrieveIdentity);
    }

    protected Token getToken(boolean valid) {
        var authResult = new AuthResult("Hi!", valid ? "ALLOW" : "DENY", null);
        return new Token(null, null, null, null, null, null, null, authResult, null);
    }

}
