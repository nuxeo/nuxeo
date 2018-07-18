/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthPreFilter;
import org.nuxeo.ecm.platform.ui.web.auth.service.AuthenticationPluginDescriptor;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.HotDeployer;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy("org.nuxeo.ecm.platform.web.common:OSGI-INF/authentication-framework.xml")
@Deploy("org.nuxeo.ecm.platform.web.common:OSGI-INF/authentication-contrib.xml")
public class TestAuthPlugins {

    @Inject
    protected HotDeployer hotDeployer;

    private PluggableAuthenticationService getAuthService() {
        Object object = Framework.getRuntime().getComponent(PluggableAuthenticationService.NAME);
        return (PluggableAuthenticationService) object;
    }

    @Test
    public void testRegister() {
        PluggableAuthenticationService authService = getAuthService();
        assertNotNull(authService);
        // Rux NXP-1972: webservices plugin also
        assertEquals(7, authService.getAuthChain().size());
        assertEquals("BASIC_AUTH", authService.getAuthChain().get(0));
    }

    @Test
    public void testServiceParameters() {
        PluggableAuthenticationService authService = getAuthService();
        AuthenticationPluginDescriptor plugin = authService.getDescriptor("FORM_AUTH");
        assertTrue(!plugin.getParameters().isEmpty());
        assertTrue(plugin.getParameters().containsKey("LoginPage"));
        assertEquals("login.jsp", plugin.getParameters().get("LoginPage"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test")
    public void testDescriptorMerge() throws Exception {

        PluggableAuthenticationService service = getAuthService();
        AuthenticationPluginDescriptor plugin = service.getDescriptor("ANONYMOUS_AUTH");

        assertFalse(plugin.getStateful());
        assertTrue(plugin.getNeedStartingURLSaving());
        assertEquals("Dummy_LM", plugin.getLoginModulePlugin());
        assertSame(Class.forName("org.nuxeo.ecm.platform.ui.web.auth.DummyAuthenticator"), plugin.getClassName());
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-prefilter.xml")
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-prefilter-disable.xml")
    public void preFilterCanBeDisabled() throws Exception {
        PluggableAuthenticationService authService = getAuthService();
        authService.initPreFilters();
        List<NuxeoAuthPreFilter> filters = authService.getPreFilters();
        assertEquals(2, filters.size());
    }

}
