/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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

/**
 * @since 9.2
 */
public class OAuth2Error {

    public static final String INVALID_REQUEST = "invalid_request";

    public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";

    public static final String ACCESS_DENIED = "access_denied";

    public static final String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";

    public static final String INVALID_SCOPE = "invalid_scope";

    public static final String SERVER_ERROR = "server_error";

    public static final String TEMPORARILY_UNAVAILABLE = "temporarily_unavailable";

    public static final String INVALID_CLIENT = "invalid_client";

    public static final String INVALID_GRANT = "invalid_grant";

    public static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";

    protected final String id;

    protected final String description;

    protected OAuth2Error(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static OAuth2Error invalidRequest(String description) {
        return new OAuth2Error(INVALID_REQUEST, description);
    }

    public static OAuth2Error invalidRequest() {
        return invalidRequest(null);
    }

    public static OAuth2Error unauthorizedClient(String description) {
        return new OAuth2Error(UNAUTHORIZED_CLIENT, description);
    }

    public static OAuth2Error unauthorizedClient() {
        return unauthorizedClient(null);
    }

    public static OAuth2Error accessDenied(String description) {
        return new OAuth2Error(ACCESS_DENIED, description);
    }

    public static OAuth2Error accessDenied() {
        return accessDenied(null);
    }

    public static OAuth2Error unsupportedResponseType(String description) {
        return new OAuth2Error(UNSUPPORTED_RESPONSE_TYPE, description);
    }

    public static OAuth2Error unsupportedResponseType() {
        return unsupportedResponseType(null);
    }

    public static OAuth2Error invalidScope(String description) {
        return new OAuth2Error(INVALID_SCOPE, description);
    }

    public static OAuth2Error invalidScope() {
        return invalidScope(null);
    }

    public static OAuth2Error serverError(String description) {
        return new OAuth2Error(SERVER_ERROR, description);
    }

    public static OAuth2Error serverError() {
        return serverError(null);
    }

    public static OAuth2Error temporarilyUnavailable(String description) {
        return new OAuth2Error(TEMPORARILY_UNAVAILABLE, description);
    }

    public static OAuth2Error temporarilyUnavailable() {
        return temporarilyUnavailable(null);
    }

    public static OAuth2Error invalidClient(String description) {
        return new OAuth2Error(INVALID_CLIENT, description);
    }

    public static OAuth2Error invalidClient() {
        return invalidClient(null);
    }

    public static OAuth2Error invalidGrant(String description) {
        return new OAuth2Error(INVALID_GRANT, description);
    }

    public static OAuth2Error invalidGrant() {
        return invalidGrant(null);
    }

    public static OAuth2Error unsupportedGrantType(String description) {
        return new OAuth2Error(UNSUPPORTED_GRANT_TYPE, description);
    }

    public static OAuth2Error unsupportedGrantType() {
        return unsupportedGrantType(null);
    }

    @Override
    public String toString() {
        return String.format("%s(id=%s, description=%s)", getClass().getSimpleName(), id, description);
    }
}
