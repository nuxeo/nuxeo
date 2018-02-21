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

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerManager;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerService;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestFilterConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import com.thetransactioncompany.cors.CORSConfiguration;
import com.thetransactioncompany.cors.CORSFilter;
import com.thetransactioncompany.cors.Origin;

public class TestService extends NXRuntimeTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.web.common", "OSGI-INF/web-request-controller-framework.xml");
    }

    @Test
    public void testServiceRegistration() {
        RequestControllerManager rcm = Framework.getLocalService(RequestControllerManager.class);
        assertNotNull(rcm);
    }

    @Test
    public void testServiceContrib() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.web.common", "OSGI-INF/web-request-controller-contrib-test.xml");

        RequestControllerManager rcm = Framework.getLocalService(RequestControllerManager.class);
        assertNotNull(rcm);

        RequestControllerService rcmTest = (RequestControllerService) rcm;

        String uri = "";
        RequestFilterConfig config;

        uri = "/SyncNoTx/test";
        config = rcmTest.computeConfigForRequest(uri);
        assertTrue(config.needSynchronization());
        assertFalse(config.needTransaction());

        uri = "/NoSyncTx/test";
        config = rcmTest.computeConfigForRequest(uri);
        assertFalse(config.needSynchronization());
        assertTrue(config.needTransaction());

        uri = "/SyncTx/test";
        config = rcmTest.computeConfigForRequest(uri);
        assertTrue(config.needSynchronization());
        assertTrue(config.needTransaction());

        uri = "/SyncTx/deny";
        config = rcmTest.computeConfigForRequest(uri);
        assertFalse(config.needSynchronization());
        assertFalse(config.needTransaction());

        uri = "/whatever";
        config = rcmTest.computeConfigForRequest(uri);
        assertFalse(config.needSynchronization());
        assertFalse(config.needTransaction());

        uri = "/nuxeo/TestServlet/";
        config = rcmTest.computeConfigForRequest(uri);
        assertTrue(config.needSynchronization());
        assertTrue(config.needTransaction());

        uri = "/nuxeo/TestServlet";
        config = rcmTest.computeConfigForRequest(uri);
        assertTrue(config.needSynchronization());
        assertTrue(config.needTransaction());

        uri = "/nuxeo/TestServlet/toto";
        config = rcmTest.computeConfigForRequest(uri);
        assertTrue(config.needSynchronization());
        assertTrue(config.needTransaction());

        uri = "/nuxeo/CacheDefault";
        config = rcmTest.computeConfigForRequest(uri);
        assertFalse(config.isCached());
        assertFalse(config.isPrivate());
        assertEquals("3599", config.getCacheTime());

        uri = "/nuxeo/Cache";
        config = rcmTest.computeConfigForRequest(uri);
        assertTrue(config.isCached());
        assertTrue(config.isPrivate());
        assertEquals("3000", config.getCacheTime());
    }

    @Test
    public void testCorsContrib() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.web.common", "OSGI-INF/web-request-controller-contrib-test.xml");

        RequestControllerManager rcm = Framework.getLocalService(RequestControllerManager.class);
        assertNotNull(rcm);

        HttpServletRequest request = mock(HttpServletRequest.class);
        String uri;
        CORSFilter filter;
        CORSConfiguration config;

        uri = "/dummy/uri";
        when(request.getRequestURI()).thenReturn(uri);
        filter = rcm.getCorsFilterForRequest(request);
        assertNull(filter);

        uri = "/nuxeo/site/minimal/something/long/dummy.html";
        when(request.getRequestURI()).thenReturn(uri);
        filter = rcm.getCorsFilterForRequest(request);
        config = filter.getConfiguration();
        assertEquals(-1, config.maxAge);
        assertEquals(Collections.emptySet(), config.allowedOrigins);

        uri = "/nuxeo/site/dummy/";
        when(request.getRequestURI()).thenReturn(uri);
        filter = rcm.getCorsFilterForRequest(request);
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
        deployContrib("org.nuxeo.ecm.platform.web.common", "OSGI-INF/web-request-controller-contrib-test.xml");

        RequestControllerManager rcm = Framework.getLocalService(RequestControllerManager.class);
        assertNotNull(rcm);

        Map<String, String> rh = rcm.getResponseHeaders();
        assertEquals(2, rh.size());
        assertTrue(rh.containsKey("WWW-Authenticate"));
        assertEquals("basic", rh.get("WWW-Authenticate"));
    }

}
