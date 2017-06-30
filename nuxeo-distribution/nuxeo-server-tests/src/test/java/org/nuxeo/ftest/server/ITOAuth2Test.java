/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ftest.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.platform.oauth2.Constants.AUTHORIZATION_CODE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CLIENT_ID_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.CODE_RESPONSE_TYPE;
import static org.nuxeo.ecm.platform.oauth2.Constants.REDIRECT_URI_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.RESPONSE_TYPE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.Constants.STATE_PARAM;
import static org.nuxeo.ecm.platform.oauth2.NuxeoOAuth2Servlet.AUTHORIZATION_KEY;
import static org.nuxeo.ecm.platform.oauth2.NuxeoOAuth2Servlet.ENDPOINT_TOKEN;
import static org.nuxeo.ecm.platform.oauth2.NuxeoOAuth2Servlet.ERROR_PARAM;
import static org.nuxeo.ecm.platform.oauth2.OAuth2Error.ACCESS_DENIED;
import static org.nuxeo.ecm.platform.oauth2.request.AuthorizationRequest.MISSING_REQUIRED_FIELD_MESSAGE;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.platform.oauth2.NuxeoOAuth2Servlet;
import org.nuxeo.ecm.platform.oauth2.clients.OAuth2ClientService;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.RestHelper;
import org.nuxeo.functionaltests.pages.LoginPage;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Tests the OAuth2 authorization flow handled by the {@link NuxeoOAuth2Servlet}.
 *
 * @since 9.2
 */
public class ITOAuth2Test extends AbstractTest {

    public static class OAuth2Token {

        public final String accessToken;

        public final String refreshToken;

        public OAuth2Token(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    public static final String DOC_PATH = "/api/v1/path/";

    public static final String JSON_CMIS_PATH = "/json/cmis";

    public static final String ATOM_CMIS_PATH = "/atom/cmis";

    public static final String ATOM_CMIS10_PATH = "/atom/cmis10";

    protected Client client = Client.create();

    @BeforeClass
    public static void beforeClass() {
        RestHelper.createUser(TEST_USERNAME, TEST_PASSWORD);
        // Create a test OAuth2 client redirecting to localhost
        Map<String, String> properties = new HashMap<>();
        properties.put("name", "Test Client");
        properties.put("clientId", "test-client");
        properties.put("redirectURIs", "http://localhost:8080/nuxeo/home.html");
        RestHelper.createDirectoryEntry(OAuth2ClientService.OAUTH2CLIENT_DIRECTORY_NAME, properties);
    }

    @AfterClass
    public static void afterClass() {
        RestHelper.cleanup();
    }

    @After
    public void after() {
        logoutSimply();
    }

    @Test
    public void testAuhorizationErrors() {
        // No client_id parameter
        OAuth2ErrorPage errorPage = getOAuth2ErrorPage("/oauth2/authorize");
        assertTrue(driver.getTitle().endsWith("400"));
        errorPage.checkTitle("Bad Request");
        errorPage.checkDescription(String.format(MISSING_REQUIRED_FIELD_MESSAGE, CLIENT_ID_PARAM));

        // No response_type parameter
        errorPage = getOAuth2ErrorPage("/oauth2/authorize?client_id=test-client");
        errorPage.checkDescription(String.format(MISSING_REQUIRED_FIELD_MESSAGE, RESPONSE_TYPE_PARAM));

        // Invalid response_type parameter
        errorPage = getOAuth2ErrorPage("/oauth2/authorize?client_id=test-client&response_type=unknown");
        errorPage.checkDescription(String.format("Unknown %s: got \"unknown\", expecting \"%s\".", RESPONSE_TYPE_PARAM,
                CODE_RESPONSE_TYPE));

        // Invalid client_id parameter
        errorPage = getOAuth2ErrorPage("/oauth2/authorize?client_id=unknown&response_type=code");
        errorPage.checkDescription(String.format("Invalid %s: unknown.", CLIENT_ID_PARAM));

        // Invalid redirect_uri parameter
        errorPage = getOAuth2ErrorPage(
                "/oauth2/authorize?client_id=test-client&response_type=code&redirect_uri=unknown");
        errorPage.checkDescription(String.format(
                "Invalid %s parameter: unknown. It must exactly match one of the redirect URIs configured for the app.",
                REDIRECT_URI_PARAM));
    }

    @Test
    public void testAuthorizationSubmitErrors() {
        OAuth2GrantPage grantPage = getOAuth2GrantPage();
        // Simulate an invalid authorization_key parameter
        grantPage.setAuthorizationKey(driver, "unknown");
        grantPage.grant();
        OAuth2ErrorPage errorPage = asPage(OAuth2ErrorPage.class);
        errorPage.checkDescription(String.format("Invalid %s: unknown.", AUTHORIZATION_KEY));
    }

    @Test
    public void testAuthorizationDenied() throws MalformedURLException {
        OAuth2GrantPage grantPage = getOAuth2GrantPage();
        grantPage.deny();
        String currentURL = driver.getCurrentUrl();
        assertEquals("http://localhost:8080/nuxeo/home.html", URIUtils.getURIPath(currentURL));
        Map<String, String> expectedParameters = new HashMap<>();
        expectedParameters.put(ERROR_PARAM, ACCESS_DENIED);
        expectedParameters.put(STATE_PARAM, "1234");
        assertEquals(expectedParameters, URIUtils.getRequestParameters(currentURL));
    }

    @Test
    public void testAuthorizationGranted() throws MalformedURLException {
        OAuth2GrantPage grantPage = getOAuth2GrantPage();
        grantPage.grant();
        String currentURL = driver.getCurrentUrl();
        assertEquals("http://localhost:8080/nuxeo/home.html", URIUtils.getURIPath(currentURL));
        Map<String, String> parameters = URIUtils.getRequestParameters(currentURL);
        assertEquals(2, parameters.size());
        assertEquals("1234", parameters.get(STATE_PARAM));
        assertTrue(parameters.containsKey(AUTHORIZATION_CODE_PARAM));
    }

    @Test
    public void testAuthorizationOnRestAPI() throws IOException {
        OAuth2Token token = getOAuth2Token();

        checkAuthorizationWithValidAccessToken(DOC_PATH, token.accessToken);

        // refresh the access token
        OAuth2Token refreshedToken = refreshOAuth2Token(token.refreshToken);

        checkAuthorizationWithInvalidAccessToken(DOC_PATH, token.accessToken);

        checkAuthorizationWithValidAccessToken(DOC_PATH, refreshedToken.accessToken);
    }

    @Test
    public void testAuthorizationOnCMIS() throws IOException {
        OAuth2Token token = getOAuth2Token();

        checkAuthorizationWithValidAccessToken(JSON_CMIS_PATH, token.accessToken);
        checkAuthorizationWithValidAccessToken(ATOM_CMIS_PATH, token.accessToken);
        checkAuthorizationWithValidAccessToken(ATOM_CMIS10_PATH, token.accessToken);

        // refresh the access token
        OAuth2Token refreshedToken = refreshOAuth2Token(token.refreshToken);

        checkAuthorizationWithInvalidAccessToken(JSON_CMIS_PATH, token.accessToken);
        checkAuthorizationWithInvalidAccessToken(ATOM_CMIS_PATH, token.accessToken);
        checkAuthorizationWithInvalidAccessToken(ATOM_CMIS10_PATH, token.accessToken);

        checkAuthorizationWithValidAccessToken(JSON_CMIS_PATH, refreshedToken.accessToken);
        checkAuthorizationWithValidAccessToken(ATOM_CMIS_PATH, refreshedToken.accessToken);
        checkAuthorizationWithValidAccessToken(ATOM_CMIS10_PATH, refreshedToken.accessToken);
    }

    protected OAuth2Token getOAuth2Token() throws IOException {
        OAuth2GrantPage grantPage = getOAuth2GrantPage("Administrator", "Administrator");
        grantPage.grant();
        String currentURL = driver.getCurrentUrl();
        Map<String, String> parameters = URIUtils.getRequestParameters(currentURL);
        String code = parameters.get(AUTHORIZATION_CODE_PARAM);

        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("client_id", "test-client");
        params.put("code", code);
        return getOAuth2Token(params);
    }

    protected OAuth2Token getOAuth2Token(Map<String, String> params) throws IOException {
        WebResource wr = client.resource(NUXEO_URL).path("oauth2").path(ENDPOINT_TOKEN);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            formData.add(entry.getKey(), entry.getValue());
        }

        ClientResponse cr = wr.post(ClientResponse.class, formData);
        String json = cr.getEntity(String.class);
        ObjectMapper obj = new ObjectMapper();
        Map<?, ?> token = obj.readValue(json, Map.class);
        return new OAuth2Token((String) token.get("access_token"), (String) token.get("refresh_token"));
    }

