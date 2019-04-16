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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, CoreFeature.class, DirectoryFeature.class })
@Deploy("org.nuxeo.ecm.platform.oauth")
@Deploy("org.nuxeo.ecm.directory.api")
@Deploy("org.nuxeo.ecm.directory")
@Deploy("org.nuxeo.ecm.directory.types.contrib")
@Deploy("org.nuxeo.ecm.platform.restapi.test:test-oauth2provider-config.xml")
@Deploy("org.nuxeo.ecm.platform.restapi.test:test-oauth2-directory-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class OAuth2ObjectTest extends BaseTest {

    public static final String OAUTH2_PROVIDER_TYPE = "nuxeoOAuth2ServiceProvider";

    public static final String OAUTH2_PROVIDERS_TYPE = "nuxeoOAuth2ServiceProviders";

    public static final String OAUTH2_TOKEN_TYPE = "nuxeoOAuth2Token";

    public static final String OAUTH2_TOKENS_TYPE = "nuxeoOAuth2Tokens";

    public static final String OAUTH2_CLIENT_TYPE = "oauth2Client";

    public static final String OAUTH2_CLIENTS_TYPE = "oauth2Clients";

    public static final String TEST_OAUTH2_PROVIDER = "test-oauth2-provider";

    public static final String TEST_OAUTH2_PROVIDER_2 = "test-oauth2-provider-2";

    public static final String TOKEN_STORE = "org.nuxeo.server.token.store";

    public static final String TEST_OAUTH2_CLIENTID = "clientId";

    public static final String TEST_OAUTH2_USER = "Administrator";

    public static final String TEST_OAUTH2_SERVICE_USERID = TEST_OAUTH2_USER + "@email.com";

    public static final String TEST_OAUTH2_ACCESS_TOKEN = "y38Hs3_sdas98l";

    protected static final String PROVIDER_PATH = "oauth2/provider";

    protected static final String TOKEN_PATH = "oauth2/token";

    protected static final String CLIENT_PATH = "oauth2/client";

    protected static final String PROVIDER_TOKEN_PATH = "oauth2/token/provider";

    protected static final String CLIENT_TOKEN_PATH = "oauth2/token/client";

    protected static final String TEST_CLIENT = "my-client";

    protected static final String TEST_CLIENT_NAME = "my-client-name";

    protected static final String TEST_CLIENT_2 = "my-client-2";

    protected static final String TEST_CLIENT_NAME_2 = "my-second-client-name";

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

    protected static String getClientPath(String clientId) {
        return CLIENT_PATH + "/" + clientId;
    }

    // test oauth2/provider

    @Test
    public void iCanGetProviders() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET, PROVIDER_PATH)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(OAUTH2_PROVIDERS_TYPE, node.get("entity-type").textValue());
            assertNotNull(node.get("entries"));
            assertEquals(2, node.get("entries").size());
            verifyProvider(node.get("entries"), TEST_OAUTH2_PROVIDER, true);
            verifyProvider(node.get("entries"), TEST_OAUTH2_PROVIDER_2, false);
        }
    }

    @Test
    public void iCanGetProvider() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET, getProviderPath(TEST_OAUTH2_PROVIDER))) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            verifyProvider(node, TEST_OAUTH2_PROVIDER, true);
        }
    }

    @Test
    public void iCantGetInvalidProvider() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET, getProviderPath("fake"))) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("Invalid provider: fake", getErrorMessage(node));
        }
    }

    @Test
    public void iCanCreateProvider() throws IOException {
        String serviceName = "myservice";
        String data = "{\n" + //
                "   \"authorizationServerURL\": \"https://test.oauth2.provider/authorization\",\n" + //
                "   \"clientId\": \"clientId\",\n" + //
                "   \"clientSecret\": \"123secret321\",\n" + //
                "   \"description\": \"My Service\",\n" + //
                "   \"entity-type\": \"nuxeoOAuth2ServiceProvider\",\n" + //
                "   \"isEnabled\": true,\n" + //
                "   \"scopes\": [\n" + //
                "      \"https://test.oauth2.provider/scopes/scope0\",\n" + //
                "      \"https://test.oauth2.provider/scopes/scope1\"\n" + //
                "   ],\n" + //
                "   \"serviceName\": \"myservice\",\n" + //
                "   \"tokenServerURL\": \"https://test.oauth2.provider/token\"\n" + //
                "}";
        service = getServiceFor("user1", "user1");
        try (CloseableClientResponse response = getResponse(RequestType.POST, PROVIDER_PATH, data)) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
        service = getServiceFor("Administrator", "Administrator");
        try (CloseableClientResponse response = getResponse(RequestType.POST, PROVIDER_PATH, data)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            verifyProvider(node, serviceName, false);
        }
    }

    @Test
    public void iCanUpdateProvider() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET, getProviderPath(TEST_OAUTH2_PROVIDER_2))) {
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("clientId", node.get("clientId").textValue());
            assertTrue(node.get("clientSecret").isNull());
            assertFalse(node.get("isEnabled").booleanValue());
        }

        String data = "{\n" + //
                "   \"authorizationServerURL\": \"https://test.oauth2.provider/authorization\",\n" + //
                "   \"clientId\": \"myId\",\n" + //
                "   \"clientSecret\": \"123secret321\",\n" + //
                "   \"description\": \"Test OAuth2 Provider 2\",\n" + //
                "   \"entity-type\": \"nuxeoOAuth2ServiceProvider\",\n" + //
                "   \"isEnabled\": true,\n" + //
                "   \"scopes\": [\n" + //
                "      \"https://test.oauth2.provider/scopes/scope0\",\n" + //
                "      \"https://test.oauth2.provider/scopes/scope1\"\n" + //
                "   ],\n" + //
                "   \"serviceName\": \"test-oauth2-provider-2\",\n" + //
                "   \"tokenServerURL\": \"https://test.oauth2.provider/token\"\n" + //
                "}";

        service = getServiceFor("user1", "user1");
        try (CloseableClientResponse response = getResponse(RequestType.PUT, getProviderPath(TEST_OAUTH2_PROVIDER_2),
                data)) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
        service = getServiceFor("Administrator", "Administrator");
        try (CloseableClientResponse response = getResponse(RequestType.PUT, getProviderPath(TEST_OAUTH2_PROVIDER_2),
                data)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("myId", node.get("clientId").textValue());
            assertEquals("123secret321", node.get("clientSecret").textValue());
            assertTrue(node.get("isEnabled").booleanValue());
        }
    }

    @Test
    public void iCantUpdateInvalidProvider() {
        String data = "{\n" + //
                "   \"authorizationServerURL\": \"https://test.oauth2.provider/authorization\",\n" + //
                "   \"clientId\": \"myId\",\n" + //
                "   \"clientSecret\": \"123secret321\",\n" + //
                "   \"description\": \"Test OAuth2 Provider 2\",\n" + //
                "   \"entity-type\": \"nuxeoOAuth2ServiceProvider\",\n" + //
                "   \"isEnabled\": true,\n" + //
                "   \"scopes\": [\n" + //
                "      \"https://test.oauth2.provider/scopes/scope0\",\n" + //
                "      \"https://test.oauth2.provider/scopes/scope1\"\n" + //
                "   ],\n" + //
                "   \"serviceName\": \"test-oauth2-provider-2\",\n" + //
                "   \"tokenServerURL\": \"https://test.oauth2.provider/token\"\n" + //
                "}";
        try (CloseableClientResponse response = getResponse(RequestType.PUT, getProviderPath("fake"), data)) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void iCanDeleteProvider() {
        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                getProviderPath(TEST_OAUTH2_PROVIDER_2))) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        service = getServiceFor("user1", "user1");
        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                getProviderPath(TEST_OAUTH2_PROVIDER))) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void iCantDeleteInvalidProvider() {
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, getProviderPath("fake"))) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    // test oauth2/provider/{provider}/token

    @Test
    public void iCanGetValidProviderToken() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET, getTokenPath(TEST_OAUTH2_PROVIDER))) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(TEST_OAUTH2_ACCESS_TOKEN, node.get("token").textValue());
        }
    }

    @Test
    public void iCantGetTokenInvalidProvider() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET, getTokenPath("fake"))) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("Invalid provider: fake", getErrorMessage(node));
        }
    }

    protected void verifyProvider(JsonNode node, String serviceName, Boolean checkToken) {
        if (node.isArray()) {
            JsonNode child;
            for (int i = 0; i < node.size(); i++) {
                child = node.get(i);
                if (child.get("entity-type").textValue().equals(OAUTH2_PROVIDER_TYPE)
                        && child.get("serviceName").textValue().equals(serviceName)
                        && child.get("clientId").textValue().equals(TEST_OAUTH2_CLIENTID)
                        && (checkToken ? child.get("userId").textValue().equals(TEST_OAUTH2_SERVICE_USERID) : true)
                        && child.get("authorizationURL")
                                .textValue()
                                .equals(AUTHORIZATION_SERVER_URL + "?client_id=" + TEST_OAUTH2_CLIENTID
                                        + "&redirect_uri=" + getBaseURL() + "/site/oauth2/" + serviceName + "/callback"
                                        + "&response_type=code&scope=" + getScopeUrl(0) + "%20" + getScopeUrl(1))) {
                    return;
                }
            }
            fail("No provider found.");
        } else {
            assertEquals(OAUTH2_PROVIDER_TYPE, node.get("entity-type").textValue());
            assertEquals(serviceName, node.get("serviceName").textValue());
            assertEquals(TEST_OAUTH2_CLIENTID, node.get("clientId").textValue());
            assertEquals(AUTHORIZATION_SERVER_URL + "?client_id=" + TEST_OAUTH2_CLIENTID + "&redirect_uri="
                    + getBaseURL() + "/site/oauth2/" + serviceName + "/callback" + "&response_type=code&scope="
                    + getScopeUrl(0) + "%20" + getScopeUrl(1), node.get("authorizationURL").textValue());
            if (checkToken) {
                assertEquals(TEST_OAUTH2_SERVICE_USERID, node.get("userId").textValue());
            }
        }
    }

    // test oauth2/token
    @Test
    public void iCanGetAllTokens() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET, TOKEN_PATH)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(OAUTH2_TOKENS_TYPE, node.get("entity-type").textValue());
            assertNotNull(node.get("entries"));
            assertEquals(5, node.get("entries").size());
            verifyToken(node.get("entries"), TEST_OAUTH2_PROVIDER, null, "Administrator", "2017-05-09 11:11:11");
            verifyToken(node.get("entries"), TEST_OAUTH2_PROVIDER, null, "user1", "2017-05-09 11:11:11");
        }

        service = getServiceFor("user1", "user1");
        try (CloseableClientResponse response = getResponse(RequestType.GET, TOKEN_PATH)) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    // test oauth2/token/provider
    @Test
    public void iCanGetUserProviderTokens() throws IOException {
        service = getServiceFor("user1", "user1");
        try (CloseableClientResponse response = getResponse(RequestType.GET, PROVIDER_TOKEN_PATH)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(OAUTH2_TOKENS_TYPE, node.get("entity-type").textValue());
            assertNotNull(node.get("entries"));
            assertEquals(1, node.get("entries").size());
            verifyToken(node.get("entries"), TEST_OAUTH2_PROVIDER, null, "user1", "2017-05-09 11:11:11");
        }
    }

    // test oauth2/token/provider/{provider}/user/{user}
    @Test
    public void iCanGetProviderToken() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user1")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(OAUTH2_TOKEN_TYPE, node.get("entity-type").textValue());
            verifyToken(node, TEST_OAUTH2_PROVIDER, null, "user1", "2017-05-09 11:11:11");
        }

        // test deprecated oauth2/token/{provider}/{user}
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                TOKEN_PATH + "/" + TEST_OAUTH2_PROVIDER + "/user1")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(OAUTH2_TOKEN_TYPE, node.get("entity-type").textValue());
            verifyToken(node, TEST_OAUTH2_PROVIDER, null, "user1", "2017-05-09 11:11:11");
        }

        // will get a 404 if not token is found for the provider/user pair
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user3")) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }

        // users must be able to fetch their own tokens
        service = getServiceFor("user1", "user1");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user1")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(OAUTH2_TOKEN_TYPE, node.get("entity-type").textValue());
            verifyToken(node, TEST_OAUTH2_PROVIDER, null, "user1", "2017-05-09 11:11:11");
        }

        // but not other users'
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user2")) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void iCanUpdateProviderToken() throws IOException {
        String data = "{\n" + //
                "   \"entity-type\": \"nuxeoOAuth2Token\",\n" + //
                "   \"clientId\": null,\n" + //
                "   \"creationDate\": \"2017-05-10 11:11:11\",\n" + //
                "   \"isShared\": false,\n" + //
                "   \"nuxeoLogin\": \"user1\",\n" + //
                "   \"serviceLogin\": \"my1@mail\",\n" + //
                "   \"serviceName\": \"test-oauth2-provider\",\n" + //
                "   \"sharedWith\": []\n" + //
                "}";

        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user1", data)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(OAUTH2_TOKEN_TYPE, node.get("entity-type").textValue());
            verifyToken(node, TEST_OAUTH2_PROVIDER, null, "user1", "2017-05-10 11:11:11");
        }

        // test deprecated oauth2/token/{provider}/{user}
        data = "{\n" + //
                "   \"entity-type\": \"nuxeoOAuth2Token\",\n" + //
                "   \"clientId\": null,\n" + //
                "   \"creationDate\": \"2017-05-10 11:11:11\",\n" + //
                "   \"isShared\": false,\n" + //
                "   \"nuxeoLogin\": \"user1\",\n" + //
                "   \"serviceLogin\": \"my1@mail\",\n" + //
                "   \"serviceName\": \"test-oauth2-provider\",\n" + //
                "   \"sharedWith\": []\n" + //
                "}";
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                TOKEN_PATH + "/" + TEST_OAUTH2_PROVIDER + "/user1", data)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(OAUTH2_TOKEN_TYPE, node.get("entity-type").textValue());
            verifyToken(node, TEST_OAUTH2_PROVIDER, null, "user1", "2017-05-10 11:11:11");
        }

        // users must be able to update their own tokens
        data = "{\n" + //
                "   \"entity-type\": \"nuxeoOAuth2Token\",\n" + //
                "   \"clientId\": null,\n" + "   \"creationDate\": \"2017-05-11 11:11:11\",\n" + //
                "   \"isShared\": false,\n" + //
                "   \"nuxeoLogin\": \"user1\",\n" + //
                "   \"serviceLogin\": \"my1@mail\",\n" + //
                "   \"serviceName\": \"test-oauth2-provider\",\n" + //
                "   \"sharedWith\": []\n" + //
                "}";
        service = getServiceFor("user1", "user1");
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user1", data)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(OAUTH2_TOKEN_TYPE, node.get("entity-type").textValue());
            verifyToken(node, TEST_OAUTH2_PROVIDER, null, "user1", "2017-05-11 11:11:11");
        }

        // but not other users'
        service = getServiceFor("user2", "user2");
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user1", data)) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void iCanDeleteProviderToken() {
        // a user cannot delete some else's token
        service = getServiceFor("user2", "user2");
        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user1")) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        // but can delete his/her own
        service = getServiceFor("user1", "user1");
        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user1")) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // and admins can delete everybody's
        service = getServiceFor("Administrator", "Administrator");
        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user2")) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                TOKEN_PATH + "/provider/" + TEST_OAUTH2_PROVIDER + "/user/user1")) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }

        // test deprecated oauth2/token/{provider}/{user}
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                TOKEN_PATH + "/" + TEST_OAUTH2_PROVIDER + "/user1")) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    // test oauth2/token/client
    @Test
    public void iCanGetUserClientTokens() throws IOException {
        service = getServiceFor("user1", "user1");
        try (CloseableClientResponse response = getResponse(RequestType.GET, CLIENT_TOKEN_PATH)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(OAUTH2_TOKENS_TYPE, node.get("entity-type").textValue());
            assertNotNull(node.get("entries"));
            assertEquals(1, node.get("entries").size());
            verifyToken(node.get("entries"), TOKEN_STORE, TEST_CLIENT, "user1", "2017-05-20 11:11:11");
        }
    }

    // test oauth2/token/client/{provider}/user/{user}
    @Test
    public void iCanGetClientToken() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user1")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            verifyToken(node, TOKEN_STORE, TEST_CLIENT, "user1", "2017-05-20 11:11:11");
        }

        // will get a 404 if not token is found for the provider/user pair
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                TOKEN_PATH + "/client/unknown-client/user/user1")) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }

        // users must be able to fetch their own tokens
        service = getServiceFor("user1", "user1");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user1")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            verifyToken(node, TOKEN_STORE, TEST_CLIENT, "user1", "2017-05-20 11:11:11");
        }

        // but not other users'
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user2")) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void iCanUpdateClientToken() throws IOException {
        String data = "{\n" + //
                "   \"entity-type\": \"nuxeoOAuth2Token\",\n" + //
                "   \"clientId\": \"my-client\",\n" + //
                "   \"creationDate\": \"2017-05-21 11:11:11\",\n" + //
                "   \"isShared\": false,\n" + //
                "   \"nuxeoLogin\": \"user1\",\n" + //
                "   \"serviceLogin\": \"my1@mail\",\n" + //
                "   \"serviceName\": \"org.nuxeo.server.token.store\",\n" + //
                "   \"sharedWith\": []\n" + //
                "}";
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user1", data)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            verifyToken(node, TOKEN_STORE, TEST_CLIENT, "user1", "2017-05-21 11:11:11");
        }

        // users must be able to update their own tokens
        data = "{\n" + //
                "   \"entity-type\": \"nuxeoOAuth2Token\",\n" + //
                "   \"clientId\": \"my-client\",\n" + //
                "   \"creationDate\": \"2017-05-22 11:11:11\",\n" + //
                "   \"isShared\": false,\n" + //
                "   \"nuxeoLogin\": \"user1\",\n" + //
                "   \"serviceLogin\": \"my1@mail\",\n" + //
                "   \"serviceName\": \"org.nuxeo.server.token.store\",\n" + //
                "   \"sharedWith\": []\n" + //
                "}";
        service = getServiceFor("user1", "user1");
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user1", data)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            verifyToken(node, TOKEN_STORE, TEST_CLIENT, "user1", "2017-05-22 11:11:11");
        }

        // but not other users'
        service = getServiceFor("user2", "user2");
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user1", data)) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void iCanDeleteClientToken() {
        // a user cannot delete some else's token
        service = getServiceFor("user2", "user2");
        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user1")) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
        }

        // but can delete his/her own
        service = getServiceFor("user1", "user1");
        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user1")) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // and admins can delete everybody's
        service = getServiceFor("Administrator", "Administrator");
        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user2")) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                TOKEN_PATH + "/client/" + TEST_CLIENT + "/user/user1")) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    protected void verifyToken(JsonNode node, String serviceName, String clientId, String nxuser, String creationDate) {
        if (node.isArray()) {
            JsonNode child;
            for (int i = 0; i < node.size(); i++) {
                child = node.get(i);
                if (child.get("entity-type").textValue().equals(OAUTH2_TOKEN_TYPE)
                        && child.get("serviceName").textValue().equals(serviceName)
                        && child.get("nuxeoLogin").textValue().equals(nxuser)
                        && child.get("creationDate").textValue().equals(creationDate)
                        && (clientId != null ? child.get("clientId").textValue().equals(clientId) : true)) {
                    return;
                }
            }
            fail("No token found.");
        } else {
            assertEquals(OAUTH2_TOKEN_TYPE, node.get("entity-type").textValue());
            assertEquals(serviceName, node.get("serviceName").textValue());
            assertEquals(nxuser, node.get("nuxeoLogin").textValue());
            assertEquals(creationDate, node.get("creationDate").textValue());
            if (clientId != null) {
                assertEquals(clientId, node.get("clientId").textValue());
            }
        }
    }

    // test oauth2/client
    @Test
    public void iCanGetClients() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET, CLIENT_PATH)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(OAUTH2_CLIENTS_TYPE, node.get("entity-type").textValue());
            assertNotNull(node.get("entries"));
            assertEquals(2, node.get("entries").size());
            verifyClient(node.get("entries"), TEST_CLIENT, TEST_CLIENT_NAME);
            verifyClient(node.get("entries"), TEST_CLIENT_2, TEST_CLIENT_NAME_2);
        }
    }

    @Test
    public void iCanGetClient() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET, getClientPath(TEST_CLIENT))) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            verifyClient(node, TEST_CLIENT, TEST_CLIENT_NAME);
        }
    }

    @Test
    public void iCantGetInvalidClient() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET, getClientPath("fake"))) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("Invalid client: fake", getErrorMessage(node));
        }
    }

    protected void verifyClient(JsonNode node, String clientId, String name) {
        if (node.isArray()) {
            JsonNode child;
            for (int i = 0; i < node.size(); i++) {
                child = node.get(i);
                if (child.get("entity-type").textValue().equals(OAUTH2_CLIENT_TYPE)
                        && child.get("id").textValue().equals(clientId) && child.get("name").textValue().equals(name)) {
                    return;
                }
            }
            fail("No client found.");
        } else {
            assertEquals(OAUTH2_CLIENT_TYPE, node.get("entity-type").textValue());
            assertEquals(clientId, node.get("id").textValue());
            assertEquals(name, node.get("name").textValue());
        }
    }

}
