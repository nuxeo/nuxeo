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
package org.nuxeo.ecm.platform.ui.web.auth;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

import java.io.IOException;
import java.util.Base64;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.local.LoginStack;

/**
 * Authentication Filter that does not check the password and trusts the user name.
 * <p>
 * Replaces the standard NuxeoAuthenticationFilter.
 *
 * @since 10.3
 */
public class TrustingNuxeoAuthenticationFilter implements Filter {

    protected static final String BASIC_SP = "Basic ";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        // get principal
        String username = getUsername(request);
        if (username == null) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }
        // login
        NuxeoPrincipal principal = new UserPrincipal(username, null, false, isAdministrator(username));
        LoginStack loginStack = ClientLoginModule.getThreadLocalLogin();
        loginStack.push(principal, null, null);
        try {
            // wrap
            request = new NuxeoSecuredRequestWrapper(request, principal);
            // chain
            chain.doFilter(request, servletResponse);
        } finally {
            loginStack.pop();
        }
    }

    @Override
    public void destroy() {
    }

    /**
     * Extracts username from request Authorization header.
     */
    protected String getUsername(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION);
        if (header == null) {
            return null;
        }
        if (!header.startsWith(BASIC_SP)) {
            return null;
        }
        String auth = new String(Base64.getDecoder().decode(header.substring(BASIC_SP.length())), UTF_8);
        int i = auth.indexOf(':');
        if (i < 0) {
            return null;
        } else {
            return auth.substring(0, i);
        }
    }

    /**
     * If its name starts with "admin", make the principal an Administrator.
     */
    protected boolean isAdministrator(String username) {
        return username.startsWith("admin");
    }

}
