/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.webengine.app;

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
import org.nuxeo.ecm.platform.web.common.ServletHelper;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.BufferingHttpServletResponse;
import org.nuxeo.ecm.webengine.PathDescriptor;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.impl.AbstractWebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * This filter must be declared after the nuxeo authentication filter since it
 * needs an authentication info.
 *
 * The session synchronization is done only if NuxeoRequestControllerFilter was
 * not already done it and stateful flag for the request path is true.
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
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        initIfNeeded();
        if (request instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse resp = (HttpServletResponse) response;
            PathDescriptor pd = engine.getRequestConfiguration().getMatchingConfiguration(
                    req);
            Config config = new Config(req, pd);
            AbstractWebContext ctx = initRequest(config, req, resp);
            if (config.txStarted) {
                resp = new BufferingHttpServletResponse(resp);
            }
            try {
                preRequest(req, resp);
                chain.doFilter(request, resp);
                postRequest(req, resp);
            } catch (Throwable e) {
                TransactionHelper.setTransactionRollbackOnly();
                if (e instanceof ServletException) {
                    throw (ServletException) e;
                } else if (e instanceof IOException) {
                    throw (IOException) e;
                } else {
                    throw new ServletException(e);
                }
            } finally {
                try {
                    cleanup(config, ctx, req, resp);
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

    public void preRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
        // need to set the encoding of characters manually
        if (null == request.getCharacterEncoding()) {
            request.setCharacterEncoding("UTF-8");
        }
        // response.setCharacterEncoding("UTF-8");
    }

    public void postRequest(HttpServletRequest request,
            HttpServletResponse response) throws Exception {
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

    public AbstractWebContext initRequest(Config config,
            HttpServletRequest request, HttpServletResponse response) {
        initTx(config, request);
        // user session is registered even for static resources - because some
        // static resources are served by JAX-RS resources that needs a user
        // session
        DefaultContext ctx = new DefaultContext((HttpServletRequest) request);
        request.setAttribute(WebContext.class.getName(), ctx);
        return ctx;
    }

    public void cleanup(Config config, AbstractWebContext ctx,
            HttpServletRequest request, HttpServletResponse response) {
        try {
            closeTx(config, request);
        } finally {
            request.removeAttribute(WebContext.class.getName());
        }
    }

    public void initTx(Config config, HttpServletRequest req) {
        if (!config.isStatic && config.autoTx
                && !TransactionHelper.isTransactionActive()) {
            config.txStarted = ServletHelper.startTransaction(req);
        }
    }

    public void closeTx(Config config, HttpServletRequest req) {
        if (config.txStarted) {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    protected static class Config {
        boolean autoTx;

        boolean txStarted;

        boolean locked;

        boolean isStatic;

        String pathInfo;

        public Config(HttpServletRequest req, PathDescriptor pd) {
            autoTx = pd == null ? true : pd.isAutoTx(true);
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
            sb.append("\nAuto TX:");
            sb.append(autoTx);
            sb.append("\nStatic:");
            sb.append(isStatic);
            return sb.toString();
        }
    }
}
