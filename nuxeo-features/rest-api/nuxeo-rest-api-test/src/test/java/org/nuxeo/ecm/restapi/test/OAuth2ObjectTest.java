/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Gabriel Barata <gbarata@nuxeo.com>
 */
package org.nuxeo.ecm.restapi.test;

import com.sun.jersey.api.client.ClientResponse;
import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import javax.ws.rs.core.Response;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features({RestServerFeature.class, CoreFeature.class, DirectoryFeature.class})
@Jetty(port = 18090)
@Deploy({ "org.nuxeo.ecm.platform.oauth",
          "org.nuxeo.ecm.directory.api",
          "org.nuxeo.ecm.directory",
          "org.nuxeo.ecm.directory.types.contrib" })
@LocalDeploy({"org.nuxeo.ecm.platform.restapi.test:test-oauth2provider-config.xml",
              "org.nuxeo.ecm.platform.restapi.test:test-oauth2-directory-contrib.xml"})
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class OAuth2ObjectTest extends BaseTest {

    public static final String OAUTH2_PROVIDER_TYPE = "nuxeoOAuth2ServiceProvider";

    public static final String OAUTH2_PROVIDERS_TYPE = "nuxeoOAuth2ServiceProviders";

    public static final String OAUTH2_TOKEN_TYPE = "nuxeoOAuth2Token";

    public static final String OAUTH2_TOKENS_TYPE = "nuxeoOAuth2Tokens";

    public static final String TEST_OAUTH2_PROVIDER = "test-oauth2-provider";

    public static final String TEST_OAUTH2_PROVIDER_2 = "test-oauth2-provider-2";

    public static final String TEST_OAUTH2_CLIENTID = "clientId";

    public static final String TEST_OAUTH2_USER = "Administrator";

    public static final String TEST_OAUTH2_SERVICE_USERID = TEST_OAUTH2_USER + "@email.com";

    public static final String TEST_OAUTH2_ACCESS_TOKEN = "y38Hs3_sdas98l";

    protected static final String PROVIDER_PATH = "oauth2/provider";

    protected static final String TOKEN_PATH = "oauth2/token";

    protected static final String AUTHORIZATION_SERVER_URL = "https://test.oauth2.provider/authorization";

    protected static String getScopeUrl(int id) {
        return "https://test.oauth2.provider/scopes/scope" + Integer.toString(id);
    }

    protected static String getProviderPath(String providerId) {
        return PROVIDER_PATH + "/" + providerId;
    }

    protected static String getTokenPath(String providerId) {
        return getProviderPath(providerId) + "/token";
    }

    // test oauth2/provider

    @Test
    public void iCanGetProviders() throws IOException {
        ClientResponse response = getResponse(RequestType.GET, PROVIDER_PATH);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(OAUTH2_PROVIDERS_TYPE, node.get("entity-type").getTextValue());
        assertNotNull(node.get("entries"));
        assertEquals(2, node.get("entries").size());
        verifyProvider(node.get("entries").get(0), TEST_OAUTH2_PROVIDER, true);
        verifyProvider(node.get("entries").get(1), TEST_OAUTH2_PROVIDER_2, false);
    }

    @Test
    public void iCanGetProvider() throws IOException {
        ClientResponse response = getResponse(RequestType.GET, getProviderPath(TEST_OAUTH2_PROVIDER));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        verifyProvider(node, TEST_OAUTH2_PROVIDER, true);
    }

    @Test
    public void iCantGetInvalidProvider() throws IOException {
        ClientResponse response = getResponse(RequestType.GET, getProviderPath("fake"));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("Invalid provider: fake", getErrorMessage(node));
    }

    @Test
    public void iCanCreateProvider() throws IOException {
        String serviceName = "myservice";
        String data =
            "{\n" +
            "    \"authorizationServerURL\": \"https://test.oauth2.provider/authorization\",\n" +
            "    \"clientId\": \"clientId\",\n" +
            "    \"clientSecret\": \"123secret321\",\n" +
            "    \"description\": \"My Service\",\n" +
            "    \"entity-type\": \"nuxeoOAuth2ServiceProvider\",\n" +
            "    \"isEnabled\": true,\n" +
            "    \"scopes\": [\n" +
            "        \"https://test.oauth2.provider/scopes/scope0\",\n" +
            "        \"https://test.oauth2.provider/scopes/scope1\"\n" +
            "    ],\n" +
            "    \"serviceName\": \"myservice\",\n" +
            "    \"tokenServerURL\": \"https://test.oauth2.provider/token\"\n" +
            "}";
        ClientResponse response = getResponse(RequestType.POST, PROVIDER_PATH, data);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        verifyProvider(node, serviceName, false);

        service = getServiceFor("user1", "user1");
        response = getResponse(RequestType.POST, PROVIDER_PATH, data);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void iCanUpdateProvider() throws IOException {
        ClientResponse response = getResponse(RequestType.GET, getProviderPath(TEST_OAUTH2_PROVIDER_2));
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("clientId", node.get("clientId").getTextValue());
        assertTrue(node.get("clientSecret").isNull());
        assertFalse(node.get("isEnabled").getBooleanValue());
        String data =
            "{\n" +
                "    \"authorizationServerURL\": \"https://test.oauth2.provider/authorization\",\n" +
                "    \"clientId\": \"myId\",\n" +
                "    \"clientSecret\": \"123secret321\",\n" +
                "    \"description\": \"Test OAuth2 Provider 2\",\n" +
                "    \"entity-type\": \"nuxeoOAuth2ServiceProvider\",\n" +
                "    \"isEnabled\": true,\n" +
                "    \"scopes\": [\n" +
                "        \"https://test.oauth2.provider/scopes/scope0\",\n" +
                "        \"https://test.oauth2.provider/scopes/scope1\"\n" +
                "    ],\n" +
                "    \"serviceName\": \"test-oauth2-provider-2\",\n" +
                "    \"tokenServerURL\": \"https://test.oauth2.provider/token\"\n" +
                "}";
        response = getResponse(RequestType.PUT, getProviderPath(TEST_OAUTH2_PROVIDER_2), data);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals("myId", node.get("clientId").getTextValue());
        assertEquals("123secret321", node.get("clientSecret").getTextValue());
        assertTrue(node.get("isEnabled").getBooleanValue());

        service = getServiceFor("user1", "user1");
        response = getResponse(RequestType.PUT, getProviderPath(TEST_OAUTH2_PROVIDER_2), data);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void iCantUpdateInvalidProvider() throws IOException {
        String data =
            "{\n" +
                "    \"authorizationServerURL\": \"https://test.oauth2.provider/authorization\",\n" +
                "    \"clientId\": \"myId\",\n" +
                "    \"clientSecret\": \"123secret321\",\n" +
                "    \"description\": \"Test OAuth2 Provider 2\",\n" +
                "    \"entity-type\": \"nuxeoOAuth2ServiceProvider\",\n" +
                "    \"isEnabled\": true,\n" +
                "    \"scopes\": [\n" +
                "        \"https://test.oauth2.provider/scopes/scope0\",\n" +
                "        \"https://test.oauth2.provider/scopes/scope1\"\n" +
                "    ],\n" +
                "    \"serviceName\": \"test-oauth2-provider-2\",\n" +
                "    \"tokenServerURL\": \"https://test.oauth2.provider/token\"\n" +
                "}";
        ClientResponse response = getResponse(RequestType.PUT, getProviderPath("fake"), data);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void iCanDeleteProvider() throws IOException {
        ClientResponse response = getResponse(RequestType.DELETE, getProviderPath(TEST_OAUTH2_PROVIDER_2));
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        service = getServiceFor("user1", "user1");
        response = getResponse(RequestType.DELETE, getProviderPath(TEST_OAUTH2_PROVIDER));
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void iCantDeleteInvalidProvider() throws IOException {
        ClientResponse response = getResponse(RequestType.DELETE, getProviderPath("fake"));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    // test oauth2/provider/{provider}/token

    @Test
    public void iCanGetProviderToken() throws IOException {
        ClientResponse response = getResponse(RequestType.GET, getTokenPath(TEST_OAUTH2_PROVIDER));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(TEST_OAUTH2_ACCESS_TOKEN, node.get("token").getTextValue());
    }

    @Test
    public void iCantGetTokenInvalidProvider() throws IOException {
        ClientResponse response = getResponse(RequestType.GET, getTokenPath("fake"));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("Invalid provider: fake", getErrorMessage(node));
    }

    protected void verifyProvider(JsonNode node, String serviceName, Boolean checkToken) {
        assertEquals(OAUTH2_PROVIDER_TYPE, node.get("entity-type").getTextValue());
        assertEquals(serviceName, node.get("serviceName").getTextValue());
        assertEquals(TEST_OAUTH2_CLIENTID, node.get("clientId").getTextValue());
        assertEquals(AUTHORIZATION_SERVER_URL + "?client_id=" + TEST_OAUTH2_CLIENTID +
                "&redirect_uri=http://localhost:18090/site/oauth2/" + serviceName + "/callback" +
                "&response_type=code&scope=" + getScopeUrl(0) + "%20" + getScopeUrl(1),
            node.get("authorizationURL").getTextValue());
        if (checkToken) {
            assertEquals(TEST_OAUTH2_SERVICE_USERID, node.get("userId").getTextValue());
        }
    }

    // test oauth2/token
    @Test
    public void iCanGetTokens() throws IOException {
        ClientResponse response = getResponse(RequestType.GET, TOKEN_PATH);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(OAUTH2_TOKENS_TYPE, node.get("entity-type").getTextValue());
        assertNotNull(node.get("entries"));
        assertEquals(2, node.get("entries").size());
        verifyToken(node.get("entries"), TEST_OAUTH2_PROVIDER, "Administrator", "2017-05-09 11:11:11");
        verifyToken(node.get("entries"), TEST_OAUTH2_PROVIDER, "user1", "2017-05-09 11:11:11");

        service = getServiceFor("user1", "user1");
        response = getResponse(RequestType.GET, TOKEN_PATH);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }



    // test oauth2/token/{provider}/{user}
    @Test
    public void iCanGetToken() throws IOException {
        ClientResponse response = getResponse(RequestType.GET, TOKEN_PATH + "/" + TEST_OAUTH2_PROVIDER + "/user1");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(OAUTH2_TOKEN_TYPE, node.get("entity-type").getTextValue());
        verifyToken(node, TEST_OAUTH2_PROVIDER, "user1", "2017-05-09 11:11:11");

        service = getServiceFor("user1", "user1");
        response = getResponse(RequestType.GET, TOKEN_PATH + "/" + TEST_OAUTH2_PROVIDER + "/user1");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void iCanUpdateToken() throws IOException {
        String data =
            "{  \n" +
            "   \"entity-type\":\"nuxeoOAuth2Token\",\n" +
            "   \"clientId\":null,\n" +
            "   \"creationDate\":\"2017-05-10 11:11:11\",\n" +
            "   \"isShared\":false,\n" +
            "   \"nuxeoLogin\":\"user1\",\n" +
            "   \"serviceLogin\":\"my1@mail\",\n" +
            "   \"serviceName\":\"test-oauth2-provider\",\n" +
            "   \"sharedWith\":[  \n" +
            "   ]\n" +
            "}";
        ClientResponse response = getResponse(RequestType.PUT, TOKEN_PATH + "/" + TEST_OAUTH2_PROVIDER + "/user1", data);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(OAUTH2_TOKEN_TYPE, node.get("entity-type").getTextValue());
        verifyToken(node, TEST_OAUTH2_PROVIDER, "user1", "2017-05-10 11:11:11");

        service = getServiceFor("user1", "user1");
        response = getResponse(RequestType.PUT, TOKEN_PATH + "/" + TEST_OAUTH2_PROVIDER + "/user1", data);
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
    }

    @Test
    public void iCanDeleteToken() {
        service = getServiceFor("user1", "user1");
        ClientResponse response = getResponse(RequestType.DELETE, TOKEN_PATH + "/" + TEST_OAUTH2_PROVIDER + "/user1");
        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

        service = getServiceFor("Administrator", "Administrator");
        response = getResponse(RequestType.DELETE, TOKEN_PATH + "/" + TEST_OAUTH2_PROVIDER + "/user1");
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        response = getResponse(RequestType.GET, TOKEN_PATH + "/" + TEST_OAUTH2_PROVIDER + "/user1");
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    protected void verifyToken(JsonNode node, String serviceName, String nxuser, String creationDate) {
        if (node.isArray()) {
            JsonNode token = null;
            for (int i = 0; i < node.size(); i++) {
                if (node.get(i).get("entity-type").getTextValue().equals(OAUTH2_TOKEN_TYPE) &&
                    node.get(i).get("serviceName").getTextValue().equals(serviceName) &&
                    node.get(i).get("nuxeoLogin").getTextValue().equals(nxuser)  &&
                    node.get(i).get("creationDate").getTextValue().equals(creationDate)) {
                    token = node.get(i);
                }
            }
            assertNotNull(token);
        } else {
            assertEquals(OAUTH2_TOKEN_TYPE, node.get("entity-type").getTextValue());
            assertEquals(serviceName, node.get("serviceName").getTextValue());
            assertEquals(nxuser, node.get("nuxeoLogin").getTextValue());
            assertEquals(creationDate, node.get("creationDate").getTextValue());
        }
    }
}
