/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.platform.wi.filter;

import java.io.IOException;
import java.security.Principal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.BufferingHttpServletResponse;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.RemoteHostGuessExtractor;
import org.nuxeo.ecm.platform.wi.backend.Backend;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class WIRequestFilter implements Filter {

    public static String WEBDAV_USERAGENT = "Microsoft-WebDAV-MiniRedir";

    public static String MSOFFICE_USERAGENT = "Microsoft Office Existence Discovery";

    public static final String SESSION_KEY = "org.nuxeo.ecm.platform.wi.session";

    public static final String BACKEND_KEY = "org.nuxeo.ecm.platform.wi.backend";

    public static final int LOCK_TIMOUT_S = 120;

    private static final Log log = LogFactory.getLog(WIRequestFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (isWIRequest(httpRequest) && httpRequest.getAttribute(SESSION_KEY) != null) {

            WISession session = new WISession(null);
            httpRequest.setAttribute(SESSION_KEY, session);

            boolean txStarted = false;
            try {
                txStarted = TransactionHelper.startTransaction();
                if (txStarted) {
                    response = new BufferingHttpServletResponse(httpResponse);
                }
                chain.doFilter(request, response);
            } catch (Exception e) {
                log.error(
                        doFormatLogMessage(httpRequest,
                                "Unhandled error was cauth by the Filter"), e);
                if (txStarted) {
                    if (log.isDebugEnabled()) {
                        log.debug(doFormatLogMessage(httpRequest,
                                "Marking transaction for RollBack"));
                    }
                    try {
                        TransactionHelper.setTransactionRollbackOnly();
                    } catch (Exception e1) {
                        log.warn("Could not mark transaction as rollback only.");
                    }
                }
                throw new ServletException(e);
            } finally {
                if (txStarted) {
                    try {
                        TransactionHelper.commitOrRollbackTransaction();
                    } finally {
                        ((BufferingHttpServletResponse) response).stopBuffering();
                    }
                }
                Backend backend = (Backend)session.getAttribute(BACKEND_KEY);
                if (backend != null) {
                    backend.destroy();
                }
                if (log.isDebugEnabled()) {
                    log.debug(doFormatLogMessage(httpRequest,
                            "Exiting NuxeoRequestControler filter"));
                }
            }
        } else {
            chain.doFilter(request, response);
            return;
        }
    }

    @Override
    public void destroy() {
    }

    private boolean isWIRequest(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return StringUtils.isNotEmpty(ua)
                && (ua.contains(WEBDAV_USERAGENT) || ua.contains(MSOFFICE_USERAGENT));
    }


    protected String doFormatLogMessage(HttpServletRequest request, String info) {
        String remoteHost = RemoteHostGuessExtractor.getRemoteHost(request);
        Principal principal = request.getUserPrincipal();
        String principalName = principal != null ? principal.getName() : "none";
        String uri = request.getRequestURI();
        String method = request.getMethod();
        HttpSession session = request.getSession(false);
        String sessionId = session != null ? session.getId() : "none";
        String threadName = Thread.currentThread().getName();
        return "remote=" + remoteHost + ",principal=" + principalName + ",uri="
                + uri + ", method=" + method + ",session=" + sessionId
                + ",thread=" + threadName + ",info=" + info;
    }

}
