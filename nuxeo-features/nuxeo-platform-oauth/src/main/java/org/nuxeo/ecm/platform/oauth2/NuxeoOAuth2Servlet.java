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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.nuxeo.ecm.platform.oauth2.Constants.AUTHORIZATION_CODE_GRANT_TYPE;
import static org.nuxeo.ecm.platform.oauth2.Constants.AUTHORIZATION_CODE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.REFRESH_TOKEN_GRANT_TYPE;
import static org.nuxeo.ecm.platform.oauth2.Constants.STATE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.TOKEN_SERVICE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2Client;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2ClientService;
import org.nuxeo.ecm.platform.oauth2.request.AuthorizationRequest;
import org.nuxeo.ecm.platform.oauth2.request.TokenRequest;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @since 9.2
 */
public class NuxeoOAuth2Servlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String ENDPOINT_AUTH = "authorization";

    public static final String ENDPOINT_TOKEN = "token";

    public static final String ENDPOINT_AUTH_SUBMIT = "authorization_submit";

    public static final String AUTHORIZATION_KEY = "authorization_key";

    public static final String ERROR_PARAM = "error";

    public static final String ERROR_DESCRIPTION_PARAM = "error_description";

    public static final String CLIENT_NAME = "client_name";

    public static final String GRANT_JSP_PAGE_PATH = "/oauth2Grant.jsp";

    public static final String GRANT_ACCESS_PARAM = "grant_access";

    public static final String ERROR_JSP_PAGE_PATH = "/oauth2error.jsp";

    public static final OAuth2Error MISSING_STATE_ERROR = OAuth2Error.invalidRequest(
            String.format(AuthorizationRequest.MISSING_REQUIRED_FIELD_MESSAGE, STATE_PARAM));

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

    protected void doGetAuthorization(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        AuthorizationRequest authRequest = AuthorizationRequest.fromRequest(request);
        OAuth2Error error = authRequest.checkError();
        if (error != null) {
            handleError(error, request, response);
            return;
        }

        String state = request.getParameter(STATE_PARAM);
        if (StringUtils.isBlank(state)) {
            handleError(MISSING_STATE_ERROR, request, response);
            return;
        }

        AuthorizationRequest.store(authRequest.getAuthorizationKey(), authRequest);
        OAuth2ClientService clientService = Framework.getService(OAuth2ClientService.class);
        request.setAttribute(AUTHORIZATION_KEY, authRequest.getAuthorizationKey());
        request.setAttribute(STATE_PARAM, state);
        request.setAttribute(CLIENT_NAME, clientService.getClient(authRequest.getClientId()).getName());

        RequestDispatcher requestDispatcher = request.getRequestDispatcher(GRANT_JSP_PAGE_PATH);
        requestDispatcher.forward(request, response);
    }

    protected void doPostAuthorizationSubmit(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        String authKeyForm = request.getParameter(AUTHORIZATION_KEY);
        AuthorizationRequest authRequest = AuthorizationRequest.get(authKeyForm);
        if (authRequest == null) {
            handleError(OAuth2Error.invalidRequest(), request, response);
            return;
        }

        AuthorizationRequest.remove(authRequest.getAuthorizationKey());

        OAuth2Error error = authRequest.checkError();
        if (error != null) {
            handleError(error, request, response);
            return;
        }

        String state = request.getParameter(STATE_PARAM);
        if (StringUtils.isBlank(state)) {
            handleError(MISSING_STATE_ERROR, request, response);
            return;
        }

        String grantAccess = request.getParameter(GRANT_ACCESS_PARAM);
        if (grantAccess == null) {
            // the user deny access
            error = OAuth2Error.accessDenied();
            Map<String, String> params = new HashMap<>();
            params.put(ERROR_PARAM, error.getId());
            String errorDescription = error.getDescription();
            if (StringUtils.isNotBlank(errorDescription)) {
                params.put(ERROR_DESCRIPTION_PARAM, errorDescription);
            }
            params.put(STATE_PARAM, state);
            sendRedirect(response, authRequest.getRedirectUri(), params);
            return;
        }

        // now store the authorization request according to its code
        // to be able to retrieve it in the "/oauth2/token" endpoint
        String authorizationCode = authRequest.getAuthorizationCode();
        AuthorizationRequest.store(authorizationCode, authRequest);
        Map<String, String> params = new HashMap<>();
        params.put(AUTHORIZATION_CODE_PARAM, authorizationCode);
        params.put(STATE_PARAM, state);

        request.getSession().invalidate();
        sendRedirect(response, authRequest.getRedirectUri(), params);
    }

    protected void doGetToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        TokenRequest tokenRequest = new TokenRequest(request);
        OAuth2ClientService clientService = Framework.getService(OAuth2ClientService.class);
        // Process Authorization code
        if (AUTHORIZATION_CODE_GRANT_TYPE.equals(tokenRequest.getGrantType())) {
            AuthorizationRequest authRequest = AuthorizationRequest.get(tokenRequest.getCode());
            OAuth2Error error = null;
            if (authRequest == null) {
                error = OAuth2Error.accessDenied();
            }
            // Check that clientId is the good one, already verified in
            // authorization request
            else if (!authRequest.getClientId().equals(tokenRequest.getClientId())) {
                error = OAuth2Error.accessDenied();
            } else {
                OAuth2Client client = clientService.getClient(authRequest.getClientId());
                // Validate client secret
                if (client == null || !client.isValidWith(tokenRequest.getClientId(), tokenRequest.getClientSecret())) {
                    error = OAuth2Error.unauthorizedClient();
                }
                // Ensure redirect URIs are identical
                else {
                    String redirectURI = tokenRequest.getRedirectUri();
                    if (StringUtils.isBlank(redirectURI) || !OAuth2Client.isRedirectURIValid(redirectURI)
                            || !redirectURI.equals(client.getRedirectURI())
                            || !redirectURI.equals(authRequest.getRedirectUri())) {
                        error = OAuth2Error.invalidRequest();
                    }
                }
            }

            if (authRequest != null) {
                AuthorizationRequest.remove(authRequest.getAuthorizationCode());
            }

            if (error != null) {
                handleJsonError(error, response);
                return;
            }

            // Store token
            NuxeoOAuth2Token token = new NuxeoOAuth2Token(ACCESS_TOKEN_EXPIRATION_TIME, authRequest.getClientId());
            TransactionHelper.runInTransaction(() -> tokenStore.store(authRequest.getUsername(), token));

            handleTokenResponse(token, response);
        } else if (REFRESH_TOKEN_GRANT_TYPE.equals(tokenRequest.getGrantType())) {
            OAuth2Error error = null;
            if (StringUtils.isBlank(tokenRequest.getClientId())) {
                error = OAuth2Error.accessDenied();
            } else if (!clientService.isValidClient(tokenRequest.getClientId(), tokenRequest.getClientSecret())) {
                error = OAuth2Error.accessDenied();
            }

            if (error != null) {
                handleJsonError(error, response);
                return;
            }

            NuxeoOAuth2Token refreshed = TransactionHelper.runInTransaction(
                    () -> tokenStore.refresh(tokenRequest.getRefreshToken(), tokenRequest.getClientId()));

            if (refreshed == null) {
                handleJsonError(OAuth2Error.invalidRequest(), response);
            } else {
                handleTokenResponse(refreshed, response);
            }
        } else {
            handleJsonError(OAuth2Error.invalidGrant(), response);
        }
    }

    protected void handleTokenResponse(NuxeoOAuth2Token token, HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "application/json");
        response.setStatus(SC_OK);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getWriter(), token.toJsonObject());
    }

    protected void handleError(OAuth2Error error, HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        response.reset();
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        request.setAttribute("error", error);
        RequestDispatcher requestDispatcher = request.getRequestDispatcher(ERROR_JSP_PAGE_PATH);
        requestDispatcher.forward(request, response);
    }

    protected void handleJsonError(OAuth2Error error, HttpServletResponse response) throws IOException {
        response.setHeader("Content-Type", "application/json");
        response.setStatus(SC_BAD_REQUEST);

        Map<String, String> object = new HashMap<>();
        object.put(ERROR_PARAM, error.getId());
        if (StringUtils.isNotBlank(error.getDescription())) {
            object.put(ERROR_DESCRIPTION_PARAM, error.getDescription());
        }
        ObjectMapper mapper = new ObjectMapper();
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
