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
 *     Academie de Rennes - proxy CAS support
 */
package org.nuxeo.ecm.platform.ui.web.auth.simple;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author Benjamin JALON
 */
@RunWith(FeaturesRunner.class)
@Features(RuntimeFeature.class)
// Mock the usermanager service (we don't want to pull all nuxeo framework) Needed by Anonymous
@Deploy("org.nuxeo.ecm.platform.login.cas2.test:OSGI-INF/mock-usermanager-framework.xml")
// Mock the event producer (we don't want to pull all nuxeo framework) NuxeoAuthenticationFilter sends events
@Deploy("org.nuxeo.ecm.platform.login.cas2.test:OSGI-INF/mock-event-framework.xml")
@Deploy("org.nuxeo.ecm.platform.login")
@Deploy("org.nuxeo.ecm.platform.web.common")
public abstract class AbstractAuthenticator {

    protected static final String SCHEME = "http";

    protected static final String HOST = "localhost";

    protected static final int PORT = 8080;

    protected static final String CONTEXT = "/nuxeo";

    protected HttpServletResponse response;

    protected HttpServletRequest request;

    protected NuxeoAuthenticationFilter naf;

    protected FilterChain chain;

    protected PluggableAuthenticationService getAuthService() {
        return Framework.getService(PluggableAuthenticationService.class);
    }

    protected void initRequest() throws ServletException, IOException {
        // Cookie[] cookieArray = cookieList.toArray(new Cookie[] {});

        naf = new NuxeoAuthenticationFilter();

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        mockRequestAttributes(request);
        mockRequestURI(request, "/something", "", "");

        chain = new MockFilterChain();

        FilterConfig config = mock(FilterConfig.class);
        naf.init(config);

        // record output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        @SuppressWarnings("resource")
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, UTF_8), true);
        when(response.getWriter()).thenReturn(writer);
    }

    protected void mockRequestURI(HttpServletRequest request, String servletPath, String pathInfo, String queryString) {
        mockRequestURI(request, servletPath, pathInfo, queryString, null);
    }

    protected void mockRequestURI(HttpServletRequest request, String servletPath, String pathInfo, String queryString,
            String requestURI) {
        if ("".equals(pathInfo)) {
            pathInfo = null;
        }
        if ("".equals(queryString)) {
            queryString = null;
        }
        if (requestURI == null) {
            // requestURI is not always exactly contextPath + servletPath + pathInfo, despite the spec
            requestURI = CONTEXT + servletPath;
            if (pathInfo != null) {
                requestURI += pathInfo;
            }
        }
        // good enough for tests that don't use encoded/decoded URLs
        when(request.getScheme()).thenReturn(SCHEME);
        when(request.getServerName()).thenReturn(HOST);
        when(request.getServerPort()).thenReturn(PORT);
        when(request.getRequestURI()).thenReturn(requestURI);
        when(request.getContextPath()).thenReturn(CONTEXT);
        when(request.getServletPath()).thenReturn(servletPath);
        when(request.getPathInfo()).thenReturn(pathInfo);
        when(request.getQueryString()).thenReturn(queryString);
    }

    protected Map<String, Object> mockRequestAttributes(HttpServletRequest request) {
        Map<String, Object> attributes = new HashMap<>();
        // getAttribute
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            return attributes.get(key);
        }).when(request).getAttribute(anyString());
        // setAttribute
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            Object value = i.getArguments()[1];
            attributes.put(key, value);
            return null;
        }).when(request).setAttribute(anyString(), any());
        // removeAttribute
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            attributes.remove(key);
            return null;
        }).when(request).removeAttribute(anyString());
        // getAttributeNames
        doAnswer(i -> attributes.keySet()).when(request).getAttributeNames();
        return attributes;
    }

    protected void setLoginPasswordInHeader(String login, String password, HttpServletRequest request) {
        String b64userpassword = Base64.encodeBase64String((login + ":" + password).getBytes());
        when(request.getHeader(eq("authorization"))).thenReturn("basic " + b64userpassword);
    }

}
