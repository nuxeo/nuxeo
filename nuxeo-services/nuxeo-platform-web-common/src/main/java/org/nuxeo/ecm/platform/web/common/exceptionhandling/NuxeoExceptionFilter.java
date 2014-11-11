/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.web.common.exceptionhandling;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.service.ExceptionHandlingService;
import org.nuxeo.runtime.api.Framework;

public class NuxeoExceptionFilter implements Filter {

    /**
     * @deprecated use {@link NuxeoExceptionHandler#EXCEPTION_HANDLER_MARKER}
     */
    @Deprecated
    public static final String EXCEPTION_FILTER_ATTRIBUTE = "NuxeoExceptionFilter";

    private NuxeoExceptionHandler exceptionHandler;

    private static final Log log = LogFactory.getLog(NuxeoExceptionFilter.class);

    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            getHandler();
        } catch (ServletException e) {
            log.info("NuxeoExceptionHandler will be lazy loaded");
        }
    }

    protected NuxeoExceptionHandler getHandler() throws ServletException {
        if (exceptionHandler == null) {
            ExceptionHandlingService service;
            try {
                service = Framework.getService(ExceptionHandlingService.class);
                exceptionHandler = service.getExceptionHandler();
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }
        return exceptionHandler;
    }

    private void handleException(HttpServletRequest request,
            HttpServletResponse response, Throwable t) throws IOException,
            ServletException {
        getHandler().handleException(request, response, t);
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (Throwable t) {
            try {
                handleException((HttpServletRequest) request,
                        (HttpServletResponse) response, t);
            } catch (ServletException e) {
                throw e;
            } catch (Throwable newThrowable) {
                throw new ServletException(newThrowable);
            }
        }
    }

    public void destroy() {
    }

}
