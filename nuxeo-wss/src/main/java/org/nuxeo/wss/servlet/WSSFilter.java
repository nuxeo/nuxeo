/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.wss.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.UserPrincipal;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoSecuredRequestWrapper;
import org.nuxeo.ecm.webengine.app.DefaultContext;
import org.nuxeo.ecm.webengine.jaxrs.context.RequestContext;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.wss.WSSConfig;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.fprpc.FPRPCRequest;
import org.nuxeo.wss.fprpc.FPRPCResponse;
import org.nuxeo.wss.handlers.fakews.FakeWSRequest;
import org.nuxeo.wss.handlers.fakews.FakeWSRouter;
import org.nuxeo.wss.handlers.fprpc.FPRPCRouter;
import org.nuxeo.wss.handlers.get.SimpleGetHandler;
import org.nuxeo.wss.handlers.resources.ResourcesHandler;
import org.nuxeo.wss.servlet.config.FilterBindingConfig;
import org.nuxeo.wss.spi.Backend;
import org.nuxeo.wss.spi.WSSBackend;

/**
 * This filter can be used either as a Front End of BackEnd filter It is designed to be either : - instantiated twice :
 * -- on / in rootFilter mode -- on /nuxeo in backend mode - instantiated once in backend mode but behind a
 * WSSFrontFilter
 *
 * @author tiry
 */
public class WSSFilter extends BaseWSSFilter implements Filter {

    protected static final Log log = LogFactory.getLog(WSSFilter.class);

    /**
     * Dummy username used for requests not authenticated otherwise.
     */
    public static final String WSS_USERNAME = "__wss__dummy__user__";

    protected Boolean rootFilter = null;

    protected SimpleGetHandler simpleGetHandler = null;

    protected ResourcesHandler resourcesHandler = null;

    @Override
    protected void doForward(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            FilterBindingConfig config) throws ServletException, IOException {
        // To forward to the backend filter, we need to change context
        // but on some App Server (ex: Tomcat 6) default config prohibit this
        ServletContext targetContext = ctx.getContext(getRootFilterTarget());
        if (targetContext != null) {
            targetContext.getRequestDispatcher(httpRequest.getRequestURI()).forward(httpRequest, httpResponse);
        } else {
            String newTarget = getRootFilterTarget() + httpRequest.getRequestURI() + "?" + httpRequest.getQueryString();
            if ("VtiHandler".equals(config.getTargetService()) || "SHtmlHandler".equals(config.getTargetService())) {
                handleWSSCall(httpRequest, httpResponse, config);
            } else {
                // try to redirect, but this won't work for all cases
                // since MS http libs don't seem to handle redirect
                // transparently
                httpResponse.sendRedirect(newTarget);
            }
        }
    }

    /*
     * Init transaction and WebEngine before handling WSS call.
     */
    @Override
    protected void handleWSSCall(HttpServletRequest request, HttpServletResponse response, FilterBindingConfig config)
            throws ServletException {
        boolean tx = false;
        boolean ok = false;
        RequestContext requestContext = null;
        LoginContext loginContext = null;
        try {
            // init transaction
            tx = TransactionHelper.startTransaction();
            // make sure we have a user
            if (request.getUserPrincipal() == null) {
                // if we didn't go through the NuxeoAuthenticationFilter
                // use a dummy user
                Principal principal = new UserPrincipal(WSS_USERNAME, new ArrayList<String>(), false, false);
                try {
                    loginContext = Framework.loginAs(principal.getName());
                } catch (LoginException e) {
                    throw new NuxeoException(e);
                }
                request = new NuxeoSecuredRequestWrapper(request, principal);
            }
            // init WebEngine context needed to later get the session
            // in AbstractBackendFactory
            requestContext = new RequestContext(request, response);
            request.setAttribute(WebContext.class.getName(), new DefaultContext(request));
            // we could use BufferingHttpServletResponse also here
            // do WSS call
            try {
                doWSSCall(request, response, config);
            } catch (WSSException e) {
                throw new ServletException(e);
            }
            ok = true;
        } finally {
            try {
                if (tx) {
                    if (!ok) {
                        TransactionHelper.setTransactionRollbackOnly();
                    }
                    TransactionHelper.commitOrRollbackTransaction();
                }
            } finally {
                SessionFactory.dispose(request);
                if (requestContext != null) {
                    requestContext.dispose();
                }
                if (loginContext != null) {
                    try {
                        loginContext.logout();
                    } catch (LoginException e) {
                        throw new NuxeoException(e);
                    }
                }
            }
        }
    }

    protected void doWSSCall(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            FilterBindingConfig config) throws WSSException, ServletException {

        try {
            httpRequest.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new NuxeoException(e);
        }
        httpResponse.setCharacterEncoding("UTF-8");

        WSSRequest request = new WSSRequest(httpRequest, config.getSiteName());
        WSSResponse response = null;

        WSSBackend backend = Backend.get(request);

        backend.begin();

        log.debug("Handling WSS call : " + httpRequest.getRequestURL().toString());

        try {
            if (FilterBindingConfig.FP_REQUEST_TYPE.equals(config.getRequestType())) {
                FPRPCRequest fpRequest = new FPRPCRequest(httpRequest, config.getSiteName());
                request = fpRequest;
                response = new FPRPCResponse(httpResponse);
                FPRPCRouter.handleFPRCPRequest(fpRequest, (FPRPCResponse) response, config);
            } else if (FilterBindingConfig.GET_REQUEST_TYPE.equals(config.getRequestType())) {
                response = new WSSResponse(httpResponse);
                simpleGetHandler.handleRequest(request, response);
            } else if (FilterBindingConfig.RESOURCES_REQUEST_TYPE.equals(config.getRequestType())) {
                resourcesHandler.handleResource(httpRequest, httpResponse);
                return;
            } else if (FilterBindingConfig.FAKEWS_REQUEST_TYPE.equals(config.getRequestType())) {
                FakeWSRequest wsRequest = new FakeWSRequest(httpRequest, config.getSiteName());
                request = wsRequest;
                response = new WSSResponse(httpResponse);
                FakeWSRouter.handleFakeWSRequest(wsRequest, response, config);
            }

            if (response == null) {
                log.error("no response was created by WSS call handling");
                throw new ServletException("WSSResponse is not set");
            } else {
                response.processIfNeeded();
            }

            backend.saveChanges();
        } catch (IOException e) {
            throw new WSSException("Error while processing WSS request", e);
        }
    }

    @Override
    protected boolean isRootFilter() {
        if (rootFilter == null) {
            if (filterConfig != null) {
                String target = filterConfig.getInitParameter(ROOT_FILTER_PARAM);
                if (target != null && !"".equals(target)) {
                    rootFilter = true;
                    rootFilterTarget = target;
                } else {
                    rootFilter = false;
                }
            } else {
                rootFilter = false;
            }
        }
        return rootFilter;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        super.init(filterConfig);
        if (isRootFilter()) {
            WSSConfig.instance().setContextPath(rootFilterTarget);
        }
    }

    @Override
    protected void initBackend(FilterConfig filterConfig) {
        String factoryName = filterConfig.getInitParameter(BACKEND_FACTORY_PARAM);
        if (factoryName != null) {
            WSSConfig.instance().setWssBackendFactoryClassName(factoryName);
        }
    }

    @Override
    protected void initHandlers(FilterConfig filterConfig) {
        simpleGetHandler = new SimpleGetHandler();
        resourcesHandler = new ResourcesHandler();
    }

}
