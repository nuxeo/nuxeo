/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *     Academie de Rennes - proxy CAS support
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.auth.simple;

import java.util.Collections;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.jboss.seam.mock.MockFilterConfig;
import org.jboss.seam.mock.MockHttpSession;
import org.jboss.seam.mock.MockServletContext;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.NXRuntimeTestCase;

import sun.misc.BASE64Encoder;

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

    protected void setLoginPasswordInHeader(String login, String password,
            MockHttpRequest request) {
        BASE64Encoder encoder = new BASE64Encoder();
        String b64userpassword = encoder.encodeBuffer((login + ":" + password).getBytes());
        request.setHeaderParam("authorization",
                new String[] { "basic " + b64userpassword, });
    }

    protected void initStandardPlugins() throws Exception {
        // Mock the usermanager service (we don't want to pull all nuxeo framework)
        // Needed by Anonymous
        deployContrib("org.nuxeo.ecm.platform.login.cas2.test",
                "OSGI-INF/mock-usermanager-framework.xml");
        // Mock the event producer (we don't want to pull all nuxeo framework)
        // NuxeoAuthenticationFilter sends events
        deployContrib("org.nuxeo.ecm.platform.login.cas2.test",
        "OSGI-INF/mock-event-framework.xml");

        
        deployBundle("org.nuxeo.ecm.platform.login");
        deployBundle("org.nuxeo.ecm.platform.web.common");
    }

}
