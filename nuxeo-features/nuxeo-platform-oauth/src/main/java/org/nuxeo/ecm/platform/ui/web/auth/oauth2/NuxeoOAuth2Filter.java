/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.ui.web.auth.oauth2;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.IOException;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry;
import org.nuxeo.ecm.platform.oauth2.request.AuthorizationRequest;
import org.nuxeo.ecm.platform.oauth2.request.TokenRequest;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoAuthenticationFilter;
import org.nuxeo.ecm.platform.ui.web.auth.NuxeoSecuredRequestWrapper;
import org.nuxeo.ecm.platform.ui.web.auth.interfaces.NuxeoAuthPreFilter;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
public class NuxeoOAuth2Filter implements NuxeoAuthPreFilter {

    private static final Log log = LogFactory.getLog(NuxeoOAuth2Filter.class);

    protected static final String TOKEN_SERVICE = "org.nuxeo.server.token.store";

    protected static final String OAUTH2_SEGMENT = "/oauth2/";

    protected static final String ENDPOINT_AUTH = "authorization";

    protected static final String ENDPOINT_TOKEN = "token";

    public static String USERNAME_KEY = "nuxeo_user";

    public static String AUTHORIZATION_KEY = "authorization_key";

    public static String CLIENTNAME_KEY = "client_name";

    public static enum ERRORS {
        invalid_request, invalid_grant, unauthorized_client, access_denied, unsupported_response_type, invalid_scope, server_error, temporarily_unavailable
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!isValid(request)) {
            chain.doFilter(request, response);
            return;
        }

