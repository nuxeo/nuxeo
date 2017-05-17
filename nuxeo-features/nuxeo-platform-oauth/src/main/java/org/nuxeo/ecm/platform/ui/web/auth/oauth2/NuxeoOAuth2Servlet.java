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
 *     Arnaud Kervern
 *     Thomas Roger
 *
 */
package org.nuxeo.ecm.platform.ui.web.auth.oauth2;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.nuxeo.ecm.platform.ui.web.auth.oauth2.Constants.TOKEN_SERVICE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.platform.oauth2.OAuth2Error;
import org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry;
import org.nuxeo.ecm.platform.oauth2.request.AuthorizationRequest;
import org.nuxeo.ecm.platform.oauth2.request.TokenRequest;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 9.2
 */
public class NuxeoOAuth2Servlet extends HttpServlet {

    protected static final String ENDPOINT_AUTH = "authorization";

    protected static final String ENDPOINT_TOKEN = "token";

    protected static final String ENDPOINT_AUTH_SUBMIT = "authorization_submit";

    public static final String USERNAME_KEY = "nuxeo_user";

    public static final String AUTHORIZATION_KEY = "authorization_key";

    public static final String STATE_KEY = "state";

    public static final String AUTHORIZATION_CODE_PARAM = "code";

    public static final String ERROR_PARAM = "error";

    public static final String CLIENT_NAME = "client_name";

    public static final String GRANT_JSP_PAGE = "oauth2Grant.jsp";

    public static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";

    public static final String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";

    public static final String GRANT_ACCESS_PARAM = "grant_access";

    public static final String DENY_ACCESS_PARAM = "deny_access";

    public static final int ACCESS_TOKEN_EXPIRATION_TIME = 3600 * 1000;

