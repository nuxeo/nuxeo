/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.ui.web.auth.ntlm;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Manage NTLM "Protected POST" see : http://jcifs.samba.org/src/docs/ntlmhttpauth.html
 * http://curl.haxx.se/rfc/ntlm.html
 *
 * @author Thierry Delprat
 */
public class NTLMPostFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            if ("POST".equals(httpRequest.getMethod())) {
                String ntlmHeader = httpRequest.getHeader("Authorization");
                if (ntlmHeader != null && ntlmHeader.startsWith("NTLM") && httpRequest.getContentLength() == 0) {
                    handleNtlmPost(httpRequest, (HttpServletResponse) response, ntlmHeader);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    protected void handleNtlmPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String ntlmHeader)
            throws IOException, ServletException {
        NTLMAuthenticator.negotiate(httpRequest, httpResponse, true);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // NOP
    }

    @Override
    public void destroy() {
        // NOP
    }

}
