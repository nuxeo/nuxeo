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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.oauth2.Constants.AUTHORIZATION_CODE_GRANT_TYPE;
import static org.nuxeo.ecm.platform.oauth2.Constants.AUTHORIZATION_CODE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CLIENT_ID_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CLIENT_SECRET_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_RESPONSE_TYPE;
import static org.nuxeo.ecm.platform.oauth2.Constants.GRANT_TYPE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.REDIRECT_URI_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.REFRESH_TOKEN_GRANT_TYPE;
import static org.nuxeo.ecm.platform.oauth2.Constants.REFRESH_TOKEN_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.RESPONSE_TYPE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.STATE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.NuxeoOAuth2Servlet.ENDPOINT_AUTH;
import static org.nuxeo.ecm.platform.oauth2.NuxeoOAuth2Servlet.ENDPOINT_AUTH_SUBMIT;
import static org.nuxeo.ecm.platform.oauth2.NuxeoOAuth2Servlet.ENDPOINT_TOKEN;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.ecm.platform.oauth2.NuxeoOAuth2Servlet;
import org.nuxeo.ecm.platform.oauth2.request.AuthorizationRequest;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
@RunWith(FeaturesRunner.class)
@Features({ OAuthFeature.class, OAuth2JettyFeature.class })
@Jetty(port = 18090)
public class OAuth2ChallengeFixture {

    protected static final String CLIENT_ID = "testClient";

    protected static final String CLIENT_SECRET = "testSecret";

    protected static final String REDIRECT_URI = "https://redirect.uri";

    protected static final String STATE = "testState";

    protected static final String BASE_URL = "http://localhost:18090";

    private static final int TIMEOUT = 1000 * 60 * 5; // 5min

    @Inject
    protected TransientStoreService transientStoreService;

    protected Client client;

    protected TransientStore store;

    @Before
    public void initOAuthClient() {
        // First client to request like a "Client" as OAuth RFC describe it
        client = Client.create();
        client.setConnectTimeout(TIMEOUT);
        client.setReadTimeout(TIMEOUT);
        client.setFollowRedirects(Boolean.FALSE);

        store = transientStoreService.getStore(AuthorizationRequest.STORE_NAME);
    }

    @Test
    public void authorizationShouldReturn200() {
        Map<String, String> params = new HashMap<>();
        params.put(REDIRECT_URI_PARAM, REDIRECT_URI);
        params.put(CLIENT_ID_PARAM, CLIENT_ID);
        params.put(RESPONSE_TYPE_PARAM, CODE_RESPONSE_TYPE);
        params.put(STATE_PARAM, STATE);

        ClientResponse cr = responseFromGetAuthorizationWith(params);
        assertEquals(200, cr.getStatus());
    }

    @Test
    public void authorizationShouldRejectMissingState() {
        Map<String, String> params = new HashMap<>();
        params.put(REDIRECT_URI_PARAM, REDIRECT_URI);
        params.put(CLIENT_ID_PARAM, CLIENT_ID);
        params.put(RESPONSE_TYPE_PARAM, CODE_RESPONSE_TYPE);

        ClientResponse cr = responseFromGetAuthorizationWith(params);
        assertEquals(400, cr.getStatus());
    }

    @Test
    public void authorizationShouldRejectUnknownClient() {
        Map<String, String> params = new HashMap<>();
        params.put(REDIRECT_URI_PARAM, REDIRECT_URI);
        params.put(CLIENT_ID_PARAM, "unknown");
        params.put(RESPONSE_TYPE_PARAM, CODE_RESPONSE_TYPE);
        params.put(STATE_PARAM, STATE);

        ClientResponse cr = responseFromGetAuthorizationWith(params);
        assertEquals(400, cr.getStatus());
    }

