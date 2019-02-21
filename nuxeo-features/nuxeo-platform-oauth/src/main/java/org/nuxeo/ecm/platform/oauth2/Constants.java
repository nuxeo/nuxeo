/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.Arrays;
import java.util.List;

/**
 * @since 9.2
 */
public final class Constants {

    private Constants() {
        // constants class
    }

    public static final String TOKEN_SERVICE = "org.nuxeo.server.token.store";

    public static final String RESPONSE_TYPE_PARAM = "response_type";

    public static final String CODE_RESPONSE_TYPE = "code";

    public static final String SCOPE_PARAM = "scope";

    public static final String STATE_PARAM = "state";

    public static final String CLIENT_ID_PARAM = "client_id";

    public static final String CLIENT_SECRET_PARAM = "client_secret";

    public static final String REDIRECT_URI_PARAM = "redirect_uri";

    public static final String REDIRECT_URL_PARAM = "redirect_url";

    public static final String AUTHORIZATION_CODE_PARAM = "code";

    public static final String REFRESH_TOKEN_PARAM = "refresh_token";

    public static final String GRANT_TYPE_PARAM = "grant_type";

    /** @since 11.1 */
    public static final String ASSERTION_PARAM = "assertion";

    public static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";

    public static final String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";

    /** @since 11.1 */
    public static final String JWT_BEARER_GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";

    /** --------------------------- PKCE --------------------------- */
    public static final String CODE_CHALLENGE_PARAM = "code_challenge";

    public static final String CODE_CHALLENGE_METHOD_PARAM = "code_challenge_method";

    public static final String CODE_VERIFIER_PARAM = "code_verifier";

    public static final String CODE_CHALLENGE_METHOD_PLAIN = "plain";

    public static final String CODE_CHALLENGE_METHOD_S256 = "S256";

    public static final List<String> CODE_CHALLENGE_METHODS_SUPPORTED = Arrays.asList(CODE_CHALLENGE_METHOD_PLAIN,
            CODE_CHALLENGE_METHOD_S256);

}
