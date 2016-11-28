/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.Collections;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.commons.codec.binary.Base64;
import org.jboss.seam.mock.MockFilterConfig;
import org.jboss.seam.mock.MockHttpSession;
import org.jboss.seam.mock.MockServletContext;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

/**
 * @author Benjamin JALON
 */
public abstract class AbstractAuthenticator extends NXRuntimeTestCase {

    protected PluggableAuthenticationService authService;

    protected MockHttpResponse response;

    protected MockHttpRequest request;

    protected List<Cookie> cookieList;

    protected NuxeoAuthenticationFilter naf;

    protected FilterChain chain;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        initStandardPlugins();
    }

    protected PluggableAuthenticationService getAuthService() {
        if (authService == null) {
            authService = (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                    PluggableAuthenticationService.NAME);
        }
        return authService;
    }

    protected void initRequest() throws ServletException {
        // Cookie[] cookieArray = cookieList.toArray(new Cookie[] {});

        naf = new NuxeoAuthenticationFilter();

        ServletContext servletContext = new MockServletContext();
        MockHttpSession session = new MockHttpSession(new MockServletContext());
        FilterConfig config = new MockFilterConfig(servletContext);

        chain = new MockFilterChain();

        naf.init(config);

        request = new MockHttpRequest(session);
        response = new MockHttpResponse();
    }

    protected void initCookieList() {
        cookieList = Collections.emptyList();
    }

    protected void setLoginPasswordInHeader(String login, String password, MockHttpRequest request) {
        String b64userpassword = Base64.encodeBase64String((login + ":" + password).getBytes());
        request.setHeaderParam("authorization", new String[] { "basic " + b64userpassword, });
    }

    protected void initStandardPlugins() throws Exception {
        // Mock the usermanager service (we don't want to pull all nuxeo framework)
        // Needed by Anonymous
        deployContrib("org.nuxeo.ecm.platform.login.cas2.test", "OSGI-INF/mock-usermanager-framework.xml");
        // Mock the event producer (we don't want to pull all nuxeo framework)
        // NuxeoAuthenticationFilter sends events
        deployContrib("org.nuxeo.ecm.platform.login.cas2.test", "OSGI-INF/mock-event-framework.xml");

        deployBundle("org.nuxeo.ecm.platform.login");
        deployBundle("org.nuxeo.ecm.platform.web.common");
    }

}