        boolean startedTx = false;
        if (!TransactionHelper.isTransactionActive()) {
            startedTx = TransactionHelper.startTransaction();
        }
        boolean done = false;
        try {
            process(request, response, chain);
            done = true;
        } finally {
            if (startedTx) {
                if (!done) {
                    TransactionHelper.setTransactionRollbackOnly();
                }
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
    }

    protected boolean isValid(ServletRequest request) {
        if (!(request instanceof HttpServletRequest)) {
            return false;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        return isAuthorizedRequest(httpRequest) || httpRequest.getRequestURI().contains(OAUTH2_SEGMENT);
    }

    protected void process(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();
        if (uri.contains(OAUTH2_SEGMENT) && !uri.contains("/api/v1" + OAUTH2_SEGMENT)) {
            String endpoint = uri.split(OAUTH2_SEGMENT)[1];
            switch (endpoint) {
            case ENDPOINT_AUTH:
                processAuthorization(httpRequest, httpResponse, chain);
                break;
            case ENDPOINT_TOKEN:
                processToken(httpRequest, httpResponse, chain);
                break;
            }
        } else if (isAuthorizedRequest(httpRequest)) {
            processAuthentication(httpRequest, httpResponse, chain);
        }

        if (!response.isCommitted()) {
            chain.doFilter(request, response);
        }
    }

    protected void processAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String key = URLDecoder.decode(request.getHeader("Authorization").substring(7), "UTF-8").trim();
        NuxeoOAuth2Token token = getTokenStore().getToken(key);

        if (token == null) {
            return;
        }

        if (token.isExpired() || !getClientRegistry().hasClient(token.getClientId())) {
            response.setStatus(401);
            return;
        }

        LoginContext loginContext = buildLoginContext(token);
        if (loginContext != null) {
            Principal principal = (Principal) loginContext.getSubject().getPrincipals().toArray()[0];
            try {
                chain.doFilter(new NuxeoSecuredRequestWrapper(request, principal), response);
            } finally {
                try {
                    loginContext.logout();
                } catch (LoginException e) {
                    log.warn("Error when logging out", e);
                }
            }
        }
    }

    protected LoginContext buildLoginContext(NuxeoOAuth2Token token) {
        try {
            return NuxeoAuthenticationFilter.loginAs(token.getNuxeoLogin());
        } catch (LoginException e) {
            log.warn("Error while authenticate user");
        }
        return null;
    }

    protected boolean isAuthorizedRequest(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization != null && authorization.startsWith("Bearer");
    }

    protected void processAuthorization(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException {

        AuthorizationRequest authRequest = AuthorizationRequest.from(request);
        String error = authRequest.checkError();
        if (isNotBlank(error)) {
            handleError(error, request, response);
            return;
        }

        // Redirect to grant form
        if (request.getMethod().equals("GET")) {
            request.getSession().setAttribute("response_type", "code");
            request.getSession().setAttribute("client_id", authRequest.getClientId());
            request.getSession().setAttribute("redirect_uri", authRequest.getRedirectUri());
            request.getSession().setAttribute("state", authRequest.getState());
            request.getSession().setAttribute(CLIENTNAME_KEY,
                    getClientRegistry().getClient(authRequest.getClientId()).getName());
            String base = VirtualHostHelper.getBaseURL(request);
            sendRedirect(response, base + "oauth2Grant.jsp", null);
            return;
        }

        // now store the authorization request according to its code
        // to be able to retrieve it in the "/oauth2/token" endpoint
        String authorizationCode = storeAuthorizationRequest(authRequest);
        Map<String, String> params = new HashMap<>();
        params.put("code", authorizationCode);
        if (isNotBlank(authRequest.getState())) {
            params.put("state", authRequest.getState());
        }

        request.getSession().invalidate();
        sendRedirect(response, authRequest.getRedirectUri(), params);
    }

    protected String storeAuthorizationRequest(AuthorizationRequest authRequest) {
        String authorizationCode = authRequest.getAuthorizationCode();
        AuthorizationRequest.store(authorizationCode, authRequest);
        return authorizationCode;
    }

    ClientRegistry getClientRegistry() {
        return Framework.getLocalService(ClientRegistry.class);
    }

    protected void processToken(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException {
        TokenRequest tokRequest = new TokenRequest(request);
        // Process Authorization code
        if ("authorization_code".equals(tokRequest.getGrantType())) {
            String authorizationCode = tokRequest.getCode();
            AuthorizationRequest authRequest = AuthorizationRequest.fromCode(authorizationCode);
            ERRORS error = null;
            if (authRequest == null) {
                error = ERRORS.access_denied;
            }
            // Check that clientId is the good one, already verified in
            // authorization request
            else if (!authRequest.getClientId().equals(tokRequest.getClientId())) {
                error = ERRORS.access_denied;
            }
            // Validate client secret
            else if (!getClientRegistry().isValidClient(tokRequest.getClientId(), tokRequest.getClientSecret())) {
                error = ERRORS.unauthorized_client;
            }
            // Ensure redirect uris are identical
            else {
                boolean sameRedirectUri = authRequest.getRedirectUri().equals(tokRequest.getRedirectUri());
                if (!(isBlank(authRequest.getRedirectUri()) || sameRedirectUri)) {
                    error = ERRORS.invalid_request;
                }
            }

            if (authRequest != null) {
                AuthorizationRequest.remove(authorizationCode);
            }

            if (error != null) {
                handleError(error, request, response);
                return;
            }

            // Store token
            NuxeoOAuth2Token token = new NuxeoOAuth2Token(3600 * 1000, authRequest.getClientId());
            getTokenStore().store(authRequest.getUsername(), token);

            handleTokenResponse(token, response);
        } else if ("refresh_token".equals(tokRequest.getGrantType())) {
            ERRORS error = null;
            if (isBlank(tokRequest.getClientId())) {
                error = ERRORS.access_denied;
            } else if (!getClientRegistry().isValidClient(tokRequest.getClientId(), tokRequest.getClientSecret())) {
                error = ERRORS.access_denied;
            }

            if (error != null) {
                handleError(error, request, response);
                return;
            }

            NuxeoOAuth2Token refreshed = getTokenStore().refresh(tokRequest.getRefreshToken(),
                    tokRequest.getClientId());
            if (refreshed == null) {
                handleJsonError(ERRORS.invalid_request, request, response);
            } else {
                handleTokenResponse(refreshed, response);
            }
        } else {
            handleJsonError(ERRORS.invalid_grant, request, response);
        }
    }

    protected void handleTokenResponse(NuxeoOAuth2Token token, HttpServletResponse response) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        response.setHeader("Content-Type", "application/json");
        response.setStatus(200);
        mapper.writeValue(response.getWriter(), token.toJsonObject());
    }

    protected void handleError(ERRORS error, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        handleError(error.toString(), request, response);
    }

    protected void handleError(String error, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("error", error);
        String state = request.getParameter("state");
        if (isNotBlank(state)) {
            params.put("state", state);
        }

        String redirectUri = request.getParameter("redirect_uri");
        sendRedirect(response, redirectUri, params);
    }

    protected void handleJsonError(ERRORS error, HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        response.setHeader("Content-Type", "application/json");
        response.setStatus(400);

        Map<String, String> object = new HashMap<>();
        object.put("error", error.toString());
        mapper.writeValue(response.getWriter(), object);
    }

    protected void sendRedirect(HttpServletResponse response, String uri, Map<String, String> params)
            throws IOException {
        if (uri == null) {
            uri = "http://dummyurl";
        }

        StringBuilder sb = new StringBuilder(uri);
        if (params != null) {
            if (!uri.contains("?")) {
                sb.append("?");
            } else {
                sb.append("&");
            }

            for (String key : params.keySet()) {
                sb.append(key).append("=").append(params.get(key)).append("&");
            }
            sb.deleteCharAt(sb.length() - 1);
        }
        response.sendRedirect(sb.toString());
    }

    protected OAuth2TokenStore getTokenStore() {
        return new OAuth2TokenStore(TOKEN_SERVICE);
    }
}
