package org.nuxeo.wss.servlet;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.wss.servlet.config.FilterBindingConfig;

/**
 * Root filter that must handle requests sent directly on /. Outside of the
 * OPTIONS calls, all other calls are forwarded to the backend filter.
 *
 * @author tiry
 */
public class WSSFrontFilter extends BaseWSSFilter implements Filter {

    @Override
    protected void handleWSSCall(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, FilterBindingConfig config)
            throws Exception {
        throw new UnsupportedOperationException(
                "This filter is not intended to receive actual WSS calls, check your configuration");
    }

    @Override
    protected void doForward(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse, FilterBindingConfig config)
            throws Exception {
        // To forward to the backend filter, we need to change context
        // but on some App Server (ex: Tomcat 6) default config prohibit this
        ServletContext targetContext = ctx.getContext(getRootFilterTarget());
        if (targetContext != null) {
            targetContext.getRequestDispatcher(httpRequest.getRequestURI()).forward(
                    httpRequest, httpResponse);
        } else {
            String newTarget = getRootFilterTarget()
                    + httpRequest.getRequestURI() + "?"
                    + httpRequest.getQueryString();
            httpResponse.sendRedirect(newTarget);
        }
    }

    @Override
    protected boolean isRootFilter() {
        String target = filterConfig.getInitParameter(ROOT_FILTER_PARAM);
        if (target != null && !"".equals(target)) {
            rootFilterTarget = target;
        } else {
            rootFilterTarget = System.getProperty("org.nuxeo.ecm.contextPath", "/nuxeo");
        }
        return true;
    }

    @Override
    protected void initBackend(FilterConfig filterConfig) {
        // No Backend to init
    }

    @Override
    protected void initHandlers(FilterConfig filterConfig) {
        // No Handlers to init

    }

}
