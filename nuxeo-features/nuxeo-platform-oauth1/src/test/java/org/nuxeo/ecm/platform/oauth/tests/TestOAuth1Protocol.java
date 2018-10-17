/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.oauth.tests;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.servlet.http.HttpServletResponse.SC_MOVED_TEMPORARILY;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.test.ServletContainerTransactionalFeature;
import org.nuxeo.ecm.platform.oauth.consumers.NuxeoOAuthConsumer;
import org.nuxeo.ecm.platform.oauth.consumers.OAuthConsumerRegistry;
import org.nuxeo.ecm.platform.oauth.tokens.OAuthToken;
import org.nuxeo.ecm.platform.oauth.tokens.OAuthTokenStore;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.jaxrs.test.JerseyClientHelper;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthMessage;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features({ OAuth1Feature.class, ServletContainerTransactionalFeature.class })
@ServletContainer(port = 18090)
@Deploy("org.nuxeo.ecm.platform.oauth1:OSGI-INF/test-servletcontainer-config.xml")
@Deploy("org.nuxeo.ecm.platform.oauth1:OSGI-INF/test-authentication-config.xml")
public class TestOAuth1Protocol {

    protected static final String HMAC_SHA1 = "HMAC-SHA1";

    protected static final String BASE_URL = "http://localhost:18090";

    protected static final String ENDPOINT = "oauth";

    protected static final String ENDPOINT_REQUEST_TOKEN = "request-token";

    protected static final String ENDPOINT_AUTHORIZE = "authorize";

    protected static final String ENDPOINT_ACCESS_TOKEN = "access-token";

    protected static final String CONSUMER = "myconsumer";

    protected static final String CONSUMER_SECRET = "mysecret";

    protected static final String CALLBACK_URL = "http://my-site";

    protected static final String BAD_SIGNATURE = "YmFkc2ln"; // base64 for "badsig"

    @Inject
    protected OAuthConsumerRegistry consumerRegistry;

    @Inject
    protected OAuthTokenStore tokenStore;

    @Inject
    protected TransactionalFeature txFeature;

    protected Client client;

    /**
     * Dummy filter that just records that it was executed.
     */
    public static class DummyFilter extends HttpFilter {

        private static final long serialVersionUID = 1L;

        public static String info;

