package org.nuxeo.apidoc.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.nuxeo.runtime.api.Framework;

public abstract class BaseApiDocFilter implements Filter {



    public static String APIDOC_FILTERS_ACTIVATED = "org.nuxeo.apidoc.activatefilter";

    protected List<String> allowedConnectUrls = new ArrayList<String>();

    protected Boolean activated = null;

    protected boolean isFilterActivated() {
        if (activated == null) {
            // don't activate by default
            activated = Boolean.valueOf(Framework.getProperty(
                    APIDOC_FILTERS_ACTIVATED, "false"));
        }
        return activated;
    }

    protected abstract void internalDoFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException;

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        if (!isFilterActivated()) {
            chain.doFilter(request, response);
            return;
        }
        internalDoFilter(request, response, chain);
    }


    public void destroy() {
    }

    public void init(FilterConfig filterConfig) throws ServletException {
    }

}
