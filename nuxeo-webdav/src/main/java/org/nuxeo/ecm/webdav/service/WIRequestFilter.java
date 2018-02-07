/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Gagnavarslan ehf
 */
package org.nuxeo.ecm.webdav.service;

import java.io.IOException;

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

/**
 * Windows Integration Request filter, bound to /nuxeo. Allows Windows user agents to bind to the root as the expect
 * (not /nuxeo/site/dav) and still work.
 */
public class WIRequestFilter implements Filter {

    public static final String WEBDAV_USERAGENT = "Microsoft-WebDAV-MiniRedir";

    public static final String MSOFFICE_USERAGENT = "Microsoft Office Existence Discovery";

    public static final String BACKEND_KEY = "org.nuxeo.ecm.webdav.service.backend";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
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
        boolean useTx = isWIRequest(request);

        ServletHelper.doFilter(chain, request, response, useTx, true);
    }

    private boolean isWIRequest(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        return StringUtils.isNotEmpty(ua) && (ua.contains(WEBDAV_USERAGENT) || ua.contains(MSOFFICE_USERAGENT));
    }

}
