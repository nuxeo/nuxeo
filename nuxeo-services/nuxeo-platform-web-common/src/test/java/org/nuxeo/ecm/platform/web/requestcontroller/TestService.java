/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.web.requestcontroller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerManager;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerService;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestFilterConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.thetransactioncompany.cors.CORSConfiguration;
import com.thetransactioncompany.cors.CORSFilter;
import com.thetransactioncompany.cors.Origin;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.web.common:OSGI-INF/web-request-controller-framework.xml",
        "org.nuxeo.ecm.platform.web.common:OSGI-INF/web-request-controller-contrib-test.xml" })
public class TestService {

    @Inject
    protected RequestControllerManager requestControllerManager;

    @Test
    public void testServiceRegistration() {
        assertNotNull(requestControllerManager);
    }

    @Test
    public void testServiceContrib() throws Exception {
        RequestControllerService requestControllerService = (RequestControllerService) requestControllerManager;
        String uri;
        RequestFilterConfig config;

        uri = "/SyncNoTx/test";
        config = requestControllerService.computeConfigForRequest(uri);
        assertTrue(config.needSynchronization());
        assertFalse(config.needTransaction());

        uri = "/NoSyncTx/test";
        config = requestControllerService.computeConfigForRequest(uri);
        assertFalse(config.needSynchronization());
        assertTrue(config.needTransaction());

        uri = "/SyncTx/test";
        config = requestControllerService.computeConfigForRequest(uri);
        assertTrue(config.needSynchronization());
        assertTrue(config.needTransaction());

        uri = "/SyncTx/deny";
        config = requestControllerService.computeConfigForRequest(uri);
        assertFalse(config.needSynchronization());
        assertFalse(config.needTransaction());

        uri = "/whatever";
        config = requestControllerService.computeConfigForRequest(uri);
        assertFalse(config.needSynchronization());
        assertFalse(config.needTransaction());

        uri = "/nuxeo/TestServlet/";
        config = requestControllerService.computeConfigForRequest(uri);
        assertTrue(config.needSynchronization());
        assertTrue(config.needTransaction());

        uri = "/nuxeo/TestServlet";
        config = requestControllerService.computeConfigForRequest(uri);
        assertTrue(config.needSynchronization());
        assertTrue(config.needTransaction());

        uri = "/nuxeo/TestServlet/toto";
        config = requestControllerService.computeConfigForRequest(uri);
        assertTrue(config.needSynchronization());
        assertTrue(config.needTransaction());

        uri = "/nuxeo/CacheDefault";
        config = requestControllerService.computeConfigForRequest(uri);
        assertFalse(config.isCached());
        assertFalse(config.isPrivate());
        assertEquals("3599", config.getCacheTime());

        uri = "/nuxeo/Cache";
        config = requestControllerService.computeConfigForRequest(uri);
        assertTrue(config.isCached());
        assertTrue(config.isPrivate());
        assertEquals("3000", config.getCacheTime());
    }

    @Test
    public void testCorsContrib() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        String uri;
        CORSFilter filter;
        CORSConfiguration config;

        uri = "/dummy/uri";
        when(request.getRequestURI()).thenReturn(uri);
        filter = requestControllerManager.getCorsFilterForRequest(request);
        assertNull(filter);

        uri = "/nuxeo/site/minimal/something/long/dummy.html";
        when(request.getRequestURI()).thenReturn(uri);
        filter = requestControllerManager.getCorsFilterForRequest(request);
        config = filter.getConfiguration();
        assertEquals(-1, config.maxAge);
        assertEquals(Collections.emptySet(), config.allowedOrigins);

        uri = "/nuxeo/site/dummy/";
        when(request.getRequestURI()).thenReturn(uri);
        filter = requestControllerManager.getCorsFilterForRequest(request);
        config = filter.getConfiguration();
        assertEquals(3600, config.maxAge);
        Set<Origin> expectedOrigins = new HashSet<>();
        expectedOrigins.add(new Origin("http://example.com"));
        expectedOrigins.add(new Origin("http://example.com:8080"));
        assertEquals(expectedOrigins, config.allowedOrigins);
        assertFalse(config.supportsCredentials);
    }

    @Test
    public void testHeadersContrib() throws Exception {
        Map<String, String> rh = requestControllerManager.getResponseHeaders();
        assertEquals(2, rh.size());
        assertTrue(rh.containsKey("WWW-Authenticate"));
        assertEquals("basic", rh.get("WWW-Authenticate"));
        assertTrue(rh.containsKey("X-UA-Compatible"));
    }

    /**
     * Test if the "enabled" attribute of the "header" extension point is taken into
     * account to disable a response header
     *
     * @since 10.3
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-requestcontrollerservice-disabled-config.xml")
    public void testDisabledHeadersContrib() throws Exception {
        Map<String, String> rh = requestControllerManager.getResponseHeaders();
        assertEquals(6, rh.size());
        assertFalse(rh.containsKey("X-UA-Compatible"));
    }

}
