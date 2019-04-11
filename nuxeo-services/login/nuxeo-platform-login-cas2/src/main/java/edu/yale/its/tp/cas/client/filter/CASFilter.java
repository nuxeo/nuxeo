/*
 *  (C) Copyright 2000-2003 Yale University. All rights reserved.
 *
 *  THIS SOFTWARE IS PROVIDED "AS IS," AND ANY EXPRESS OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, ARE EXPRESSLY
 *  DISCLAIMED. IN NO EVENT SHALL YALE UNIVERSITY OR ITS EMPLOYEES BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED, THE COSTS OF
 *  PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA OR
 *  PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 *  LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *  NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED IN ADVANCE OF THE POSSIBILITY OF SUCH
 *  DAMAGE.
 *
 *  Redistribution and use of this software in source or binary forms,
 *  with or without modification, are permitted, provided that the
 *  following conditions are met:
 *
 *  1. Any redistribution must include the above copyright notice and
 *  disclaimer and this list of conditions in any related documentation
 *  and, if feasible, in the redistributed software.
 *
 *  2. Any redistribution must include the acknowledgment, "This product
 *  includes software developed by Yale University," in any related
 *  documentation and, if feasible, in the redistributed software.
 *
 *  3. The names "Yale" and "Yale University" must not be used to endorse
 *  or promote products derived from this software.
 */

