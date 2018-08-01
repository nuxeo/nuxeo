/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.webengine.app;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.webengine.model.WebContext;

/**
 * This filter sets up the {@link WebContext}, made available through {@link WebEngine#getActiveContext}.
 */
public class WebContextFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // nothing to do
    }

    @Override
    public void destroy() {
        // nothing to do
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String name = WebContext.class.getName();
        request.setAttribute(name, new DefaultContext((HttpServletRequest) request, (HttpServletResponse) response));
        try {
            chain.doFilter(request, response);
        } finally {
            request.removeAttribute(name);
        }
    }

}
