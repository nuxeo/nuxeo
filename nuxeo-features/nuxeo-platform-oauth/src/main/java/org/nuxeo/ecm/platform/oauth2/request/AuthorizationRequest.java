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

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.platform.oauth2.OAuth2Error;
import org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public class AuthorizationRequest extends Oauth2Request {
    private static final Log log = LogFactory.getLog(AuthorizationRequest.class);

    protected static Map<String, AuthorizationRequest> requests = new ConcurrentHashMap<>();

    protected String responseType;

    protected String scope;

    protected String state;

    protected String sessionId;

    protected Date creationDate;

    protected String authorizationCode;

    protected String authorizationKey;

    protected String username;

    public static final String RESPONSE_TYPE = "response_type";

    public static final String SCOPE = "scope";

    public static final String STATE = "state";

    public AuthorizationRequest() {
    }

    public AuthorizationRequest(HttpServletRequest request) {
        super(request);
        responseType = request.getParameter(RESPONSE_TYPE);

        scope = request.getParameter(SCOPE);
        state = request.getParameter(STATE);
        sessionId = request.getSession(true).getId();

        creationDate = new Date();
        authorizationKey = RandomStringUtils.random(6, true, false);
    }

    public OAuth2Error checkError() {
        // Check mandatory fields
        if (isBlank(responseType) || isBlank(clientId) || isBlank(redirectUri)) {
            return OAuth2Error.INVALID_REQUEST;
        }

        // Check if client exists
        try {
            ClientRegistry registry = Framework.getLocalService(ClientRegistry.class);
            if (!registry.hasClient(clientId)) {
                return OAuth2Error.UNAUTHORIZED_CLIENT;
            }
        } catch (DirectoryException e) {
            log.warn(e, e);
            return OAuth2Error.SERVER_ERROR;
        }

        // Check request type
        if (!"code".equals(responseType)) {
            return OAuth2Error.UNSUPPORTED_RESPONSE_TYPE;
        }
        return null;
    }

    public boolean isExpired() {
        // RFC 4.1.2, Authorization code lifetime is 10
        return new Date().getTime() - creationDate.getTime() > 10 * 60 * 1000;
    }

    public boolean isValidState(HttpServletRequest request) {
        return isBlank(getState()) || request.getParameter(STATE).equals(getState());
    }

    public String getUsername() {
        return username;
    }

    public String getResponseType() {
        return responseType;
    }

    public String getScope() {
        return scope;
    }

    public String getState() {
        return state;
    }

    public String getAuthorizationCode() {
        if (isBlank(authorizationCode)) {
            authorizationCode = RandomStringUtils.random(10, true, true);
        }
        return authorizationCode;
    }

    public String getAuthorizationKey() {
        return authorizationKey;
    }

    private static void deleteExpiredRequests() {
        Iterator<AuthorizationRequest> iterator = requests.values().iterator();
        AuthorizationRequest req;
        while (iterator.hasNext() && (req = iterator.next()) != null) {
            if (req.isExpired()) {
                requests.remove(req.sessionId);
            }
        }
    }

    public static AuthorizationRequest from(HttpServletRequest request) throws UnsupportedEncodingException {
        deleteExpiredRequests();

        String sessionId = request.getSession(true).getId();
        if (requests.containsKey(sessionId)) {
            AuthorizationRequest authRequest = requests.get(sessionId);
            if (!authRequest.isExpired() && authRequest.isValidState(request)) {
                return authRequest;
            }
        }

        AuthorizationRequest authRequest = new AuthorizationRequest(request);
        requests.put(sessionId, authRequest);
        return authRequest;
    }

    public static AuthorizationRequest fromCode(String authorizationCode) {
        for (AuthorizationRequest auth : requests.values()) {
            if (auth.authorizationCode != null && auth.authorizationCode.equals(authorizationCode)) {
                if (auth.sessionId != null) {
                    requests.remove(auth.sessionId);
                }
                return auth.isExpired() ? null : auth;
            }
        }
        return null;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