    @Test
    public void authorizationShouldValidateRedirectURI() {
        // Invalid: no redirect URI
        Map<String, String> params = new HashMap<>();
        params.put(CLIENT_ID_PARAM, CLIENT_ID);
        params.put(RESPONSE_TYPE_PARAM, CODE_RESPONSE_TYPE);
        params.put(STATE_PARAM, STATE);
        ClientResponse cr = responseFromGetAuthorizationWith(params);
        assertEquals(400, cr.getStatus());

        // Invalid: not starting with https
        params.put(REDIRECT_URI_PARAM, "http://redirect.uri");
        cr = responseFromGetAuthorizationWith(params);
        assertEquals(400, cr.getStatus());

        // Invalid: starting with http://localhost with localhost part of the domain name
        params.put(REDIRECT_URI_PARAM, "http://localhost.somecompany.com");
        cr = responseFromGetAuthorizationWith(params);
        assertEquals(400, cr.getStatus());

        // Invalid: not matching the one from the registered client
        params.put(REDIRECT_URI_PARAM, "https://unknown.uri");
        cr = responseFromGetAuthorizationWith(params);
        assertEquals(400, cr.getStatus());

        // Valid: not starting with http
        params.put(CLIENT_ID_PARAM, "nuxeo-mobile-app");
        params.put(REDIRECT_URI_PARAM, "nuxeo://authorize");
        cr = responseFromGetAuthorizationWith(params);
        assertEquals(200, cr.getStatus());

        // Valid: starting with http://localhost with localhost not part of the domain name
        params.put(CLIENT_ID_PARAM, "localhost");
        params.put(REDIRECT_URI_PARAM, "http://localhost:8080/nuxeo");
        cr = responseFromGetAuthorizationWith(params);
        assertEquals(200, cr.getStatus());

        // Valid: starting with https
        params.put(CLIENT_ID_PARAM, "secure");
        params.put(REDIRECT_URI_PARAM, REDIRECT_URI);
        cr = responseFromGetAuthorizationWith(params);
        assertEquals(200, cr.getStatus());
    }

    @Test
    public void shouldDenyAccess() {
        AuthorizationRequest authorizationRequest = initValidAuthorizationRequestCall();
        String key = authorizationRequest.getAuthorizationKey();

        // missing "grant_access" parameter to grant access
        Map<String, String> params = new HashMap<>();
        params.put(REDIRECT_URI_PARAM, REDIRECT_URI);
        params.put(CLIENT_ID_PARAM, CLIENT_ID);
        params.put(STATE_PARAM, STATE);
        params.put(NuxeoOAuth2Servlet.AUTHORIZATION_KEY, key);
        ClientResponse cr = responseFromPostAuthorizationWith(params);
        assertEquals(302, cr.getStatus());
        String redirect = cr.getHeaders().get("Location").get(0);
        assertTrue(redirect.contains("error=access_denied"));
        String state = extractParameter(redirect, STATE_PARAM);
        assertEquals(STATE, state);

        // ensure authorization request has been removed
        Set<String> keys = store.keySet();
        assertFalse(keys.contains(key));
    }

