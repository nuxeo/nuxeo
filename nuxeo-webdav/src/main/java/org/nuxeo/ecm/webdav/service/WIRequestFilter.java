/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.webdav.service;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.BufferingHttpServletResponse;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

/**
 * Windows Integration Request filter, bound to /nuxeo. Allows Windows user agents to bind to the root as the expect
 * (not /nuxeo/site/dav) and still work.
 */
public class WIRequestFilter implements Filter {

    private static final Log log = LogFactory.getLog(WIRequestFilter.class);

    public static final String WEBDAV_USERAGENT = "Microsoft-WebDAV-MiniRedir";

    public static final String MSOFFICE_USERAGENT = "Microsoft Office Existence Discovery";

    public static final String BACKEND_KEY = "org.nuxeo.ecm.webdav.service.backend";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (!isWIRequest(httpRequest)) {
            chain.doFilter(request, response);
            return;
        }

        // do what WebEngineFilter does:
        // - start a transaction
        // - do response buffering
        boolean txStarted = false;
        boolean ok = false;
        try {
            if (!TransactionHelper.isTransactionActive()) {
                txStarted = TransactionHelper.startTransaction();
                if (!txStarted) {
                    throw new ServletException("A transaction is needed.");
                }
                response = new BufferingHttpServletResponse(httpResponse);
            }
            chain.doFilter(request, response);
            ok = true;
        } finally {
            if (txStarted) {
                try {
                    if (!ok) {
                        TransactionHelper.setTransactionRollbackOnly();
                    }
                    TransactionHelper.commitOrRollbackTransaction();
                } catch (TransactionRuntimeException e) {
                    // commit failed, report this to the client before stopping buffering
                    ((HttpServletResponse) response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            e.getMessage());
                    log.error(e); // don't rethrow inside finally
                } finally {
                    ((BufferingHttpServletResponse) response).stopBuffering();
                }
            }
        }
    }

    @Override
    public void destroy() {
    }

    private boolean isWIRequest(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return StringUtils.isNotEmpty(ua) && (ua.contains(WEBDAV_USERAGENT) || ua.contains(MSOFFICE_USERAGENT));
    }

}
