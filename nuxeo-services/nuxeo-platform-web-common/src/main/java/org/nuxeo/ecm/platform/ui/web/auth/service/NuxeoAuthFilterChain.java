package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthPreFilter;

public class NuxeoAuthFilterChain implements FilterChain {

    protected List<NuxeoAuthPreFilter> preFilters = new ArrayList<NuxeoAuthPreFilter>();

    protected NuxeoAuthenticationFilter mainFilter;

    protected FilterChain standardFilterChain;

    public NuxeoAuthFilterChain(List<NuxeoAuthPreFilter> preFilters,FilterChain standardFilterChain, NuxeoAuthenticationFilter mainFilter ) {
        this.preFilters.addAll(preFilters);
        this.mainFilter=mainFilter;
        this.standardFilterChain = standardFilterChain;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (preFilters!=null && ! preFilters.isEmpty()) {
            NuxeoAuthPreFilter preFilter = preFilters.remove(0);
            preFilter.doFilter(request, response, this);
        } else {
            mainFilter.doFilterInternal(request, response, standardFilterChain);
        }
    }

}