        @Override
        public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            Principal principal = request.getUserPrincipal();
            info = principal == null ? "null" : principal.getName();
            super.doFilter(request, response, chain);
        }
    }

    @Before
    public void setUp() {
        client = JerseyClientHelper.clientBuilder().setRedirectsEnabled(false).build();
    }

    @After
    public void tearDown() {
        client.destroy();
    }

    protected static MultivaluedMap<String, String> multivalued(Map<String, String> map) {
        MultivaluedMap<String, String> mvmap = new MultivaluedMapImpl();
        map.forEach((k, v) -> mvmap.putSingle(k, v));
        return mvmap;
    }

    protected String signURL(String method, WebResource resource, Map<String, String> params, OAuthConsumer consumer,
            String tokenSecret) throws Exception {
        String url = resource.getURI().toASCIIString();
        OAuthMessage message = new OAuthMessage(method, url, params.entrySet(), null);
        OAuthAccessor accessor = new OAuthAccessor(consumer);
        accessor.tokenSecret = tokenSecret;
        message.sign(accessor);
        return message.getSignature();
    }

    @Test
    public void testRequestTokenBadConsumerKey() {
        WebResource resource = client.resource(BASE_URL).path(ENDPOINT).path(ENDPOINT_REQUEST_TOKEN);
        Map<String, String> params = new HashMap<>();
        params.put("oauth_consumer_key", "nosuchconsumer");
        ClientResponse cr = resource.queryParams(multivalued(params)).get(ClientResponse.class);
        try (CloseableClientResponse response = CloseableClientResponse.of(cr)) {
            assertEquals(SC_UNAUTHORIZED, response.getStatus());
        }
    }

    @Test
    public void testRequestToken() throws Exception {
        doTestRequestToken(false);
    }

    @Test
    public void testRequestTokenBadSignature() throws Exception {
        doTestRequestToken(true);
    }

    protected void doTestRequestToken(boolean badSignature) throws Exception {
        // create consumer
        NuxeoOAuthConsumer consumer = new NuxeoOAuthConsumer(null, CONSUMER, CONSUMER_SECRET, null);
        consumerRegistry.storeConsumer(consumer);

        txFeature.nextTransaction();

        WebResource resource = client.resource(BASE_URL).path(ENDPOINT).path(ENDPOINT_REQUEST_TOKEN);
        Map<String, String> params = new HashMap<>();
        params.put("oauth_consumer_key", CONSUMER);
        params.put("oauth_signature_method", HMAC_SHA1);
        params.put("oauth_timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("oauth_nonce", "abcdefgh");
        String signature = signURL("GET", resource, params, consumer, null);
        params.put("oauth_signature", badSignature ? BAD_SIGNATURE : signature);

        ClientResponse cr = resource.queryParams(multivalued(params)).get(ClientResponse.class);
        try (CloseableClientResponse response = CloseableClientResponse.of(cr)) {
            if (badSignature) {
                assertEquals(SC_UNAUTHORIZED, response.getStatus());
            } else {
                assertEquals(SC_OK, response.getStatus());
                assertEquals("application/x-www-form-urlencoded; charset=ISO-8859-1", response.getType().toString());
                String body = response.getEntity(String.class);
                List<NameValuePair> res = URLEncodedUtils.parse(body, UTF_8);
                assertEquals(res.toString(), 3, res.size());
                assertEquals("oauth_token", res.get(0).getName());
                String token = res.get(0).getValue();
                assertEquals("oauth_token_secret", res.get(1).getName());
                assertEquals("oauth_callback_confirmed", res.get(2).getName());
                assertEquals("true", res.get(2).getValue()); // OAuth 1.0a
                // check request token created
                OAuthToken rToken = tokenStore.getRequestToken(token);
                assertNotNull(rToken);
            }
        }
    }

    @Test
    public void testAuthorizeGetBadToken() throws Exception {
        WebResource resource = client.resource(BASE_URL).path(ENDPOINT).path(ENDPOINT_AUTHORIZE);
        Map<String, String> params = new HashMap<>();
        params.put("oauth_token", "nosuchtoken");
        ClientResponse cr = resource.queryParams(multivalued(params)).get(ClientResponse.class);
        try (CloseableClientResponse response = CloseableClientResponse.of(cr)) {
            assertEquals(SC_MOVED_TEMPORARILY, response.getStatus());
            URI uri = response.getLocation();
            String expectedRedir = "oauthGrant.jsp?oauth_token=nosuchtoken";
            String expected = BASE_URL + "/login.jsp?requestedUrl=" + URLEncoder.encode(expectedRedir, "UTF-8");
            assertEquals(expected, uri.toASCIIString());
        }
    }

    @Test
    public void testAuthorizeGet() throws Exception {
        // create request token
        OAuthToken rToken = tokenStore.createRequestToken(CONSUMER, CALLBACK_URL);

        txFeature.nextTransaction();

        WebResource resource = client.resource(BASE_URL).path(ENDPOINT).path(ENDPOINT_AUTHORIZE);
        Map<String, String> params = new HashMap<>();
        params.put("oauth_token", rToken.getToken());
        ClientResponse cr = resource.queryParams(multivalued(params)).get(ClientResponse.class);
        try (CloseableClientResponse response = CloseableClientResponse.of(cr)) {
            assertEquals(SC_MOVED_TEMPORARILY, response.getStatus());
            URI uri = response.getLocation();
            String expectedRedir = "oauthGrant.jsp?oauth_token=" + rToken.getToken();
            String expected = BASE_URL + "/login.jsp?requestedUrl=" + URLEncoder.encode(expectedRedir, "UTF-8");
            assertEquals(expected, uri.toASCIIString());
        }
    }

    @Test
    public void testAuthorizePost() throws Exception {
        // create request token
        OAuthToken rToken = tokenStore.createRequestToken(CONSUMER, CALLBACK_URL);

        txFeature.nextTransaction();

        WebResource resource = client.resource(BASE_URL).path(ENDPOINT).path(ENDPOINT_AUTHORIZE);
        Map<String, String> params = new HashMap<>();
        params.put("oauth_token", rToken.getToken());
        params.put("nuxeo_login", "bob");
        params.put("duration", "60"); // minutes
        ClientResponse cr = resource.queryParams(multivalued(params)).post(ClientResponse.class);
        try (CloseableClientResponse response = CloseableClientResponse.of(cr)) {
            assertEquals(SC_MOVED_TEMPORARILY, response.getStatus());
            URI uri = response.getLocation();

            Map<String, String> parameters = new LinkedHashMap<String, String>();
            parameters.put("oauth_token", rToken.getToken());
            parameters.put("oauth_verifier", rToken.getVerifier());
            String expected = URIUtils.addParametersToURIQuery(CALLBACK_URL, parameters);
            assertEquals(expected, uri.toASCIIString());
        }

        // checks that a verifier was added to the request token
        assertNotNull(rToken.getVerifier());
        // check that the requested login was associated to the request token
        assertEquals("bob", rToken.getNuxeoLogin());
    }

    @Test
    public void testAccessToken() throws Exception {
        doTestAccessToken(false, false);
    }

    @Test
    public void testAccessTokenBadSignature() throws Exception {
        doTestAccessToken(true, false);
    }

    @Test
    public void testAccessTokenBadVerifier() throws Exception {
        doTestAccessToken(false, true);
    }

    protected void doTestAccessToken(boolean badSignature, boolean badVerifier) throws Exception {
        // create consumer
        NuxeoOAuthConsumer consumer = new NuxeoOAuthConsumer(null, CONSUMER, CONSUMER_SECRET, null);
        consumerRegistry.storeConsumer(consumer);
        // create request token
        OAuthToken rToken = tokenStore.createRequestToken(CONSUMER, CALLBACK_URL);
        // include verifier and login from authorize phase
        tokenStore.addVerifierToRequestToken(rToken.getToken(), Long.valueOf(60));
        rToken.setNuxeoLogin("bob");

        txFeature.nextTransaction();

        WebResource resource = client.resource(BASE_URL).path(ENDPOINT).path(ENDPOINT_ACCESS_TOKEN);
        Map<String, String> params = new HashMap<>();
        params.put("oauth_consumer_key", CONSUMER);
        params.put("oauth_token", rToken.getToken());
        params.put("oauth_verifier", badVerifier ? "badverif" : rToken.getVerifier());
        params.put("oauth_signature_method", HMAC_SHA1);
        params.put("oauth_timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("oauth_nonce", "123456789");
        String signature = signURL("GET", resource, params, consumer, rToken.getTokenSecret());
        params.put("oauth_signature", badSignature ? BAD_SIGNATURE : signature);

        ClientResponse cr = resource.queryParams(multivalued(params)).get(ClientResponse.class);
        try (CloseableClientResponse response = CloseableClientResponse.of(cr)) {
            if (badSignature || badVerifier) {
                assertEquals(SC_UNAUTHORIZED, response.getStatus());
            } else {
                assertEquals(SC_OK, response.getStatus());
                assertEquals("application/x-www-form-urlencoded; charset=ISO-8859-1", response.getType().toString());
                String body = response.getEntity(String.class);
                List<NameValuePair> res = URLEncodedUtils.parse(body, UTF_8);
                assertEquals(res.toString(), 2, res.size());
                assertEquals("oauth_token", res.get(0).getName());
                String token = res.get(0).getValue();
                assertEquals("oauth_token_secret", res.get(1).getName());
                String secret = res.get(1).getValue();
                // check request token is gone
                assertNull(tokenStore.getRequestToken(rToken.getToken()));
                // check access token exists
                OAuthToken aToken = tokenStore.getAccessToken(token);
                assertNotNull(aToken);
                assertEquals(aToken.getTokenSecret(), secret);
                assertEquals("bob", aToken.getNuxeoLogin());
            }
        }
    }

    @Test
    public void testSignedRequestTwoLegged() throws Exception {
        // create consumer
        NuxeoOAuthConsumer consumer = new NuxeoOAuthConsumer(null, CONSUMER, CONSUMER_SECRET, null);
        setSignedFetchSupport(consumer, "Administrator");
        consumerRegistry.storeConsumer(consumer);
        // create request token
        tokenStore.createRequestToken(CONSUMER, CALLBACK_URL);
        // two-legged: no token
        String token = "";
        String tokenSecret = null;

        doTestSignedRequest(consumer, token, tokenSecret);
    }

    @Test
    public void testSignedRequestThreeLegged() throws Exception {
        // create consumer
        NuxeoOAuthConsumer consumer = new NuxeoOAuthConsumer(null, CONSUMER, CONSUMER_SECRET, null);
        consumerRegistry.storeConsumer(consumer);
        // create request token
        OAuthToken rToken = tokenStore.createRequestToken(CONSUMER, CALLBACK_URL);
        rToken.setNuxeoLogin("Administrator"); // present in the default usermanager
        // exchange with access token
        OAuthToken aToken = tokenStore.createAccessTokenFromRequestToken(rToken);
        String token = aToken.getToken();
        String tokenSecret = aToken.getTokenSecret();

        doTestSignedRequest(consumer, token, tokenSecret);
    }

    protected void setSignedFetchSupport(NuxeoOAuthConsumer consumer, String value)
            throws ReflectiveOperationException {
        Field field = consumer.getClass().getDeclaredField("signedFetchSupport");
        field.setAccessible(true);
        field.set(consumer, value);
    }

    protected void doTestSignedRequest(OAuthConsumer consumer, String token, String tokenSecret) throws Exception {
        txFeature.nextTransaction();

        DummyFilter.info = null;
        WebResource resource = client.resource(BASE_URL).path("somepage.html");
        Map<String, String> params = new HashMap<>();
        params.put("oauth_consumer_key", CONSUMER);
        params.put("oauth_token", token);
        params.put("oauth_signature_method", HMAC_SHA1);
        params.put("oauth_timestamp", String.valueOf(System.currentTimeMillis() / 1000));
        params.put("oauth_nonce", "123456789");
        String signature = signURL("GET", resource, params, consumer, tokenSecret);
        params.put("oauth_signature", signature);
        Builder builder = resource.queryParams(multivalued(params)).header("Authorization", "OAuth");
        ClientResponse cr = builder.get(ClientResponse.class);
        try (CloseableClientResponse response = CloseableClientResponse.of(cr)) {
            assertEquals(SC_NOT_FOUND, response.getStatus());
            // but the request was authenticated (our dummy filter captured the user)
            assertEquals("Administrator", DummyFilter.info);
        }
    }

}
