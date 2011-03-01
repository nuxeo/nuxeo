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

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.nuxeo.ecm.webengine.jaxrs.servlet.config.FilterDescriptor;
import org.nuxeo.ecm.webengine.jaxrs.servlet.config.FilterSetDescriptor;
import org.nuxeo.ecm.webengine.jaxrs.servlet.mapping.Path;

/**
 * A filter set is a collections of filters that should be run for a given request in a servlet context.
 * <p>
 * The filter set is selected when it match the current pathInfo of the request.
 * Only one filter set can match a given path - the first one which is matching will be used,
 * all the other filter sets defined by a servlet will be ignored.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FilterSet {

    protected FilterSetDescriptor descriptor;
    private Filter[] filters;

    public FilterSet(FilterSetDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public boolean matches(String pathInfo) {
        return descriptor.matches(pathInfo);
    }

    public boolean matches(Path pathInfo) {
        return descriptor.matches(pathInfo);
    }

    public Filter[] getFilters() {
        return filters;
    }

    public void init(ServletConfig config) throws ServletException {
        try {
            List<FilterDescriptor> fds = descriptor.getFilters();
            filters = new Filter[fds.size()];
            for (int i=0, len=fds.size(); i<len; i++) {
                FilterDescriptor fd = fds.get(i);
                Filter filter = fd.getFilter();
                filter.init(new FilterConfigAdapter(fd, config));
                filters[i] = filter;
            }
        } catch (Exception e) {
            throw new ServletException("Failed to initialize filter set", e);
        }
    }

    public void destroy() {
        for (Filter filter : filters) {
            filter.destroy();
        }
        descriptor = null;
        filters = null;
    }


    static class FilterConfigAdapter implements FilterConfig {
        protected final ServletConfig config;
        protected final FilterDescriptor fd;

        public FilterConfigAdapter(FilterDescriptor fd, ServletConfig config) {
            this.fd = fd;
            this.config = config;
        }

        @Override
        public String getFilterName() {
            return fd.getRawClassRef();
        }

        @Override
        public String getInitParameter(String key) {
            return fd.getInitParams().get(key);
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return Collections.enumeration(fd.getInitParams().keySet());
        }

        @Override
        public ServletContext getServletContext() {
            return config.getServletContext();
        }

    }

}
