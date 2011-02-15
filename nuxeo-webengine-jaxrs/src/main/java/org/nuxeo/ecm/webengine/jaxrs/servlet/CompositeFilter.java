/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.servlet;

import java.io.IOException;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.webengine.jaxrs.Utils;
import org.nuxeo.ecm.webengine.jaxrs.servlet.mapping.Path;
import org.nuxeo.ecm.webengine.jaxrs.servlet.mapping.PathMapping;
import org.nuxeo.ecm.webengine.jaxrs.servlet.mapping.PathMappingRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CompositeFilter implements Filter {

    protected PathMappingRegistry mappingReg;

    protected Filter[] filters;


    private static Filter[] loadFilters(String classRefs) throws Exception {
        return Utils.newInstances(Filter.class, classRefs);
    }

    public Filter[] getFilters() {
        return filters;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String v = filterConfig.getInitParameter("filters");
        try {
            mappingReg = Framework.getLocalService(PathMappingRegistry.class);
            filters = v != null ? loadFilters(v.trim()) : new Filter[0];
        } catch (ServletException e) {
            throw e;
        } catch (Throwable t) {
            throw new ServletException(t);
        }
        for (Filter filter : filters) {
            filter.init(filterConfig);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest)request;

        configureRequest(req);

        if (filters.length > 0) {
            SubFilterChain subChain = new SubFilterChain(chain, filters);
            subChain.doFilter(request, response);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        for (Filter filter : filters) {
            filter.destroy();
        }
        filters = null;
        mappingReg = null;
    }

    protected void configureRequest(HttpServletRequest request) {
        String cpath = request.getContextPath();
        String pathInfo = request.getPathInfo();
        if (pathInfo != null) {
            cpath = cpath + pathInfo;
        }

        Path path = Path.parse(cpath);
        for (PathMapping mapping : mappingReg.getMappings()) {
            if (mapping.getMatcher().matches(path)) {
                for (Map.Entry<String,String> entry : mapping.getParams().entrySet()) {
                    request.setAttribute(entry.getKey(), entry.getValue());
                }
            }
        }
    }



    public static class SubFilterChain implements FilterChain {

        protected final FilterChain chain;

        protected final Filter[] filters;

        protected int filterIndex;

        public SubFilterChain(FilterChain chain, Filter[] filters) {
            this.chain = chain;
            this.filters = filters;
            filterIndex = 0;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response)
                throws IOException, ServletException {
            if (filterIndex < filters.length) {
                Filter filter = filters[filterIndex++];
                filter.doFilter(request, response, this);
            } else {
                chain.doFilter(request, response);
            }
        }
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
        public void doFilter(ServletRequest request, ServletResponse response)
                throws IOException, ServletException {
            if (filterIndex < filters.length) {
                Filter filter = filters[filterIndex++];
                filter.doFilter(request, response, this);
            } else {
                servlet.service(request, response);
            }
        }
    }

}
