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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.wss.WSSConfig;
import org.nuxeo.wss.WSSException;
import org.nuxeo.wss.fm.FreeMarkerRenderer;
import org.nuxeo.wss.fprpc.FPRPCRequest;
import org.nuxeo.wss.fprpc.FPRPCResponse;
import org.nuxeo.wss.handlers.fakews.FakeWSRequest;
import org.nuxeo.wss.handlers.fakews.FakeWSRouter;
import org.nuxeo.wss.handlers.fprpc.FPRPCRouter;
import org.nuxeo.wss.handlers.get.SimpleGetHandler;
import org.nuxeo.wss.handlers.resources.ResourcesHandler;
import org.nuxeo.wss.servlet.config.FilterBindingConfig;
import org.nuxeo.wss.servlet.config.FilterBindingResolver;
import org.nuxeo.wss.spi.Backend;
import org.nuxeo.wss.spi.WSSBackend;

public class WSSFilter implements Filter {

    protected SimpleGetHandler simpleGetHandler;
    protected ResourcesHandler resourcesHandler;
    protected FilterConfig filterConfig;
    protected Boolean rootFilter = null;
    protected String rootFilterTarget = null;
    protected ServletContext ctx;
    public static final String ROOT_FILTER_PARAM = "org.nuxeo.wss.rootFilter";
    public static final String BACKEND_FACTORY_PARAM = "org.nuxeo.wss.backendFactory";
    public static final String FILTER_FORWARD_PARAM = "org.nuxeo.wss.forwardedFilter";
    public static final String WSSFORWARD_KEY = "WSSForward";
    private static final Log log = LogFactory.getLog(WSSFilter.class);

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            String uri = httpRequest.getRequestURI();

            if (isRootFilter()) {
                String forward = httpRequest.getParameter(WSSFORWARD_KEY);
                if (forward !=null) {
                    httpResponse.sendRedirect(forward);
                    return;
                }
            }

            try {
                if ("OPTIONS".equals(httpRequest.getMethod())) {
                    handleOptionCall(httpRequest, httpResponse);
                    return;
                }
            } catch (Exception e) {
                throw new ServletException("error processing request", e);
            }

            // let back filter do the job if any
            if (isRootFilter() && uri.startsWith(getRootFilterTarget())) {
                log.debug("let WSS request to to back filter");
                chain.doFilter(request, response);
                return;
            }

            Object forwardedConfig = httpRequest.getAttribute(FILTER_FORWARD_PARAM);
            try {

                // check if we have behind a root filter
                if (forwardedConfig!=null) {
                    log.debug("handle call forwarded by root filter");
                    handleWSSCall(httpRequest, httpResponse, (FilterBindingConfig) forwardedConfig);
                     return;
                }


                FilterBindingConfig config = FilterBindingResolver.getBinding(httpRequest);
                if (config!=null) {
                    if (isRootFilter()) {
                        log.debug("Forward call to backend filter");
                        httpRequest.setAttribute(FILTER_FORWARD_PARAM, config);
                        // To forward to the backend filter, we need to change context
                        // but on some App Server (ex: Tomcat 6) default config prohibit this
                        ServletContext targetContext =ctx.getContext(getRootFilterTarget());
                        if (targetContext!=null) {
                            targetContext.getRequestDispatcher(httpRequest.getRequestURI()).forward(request, response);
                        } else {
                            String newTarget = getRootFilterTarget() + httpRequest.getRequestURI() + "?" + httpRequest.getQueryString();
                            if ("VtiHandler".equals(config.getTargetService()) || "SHtmlHandler".equals(config.getTargetService())) {
                                handleWSSCall(httpRequest, httpResponse, config);
                            } else {
                                // try to redirect, but this won't work for all cases
                                // since MS http libs don't seem to handle redirect transparently
                                httpResponse.sendRedirect(newTarget);
                            }
                        }
                    } else {
                        handleWSSCall(httpRequest, httpResponse, config);
                    }
                    return;
                }
            } catch (Exception e) {
                throw new ServletException("error processing request", e);
            }
        }
        chain.doFilter(request, response);
    }

    protected void handleWSSCall(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterBindingConfig config) throws Exception {

        httpRequest.setCharacterEncoding("UTF-8");
        httpResponse.setCharacterEncoding("UTF-8");

        WSSRequest request = new WSSRequest(httpRequest, config.getSiteName());
        WSSResponse response = null;

        WSSBackend backend = Backend.get(request);

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

            if (response==null) {
                log.error("no response was created by WSS call handling");
                throw new ServletException("WSSResponse is not set");
            } else {
                response.processIfNeeded();
            }

            backend.saveChanges();
        }
        catch (Exception e) {
            if (backend!=null) {
                backend.discardChanges();
            }
            log.error("Error during WSS call processing", e);
            throw new WSSException("Error while processing WSS request", e);
        }
    }

    protected void handleOptionCall(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {

        WSSResponse response = new WSSResponse(httpResponse);

        httpResponse.setHeader("MS-Author-Via", "MS-FP/4.0,DAV");
        httpResponse.setHeader("MicrosoftOfficeWebServer", "5.0_Collab");
        httpResponse.setHeader("X-MSDAVEXT","1");
        httpResponse.setHeader("DocumentManagementServer", "Properties Schema;Source Control;Version History;");
        httpResponse.setHeader("DAV","1,2");
        httpResponse.setHeader("Accept-Ranges","none");
        httpResponse.setHeader("Allow", "GET, POST, OPTIONS, HEAD, MKCOL, PUT, PROPFIND, PROPPATCH, DELETE, MOVE, COPY, GETLIB, LOCK, UNLOCK");

        response.process();

    }

    protected String getRootFilterTarget()  {
        return rootFilterTarget;
    }

    protected boolean isRootFilter() {
        if (rootFilter==null) {
            if (filterConfig!=null ) {
                String target = filterConfig.getInitParameter(ROOT_FILTER_PARAM);
                if (target!=null && !"".equals(target)) {
                    rootFilter=true;
                    rootFilterTarget = target;
                } else {
                    rootFilter = false;
                }
            } else {
                rootFilter=false;
            }
        }
        return rootFilter;
    }

    public void  init(FilterConfig filterConfig) throws ServletException {

        if (filterConfig!=null) { // For Testing
            this.ctx = filterConfig.getServletContext();
        }

        synchronized (this.getClass()) {
            simpleGetHandler = new SimpleGetHandler();
            resourcesHandler = new ResourcesHandler();
            this.filterConfig = filterConfig;

            if (filterConfig!=null) {
                String factoryName = filterConfig.getInitParameter(BACKEND_FACTORY_PARAM);
                if (factoryName!=null) {
                    WSSConfig.instance().setWssBackendFactoryClassName(factoryName);
                    Class factoryKlass = Backend.getFactory().getClass();
                    FreeMarkerRenderer.addLoader(factoryKlass);
                }
            }
            if (isRootFilter()) {
                WSSConfig.instance().setContextPath(rootFilterTarget);
            }
        }
    }

    public void destroy() {
    }

}
