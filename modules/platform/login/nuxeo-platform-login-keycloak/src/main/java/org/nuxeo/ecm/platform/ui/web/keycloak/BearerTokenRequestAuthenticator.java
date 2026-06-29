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

import javax.security.cert.X509Certificate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OIDCAuthenticationError;
import org.keycloak.adapters.rotation.AdapterTokenVerifier;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.common.VerificationException;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.representations.AccessToken;

/**
 * Extends {@link org.keycloak.adapters.BearerTokenRequestAuthenticator} to allow overriding methods calling removed
 * deprecated APIs.
 *
 * @since 2023.50
 */
public class BearerTokenRequestAuthenticator extends org.keycloak.adapters.BearerTokenRequestAuthenticator {

    private static final Logger log = LogManager.getLogger(BearerTokenRequestAuthenticator.class);

    public BearerTokenRequestAuthenticator(KeycloakDeployment deployment) {
        super(deployment);
    }

    /**
     * Calls {@link AccessToken#getIat()} instead of removed deprecated {@code getIssuedAt()}.
     */
    @Override
    protected AuthOutcome authenticateToken(HttpFacade exchange, String tokenString) {
        log.debug("Verifying access_token");
        if (log.isTraceEnabled()) {
            try {
                JWSInput jwsInput = new JWSInput(tokenString);
                String wireString = jwsInput.getWireString();
                log.trace("\taccess_token: {}",
                        () -> wireString.substring(0, wireString.lastIndexOf(".")) + ".signature");
            } catch (JWSInputException e) {
                log.error("Failed to parse access_token: {}", tokenString, e);
            }
        }
        try {
            token = AdapterTokenVerifier.verifyToken(tokenString, deployment);
        } catch (VerificationException e) {
            log.debug("Failed to verify token: {}", e::getMessage);
            challenge = challengeResponse(exchange, OIDCAuthenticationError.Reason.INVALID_TOKEN, "invalid_token",
                    e.getMessage());
            return AuthOutcome.FAILED;
        }
        if (token.getIat() < deployment.getNotBefore()) {
            log.debug("Stale token");
            challenge = challengeResponse(exchange, OIDCAuthenticationError.Reason.STALE_TOKEN, "invalid_token",
                    "Stale token");
            return AuthOutcome.FAILED;
        }
        boolean verifyCaller = false;
        if (deployment.isUseResourceRoleMappings()) {
            verifyCaller = token.isVerifyCaller(deployment.getResourceName());
        } else {
            verifyCaller = token.isVerifyCaller();
        }
        surrogate = null;
        if (verifyCaller) {
            if (token.getTrustedCertificates() == null || token.getTrustedCertificates().isEmpty()) {
                log.warn("No trusted certificates in token");
                challenge = clientCertChallenge();
                return AuthOutcome.FAILED;
            }

            // for now, we just make sure Undertow did two-way SSL
            // assume JBoss Web verifies the client cert
            X509Certificate[] chain = new X509Certificate[0];
            try {
                chain = exchange.getCertificateChain();
            } catch (Exception ignore) {

            }
            if (chain == null || chain.length == 0) {
                log.warn("No certificates provided by undertow to verify the caller");
                challenge = clientCertChallenge();
                return AuthOutcome.FAILED;
            }
            surrogate = chain[0].getSubjectDN().getName();
        }
        log.debug("successful authorized");
        return AuthOutcome.AUTHENTICATED;
    }

}
