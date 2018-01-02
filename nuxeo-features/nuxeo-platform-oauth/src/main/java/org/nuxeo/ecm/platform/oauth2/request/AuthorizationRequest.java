/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.platform.oauth2.Constants.CLIENT_ID_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_CHALLENGE_METHODS_SUPPORTED;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_CHALLENGE_METHOD_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_CHALLENGE_METHOD_PLAIN;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_CHALLENGE_METHOD_S256;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_CHALLENGE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_RESPONSE_TYPE;
import static org.nuxeo.ecm.platform.oauth2.Constants.REDIRECT_URI_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.RESPONSE_TYPE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.SCOPE_PARAM;

import java.io.Serializable;
import java.security.Principal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.platform.oauth2.OAuth2Error;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2Client;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2ClientService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public class AuthorizationRequest extends OAuth2Request {

    private static final Log log = LogFactory.getLog(AuthorizationRequest.class);

    public static final String MISSING_REQUIRED_FIELD_MESSAGE = "Missing required field \"%s\".";

    public static final String STORE_NAME = "authorizationRequestStore";

    protected String responseType;

    protected String scope;

    protected Date creationDate;

    protected String authorizationCode;

    protected String username;

    protected String codeChallenge;

    protected String codeChallengeMethod;

    public static AuthorizationRequest fromRequest(HttpServletRequest request) {
        return new AuthorizationRequest(request);
    }

    public static AuthorizationRequest fromMap(Map<String, Serializable> map) {
        return new AuthorizationRequest(map);
    }

    public static void store(String key, AuthorizationRequest authorizationRequest) {
        TransientStoreService transientStoreService = Framework.getService(TransientStoreService.class);
        TransientStore store = transientStoreService.getStore(STORE_NAME);
        store.putParameters(key, authorizationRequest.toMap());
    }

    public static AuthorizationRequest get(String key) {
        TransientStoreService transientStoreService = Framework.getService(TransientStoreService.class);
        TransientStore store = transientStoreService.getStore(STORE_NAME);
        Map<String, Serializable> parameters = store.getParameters(key);
        if (parameters != null) {
            AuthorizationRequest authorizationRequest = AuthorizationRequest.fromMap(parameters);
            return authorizationRequest.isExpired() ? null : authorizationRequest;
        }
        return null;
    }

    public static void remove(String key) {
        TransientStoreService transientStoreService = Framework.getService(TransientStoreService.class);
        TransientStore store = transientStoreService.getStore(STORE_NAME);
        store.remove(key);
    }

    protected AuthorizationRequest(HttpServletRequest request) {
        super(request);
        responseType = request.getParameter(RESPONSE_TYPE_PARAM);
        scope = request.getParameter(SCOPE_PARAM);

        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            username = principal.getName();
        }

        creationDate = new Date();

        codeChallenge = request.getParameter(CODE_CHALLENGE_PARAM);
        codeChallengeMethod = request.getParameter(CODE_CHALLENGE_METHOD_PARAM);
    }

    protected AuthorizationRequest(Map<String, Serializable> map) {
        clientId = (String) map.get("clientId");
        redirectURI = (String) map.get("redirectURI");
        responseType = (String) map.get("responseType");
        scope = (String) map.get("scope");
        creationDate = (Date) map.get("creationDate");
        authorizationCode = (String) map.get("authorizationCode");
        username = (String) map.get("username");
        codeChallenge = (String) map.get("codeChallenge");
        codeChallengeMethod = (String) map.get("codeChallengeMethod");
    }

    public OAuth2Error checkError() {
        // Check mandatory fields
        if (StringUtils.isBlank(clientId)) {
            return OAuth2Error.invalidRequest(String.format(MISSING_REQUIRED_FIELD_MESSAGE, CLIENT_ID_PARAM));
        }
        if (StringUtils.isBlank(responseType)) {
            return OAuth2Error.invalidRequest(String.format(MISSING_REQUIRED_FIELD_MESSAGE, RESPONSE_TYPE_PARAM));
        }
        // Check response type
        if (!CODE_RESPONSE_TYPE.equals(responseType)) {
            return OAuth2Error.unsupportedResponseType(String.format("Unknown %s: got \"%s\", expecting \"%s\".",
                    RESPONSE_TYPE_PARAM, responseType, CODE_RESPONSE_TYPE));
        }

        // Check if client exists
        OAuth2ClientService clientService = Framework.getService(OAuth2ClientService.class);
        OAuth2Client client = clientService.getClient(clientId);
        if (client == null) {
            return OAuth2Error.invalidRequest(String.format("Invalid %s: %s.", CLIENT_ID_PARAM, clientId));
        }
        if (!client.isEnabled()) {
            return OAuth2Error.accessDenied(String.format("Client %s is disabled.", clientId));
        }

        String clientName = client.getName();
        if (StringUtils.isBlank(clientName)) {
            log.error(String.format(
                    "No name set for OAuth2 client %s. It is a required field, please make sure you update this OAuth2 client.",
                    client));
            // Here we are just checking that the client has a name since it is now a required field but it might be
            // empty for an old client.
            // Yet we don't return an error for backward compatibility since an empty name is not a security issue and
            // should not prevent the authorization request from working.
        }

        List<String> clientRedirectURIs = client.getRedirectURIs();
        if (CollectionUtils.isEmpty(clientRedirectURIs)) {
            log.error(String.format(
                    "No redirect URI set for OAuth2 client %s, at least one is required. Please make sure you update this OAuth2 client.",
                    client));
            // Checking that the client has at least one redirect URI since it is now a required field but it might be
            // empty for an old client.
            // In this case we return an error since we cannot trust the redirect_uri parameter for security reasons.
            return OAuth2Error.accessDenied("No redirect URI configured for the app.");
        }

        String clientRedirectURI = null;
        // No redirect_uri parameter, use the first redirect URI registered for this client
        if (StringUtils.isBlank(redirectURI)) {
            clientRedirectURI = clientRedirectURIs.get(0);
        } else {
            // Check that the redirect_uri parameter matches one of the the redirect URIs registered for this client
            if (!clientRedirectURIs.contains(redirectURI)) {
                return OAuth2Error.invalidRequest(String.format(
                        "Invalid %s parameter: %s. It must exactly match one of the redirect URIs configured for the app.",
                        REDIRECT_URI_PARAM, redirectURI));
            }
            clientRedirectURI = redirectURI;
        }

        // Check redirect URI validity
        if (!OAuth2Client.isRedirectURIValid(clientRedirectURI)) {
            log.error(String.format(
                    "The redirect URI %s set for OAuth2 client %s is invalid: it must not be empty and start with https for security reasons. Please make sure you update this OAuth2 client.",
                    clientRedirectURI, client));
            return OAuth2Error.invalidRequest(String.format(
                    "Invalid redirect URI configured for the app: %s. It must not be empty and start with https for security reasons.",
                    clientRedirectURI));
        }

        // Check PKCE parameters
        if (codeChallenge != null && codeChallengeMethod == null
                || codeChallenge == null && codeChallengeMethod != null) {
            return OAuth2Error.invalidRequest(String.format(
                    "Invalid PKCE parameters: either both %s and %s parameters must be sent or none of them.",
                    CODE_CHALLENGE_PARAM, CODE_CHALLENGE_METHOD_PARAM));
        }
        if (codeChallengeMethod != null && !CODE_CHALLENGE_METHODS_SUPPORTED.contains(codeChallengeMethod)) {
            return OAuth2Error.invalidRequest(String.format(
                    "Invalid %s parameter: transform algorithm %s not supported. The server only supports %s.",
                    CODE_CHALLENGE_METHOD_PARAM, codeChallengeMethod, CODE_CHALLENGE_METHODS_SUPPORTED));
        }

        return null;
    }

    public boolean isExpired() {
        // RFC 4.1.2, Authorization code lifetime is 10
        return new Date().getTime() - creationDate.getTime() > 10 * 60 * 1000;
    }

    public String getResponseType() {
        return responseType;
    }

    public String getScope() {
        return scope;
    }

    public String getUsername() {
        return username;
    }

    public String getAuthorizationCode() {
        if (StringUtils.isBlank(authorizationCode)) {
            authorizationCode = RandomStringUtils.random(10, true, true);
        }
        return authorizationCode;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }

    public Map<String, Serializable> toMap() {
        Map<String, Serializable> map = new HashMap<>();
        if (clientId != null) {
            map.put("clientId", clientId);
        }
        if (redirectURI != null) {
            map.put("redirectURI", redirectURI);
        }
        if (responseType != null) {
            map.put("responseType", responseType);
        }
        if (scope != null) {
            map.put("scope", scope);
        }
        if (creationDate != null) {
            map.put("creationDate", creationDate);
        }
        if (authorizationCode != null) {
            map.put("authorizationCode", authorizationCode);
        }
        if (username != null) {
            map.put("username", username);
        }
        if (codeChallenge != null) {
            map.put("codeChallenge", codeChallenge);
        }
        if (codeChallengeMethod != null) {
            map.put("codeChallengeMethod", codeChallengeMethod);
        }
        return map;
    }

    public boolean isCodeVerifierValid(String codeVerifier) {
        if (codeChallenge == null || codeChallengeMethod == null) {
            return false;
        }
        switch (codeChallengeMethod) {
        case CODE_CHALLENGE_METHOD_S256:
            return codeChallenge.equals(Base64.encodeBase64URLSafeString(DigestUtils.sha256(codeVerifier)));
        case CODE_CHALLENGE_METHOD_PLAIN:
            return codeChallenge.equals(codeVerifier);
        default:
            return false;
        }
    }

}
