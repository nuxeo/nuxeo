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

/**
 * JSON Web Token (JWT) standard claims.
 *
 * @since 10.3
 */
public class JWTClaims {

    private JWTClaims() {
        // utility class
    }

    // header claims

    public static final String CLAIM_ALGORITHM = "alg";

    public static final String CLAIM_CONTENT_TYPE = "cty";

    public static final String CLAIM_KEY_ID = "kid";

    public static final String CLAIM_TYPE = "typ";

    // payload claims

    public static final String CLAIM_AUDIENCE = "aud";

    public static final String CLAIM_EXPIRES_AT = "exp";

    public static final String CLAIM_ISSUED_AT = "iat";

    public static final String CLAIM_ISSUER = "iss";

    public static final String CLAIM_JWT_ID = "jti";

    public static final String CLAIM_NOT_BEFORE = "nbf";

    public static final String CLAIM_SUBJECT = "sub";

}
