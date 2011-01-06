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
import org.nuxeo.wss.servlet.config.FilterBindingConfig;
import org.nuxeo.wss.servlet.config.FilterBindingResolver;

public abstract class BaseWSSFilter implements Filter {

    protected FilterConfig filterConfig;

    protected ServletContext ctx;

    protected String rootFilterTarget = null;

    public static final String ROOT_FILTER_PARAM = "org.nuxeo.wss.rootFilter";

    public static final String BACKEND_FACTORY_PARAM = "org.nuxeo.wss.backendFactory";

    public static final String FILTER_FORWARD_PARAM = "org.nuxeo.wss.forwardedFilter";

    public static final String WSSFORWARD_KEY = "WSSForward";

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

            try {
                if ("OPTIONS".equals(httpRequest.getMethod())) {
                    handleOptionCall(httpRequest, httpResponse);
                    return;
                }
            } catch (Exception e) {
                throw new ServletException("Error processing request", e);
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

    protected void handleOptionCall(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) throws Exception {

        WSSStaticResponse response = new WSSStaticResponse(httpResponse);

        httpResponse.setHeader("MS-Author-Via", "MS-FP/4.0,DAV");
        httpResponse.setHeader("MicrosoftOfficeWebServer", "5.0_Collab");
        httpResponse.setHeader("X-MSDAVEXT", "1");
        httpResponse.setHeader("DocumentManagementServer",
                "Properties Schema;Source Control;Version History;");
        httpResponse.setHeader("DAV", "1,2");
        httpResponse.setHeader("Accept-Ranges", "none");
        httpResponse.setHeader(
                "Allow",
                "GET, POST, OPTIONS, HEAD, MKCOL, PUT, PROPFIND, PROPPATCH, DELETE, MOVE, COPY, GETLIB, LOCK, UNLOCK");

        response.process();
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