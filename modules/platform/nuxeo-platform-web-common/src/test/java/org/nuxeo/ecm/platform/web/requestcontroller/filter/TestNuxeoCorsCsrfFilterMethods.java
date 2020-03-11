/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.web.requestcontroller.filter;

import static com.google.common.net.HttpHeaders.ORIGIN;
import static com.google.common.net.HttpHeaders.REFERER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.platform.web.common.requestcontroller.filter.NuxeoCorsCsrfFilter.PRIVACY_SENSITIVE;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.NuxeoCorsCsrfFilter;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestNuxeoCorsCsrfFilterMethods {

    protected static final String NUXEO_VIRTUAL_HOST = "nuxeo-virtual-host";

    // lowercase because that's what VirtualHostHelper is using when calling getHeader
    // (avoids doing a more complex Mockito matcher than Matchers.eq)
    protected static final String X_FORWARDED_PROTO = "x-forwarded-proto";

    protected static final String X_FORWARDED_HOST = "x-forwarded-host";

    protected static final String X_FORWARDED_PORT = "x-forwarded-port";

    protected NuxeoCorsCsrfFilter filter;

    protected HttpServletRequest request;

    @Before
    public void setUp() {
        filter = new NuxeoCorsCsrfFilter();
        request = mock(HttpServletRequest.class);
    }

    @Test
    public void testSourceURIOrigin() {
        when(request.getHeader(eq(ORIGIN))).thenReturn("http://example.com:8080");
        assertEquals("http://example.com:8080", filter.getSourceURI(request).toASCIIString());
    }

    @Test
    public void testSourceURIOriginList() {
        when(request.getHeader(eq(ORIGIN))).thenReturn("http://example.com:8080 http://other.com");
        assertEquals("http://example.com:8080", filter.getSourceURI(request).toASCIIString());
    }

    @Test
    public void testSourceURIOriginNullDefault() {
        doTestSourceURIOriginNull(false);
    }

    protected void doTestSourceURIOriginNull(boolean allowNullOrigin) {
        when(request.getHeader(eq(ORIGIN))).thenReturn("null");
        URI uri = filter.getSourceURI(request);
        if (allowNullOrigin) {
            assertNull(uri);
        } else {
            assertNotNull(uri);
            assertEquals("privacy-sensitive:///", uri.toASCIIString());
        }
    }

    @Test
    public void testSourceURIReferer() {
        when(request.getHeader(eq(REFERER))).thenReturn("http://example.com:8080/nuxeo");
        assertEquals("http://example.com:8080/nuxeo", filter.getSourceURI(request).toASCIIString());
    }

    @SuppressWarnings("boxing")
    @Test
    public void testTargetURI() {
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("example.com");
        when(request.getServerPort()).thenReturn(8080);
        assertEquals("http://example.com:8080/", filter.getTargetURI(request).toASCIIString());
    }

    @Test
    public void testTargetURINuxeoVirtualHostHeader() {
        when(request.getHeader(eq(NUXEO_VIRTUAL_HOST))).thenReturn("http://example.com:8080/nuxeo/");
        assertEquals("http://example.com:8080/nuxeo/", filter.getTargetURI(request).toASCIIString());
    }

    @Test
    public void testTargetURIForwardedHeaders() {
        when(request.getHeader(eq(X_FORWARDED_PROTO))).thenReturn("http");
        when(request.getHeader(eq(X_FORWARDED_HOST))).thenReturn("example.com");
        when(request.getHeader(eq(X_FORWARDED_PORT))).thenReturn("80");
        assertEquals("http://example.com/", filter.getTargetURI(request).toASCIIString());
    }

    @Test
    public void testTargetURIForwardedHeadersHttps() {
        when(request.getHeader(eq(X_FORWARDED_PROTO))).thenReturn("https");
        when(request.getHeader(eq(X_FORWARDED_HOST))).thenReturn("example.com");
        when(request.getHeader(eq(X_FORWARDED_PORT))).thenReturn("443");
        assertEquals("https://example.com/", filter.getTargetURI(request).toASCIIString());
    }

    @Test
    public void testTargetURIForwardedHeadersCustomPort() {
        when(request.getHeader(eq(X_FORWARDED_PROTO))).thenReturn("http");
        when(request.getHeader(eq(X_FORWARDED_HOST))).thenReturn("example.com");
        when(request.getHeader(eq(X_FORWARDED_PORT))).thenReturn("8080"); // TODO bug in VHH, ignored
        assertEquals("http://example.com/", filter.getTargetURI(request).toASCIIString());
    }

    @Test
    public void testPrivacySensitiveURIDoesNotMatch() {
        assertFalse(filter.sourceAndTargetMatch(PRIVACY_SENSITIVE, URI.create("http://example.com:8080")));
    }

}
