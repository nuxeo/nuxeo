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
package org.nuxeo.ecm.platform.web.common;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.io.download.DownloadHelper;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.BufferingHttpServletResponse;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

/**
 * Helpers for servlets.
 *
 * @since 5.6
 */
public class ServletHelper {

    private static final Log log = LogFactory.getLog(ServletHelper.class);

    public static final String TX_TIMEOUT_HEADER_KEY = "Nuxeo-Transaction-Timeout";

    private static final ThreadLocal<ServletContext> SERVLET_CONTEXT = new ThreadLocal<>();

    private ServletHelper() {
        // utility class
    }

    public static boolean startTransaction(HttpServletRequest request) {
        String header = request.getHeader(TX_TIMEOUT_HEADER_KEY);
        int timeout = 0; // default
        if (header != null) {
            try {
                timeout = Integer.parseInt(header);
            } catch (NumberFormatException e) {
                // bad header
                log.warn("Invalid request transaction timeout: " + header);
            }
        }
        return TransactionHelper.startTransaction(timeout);
    }

    /**
     * Generate a Content-Disposition string based on the servlet request for a given filename. The value follows
     * RFC2231
     *
     * @param request
     * @param filename
     * @return a full string to set as value of a {@code Content-Disposition} header
     * @since 5.7.2
     */
    public static String getRFC2231ContentDisposition(HttpServletRequest request, String filename) {
        return DownloadHelper.getRFC2231ContentDisposition(request, filename);
    }

    /**
     * @since 8.3
     */
    public static void setServletContext(ServletContext servletContext) {
        SERVLET_CONTEXT.set(servletContext);
    }

    /**
     * @since 8.3
     */
    public static ServletContext getServletContext() {
        return SERVLET_CONTEXT.get();
    }

    /**
     * @since 8.3
     */
    public static void removeServletContext() {
        SERVLET_CONTEXT.remove();
    }

    /**
     * Invokes a filter chain, possibly in a transaction and with buffered output.
     *
     * @since 10.3
     */
    public static void doFilter(FilterChain chain, HttpServletRequest request, HttpServletResponse response,
            boolean useTx, boolean useBuffer) throws IOException, ServletException {
        boolean txStarted = false;
        boolean buffered = false;

        setServletContext(request.getServletContext());
        try {
            if (useTx) {
                if (!TransactionHelper.isTransactionActiveOrMarkedRollback()) {
                    txStarted = startTransaction(request);
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
            // Don't call response.sendError, because it commits the response
            // which prevents NuxeoExceptionFilter from returning a custom error page.
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            if (TransactionHelper.isTransactionActive()) {
                TransactionHelper.setTransactionRollbackOnly();
            }
            if (DownloadHelper.isClientAbortError(e)) {
                DownloadHelper.logClientAbort(e);
            } else if (e instanceof RuntimeException) { //NOSONAR
                throw new ServletException(e);
            } else {
                throw e; // IOException | ServletException
            }
        } finally {
            removeServletContext();
            try {
                if (txStarted) {
                    try {
                        TransactionHelper.commitOrRollbackTransaction();
                    } catch (TransactionRuntimeException e) {
                        // commit failed, report this to the client before stopping buffering
                        // Don't call response.sendError, because it commits the response
                        // which prevents NuxeoExceptionFilter from returning a custom error page.
                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        log.error(e); // don't rethrow inside finally
                    }
                }
            } finally {
                if (buffered) {
                    ((BufferingHttpServletResponse) response).stopBuffering();
                }
            }
        }
    }

}
