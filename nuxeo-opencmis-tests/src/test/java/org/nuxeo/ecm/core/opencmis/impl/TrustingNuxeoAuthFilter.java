/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.opencmis.impl;

import java.io.IOException;
import java.security.Principal;

import javax.security.auth.login.LoginContext;
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
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoSecuredRequestWrapper;
import org.nuxeo.ecm.platform.usermanager.NuxeoPrincipalImpl;
import org.nuxeo.runtime.api.Framework;

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
        try {
            LoginContext loginContext = Framework.loginAsUser(username);
            try {
                Principal principal = (Principal) loginContext.getSubject().getPrincipals().toArray()[0];
                maybeMakeAdministrator(principal);
                // propagate
                ClientLoginModule.getThreadLocalLogin().push(principal, null, loginContext.getSubject());
                // wrap
                request = new NuxeoSecuredRequestWrapper(httpRequest, principal);
                // chain
                chain.doFilter(request, response);
            } finally {
                loginContext.logout();
                ClientLoginModule.getThreadLocalLogin().pop();
            }
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
