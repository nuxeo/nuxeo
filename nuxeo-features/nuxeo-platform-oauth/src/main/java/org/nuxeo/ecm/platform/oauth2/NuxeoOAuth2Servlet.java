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
import static org.nuxeo.ecm.platform.oauth2.Constants.CLIENT_ID_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_CHALLENGE_METHOD_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_CHALLENGE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_VERIFIER_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.GRANT_TYPE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.REDIRECT_URI_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.REFRESH_TOKEN_GRANT_TYPE;
import static org.nuxeo.ecm.platform.oauth2.Constants.RESPONSE_TYPE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.SCOPE_PARAM;
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    private static final Log log = LogFactory.getLog(NuxeoOAuth2Servlet.class);

    public static final String ENDPOINT_AUTH = "authorize";

    public static final String ENDPOINT_TOKEN = "token";

    public static final String ENDPOINT_AUTH_SUBMIT = "authorize_submit";

    public static final String ERROR_PARAM = "error";

    public static final String ERROR_DESCRIPTION_PARAM = "error_description";

    public static final String CLIENT_NAME = "client_name";

    public static final String GRANT_JSP_PAGE_PATH = "/oauth2Grant.jsp";

    public static final String GRANT_ACCESS_PARAM = "grant_access";

    public static final String ERROR_JSP_PAGE_PATH = "/oauth2error.jsp";

    public static final int ACCESS_TOKEN_EXPIRATION_TIME = 3600 * 1000;

    protected OAuth2TokenStore tokenStore = new OAuth2TokenStore(TOKEN_SERVICE);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo.endsWith(ENDPOINT_AUTH)) {
            doGetAuthorize(request, response);
        } else {
            response.sendError(SC_NOT_FOUND);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String pathInfo = request.getPathInfo();
        if (pathInfo.endsWith(ENDPOINT_AUTH_SUBMIT)) {
            doPostAuthorizeSubmit(request, response);
        } else if (pathInfo.endsWith(ENDPOINT_TOKEN)) {
            doPostToken(request, response);
        } else {
            response.sendError(SC_NOT_FOUND);
        }
    }

    protected void doGetAuthorize(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        AuthorizationRequest authRequest = AuthorizationRequest.fromRequest(request);
        OAuth2Error error = authRequest.checkError();
        if (error != null) {
            handleError(error, request, response);
            return;
        }

        request.setAttribute(RESPONSE_TYPE_PARAM, authRequest.getResponseType());
        request.setAttribute(CLIENT_ID_PARAM, authRequest.getClientId());
        String redirectURI = authRequest.getRedirectURI();
        if (StringUtils.isNotBlank(redirectURI)) {
            request.setAttribute(REDIRECT_URI_PARAM, redirectURI);
        }
        String scope = authRequest.getScope();
        if (StringUtils.isNotBlank(scope)) {
            request.setAttribute(SCOPE_PARAM, scope);
        }
        String state = request.getParameter(STATE_PARAM);
        if (StringUtils.isNotBlank(state)) {
            request.setAttribute(STATE_PARAM, state);
        }
        String codeChallenge = authRequest.getCodeChallenge();
        String codeChallengeMethod = authRequest.getCodeChallengeMethod();
        if (codeChallenge != null && codeChallengeMethod != null) {
            request.setAttribute(CODE_CHALLENGE_PARAM, codeChallenge);
            request.setAttribute(CODE_CHALLENGE_METHOD_PARAM, codeChallengeMethod);
        }
        request.setAttribute(CLIENT_NAME,
                Framework.getService(OAuth2ClientService.class).getClient(authRequest.getClientId()).getName());

        RequestDispatcher requestDispatcher = request.getRequestDispatcher(GRANT_JSP_PAGE_PATH);
        requestDispatcher.forward(request, response);
    }

    protected void doPostAuthorizeSubmit(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        AuthorizationRequest authRequest = AuthorizationRequest.fromRequest(request);
        OAuth2Error error = authRequest.checkError();
        if (error != null) {
            handleError(error, request, response);
            return;
        }

        // If the redirect URI was included in the authorization request use it else fall back on the first one
        // registered for the client
        String redirectURI = authRequest.getRedirectURI();
        if (StringUtils.isBlank(redirectURI)) {
            redirectURI = Framework.getService(OAuth2ClientService.class)
                                   .getClient(authRequest.getClientId())
                                   .getRedirectURIs()
                                   .get(0);
        }
        String state = request.getParameter(STATE_PARAM);
        String grantAccess = request.getParameter(GRANT_ACCESS_PARAM);
        if (grantAccess == null) {
            // the user deny access
            error = OAuth2Error.accessDenied("Access denied by the user");
            Map<String, String> params = new HashMap<>();
            params.put(ERROR_PARAM, error.getId());
            String errorDescription = error.getDescription();
            if (StringUtils.isNotBlank(errorDescription)) {
                params.put(ERROR_DESCRIPTION_PARAM, errorDescription);
            }
            if (StringUtils.isNotBlank(state)) {
                params.put(STATE_PARAM, state);
            }
            sendRedirect(request, response, redirectURI, params);
            return;
        }

        // now store the authorization request according to its code
        // to be able to retrieve it in the "/oauth2/token" endpoint
        String authorizationCode = authRequest.getAuthorizationCode();
        AuthorizationRequest.store(authorizationCode, authRequest);
        Map<String, String> params = new HashMap<>();
        params.put(AUTHORIZATION_CODE_PARAM, authorizationCode);
        if (StringUtils.isNotBlank(state)) {
            params.put(STATE_PARAM, state);
        }

        sendRedirect(request, response, redirectURI, params);
    }

    protected void doPostToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        TokenRequest tokenRequest = new TokenRequest(request);
        OAuth2ClientService clientService = Framework.getService(OAuth2ClientService.class);
        String grantType = tokenRequest.getGrantType();
        // Process Authorization code
        if (AUTHORIZATION_CODE_GRANT_TYPE.equals(grantType)) {
            String authorizationCode = tokenRequest.getCode();
            AuthorizationRequest authRequest = AuthorizationRequest.get(authorizationCode);
            OAuth2Error error = null;
            if (authRequest == null) {
                error = OAuth2Error.invalidGrant("Invalid authorization code");
            }
            // Check that clientId is the good one, already verified in authorization request
            else {
                String tokenClientId = tokenRequest.getClientId();
                if (!authRequest.getClientId().equals(tokenClientId)) {
                    error = OAuth2Error.invalidClient(String.format("Invalid client id: %s", tokenClientId));
                } else {
                    OAuth2Client client = clientService.getClient(authRequest.getClientId());
                    // Validate client secret
                    if (client == null
                            || !client.isValidWith(tokenRequest.getClientId(), tokenRequest.getClientSecret())) {
                        error = OAuth2Error.invalidClient("Disabled client or invalid client secret");
                    } else {
                        // Ensure redirect URIs are identical if the redirect_uri parameter was included in the
                        // authorization request
                        String authRequestRedirectURI = authRequest.getRedirectURI();
                        String tokenRequestRedirectURI = tokenRequest.getRedirectURI();
                        if (StringUtils.isNotBlank(authRequestRedirectURI)
                                && !authRequestRedirectURI.equals(tokenRequestRedirectURI)) {
                            error = OAuth2Error.invalidGrant(
                                    String.format("Invalid redirect URI: %s", tokenRequestRedirectURI));
                        } else {
                            // Check PKCE
                            String codeChallenge = authRequest.getCodeChallenge();
                            if (codeChallenge != null) {
                                String codeVerifier = tokenRequest.getCodeVerifier();
                                if (codeVerifier == null) {
                                    error = OAuth2Error.invalidRequest(
                                            String.format("Missing %s parameter", CODE_VERIFIER_PARAM));
                                } else if (!authRequest.isCodeVerifierValid(codeVerifier)) {
                                    error = OAuth2Error.invalidGrant(
                                            String.format("Invalid %s parameter", CODE_VERIFIER_PARAM));
                                }
                            }
                        }
                    }
                }
            }

            if (authRequest != null) {
                AuthorizationRequest.remove(authorizationCode);
            }

            if (error != null) {
                handleJsonError(error, response);
                return;
            }

            // Store token
            NuxeoOAuth2Token token = new NuxeoOAuth2Token(ACCESS_TOKEN_EXPIRATION_TIME, authRequest.getClientId());
            TransactionHelper.runInTransaction(() -> tokenStore.store(authRequest.getUsername(), token));

            handleTokenResponse(token, response);
        } else if (REFRESH_TOKEN_GRANT_TYPE.equals(grantType)) {
            OAuth2Error error = null;
            if (StringUtils.isBlank(tokenRequest.getClientId())) {
                error = OAuth2Error.invalidRequest("Empty client id");
            } else if (!clientService.isValidClient(tokenRequest.getClientId(), tokenRequest.getClientSecret())) {
                error = OAuth2Error.invalidClient("Disabled client or invalid client secret");
            }

            if (error != null) {
                handleJsonError(error, response);
                return;
            }

            NuxeoOAuth2Token refreshed = TransactionHelper.runInTransaction(
                    () -> tokenStore.refresh(tokenRequest.getRefreshToken(), tokenRequest.getClientId()));

            if (refreshed == null) {
                handleJsonError(OAuth2Error.invalidGrant("Cannot refresh token"), response);
            } else {
                handleTokenResponse(refreshed, response);
            }
        } else {
            handleJsonError(OAuth2Error.unsupportedGrantType(
                    String.format("Unknown %s: got \"%s\", expecting \"%s\" or \"%s\".", GRANT_TYPE_PARAM, grantType,
                            AUTHORIZATION_CODE_GRANT_TYPE, REFRESH_TOKEN_GRANT_TYPE)),
                    response);
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
        log.warn(String.format("OAuth2 authorization request error: %s", error));
        response.reset();
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        request.setAttribute("error", error);
        RequestDispatcher requestDispatcher = request.getRequestDispatcher(ERROR_JSP_PAGE_PATH);
        requestDispatcher.forward(request, response);
    }

    protected void handleJsonError(OAuth2Error error, HttpServletResponse response) throws IOException {
        log.warn(String.format("OAuth2 token request error: %s", error));
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

    protected void sendRedirect(HttpServletRequest request, HttpServletResponse response, String redirectURI,
            Map<String, String> params) throws IOException {
        if (redirectURI == null) {
            response.sendError(SC_BAD_REQUEST, "No redirect URI");
            return;
        }

        String url = URIUtils.addParametersToURIQuery(redirectURI, params);
        response.sendRedirect(url);
    }
}
