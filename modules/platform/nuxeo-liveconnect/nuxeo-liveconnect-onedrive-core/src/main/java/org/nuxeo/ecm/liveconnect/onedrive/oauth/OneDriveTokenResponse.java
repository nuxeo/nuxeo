/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.liveconnect.onedrive.oauth;

import com.google.api.client.auth.oauth2.RefreshTokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonString;
import com.google.api.client.util.Key;
import com.google.api.client.util.Preconditions;

/**
 * We need this class for oauth with OneDrive as they don't return {@code expiresInSeconds} in number format. See
 * https://github.com/google/google-oauth-java-client/issues/62. Copy of {@link TokenResponse}.
 *
 * @since 8.2
 */
public class OneDriveTokenResponse extends GenericJson {

    /** Access token issued by the authorization server. */
    @Key("access_token")
    private String accessToken;

    /**
     * Token type (as specified in <a href="http://tools.ietf.org/html/rfc6749#section-7.1">Access
     * Token Types</a>).
     */
    @Key("token_type")
    private String tokenType;

    /**
     * Lifetime in seconds of the access token (for example 3600 for an hour) or {@code null} for
     * none.
     */
    @Key("expires_in")
    @JsonString
    private Long expiresInSeconds;

    /**
     * Refresh token which can be used to obtain new access tokens using {@link RefreshTokenRequest}
     * or {@code null} for none.
     */
    @Key("refresh_token")
    private String refreshToken;

    /**
     * Scope of the access token as specified in <a
     * href="http://tools.ietf.org/html/rfc6749#section-3.3">Access Token Scope</a> or {@code null}
     * for none.
     */
    @Key
    private String scope;

    /** Returns the access token issued by the authorization server. */
    public final String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the access token issued by the authorization server.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public OneDriveTokenResponse setAccessToken(String accessToken) {
        this.accessToken = Preconditions.checkNotNull(accessToken);
        return this;
    }

    /**
     * Returns the token type (as specified in <a
     * href="http://tools.ietf.org/html/rfc6749#section-7.1">Access Token Types</a>).
     */
    public final String getTokenType() {
        return tokenType;
    }

    /**
     * Sets the token type (as specified in <a
     * href="http://tools.ietf.org/html/rfc6749#section-7.1">Access Token Types</a>).
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public OneDriveTokenResponse setTokenType(String tokenType) {
        this.tokenType = Preconditions.checkNotNull(tokenType);
        return this;
    }

    /**
     * Returns the lifetime in seconds of the access token (for example 3600 for an hour) or
     * {@code null} for none.
     */
    public final Long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    /**
     * Sets the lifetime in seconds of the access token (for example 3600 for an hour) or {@code null}
     * for none.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public OneDriveTokenResponse setExpiresInSeconds(Long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
        return this;
    }

    /**
     * Returns the refresh token which can be used to obtain new access tokens using the same
     * authorization grant or {@code null} for none.
     */
    public final String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Sets the refresh token which can be used to obtain new access tokens using the same
     * authorization grant or {@code null} for none.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public OneDriveTokenResponse setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    /**
     * Returns the scope of the access token or {@code null} for none.
     */
    public final String getScope() {
        return scope;
    }

    /**
     * Sets the scope of the access token or {@code null} for none.
     *
     * <p>
     * Overriding is only supported for the purpose of calling the super implementation and changing
     * the return type, but nothing else.
     * </p>
     */
    public OneDriveTokenResponse setScope(String scope) {
        this.scope = scope;
        return this;
    }

    @Override
    public OneDriveTokenResponse set(String fieldName, Object value) {
        return (OneDriveTokenResponse) super.set(fieldName, value);
    }

    @Override
    public OneDriveTokenResponse clone() {
        return (OneDriveTokenResponse) super.clone();
    }

    public TokenResponse toTokenResponse() {
        TokenResponse response = new TokenResponse();
        response.setAccessToken(getAccessToken());
        response.setTokenType(getTokenType());
        response.setExpiresInSeconds(getExpiresInSeconds());
        response.setRefreshToken(getRefreshToken());
        response.setScope(getScope());
        return response;
    }

}