package edu.yale.its.tp.cas.client.filter;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import edu.yale.its.tp.cas.client.*;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 * <p>
 * Protects web-accessible resources with CAS.
 * </p>
 * <p>
 * The following filter initialization parameters are declared in <code>web.xml</code>:
 * </p>
 * <ul>
 * <li><code>edu.yale.its.tp.cas.client.filter.loginUrl</code>: URL to login page on CAS server. (Required)</li>
 * <li><code>edu.yale.its.tp.cas.client.filter.validateUrl</code>: URL to validation URL on CAS server. (Required)</li>
 * <li><code>edu.yale.its.tp.cas.client.filter.serviceUrl</code>: URL of this service. (Required if
 * <code>serverName</code> is not specified)</li>
 * <li><code>edu.yale.its.tp.cas.client.filter.serverName</code>: full hostname with port number (e.g.
 * <code>www.foo.com:8080</code>). Port number isn't required if it is standard (80 for HTTP, 443 for HTTPS). (Required
 * if <code>serviceUrl</code> is not specified)</li>
 * <li><code>edu.yale.its.tp.cas.client.filter.authorizedProxy</code>: whitespace-delimited list of valid proxies
 * through which authentication may have proceeded. One one proxy must match. (Optional. If nothing is specified, the
 * filter will only accept service tickets &#150; not proxy tickets.)</li>
 * <li><code>edu.yale.its.tp.cas.client.filter.renew</code>: value of CAS "renew" parameter. Bypasses single sign-on and
 * requires user to provide CAS with his/her credentials again. (Optional. If nothing is specified, this defaults to
 * false.)</li>
 * <li><code>edu.yale.its.tp.cas.client.filter.wrapRequest</code>: wrap the <code>HttpServletRequest</code> object,
 * overriding the <code>getRemoteUser()</code> method. When set to "true", <code>request.getRemoteUser()</code> will
 * return the username of the currently logged-in CAS user. (Optional. If nothing is specified, this defaults to false.)
 * </li>
 * </ul>
 * <p>
 * The logged-in username is set in the session attribute defined by the value of <code>CAS_FILTER_USER</code> and may
 * be accessed from within your application either by setting <code>wrapRequest</code> and calling
 * <code>request.getRemoteUser()</code>, or by calling <code>session.getAttribute(CASFilter.CAS_FILTER_USER)</code>.
 * </p>
 *
 * @author Shawn Bayern
 */
public class CASFilter implements Filter {

    // *********************************************************************
    // Constants

    /** Session attribute in which the username is stored */
    public final static String CAS_FILTER_USER = "edu.yale.its.tp.cas.client.filter.user";

    // *********************************************************************
    // Configuration state

    private String casLogin, casValidate, casAuthorizedProxy, casServiceUrl, casRenew, casServerName;

    private boolean wrapRequest;

    // *********************************************************************
    // Initialization

    @Override
    public void init(FilterConfig config) throws ServletException {
        casLogin = config.getInitParameter("edu.yale.its.tp.cas.client.filter.loginUrl");
        casValidate = config.getInitParameter("edu.yale.its.tp.cas.client.filter.validateUrl");
        casServiceUrl = config.getInitParameter("edu.yale.its.tp.cas.client.filter.serviceUrl");
        casAuthorizedProxy = config.getInitParameter("edu.yale.its.tp.cas.client.filter.authorizedProxy");
        casRenew = config.getInitParameter("edu.yale.its.tp.cas.client.filter.renew");
        casServerName = config.getInitParameter("edu.yale.its.tp.cas.client.filter.serverName");
        wrapRequest = Boolean.valueOf(config.getInitParameter("edu.yale.its.tp.cas.client.filter.wrapRequest")).booleanValue();
    }

    // *********************************************************************
    // Filter processing

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain fc) throws ServletException,
            IOException {

        // make sure we've got an HTTP request
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse))
            throw new ServletException("CASFilter protects only HTTP resources");

        // Wrap the request if desired
        if (wrapRequest) {
            request = new CASFilterRequestWrapper((HttpServletRequest) request);
        }

        HttpSession session = ((HttpServletRequest) request).getSession();

        // if our attribute's already present, don't do anything
        if (session != null && session.getAttribute(CAS_FILTER_USER) != null) {
            fc.doFilter(request, response);
            return;
        }

        // otherwise, we need to authenticate via CAS
        String ticket = request.getParameter("ticket");

        // no ticket? abort request processing and redirect
        if (ticket == null || ticket.equals("")) {
            if (casLogin == null) {
                throw new ServletException("When CASFilter protects pages that do not receive a 'ticket' "
                        + "parameter, it needs a edu.yale.its.tp.cas.client.filter.loginUrl " + "filter parameter");
            }
            ((HttpServletResponse) response).sendRedirect(casLogin + "?service="
                    + getService((HttpServletRequest) request)
                    + ((casRenew != null && !casRenew.equals("")) ? "&renew=" + casRenew : ""));

            // abort chain
            return;
        }

        // Yay, ticket! Validate it.
        String user = getAuthenticatedUser((HttpServletRequest) request);
        if (user == null)
            throw new ServletException("Unexpected CAS authentication error");

        // Store the authenticated user in the session
        if (session != null) // probably unncessary
            session.setAttribute(CAS_FILTER_USER, user);

        // continue processing the request
        fc.doFilter(request, response);
    }

    // *********************************************************************
    // Destruction

    @Override
    public void destroy() {
    }

    // *********************************************************************
    // Utility methods

    /**
     * Converts a ticket parameter to a username, taking into account an optionally configured trusted proxy in the tier
     * immediately in front of us.
     */
    private String getAuthenticatedUser(HttpServletRequest request) throws ServletException {
        ProxyTicketValidator pv = null;
        try {
            pv = new ProxyTicketValidator();
            pv.setCasValidateUrl(casValidate);
            pv.setServiceTicket(request.getParameter("ticket"));
            pv.setService(getService(request));
            pv.setRenew(Boolean.valueOf(casRenew).booleanValue());
            pv.validate();
            if (!pv.isAuthenticationSuccesful())
                throw new ServletException("CAS authentication error: " + pv.getErrorCode() + ": "
                        + pv.getErrorMessage());
            if (pv.getProxyList().size() != 0) {
                // ticket was proxied
                if (casAuthorizedProxy == null) {
                    throw new ServletException("this page does not accept proxied tickets");
                } else {
                    boolean authorized = false;
                    String proxy = (String) pv.getProxyList().get(0);
                    StringTokenizer casProxies = new StringTokenizer(casAuthorizedProxy);
                    while (casProxies.hasMoreTokens()) {
                        if (proxy.equals(casProxies.nextToken())) {
                            authorized = true;
                            break;
                        }
                    }
                    if (!authorized) {
                        throw new ServletException("unauthorized top-level proxy: '" + pv.getProxyList().get(0) + "'");
                    }
                }
            }
            return pv.getUser();
        } catch (SAXException ex) {
            String xmlResponse = "";
            if (pv != null)
                xmlResponse = pv.getResponse();
            throw new ServletException(ex + " " + xmlResponse);
        } catch (ParserConfigurationException ex) {
            throw new ServletException(ex);
        } catch (IOException ex) {
            throw new ServletException(ex);
        }
    }

    /**
     * Returns either the configured service or figures it out for the current request. The returned service is
     * URL-encoded.
     */
    private String getService(HttpServletRequest request) throws ServletException {
        // ensure we have a server name or service name
        if (casServerName == null && casServiceUrl == null)
            throw new ServletException("need one of the following configuration "
                    + "parameters: edu.yale.its.tp.cas.client.filter.serviceUrl or "
                    + "edu.yale.its.tp.cas.client.filter.serverName");

        // use the given string if it's provided
        if (casServiceUrl != null)
            return URLEncoder.encode(casServiceUrl);
        else
            // otherwise, return our best guess at the service
            return Util.getService(request, casServerName);
    }
}
