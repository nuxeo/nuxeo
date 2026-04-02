/*
 * (C) Copyright 2006-2024 Nuxeo (http://nuxeo.com/) and others.
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.web.common.WebCommonFeature;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

/**
 * @author Benjamin JALON
 */
@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, MockitoFeature.class, WebCommonFeature.class })
// Mock the event producer (we don't want to pull all nuxeo framework) NuxeoAuthenticationFilter sends events
@Deploy("org.nuxeo.ecm.platform.login.cas2.test:OSGI-INF/mock-event-framework.xml")
@Deploy("org.nuxeo.ecm.platform.login")
public abstract class AbstractAuthenticator {

    protected static final String CAS_USER = "CasUser";

    protected static final String SCHEME = "http";

    protected static final String HOST = "localhost";

    protected static final int PORT = 8080;

    protected static final String CONTEXT = "/nuxeo";

    protected HttpServletResponse response;

    protected HttpServletRequest request;

    protected NuxeoAuthenticationFilter naf;

    protected FilterChain chain;

    @Mock
    @RuntimeService
    protected UserManager userManager;

    @Before
    public void setUp() {
        String anonymousUserId = "Anonymous";
        when(userManager.getAnonymousUserId()).thenReturn(anonymousUserId);
        NuxeoPrincipal anonymousPrincipal = new UserPrincipal(anonymousUserId, null, true, false);
        NuxeoPrincipal casUserPrincipal = new UserPrincipal(CAS_USER, null, false, false);
        when(userManager.getPrincipal(anonymousUserId)).thenReturn(anonymousPrincipal);
        when(userManager.getPrincipal(CAS_USER)).thenReturn(casUserPrincipal);
    }

    protected void initRequest() throws ServletException, IOException {
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
}
