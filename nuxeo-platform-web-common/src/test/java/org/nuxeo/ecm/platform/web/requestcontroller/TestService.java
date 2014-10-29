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

import java.util.Map;

import javax.servlet.FilterConfig;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerManager;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerService;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestFilterConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestService extends NXRuntimeTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.web.common",
                "OSGI-INF/web-request-controller-framework.xml");
    }

    @Test
    public void testServiceRegistration() {
        RequestControllerManager rcm = Framework
                .getLocalService(RequestControllerManager.class);
        assertNotNull(rcm);
    }

    @Test
    public void testServiceContrib() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.web.common",
                "OSGI-INF/web-request-controller-contrib-test.xml");

        RequestControllerManager rcm = Framework
                .getLocalService(RequestControllerManager.class);
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
        deployContrib("org.nuxeo.ecm.platform.web.common",
                "OSGI-INF/web-request-controller-contrib-test.xml");

        RequestControllerManager rcm = Framework
                .getLocalService(RequestControllerManager.class);
        assertNotNull(rcm);

        String uri = "";
        FilterConfig fc;

        RequestControllerService rcs = (RequestControllerService) rcm;
        assertNull(rcs.computeCorsFilterConfigForUri("/dummy/uri"));

        uri = "/nuxeo/site/minimal/something/long/dummy.html";
        fc = rcs.computeCorsFilterConfigForUri(uri);
        assertEquals("-1", fc.getInitParameter("cors.maxAge"));
        assertEquals(null, fc.getInitParameter("cors.allowOrigin"));

        uri = "/nuxeo/site/dummy/";
        fc = rcs.computeCorsFilterConfigForUri(uri);
        assertEquals("3600", fc.getInitParameter("cors.maxAge"));
        assertEquals("http://example.com http://example.com:8080", fc.getInitParameter("cors.allowOrigin"));
        assertEquals("false", fc.getInitParameter("cors.supportsCredentials"));
    }

    @Test
    public void testHeadersContrib() throws Exception {
        deployContrib("org.nuxeo.ecm.platform.web.common",
                "OSGI-INF/web-request-controller-contrib-test.xml");

        RequestControllerManager rcm = Framework
                .getLocalService(RequestControllerManager.class);
        assertNotNull(rcm);

        Map<String, String> rh = rcm.getResponseHeaders();
        assertEquals(2, rh.size());
        assertTrue(rh.containsKey("WWW-Authenticate"));
        assertEquals("basic", rh.get("WWW-Authenticate"));
    }

}
