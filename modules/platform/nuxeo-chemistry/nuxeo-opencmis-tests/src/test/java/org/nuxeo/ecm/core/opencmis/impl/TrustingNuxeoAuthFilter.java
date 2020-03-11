/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.opencmis.impl;

import java.io.IOException;
import java.security.Principal;

import javax.security.auth.login.LoginException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.chemistry.opencmis.commons.server.CallContext;
import org.apache.chemistry.opencmis.server.shared.BasicAuthCallContextHandler;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoSecuredRequestWrapper;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.NuxeoLoginContext;

/**
 * Auth Filter that does not check the password and trusts the user name.
 * <p>
 * Replace the standard NuxeoAuthenticationFilter.
 */
public class TrustingNuxeoAuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        // get principal
        String username = getUserName(httpRequest);
        if (username == null) {
            chain.doFilter(request, response);
            return;
        }
        // login
        try (NuxeoLoginContext loginContext = Framework.loginUser(username)) {
            Principal principal = NuxeoPrincipal.getCurrent();
            maybeMakeAdministrator(principal);
            // wrap
            request = new NuxeoSecuredRequestWrapper(httpRequest, principal);
            // chain
            chain.doFilter(request, response);
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void destroy() {
    }

    protected String getUserName(HttpServletRequest request) {
        BasicAuthCallContextHandler ba = new BasicAuthCallContextHandler();
        return ba.getCallContextMap(request).get(CallContext.USERNAME);
    }

    /**
     * If its name starts with "admin", makes the principal an Administrator.
     */
    protected static void maybeMakeAdministrator(Principal principal) {
        if (principal.getName().toLowerCase().startsWith("admin") && principal instanceof NuxeoPrincipalImpl) {
            ((NuxeoPrincipalImpl) principal).isAdministrator = true;
        }
    }

}
