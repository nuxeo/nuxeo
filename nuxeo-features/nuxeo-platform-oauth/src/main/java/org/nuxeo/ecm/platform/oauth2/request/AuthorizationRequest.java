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
import static org.nuxeo.ecm.platform.ui.web.auth.oauth2.NuxeoOAuth2Filter.ERRORS.invalid_request;
import static org.nuxeo.ecm.platform.ui.web.auth.oauth2.NuxeoOAuth2Filter.ERRORS.server_error;
import static org.nuxeo.ecm.platform.ui.web.auth.oauth2.NuxeoOAuth2Filter.ERRORS.unauthorized_client;
import static org.nuxeo.ecm.platform.ui.web.auth.oauth2.NuxeoOAuth2Filter.ERRORS.unsupported_response_type;

import java.io.Serializable;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public class AuthorizationRequest extends Oauth2Request {

    private static final Log log = LogFactory.getLog(AuthorizationRequest.class);

    /**
     * @deprecated since 8.10-HF34
     */
    @Deprecated
    protected static Map<String, AuthorizationRequest> requests = new ConcurrentHashMap<>();

    /**
     * @since 8.10-HF34
     */
    public static final String STORE_NAME = "authorizationRequestStore";

    protected String responseType;

    protected String scope;

    protected String state;

    /**
     * @deprecated since 8.10-HF34
     */
    @Deprecated
    protected String sessionId;

    protected Date creationDate;

    protected String authorizationCode;

    protected String authorizationKey;

    protected String username;

    public static final String RESPONSE_TYPE = "response_type";

    public static final String SCOPE = "scope";

    public static final String STATE = "state";

    public static AuthorizationRequest from(HttpServletRequest request) {
        return new AuthorizationRequest(request);
    }

    /**
     * @since 8.10-HF34
     */
    public static AuthorizationRequest fromMap(Map<String, Serializable> map) {
        return new AuthorizationRequest(map);
    }

    /**
     * @since 8.10-HF34
     */
    public static void store(String key, AuthorizationRequest authorizationRequest) {
        TransientStoreService transientStoreService = Framework.getService(TransientStoreService.class);
        TransientStore store = transientStoreService.getStore(STORE_NAME);
        store.putParameters(key, authorizationRequest.toMap());
    }

    public static AuthorizationRequest fromCode(String key) {
        TransientStoreService transientStoreService = Framework.getService(TransientStoreService.class);
        TransientStore store = transientStoreService.getStore(STORE_NAME);
        Map<String, Serializable> parameters = store.getParameters(key);
        if (parameters != null) {
            AuthorizationRequest authorizationRequest = AuthorizationRequest.fromMap(parameters);
            return authorizationRequest.isExpired() ? null : authorizationRequest;
        }
        return null;
    }

    /**
     * @since 8.10-HF34
     */
    public static void remove(String key) {
        TransientStoreService transientStoreService = Framework.getService(TransientStoreService.class);
        TransientStore store = transientStoreService.getStore(STORE_NAME);
        store.remove(key);
    }

    public AuthorizationRequest() {
    }

    public AuthorizationRequest(HttpServletRequest request) {
        super(request);
        responseType = request.getParameter(RESPONSE_TYPE);

        scope = request.getParameter(SCOPE);
        state = request.getParameter(STATE);

        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            username = principal.getName();
        }

        creationDate = new Date();
        authorizationKey = RandomStringUtils.random(6, true, false);
    }

    /**
     * @since 8.10-HF34
     */
    protected AuthorizationRequest(Map<String, Serializable> map) {
        clientId = (String) map.get("clientId");
        redirectUri = (String) map.get("redirectUri");
        responseType = (String) map.get("responseType");
        scope = (String) map.get("scope");
        state = (String) map.get("state");
        creationDate = (Date) map.get("creationDate");
        authorizationCode = (String) map.get("authorizationCode");
        authorizationKey = (String) map.get("authorizationKey");
        username = (String) map.get("username");
    }

    public String checkError() {
        // Check mandatory fields
        if (isBlank(responseType) || isBlank(clientId) || isBlank(redirectUri)) {
            return invalid_request.toString();
        }

        // Check if client exists
        try {
            ClientRegistry registry = Framework.getLocalService(ClientRegistry.class);
            if (!registry.hasClient(clientId)) {
                return unauthorized_client.toString();
            }
        } catch (DirectoryException e) {
            log.warn(e, e);
            return server_error.toString();
        }

        // Check request type
        if (!"code".equals(responseType)) {
            return unsupported_response_type.toString();
        }
        return null;
    }

    public String getResponseType() {
        return responseType;
    }

    public String getScope() {
        return scope;
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

    /**
     * @since 8.10-HF34
     */
    public Map<String, Serializable> toMap() {
        Map<String, Serializable> map = new HashMap<>();
        if (clientId != null) {
            map.put("clientId", clientId);
        }
        if (redirectUri != null) {
            map.put("redirectUri", redirectUri);
        }
        if (responseType != null) {
            map.put("responseType", responseType);
        }
        if (scope != null) {
            map.put("scope", scope);
        }
        if (state != null) {
            map.put("state", state);
        }
        if (creationDate != null) {
            map.put("creationDate", creationDate);
        }
        if (authorizationCode != null) {
            map.put("authorizationCode", authorizationCode);
        }
        if (authorizationKey != null) {
            map.put("authorizationKey", authorizationKey);
        }
        if (username != null) {
            map.put("username", username);
        }
        return map;
    }

    public void setUsername(String username) {
        this.username = username;
    }

}
