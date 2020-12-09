/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Martins
 */
package org.nuxeo.ecm.platform.web.common.external;

import static org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants.REQUESTED_URL;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.runtime.api.Framework;

/**
 * Filter that checks if the current request was called from an external tool (MS Office for instance)
 * <p>
 * Then if a session was already opened in user browser, it automatically redirects to the requested URL
 *
 * @author Thierry Martins
 * @since 5.6
 */
public class ExternalRequestFilter implements Filter {

    private static final Log log = LogFactory.getLog(ExternalRequestFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        /*
         * Check if login page was accessed after a redirection and if a Nuxeo session has been started
         */
        if (request != null && httpRequest.getParameter(NXAuthConstants.REQUESTED_URL) != null) {
            HttpSession httpSession = httpRequest.getSession(false);
            if (httpSession != null && httpSession.getAttribute(NXAuthConstants.USERIDENT_KEY) != null) {

                log.debug("Detect redirection while an active session is running");

                String requestedUrl = httpRequest.getParameter(REQUESTED_URL);
                if (requestedUrl != null && !"".equals(requestedUrl)) {
                    try {
                        requestedUrl = URLDecoder.decode(requestedUrl, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        log.error("Unable to get the requestedUrl parameter" + e);
                    }
                }

                if (requestedUrl != null) {
                    PluggableAuthenticationService service = Framework.getService(PluggableAuthenticationService.class);
                    String baseURL = service.getBaseURL(request);
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.sendRedirect(baseURL + requestedUrl);
                    return;
                }
            }
        }

        chain.doFilter(request, response);

    }

    @Override
    public void destroy() {
    }

}
