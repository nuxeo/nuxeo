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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OAuthRequestAuthenticator;
import org.keycloak.adapters.spi.HttpFacade;

/**
 * Extends {@link org.keycloak.adapters.RequestAuthenticator} to allow overriding methods calling removed deprecated
 * APIs.
 *
 * @since 2023.50
 */
public abstract class RequestAuthenticator extends org.keycloak.adapters.RequestAuthenticator {

    private static final Logger log = LogManager.getLogger(RequestAuthenticator.class);

    public RequestAuthenticator(HttpFacade facade, KeycloakDeployment deployment, AdapterTokenStore tokenStore,
            int sslRedirectPort) {
        super(facade, deployment, tokenStore, sslRedirectPort);
    }

    public RequestAuthenticator(HttpFacade facade, KeycloakDeployment deployment) {
        super(facade, deployment);
    }

    /**
     * Calls overridden {@link BearerTokenRequestAuthenticator} instead of
     * {@link org.keycloak.adapters.BearerTokenRequestAuthenticator}.
     */
    @Override
    protected org.keycloak.adapters.BearerTokenRequestAuthenticator createBearerTokenAuthenticator() {
        return new BearerTokenRequestAuthenticator(deployment);
    }

    /**
     * Calls overridden {@link RefreshableKeycloakSecurityContext} instead of
     * {@link org.keycloak.adapters.RefreshableKeycloakSecurityContext}.
     */
    @Override
    protected void completeAuthentication(OAuthRequestAuthenticator oauth) {
        org.keycloak.adapters.RefreshableKeycloakSecurityContext session = new RefreshableKeycloakSecurityContext(
                deployment, tokenStore, oauth.getTokenString(), oauth.getToken(), oauth.getIdTokenString(),
                oauth.getIdToken(), oauth.getRefreshToken());
        final KeycloakPrincipal<org.keycloak.adapters.RefreshableKeycloakSecurityContext> principal = new KeycloakPrincipal<>(
                AdapterUtils.getPrincipalName(deployment, oauth.getToken()), session);
        completeOAuthAuthentication(principal);
        log.debug("User ''{}'' invoking ''{}'' on client ''{}''", principal::getName,
                () -> facade.getRequest().getURI(), deployment::getResourceName);
    }

    /**
     * Calls overridden {@link RefreshableKeycloakSecurityContext} instead of
     * {@link org.keycloak.adapters.RefreshableKeycloakSecurityContext}.
     */
    @Override
    protected void completeAuthentication(org.keycloak.adapters.BearerTokenRequestAuthenticator bearer, String method) {
        org.keycloak.adapters.RefreshableKeycloakSecurityContext session = new RefreshableKeycloakSecurityContext(
                deployment, null, bearer.getTokenString(), bearer.getToken(), null, null, null);
        final KeycloakPrincipal<org.keycloak.adapters.RefreshableKeycloakSecurityContext> principal = new KeycloakPrincipal<>(
                AdapterUtils.getPrincipalName(deployment, bearer.getToken()), session);
        completeBearerAuthentication(principal, method);
        log.debug("User ''{}'' invoking ''{}'' on client ''{}''", principal::getName,
                () -> facade.getRequest().getURI(), deployment::getResourceName);
    }

}
