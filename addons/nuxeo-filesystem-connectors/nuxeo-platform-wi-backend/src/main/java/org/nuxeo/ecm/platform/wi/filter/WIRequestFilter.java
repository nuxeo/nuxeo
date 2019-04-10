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
import org.nuxeo.runtime.transaction.TransactionHelper;

public class WIRequestFilter implements Filter {

    public static String WEBDAV_USERAGENT = "Microsoft-WebDAV-MiniRedir";

    public static String MSOFFICE_USERAGENT = "Microsoft Office Existence Discovery";

    public static final String SESSION_KEY = "org.nuxeo.ecm.platform.wi.session";

    public static final String BACKEND_KEY = "org.nuxeo.ecm.platform.wi.backend";

    public static final String SESSION_LOCK_KEY = "SessionLockKey";

    public static final String SESSION_LOCK_TIME = "SessionLockTime";

    public static final String SYNCED_REQUEST_FLAG = "NuxeoSessionAlreadySync";

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

        if (isWIRequest(httpRequest)) {
            WISession session = SessionCacheHolder.getInstance().getCache().get(
                    httpRequest);
            httpRequest.setAttribute(SESSION_KEY, session);

            boolean sessionSynched = false;
            sessionSynched = simpleSyncOnSession(httpRequest);
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
                if (sessionSynched) {
                    simpleReleaseSyncOnSession(httpRequest);
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

    protected boolean simpleSyncOnSession(HttpServletRequest request) {
        if (log.isDebugEnabled()) {
            log.debug(doFormatLogMessage(request, "Trying to sync on session "));
        }

        if (request.getAttribute(SYNCED_REQUEST_FLAG) != null) {
            if (log.isWarnEnabled()) {
                log.warn(doFormatLogMessage(request,
                        "Request has already be synced, filter is reentrant, exiting without locking"));
            }
            return false;
        }

        WISession session = (WISession) request.getAttribute(SESSION_KEY);

        Lock lock = (Lock) session.getAttribute(SESSION_LOCK_KEY);
        if (lock == null) {
            lock = new ReentrantLock();
            session.setAttribute(SESSION_LOCK_KEY, lock);
        }

        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_TIMOUT_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error(
                    doFormatLogMessage(request,
                            "Unable to acuire lock for Session sync"), e);
            return false;
        }

        if (locked) {
            request.setAttribute(SYNCED_REQUEST_FLAG, Boolean.TRUE);
            request.setAttribute(SESSION_LOCK_TIME,
                    Long.valueOf(System.currentTimeMillis()));
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(request,
                        "Request synced on session"));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(request, "Sync timeout"));
            }
        }

        return locked;
    }

    protected void simpleReleaseSyncOnSession(HttpServletRequest request) {
        /*
         * HttpSession httpSession = request.getSession(false); if (httpSession
         * == null) { if (log.isDebugEnabled()) { log.debug(doFormatLogMessage(
         * request,
         * "No more HttpSession : can not unlock !, HttpSession must have been invalidated"
         * )); } return; }
         */

        WISession session = (WISession) request.getAttribute(SESSION_KEY);

        log.debug("Trying to unlock on httpSession key " + session.getKey()
                + " WISession:" + session.getKey() + " on Thread "
                + Thread.currentThread().getId());

        Lock lock = (Lock) session.getAttribute(SESSION_LOCK_KEY);
        if (lock == null) {
            log.error("Unable to find session lock, HttpSession may have been invalidated");
        } else {
            lock.unlock();
            if (log.isDebugEnabled()) {
                log.debug("session unlocked on Thread ");
                log.debug(doExecutionRequestLogMessage(request));
            }
        }
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

    protected String doExecutionRequestLogMessage(HttpServletRequest request) {
        Object lockTime = request.getAttribute(SESSION_LOCK_TIME);
        if (lockTime != null) {
            long time = ((Long) lockTime).longValue();
            long executionTime = System.currentTimeMillis() - time;
            return doFormatLogMessage(request, "Execution time:"
                    + executionTime + " ms.");
        } else {
            return doFormatLogMessage(request, "Unknown time of execution");
        }
    }

}
