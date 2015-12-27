/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.webengine.jaxrs.servlet.config.ServletDescriptor;
import org.nuxeo.ecm.webengine.jaxrs.servlet.mapping.Path;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class RequestChain {

    protected HttpServlet servlet;

    protected FilterSet[] filters;

    /**
     * Create a new request chain given the target servlet and an optional list of filter sets.
     *
     * @param servlet the target
     * @param filters the filter sets
     */
    public RequestChain(HttpServlet servlet, FilterSet[] filters) {
        if (servlet == null) {
            throw new IllegalArgumentException("No target servlet defined");
        }
        this.filters = filters == null ? new FilterSet[0] : filters;
        this.servlet = servlet;
    }

    public FilterSet[] getFilters() {
        return filters;
    }

    public HttpServlet getServlet() {
        return servlet;
    }

    public void init(ServletDescriptor sd, ServletConfig config) throws ServletException {
        for (FilterSet filterSet : filters) {
            filterSet.init(config);
        }
        if (servlet instanceof ManagedServlet) {
            ((ManagedServlet) servlet).setDescriptor(sd);
        }
        servlet.init(new ServletConfigAdapter(sd, config));
    }

    public void execute(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (filters.length == 0 || (request instanceof HttpServletRequest == false)) {
            servlet.service(request, response);
            return;
        }
        String pathInfo = ((HttpServletRequest) request).getPathInfo();
        Path path = pathInfo == null || pathInfo.length() == 0 ? Path.ROOT : Path.parse(pathInfo);
        for (FilterSet filterSet : filters) {
            if (filterSet.matches(path)) {
                new ServletFilterChain(servlet, filterSet.getFilters()).doFilter(request, response);
                return; // avoid running the servlet twice
            }
        }
        // if not filters matched just run the target servlet.
        servlet.service(request, response);
    }

    public void destroy() {
        if (servlet != null) {
            servlet.destroy();
            servlet = null;
        }
        for (FilterSet filterSet : filters) {
            filterSet.destroy();
        }
        filters = null;
    }

    public static class ServletFilterChain implements FilterChain {

        protected final HttpServlet servlet;

        protected final Filter[] filters;

        protected int filterIndex;

        public ServletFilterChain(HttpServlet servlet, Filter[] filters) {
            this.servlet = servlet;
            this.filters = filters;
            filterIndex = 0;

        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            if (filterIndex < filters.length) {
                Filter filter = filters[filterIndex++];
                filter.doFilter(request, response, this);
            } else {
                servlet.service(request, response);
            }
        }
    }

    static class ServletConfigAdapter implements ServletConfig {
        protected final ServletConfig config;

        protected final ServletDescriptor sd;

        public ServletConfigAdapter(ServletDescriptor sd, ServletConfig config) {
            this.config = config;
            this.sd = sd;
        }

        @Override
        public String getInitParameter(String key) {
            return sd.getInitParams().get(key);
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return Collections.enumeration(sd.getInitParams().keySet());
        }

        @Override
        public ServletContext getServletContext() {
            return config.getServletContext();
        }

        @Override
        public String getServletName() {
            return sd.getName();
        }
    }

}
