/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *
 */
package org.nuxeo.ecm.platform.oauth2;

import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.nuxeo.ecm.platform.oauth2.Constants.TOKEN_SERVICE;

import java.io.IOException;
import java.security.Principal;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2ClientService;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoSecuredRequestWrapper;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthPreFilter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public class NuxeoOAuth2Filter implements NuxeoAuthPreFilter {

    private static final Log log = LogFactory.getLog(NuxeoOAuth2Filter.class);

    public static final String ACCESS_TOKEN_PARAM = "access_token";

    public static final String AUTHORIZATION_HEADER = "Authorization";

    public static final String BEARER_AUTHENTICATION_SCHEME = "Bearer ";

    protected OAuth2TokenStore tokenStore = new OAuth2TokenStore(TOKEN_SERVICE);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String accessToken = getAccessToken(httpRequest);
        if (accessToken != null) {
            processAuthentication(accessToken, httpRequest, httpResponse, chain);
        } else {
            chain.doFilter(request, response);
        }
    }

    protected void processAuthentication(String accessToken, HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        NuxeoOAuth2Token token = TransactionHelper.runInTransaction(() -> tokenStore.getToken(accessToken));

        OAuth2ClientService clientService = Framework.getService(OAuth2ClientService.class);
        if (token == null || token.isExpired() || !clientService.hasClient(token.getClientId())) {
            response.setStatus(SC_UNAUTHORIZED);
            return;
        }

        LoginContext loginContext = buildLoginContext(token);
        if (loginContext != null) {
            Principal principal = (Principal) loginContext.getSubject().getPrincipals().toArray()[0];
            try {
                chain.doFilter(new NuxeoSecuredRequestWrapper(request, principal), response);
            } finally {
                try {
                    loginContext.logout();
                } catch (LoginException e) {
                    log.warn("Error when logging out", e);
                }
            }
        }
    }

    protected String getAccessToken(HttpServletRequest request) {
        String accessToken = request.getParameter(ACCESS_TOKEN_PARAM);
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        return StringUtils.isNotBlank(accessToken) ? accessToken
                : authorization != null && authorization.startsWith(BEARER_AUTHENTICATION_SCHEME)
                        ? authorization.substring(BEARER_AUTHENTICATION_SCHEME.length()).trim() : null;
    }

    protected LoginContext buildLoginContext(NuxeoOAuth2Token token) {
        try {
            return NuxeoAuthenticationFilter.loginAs(token.getNuxeoLogin());
        } catch (LoginException e) {
            log.warn("Error while authenticate user");
        }
        return null;
    }

}
