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
package org.nuxeo.ecm.platform.oauth.tests;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.oauth2.Constants.AUTHORIZATION_CODE_GRANT_TYPE;
import static org.nuxeo.ecm.platform.oauth2.Constants.AUTHORIZATION_CODE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CLIENT_ID_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CLIENT_SECRET_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_CHALLENGE_METHOD_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_CHALLENGE_METHOD_PLAIN;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_CHALLENGE_METHOD_S256;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_CHALLENGE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_RESPONSE_TYPE;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_VERIFIER_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.GRANT_TYPE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.REDIRECT_URI_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.REFRESH_TOKEN_GRANT_TYPE;
import static org.nuxeo.ecm.platform.oauth2.Constants.REFRESH_TOKEN_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.RESPONSE_TYPE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.STATE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.TOKEN_SERVICE;
import static org.nuxeo.ecm.platform.oauth2.NuxeoOAuth2Servlet.ENDPOINT_AUTH;
import static org.nuxeo.ecm.platform.oauth2.NuxeoOAuth2Servlet.ENDPOINT_AUTH_SUBMIT;
import static org.nuxeo.ecm.platform.oauth2.NuxeoOAuth2Servlet.ENDPOINT_TOKEN;
import static org.nuxeo.ecm.platform.oauth2.NuxeoOAuth2Servlet.ERROR_DESCRIPTION_PARAM;
import static org.nuxeo.ecm.platform.oauth2.NuxeoOAuth2Servlet.ERROR_PARAM;
import static org.nuxeo.ecm.platform.oauth2.OAuth2Error.ACCESS_DENIED;
import static org.nuxeo.ecm.platform.oauth2.OAuth2Error.INVALID_CLIENT;
import static org.nuxeo.ecm.platform.oauth2.OAuth2Error.INVALID_GRANT;
import static org.nuxeo.ecm.platform.oauth2.OAuth2Error.INVALID_REQUEST;
import static org.nuxeo.ecm.platform.oauth2.OAuth2Error.UNSUPPORTED_GRANT_TYPE;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreProvider;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.platform.oauth2.NuxeoOAuth2Servlet;
import org.nuxeo.ecm.platform.oauth2.request.AuthorizationRequest;
import org.nuxeo.ecm.platform.oauth2.tokens.NuxeoOAuth2Token;
import org.nuxeo.ecm.platform.oauth2.tokens.OAuth2TokenStore;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.jaxrs.test.JerseyClientHelper;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
@RunWith(FeaturesRunner.class)
@Features({ OAuthFeature.class, OAuth2ServletContainerFeature.class })
@ServletContainer(port = 18090)
public class OAuth2ChallengeFixture {

    protected static final String CLIENT_ID = "testClient";

    protected static final String CLIENT_SECRET = "testSecret";

    protected static final String REDIRECT_URI = "https://redirect.uri";

    protected static final String STATE = "testState";

    protected static final String BASE_URL = "http://localhost:18090";

    @Inject
    protected TransientStoreService transientStoreService;

    @Inject
    protected TransactionalFeature txFeature;

    protected Client client;

    protected TransientStoreProvider transientStore;

    protected OAuth2TokenStore tokenStore;

    @Before
    public void initOAuthClient() {
        // Client to make the requests like a "Client" as the OAuth2 RFC describes it
        client = JerseyClientHelper.clientBuilder()
                                   .setRedirectsEnabled(false)
                                   .setCredentials("Administrator", "Administrator")
                                   .build();

        transientStore = (TransientStoreProvider) transientStoreService.getStore(AuthorizationRequest.STORE_NAME);

        tokenStore = new OAuth2TokenStore(TOKEN_SERVICE);
    }

    @After
    public void tearDown() {
        client.destroy();
    }

