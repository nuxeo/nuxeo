/*
 * (C) Copyright 2006-2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import java.io.IOException;
import java.security.Principal;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TimeZone;
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

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.download.DownloadHelper;
import org.nuxeo.ecm.platform.web.common.ServletHelper;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestControllerManager;
import org.nuxeo.ecm.platform.web.common.requestcontroller.service.RequestFilterConfig;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

/**
 * Filter to handle transactions, response buffering, and request synchronization.
 *
 * @author tiry
 */
public class NuxeoRequestControllerFilter implements Filter {

    private static final Log log = LogFactory.getLog(NuxeoRequestControllerFilter.class);

    protected static final String SESSION_LOCK_KEY = "NuxeoSessionLockKey";

    protected static final String SYNCED_REQUEST_FLAG = "NuxeoSessionAlreadySync";

    protected static final int LOCK_TIMEOUT_S = 120;

    // formatted http Expires: Thu, 01 Dec 1994 16:00:00 GMT
    public static final FastDateFormat HTTP_EXPIRES_DATE_FORMAT = FastDateFormat.getInstance(
            "EEE, dd MMM yyyy HH:mm:ss z", TimeZone.getTimeZone("GMT"), Locale.US);

    @Override
    public void init(FilterConfig filterConfig) {
        // nothing to do
    }

    @Override
    public void destroy() {
        // nothing to do
    }

    public static String doFormatLogMessage(HttpServletRequest request, String info) {
        String remoteHost = RemoteHostGuessExtractor.getRemoteHost(request);
        Principal principal = request.getUserPrincipal();
        String principalName = principal != null ? principal.getName() : "none";
        String uri = request.getRequestURI();
        HttpSession session = request.getSession(false);
        String sessionId = session != null ? session.getId() : "none";
        String threadName = Thread.currentThread().getName();
        return "remote=" + remoteHost + ",principal=" + principalName + ",uri=" + uri + ",session=" + sessionId
                + ",thread=" + threadName + ",info=" + info;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        if (log.isDebugEnabled()) {
            log.debug(doFormatLogMessage(request, "Entering NuxeoRequestController filter"));
        }

        RequestControllerManager rcm = Framework.getService(RequestControllerManager.class);
        RequestFilterConfig config = rcm.getConfigForRequest(request);
        boolean useSync = config.needSynchronization();
        boolean useTx = config.needTransaction();
        boolean useBuffer = config.needTransactionBuffered();
        if (log.isDebugEnabled()) {
            log.debug(doFormatLogMessage(request,
                    "Handling request with tx=" + useTx + " and sync=" + useSync + " and buffer=" + useBuffer));
        }
        addHeaders(request, response, config);

        boolean sessionSynched = false;
        boolean txStarted = false;
        boolean buffered = false;
        try {
            ServletHelper.setServletContext(request.getServletContext());
            if (useSync) {
                sessionSynched = simpleSyncOnSession(request);
            }
            if (useTx) {
                if (!TransactionHelper.isTransactionActiveOrMarkedRollback()) {
                    txStarted = ServletHelper.startTransaction(request);
                    if (!txStarted) {
                        throw new ServletException("Failed to start transaction");
                    }
                }
                if (useBuffer) {
                    response = new BufferingHttpServletResponse(response);
                    buffered = true;
                }
            }
            chain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException e) {
            if (!response.isCommitted()) {
                response.resetBuffer();
            }
            int status;
            if (e instanceof NuxeoException) {
                status = ((NuxeoException) e).getStatusCode();
            } else {
                status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            }
            // Don't call response.sendError, because it commits the response
            // which prevents NuxeoExceptionFilter from returning a custom error page.
            response.setStatus(status);
            if (TransactionHelper.isTransactionActive()) {
                TransactionHelper.setTransactionRollbackOnly();
            }
            if (DownloadHelper.isClientAbortError(e)) {
                DownloadHelper.logClientAbort(e);
            } else if (e instanceof RuntimeException) { // NOSONAR
                throw new ServletException(e);
            } else {
                throw e; // IOException | ServletException
            }
        } finally {
            try {
                if (txStarted) {
                    try {
                        TransactionHelper.commitOrRollbackTransaction();
                    } catch (TransactionRuntimeException e) {
                        // commit failed, report this to the client before stopping buffering
                        // Don't call response.sendError, because it commits the response
                        // which prevents NuxeoExceptionFilter from returning a custom error page.
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        log.error(e, e); // don't rethrow inside finally
                    }
                }
            } finally {
                if (buffered) {
                    ((BufferingHttpServletResponse) response).stopBuffering();
                }
                if (sessionSynched) {
                    simpleReleaseSyncOnSession(request);
                }
                ServletHelper.removeServletContext();
            }
        }

        if (log.isDebugEnabled()) {
            log.debug(doFormatLogMessage(request, "Exiting NuxeoRequestController filter"));
        }
    }

