/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Manage NTLM "Protected POST"
 *
 * see : http://jcifs.samba.org/src/docs/ntlmhttpauth.html
 *       http://curl.haxx.se/rfc/ntlm.html
 *
 * @author Thierry Delprat
 */
public class NTLMPostFilter implements Filter {

    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            if ("POST".equals(httpRequest.getMethod())) {
                String ntlmHeader = httpRequest.getHeader("Authorization");
                if (ntlmHeader!=null && ntlmHeader.startsWith("NTLM") && httpRequest.getContentLength()==0) {
                    handleNtlmPost(httpRequest, (HttpServletResponse) response, ntlmHeader);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }

    protected void handleNtlmPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String ntlmHeader) throws IOException, ServletException {
        NTLMAuthenticator.negotiate(httpRequest, httpResponse, true);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // NOP
    }

    public void destroy() {
        // NOP
    }

}
