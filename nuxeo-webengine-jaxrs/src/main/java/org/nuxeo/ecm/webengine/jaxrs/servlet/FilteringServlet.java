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
import java.util.Enumeration;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FilteringServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected CompositeFilter filter;
    protected HttpServlet servlet;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String v = (String)config.getInitParameter("servlet");
        if (v == null) {
            throw new ServletException("servlet init-parameter is required and must point to a servlet class to delegate request handling.");
        }
        //TODO use bundle to load the class
        //servlet = new xxx;
        filter = new CompositeFilter();
        filter.init(new FilterConfigAdapter(config));
        servlet.init(config);
    }

    @Override
    public void destroy() {
        super.destroy();
        if (servlet != null) {
            servlet.destroy();
            servlet = null;
        }
        if (filter != null) {
            filter.destroy();
            filter = null;
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        FilterChain chain = new CompositeFilter.ServletFilterChain(servlet, filter.getFilters());
        chain.doFilter(request, response);
    }


    static class FilterConfigAdapter implements FilterConfig {
        protected ServletConfig config;

        public FilterConfigAdapter(ServletConfig config) {
            this.config = config;
        }

        @Override
        public String getFilterName() {
            return config.getServletName();
        }

        @Override
        public String getInitParameter(String arg0) {
            return config.getInitParameter(arg0);
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Enumeration getInitParameterNames() {
            return config.getInitParameterNames();
        }

        @Override
        public ServletContext getServletContext() {
            return config.getServletContext();
        }

    }

}
