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

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.BufferingHttpServletResponse;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Windows Integration Request filter, bound to /nuxeo.
 *
 * Allows Windows user agents to bind to the root as the expect (not
 * /nuxeo/site/dav) and still work.
 */
public class WIRequestFilter implements Filter {

    public static String WEBDAV_USERAGENT = "Microsoft-WebDAV-MiniRedir";

    public static String MSOFFICE_USERAGENT = "Microsoft Office Existence Discovery";

    public static final String BACKEND_KEY = "org.nuxeo.ecm.webdav.service.backend";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

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
        return StringUtils.isNotEmpty(ua)
                && (ua.contains(WEBDAV_USERAGENT) || ua.contains(MSOFFICE_USERAGENT));
    }

}
