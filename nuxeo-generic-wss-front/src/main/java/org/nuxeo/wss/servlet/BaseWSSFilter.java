package org.nuxeo.wss.servlet;

import java.io.IOException;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.wss.fprpc.FPRPCConts;
import org.nuxeo.wss.servlet.config.FilterBindingConfig;
import org.nuxeo.wss.servlet.config.FilterBindingResolver;

public abstract class BaseWSSFilter implements Filter {

    protected FilterConfig filterConfig;

    protected ServletContext ctx;

    protected String rootFilterTarget = null;

    protected String webDavUrl = DEFAULT_WEBDAV_URL;

    public static final String ROOT_FILTER_PARAM = "org.nuxeo.wss.rootFilter";

    public static final String BACKEND_FACTORY_PARAM = "org.nuxeo.wss.backendFactory";

    public static final String FILTER_FORWARD_PARAM = "org.nuxeo.wss.forwardedFilter";

    public static final String WSSFORWARD_KEY = "WSSForward";

    public static final String DEFAULT_WEBDAV_URL = "/site/dav";

    public static final String NUXEO_ROOT_URL = "/nuxeo";

    private static final Log log = LogFactory.getLog(WSSFrontFilter.class);

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

            //check WebDAV calls
            try {
                if (isWebDavRequest(httpRequest) && !uri.startsWith(NUXEO_ROOT_URL + webDavUrl)) {
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

        //Wrap 'Destination' header parameter if need. Need for COPY and MOVE WebDAV methods
        String destination = httpRequest.getHeader("Destination");
        if (StringUtils.isNotEmpty(destination)) {
            destination = resolveDestinationPath(destination);
            HttpServletRequestWrapper httpRequestWrapper = new HttpServletRequestWrapper(httpRequest);
            httpRequestWrapper.setHeader("destination", destination);
            httpRequest = httpRequestWrapper;
        }

        //add correct header for WebDAV response
        httpResponse.setHeader("Server", "Microsoft-IIS/6.0");
        httpResponse.setHeader("X-Powered-By", "ASP.NET");
        httpResponse.setHeader("MicrosoftSharePointTeamServices", "12.0.0.6421");
        httpResponse.setHeader("Content-Type", "text/xml");
        httpResponse.setHeader("Cache-Control", "no-cache");
        httpResponse.setHeader("Public-Extension", "http://schemas.microsoft.com/repl-2");

        //forward request to WebDAV
        String createdURL = createPathToWebDav(httpRequest.getRequestURI());
        RequestDispatcher dispatcher = httpRequest.getRequestDispatcher(createdURL);
        dispatcher.forward(httpRequest, httpResponse);
    }

    private String createPathToWebDav(String basePath) {
        if (basePath.contains(NUXEO_ROOT_URL + webDavUrl)) {
            return basePath;
        } else {
            if (basePath.startsWith(NUXEO_ROOT_URL)) {
                return webDavUrl + basePath.substring(6);
            } else {
                return webDavUrl;
            }
        }
    }

    //check WebDAV request. Implemented only for Microsoft WebDAV client.
    //@TODO: check other WebDAV clients or implement general check
    private boolean isWebDavRequest(HttpServletRequest request){
        String ua = request.getHeader("User-Agent");
        return StringUtils.isNotEmpty(ua) && request.getHeader("User-Agent").contains(FPRPCConts.WEBDAV_USERAGENT);
    }

    //resolve destination path for WebDAV requests
    private String resolveDestinationPath(String destination){
        int index = destination.indexOf(NUXEO_ROOT_URL);
        String prefix = destination.substring(0, index + 6);
        String suffix = destination.substring(index + 6);
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

    public void destroy() {
    }

}