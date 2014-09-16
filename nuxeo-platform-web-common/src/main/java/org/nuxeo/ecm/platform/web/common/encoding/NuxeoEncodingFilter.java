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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.web.common.encoding;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Filter that sets encoding to UTF-8, before any other filter tries to parse
 * the request. Also set the X-UA-Compatible meta for browsers.
 * <p>
 * See NXP-5555: the first parsing of the request is cached, so it should be
 * done with the right encoding.
 * See NXP-12862: we must pass the X-UA-Compatible meta in the header.
 *
 * @author Anahide Tchertchian
 * @since 5.4.2
 */
public class NuxeoEncodingFilter implements Filter {

    private static final Log log = LogFactory.getLog(NuxeoEncodingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if (request != null) {
            // NXP-5555: set encoding to UTF-8 in case this method is called
            // before
            // encoding is set to UTF-8 on the request
            if (request.getCharacterEncoding() == null) {
                try {
                    request.setCharacterEncoding("UTF-8");
                } catch (UnsupportedEncodingException e) {
                    log.error(e, e);
                }
            }
            String requestURI = ((HttpServletRequest)request).getRequestURI();
            String gcfMeta = ",chrome=1";
            if (requestURI != null && requestURI.contains("login.jsp")) {
                gcfMeta = "";
            }
            if (response instanceof HttpServletResponse
                    && !((HttpServletResponse) response).containsHeader("X-UA-Compatible")) {
                ((HttpServletResponse) response).addHeader("X-UA-Compatible",
                        "IE=Edge" + gcfMeta);
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

}
