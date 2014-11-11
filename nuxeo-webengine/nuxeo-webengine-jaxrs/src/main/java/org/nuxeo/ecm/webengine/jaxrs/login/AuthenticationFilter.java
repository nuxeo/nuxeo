/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.login;

import java.io.IOException;
import java.security.Principal;
import java.util.Set;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.common.utils.Base64;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.webengine.jaxrs.HttpFilter;
import org.nuxeo.runtime.api.Framework;

/**
 * Filter using the {@link SimpleLoginModule} to authenticate a request.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AuthenticationFilter extends HttpFilter {

    public static final String DEFAULT_SECURITY_DOMAIN = "nuxeo-client-login";

    protected String domain = DEFAULT_SECURITY_DOMAIN;

    protected boolean autoPrompt = true;
    protected String realmName = "Nuxeo";


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String v = filterConfig.getInitParameter("securityDomain");
        if (v != null) {
            domain = v;
        }
        v = filterConfig.getInitParameter("realmName");
        if (v != null) {
            realmName = v;
        }
    }

    @Override
    public void run(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        LoginContext lc = null;
        if (request.getUserPrincipal() == null) {
            try {
                lc = doLogin(request, response);
                request = wrapRequest(request, lc);
            } catch (LoginException e) {
                // login failed
                handleLoginFailure(request, response, e);
                return;
            }
        }

        try {
            chain.doFilter(request, response);
        } finally {
            ClientLoginModule.getThreadLocalLogin().clear();
            if (lc != null) {
                // a null lc may indicate an anonymous login
                try { lc.logout(); } catch (Exception e) {}
            }
        }
    }

    @Override
    public void destroy() {
    }

    protected String[] retrieveBasicLogin(HttpServletRequest httpRequest) {
        String auth = httpRequest.getHeader("authorization");
        if (auth != null && auth.toLowerCase().startsWith("basic")) {
            int idx = auth.indexOf(' ');
            String b64userpassword = auth.substring(idx + 1);
            byte[] clearUp = Base64.decode(b64userpassword);
            String userpassword = new String(clearUp);
            String[] up = StringUtils.split(userpassword, ':', false);
            if (up.length != 2) {
                return null;
            }
            return up;
        }
        return null;
    }

    protected LoginContext doLogin(HttpServletRequest request, HttpServletResponse response) throws LoginException {
        String[] login = retrieveBasicLogin(request);
        if (login != null) {
            return Framework.login(login[0], login[1]);
        }
        // TODO no login provided - use anonymous ?
        // for now no anonymous user supported - we require a login
        throw new LoginException("User must login");
        //return null;
    }

    protected void handleLoginFailure(HttpServletRequest request, HttpServletResponse response, LoginException e) {
        String s = "Basic realm=\""+realmName+"\"";
        response.setHeader("WWW-Authenticate", s);
        response.setStatus(401);
    }

    protected HttpServletRequest wrapRequest(HttpServletRequest request, LoginContext lc) {
        Set<Principal> set = lc.getSubject().getPrincipals();
        if (!set.isEmpty()) {
            final Principal principal = set.iterator().next();
        return new HttpServletRequestWrapper(request) {
            @Override
            public Principal getUserPrincipal() {
                return principal;
            }
        };
        }
        return request;
    }
}
