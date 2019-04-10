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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

public class RedirectFilter extends BaseApiDocFilter {

    protected boolean isUriValidForAnonymous(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.contains("/nxpath/")) {
            return false;
        }
        if (uri.contains("/nxdoc/")) {
            return false;
        }
        if (uri.contains(".faces")) {
            return false;
        }
        if (uri.contains(".xhtml")) {
            return false;
        }
        return true;
    }

    protected void redirectToWebEngineView(HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) throws IOException {
        String base = VirtualHostHelper.getBaseURL(httpRequest);
        String location = base + "site/distribution/";
        httpResponse.sendRedirect(location);
    }

    @Override
    protected void internalDoFilter(ServletRequest request,
            ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        NuxeoPrincipal nxUser = (NuxeoPrincipal) httpRequest.getUserPrincipal();

        if (nxUser != null && nxUser.isAnonymous()) {
            if (!isUriValidForAnonymous(httpRequest)) {
                redirectToWebEngineView(httpRequest, httpResponse);
            }
        }

        chain.doFilter(httpRequest, httpResponse);
    }

}
