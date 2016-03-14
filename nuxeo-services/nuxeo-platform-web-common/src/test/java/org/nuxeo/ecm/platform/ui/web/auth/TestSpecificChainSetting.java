/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.ui.web.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestSpecificChainSetting extends NXRuntimeTestCase {

    private static final String WEB_BUNDLE = "org.nuxeo.ecm.platform.web.common";

    private static final String WEB_BUNDLE_TEST = "org.nuxeo.ecm.platform.web.common.test";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployContrib(WEB_BUNDLE, "OSGI-INF/authentication-framework.xml");
        deployContrib(WEB_BUNDLE, "OSGI-INF/authentication-contrib.xml");
        deployContrib(WEB_BUNDLE_TEST, "OSGI-INF/test-specific-chain.xml");
    }

    private PluggableAuthenticationService getAuthService() {
        PluggableAuthenticationService authService;
        authService = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                PluggableAuthenticationService.NAME);

        return authService;
    }

    @Test
    public void testStdChain() {
        PluggableAuthenticationService authService = getAuthService();
        assertNotNull(authService);

        List<String> chain = authService.getAuthChain();
        assertTrue(chain.contains("FORM_AUTH"));
        assertTrue(chain.contains("BASIC_AUTH"));
        assertTrue(chain.contains("ANONYMOUS_AUTH"));
        assertTrue(chain.contains("WEBSERVICES_AUTH"));

        HttpServletRequest request = new DummyHttpServletRequest("/toto", null);
        chain = authService.getAuthChain(request);
        assertTrue(chain.contains("FORM_AUTH"));
        assertTrue(chain.contains("BASIC_AUTH"));
        assertTrue(chain.contains("ANONYMOUS_AUTH"));
        assertTrue(chain.contains("WEBSERVICES_AUTH"));
    }

    @Test
    public void testSpecificChain() {
        List<String> chain;

        PluggableAuthenticationService authService = getAuthService();
        assertNotNull(authService);

        HttpServletRequest request = new DummyHttpServletRequest("/toto", null);
        String chainName = authService.getSpecificAuthChainName(request);
        assertNull(chainName);

        // test
        request = new DummyHttpServletRequest("/test/toto", null);
        chainName = authService.getSpecificAuthChainName(request);
        assertNotNull(chainName);
        assertEquals("test", chainName);

        chain = authService.getAuthChain(request);
        assertTrue(chain.contains("FORM_AUTH"));
        assertFalse(chain.contains("BASIC_AUTH"));
        assertTrue(chain.contains("ANONYMOUS_AUTH"));
        assertFalse(chain.contains("WEBSERVICES_AUTH"));

        // test-allow
        request = new DummyHttpServletRequest("/testallow/toto", null);
        chainName = authService.getSpecificAuthChainName(request);
        assertNotNull(chainName);
        assertEquals("test-allow", chainName);

        chain = authService.getAuthChain(request);
        assertTrue(chain.contains("FORM_AUTH"));
        assertFalse(chain.contains("BASIC_AUTH"));
        assertFalse(chain.contains("ANONYMOUS_AUTH"));
        assertFalse(chain.contains("WEBSERVICES_AUTH"));

        // test-headers
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("test-header", "only-anonymous");
        request = new DummyHttpServletRequest("/toto", headers);
        chainName = authService.getSpecificAuthChainName(request);
        assertNotNull(chainName);
        assertEquals("test-headers", chainName);

        chain = authService.getAuthChain(request);
        assertFalse(chain.contains("FORM_AUTH"));
        assertFalse(chain.contains("BASIC_AUTH"));
        assertTrue(chain.contains("ANONYMOUS_AUTH"));
        assertFalse(chain.contains("WEBSERVICES_AUTH"));

        // test-headers2
        headers = new HashMap<String, String>();
        headers.put("test-header", "only-ba");
        request = new DummyHttpServletRequest("/toto", headers);
        chainName = authService.getSpecificAuthChainName(request);
        assertNotNull(chainName);
        assertEquals("test-headers2", chainName);

        chain = authService.getAuthChain(request);
        assertFalse(chain.contains("FORM_AUTH"));
        assertTrue(chain.contains("BASIC_AUTH"));
        assertFalse(chain.contains("ANONYMOUS_AUTH"));
        assertFalse(chain.contains("WEBSERVICES_AUTH"));

        // WSS url
        headers = new HashMap<String, String>();
        request = new DummyHttpServletRequest("/_vti_bin/owssvr.dll", null);
        chainName = authService.getSpecificAuthChainName(request);
        assertNotNull(chainName);
        assertEquals("WSS", chainName);

        chain = authService.getAuthChain(request);
        assertFalse(chain.contains("FORM_AUTH"));
        assertTrue(chain.contains("BASIC_AUTH"));
        assertFalse(chain.contains("ANONYMOUS_AUTH"));
        assertFalse(chain.contains("WEBSERVICES_AUTH"));

        // WSS header
        headers = new HashMap<String, String>();
        headers.put("User-Agent", "MSFrontPage/12.0");
        request = new DummyHttpServletRequest("/", headers);
        chainName = authService.getSpecificAuthChainName(request);
        assertNotNull(chainName);
        assertEquals("WSS", chainName);

        chain = authService.getAuthChain(request);
        assertFalse(chain.contains("FORM_AUTH"));
        assertTrue(chain.contains("BASIC_AUTH"));
        assertFalse(chain.contains("ANONYMOUS_AUTH"));
        assertFalse(chain.contains("WEBSERVICES_AUTH"));
    }

    @Test
    public void specificChainMayHandleLoginPromptBehavior() throws Exception {

        // Without a specific auth chain
        HttpServletRequest request = new DummyHttpServletRequest("/bla", null);
        assertTrue(getAuthService().doHandlePrompt(request));

        // With a non configured specific auth chain
        request = new DummyHttpServletRequest("/test", null);
        assertTrue(getAuthService().doHandlePrompt(request));

        // With a configured specific auth chain to handle prompt
        request = new DummyHttpServletRequest("/test-allow", null);
        assertTrue(getAuthService().doHandlePrompt(request));

        // With a configured specific auth chain to not handle prompt
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("test-header", "only-anonymous");
        request = new DummyHttpServletRequest("/bla", headers);
        assertFalse(getAuthService().doHandlePrompt(request));

    }
}
