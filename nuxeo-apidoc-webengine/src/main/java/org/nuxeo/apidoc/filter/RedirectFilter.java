package org.nuxeo.apidoc.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

public class RedirectFilter extends BaseApiDocFilter {

    protected boolean isUriValidForAnnonymous(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.contains("/nxpath/")) {
            return false;
        }
        if (uri.contains("/nxdoc/")) {
            return false;
        }
        if (uri.contains(".faces")) {
            return false;
        }
        if (uri.contains(".xhtml")) {
            return false;
        }
        return true;
    }

    protected void redirectToWebEngineView(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) throws IOException {
        String base = VirtualHostHelper.getBaseURL(httpRequest);
        String location = base + "site/distribution/";
        httpResponse.sendRedirect(location);
    }

    protected void internalDoFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        NuxeoPrincipal nxUser = (NuxeoPrincipal) httpRequest.getUserPrincipal();

        if (nxUser!=null && nxUser.isAnonymous()) {
            if (!isUriValidForAnnonymous(httpRequest)) {
                redirectToWebEngineView(httpRequest, httpResponse);
            }
        }

        chain.doFilter(httpRequest, httpResponse);
    }

}
