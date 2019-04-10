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

    protected void redirectToWebEngineView(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws IOException {
        String base = VirtualHostHelper.getBaseURL(httpRequest);
        String location = base + "site/distribution/";
        httpResponse.sendRedirect(location);
    }

    @Override
    protected void internalDoFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

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
