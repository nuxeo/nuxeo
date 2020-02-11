/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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

    protected List<String> allowedConnectUrls = new ArrayList<>();

    protected Boolean activated;

    protected boolean isFilterActivated() {
        if (activated == null) {
            // don't activate by default
            activated = Boolean.valueOf(Framework.isBooleanPropertyTrue(APIDOC_FILTERS_ACTIVATED));
        }
        return activated.booleanValue();
    }

    protected abstract void internalDoFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

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