    @Test
    public void authorizeShouldReturn200() {
        Map<String, String> params = getAuthorizationRequestParams(STATE);
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(200, cr.getStatus());
        }
    }

    @Test
    public void authorizeShouldRejectUnknownClient() {
        Map<String, String> params = getAuthorizationRequestParams(STATE);
        params.put(CLIENT_ID_PARAM, "unknown");
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(400, cr.getStatus());
        }
    }

    @Test
    public void authorizeShouldValidateRedirectURI() {
        Map<String, String> params = new HashMap<>();
        params.put(RESPONSE_TYPE_PARAM, CODE_RESPONSE_TYPE);
        params.put(STATE_PARAM, STATE);

        // Invalid: no redirect_uri parameter and no registered redirect URI
        params.put(CLIENT_ID_PARAM, "no-redirect-uri");
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(400, cr.getStatus());
        }

        // Invalid: no redirect_uri parameter with invalid first registered redirect URI: not starting with https
        params.put(CLIENT_ID_PARAM, "not-https");
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(400, cr.getStatus());
        }

        // Invalid: no redirect_uri parameter with invalid first registered redirect URI: starting with http://localhost
        // with localhost part of the domain name
        params.put(CLIENT_ID_PARAM, "localhost-domain-name");
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(400, cr.getStatus());
        }

        // Valid: no redirect_uri parameter with valid first registered redirect URI: starting with https
        params.put(CLIENT_ID_PARAM, CLIENT_ID);
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(200, cr.getStatus());
        }

        // Invalid: redirect_uri parameter not matching any of the registered redirect URIs
        params.put(REDIRECT_URI_PARAM, "https://unknown.uri");
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(400, cr.getStatus());
        }

        // Invalid: redirect_uri parameter matching one of the registered redirect URIs not starting with https
        params.put(CLIENT_ID_PARAM, "not-https");
        params.put(REDIRECT_URI_PARAM, "http://redirect.uri");
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(400, cr.getStatus());
        }

        // Valid: redirect_uri parameter matching one of the registered redirect URIs starting with https
        params.put(CLIENT_ID_PARAM, CLIENT_ID);
        params.put(REDIRECT_URI_PARAM, REDIRECT_URI);
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(200, cr.getStatus());
        }

        // Valid: redirect_uri parameter matching one of the registered redirect URIs starting with http://localhost
        // with localhost not part of the domain name
        params.put(REDIRECT_URI_PARAM, "http://localhost:8080/nuxeo");
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(200, cr.getStatus());
        }

        // Valid: redirect_uri parameter matching one of the registered redirect URIs not starting with http
        params.put(REDIRECT_URI_PARAM, "nuxeo://authorize");
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(200, cr.getStatus());
        }
    }

    /**
     * The client must send either both "code_challenge" and "code_challenge_method" parameters along with the
     * authorization request or none of them.
     * <p>
     * The value of the "code_challenge_method" parameter must be either "plain" or "S256".
     */
    @Test
    public void authorizeShouldValidatePKCE() {
        // Invalid: code_challenge_method but no code_challenge
        Map<String, String> params = getAuthorizationRequestParams();
        params.put(CODE_CHALLENGE_METHOD_PARAM, CODE_CHALLENGE_METHOD_S256);
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(400, cr.getStatus());
        }

        // Invalid: code_challenge but no code_challenge_method
        params.remove(CODE_CHALLENGE_METHOD_PARAM);
        params.put(CODE_CHALLENGE_PARAM, "myCodeChallenge");
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(400, cr.getStatus());
        }

        // Invalid: code_challenge_method not supported
        params.put(CODE_CHALLENGE_METHOD_PARAM, "unknown");
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(400, cr.getStatus());
        }

        // Valid: code_challenge and supported code_challenge_method
        params.put(CODE_CHALLENGE_METHOD_PARAM, "S256");
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(200, cr.getStatus());
        }
    }

    @Test
    public void authorizeShouldDenyAccess() throws UnsupportedEncodingException {
        initValidAuthorizeRequestCall(STATE);

        // missing "grant_access" parameter to grant access
        Map<String, String> params = getAuthorizationRequestParams(STATE);
        try (CloseableClientResponse cr = responseFromPostAuthorizeWith(params)) {
            assertEquals(302, cr.getStatus());
            String redirect = cr.getHeaders().get("Location").get(0);
            String error = extractParameter(redirect, ERROR_PARAM);
            assertEquals(ACCESS_DENIED, error);
            String errorDescription = extractParameter(redirect, ERROR_DESCRIPTION_PARAM);
            assertEquals(URLEncoder.encode("Access denied by the user", UTF_8.name()),
                    errorDescription);
            String state = extractParameter(redirect, STATE_PARAM);
            assertEquals(STATE, state);
            // ensure no authorization request has been stored
            assertStoreIsEmpty();
        }
    }

    @Test
    public void authorizeWithExistingTokenShouldBypassGrant() throws IOException, InterruptedException {
        ObjectMapper obj = new ObjectMapper();
        NuxeoOAuth2Token initialToken;
        String initialAccessToken;
        // acquire an access token
        try (CloseableClientResponse cr = getTokenResponse()) {
            assertEquals(200, cr.getStatus());
            initialToken = tokenStore.getToken(CLIENT_ID, "Administrator");
            assertNotNull(initialToken);
            initialAccessToken = initialToken.getAccessToken();
        }

        // get authorization, should redirect to the redirect_uri with a code parameter
        String code;
        Map<String, String> params = getAuthorizationRequestParams();
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(302, cr.getStatus());
            String redirect = cr.getHeaders().get("Location").get(0);
            assertTrue(redirect.startsWith(REDIRECT_URI));
            code = extractParameter(redirect, AUTHORIZATION_CODE_PARAM);
            assertNotNull(code);
        }

        // ask for an access token with the returned code, should get the one previously acquired
        params = getTokenRequestParams(code, null);
        try (CloseableClientResponse cr = responseFromTokenWith(params)) {
            assertEquals(200, cr.getStatus());
            String json = cr.getEntity(String.class);
            @SuppressWarnings("unchecked")
            Map<String, Serializable> token = obj.readValue(json, Map.class);
            assertNotNull(token);
            assertEquals(initialAccessToken, token.get("access_token"));
        }

        // set a short expiration time on the token to force its refresh
        initialToken.setExpirationTimeMilliseconds(100L);
        tokenStore.update(initialToken);
        txFeature.nextTransaction();
        Thread.sleep(200);

        // get authorization, should redirect to the redirect_uri with a new code parameter
        String newCode;
        params = getAuthorizationRequestParams();
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(302, cr.getStatus());
            String redirect = cr.getHeaders().get("Location").get(0);
            assertTrue(redirect.startsWith(REDIRECT_URI));
            newCode = extractParameter(redirect, AUTHORIZATION_CODE_PARAM);
            assertNotEquals(code, newCode);
        }

        // ask for an access token with the returned code, should get a refreshed token
        String refreshedAccessToken;
        params = getTokenRequestParams(newCode, null);
        try (CloseableClientResponse cr = responseFromTokenWith(params)) {
            assertEquals(200, cr.getStatus());
            String json = cr.getEntity(String.class);
            @SuppressWarnings("unchecked")
            Map<String, Serializable> refreshedToken = obj.readValue(json, Map.class);
            assertNotNull(refreshedToken);
            refreshedAccessToken = (String) refreshedToken.get("access_token");
            assertNotEquals(initialAccessToken, refreshedAccessToken);
        }

        clearToken(refreshedAccessToken, CLIENT_ID);
    }

    @Test
    public void authorizeWithAutoGrantShouldBypassGrant() {
        // get authorization, should redirect to the redirect_uri with a code parameter
        Map<String, String> params = getAuthorizationRequestParams();
        params.put(CLIENT_ID_PARAM, "autoGrant");
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(302, cr.getStatus());
            String redirect = cr.getHeaders().get("Location").get(0);
            assertTrue(redirect.startsWith(REDIRECT_URI));
            String code = extractParameter(redirect, AUTHORIZATION_CODE_PARAM);
            assertNotNull(code);
        }
    }

    @Test
    public void getAuthorizeSubmitShouldReturn500() {
        try (CloseableClientResponse response = CloseableClientResponse.of(
                client.resource(BASE_URL).path("oauth2").path(ENDPOINT_AUTH_SUBMIT).get(ClientResponse.class))) {
            assertEquals(405, response.getStatus());
        }
    }

    @Test
    public void getTokenShouldReturn500() {
        try (CloseableClientResponse response = CloseableClientResponse.of(
                client.resource(BASE_URL).path("oauth2").path(ENDPOINT_TOKEN).get(ClientResponse.class))) {
            assertEquals(405, response.getStatus());
        }
    }

    @Test
    public void tokenShouldValidateParameters() throws IOException {
        ObjectMapper obj = new ObjectMapper();
        // unsupported grant_type parameter
        Map<String, String> params = new HashMap<>();
        params.put(GRANT_TYPE_PARAM, "unknown");
        try (CloseableClientResponse cr = responseFromTokenWith(params)) {
            assertEquals(400, cr.getStatus());
            String json = cr.getEntity(String.class);
            Map<?, ?> error = obj.readValue(json, Map.class);
            assertEquals(UNSUPPORTED_GRANT_TYPE, error.get(ERROR_PARAM));
            assertEquals(
                    String.format("Unknown %s: got \"unknown\", expecting \"%s\" or \"%s\".", GRANT_TYPE_PARAM,
                            AUTHORIZATION_CODE_GRANT_TYPE, REFRESH_TOKEN_GRANT_TYPE),
                    error.get(ERROR_DESCRIPTION_PARAM));
            assertStoreIsEmpty();
        }

        // invalid authorization code
        params.put(GRANT_TYPE_PARAM, AUTHORIZATION_CODE_GRANT_TYPE);
        params.put(AUTHORIZATION_CODE_PARAM, "invalidCode");
        try (CloseableClientResponse cr = responseFromTokenWith(params)) {
            assertEquals(400, cr.getStatus());
            String json = cr.getEntity(String.class);
            Map<?, ?> error = obj.readValue(json, Map.class);
            assertEquals(INVALID_GRANT, error.get(ERROR_PARAM));
            assertEquals("Invalid authorization code", error.get(ERROR_DESCRIPTION_PARAM));
            assertStoreIsEmpty();
        }

        // invalid client_id parameter
        initValidAuthorizeRequestCall();
        String code = getAuthorizationCode();
        params.put(AUTHORIZATION_CODE_PARAM, code);
        params.put(CLIENT_ID_PARAM, "unknown");
        try (CloseableClientResponse cr = responseFromTokenWith(params)) {
            assertEquals(400, cr.getStatus());
            String json = cr.getEntity(String.class);
            Map<?, ?> error = obj.readValue(json, Map.class);
            assertEquals(INVALID_CLIENT, error.get(ERROR_PARAM));
            assertEquals("Invalid client id: unknown", error.get(ERROR_DESCRIPTION_PARAM));
            assertStoreIsEmpty();
        }

        // invalid client_secret parameter
        initValidAuthorizeRequestCall();
        code = getAuthorizationCode();
        params.put(AUTHORIZATION_CODE_PARAM, code);
        params.put(CLIENT_ID_PARAM, CLIENT_ID);
        params.put(CLIENT_SECRET_PARAM, "invalidSecret");
        try (CloseableClientResponse cr = responseFromTokenWith(params)) {
            assertEquals(400, cr.getStatus());
            String json = cr.getEntity(String.class);
            Map<?, ?> error = obj.readValue(json, Map.class);
            assertEquals(INVALID_CLIENT, error.get(ERROR_PARAM));
            assertEquals("Disabled client or invalid client secret", error.get(ERROR_DESCRIPTION_PARAM));
            assertStoreIsEmpty();
        }

        // check that the redirect_uri parameter is required when included in the authorization request
        initValidAuthorizeRequestCall();
        code = getAuthorizationCode();
        params.put(AUTHORIZATION_CODE_PARAM, code);
        params.put(CLIENT_SECRET_PARAM, CLIENT_SECRET);
        try (CloseableClientResponse cr = responseFromTokenWith(params)) {
            assertEquals(400, cr.getStatus());
            String json = cr.getEntity(String.class);
            Map<?, ?> error = obj.readValue(json, Map.class);
            assertEquals(INVALID_GRANT, error.get(ERROR_PARAM));
            assertEquals("Invalid redirect URI: null", error.get(ERROR_DESCRIPTION_PARAM));
            assertStoreIsEmpty();
        }
    }

    /**
     * If the "code_challenge" and "code_challenge_method" parameters are sent along with the authorization request, the
     * client must send a "code_verifier" parameter along with the token request.
     * <p>
     * The server performs the proof of possession of the code verifier by the client by calculating the code challenge
     * from the received "code_verifier" parameter transformed according to the "code_challenge_method" and comparing it
     * with the previously associated "code_challenge".
     */
    @Test
    public void tokenShouldValidatePKCE() throws IOException {
        ObjectMapper obj = new ObjectMapper();

        // let's first issue a code verifier by generating high-entropy cryptographic random string using unreserved
        // characters with a minimum length of 43 characters
        String codeVerifier = Base64.encodeBase64URLSafeString(RandomUtils.nextBytes(32));
        // let's first use a code challenge derived from the code verifier by using the plain transformation
        String codeChallenge = codeVerifier;

        // missing code_verifier parameter
        try (CloseableClientResponse cr = getTokenResponse(null, codeChallenge, CODE_CHALLENGE_METHOD_PLAIN, null)) {
            assertEquals(400, cr.getStatus());
            String json = cr.getEntity(String.class);
            Map<?, ?> error = obj.readValue(json, Map.class);
            assertEquals(INVALID_REQUEST, error.get(ERROR_PARAM));
            assertEquals(String.format("Missing %s parameter", CODE_VERIFIER_PARAM),
                    error.get(ERROR_DESCRIPTION_PARAM));
        }

        // invalid code_verifier parameter with plain code challenge method
        try (CloseableClientResponse cr = getTokenResponse(null, codeChallenge, CODE_CHALLENGE_METHOD_PLAIN,
                "invalidCodeVerifier")) {
            assertEquals(400, cr.getStatus());
            String json = cr.getEntity(String.class);
            Map<?, ?> error = obj.readValue(json, Map.class);
            assertEquals(INVALID_GRANT, error.get(ERROR_PARAM));
            assertEquals(String.format("Invalid %s parameter", CODE_VERIFIER_PARAM),
                    error.get(ERROR_DESCRIPTION_PARAM));
        }

        // valid code_verifier parameter with plain code challenge method
        String accessToken;
        try (CloseableClientResponse cr = getTokenResponse(null, codeChallenge, CODE_CHALLENGE_METHOD_PLAIN,
                codeVerifier)) {
            assertEquals(200, cr.getStatus());
            String json = cr.getEntity(String.class);
            Map<?, ?> token = obj.readValue(json, Map.class);
            assertNotNull(token);
            accessToken = (String) token.get("access_token");
            assertEquals(32, accessToken.length());
            String refreshToken = (String) token.get("refresh_token");
            assertEquals(64, refreshToken.length());
        }
        clearToken(accessToken, CLIENT_ID);

        // let's now use a code challenge derived from the code verifier by using the S256 transformation
        codeChallenge = Base64.encodeBase64URLSafeString(DigestUtils.sha256(codeVerifier));

        // invalid code_verifier parameter with S256 code challenge method
        try (CloseableClientResponse cr = getTokenResponse(null, codeChallenge, CODE_CHALLENGE_METHOD_S256,
                "invalidCodeVerifier")) {
            assertEquals(400, cr.getStatus());
            String json = cr.getEntity(String.class);
            Map<?, ?> error = obj.readValue(json, Map.class);
            assertEquals(INVALID_GRANT, error.get(ERROR_PARAM));
            assertEquals(String.format("Invalid %s parameter", CODE_VERIFIER_PARAM),
                    error.get(ERROR_DESCRIPTION_PARAM));
        }

        // valid code_verifier parameter with S256 code challenge method
        try (CloseableClientResponse cr = getTokenResponse(null, codeChallenge, CODE_CHALLENGE_METHOD_S256,
                codeVerifier)) {
            assertEquals(200, cr.getStatus());
            String json = cr.getEntity(String.class);
            Map<?, ?> token = obj.readValue(json, Map.class);
            assertNotNull(token);
            accessToken = (String) token.get("access_token");
            assertEquals(32, accessToken.length());
            String refreshToken = (String) token.get("refresh_token");
            assertEquals(64, refreshToken.length());
        }
        clearToken(accessToken, CLIENT_ID);
    }

    @Test
    public void shouldRetrieveAccessAndRefreshTokenWithoutState() throws IOException {
        shouldRetrieveAccessAndRefreshToken(null);
    }

    @Test
    public void shouldRetrieveAccessAndRefreshTokenWithState() throws IOException {
        shouldRetrieveAccessAndRefreshToken(STATE);
    }

    protected void shouldRetrieveAccessAndRefreshToken(String state) throws IOException {
        ObjectMapper obj = new ObjectMapper();
        Map<?, ?> token;
        String refreshToken;
        try (CloseableClientResponse cr = getTokenResponse(state)) {
            assertEquals(200, cr.getStatus());
            String json = cr.getEntity(String.class);
            token = obj.readValue(json, Map.class);
            assertNotNull(token);
            String accessToken = (String) token.get("access_token");
            assertEquals(32, accessToken.length());
            refreshToken = (String) token.get("refresh_token");
            assertEquals(64, refreshToken.length());
        }

        // Refresh this token
        String newAccessToken;
        Map<String, String> params = new HashMap<>();
        params.put(GRANT_TYPE_PARAM, REFRESH_TOKEN_GRANT_TYPE);
        params.put(CLIENT_ID_PARAM, CLIENT_ID);
        params.put(CLIENT_SECRET_PARAM, CLIENT_SECRET);
        params.put(REFRESH_TOKEN_PARAM, refreshToken);
        try (CloseableClientResponse cr = responseFromTokenWith(params)) {
            assertEquals(200, cr.getStatus());
            String json = cr.getEntity(String.class);
            @SuppressWarnings("unchecked")
            Map<String, Serializable> refreshed = obj.readValue(json, Map.class);
            newAccessToken = (String) refreshed.get("access_token");
            assertNotSame(newAccessToken, token.get("access_token"));
            assertStoreIsEmpty();
        }
        clearToken(newAccessToken, CLIENT_ID);
    }

    protected void initValidAuthorizeRequestCall() {
        initValidAuthorizeRequestCall(null);
    }

    protected void initValidAuthorizeRequestCall(String state) {
        initValidAuthorizeRequestCall(state, null, null);
    }

    protected void initValidAuthorizeRequestCall(String state, String codeChallenge, String codeChallengeMethod) {
        Map<String, String> params = getAuthorizationRequestParams(state, codeChallenge, codeChallengeMethod);
        try (CloseableClientResponse cr = responseFromGetAuthorizeWith(params)) {
            assertEquals(200, cr.getStatus());
        }
    }

    protected String getAuthorizationCode() {
        return getAuthorizationCode(null, null, null);
    }

    protected String getAuthorizationCode(String state, String codeChallenge, String codeChallengeMethod) {
        Map<String, String> params = getAuthorizationRequestParams(state, codeChallenge, codeChallengeMethod);
        params.put(NuxeoOAuth2Servlet.GRANT_ACCESS_PARAM, "true");
        try (CloseableClientResponse cr = responseFromPostAuthorizeWith(params)) {
            assertEquals(302, cr.getStatus());
            String redirect = cr.getHeaders().get("Location").get(0);
            if (state != null) {
                String redirectState = extractParameter(redirect, STATE_PARAM);
                assertEquals(state, redirectState);
            }
            String code = extractParameter(redirect, AUTHORIZATION_CODE_PARAM);

            // ensure we have only one authorization request and its key is the returned code
            Set<String> keys = transientStore.keySet();
            assertEquals(1, keys.size());
            assertTrue(keys.contains(code));

            return code;
        }
    }

    protected Map<String, String> getAuthorizationRequestParams() {
        return getAuthorizationRequestParams(null);
    }

    protected Map<String, String> getAuthorizationRequestParams(String state) {
        return getAuthorizationRequestParams(state, null, null);
    }

    protected Map<String, String> getAuthorizationRequestParams(String state, String codeChallenge,
            String codeChallengeMethod) {
        Map<String, String> params = new HashMap<>();
        params.put(RESPONSE_TYPE_PARAM, CODE_RESPONSE_TYPE);
        params.put(CLIENT_ID_PARAM, CLIENT_ID);
        params.put(REDIRECT_URI_PARAM, REDIRECT_URI);
        if (state != null) {
            params.put(STATE_PARAM, STATE);
        }
        if (codeChallenge != null) {
            params.put(CODE_CHALLENGE_PARAM, codeChallenge);
        }
        if (codeChallengeMethod != null) {
            params.put(CODE_CHALLENGE_METHOD_PARAM, codeChallengeMethod);
        }
        return params;
    }

    protected CloseableClientResponse getTokenResponse() {
        return getTokenResponse(null);
    }

    protected CloseableClientResponse getTokenResponse(String state) {
        return getTokenResponse(state, null, null, null);
    }

    protected CloseableClientResponse getTokenResponse(String state, String codeChallenge, String codeChallengeMethod,
            String codeVerifier) {
        initValidAuthorizeRequestCall(state, codeChallenge, codeChallengeMethod);
        String code = getAuthorizationCode(state, codeChallenge, codeChallengeMethod);
        Map<String, String> params = getTokenRequestParams(code, codeVerifier);
        CloseableClientResponse cr = responseFromTokenWith(params);
        assertStoreIsEmpty();
        return cr;
    }

    protected Map<String, String> getTokenRequestParams(String code, String codeVerifier) {
        Map<String, String> params = new HashMap<>();
        params.put(GRANT_TYPE_PARAM, AUTHORIZATION_CODE_GRANT_TYPE);
        params.put(AUTHORIZATION_CODE_PARAM, code);
        params.put(CLIENT_ID_PARAM, CLIENT_ID);
        params.put(CLIENT_SECRET_PARAM, CLIENT_SECRET);
        params.put(REDIRECT_URI_PARAM, REDIRECT_URI);
        if (codeVerifier != null) {
            params.put(CODE_VERIFIER_PARAM, codeVerifier);
        }
        return params;
    }

    protected void clearToken(String accessToken, String clientId) {
        tokenStore.delete(accessToken, clientId);
        txFeature.nextTransaction();
    }

    protected String extractParameter(String url, String parameterName) {
        Pattern pattern = Pattern.compile(parameterName + "=(.*?)(&|$)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    protected CloseableClientResponse responseFromGetAuthorizeWith(Map<String, String> queryParams) {
        WebResource wr = client.resource(BASE_URL).path("oauth2").path(ENDPOINT_AUTH);

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            params.add(entry.getKey(), entry.getValue());
        }

        return CloseableClientResponse.of(wr.queryParams(params).get(ClientResponse.class));
    }

    protected CloseableClientResponse responseFromPostAuthorizeWith(Map<String, String> queryParams) {
        WebResource wr = client.resource(BASE_URL).path("oauth2").path(ENDPOINT_AUTH_SUBMIT);

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            params.add(entry.getKey(), entry.getValue());
        }

        return CloseableClientResponse.of(wr.queryParams(params).post(ClientResponse.class));
    }

    protected CloseableClientResponse responseFromTokenWith(Map<String, String> params) {
        WebResource wr = client.resource(BASE_URL).path("oauth2").path(ENDPOINT_TOKEN);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            formData.add(entry.getKey(), entry.getValue());
        }

        return CloseableClientResponse.of(wr.post(ClientResponse.class, formData));
    }

    protected void assertStoreIsEmpty() {
        assertEquals(0, transientStore.keySet().size());
    }

}
