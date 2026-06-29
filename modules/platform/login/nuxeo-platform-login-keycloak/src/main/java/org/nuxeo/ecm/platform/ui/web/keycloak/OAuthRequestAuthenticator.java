/*
 * (C) Copyright 2026 Nuxeo (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.ui.web.keycloak;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.catalina.connector.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OIDCAuthenticationError;
import org.keycloak.adapters.ServerRequest;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.adapters.spi.AdapterSessionStore;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.common.VerificationException;
import org.keycloak.enums.TokenStore;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AccessTokenResponse;

/**
 * Extends {@link org.keycloak.adapters.OAuthRequestAuthenticator} to allow overriding methods calling removed
 * deprecated APIs.
 *
 * @since 2023.50
 */
public class OAuthRequestAuthenticator extends org.keycloak.adapters.OAuthRequestAuthenticator {

    private static final Logger log = LogManager.getLogger(OAuthRequestAuthenticator.class);

    protected Request request;

    public OAuthRequestAuthenticator(RequestAuthenticator requestAuthenticator, HttpFacade facade,
            KeycloakDeployment deployment, int sslRedirectPort, AdapterSessionStore tokenStore, Request request) {
        super(requestAuthenticator, facade, deployment, sslRedirectPort, tokenStore);
        this.request = request;
    }

    /**
     * Calls {@link AccessToken#getIat()} instead of removed deprecated {@code getIssuedAt()}.
     */
    @Override
    protected AuthChallenge resolveCode(String code) {
        // abort if not HTTPS
        if (!isRequestSecure() && deployment.getSslRequired().isRequired(facade.getRequest().getRemoteAddr())) {
            log.error("Adapter requires SSL. Request: {}", () -> facade.getRequest().getURI());
            return challenge(403, OIDCAuthenticationError.Reason.SSL_REQUIRED, null);
        }

        log.debug("checking state cookie for after code");
        AuthChallenge challenge = checkStateCookie();
        if (challenge != null) {
            return challenge;
        }

        AccessTokenResponse tokenResponse = null;
        strippedOauthParametersRequestUri = rewrittenRedirectUri(stripOauthParametersFromRedirect());

        try {
            // For COOKIE store we don't have httpSessionId and single sign-out won't be available
            String httpSessionId = deployment.getTokenStore() == TokenStore.SESSION ? changeHttpSessionId(request, true)
                    : null;
            tokenResponse = ServerRequest.invokeAccessCodeToToken(deployment, code, strippedOauthParametersRequestUri,
                    httpSessionId);
        } catch (ServerRequest.HttpFailure failure) {
            log.error("failed to turn code into token");
            log.error("status from server: {}", failure::getStatus);
            if (failure.getError() != null && !failure.getError().trim().isEmpty()) {
                log.error("   {}", failure::getError);
            }
            return challenge(403, OIDCAuthenticationError.Reason.CODE_TO_TOKEN_FAILURE, null);

        } catch (IOException e) {
            log.error("failed to turn code into token", e);
            return challenge(403, OIDCAuthenticationError.Reason.CODE_TO_TOKEN_FAILURE, null);
        }

        tokenString = tokenResponse.getToken();
        refreshToken = tokenResponse.getRefreshToken();
        idTokenString = tokenResponse.getIdToken();

        log.debug("Verifying tokens");
        if (log.isTraceEnabled()) {
            logToken("\taccess_token", tokenString);
            logToken("\tid_token", idTokenString);
            logToken("\trefresh_token", refreshToken);
        }

        try {
            AdapterTokenVerifier.VerifiedTokens tokens = AdapterTokenVerifier.verifyTokens(tokenString, idTokenString,
                    deployment);
            token = tokens.getAccessToken();
            idToken = tokens.getIdToken();
            log.debug("Token Verification succeeded!");
        } catch (VerificationException e) {
            log.error("failed verification of token: {}", e::getMessage);
            return challenge(403, OIDCAuthenticationError.Reason.INVALID_TOKEN, null);
        }
        if (tokenResponse.getNotBeforePolicy() > deployment.getNotBefore()) {
            deployment.updateNotBefore(tokenResponse.getNotBeforePolicy());
        }
        if (token.getIat() < deployment.getNotBefore()) {
            log.error("Stale token");
            return challenge(403, OIDCAuthenticationError.Reason.STALE_TOKEN, null);
        }
        log.debug("successful authenticated");
        return null;
    }

    private String rewrittenRedirectUri(String originalUri) {
        Map<String, String> rewriteRules = deployment.getRedirectRewriteRules();
        if (rewriteRules != null && !rewriteRules.isEmpty()) {
            try {
                URL url = new URL(originalUri);
                Map.Entry<String, String> rule = rewriteRules.entrySet().iterator().next();
                StringBuilder redirectUriBuilder = new StringBuilder(url.getProtocol());
                redirectUriBuilder.append("://" + url.getAuthority());
                redirectUriBuilder.append(url.getPath().replaceFirst(rule.getKey(), rule.getValue()));
                return redirectUriBuilder.toString();
            } catch (MalformedURLException ex) {
                log.error("Not a valid request url");
                throw new RuntimeException(ex);
            }
        }
        return originalUri;
    }

    protected String changeHttpSessionId(Request request, boolean create) {
        HttpSession session = request.getSession(create);
        return session != null ? session.getId() : null;
    }

    private void logToken(String name, String token) {
        try {
            JWSInput jwsInput = new JWSInput(token);
            String wireString = jwsInput.getWireString();
            log.trace("\t{}: {}", () -> name,
                    () -> wireString.substring(0, wireString.lastIndexOf(".")) + ".signature");
        } catch (JWSInputException e) {
            log.error("Failed to parse {}: {}", name, token, e);
        }
    }

}
