package org.nuxeo.ecm.platform.ui.web.auth.interfaces;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public interface NuxeoAuthPreFilter {

    /**
     * Main Filter method @See {@see Filter}
     *
     * @param request
     * @param response
     * @return
     * @throws IOException
     * @throws ServletException
     */
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException;

}
