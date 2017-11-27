/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.web.ui;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.platform.url.api.DocumentView;
import org.nuxeo.ecm.platform.url.api.DocumentViewCodecManager;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 9.10
 */
public class NuxeoJSFWebUIRedirectFilter implements Filter {

    public static final String WEB_UI_CODEC_NAME = "webUIRedirect";

    @Override
    public void init(FilterConfig filterConfig) {
        // do nothing
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        DocumentViewCodecManager docViewManager = Framework.getService(DocumentViewCodecManager.class);
        String requestURL = httpRequest.getRequestURL().toString();
        String baseURL = VirtualHostHelper.getBaseURL(request);
        DocumentView docView = docViewManager.getDocumentViewFromUrl(requestURL, true, baseURL);
        if (docView != null) {
            String url = docViewManager.getUrlFromDocumentView(WEB_UI_CODEC_NAME, docView, true, baseURL);
            if (url != null) {
                httpResponse.sendRedirect(url);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