    protected OAuth2TokenStore tokenStore = new OAuth2TokenStore(TOKEN_SERVICE);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo.endsWith(ENDPOINT_AUTH)) {
            doGetAuthorization(request, response);
        } else if (pathInfo.endsWith(ENDPOINT_TOKEN)) {
            doGetToken(request, response);
        } else {
            response.sendError(SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo.endsWith(ENDPOINT_AUTH_SUBMIT)) {
            doPostAuthorizationSubmit(request, response);
        } else {
            response.sendError(SC_NOT_FOUND);
        }
    }

    protected void doGetAuthorization(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AuthorizationRequest authRequest = AuthorizationRequest.from(request);
        OAuth2Error error = authRequest.checkError();
        if (error != null) {
            handleError(error, request, response, authRequest.getRedirectUri());
            return;
        }

        ClientRegistry clientRegistry = Framework.getService(ClientRegistry.class);
        request.getSession().setAttribute(AUTHORIZATION_KEY, authRequest.getAuthorizationKey());
        request.getSession().setAttribute(STATE_KEY, authRequest.getState());
        request.getSession().setAttribute(CLIENT_NAME, clientRegistry.getClient(authRequest.getClientId()).getName());
        String base = VirtualHostHelper.getBaseURL(request);
        sendRedirect(response, base + GRANT_JSP_PAGE, null);
    }

    protected void doGetToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        TokenRequest tokRequest = new TokenRequest(request);
        ClientRegistry clientRegistry = Framework.getService(ClientRegistry.class);
        // Process Authorization code
        if (AUTHORIZATION_CODE_GRANT_TYPE.equals(tokRequest.getGrantType())) {
            AuthorizationRequest authRequest = AuthorizationRequest.fromCode(tokRequest.getCode());
            OAuth2Error error = null;
            if (authRequest == null) {
                error = OAuth2Error.ACCESS_DENIED;
            }
            // Check that clientId is the good one, already verified in
            // authorization request
            else if (!authRequest.getClientId().equals(tokRequest.getClientId())) {
                error = OAuth2Error.ACCESS_DENIED;
            }
            // Validate client secret
            else if (!clientRegistry.isValidClient(tokRequest.getClientId(), tokRequest.getClientSecret())) {
                error = OAuth2Error.UNAUTHORIZED_CLIENT;
            }
            // Ensure redirect uris are identical
            else {
                boolean sameRedirectUri = authRequest.getRedirectUri().equals(tokRequest.getRedirectUri());
                if (!(isBlank(authRequest.getRedirectUri()) || sameRedirectUri)) {
                    error = OAuth2Error.INVALID_REQUEST;
                }
            }

            if (error != null) {
                handleJsonError(error, response);
                return;
            }

            // Store token
            NuxeoOAuth2Token token = new NuxeoOAuth2Token(ACCESS_TOKEN_EXPIRATION_TIME, authRequest.getClientId());
            TransactionHelper.runInTransaction(() -> tokenStore.store(authRequest.getUsername(), token));

            handleTokenResponse(token, response);
        } else if (REFRESH_TOKEN_GRANT_TYPE.equals(tokRequest.getGrantType())) {
            OAuth2Error error = null;
            if (isBlank(tokRequest.getClientId())) {
                error = OAuth2Error.ACCESS_DENIED;
            } else if (!clientRegistry.isValidClient(tokRequest.getClientId(), tokRequest.getClientSecret())) {
                error = OAuth2Error.ACCESS_DENIED;
            }

            if (error != null) {
                handleJsonError(error, response);
                return;
            }

            NuxeoOAuth2Token refreshed = TransactionHelper.runInTransaction(
                    () -> tokenStore.refresh(tokRequest.getRefreshToken(), tokRequest.getClientId()));

            if (refreshed == null) {
                handleJsonError(OAuth2Error.INVALID_REQUEST, response);
            } else {
                handleTokenResponse(refreshed, response);
            }
        } else {
            handleJsonError(OAuth2Error.INVALID_GRANT, response);
        }
    }

    protected void doPostAuthorizationSubmit(HttpServletRequest request, HttpServletResponse response) throws IOException {
        AuthorizationRequest authRequest = AuthorizationRequest.from(request);
        OAuth2Error error = authRequest.checkError();
        if (error != null) {
            handleError(error, request, response, authRequest.getRedirectUri());
            return;
        }

        String grantAccess = request.getParameter(GRANT_ACCESS_PARAM);
        String denyAccess = request.getParameter(DENY_ACCESS_PARAM);

        // Ensure that the user actually grant access and the authorization key is the correct one
        String authKeyForm = request.getParameter(AUTHORIZATION_KEY);
        if ((denyAccess != null || grantAccess == null) || !authRequest.getAuthorizationKey().equals(authKeyForm)) {
            handleError(OAuth2Error.ACCESS_DENIED, request, response, authRequest.getRedirectUri());
            return;
        }

        // Save username in request object
        authRequest.setUsername((String) request.getSession().getAttribute(USERNAME_KEY));

        Map<String, String> params = new HashMap<>();
        params.put(AUTHORIZATION_CODE_PARAM, authRequest.getAuthorizationCode());
        if (isNotBlank(authRequest.getState())) {
            params.put(STATE_KEY, authRequest.getState());
        }

        request.getSession().invalidate();
        sendRedirect(response, authRequest.getRedirectUri(), params);
    }

    protected void handleTokenResponse(NuxeoOAuth2Token token, HttpServletResponse response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        response.setHeader("Content-Type", "application/json");
        response.setStatus(SC_OK);
        mapper.writeValue(response.getWriter(), token.toJsonObject());
    }

    protected void handleError(OAuth2Error error, HttpServletRequest request, HttpServletResponse response,
            String redirectURI) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put(ERROR_PARAM, error.toString().toLowerCase());
        String state = request.getParameter(STATE_KEY);
        if (isNotBlank(state)) {
            params.put(STATE_KEY, state);
        }

        sendRedirect(response, redirectURI, params);
    }

    protected void handleJsonError(OAuth2Error error, HttpServletResponse response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        response.setHeader("Content-Type", "application/json");
        response.setStatus(SC_BAD_REQUEST);

        Map<String, String> object = new HashMap<>();
        object.put(ERROR_PARAM, error.toString().toLowerCase());
        mapper.writeValue(response.getWriter(), object);
    }

    protected void sendRedirect(HttpServletResponse response, String redirectURI, Map<String, String> params)
            throws IOException {
        if (redirectURI == null) {
            response.sendError(SC_BAD_REQUEST, "No redirect URI");
            return;
        }

        String url = URIUtils.addParametersToURIQuery(redirectURI, params);
        response.sendRedirect(url);
    }
}
