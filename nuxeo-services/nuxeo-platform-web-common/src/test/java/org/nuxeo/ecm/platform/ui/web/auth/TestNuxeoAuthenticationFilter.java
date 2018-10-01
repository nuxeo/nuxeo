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
package org.nuxeo.ecm.platform.ui.web.auth;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
public class TestNuxeoAuthenticationFilter {

    /**
     * Computation of the requested page based on request info.
     */
    @Test
    public void testGetRequestedPage() throws Exception {
        // case of a servlet mapped with <url-pattern>*.xhtml</url-pattern>
        doTestGetRequestedPage("foo/bar.xhtml", "/nuxeo/foo/bar.xhtml", "/nuxeo", "/foo/bar.xhtml", null, null);
        doTestGetRequestedPage("foo/bar.xhtml", "/nuxeo/login.jsp/../foo/bar.xhtml;jsessionid=123?gee=moo", "/nuxeo",
                "/foo/bar.xhtml", null, "gee=moo");
        // case of a servlet mapped with <url-pattern>/foo/*</url-pattern>
        doTestGetRequestedPage("foo/bar.xhtml", "/nuxeo/foo/bar.xhtml", "/nuxeo", "/foo", "/bar.xhtml", null);
        doTestGetRequestedPage("foo/bar.xhtml", "/nuxeo/login.jsp/../foo/bar.xhtml;jsessionid=123?gee=moo", "/nuxeo",
                "/foo", "/bar.xhtml", "gee=moo");
        // index.jsp requested
        doTestGetRequestedPage("ui/index.jsp", "/nuxeo/ui/index.jsp", "/nuxeo", "/ui/index.jsp", null, null);
        // index.jsp not in the request uri but present in the servlet path (welcome file)
        doTestGetRequestedPage("ui/", "/nuxeo/ui/", "/nuxeo", "/ui/index.jsp", null, null);
    }

    protected static void doTestGetRequestedPage(String expected, String requestURI, String contextPath,
            String servletPath, String pathInfo, String queryString) {
        doTestMockRequest(NuxeoAuthenticationFilter::getRequestedPage, expected, requestURI, contextPath, servletPath,
                pathInfo, queryString);
    }

    /**
     * Computation of the requested URL based on request info.
     */
    @Test
    public void testGetRequestedUrl() {
        doTestGetRequestedUrl("", null);
        doTestGetRequestedUrl("", "");
        doTestGetRequestedUrl("?gee=moo", "gee=moo");
        doTestGetRequestedUrl("?gee=moo&abc=def", "gee=moo&abc=def");
        // strip conversationId
        doTestGetRequestedUrl("?gee=moo", "gee=moo&conversationId=1234");
        doTestGetRequestedUrl("?gee=moo", "conversationId=1234&gee=moo");
        doTestGetRequestedUrl("", "conversationId=1234");
    }

    protected static void doTestGetRequestedUrl(String expectedSuffix, String queryString) {
        doTestGetRequestedUrl("foo/bar.xhtml" + expectedSuffix, "/nuxeo/foo/bar.xhtml", "/nuxeo", "/foo/bar.xhtml",
                null, queryString);
        doTestGetRequestedUrl("foo/bar.xhtml" + expectedSuffix, "/nuxeo/foo/bar.xhtml", "/nuxeo", "/foo", "/bar.xhtml",
                queryString);
        // here we use info that a servlet container would provide, based on parsing per the servlet spec
        doTestGetRequestedUrl("foo/bar.xhtml" + expectedSuffix, "/nuxeo/login.jsp/../foo/bar.xhtml;jsessionid=123",
                "/nuxeo", "/foo", "/bar.xhtml", queryString);
        // index.jsp requested
        doTestGetRequestedUrl("ui/index.jsp" + expectedSuffix, "/nuxeo/ui/index.jsp", "/nuxeo", "/ui/index.jsp", null,
                queryString);
        // index.jsp not in the request uri but present in the servlet path (welcome file)
        doTestGetRequestedUrl("ui/" + expectedSuffix, "/nuxeo/ui/", "/nuxeo", "/ui/index.jsp", null, queryString);
    }

    protected static void doTestGetRequestedUrl(String expected, String requestURI, String contextPath,
            String servletPath, String pathInfo, String queryString) {
        doTestMockRequest(NuxeoAuthenticationFilter::getRequestedUrl, expected, requestURI, contextPath, servletPath,
                pathInfo, queryString);
    }

    protected static void doTestMockRequest(Function<HttpServletRequest, String> function, String expected,
            String requestURI, String contextPath, String servletPath, String pathInfo, String queryString) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn(requestURI);
        when(request.getContextPath()).thenReturn(contextPath);
        when(request.getServletPath()).thenReturn(servletPath);
        when(request.getPathInfo()).thenReturn(pathInfo);
        when(request.getQueryString()).thenReturn(queryString);
        assertEquals(expected, function.apply(request));
    }

}
