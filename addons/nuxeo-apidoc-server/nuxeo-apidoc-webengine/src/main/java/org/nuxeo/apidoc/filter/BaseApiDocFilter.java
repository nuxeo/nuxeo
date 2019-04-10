/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
package org.nuxeo.apidoc.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.nuxeo.runtime.api.Framework;

public abstract class BaseApiDocFilter implements Filter {

    public static final String APIDOC_FILTERS_ACTIVATED = "org.nuxeo.apidoc.activatefilter";

    protected List<String> allowedConnectUrls = new ArrayList<String>();

    protected Boolean activated;

    protected boolean isFilterActivated() {
        if (activated == null) {
            // don't activate by default
            activated = Boolean.valueOf(Framework.isBooleanPropertyTrue(APIDOC_FILTERS_ACTIVATED));
        }
        return activated.booleanValue();
    }

    protected abstract void internalDoFilter(ServletRequest request,
            ServletResponse response, FilterChain chain) throws IOException,
            ServletException;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        if (!isFilterActivated()) {
            chain.doFilter(request, response);
            return;
        }
        internalDoFilter(request, response, chain);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

}
