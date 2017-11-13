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
package org.nuxeo.ecm.platform.web.requestcontroller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.inject.Inject;
import javax.servlet.FilterConfig;

import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerService;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestFilterConfig;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

public class TestService extends NXRuntimeTestCase {

    @Inject
    protected RequestControllerService requestControllerService;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        deployContrib("org.nuxeo.ecm.platform.web.common", "OSGI-INF/web-request-controller-framework.xml");
        deployContrib("org.nuxeo.ecm.platform.web.common", "OSGI-INF/web-request-controller-contrib.xml");
    }

    @Override
    public void tearDown() throws Exception {
        undeployContrib("org.nuxeo.ecm.platform.web.common", "OSGI-INF/web-request-controller-contrib.xml");
        undeployContrib("org.nuxeo.ecm.platform.web.common", "OSGI-INF/web-request-controller-framework.xml");
        super.tearDown();
    }

    @Test
    public void testServiceRegistration() {
        assertNotNull(requestControllerService);
    }

    @Test
    public void testServiceContrib() throws Exception {
        pushInlineDeployments(
                "org.nuxeo.ecm.platform.web.common.test:OSGI-INF/web-request-controller-contrib-test.xml");

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
        pushInlineDeployments(
                "org.nuxeo.ecm.platform.web.common.test:OSGI-INF/web-request-controller-contrib-test.xml");

        String uri;
        FilterConfig fc;

        assertNull(requestControllerService.computeCorsFilterConfigForUri("/dummy/uri"));

        uri = "/nuxeo/site/minimal/something/long/dummy.html";
        fc = requestControllerService.computeCorsFilterConfigForUri(uri);
        assertEquals("-1", fc.getInitParameter("cors.maxAge"));
        assertEquals(null, fc.getInitParameter("cors.allowOrigin"));

        uri = "/nuxeo/site/dummy/";
        fc = requestControllerService.computeCorsFilterConfigForUri(uri);
        assertEquals("3600", fc.getInitParameter("cors.maxAge"));
        assertEquals("http://example.com http://example.com:8080", fc.getInitParameter("cors.allowOrigin"));
        assertEquals("false", fc.getInitParameter("cors.supportsCredentials"));
    }

    @Test
    public void testHeadersContrib() throws Exception {
        pushInlineDeployments(
                "org.nuxeo.ecm.platform.web.common.test:OSGI-INF/web-request-controller-contrib-test.xml");

        Map<String, String> rh = requestControllerService.getResponseHeaders();
        assertEquals(7, rh.size());
        assertTrue(rh.containsKey("WWW-Authenticate"));
        assertEquals("basic", rh.get("WWW-Authenticate"));
    }

    /**
     * @since 9.3
     */
    @Test
    public void testNXFilePatterns() {
        // file:content
        RequestFilterConfig config = requestControllerService.computeConfigForRequest(
                "/nuxeo/nxfile/default/45ad4c71-f1f3-4e9d-97a1-61aa317ce105/file:content/IMG_20170910_110134.jpg");
        assertNotNull(config);
        assertFalse(config.isCached());
        assertFalse(config.isPrivate());
        assertFalse(config.needTransaction());
        assertFalse(config.needSynchronization());

        // picture:views
        config = requestControllerService.computeConfigForRequest(
                "/nuxeo/nxfile/default/45ad4c71-f1f3-4e9d-97a1-61aa317ce105/picture:views/3/content/FullHD_IMG_20170910_110134.jpg");
        assertNotNull(config);
        assertFalse(config.isCached());
        assertFalse(config.isPrivate());
        assertFalse(config.needTransaction());
        assertFalse(config.needSynchronization());
    }

}
