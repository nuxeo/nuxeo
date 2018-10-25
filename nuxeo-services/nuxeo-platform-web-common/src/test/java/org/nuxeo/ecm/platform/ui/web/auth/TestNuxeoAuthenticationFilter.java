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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.nuxeo.ecm.platform.ui.web.auth.DummyAuthPluginAnonymous.DUMMY_ANONYMOUS_LOGIN;
import static org.nuxeo.ecm.platform.ui.web.auth.DummyAuthPluginForm.DUMMY_AUTH_FORM_PASSWORD_KEY;
import static org.nuxeo.ecm.platform.ui.web.auth.DummyAuthPluginForm.DUMMY_AUTH_FORM_USERNAME_KEY;
import static org.nuxeo.ecm.platform.ui.web.auth.DummyAuthPluginSSO.DUMMY_SSO_TICKET;
import static org.nuxeo.ecm.platform.ui.web.auth.DummyAuthPluginToken.DUMMY_AUTH_TOKEN_KEY;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.CALLBACK_URL_PARAMETER;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.LOGIN_PAGE;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.LOGOUT_PAGE;
import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.REQUESTED_URL;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventProducer;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.runtime.mockito.MockitoFeature;
import org.nuxeo.runtime.mockito.RuntimeService;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

@RunWith(FeaturesRunner.class)
@Features({ RuntimeFeature.class, MockitoFeature.class })
@Deploy("org.nuxeo.ecm.platform.web.common:OSGI-INF/authentication-framework.xml")
public class TestNuxeoAuthenticationFilter {

    // from NuxeoAuthenticationFilter
    protected static final String BYPASS_AUTHENTICATION_LOG = "byPassAuthenticationLog";

    // from NuxeoAuthenticationFilter
    protected static final String SECURITY_DOMAIN = "securityDomain";

    // from NuxeoAuthenticationFilter
    protected static final String EVENT_LOGIN_SUCCESS = "loginSuccess";

    // from NuxeoAuthenticationFilter
    protected static final String EVENT_LOGOUT = "logout";

    protected static final String SCHEME = "http";

    protected static final String HOST = "localhost";

    protected static final int PORT = 8080;

    protected static final String CONTEXT = "/nuxeo";

    @Mock
    @RuntimeService
    protected UserManager userManager;

    @Mock
    @RuntimeService
    protected EventProducer eventProducer;

    protected NuxeoAuthenticationFilter filter;

    protected DummyFilterChain chain;

    protected ArgumentCaptor<Event> eventCaptor;

    public static class DummyFilterConfig implements FilterConfig {

        protected final Map<String, String> initParameters;

        public DummyFilterConfig(Map<String, String> initParameters) {
            this.initParameters = initParameters;
        }

        @Override
        public String getFilterName() {
            return "NuxeoAuthenticationFilter";
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public String getInitParameter(String name) {
            return initParameters.get(name);
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return Collections.enumeration(initParameters.keySet());
        }
    }

    public static class DummyFilterChain implements FilterChain {

        protected boolean called;

        protected Principal principal;

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            called = true;
            principal = ((HttpServletRequest) request).getUserPrincipal();
        }
    }

    @Before
    public void setUp() throws Exception {
        // filter config
        Map<String, String> initParameters = new HashMap<>();
        initParameters.put(BYPASS_AUTHENTICATION_LOG, "false");
        initParameters.put(SECURITY_DOMAIN, NuxeoAuthenticationFilter.LOGIN_DOMAIN);
        FilterConfig config = new DummyFilterConfig(initParameters);
        // filter
        filter = new NuxeoAuthenticationFilter();
        filter.init(config);
        // filter chain
        chain = new DummyFilterChain();

        // usemanager
        when(userManager.getAnonymousUserId()).thenReturn(DUMMY_ANONYMOUS_LOGIN);
        // events
        eventCaptor = ArgumentCaptor.forClass(Event.class);
    }

    @After
    public void tearDown() {
        filter.destroy();
    }

