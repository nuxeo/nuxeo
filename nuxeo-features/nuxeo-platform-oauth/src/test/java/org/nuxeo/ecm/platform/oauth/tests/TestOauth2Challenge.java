package org.nuxeo.ecm.platform.oauth.tests;

import static org.junit.Assert.*;
import static org.nuxeo.ecm.platform.ui.web.auth.oauth2.NuxeoOAuth2Filter.ERRORS;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.oauth2.clients.ClientRegistry;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2Client;
import org.nuxeo.ecm.platform.oauth2.request.AuthorizationRequest;
import org.nuxeo.ecm.webengine.test.WebEngineFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @author <a href="mailto:ak@nuxeo.com">Arnaud Kervern</a>
 * @since 5.9.2
 */
@RunWith(FeaturesRunner.class)
@Features({ OAuthFeature.class, WebEngineFeature.class })
@Jetty(port = 18090)
public class TestOauth2Challenge {

    protected static final String CLIENT_ID = "testClient";

    protected static final String CLIENT_SECRET = "testSecret";

    protected static final String BASE_URL = "http://localhost:18090";

    @Inject
    protected ClientRegistry clientRegistry;

    protected Client client;

    @Before
    public void initOAuthClient() throws ClientException {

        if (!clientRegistry.hasClient(CLIENT_ID)) {
            OAuth2Client oauthClient = new OAuth2Client("Dummy", CLIENT_ID,
                    CLIENT_SECRET);
            assertTrue(clientRegistry.registerClient(oauthClient));
        }

        ClientConfig config = new DefaultClientConfig();
        config.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS,
                false);
        // First client to request like a "Client" as OAuth RFC describe it
        client = Client.create(config);

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

        TestAuthorizationRequest.getRequests();

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
        assertTrue(redirect.contains("error=" + ERRORS.unauthorized_client));
    }

    @Test
    public void tokenShouldCreateAndRefreshWithDummyAuthorization()
            throws IOException {
        AuthorizationRequest request = new TestAuthorizationRequest(CLIENT_ID,
                "code", null, "Dummy", new Date());
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

        Map<?,?> token = obj.readValue(json, Map.class);
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
        Map<?,?> refreshed = obj.readValue(json, Map.class);

        assertNotSame(refreshed.get("access_token"), token.get("access_token"));
    }

    protected ClientResponse responseFromTokenWith(
            Map<String, String> queryParams) {
        WebResource wr = client.resource(BASE_URL).path("oauth2").path("token");

        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            params.add(entry.getKey(), entry.getValue());
        }

        return wr.queryParams(params).get(ClientResponse.class);
    }

    protected ClientResponse responseFromAuthorizationWith(
            Map<String, String> queryParams) {
        WebResource wr = client.resource(BASE_URL).path("oauth2").path(
                "authorization");

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