    /**
     * Synchronizes the HttpSession.
     * <p>
     * Uses a {@link Lock} object in the HttpSession and locks it. If HttpSession is not created, exits without locking
     * anything.
     */
    public static boolean simpleSyncOnSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(request, "HttpSession does not exist, this request won't be synched"));
            }
            return false;
        }

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

        Lock lock = (Lock) session.getAttribute(SESSION_LOCK_KEY);
        if (lock == null) {
            lock = new ReentrantLock();
            session.setAttribute(SESSION_LOCK_KEY, lock);
        }

        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_TIMEOUT_S, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NuxeoException(e);
        }

        if (locked) {
            request.setAttribute(SYNCED_REQUEST_FLAG, true);
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(request, "Request synced on session"));
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(request, "Sync timeout"));
            }
        }

        return locked;
    }

    /**
     * Releases the {@link Lock} if present in the HttpSession.
     */
    public static boolean simpleReleaseSyncOnSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            if (log.isDebugEnabled()) {
                log.debug(doFormatLogMessage(request,
                        "No more HttpSession: can not unlock !, HttpSession must have been invalidated"));
            }
            return false;
        }
        log.debug("Trying to unlock on session " + session.getId() + " on Thread " + Thread.currentThread().getId());

        Lock lock = (Lock) session.getAttribute(SESSION_LOCK_KEY);
        if (lock == null) {
            log.error("Unable to find session lock, HttpSession may have been invalidated");
            return false;
        } else {
            lock.unlock();
            if (request.getAttribute(SYNCED_REQUEST_FLAG) != null) {
                request.removeAttribute(SYNCED_REQUEST_FLAG);
            }
            if (log.isDebugEnabled()) {
                log.debug("session unlocked on Thread ");
            }
            return true;
        }
    }

    protected void addHeaders(HttpServletRequest request, HttpServletResponse response,
            RequestFilterConfig config) {
        addConfiguredHeaders(response);
        if (request.getMethod().equals("GET")) {
            addCacheHeaders(response, config);
        }
    }

    protected void addConfiguredHeaders(HttpServletResponse response) {
        RequestControllerManager rcm = Framework.getService(RequestControllerManager.class);
        for (Entry<String, String> en : rcm.getResponseHeaders().entrySet()) {
            String headerName = en.getKey();
            if (!response.containsHeader(headerName)) {
                response.addHeader(headerName, en.getValue());
            }
        }
    }

    protected void addCacheHeaders(HttpServletResponse response, RequestFilterConfig config) {
        if (config.isCached()) {
            String privateOrPublic = config.isPrivate() ? "private" : "public";
            response.setHeader("Cache-Control", privateOrPublic + ", max-age=" + config.getCacheTime());
            long expires = System.currentTimeMillis() + Long.parseLong(config.getCacheTime()) * 1000;
            response.setHeader("Expires", HTTP_EXPIRES_DATE_FORMAT.format(expires));
        } else if (config.isPrivate()) {
            response.setHeader("Cache-Control", "private, no-cache, no-store, must-revalidate");
        }
    }

}
