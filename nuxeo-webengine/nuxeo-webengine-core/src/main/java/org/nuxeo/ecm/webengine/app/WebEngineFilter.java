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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.web.common.ServletHelper;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.BufferingHttpServletResponse;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.impl.AbstractWebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

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
        initIfNeeded();
    }

    protected void initIfNeeded() {
        if (engine != null || Framework.getRuntime() == null) {
            return;
        }
        engine = Framework.getLocalService(WebEngine.class);
    }

    @Override
    public void destroy() {
        engine = null;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        initIfNeeded();
        if (request instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse resp = (HttpServletResponse) response;
            Config config = new Config(req);
            AbstractWebContext ctx = initRequest(config, req, resp);
            if (config.txStarted) {
                resp = new BufferingHttpServletResponse(resp);
            }
            boolean completedAbruptly = true;
            try {
                preRequest(req, resp);
                chain.doFilter(request, resp);
                postRequest(req, resp);
                completedAbruptly = false;
            } finally {
                if (completedAbruptly) {
                    TransactionHelper.setTransactionRollbackOnly();
                }
                try {
                    cleanup(config, ctx, req, resp);
                } catch (TransactionRuntimeException e) {
                    // commit failed, report this to the client before stopping buffering
                    resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
                    throw e;
                } finally {
                    if (config.txStarted) {
                        ((BufferingHttpServletResponse) resp).stopBuffering();
                    }
                }
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    public void preRequest(HttpServletRequest request, HttpServletResponse response) {
        // need to set the encoding of characters manually
        if (request.getCharacterEncoding() == null) {
            try {
                request.setCharacterEncoding("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void postRequest(HttpServletRequest request, HttpServletResponse response) {
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

    public AbstractWebContext initRequest(Config config, HttpServletRequest request, HttpServletResponse response) {
        initTx(config, request);
        // user session is registered even for static resources - because some
        // static resources are served by JAX-RS resources that needs a user
        // session
        DefaultContext ctx = new DefaultContext((HttpServletRequest) request);
        request.setAttribute(WebContext.class.getName(), ctx);
        return ctx;
    }

    public void cleanup(Config config, AbstractWebContext ctx, HttpServletRequest request, HttpServletResponse response) {
        try {
            closeTx(config, request);
        } finally {
            request.removeAttribute(WebContext.class.getName());
        }
    }

    public void initTx(Config config, HttpServletRequest req) {
        if (!config.isStatic && !TransactionHelper.isTransactionActiveOrMarkedRollback()) {
            config.txStarted = ServletHelper.startTransaction(req);
        }
    }

    public void closeTx(Config config, HttpServletRequest req) {
        if (config.txStarted) {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    protected static class Config {

        boolean txStarted;

        boolean isStatic;

        String pathInfo;

        public Config(HttpServletRequest req) {
            pathInfo = req.getPathInfo();
            if (pathInfo == null || pathInfo.length() == 0) {
                pathInfo = "/";
            }
            String spath = req.getServletPath();
            isStatic = spath.contains("/skin") || pathInfo.contains("/skin/");
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("WebEngineFilter&Confi:");
            sb.append("\nPath Info:");
            sb.append(pathInfo);
            sb.append("\nStatic:");
            sb.append(isStatic);
            return sb.toString();
        }
    }
}
