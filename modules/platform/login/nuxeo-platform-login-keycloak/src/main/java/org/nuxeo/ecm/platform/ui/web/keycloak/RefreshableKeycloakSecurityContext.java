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

import org.keycloak.adapters.AdapterTokenStore;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.common.util.Time;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

/**
 * Extends {@link org.keycloak.adapters.RefreshableKeycloakSecurityContext} to allow overriding methods calling removed
 * deprecated APIs.
 *
 * @since 2023.50
 */
public class RefreshableKeycloakSecurityContext extends org.keycloak.adapters.RefreshableKeycloakSecurityContext {

    private static final long serialVersionUID = 1L;

    public RefreshableKeycloakSecurityContext(KeycloakDeployment deployment, AdapterTokenStore tokenStore,
            String tokenString, AccessToken token, String idTokenString, IDToken idToken, String refreshToken) {
        super(deployment, tokenStore, tokenString, token, idTokenString, idToken, refreshToken);
    }

    /**
     * Calls {@link AccessToken#getIat()} instead of removed deprecated {@code getIssuedAt()}.
     */
    @Override
    public boolean isActive() {
        return token != null && this.token.isActive() && deployment != null
                && this.token.getIat() >= deployment.getNotBefore();
    }

    /**
     * Calls {@link AccessToken#getExp()} instead of removed deprecated {@code getExpiration()}.
     */
    @Override
    public boolean isTokenTimeToLiveSufficient(AccessToken token) {
        return token != null && (token.getExp() - this.deployment.getTokenMinimumTimeToLive()) > Time.currentTime();
    }

}
