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
@Deploy("org.nuxeo.ecm.platform.web.common:OSGI-INF/web-request-controller-framework.xml")
@Deploy("org.nuxeo.ecm.platform.web.common:OSGI-INF/web-request-controller-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/web-request-controller-contrib-test.xml")
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
        assertEquals(8, rh.size());
        assertTrue(rh.containsKey("WWW-Authenticate"));
        assertEquals("basic", rh.get("WWW-Authenticate"));
        assertTrue(rh.containsKey("X-UA-Compatible"));
        assertFalse(rh.containsKey("Warning"));
    }

    /**
     * @since 9.3
     */
    @Test
    public void testNXFilePatterns() {
        RequestControllerService requestControllerService = (RequestControllerService) requestControllerManager;

        // file:content
        RequestFilterConfig config = requestControllerService.computeConfigForRequest(
                "/nuxeo/nxfile/default/45ad4c71-f1f3-4e9d-97a1-61aa317ce105/file:content/IMG_20170910_110134.jpg");
        assertNotNull(config);
        // !!!WARN: Don't change this unit test without asking front end devs !!!
        assertTrue(config.isCached());
        assertEquals("0", config.getCacheTime());
        // end of WARN !!!
        assertTrue(config.isPrivate());
        assertFalse(config.needTransaction());
        assertFalse(config.needSynchronization());

        // picture:views
        config = requestControllerService.computeConfigForRequest(
                "/nuxeo/nxfile/default/45ad4c71-f1f3-4e9d-97a1-61aa317ce105/picture:views/3/content/FullHD_IMG_20170910_110134.jpg");
        assertNotNull(config);
        // !!!WARN: Don't change this unit test without asking front end devs !!!
        assertTrue(config.isCached());
        assertEquals("0", config.getCacheTime());
        // end of WARN !!!
        assertTrue(config.isPrivate());
        assertFalse(config.needTransaction());
        assertFalse(config.needSynchronization());

        // picture:views but with changeToken
        config = requestControllerService.computeConfigForRequest(
                "/nuxeo/nxfile/default/45ad4c71-f1f3-4e9d-97a1-61aa317ce105/picture:views/3/content/FullHD_IMG_20170910_110134.jpg?changeToken=5-1");
        assertNotNull(config);
        // !!!WARN: Don't change this unit test without asking front end devs !!!
        assertTrue(config.isCached());
        assertEquals("31536000", config.getCacheTime());
        // end of WARN !!!
        assertTrue(config.isPrivate());
        assertFalse(config.needTransaction());
        assertFalse(config.needSynchronization());
    }

    /**
     * @since 10.3
     */
    @Test
    public void testThumbnailPatterns() {
        RequestControllerService requestControllerService = (RequestControllerService) requestControllerManager;
        RequestFilterConfig config = requestControllerService.computeConfigForRequest(
                "/nuxeo/api/v1/repo/default/id/45ad4c71-f1f3-4e9d-97a1-61aa317ce105/@rendition/thumbnail");
        assertNotNull(config);
        // !!!WARN: Don't change this unit test without asking front end devs !!!
        assertTrue(config.isCached());
        assertEquals("0", config.getCacheTime());
        // end of WARN !!!
        assertTrue(config.isPrivate());
        assertTrue(config.needTransaction());
        config = requestControllerService.computeConfigForRequest(
                "/nuxeo/api/v1/repo/default/id/45ad4c71-f1f3-4e9d-97a1-61aa317ce105/@rendition/thumbnail?changeToken=5-1");
        assertNotNull(config);
        // !!!WARN: Don't change this unit test without asking front end devs !!!
        assertTrue(config.isCached());
        assertEquals("31536000", config.getCacheTime());
        // end of WARN !!!
        assertTrue(config.isPrivate());
        assertTrue(config.needTransaction());
    }

    /**
     * @since 10.3
     */
    @Test
    public void testAdminPatterns() {
        RequestControllerService requestControllerService = (RequestControllerService) requestControllerManager;
        RequestFilterConfig config = requestControllerService.computeConfigForRequest(
                "/nuxeo/nxadmin/default/default-domain@view_admin?tabIds=NUXEO_ADMIN");
        assertNotNull(config);
        assertFalse(config.isCached());
        assertTrue(config.isPrivate());
    }
}
