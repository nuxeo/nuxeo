package org.nuxeo.elasticsearch.web.sync;

import static org.jboss.seam.ScopeType.APPLICATION;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.Filter;
import org.jboss.seam.web.AbstractFilter;
import org.nuxeo.elasticsearch.listener.ElasticsearchInlineListener;

@Scope(APPLICATION)
@Filter(within="org.jboss.seam.web.ajax4jsfFilter")
@BypassInterceptors
@Name("UIThreadMarker")
public class UIThreadMarker extends AbstractFilter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        ElasticsearchInlineListener.useSyncIndexing.set(true);
        try {
            chain.doFilter(request, response);
        } finally {
            ElasticsearchInlineListener.useSyncIndexing.set(false);
        }
    }

}
