/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.oauth2.request;

import static org.nuxeo.ecm.platform.oauth2.Constants.ASSERTION_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.AUTHORIZATION_CODE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CLIENT_SECRET_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_VERIFIER_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.GRANT_TYPE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.REFRESH_TOKEN_PARAM;

import javax.servlet.http.HttpServletRequest;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public class TokenRequest extends OAuth2Request {

    protected static final String BASIC_AUTHENTICATION_HEADER_PREFIX = "basic ";

    protected String grantType;

    protected String code;

    protected String clientSecret;

    protected String refreshToken;

    protected String codeVerifier;

    protected String assertion;

    public TokenRequest(HttpServletRequest request) {
        super(request);
        grantType = request.getParameter(GRANT_TYPE_PARAM);
        code = request.getParameter(AUTHORIZATION_CODE_PARAM);
        clientSecret = request.getParameter(CLIENT_SECRET_PARAM);
        refreshToken = request.getParameter(REFRESH_TOKEN_PARAM);
        codeVerifier = request.getParameter(CODE_VERIFIER_PARAM);
        assertion = request.getParameter(ASSERTION_PARAM);

        checkAuthorization(request);
    }

    protected void checkAuthorization(HttpServletRequest request) {
        final String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.toLowerCase().startsWith(BASIC_AUTHENTICATION_HEADER_PREFIX)) {
            // Authorization: Basic base64credentials
            String base64Credentials = authorization.substring(BASIC_AUTHENTICATION_HEADER_PREFIX.length()).trim();
            byte[] decodedCredentials = java.util.Base64.getDecoder().decode(base64Credentials);
            String credentials = new String(decodedCredentials, java.nio.charset.StandardCharsets.UTF_8);
            // credentials = client_id:secret
            String[] values = credentials.split(":", 2);
            if (values.length == 2) {
                clientId = values[0];
                clientSecret = values[1];
            }
        }
    }

    public String getGrantType() {
        return grantType;
    }

    public String getCode() {
        return code;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getCodeVerifier() {
        return codeVerifier;
    }

    /**
     * @since 11.1
     */
    public String getAssertion() {
        return assertion;
    }
}
