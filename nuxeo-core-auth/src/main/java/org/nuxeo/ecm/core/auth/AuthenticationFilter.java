/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.core.auth;

import java.io.IOException;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.common.utils.Base64;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Filter using the {@link SimpleLoginModule} to authenticate a request.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class AuthenticationFilter implements Filter {

    public final static String DEFAULT_SECURITY_DOMAIN = "nuxeo-client-login";

    protected String domain = DEFAULT_SECURITY_DOMAIN;
    protected boolean startTx = true;

    protected boolean autoPrompt = true;
    protected String realmName = "Nuxeo";



    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String v = filterConfig.getInitParameter("securityDomain");
        if (v != null) {
            domain = v;
        }
        v = filterConfig.getInitParameter("startTx");
        if (v != null && Boolean.parseBoolean(v)) {
            startTx = true;
        }
        v = filterConfig.getInitParameter("realmName");
        if (v != null) {
            realmName = v;
        }
    }

    protected void internalDoFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
        LoginContext lc = null;
        String[] login = retrieveBasicLogin(req);
        if (login != null) {
            try {
                lc = Framework.login(login[0], login[1]);
            } catch (LoginException e) {
                // TODO
                e.printStackTrace();
            }
        }

        if (lc == null) { // login failed

            String s = "Basic realm=\""+realmName+"\"";
            resp.setHeader("WWW-Authenticate", s);
            resp.setStatus(401);
            return;
        }

        try {
            startTx(req);
            chain.doFilter(req, resp);
        } finally {
            closeTx(req);
            ClientLoginModule.getThreadLocalLogin().clear();
            if (lc != null) {
                try { lc.logout(); } catch (Exception e) {}
            }
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (request instanceof HttpServletRequest == false) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse)response;

//        Thread t = Thread.currentThread();
//        ClassLoader oldcl = t.getContextClassLoader();
//        t.setContextClassLoader(getClass().getClassLoader());
//        try {
        internalDoFilter(req, resp, chain);
//        } finally {
//            t.setContextClassLoader(oldcl);
//        }
    }

    @Override
    public void destroy() {
    }


    protected boolean requireTx(HttpServletRequest req) {
        //TODO check path filters
        return startTx && !TransactionHelper.isTransactionActive();
    }

    public void startTx(HttpServletRequest req) {
        if (requireTx(req))  {
            if (TransactionHelper.startTransaction()) {
                req.setAttribute(getClass().getName()+".txStarted", "true");
            }
            // log.warn("tx started for " + req.getPathInfo());
        }
    }

    public void closeTx(HttpServletRequest req) {
        if ("true".equals(req.getAttribute(getClass().getName()+".txStarted"))) {
            TransactionHelper.commitOrRollbackTransaction();
            // log.warn("tx closed for " + req.getPathInfo());
        }
    }


    public String[] retrieveBasicLogin(HttpServletRequest httpRequest) {
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

}
