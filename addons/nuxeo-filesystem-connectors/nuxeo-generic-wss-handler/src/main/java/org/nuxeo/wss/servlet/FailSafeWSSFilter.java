/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.wss.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@link WSSFilter} wrapper to avoid breaking the EAR if the front filter is not in the classpath
 */
public class FailSafeWSSFilter implements Filter {

    protected Filter wssFilter;
    protected Log log = LogFactory.getLog(FailSafeWSSFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        try {
            Class.forName("org.nuxeo.wss.servlet.BaseWSSFilter");
            Class<?> filterklass = Class.forName("org.nuxeo.wss.servlet.WSSFilter");
            wssFilter = (Filter) filterklass.newInstance();
            wssFilter.init(filterConfig);
        } catch (Throwable t) {
            log.warn("Can not initialize WSS Stack, check your installation : WSS feature won't be available", t);
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (wssFilter!=null) {
            wssFilter.doFilter(request, response, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
        if (wssFilter!=null) {
            wssFilter.destroy();
        }
    }
}
