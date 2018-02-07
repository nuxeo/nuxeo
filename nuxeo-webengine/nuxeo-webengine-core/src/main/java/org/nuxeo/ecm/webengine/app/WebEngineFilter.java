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
import java.io.UnsupportedEncodingException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.platform.web.common.ServletHelper;
import org.nuxeo.ecm.webengine.model.WebContext;

/**
 * This filter must be declared after the nuxeo authentication filter since it needs an authentication info. The session
 * synchronization is done only if NuxeoRequestControllerFilter was not already done it and stateful flag for the
 * request path is true.
 */
public class WebEngineFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
        // nothing to do
    }

    @Override
    public void destroy() {
        // nothing to do
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        boolean useTx = !isStatic(request);

        preRequest(request);
        ServletHelper.doFilter(new WebContextFilterChain(chain), request, response, useTx, true);
        postRequest(request, response);
    }

    protected boolean isStatic(HttpServletRequest request) {
        String pathInfo = StringUtils.defaultIfEmpty(request.getPathInfo(), "/");
        return request.getServletPath().contains("/skin") || pathInfo.contains("/skin/");
    }

    protected void preRequest(HttpServletRequest request) {
        // need to set the encoding of characters manually
        if (request.getCharacterEncoding() == null) {
            try {
                request.setCharacterEncoding("UTF-8");
            } catch (UnsupportedEncodingException e) {
                // cannot happen
                throw new RuntimeException(e);
            }
        }
    }

    protected void postRequest(HttpServletRequest request, HttpServletResponse response) {
        // check if the target resource don't want automatic headers to be inserted
        if (request.getAttribute("org.nuxeo.webengine.DisableAutoHeaders") != null) {
            // insert automatic headers
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "no-cache");
            response.addHeader("Cache-Control", "no-store");
            response.addHeader("Cache-Control", "must-revalidate");
            response.addHeader("Expires", "0");
            response.setDateHeader("Expires", 0); // prevents caching
        }
    }

    /**
     * Wraps a filter chain to provide the WebEngine {@link WebContext} in the request attributes.
     * <p>
     * The filter we're executing ({@link ServletHelper#doFilter}) may wrap the response, and it's this wrapped version
     * that must be available in the {@link WebContext}.
     *
     * @since 10.3
     */
    protected static class WebContextFilterChain implements FilterChain {

        protected final FilterChain chain;

        public WebContextFilterChain(FilterChain chain) {
            this.chain = chain;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            request.setAttribute(WebContext.class.getName(),
                    new DefaultContext((HttpServletRequest) request, (HttpServletResponse) response));
            try {
                chain.doFilter(request, response);
            } finally {
                request.removeAttribute(WebContext.class.getName());
            }
        }
    }

}
