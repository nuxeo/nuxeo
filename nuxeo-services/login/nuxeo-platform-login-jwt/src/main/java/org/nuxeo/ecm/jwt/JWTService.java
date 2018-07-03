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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.jwt;

import java.util.Map;

/**
 * The JSON Web Token (JWT) Service.
 *
 * @since 10.3
 */
public interface JWTService {

    /**
     * A builder for a JSON Web Token (JWT).
     *
     * @since 10.3
     */
    interface JWTBuilder {

        /**
         * Adds a TTL (in seconds) to the token to be built. This may be capped by the service configuration.
         *
         * @param ttlSeconds the TTL, in seconds
         */
        JWTBuilder withTTL(int ttlSeconds);

        /**
         * Adds a claim to the token to be built. The standard claim names are available in {@link JWTClaims}.
         *
         * @param name the claim name
         * @param value the claim value
         */
        JWTBuilder withClaim(String name, Object value);

        /**
         * Builds and returns the token.
         * <p>
         * The {@link JWTClaims#CLAIM_SUBJECT} of the token is set to the current user id. The
         * {@link JWTClaims#CLAIM_ISSUER} of the token is set to the string {@code "nuxeo"}.
         * <p>
         * The token hash algorithm is based on a secret provided by the service configuration.
         *
         * @return the token
         */
        String build();

    }

    /**
     * Creates a new builder for a JSON Web Token.
     *
     * @return the new builder
     */
    JWTBuilder newBuilder();

    /**
     * Verifies the token and returns its claims, or {@code null} if the token is invalid (corrupted, constructed from
     * an invalid secret, or expired).
     * <p>
     * The claim {@link JWTClaims#CLAIM_SUBJECT} contains the token's creator user id.
     * <p>
     * The token hash algorithm is based on a secret provided by the service configuration.
     *
     * @param token the token
     * @return the claims if the token is valid, or {@code null} if the token is invalid
     */
    Map<String, Object> verifyToken(String token);

}
