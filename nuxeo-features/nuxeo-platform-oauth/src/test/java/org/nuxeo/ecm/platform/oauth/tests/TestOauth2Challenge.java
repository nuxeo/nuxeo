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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.platform.oauth2.OAuth2Error;
import org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2Client;
import org.nuxeo.ecm.platform.oauth2.request.AuthorizationRequest;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
@RunWith(FeaturesRunner.class)
@Features({ OAuthFeature.class, WebEngineFeature.class })
@LocalDeploy("org.nuxeo.ecm.platform.oauth:OSGI-INF/jetty-test-config.xml")
@Jetty(port = 18090)
public class TestOauth2Challenge {

    protected static final String CLIENT_ID = "testClient";

    protected static final String CLIENT_SECRET = "testSecret";

    protected static final String BASE_URL = "http://localhost:18090";

    private static final Integer TIMEOUT = Integer.valueOf(1000 * 60 * 5); // 5min

    @Inject
    protected ClientRegistry clientRegistry;

    protected Client client;

    @Before
    public void initOAuthClient() {

        if (!clientRegistry.hasClient(CLIENT_ID)) {
            OAuth2Client oauthClient = new OAuth2Client("Dummy", CLIENT_ID, CLIENT_SECRET);
            assertTrue(clientRegistry.registerClient(oauthClient));

            // commit the transaction so that the HTTP thread finds the newly created directory entry
            if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
                TransactionHelper.commitOrRollbackTransaction();
                TransactionHelper.startTransaction();
            }
        }

        // First client to request like a "Client" as OAuth RFC describe it
        client = Client.create();
        client.setConnectTimeout(TIMEOUT);
        client.setReadTimeout(TIMEOUT);
        client.setFollowRedirects(Boolean.FALSE);

        TestAuthorizationRequest.getRequests().clear();
    }

    @Test
    public void authorizationShouldRedirectToJSP() {
        // Request an code
        Map<String, String> params = new HashMap<>();
        params.put("redirect_uri", "Dummy");
        params.put("client_id", CLIENT_ID);
        params.put("response_type", "code");

        ClientResponse cr = responseFromAuthorizationWith(params);
        assertEquals(302, cr.getStatus());

        String redirect = cr.getHeaders().get("Location").get(0);
        assertTrue(redirect.contains(".jsp"));
    }

    @Test
    public void authorizationShouldForbidsUnknownClient() {
        // Request an code
        Map<String, String> params = new HashMap<>();
        params.put("redirect_uri", "Dummy");
        params.put("client_id", "unknow");
        params.put("response_type", "code");

        ClientResponse cr = responseFromAuthorizationWith(params);
        assertEquals(302, cr.getStatus());
        String redirect = cr.getHeaders().get("Location").get(0);
        assertTrue(redirect.contains("error=" + OAuth2Error.UNAUTHORIZED_CLIENT.toString().toLowerCase()));
    }

    @Test
    public void tokenShouldCreateAndRefreshWithDummyAuthorization() throws IOException {
        AuthorizationRequest request = new TestAuthorizationRequest(CLIENT_ID, "code", null, "Dummy", new Date());
        TestAuthorizationRequest.getRequests().put("fake", request);

        // Request a token
        Map<String, String> params = new HashMap<>();
        params.put("redirect_uri", "Dummy");
        params.put("client_id", CLIENT_ID);
        params.put("client_secret", CLIENT_SECRET);
        params.put("grant_type", "authorization_code");
        params.put("code", request.getAuthorizationCode());

        ClientResponse cr = responseFromTokenWith(params);
        assertEquals(200, cr.getStatus());
        String json = cr.getEntity(String.class);

        ObjectMapper obj = new ObjectMapper();

        Map<?, ?> token = obj.readValue(json, Map.class);
        assertNotNull(token);
        String accessToken = (String) token.get("access_token");
        assertEquals(32, accessToken.length());

        // Refresh this token
        params.remove("code");
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", (String) token.get("refresh_token"));
        cr = responseFromTokenWith(params);
        assertEquals(200, cr.getStatus());

        json = cr.getEntity(String.class);
        Map<?, ?> refreshed = obj.readValue(json, Map.class);

        assertNotSame(refreshed.get("access_token"), token.get("access_token"));
    }

    protected ClientResponse responseFromTokenWith(Map<String, String> queryParams) {
        WebResource wr = client.resource(BASE_URL).path("oauth2").path("token");

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            params.add(entry.getKey(), entry.getValue());
        }

        return wr.queryParams(params).get(ClientResponse.class);
    }

    protected ClientResponse responseFromAuthorizationWith(Map<String, String> queryParams) {
        WebResource wr = client.resource(BASE_URL).path("oauth2").path("authorization");

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            params.add(entry.getKey(), entry.getValue());
        }

        return wr.queryParams(params).get(ClientResponse.class);
    }

    protected ClientResponse responseFromUrl(String url, String accessToken) {
        WebResource wr = client.resource(BASE_URL).path(url);
        wr.header("Authentication", "Bearer " + accessToken);
        return wr.get(ClientResponse.class);
    }
}