    protected Map<String, Object> mockSessionAttributes(HttpSession session) {
        Map<String, Object> attributes = new HashMap<>();
        // getAttribute
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            return attributes.get(key);
        }).when(session).getAttribute(anyString());
        // setAttribute
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            Object value = i.getArguments()[1];
            attributes.put(key, value);
            return null;
        }).when(session).setAttribute(anyString(), any());
        // removeAttribute
        doAnswer(i -> {
            String key = (String) i.getArguments()[0];
            attributes.remove(key);
            return null;
        }).when(session).removeAttribute(anyString());
        // invalidate
        doAnswer(i -> {
            attributes.clear();
            return null;
        }).when(session).invalidate();
        return attributes;
    }

    protected void mockRequestURI(HttpServletRequest request, String servletPath, String pathInfo, String queryString) {
        mockRequestURI(request, servletPath, pathInfo, queryString, null);
    }

    @SuppressWarnings("boxing")
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

    protected void checkEvents(String... expectedEventNames) {
        if (expectedEventNames.length == 0) {
            verifyZeroInteractions(eventProducer);
        } else {
            verify(eventProducer).fireEvent(eventCaptor.capture());
            List<Event> events = eventCaptor.getAllValues();
            List<String> eventNames = events.stream().map(Event::getName).collect(toList());
            assertEquals(Arrays.asList(expectedEventNames), eventNames);
        }
    }

    protected void checkNoEvents() {
        checkEvents(new String[] {});
    }

    protected void checkCachedUser(Map<String, Object> sessionAttributes, String username) {
        CachableUserIdentificationInfo cuii = (CachableUserIdentificationInfo) sessionAttributes.get(
                NXAuthConstants.USERIDENT_KEY);
        assertNotNull(cuii);
        assertEquals(username, cuii.getUserInfo().getUserName());
    }

    protected void checkNoCachedUser(Map<String, Object> sessionAttributes) {
        CachableUserIdentificationInfo cuii = (CachableUserIdentificationInfo) sessionAttributes.get(
                NXAuthConstants.USERIDENT_KEY);
        assertNull(cuii);
    }

    /**
     * Computation of the requested page based on request info.
     */
    @Test
    public void testGetRequestedPage() throws Exception {
        // case of a servlet mapped with <url-pattern>*.xhtml</url-pattern>
        doTestGetRequestedPage("foo/bar.xhtml", "/nuxeo/foo/bar.xhtml", "/foo/bar.xhtml", null, null);
        doTestGetRequestedPage("foo/bar.xhtml", "/nuxeo/login.jsp/../foo/bar.xhtml;jsessionid=123?gee=moo",
                "/foo/bar.xhtml", null, "gee=moo");
        // case of a servlet mapped with <url-pattern>/foo/*</url-pattern>
        doTestGetRequestedPage("foo/bar.xhtml", "/nuxeo/foo/bar.xhtml", "/foo", "/bar.xhtml", null);
        doTestGetRequestedPage("foo/bar.xhtml", "/nuxeo/login.jsp/../foo/bar.xhtml;jsessionid=123?gee=moo", "/foo",
                "/bar.xhtml", "gee=moo");
        // index.jsp requested
        doTestGetRequestedPage("ui/index.jsp", "/nuxeo/ui/index.jsp", "/ui/index.jsp", null, null);
        // index.jsp not in the request uri but present in the servlet path (welcome file)
        doTestGetRequestedPage("ui/", "/nuxeo/ui/", "/ui/index.jsp", null, null);
    }

    protected void doTestGetRequestedPage(String expected, String requestURI, String servletPath, String pathInfo,
            String queryString) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        mockRequestURI(request, servletPath, pathInfo, queryString, requestURI);
        assertEquals(expected, NuxeoAuthenticationFilter.getRequestedPage(request));
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

    protected void doTestGetRequestedUrl(String expectedSuffix, String queryString) {
        doTestGetRequestedUrl("foo/bar.xhtml" + expectedSuffix, "/nuxeo/foo/bar.xhtml", "/foo/bar.xhtml", null,
                queryString);
        doTestGetRequestedUrl("foo/bar.xhtml" + expectedSuffix, "/nuxeo/foo/bar.xhtml", "/foo", "/bar.xhtml",
                queryString);
        // here we use info that a servlet container would provide, based on parsing per the servlet spec
        doTestGetRequestedUrl("foo/bar.xhtml" + expectedSuffix, "/nuxeo/login.jsp/../foo/bar.xhtml;jsessionid=123",
                "/foo", "/bar.xhtml", queryString);
        // index.jsp requested
        doTestGetRequestedUrl("ui/index.jsp" + expectedSuffix, "/nuxeo/ui/index.jsp", "/ui/index.jsp", null,
                queryString);
        // index.jsp not in the request uri but present in the servlet path (welcome file)
        doTestGetRequestedUrl("ui/" + expectedSuffix, "/nuxeo/ui/", "/ui/index.jsp", null, queryString);
    }

    protected void doTestGetRequestedUrl(String expected, String requestURI, String servletPath, String pathInfo,
            String queryString) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        mockRequestURI(request, servletPath, pathInfo, queryString, requestURI);
        assertEquals(expected, NuxeoAuthenticationFilter.getRequestedUrl(request));
    }

    /**
     * Auth in session.
     */
    @Test
    public void testAuthCached() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        Map<String, Object> sessionAttributes = mockSessionAttributes(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        mockRequestURI(request, "/foo/bar", "", "");
        // cached identity
        CachableUserIdentificationInfo cuii = new CachableUserIdentificationInfo("bob", "bobpw");
        UserPrincipal principal = new UserPrincipal("bob", null, false, false);
        cuii.setPrincipal(principal);
        sessionAttributes.put(NXAuthConstants.USERIDENT_KEY, cuii);

        filter.doFilter(request, response, chain);

        // chain called as bob
        assertTrue(chain.called);
        assertEquals("bob", chain.principal.getName());
        assertSame(principal, chain.principal);

        // bob auth still cached in session
        checkCachedUser(sessionAttributes, "bob");

        // no login event
        checkNoEvents();
    }

    /**
     * No auth chain configured: no auth.
     */
    @Test
    public void testNoAuthPlugins() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getSession(eq(false))).thenReturn(null);
        mockRequestURI(request, "/foo/bar", "", "");

        filter.doFilter(request, response, chain);

        // chain called, no auth
        assertTrue(chain.called);
        assertNull(chain.principal);
    }

    /**
     * Basic immediate login. Resulting auth saved in session.
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-loginmodule.xml")
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-token.xml")
    public void testAuthPluginToken() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        Map<String, Object> sessionAttributes = mockSessionAttributes(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        mockRequestURI(request, "/foo/bar", "", "");
        // token info
        when(request.getParameter(eq(DUMMY_AUTH_TOKEN_KEY))).thenReturn("bob");

        filter.doFilter(request, response, chain);

        // chain called as bob
        assertTrue(chain.called);
        assertEquals("bob", chain.principal.getName());

        // login success event
        checkEvents(EVENT_LOGIN_SUCCESS);

        // bob auth cached in session
        checkCachedUser(sessionAttributes, "bob");
    }

    /**
     * Basic immediate login. Resulting auth saved in session. Then redirects to previously requested page.
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-loginmodule.xml")
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-token.xml")
    public void testAuthPluginTokenThenRedirectToPage() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        Map<String, Object> sessionAttributes = mockSessionAttributes(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        mockRequestURI(request, "/foo/bar", "", "");
        // token info + redirect page
        when(request.getParameter(eq(DUMMY_AUTH_TOKEN_KEY))).thenReturn("bob");
        when(request.getParameter(eq(REQUESTED_URL))).thenReturn("my/page");

        filter.doFilter(request, response, chain);

        // chain not called, as we redirect
        assertFalse(chain.called);

        // login success event
        checkEvents(EVENT_LOGIN_SUCCESS);

        // bob auth cached in session
        checkCachedUser(sessionAttributes, "bob");

        // redirect was called
        verify(response).sendRedirect(eq("http://localhost:8080/nuxeo/my/page"));
    }

    /**
     * Token auth failing on specific URL not handling prompt. Redirects to hard-coded /login page.
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-loginmodule.xml")
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-token.xml")
    public void testAuthPluginTokenFailedSoRedirectToLoginPage() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        mockSessionAttributes(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        // request specific page configured with specific chain without prompt
        mockRequestURI(request, "/no/prompt", "", "");
        // no token provided
        // record output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        @SuppressWarnings("resource")
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, UTF_8), true);
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, chain);

        // chain not called, as we redirect
        assertFalse(chain.called);

        // no login event
        checkNoEvents();

        // redirecting to /login
        verify(response).setStatus(SC_UNAUTHORIZED);
        verify(response).addHeader(eq("Location"), eq("http://localhost:8080/nuxeo/" + LOGIN_PAGE));
    }

    /**
     * No auth but redirect to plugin login page.
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-loginmodule.xml")
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-form.xml")
    public void testAuthPluginFormRedirectToLoginPage() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        Map<String, Object> sessionAttributes = mockSessionAttributes(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        // mystart/ is defined as a start url in the XML config
        mockRequestURI(request, "/mystart/foo", "", "");
        // record output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        @SuppressWarnings("resource")
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, UTF_8), true);
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, chain);

        // chain not called, as we redirect instead
        assertFalse(chain.called);

        // no auth
        checkNoCachedUser(sessionAttributes);

        // no login event
        checkNoEvents();

        // unauthorized
        verify(response).setStatus(eq(SC_UNAUTHORIZED));
        // a redirect is done through an HTML page containing JavaScript code
        verify(response).setContentType(eq("text/html;charset=UTF-8"));
        // check that the redirect is to our dummy login page (defined in the auth plugin)
        writer.flush();
        String entity = out.toString(UTF_8);
        assertTrue(entity, entity.contains(
                "window.location = 'http://localhost:8080/nuxeo/dummy_login.jsp?requestedUrl=mystart/foo';"));
    }

    /**
     * Auth in session and request to hard-coded /login, redirects to plugin login page.
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-loginmodule.xml")
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-form.xml")
    public void testAuthPluginFormReLogin() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        Map<String, Object> sessionAttributes = mockSessionAttributes(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        mockRequestURI(request, "/" + LOGIN_PAGE, "", "");
        // cached identity
        CachableUserIdentificationInfo cuii = new CachableUserIdentificationInfo("bob", "bobpw");
        UserPrincipal principal = new UserPrincipal("bob", null, false, false);
        cuii.setPrincipal(principal);
        sessionAttributes.put(NXAuthConstants.USERIDENT_KEY, cuii);
        // record output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        @SuppressWarnings("resource")
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, UTF_8), true);
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, chain);

        // chain not called, as we redirect instead
        assertFalse(chain.called);

        // cached auth has been removed
        // TODO checkNoCachedUser(sessionAttributes);

        // unauthorized
        verify(response).setStatus(eq(SC_UNAUTHORIZED));
        // a redirect is done through an HTML page containing JavaScript code
        verify(response).setContentType(eq("text/html;charset=UTF-8"));
        // check that the redirect is to our dummy login page (defined in the auth plugin)
        writer.flush();
        String entity = out.toString(UTF_8);
        assertTrue(entity, entity.contains("window.location = 'http://localhost:8080/nuxeo/dummy_login.jsp';"));
    }

    /**
     * Login form display. Does not need authentication
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-loginmodule.xml")
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-form.xml")
    public void testAuthPluginFormGet() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        mockRequestURI(request, "/dummy_form_login.jsp", "", "");

        filter.doFilter(request, response, chain);

        // chain called
        assertTrue(chain.called);
        // but not logged in
        assertNull(chain.principal);
    }

    /**
     * Login from form auth. Resulting auth saved in session, redirects to requested page.
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-loginmodule.xml")
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-form.xml")
    public void testAuthPluginFormSubmit() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        Map<String, Object> sessionAttributes = mockSessionAttributes(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        mockRequestURI(request, "/doesnotmatter", "", "requestedUrl=mystart/foo");
        // login info
        when(request.getParameter(eq(DUMMY_AUTH_FORM_USERNAME_KEY))).thenReturn("bob");
        when(request.getParameter(eq(DUMMY_AUTH_FORM_PASSWORD_KEY))).thenReturn("bob");
        when(request.getParameter(eq(REQUESTED_URL))).thenReturn("mystart/foo");

        filter.doFilter(request, response, chain);

        // chain not called, as we redirect
        assertFalse(chain.called);

        // login success event
        checkEvents(EVENT_LOGIN_SUCCESS);

        // bob auth cached in session
        checkCachedUser(sessionAttributes, "bob");

        // redirect was called
        verify(response).sendRedirect(eq("http://localhost:8080/nuxeo/mystart/foo"));
    }

    /**
     * Auth in session and /logout request. Removes session auth. Redirects to startup page.
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-loginmodule.xml")
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-form.xml")
    public void testAuthPluginFormLogout() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        Map<String, Object> sessionAttributes = mockSessionAttributes(session);
        initAuthPluginFormLogoutRequest(request, session, sessionAttributes);

        filter.doFilter(request, response, chain);

        // chain not called, as we redirect instead
        assertFalse(chain.called);

        // logout event
        checkEvents(EVENT_LOGOUT);

        // cached auth has been removed
        checkNoCachedUser(sessionAttributes);

        // redirect was called. home.html is the default LoginScreenHelper startup page
        verify(response).sendRedirect(eq("http://localhost:8080/nuxeo/home.html"));
    }

    protected void initAuthPluginFormLogoutRequest(HttpServletRequest request, HttpSession session,
            Map<String, Object> sessionAttributes) throws LoginException {
        when(request.getSession(anyBoolean())).thenReturn(session);
        mockRequestURI(request, "/" + LOGOUT_PAGE, "", "");
        // cached identity
        CachableUserIdentificationInfo cuii = new CachableUserIdentificationInfo("bob", "bobpw");
        cuii.getUserInfo().setAuthPluginName("DUMMY_AUTH_FORM");
        UserPrincipal principal = new UserPrincipal("bob", null, false, false);
        cuii.setPrincipal(principal);
        LoginContext loginContext = mock(LoginContext.class);
        doNothing().when(loginContext).logout();
        cuii.setLoginContext(loginContext);
        sessionAttributes.put(NXAuthConstants.USERIDENT_KEY, cuii);
    }

    /**
     * Auth in session and /logout request. Removes session auth. Redirects to callback URL.
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-loginmodule.xml")
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-form.xml")
    public void testAuthPluginFormLogoutCallbackURL() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        Map<String, Object> sessionAttributes = mockSessionAttributes(session);
        initAuthPluginFormLogoutRequest(request, session, sessionAttributes);

        // set the callbackURL parameter to a valid URL
        when(request.getParameter(eq(CALLBACK_URL_PARAMETER))).thenReturn("http://localhost:8080/nuxeo/redirect");

        filter.doFilter(request, response, chain);

        // chain not called, as we redirect instead
        assertFalse(chain.called);

        // logout event
        checkEvents(EVENT_LOGOUT);

        // cached auth has been removed
        checkNoCachedUser(sessionAttributes);

        // redirect was called and callback URL was valid
        verify(response).sendRedirect(eq("http://localhost:8080/nuxeo/redirect"));
    }

    /**
     * Auth in session and /logout request. Removes session auth. Invalid callback URL. Redirects to startup page.
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-loginmodule.xml")
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-form.xml")
    public void testAuthPluginFormLogoutInvalidCallbackURL() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        Map<String, Object> sessionAttributes = mockSessionAttributes(session);
        initAuthPluginFormLogoutRequest(request, session, sessionAttributes);

        // set the callbackURL parameter to an invalid URL
        when(request.getParameter(eq(CALLBACK_URL_PARAMETER))).thenReturn("http://example.com/redirect");

        filter.doFilter(request, response, chain);

        // chain not called, as we redirect instead
        assertFalse(chain.called);

        // logout event
        checkEvents(EVENT_LOGOUT);

        // cached auth has been removed
        checkNoCachedUser(sessionAttributes);

        // redirect was called. home.html is the default LoginScreenHelper startup page
        verify(response).sendRedirect(eq("http://localhost:8080/nuxeo/home.html"));
    }

    /**
     * No auth, no ticket, redirects to SSO login page.
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-loginmodule.xml")
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-sso.xml")
    public void testAuthPluginSSORedirectToSSOLoginPage() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        mockSessionAttributes(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        // mystart/ is defined as a start url in the XML config
        mockRequestURI(request, "/mystart/foo", "", "bar=baz");
        // no ticket provided
        // record output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        @SuppressWarnings("resource")
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, UTF_8), true);
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, chain);

        // chain not called, as we redirect
        assertFalse(chain.called);

        // no login event
        checkNoEvents();

        // unauthorized
        verify(response).setStatus(eq(SC_UNAUTHORIZED));
        // a redirect is done through an HTML page containing JavaScript code
        verify(response).setContentType(eq("text/html;charset=UTF-8"));
        // check that the redirect is to the SSO login page
        writer.flush();
        String entity = out.toString(UTF_8);
        String expectedRedirect = URLEncoder.encode("http://localhost:8080//nuxeo/mystart/foo?bar=baz", "UTF-8");
        assertTrue(entity,
                entity.contains("window.location = 'http://sso.example.com/login?redirect=" + expectedRedirect + "';"));
    }

    /**
     * SSO redirects to page, passing a proper ticket. Resulting auth saved in session.
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-loginmodule.xml")
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-sso.xml")
    public void testAuthPluginSSOWithTicket() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        Map<String, Object> sessionAttributes = mockSessionAttributes(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        // mystart/ is defined as a start url in the XML config
        mockRequestURI(request, "/mystart/foo", "", "ticket=bob");
        // ticket info
        when(request.getParameter(eq(DUMMY_SSO_TICKET))).thenReturn("bob");
        // record output
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        @SuppressWarnings("resource")
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out, UTF_8), true);
        when(response.getWriter()).thenReturn(writer);

        filter.doFilter(request, response, chain);

        // chain called as bob
        assertTrue(chain.called);
        assertEquals("bob", chain.principal.getName());

        // login success event
        checkEvents(EVENT_LOGIN_SUCCESS);

        // bob auth cached in session
        checkCachedUser(sessionAttributes, "bob");
    }

    /**
     * Auth in session and /logout request. Removes session auth. Redirects to SSO logout page.
     */
    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-loginmodule.xml")
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-sso.xml")
    public void testAuthPluginSSOLogout() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        Map<String, Object> sessionAttributes = mockSessionAttributes(session);
        when(request.getSession(anyBoolean())).thenReturn(session);
        mockRequestURI(request, "/" + LOGOUT_PAGE, "", "");
        // cached identity
        CachableUserIdentificationInfo cuii = new CachableUserIdentificationInfo("bob", "bobpw");
        cuii.getUserInfo().setAuthPluginName("DUMMY_AUTH_SSO");
        UserPrincipal principal = new UserPrincipal("bob", null, false, false);
        cuii.setPrincipal(principal);
        LoginContext loginContext = mock(LoginContext.class);
        doNothing().when(loginContext).logout();
        cuii.setLoginContext(loginContext);
        sessionAttributes.put(NXAuthConstants.USERIDENT_KEY, cuii);
        // the callbackURL parameter should be ignored as the SSO do a redirect
        when(request.getParameter(eq(CALLBACK_URL_PARAMETER))).thenReturn("http://example.com/redirected");

        filter.doFilter(request, response, chain);

        // chain not called, as we redirect instead
        assertFalse(chain.called);

        // logout event
        checkEvents(EVENT_LOGOUT);

        // cached auth has been removed
        checkNoCachedUser(sessionAttributes);

        // redirect to the SSO logout page
        verify(response).sendRedirect(eq("http://sso.example.com/logout"));
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-loginmodule.xml")
    @Deploy("org.nuxeo.ecm.platform.web.common.test:OSGI-INF/test-authchain-dummy-form.xml")
    public void testCallbackURL() {
        String baseURL = "http://localhost:8080/nuxeo/";
        assertFalse(filter.isCallbackURLValid(null, baseURL));
        assertFalse(filter.isCallbackURLValid("http://foo.bar/nuxeo/redirect", null));
        assertTrue(filter.isCallbackURLValid("http://localhost:8080/nuxeo/redirect", baseURL));
        assertFalse(filter.isCallbackURLValid("https://example.com/redirect", baseURL));
        assertTrue(filter.isCallbackURLValid("nuxeo://redirect", baseURL));
        assertTrue(filter.isCallbackURLValid("nxdrive://redirect", baseURL));
        assertFalse(filter.isCallbackURLValid("foo://", baseURL));

        // wrong callback URL => redirects to startup page
        assertEquals("http://localhost:8080/nuxeo/home.html",
                filter.getLogoutRedirectURL("https://example.com/redirect", baseURL, null));
        assertEquals("http://localhost:8080/nuxeo/home.html", filter.getLogoutRedirectURL(null, baseURL, null));
        assertEquals("http://localhost:8080/nuxeo/home.html",
                filter.getLogoutRedirectURL("foo://redirect", baseURL, null));
        // OK
        assertEquals("http://localhost:8080/nuxeo/redirect",
                filter.getLogoutRedirectURL("http://localhost:8080/nuxeo/redirect", baseURL, null));
        assertEquals("nuxeo://redirect", filter.getLogoutRedirectURL("nuxeo://redirect", baseURL, null));
        assertEquals("nxdrive://redirect", filter.getLogoutRedirectURL("nxdrive://redirect", baseURL, null));
    }

}