    protected OAuth2Token refreshOAuth2Token(String refreshToken) throws IOException {
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("client_id", "test-client");
        params.put("refresh_token", refreshToken);
        return getOAuth2Token(params);
    }

    protected void checkAuthorizationWithValidAccessToken(String path, String accessToken) {
        WebResource wr = client.resource(NUXEO_URL).path(path);
        ClientResponse cr = wr.get(ClientResponse.class);
        assertEquals(401, cr.getStatus());

        wr = client.resource(NUXEO_URL).path(path);
        cr = wr.queryParam("access_token", accessToken).get(ClientResponse.class);
        assertEquals(200, cr.getStatus());

        wr = client.resource(NUXEO_URL).path(path);
        cr = wr.header("Authorization", "Bearer " + accessToken).get(ClientResponse.class);
        assertEquals(200, cr.getStatus());
    }

    protected void checkAuthorizationWithInvalidAccessToken(String path, String accessToken) {
        WebResource wr = client.resource(NUXEO_URL).path(path);
        ClientResponse cr = wr.queryParam("access_token", accessToken).get(ClientResponse.class);
        assertEquals(401, cr.getStatus());

        wr = client.resource(NUXEO_URL).path(path);
        cr = wr.header("Authorization", "Bearer " + accessToken).get(ClientResponse.class);
        assertEquals(401, cr.getStatus());
    }

    protected OAuth2ErrorPage getOAuth2ErrorPage(String resource) {
        driver.get(NUXEO_URL + resource);
        // First need to authenticate
        LoginPage loginPage = asPage(LoginPage.class);
        return loginPage.login(TEST_USERNAME, TEST_PASSWORD, OAuth2ErrorPage.class);
    }

    protected OAuth2GrantPage getOAuth2GrantPage() {
        return getOAuth2GrantPage(TEST_USERNAME, TEST_PASSWORD);
    }

    protected OAuth2GrantPage getOAuth2GrantPage(String username, String password) {
        driver.get(NUXEO_URL + "/oauth2/authorize?client_id=test-client&response_type=code&state=1234");
        // First need to authenticate
        LoginPage loginPage = asPage(LoginPage.class);
        OAuth2GrantPage grantPage = loginPage.login(username, password, OAuth2GrantPage.class);
        grantPage.checkClientName("Test Client");
        grantPage.checkAuthorizationKey();
        grantPage.checkState("1234");
        return grantPage;
    }

}
