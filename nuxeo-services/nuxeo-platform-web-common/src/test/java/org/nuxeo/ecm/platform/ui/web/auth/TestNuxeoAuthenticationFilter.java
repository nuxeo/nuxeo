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
     * <p>
     * Case of a servlet mapped with {@code <url-pattern>*.xhtml</url-pattern>}
     */
    @Test
    public void testRequestedPageMatchExtension() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        // here we use info that a servlet container would provide, based on parsing per the servlet spec
        when(request.getRequestURI()).thenReturn("/nuxeo/login.jsp/../foo/bar.xhtml;jsessionid=123?gee=moo");
        when(request.getContextPath()).thenReturn("/nuxeo");
        when(request.getServletPath()).thenReturn("/foo/bar.xhtml");
        when(request.getPathInfo()).thenReturn(null);
        when(request.getQueryString()).thenReturn("gee=moo");

        String page = NuxeoAuthenticationFilter.getRequestedPage(request);
        assertEquals("foo/bar.xhtml", page);
    }

    /**
     * Computation of the requested page based on request info.
     * <p>
     * Case of a servlet mapped with {@code <url-pattern>/foo/*</url-pattern>}
     */
    @Test
    public void testRequestedPageMatchPath() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        // here we use info that a servlet container would provide, based on parsing per the servlet spec
        when(request.getRequestURI()).thenReturn("/nuxeo/login.jsp/../foo/bar.xhtml;jsessionid=123?gee=moo");
        when(request.getContextPath()).thenReturn("/nuxeo");
        when(request.getServletPath()).thenReturn("/foo");
        when(request.getPathInfo()).thenReturn("/bar.xhtml");
        when(request.getQueryString()).thenReturn("gee=moo");

        String page = NuxeoAuthenticationFilter.getRequestedPage(request);
        assertEquals("foo/bar.xhtml", page);
    }

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

    protected void doTestGetRequestedUrl(String expected, String queryString) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        // here we use info that a servlet container would provide, based on parsing per the servlet spec
        when(request.getRequestURI()).thenReturn("/nuxeo/login.jsp/../foo/bar.xhtml;jsessionid=123");
        when(request.getContextPath()).thenReturn("/nuxeo");
        when(request.getServletPath()).thenReturn("/foo");
        when(request.getPathInfo()).thenReturn("/bar.xhtml");
        when(request.getQueryString()).thenReturn(queryString);

        String url = NuxeoAuthenticationFilter.getRequestedUrl(request);
        assertEquals("foo/bar.xhtml" + expected, url);
    }

}
