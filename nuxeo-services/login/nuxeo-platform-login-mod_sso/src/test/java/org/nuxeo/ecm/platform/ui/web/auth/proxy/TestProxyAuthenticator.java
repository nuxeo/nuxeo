/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *     Academie de Rennes - proxy CAS support
 *     Revolution Linux - Username string cleanup
 *
 */

package org.nuxeo.ecm.platform.ui.web.auth.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author Patrick Turcotte
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.login.mod_sso.test")
@Deploy("org.nuxeo.ecm.platform.login.mod_sso.test:OSGI-INF/mod_sso-descriptor-bundle.xml")
public class TestProxyAuthenticator {

    @Test
    public void testProxyAuthenticationWithoutReplacement() throws Exception {

        ProxyAuthenticator proxyAuth = new ProxyAuthenticator();

        Map<String, String> parameters = new HashMap<>();
        parameters.put("ssoHeaderName", "remote_user");
        // Redirect requires prefilter configuration, skipping...
        parameters.put("ssoNeverRedirect", "true");
        proxyAuth.initPlugin(parameters);

        String username = "test";

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(eq("remote_user"))).thenReturn(username);

        UserIdentificationInfo identity = proxyAuth.handleRetrieveIdentity(request, response);

        assertNotNull(identity);
        assertEquals(username, identity.getUserName());
    }

    @Test
    public void testProxyAuthenticationWithReplacement() throws Exception {

        ProxyAuthenticator proxyAuth = new ProxyAuthenticator();

        Map<String, String> parameters = new HashMap<>();
        parameters.put("ssoHeaderName", "remote_user");
        // Redirect requires prefilter configuration, skipping...
        parameters.put("ssoNeverRedirect", "true");
        String regexp = "@EXAMPLE.COM";
        parameters.put(ProxyAuthenticator.USERNAME_REMOVE_EXPRESSION, regexp);
        proxyAuth.initPlugin(parameters);

        String username = "test";
        String usernameAndUnwantedPart = username + "@EXAMPLE.COM";

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(eq("remote_user"))).thenReturn(usernameAndUnwantedPart);

        UserIdentificationInfo identity = proxyAuth.handleRetrieveIdentity(request, response);

        assertNotNull(identity);
        assertEquals(username, identity.getUserName());
    }

}