    protected String extractParameter(String url, String parameterName) {
        Pattern pattern = Pattern.compile(parameterName + "=(.*?)(&|$)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    protected AuthorizationRequest initValidAuthorizationRequestCall() {
        Map<String, String> params = new HashMap<>();
        params.put(REDIRECT_URI_PARAM, REDIRECT_URI);
        params.put(CLIENT_ID_PARAM, CLIENT_ID);
        params.put(RESPONSE_TYPE_PARAM, CODE_RESPONSE_TYPE);
        params.put(STATE_PARAM, STATE);

        ClientResponse cr = responseFromGetAuthorizationWith(params);
        assertEquals(200, cr.getStatus());

        // get back the authorization request from the store for the needed authorization key
        Set<String> keys = store.keySet();
        assertEquals(1, keys.size());
        String key = keys.toArray(new String[0])[0];
        return AuthorizationRequest.get(key);
    }

    @Test
    public void shouldRetrieveAccessAndRefreshToken() throws IOException {
        AuthorizationRequest authorizationRequest = initValidAuthorizationRequestCall();
        String key = authorizationRequest.getAuthorizationKey();

        // get an authorization code
        Map<String, String> params = new HashMap<>();
        params.put(REDIRECT_URI_PARAM, REDIRECT_URI);
        params.put(CLIENT_ID_PARAM, CLIENT_ID);
        params.put(NuxeoOAuth2Servlet.AUTHORIZATION_KEY, key);
        params.put(NuxeoOAuth2Servlet.GRANT_ACCESS_PARAM, "true");
        params.put(STATE_PARAM, STATE);
        ClientResponse cr = responseFromPostAuthorizationWith(params);
        assertEquals(302, cr.getStatus());
        String redirect = cr.getHeaders().get("Location").get(0);
        String state = extractParameter(redirect, STATE_PARAM);
        assertEquals(STATE, state);
        String code = extractParameter(redirect, AUTHORIZATION_CODE_PARAM);

        // ensure we have only one authorization request
        Set<String> keys = store.keySet();
        assertTrue(keys.contains(code));
        assertFalse(keys.contains(key));

        // get access and refresh tokens
        params = new HashMap<>();
        params.put(REDIRECT_URI_PARAM, REDIRECT_URI);
        params.put(CLIENT_ID_PARAM, CLIENT_ID);
        params.put(GRANT_TYPE_PARAM, AUTHORIZATION_CODE_GRANT_TYPE);
        params.put(CLIENT_SECRET_PARAM, CLIENT_SECRET);
        params.put(AUTHORIZATION_CODE_PARAM, code);
        cr = responseFromTokenWith(params);
        assertEquals(200, cr.getStatus());
        String json = cr.getEntity(String.class);
        ObjectMapper obj = new ObjectMapper();
        Map<?, ?> token = obj.readValue(json, Map.class);
        assertNotNull(token);
        String accessToken = (String) token.get("access_token");
        assertEquals(32, accessToken.length());
        String refreshToken = (String) token.get("refresh_token");
        assertEquals(64, refreshToken.length());

        // ensure authorization request has been removed
        keys = store.keySet();
        assertFalse(keys.contains(code));
        assertFalse(keys.contains(key));

        // Refresh this token
        params.remove(AUTHORIZATION_CODE_PARAM);
        params.put(GRANT_TYPE_PARAM, REFRESH_TOKEN_GRANT_TYPE);
        params.put(REFRESH_TOKEN_PARAM, refreshToken);
        cr = responseFromTokenWith(params);
        assertEquals(200, cr.getStatus());
        json = cr.getEntity(String.class);
        Map<?, ?> refreshed = obj.readValue(json, Map.class);
        assertNotSame(refreshed.get("access_token"), token.get("access_token"));
    }

    protected ClientResponse responseFromTokenWith(Map<String, String> queryParams) {
        WebResource wr = client.resource(BASE_URL).path("oauth2").path(ENDPOINT_TOKEN);

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            params.add(entry.getKey(), entry.getValue());
        }

        return wr.queryParams(params).get(ClientResponse.class);
    }

    protected ClientResponse responseFromGetAuthorizationWith(Map<String, String> queryParams) {
        WebResource wr = client.resource(BASE_URL).path("oauth2").path(ENDPOINT_AUTH);

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            params.add(entry.getKey(), entry.getValue());
        }

        return wr.queryParams(params).get(ClientResponse.class);
    }

    protected ClientResponse responseFromPostAuthorizationWith(Map<String, String> queryParams) {
        WebResource wr = client.resource(BASE_URL).path("oauth2").path(ENDPOINT_AUTH_SUBMIT);

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            params.add(entry.getKey(), entry.getValue());
        }

        return wr.queryParams(params).post(ClientResponse.class);
    }
}
