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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

import org.nuxeo.ecm.core.api.NuxeoException;

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

    // @since 2021.23
    protected final int statusCode;

    protected OAuth2Error(String id, String description) {
        this(id, description, SC_INTERNAL_SERVER_ERROR);
    }

    protected OAuth2Error(String id, String description, int statusCode) {
        this.id = id;
        this.description = description;
        this.statusCode = statusCode;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @since 2021.23
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @since 2021.23
     */
    public static OAuth2Error invalidRequest(String description, int statusCode) {
        return new OAuth2Error(INVALID_REQUEST, description, statusCode);
    }

    public static OAuth2Error invalidRequest(String description) {
        return invalidRequest(description, SC_BAD_REQUEST);
    }

    public static OAuth2Error invalidRequest() {
        return invalidRequest(null);
    }

    public static OAuth2Error unauthorizedClient(String description) {
        return new OAuth2Error(UNAUTHORIZED_CLIENT, description, SC_BAD_REQUEST);
    }

    public static OAuth2Error unauthorizedClient() {
        return unauthorizedClient(null);
    }

    public static OAuth2Error accessDenied(String description) {
        return new OAuth2Error(ACCESS_DENIED, description, SC_BAD_REQUEST);
    }

    public static OAuth2Error accessDenied() {
        return accessDenied(null);
    }

    public static OAuth2Error unsupportedResponseType(String description) {
        return new OAuth2Error(UNSUPPORTED_RESPONSE_TYPE, description, SC_BAD_REQUEST);
    }

    public static OAuth2Error unsupportedResponseType() {
        return unsupportedResponseType(null);
    }

    public static OAuth2Error invalidScope(String description) {
        return new OAuth2Error(INVALID_SCOPE, description, SC_BAD_REQUEST);
    }

    public static OAuth2Error invalidScope() {
        return invalidScope(null);
    }

    public static OAuth2Error serverError(String description) {
        return new OAuth2Error(SERVER_ERROR, description, SC_INTERNAL_SERVER_ERROR);
    }

    public static OAuth2Error serverError() {
        return serverError(null);
    }

    public static OAuth2Error temporarilyUnavailable(String description) {
        return new OAuth2Error(TEMPORARILY_UNAVAILABLE, description, SC_SERVICE_UNAVAILABLE);
    }

    public static OAuth2Error temporarilyUnavailable() {
        return temporarilyUnavailable(null);
    }

    public static OAuth2Error invalidClient(String description) {
        return new OAuth2Error(INVALID_CLIENT, description, SC_BAD_REQUEST);
    }

    public static OAuth2Error invalidClient() {
        return invalidClient(null);
    }

    public static OAuth2Error invalidGrant(String description) {
        return new OAuth2Error(INVALID_GRANT, description, SC_BAD_REQUEST);
    }

    public static OAuth2Error invalidGrant() {
        return invalidGrant(null);
    }

    public static OAuth2Error unsupportedGrantType(String description) {
        return new OAuth2Error(UNSUPPORTED_GRANT_TYPE, description, SC_BAD_REQUEST);
    }

    public static OAuth2Error unsupportedGrantType() {
        return unsupportedGrantType(null);
    }

    /**
     * @since 2021.23
     */
    public static OAuth2Error from(NuxeoException e) {
        return new OAuth2Error(SERVER_ERROR, e.getMessage(), e.getStatusCode());
    }

    @Override
    public String toString() {
        return String.format("%s(id=%s, description=%s)", getClass().getSimpleName(), id, description);
    }
}
