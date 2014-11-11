package org.nuxeo.ecm.platform.web.common.requestcontroller.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.nuxeo.runtime.trackers.concurrent.ThreadEvent;

public class NuxeoThreadTrackerFilter implements javax.servlet.Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        ThreadEvent.onEnter(this, false).send();
        try {
            chain.doFilter(request, response);
        } finally {
            ThreadEvent.onLeave(this).send();
        }
    }

}
