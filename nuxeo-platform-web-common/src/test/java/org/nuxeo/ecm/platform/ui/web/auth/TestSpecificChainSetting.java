/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.ui.web.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestSpecificChainSetting extends NXRuntimeTestCase {


    private static final String WEB_BUNDLE = "org.nuxeo.ecm.platform.web.common";

    private static final String WEB_BUNDLE_TEST = "org.nuxeo.ecm.platform.web.common.test";

    @Override
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

    public void testSpecificChain() {
        List<String> chain = null;

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

}
