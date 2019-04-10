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
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.wss.servlet.config.FilterBindingConfig;

/**
 * Root filter that must handle requests sent directly on /. Outside of the OPTIONS calls, all other calls are forwarded
 * to the backend filter.
 */
public class WSSFrontFilter extends BaseWSSFilter implements Filter {

    @Override
    protected void handleWSSCall(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            FilterBindingConfig config) {
        throw new UnsupportedOperationException(
                "This filter is not intended to receive actual WSS calls, check your configuration");
    }

    @Override
    protected void doForward(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
            FilterBindingConfig config) throws ServletException, IOException {
        // To forward to the backend filter, we need to change context
        // but on some App Server (ex: Tomcat 6) default config prohibit this
        ServletContext targetContext = ctx.getContext(getRootFilterTarget());
        if (targetContext != null) {
            targetContext.getRequestDispatcher(httpRequest.getRequestURI()).forward(httpRequest, httpResponse);
        } else {
            String newTarget = getRootFilterTarget() + httpRequest.getRequestURI() + "?" + httpRequest.getQueryString();
            httpResponse.sendRedirect(newTarget);
        }
    }

    @Override
    protected boolean isRootFilter() {
        String target = filterConfig.getInitParameter(ROOT_FILTER_PARAM);
        if (target != null && !"".equals(target)) {
            rootFilterTarget = target;
        } else {
            rootFilterTarget = System.getProperty("org.nuxeo.ecm.contextPath", "/nuxeo");
        }
        return true;
    }

    @Override
    protected void initBackend(FilterConfig filterConfig) {
        // No Backend to init
    }

    @Override
    protected void initHandlers(FilterConfig filterConfig) {
        // No Handlers to init

    }

}
