/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat, jcarsique
 *
 */

package org.nuxeo.wizard;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Filter that let the default servlet handle the resources and forward
 * processing calls to the servlet
 *
 * @author Tiry (tdelprat@nuxeo.com)
 * @since 5.4.2
 */
public class ResourceFilter implements Filter {

    protected String[] resourcesPrefix = { "/css/", "/images/", "/scripts/",
            "/jsp/" };

    protected boolean isResourceCall(String uri) {
        for (String prefix : resourcesPrefix) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String uri = httpRequest.getRequestURI();
        uri = uri.replaceFirst(httpRequest.getContextPath(), "");

        if (!isResourceCall(uri)) {
            request.getRequestDispatcher("/router" + uri).forward(request,
                    response);
        } else {
            chain.doFilter(httpRequest, response);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

}
