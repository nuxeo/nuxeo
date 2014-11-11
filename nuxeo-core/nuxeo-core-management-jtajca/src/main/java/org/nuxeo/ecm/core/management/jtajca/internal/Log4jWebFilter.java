/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.core.management.jtajca.internal;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.MDC;

/**
 * @author matic
 * 
 */
public class Log4jWebFilter implements Filter {

    protected FilterConfig config;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        config = filterConfig;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        try {
            putProperty(request, "RemoteAddr");
            putProperty(request, "PathInfo");
            putProperty(request, "RequestURL");
            putProperty(request, "ServletPath");
            putProperty(request, "UserPrincipal");
            final HttpSession session = ((HttpServletRequest) request).getSession(false);
            if (session != null) {
                MDC.put("SessionID", session.getId());
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove("RemoteAddr");
            MDC.remove("PathInfo");
            MDC.remove("RequestURL");
            MDC.remove("ServletPath");
            MDC.remove("UserPrincipal");
            MDC.remove("SessionID");
        }

    }

    protected void putProperty(Object object, String propertyName) {
        try {
            if (object != null) {
                String name = propertyName.substring(0, 1).toLowerCase()
                        + propertyName.substring(1);
                MDC.put(propertyName, PropertyUtils.getProperty(object, name));
            }
        } catch (Exception e) {
        }
    }

}
