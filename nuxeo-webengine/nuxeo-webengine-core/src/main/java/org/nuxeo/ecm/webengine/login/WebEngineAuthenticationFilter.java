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
import java.security.Principal;

import javax.security.auth.Subject;
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
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.runtime.api.Framework;

import sun.misc.BASE64Decoder;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineAuthenticationFilter implements Filter {

    protected UserManager mgr;
    //used to propagate login info on server applications
    protected AuthenticationPropagator propagator;
    protected String exclude; // exclude this path from filtering - can be used to optimize static resource requests

    public void destroy() {
        mgr = null;
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest)request;
        HttpServletResponse resp = (HttpServletResponse)response;
        if (exclude != null && req.getServletPath().startsWith(exclude)) { // avoid authentication on static resources
            chain.doFilter(request, response);
            return;
        }
        if (mgr == null) {
            mgr = Framework.getLocalService(UserManager.class);
            if (mgr == null) {
                throw new ServletException("Could not find the UserManager service");
            }
        }
        HttpSession session = req.getSession(true);
        UserSession userSession = null; // the new identity after  a login / logout
        String[] auth = getClientAuthorizationTokens(req);
        try {
            if (auth != null) { // a login/logout request
                if (auth[0] == null) { // a logout request
                    userSession = UserSession.getAnonymousSession(mgr);
                } else {
                    userSession = authenticate(auth[0], auth[1]);
                    if (userSession == null) {
                        clientAuthenticationError(req, resp);
                        return;
                    }
                }
            } else {
                auth = getBasicAuthorizationTokens(req);
                if (auth != null) { // Basic HTTP login
                    userSession = authenticate(auth[0], auth[1]);
                    if (userSession == null) {
                        basicAuthenticationError(req, resp);
                        return;
                    }
                }
            }
            if (userSession != null) { // was a login or logout - switch to the new user
                session.invalidate(); // this will dispose current user session
                session = req.getSession(true);
                UserSession.setCurrentSession(session, userSession);
            } else { // not a login / logout request
                userSession = UserSession.getCurrentSession(session);
                if (userSession == null) {
                    userSession = UserSession.getAnonymousSession(mgr);
                    UserSession.setCurrentSession(session, userSession);
                }
            }
            // propagate session if needed - this can be used to initialize an application server EJB context
            propagate(userSession);
            WebEngineRequestWrapper reqw = new WebEngineRequestWrapper(req, userSession);
            chain.doFilter(reqw, response);
        } catch (Exception e) {
//            throw new ServletException("Failed to perform authentication", e);
            e.printStackTrace();
            // do nothing
        } finally {
            if (propagator == null) { // remove login information
                ClientLoginModule.getThreadLocalLogin().clear();
            }
        }
    }

    protected UserSession authenticate(String username, String password) throws ClientException {
        if (mgr.checkUsernamePassword(username, password)) {
            Principal principal = mgr.getPrincipal(username);
            if (principal != null) {
                return  new UserSession(principal, password);
            }
        }
        return null;
    }


    protected void propagate(UserSession userSession) {
        // if any propagator was registered propagate now the authentication
        if (propagator != null) {
            propagator.propagate(userSession);
        } else { //default propagator - initialize the core LocalSession
            Principal principal = userSession.getPrincipal();
            Object credentials = userSession.getCredentials();
            Subject subject = userSession.getSubject();
            ClientLoginModule.getThreadLocalLogin().push(principal, credentials, subject);
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        String klass = filterConfig.getInitParameter("propagator");
        exclude = filterConfig.getInitParameter("exclude");
        if (klass != null) {
           try {
               propagator = (AuthenticationPropagator)Class.forName(klass).newInstance();
           } catch (Exception e) {
               throw new ServletException("Failed to initialize authentication propagator: "+klass, e);
           }
        }
    }

    public void clientAuthenticationError(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        // ajax request
        if (request.getParameter("caller") != null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authentication Failed");
        } else { // normal request
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.sendRedirect(request.getRequestURL().toString()+"?failed=true");
        }
    }

    public void basicAuthenticationError(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
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

    /**
     * If a request contains the "nuxeo@@login" parameter a login will be performed using
     * 'userid' and 'password' parameters. If the 'userid' is null (not specified by the client),
     * a logout will be performed.
     *
     * @param httpRequest
     * @return
     */
    public String[] getClientAuthorizationTokens(
            HttpServletRequest httpRequest) {
        if (httpRequest.getParameter("nuxeo_login") != null) {
            String userId = httpRequest.getParameter("userid");
            String passwd = httpRequest.getParameter("password");
            return new String[] { userId, passwd };
        }
        return null;
    }

}
