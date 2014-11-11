/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth;

import org.nuxeo.ecm.platform.ui.web.auth.service.AuthenticationPluginDescriptor;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestAuthPlugins extends NXRuntimeTestCase {

    private PluggableAuthenticationService authService;

    private static final String WEB_BUNDLE = "org.nuxeo.ecm.platform.web.common";

    private static final String WEB_BUNDLE_TEST = "org.nuxeo.ecm.platform.web.common.test";

    @Override
    public void setUp() throws Exception {
        super.setUp();

        deployContrib(WEB_BUNDLE, "OSGI-INF/authentication-framework.xml");
        deployContrib(WEB_BUNDLE, "OSGI-INF/authentication-contrib.xml");
    }

    private PluggableAuthenticationService getAuthService() {
        if (authService == null) {
            authService = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                    PluggableAuthenticationService.NAME);
        }
        return authService;
    }

    public void testRegister() {
        getAuthService();
        assertNotNull(authService);
        // Rux NXP-1972: webservices plugin also
        assertEquals(4, authService.getAuthChain().size());
        assertEquals("BASIC_AUTH", authService.getAuthChain().get(0));
    }

    public void testServiceParameters() {
        getAuthService();
        AuthenticationPluginDescriptor plugin = authService.getDescriptor("FORM_AUTH");
        assertTrue(!plugin.getParameters().isEmpty());
        assertTrue(plugin.getParameters().containsKey("LoginPage"));
        assertEquals("login.jsp", plugin.getParameters().get("LoginPage"));
    }

    public void testDescriptorMerge() throws Exception {
        deployBundle(WEB_BUNDLE_TEST);
        PluggableAuthenticationService service = getAuthService();
        AuthenticationPluginDescriptor plugin = service.getDescriptor("ANONYMOUS_AUTH");

        assertFalse(plugin.getStateful());
        assertTrue(plugin.getNeedStartingURLSaving());
        assertEquals("Dummy_LM", plugin.getLoginModulePlugin());
        assertEquals(
                Class.forName("org.nuxeo.ecm.platform.ui.web.auth.DummyAuthenticator"),
                plugin.getClassName());
    }

}
