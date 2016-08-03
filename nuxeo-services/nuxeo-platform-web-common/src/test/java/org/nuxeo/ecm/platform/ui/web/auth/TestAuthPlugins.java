/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthPreFilter;
import org.nuxeo.ecm.platform.ui.web.auth.service.AuthenticationPluginDescriptor;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestAuthPlugins extends NXRuntimeTestCase {

    private static final String WEB_BUNDLE = "org.nuxeo.ecm.platform.web.common";

    private static final String WEB_BUNDLE_TEST = "org.nuxeo.ecm.platform.web.common.test";

    private PluggableAuthenticationService authService;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        deployContrib(WEB_BUNDLE, "OSGI-INF/authentication-framework.xml");
        deployContrib(WEB_BUNDLE, "OSGI-INF/authentication-contrib.xml");
    }

    private PluggableAuthenticationService getAuthService() {
        if (authService == null) {
            authService = (PluggableAuthenticationService) Framework.getRuntime()
                                                                    .getComponent(PluggableAuthenticationService.NAME);
        }
        return authService;
    }

    @Test
    public void testRegister() {
        getAuthService();
        assertNotNull(authService);
        // Rux NXP-1972: webservices plugin also
        assertEquals(5, authService.getAuthChain().size());
        assertEquals("BASIC_AUTH", authService.getAuthChain().get(0));
    }

    @Test
    public void testServiceParameters() {
        getAuthService();
        AuthenticationPluginDescriptor plugin = authService.getDescriptor("FORM_AUTH");
        assertTrue(!plugin.getParameters().isEmpty());
        assertTrue(plugin.getParameters().containsKey("LoginPage"));
        assertEquals("login.jsp", plugin.getParameters().get("LoginPage"));
    }

    @Test
    public void testDescriptorMerge() throws Exception {
        deployBundle(WEB_BUNDLE_TEST);
        PluggableAuthenticationService service = getAuthService();
        AuthenticationPluginDescriptor plugin = service.getDescriptor("ANONYMOUS_AUTH");

        assertFalse(plugin.getStateful());
        assertTrue(plugin.getNeedStartingURLSaving());
        assertEquals("Dummy_LM", plugin.getLoginModulePlugin());
        assertSame(Class.forName("org.nuxeo.ecm.platform.ui.web.auth.DummyAuthenticator"), plugin.getClassName());
    }

    @Test
    public void preFilterCanBeDisabled() throws Exception {
        deployContrib(WEB_BUNDLE_TEST, "OSGI-INF/test-prefilter.xml");
        deployContrib(WEB_BUNDLE_TEST, "OSGI-INF/test-prefilter-disable.xml");
        getAuthService().initPreFilters();
        List<NuxeoAuthPreFilter> filters = getAuthService().getPreFilters();

        assertEquals(2, filters.size());

    }

}
