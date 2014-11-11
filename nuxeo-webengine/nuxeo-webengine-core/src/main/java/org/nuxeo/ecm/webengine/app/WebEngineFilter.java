/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
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
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class WebEngineFilter implements Filter {

    // private Log log = LogFactory.getLog(WebEngineFilter.class);

    /**
     * Framework property to control whether tx is started by webengine by
     * default
     */
    public static final String TX_AUTO = "org.nuxeo.webengine.tx.auto";

    /**
     * Framework property giving the default session scope - stateful or
     * stateless.
     */
    public static final String STATEFULL = "org.nuxeo.webengine.session.stateful";

    protected WebEngine engine;

    protected boolean isAutoTxEnabled;

    protected boolean isStatefull;

    protected static Log log = LogFactory.getLog(WebEngineFilter.class);

    // protected boolean enableJsp = false;
    // private static boolean isTaglibLoaded = false;

    public void init(FilterConfig filterConfig) throws ServletException {
        initIfNeeded();
    }

    protected void initIfNeeded() {
        if (engine != null || Framework.getRuntime() == null) {
            return;
        }
        engine = Framework.getLocalService(WebEngine.class);
        String v = Framework.getProperty(TX_AUTO, "true");
        isAutoTxEnabled = Boolean.parseBoolean(v);
        v = Framework.getProperty(STATEFULL, "false");
        isStatefull = Boolean.parseBoolean(v);
        // String v =
        // Framework.getProperty("org.nuxeo.ecm.webengine.enableJsp");
        // if ("true".equals(v)) {
        // enableJsp = true;
        // }
    }

    public void destroy() {
        engine = null;
    }

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        initIfNeeded();
        if (request instanceof HttpServletRequest) {
            HttpServletRequest req = (HttpServletRequest) request;
            HttpServletResponse resp = (HttpServletResponse) response;
            PathDescriptor pd = engine.getRequestConfiguration().getMatchingConfiguration(
                    req);
            Config config = new Config(req, pd, isAutoTxEnabled, isStatefull);
            AbstractWebContext ctx = initRequest(config, req, resp);
            try {
                preRequest(req, resp);
                chain.doFilter(request, response);
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
                cleanup(config, ctx, req, resp);
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
        // TODO: remove this
        // if (enableJsp) {
        // WebEngine engine = Framework.getLocalService(WebEngine.class);
        // if (!isTaglibLoaded) {
        // synchronized (this) {
        // if (!isTaglibLoaded) {
        // engine.loadJspTaglib(this);
        // isTaglibLoaded = true;
        // }
        // }
        // }
        // engine.initJspRequestSupport(this, request,
        // response);
        // }

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
            config.txStarted = TransactionHelper.startTransaction();
            // log.warn("tx started for " + req.getPathInfo());
        }
    }

    public void closeTx(Config config, HttpServletRequest req) {
        if (config.txStarted) {
            TransactionHelper.commitOrRollbackTransaction();
            config.txStarted = false;
            // log.warn("tx closed for " + req.getPathInfo());
        }
    }

    static class Config {
        boolean autoTx;

        boolean stateful;

        boolean txStarted;

        boolean locked;

        boolean isStatic;

        String pathInfo;

        public Config(HttpServletRequest req, PathDescriptor pd,
                boolean autoTx, boolean stateful) {
            if (pd == null) {
                this.autoTx = autoTx;
                this.stateful = stateful;
            } else {
                this.autoTx = pd.isAutoTx(autoTx);
                this.stateful = pd.isStateful(stateful);
            }
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
            sb.append("\nStateful:");
            sb.append(stateful);
            sb.append("\nStatic:");
            sb.append(isStatic);
            return sb.toString();
        }
    }
}
