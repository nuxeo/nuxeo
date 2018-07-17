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
 *     Arnaud Kervern
 *     Thomas Roger
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.oauth2;

import static java.lang.Boolean.FALSE;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.nuxeo.ecm.platform.oauth2.Constants.TOKEN_SERVICE;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2ClientService;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthenticationPlugin;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * OAuth2 Authentication Plugin.
 * <p>
 * This plugin chekcs the {@code access_token} request parameter or the {@code Authorization: Bearer} request header for
 * a valid OAuth2 token (checked with the {@link OAuth2ClientService}).
 *
 * @since 10.3
 */
public class NuxeoOAuth2Authenticator implements NuxeoAuthenticationPlugin {

    private static final Log log = LogFactory.getLog(NuxeoOAuth2Authenticator.class);

    public static final String ACCESS_TOKEN = "access_token";

    public static final String BEARER_SP = "Bearer ";

    protected OAuth2TokenStore tokenStore = new OAuth2TokenStore(TOKEN_SERVICE);

    @Override
    public void initPlugin(Map<String, String> parameters) {
        // nothing to init
    }

    @Override
    public List<String> getUnAuthenticatedURLPrefix() {
        return null; // NOSONAR
    }

    @Override
    public Boolean needLoginPrompt(HttpServletRequest httpRequest) {
        return FALSE;
    }

    @Override
    public Boolean handleLoginPrompt(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String baseURL) {
        return FALSE;
    }

    @Override
    public UserIdentificationInfo handleRetrieveIdentity(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = getAccessToken(request);
        if (accessToken == null) {
            log.trace("OAuth2 token not found");
            return null;
        }
        NuxeoOAuth2Token token = TransactionHelper.runInTransaction(() -> tokenStore.getToken(accessToken));
        OAuth2ClientService clientService = Framework.getService(OAuth2ClientService.class);
        if (token == null) {
            log.trace("OAuth2 token unknown");
            return null;
        }
        if (token.isExpired()) {
            log.trace("OAuth2 token expired");
            return null;
        }
        if (!clientService.hasClient(token.getClientId())) {
            if (log.isTraceEnabled()) {
                log.trace("OAuth2 token for unknown client: " + token.getClientId());
            }
            return null;
        }

        String username = token.getNuxeoLogin();
        log.trace("OAuth2 token found for user: " + username);
        return new UserIdentificationInfo(username, username);
    }

    protected String getAccessToken(HttpServletRequest request) {
        String accessToken = request.getParameter(ACCESS_TOKEN);
        if (StringUtils.isNotBlank(accessToken)) {
            log.trace("Found access_token request parameter");
            return accessToken;
        }
        String authorization = request.getHeader(AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_SP)) {
            log.trace("Found Authorization: Bearer request header");
            return authorization.substring(BEARER_SP.length()).trim();
        }
        return null;
    }

}
