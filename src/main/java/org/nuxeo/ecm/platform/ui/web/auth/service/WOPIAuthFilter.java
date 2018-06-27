/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.ui.web.auth.service;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthPreFilter;

/**
 * @since 10.3
 */
public class WOPIAuthFilter implements NuxeoAuthPreFilter {

    public static final String ACCESS_TOKEN_PARAM = "access_token";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = ((HttpServletRequest) request);
        String accessToken = request.getParameter(ACCESS_TOKEN_PARAM);
        String requestURI = httpRequest.getRequestURI();
        if (accessToken == null || !requestURI.contains("/wopi/")) {
            // not a WOPI URL // no access token
            chain.doFilter(request, response);
            return;
        }

        // make the access token available to the TokenAuthenticator through the "token" parameter
        HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(httpRequest) {
            @Override
            public String getParameter(String name) {
                if ("token".equals(name)) {
                    return accessToken;
                }
                return super.getParameter(name);
            }
        };
        // bypass other pre filters such as Oauth2
        NuxeoAuthFilterChain nuxeoAuthFilterChain = (NuxeoAuthFilterChain) chain;
        nuxeoAuthFilterChain.mainFilter.doFilterInternal(wrappedRequest, response,
                nuxeoAuthFilterChain.standardFilterChain);

    }

}
