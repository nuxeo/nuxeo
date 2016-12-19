/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
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
            ExceptionHandlingService service = Framework.getService(ExceptionHandlingService.class);
            exceptionHandler = service.getExceptionHandler();
        }
        return exceptionHandler;
    }

    private void handleException(HttpServletRequest request, HttpServletResponse response, Exception e)
            throws IOException, ServletException {
        getHandler().handleException(request, response, e);
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } catch (RuntimeException | IOException | ServletException e) {
            try {
                handleException((HttpServletRequest) request, (HttpServletResponse) response, e);
            } catch (ServletException ee) {
                throw ee;
            } catch (RuntimeException | IOException ee) {
                throw new ServletException(ee);
            }
        }
    }

    public void destroy() {
    }

}
