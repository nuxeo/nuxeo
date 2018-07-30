/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.webengine.app;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

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
import org.nuxeo.ecm.platform.web.common.ServletHelper;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.BufferingHttpServletResponse;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * This filter must be declared after the nuxeo authentication filter since it needs an authentication info. The session
 * synchronization is done only if NuxeoRequestControllerFilter was not already done it and stateful flag for the
 * request path is true.
 */
public class WebEngineFilter implements Filter {

    protected WebEngine engine;

    protected boolean isAutoTxEnabled;

    protected boolean isStatefull;

    protected static Log log = LogFactory.getLog(WebEngineFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        engine = Framework.getService(WebEngine.class);
    }

    @Override
    public void destroy() {
        engine = null;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }
        new UnitOfWork((HttpServletRequest) request, (HttpServletResponse)response).doFilter(chain);
    }

    private static class UnitOfWork {

        private final boolean txStarted;

        private final boolean isStatic;

        private final String pathInfo;

        private final DefaultContext context;

        private UnitOfWork(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            pathInfo = StringUtils.isEmpty(req.getPathInfo()) ? "/" :  req.getPathInfo();
            isStatic = req.getServletPath().contains("/skin") || pathInfo.contains("/skin/");
            txStarted = !isStatic && !TransactionHelper.isTransactionActiveOrMarkedRollback()
                    && ServletHelper.startTransaction(req);
            context = new DefaultContext(req, txStarted ? new BufferingHttpServletResponse(resp) : resp);
            req.setAttribute(WebContext.class.getName(), context);
        }

        private void doFilter(FilterChain chain) throws ServletException, IOException {
            boolean completedAbruptly = true;
            try {
                preRequest();
                chain.doFilter(context.getRequest(), context.getResponse());
                postRequest();
                completedAbruptly = false;
            } catch (IOException | ServletException | RuntimeException error) {
                context.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        error.getMessage());
                throw error;
            } finally {
                cleanup(completedAbruptly);
            }
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("WebEngine Filter:");
            sb.append("\nPath Info:");
            sb.append(pathInfo);
            sb.append("\nStatic:");
            sb.append(isStatic);
            return sb.toString();
        }

        void cleanup(boolean completedAbruptly) throws IOException {
            context.getRequest().removeAttribute(WebContext.class.getName());

            if (!txStarted) {
                return;
            }

            if (completedAbruptly) {
                TransactionHelper.setTransactionRollbackOnly();
            }
            try {
                TransactionHelper.commitOrRollbackTransaction();
            } catch (RuntimeException cause) {
                context.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, cause.getMessage());
            } finally {
                ((BufferingHttpServletResponse) context.getResponse()).stopBuffering();
            }
        }

        void preRequest() {
            // need to set the encoding of characters manually
            HttpServletRequest request = context.getRequest();
            if (request.getCharacterEncoding() == null) {
                try {
                    request.setCharacterEncoding("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        void postRequest() {
            HttpServletRequest request = context.getRequest();
            HttpServletResponse response = context.getResponse();
            // check if the target resource don't want automatic headers to be
            // inserted
            if (null != request.getAttribute("org.nuxeo.webengine.DisableAutoHeaders")) {
                // insert automatic headers
                response.addHeader("Pragma", "no-cache");
                response.addHeader("Cache-Control", "no-cache");
                response.addHeader("Cache-Control", "no-store");
                response.addHeader("Cache-Control", "must-revalidate");
                response.addHeader("Expires", "0");
                response.setDateHeader("Expires", 0); // prevents caching
            }
        }
    }
}
