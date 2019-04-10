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
 *     Thierry Delprat
 */
package org.nuxeo.wss.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.wss.fprpc.FPRPCConts;
import org.nuxeo.wss.servlet.config.FilterBindingConfig;
import org.nuxeo.wss.servlet.config.FilterBindingResolver;

public abstract class BaseWSSFilter implements Filter {

    private static final String DEFAULT_CONTEXT_PATH = "/nuxeo";

    protected FilterConfig filterConfig;

    protected ServletContext ctx;

    protected String rootFilterTarget = null;

    protected String webDavUrl = DEFAULT_WEBDAV_URL;

    public static final String ROOT_FILTER_PARAM = "org.nuxeo.wss.rootFilter";

    public static final String BACKEND_FACTORY_PARAM = "org.nuxeo.wss.backendFactory";

    public static final String FILTER_FORWARD_PARAM = "org.nuxeo.wss.forwardedFilter";

    public static final String WSSFORWARD_KEY = "WSSForward";

    public static final String DEFAULT_WEBDAV_URL = "/site/dav";

    public final String nuxeoRootUrl = System.getProperty("org.nuxeo.ecm.contextPath", DEFAULT_CONTEXT_PATH);

    public final int nuxeoRootUrlLength = nuxeoRootUrl.length();

    private static final Log log = LogFactory.getLog(WSSFrontFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            String uri = httpRequest.getRequestURI();

            if (isRootFilter()) {
                String forward = httpRequest.getParameter(WSSFORWARD_KEY);
                if (forward != null) {
                    httpResponse.sendRedirect(forward);
                    return;
                }
            }

            // check WebDAV calls
            try {
                if (isWebDavRequest(httpRequest)
                        && !uri.startsWith(nuxeoRootUrl + webDavUrl)) {
                    handleWebDavCall(httpRequest, httpResponse);
                    return;
                }
            } catch (Exception e) {
                throw new ServletException("error processing request", e);
            }

            // let back filter do the job if any
            if (isRootFilter() && uri.startsWith(getRootFilterTarget())) {
                log.debug("Let WSS request to back filter");
                chain.doFilter(request, response);
                return;
            }

            Object forwardedConfig = httpRequest.getAttribute(FILTER_FORWARD_PARAM);

            if (forwardedConfig != null) {
                try {
                    handleForwardedCall(httpRequest, httpResponse,
                            (FilterBindingConfig) forwardedConfig);
                } catch (Exception e) {
                    throw new ServletException("Error processing WSS request",
                            e);
                }
            } else {
                FilterBindingConfig config = null;
                try {
                    config = FilterBindingResolver.getBinding(httpRequest);
                } catch (Exception e) {
                    throw new ServletException("Error processing WSS request",
                            e);
                }
                if (config != null) {
                    try {
                        if (isRootFilter()) {
                            log.debug("Forward call to backend filter");
                            httpRequest.setAttribute(FILTER_FORWARD_PARAM,
                                    config);
                            doForward(httpRequest, httpResponse, config);
                        } else {
                            handleWSSCall(httpRequest, httpResponse, config);
                        }
                    } catch (Exception e) {
                        throw new ServletException(
                                "Error processing WSS request", e);
                    }
                    return;
                } else {
                    // NOT a WSS request
                    chain.doFilter(request, response);
                }
            }
        }
    }

    protected String getRootFilterTarget() {
        return rootFilterTarget;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

        if (filterConfig != null) { // For Testing
            this.ctx = filterConfig.getServletContext();
        }

        synchronized (this.getClass()) {
            initHandlers(filterConfig);
            // simpleGetHandler = new SimpleGetHandler();
            // resourcesHandler = new ResourcesHandler();
            this.filterConfig = filterConfig;

            if (filterConfig != null) {
                initBackend(filterConfig);
            }
        }
    }

    protected void handleWebDavCall(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) throws Exception {

        // Wrap 'Destination' header parameter if need. Need for COPY and MOVE
        // WebDAV methods
        String destination = httpRequest.getHeader("Destination");
        if (StringUtils.isNotEmpty(destination)) {
            destination = resolveDestinationPath(destination);
            HttpServletRequestWrapper httpRequestWrapper = new HttpServletRequestWrapper(
                    httpRequest);
            httpRequestWrapper.setHeader("destination", destination);
            httpRequest = httpRequestWrapper;
        }

        // add correct header for WebDAV response
        if (isMSWebDavRequest(httpRequest)) {
            httpResponse.setHeader("Server", "Microsoft-IIS/6.0");
            httpResponse.setHeader("X-Powered-By", "ASP.NET");
            httpResponse.setHeader("MicrosoftSharePointTeamServices",
                    "12.0.0.6421");
            httpResponse.setHeader("Content-Type", "text/xml");
            httpResponse.setHeader("Cache-Control", "no-cache");
            httpResponse.setHeader("Public-Extension",
                    "http://schemas.microsoft.com/repl-2");
        }

        // forward request to WebDAV
        String createdURL = createPathToWebDav(httpRequest.getRequestURI());
        RequestDispatcher dispatcher = ctx.getRequestDispatcher(createdURL);
        dispatcher.forward(httpRequest, httpResponse);
    }

    private String createPathToWebDav(String basePath) {
        if (basePath.contains(nuxeoRootUrl + webDavUrl)) {
            return basePath;
        } else {
            if (basePath.startsWith(nuxeoRootUrl)) {
                return webDavUrl + basePath.substring(nuxeoRootUrlLength);
            } else {
                return webDavUrl;
            }
        }
    }

    private boolean isWebDavRequest(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return StringUtils.isNotEmpty(ua)
                && (ua.contains(FPRPCConts.MS_WEBDAV_USERAGENT));
        // || ua.contains(FPRPCConts.MAC_FINDER_USERAGENT));
    }

    private boolean isMSWebDavRequest(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return ua != null && ua.contains(FPRPCConts.MS_WEBDAV_USERAGENT);
    }

    // resolve destination path for WebDAV requests
    private String resolveDestinationPath(String destination) {
        int index = destination.indexOf(nuxeoRootUrl);
        String prefix = destination.substring(0, index + nuxeoRootUrlLength);
        String suffix = destination.substring(index + nuxeoRootUrlLength);
        return prefix + webDavUrl + suffix;
    }

    protected void handleForwardedCall(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            FilterBindingConfig forwardedConfig) throws Exception {
        log.debug("handle call forwarded by root filter");
        handleWSSCall(httpRequest, httpResponse, forwardedConfig);
    }

    protected abstract void initBackend(FilterConfig filterConfig);

    protected abstract void initHandlers(FilterConfig filterConfig);

    protected abstract boolean isRootFilter();

    protected abstract void doForward(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, FilterBindingConfig config)
            throws Exception;

    protected abstract void handleWSSCall(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, FilterBindingConfig config)
            throws Exception;

    @Override
    public void destroy() {
    }

}
