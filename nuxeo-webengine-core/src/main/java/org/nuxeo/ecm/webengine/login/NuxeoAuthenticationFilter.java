/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.login;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.DefaultWebContext;
import org.nuxeo.runtime.api.Framework;

import sun.misc.BASE64Decoder;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class NuxeoAuthenticationFilter implements Filter {

    protected UserManager mgr;

    public void destroy() {
        mgr = null;
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (mgr == null) {
            mgr = Framework.getLocalService(UserManager.class);
            if (mgr == null) {
                throw new ServletException("Could not find the UserManager service");
            }
        }
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse)response;
        HttpSession session = req.getSession(true);
        NuxeoPrincipal currentPrincipal = (NuxeoPrincipal)session.getAttribute("nuxeo.principal");
        NuxeoPrincipal principal = null;
        String[] auth = getClientAuthorizationTokens(req);
        try {
            if (auth != null) { // a login request
                if (mgr.checkUsernamePassword(auth[0], auth[1])) {
                    principal = mgr.getPrincipal(auth[0]);
                }
                if (principal == null) {
                    clientAuthenticationError(req, resp);
                    return;
                }
            } else {
                auth = getBasicAuthorizationTokens(req);
                if (auth != null) { // Basic HTTP login
                    if (mgr.checkUsernamePassword(auth[0], auth[1])) {
                        principal = mgr.getPrincipal(auth[0]);
                    }
                    if (principal == null) {
                        basicAuthenticationError(req, resp);
                        return;
                    }
                } else if (currentPrincipal == null) { // anonymous login
                    String userId = mgr.getAnonymousUserId();
                    if (userId != null) {
                        principal = mgr.getPrincipal(userId);
                    }
                }
            }
            if (principal != null) {
                // remove the existing core session if any to force a new session creation based on the new principal
                session.removeAttribute(DefaultWebContext.CORESESSION_KEY);
                session.setAttribute("nuxeo.principal", principal);
                currentPrincipal = principal;
            }
            if (currentPrincipal != null) { // if a current principal is defined wrap the request
                request = new NuxeoSecuredRequestWrapper(req, currentPrincipal);
            }
        } catch (ClientException e) {
            throw new ServletException("Failed to perform authentication", e);
        }
        chain.doFilter(request, response);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void clientAuthenticationError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication Failed");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.sendRedirect(request.getRequestURL().toString()+"?failed=true");
    }

    public void basicAuthenticationError(HttpServletRequest request, HttpServletResponse response) throws IOException {
      //***We weren't sent a valid username/password in the header, so ask for one***
      response.setHeader("WWW-Authenticate","Basic realm=\"WebEngine Authentication\"");
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication Failed");
    }

    public void sendError(HttpServletResponse response, Throwable e) throws IOException {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        PrintWriter w = response.getWriter();
        w.append("<html><head><title>Authorization Failed</title></head><body><pre>");
        e.printStackTrace(w);
        w.append("</pre></body></html>");
    }

    public String[] getBasicAuthorizationTokens(
            HttpServletRequest httpRequest) throws IOException {
        String auth = httpRequest.getHeader("Authorization");
        if (auth != null && auth.toLowerCase().startsWith("basic")) {
            int idx = auth.indexOf(" ");
            String b64userpassword = auth.substring(idx + 1);
            BASE64Decoder decoder = new BASE64Decoder();
            byte[] clearUp = decoder.decodeBuffer(b64userpassword);
            String userpassword = new String(clearUp);
            String username = userpassword.split(":")[0];
            String password = userpassword.split(":")[1];
            return new String[] {username, password};
        }
        return null;
    }

    public String[] getClientAuthorizationTokens(
            HttpServletRequest httpRequest) throws IOException {
        if (httpRequest.getParameter("nuxeo@@login") != null) {
            String userId = httpRequest.getParameter("userid");
            String passwd = httpRequest.getParameter("password");
            return new String[] { userId, passwd };
        }
        return null;
    }

}
