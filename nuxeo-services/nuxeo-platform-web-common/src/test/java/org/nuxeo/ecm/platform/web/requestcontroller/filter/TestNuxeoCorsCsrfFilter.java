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
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpTrace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.NuxeoCorsCsrfFilter;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, MockitoFeature.class })
@Deploy("org.nuxeo.ecm.platform.web.common:OSGI-INF/web-request-controller-framework.xml")
public class TestNuxeoCorsCsrfFilter {

    protected static final String NUXEO_VIRTUAL_HOST = "nuxeo-virtual-host";

    // lowercase because that's what VirtualHostHelper is using when calling getHeader
    // (avoids doing a more complex Mockito matcher than Matchers.eq)
    protected static final String X_FORWARDED_PROTO = "x-forwarded-proto";

    protected static final String X_FORWARDED_HOST = "x-forwarded-host";

    protected static final String X_FORWARDED_PORT = "x-forwarded-port";

    protected NuxeoCorsCsrfFilter filter;

    protected DummyFilterChain chain;

    protected static class DummyFilterChain implements FilterChain {

        protected boolean called;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) {
            called = true;
        }
    }

    @Before
    public void setUp() throws Exception {
        filter = new NuxeoCorsCsrfFilter();
        chain = new DummyFilterChain();
    }

    @After
    public void tearDown() {
        filter.destroy();
    }

    // test basic extraction methods

    @Test
    public void testSourceURIOrigin() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(eq(ORIGIN))).thenReturn("http://example.com:8080");
        assertEquals("http://example.com:8080", filter.getSourceURI(request).toASCIIString());
    }

    @Test
    public void testSourceURIOriginList() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(eq(ORIGIN))).thenReturn("http://example.com:8080 http://other.com");
        assertEquals("http://example.com:8080", filter.getSourceURI(request).toASCIIString());
    }

    @Test
    public void testSourceURIOriginNull() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(eq(ORIGIN))).thenReturn("null");
        assertNull(filter.getSourceURI(request));
    }

    @Test
    public void testSourceURIReferer() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(eq(REFERER))).thenReturn("http://example.com:8080/nuxeo");
        assertEquals("http://example.com:8080/nuxeo", filter.getSourceURI(request).toASCIIString());
    }

    @SuppressWarnings("boxing")
    @Test
    public void testTargetURI() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("example.com");
        when(request.getServerPort()).thenReturn(8080);
        assertEquals("http://example.com:8080/", filter.getTargetURI(request).toASCIIString());
    }

    @Test
    public void testTargetURINuxeoVirtualHostHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(eq(NUXEO_VIRTUAL_HOST))).thenReturn("http://example.com:8080/nuxeo/");
        assertEquals("http://example.com:8080/nuxeo/", filter.getTargetURI(request).toASCIIString());
    }

    @Test
    public void testTargetURIForwardedHeaders() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(eq(X_FORWARDED_PROTO))).thenReturn("http");
        when(request.getHeader(eq(X_FORWARDED_HOST))).thenReturn("example.com");
        when(request.getHeader(eq(X_FORWARDED_PORT))).thenReturn("80");
        assertEquals("http://example.com/", filter.getTargetURI(request).toASCIIString());
    }

    @Test
    public void testTargetURIForwardedHeadersHttps() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(eq(X_FORWARDED_PROTO))).thenReturn("https");
        when(request.getHeader(eq(X_FORWARDED_HOST))).thenReturn("example.com");
        when(request.getHeader(eq(X_FORWARDED_PORT))).thenReturn("443");
        assertEquals("https://example.com/", filter.getTargetURI(request).toASCIIString());
    }

    @Test
    public void testTargetURIForwardedHeadersCustomPort() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader(eq(X_FORWARDED_PROTO))).thenReturn("http");
        when(request.getHeader(eq(X_FORWARDED_HOST))).thenReturn("example.com");
        when(request.getHeader(eq(X_FORWARDED_PORT))).thenReturn("8080"); // TODO bug in VHH, ignored
        assertEquals("http://example.com/", filter.getTargetURI(request).toASCIIString());
    }

    // test full filter

    /**
     * User agent sending no Origin nor Referer header.
     */
    @Test
    public void testNoOriginNorReferer() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(eq(NUXEO_VIRTUAL_HOST))).thenReturn("http://example.com:8080/");

        filter.doFilter(request, response, chain);
        assertTrue(chain.called);
    }

    /**
     * Browser sending the Origin header, no proxy.
     */
    @SuppressWarnings("boxing")
    @Test
    public void testMatchOrigin() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(eq(ORIGIN))).thenReturn("http://example.com:8080");
        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("example.com");
        when(request.getServerPort()).thenReturn(8080);

        filter.doFilter(request, response, chain);
        assertTrue(chain.called);
    }

    /**
     * Browser sending the Origin header, proxy configured with Nuxeo-Virtual-Host header.
     */
    @Test
    public void testMatchOriginWithVirtualHost() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(eq(ORIGIN))).thenReturn("http://example.com:8080");
        when(request.getHeader(eq(NUXEO_VIRTUAL_HOST))).thenReturn("http://example.com:8080/");

        filter.doFilter(request, response, chain);
        assertTrue(chain.called);
    }

    /**
     * Browser sending the Origin header, proxy configured with X-Forwarded headers.
     */
    @Test
    public void testMatchOriginWithForwardedHeaders() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(eq(ORIGIN))).thenReturn("https://nicesite.example.com");
        when(request.getHeader(eq(X_FORWARDED_PROTO))).thenReturn("https");
        when(request.getHeader(eq(X_FORWARDED_HOST))).thenReturn("nicesite.example.com");
        when(request.getHeader(eq(X_FORWARDED_PORT))).thenReturn("443");

        filter.doFilter(request, response, chain);
        assertTrue(chain.called);
    }

    /**
     * Browser sending the Referer header.
     */
    @Test
    public void testMatchReferer() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(eq(REFERER))).thenReturn("http://example.com:8080/nuxeo/somepage.html");
        when(request.getHeader(eq(NUXEO_VIRTUAL_HOST))).thenReturn("http://example.com:8080/");

        filter.doFilter(request, response, chain);
        assertTrue(chain.called);
    }

    @Test
    public void testMismatchButGet() throws Exception {
        doTestMismatchButNonStateChangingMethod(HttpGet.METHOD_NAME);
    }

    @Test
    public void testMismatchButHead() throws Exception {
        doTestMismatchButNonStateChangingMethod(HttpHead.METHOD_NAME);
    }

    @Test
    public void testMismatchButOptions() throws Exception {
        doTestMismatchButNonStateChangingMethod(HttpOptions.METHOD_NAME);
    }

    @Test
    public void testMismatchButTrace() throws Exception {
        doTestMismatchButNonStateChangingMethod(HttpTrace.METHOD_NAME);
    }

    /**
     * Browser sending the Referer header from an external page with a non-state-changing method.
     */
    public void doTestMismatchButNonStateChangingMethod(String method) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getHeader(eq(REFERER))).thenReturn("http://google.com/");
        when(request.getHeader(eq(NUXEO_VIRTUAL_HOST))).thenReturn("http://example.com:8080/");

        filter.doFilter(request, response, chain);
        assertTrue(chain.called);
    }

    /**
     * Browser sending an Origin header with a whitelisted browser-specific scheme.
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-cors-config.xml")
    public void testOriginFromBrowserExtension() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader(eq(ORIGIN))).thenReturn("moz-extension://12345678-1234-1234-1234-1234567890ab");
        when(request.getHeader(eq(NUXEO_VIRTUAL_HOST))).thenReturn("http://example.com:8080/");
        when(request.getRequestURI()).thenReturn("/nuxeo/site/something");

        filter.doFilter(request, response, chain);
        assertTrue(chain.called);
    }

    /**
     * Browser sending the Origin header from another page which is allowed by CORS.
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-cors-config.xml")
    public void testMismatchPostButAllowedByCORS() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader(eq(ORIGIN))).thenReturn("http://friendly.com");
        when(request.getHeader(eq(NUXEO_VIRTUAL_HOST))).thenReturn("http://example.com:8080/");
        when(request.getRequestURI()).thenReturn("/nuxeo/site/something");

        filter.doFilter(request, response, chain);
        assertTrue(chain.called);
    }

    /**
     * Buggy browser (Edge/IE11) not sending a Origin header but just a Referer header (which can include path and query
     * parts) when redirecting to a POST on the site (SAML login use case).
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-cors-config.xml")
    public void testMismatchPostFromBuggyBrowser() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader(eq(REFERER))).thenReturn("http://friendly.com/myapp/login?key=123"); // SSO
        when(request.getHeader(eq(NUXEO_VIRTUAL_HOST))).thenReturn("http://example.com:8080/");
        when(request.getRequestURI()).thenReturn("/nuxeo/site/something");

        filter.doFilter(request, response, chain);
        assertTrue(chain.called);
    }

    /**
     * Browser sending the Origin header from an attacker page, must fail.
     */
    @SuppressWarnings("boxing")
    @Test
    public void testMismatchPost() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader(eq(ORIGIN))).thenReturn("http://attacker.com");
        when(request.getHeader(eq(NUXEO_VIRTUAL_HOST))).thenReturn("http://example.com:8080/");
        when(request.getRequestURI()).thenReturn("/nuxeo/site/something");
        MutableObject<InvocationOnMock> error = new MutableObject<>();
        doAnswer(invocation -> {
            error.setValue(invocation);
            return null;
        }).when(response).sendError(anyInt(), anyString());

        filter.doFilter(request, response, chain);

        assertFalse(chain.called);
        assertNotNull(error.getValue());
        Object[] arguments = error.getValue().getArguments();
        assertEquals(HttpServletResponse.SC_FORBIDDEN, arguments[0]); // 403
        assertEquals("CSRF check failure", arguments[1]);
    }

}
