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
package org.nuxeo.ecm.webengine.jaxrs.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.webengine.jaxrs.Activator;
import org.nuxeo.ecm.webengine.jaxrs.context.mapping.Path;
import org.nuxeo.ecm.webengine.jaxrs.context.mapping.PathMapping;
import org.nuxeo.ecm.webengine.jaxrs.context.mapping.PathMappingRegistry;
import org.nuxeo.runtime.api.Framework;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CompositeFilter implements Filter {

    protected PathMappingRegistry mappingReg;

    protected Filter[] filters;


    private static Filter[] loadFilters(String v) throws Exception {
        BundleContext ctx = Activator.getInstance().getContext();
        ServiceReference ref = ctx.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin adm = (PackageAdmin) ctx.getService(ref);
        List<Filter> filters = new ArrayList<Filter>();
        try {
            String[] ar = v.split("\\s+");
            for (String s : ar) {
                if (s.length() == 0) {
                    continue;
                }
                Class<?> cl = null;
                int i = s.indexOf(':');
                if (i > -1) {
                    String bname = s.substring(0, i);
                    String cname = s.substring(i + 1);
                    Bundle[] bundles = adm.getBundles(bname, null);
                    if (bundles != null) {
                        cl = bundles[0].loadClass(cname);
                    } else {
                        throw new IllegalStateException("No bundle was found: "+bname);
                    }
                } else {
                    cl = CompositeFilter.class.getClassLoader().loadClass(s);
                }
                if (cl != null) {
                    filters.add((Filter) cl.newInstance());
                }
            }
        } finally {
            ctx.ungetService(ref);
        }
        return filters.toArray(new Filter[filters.size()]);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String v = (String)filterConfig.getInitParameter("filters");
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
        if (request instanceof HttpServletRequest == false) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest)request;

        configureRequest(req);

        if (filters.length > 0) {
            SubFilterChain subChain = new SubFilterChain(chain, this);
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


    public static final boolean getBoolean(ServletRequest request, String key) {
        return Boolean.parseBoolean((String)request.getAttribute(key));
    }

    public static final boolean isEnabled(ServletRequest request, Filter filter) {
        return !getBoolean(request, filter.getClass().getName().concat(".disabled"));
    }


    static class SubFilterChain implements FilterChain {

        protected FilterChain chain;

        protected CompositeFilter parent;

        protected Filter[] filters;

        protected int filterIndex;

        public SubFilterChain(FilterChain chain, CompositeFilter parent) {
            this.chain = chain;
            this.parent = parent;
            this.filters = parent.filters;
            filterIndex = 0;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response)
                throws IOException, ServletException {
            if (filterIndex < filters.length) {
                Filter filter = filters[filterIndex];
                if (CompositeFilter.isEnabled(request, filter)) {
                    filter.doFilter(request, response, this);
                }
                filterIndex++;
            } else {
                chain.doFilter(request, response);
            }
        }
    }
}
