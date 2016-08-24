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
import static org.junit.Assert.assertTrue;

/**
 * @since 8.4
 */
@RunWith(FeaturesRunner.class)
@Features({RestServerFeature.class, CoreFeature.class})
@Jetty(port = 18090)
@Deploy({ "org.nuxeo.ecm.platform.oauth",
          "org.nuxeo.ecm.directory.api",
          "org.nuxeo.ecm.directory",
          "org.nuxeo.ecm.directory.types.contrib" })
@LocalDeploy({"org.nuxeo.ecm.platform.restapi.test:test-oauth2provider-config.xml",
              "org.nuxeo.ecm.platform.restapi.test:test-oauth2-directory-contrib.xml"})
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class OAuth2ObjectTest extends BaseTest {

    public static final String TEST_OAUTH2_PROVIDER = "test-oauth2-provider";

    public static final String TEST_OAUTH2_PROVIDER_2 = "test-oauth2-provider-2";

    public static final String TEST_OAUTH2_CLIENTID = "clientId";

    public static final String TEST_OAUTH2_USER = "Administrator";

    public static final String TEST_OAUTH2_SERVICE_USERID = TEST_OAUTH2_USER + "@email.com";

    public static final String TEST_OAUTH2_ACCESS_TOKEN = "y38Hs3_sdas98l";

    protected static final String PROVIDER_PATH = "oauth2/provider";

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

    @Test
    public void iCanGetAuthData() throws IOException {
        ClientResponse response = getResponse(RequestType.GET, getProviderPath(TEST_OAUTH2_PROVIDER));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(TEST_OAUTH2_PROVIDER, node.get("serviceName").getTextValue());
        assertEquals(TEST_OAUTH2_CLIENTID, node.get("clientId").getTextValue());
        assertEquals(AUTHORIZATION_SERVER_URL + "?client_id=" + TEST_OAUTH2_CLIENTID +
            "&redirect_uri=http://localhost:18090/site/oauth2/" + TEST_OAUTH2_PROVIDER + "/callback" +
            "&response_type=code&scope=" + getScopeUrl(0) + "%20" + getScopeUrl(1),
            node.get("authorizationURL").getTextValue());
        assertEquals(TEST_OAUTH2_SERVICE_USERID, node.get("userId").getTextValue());
    }

    @Test
    public void iCantGetAuthDataInvalidProvider() throws IOException {
        ClientResponse response = getResponse(RequestType.GET, getProviderPath("fake"));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("Invalid provider: fake", getErrorMessage(node));
    }

    @Test
    public void iCanGetToken() throws IOException {
        ClientResponse response = getResponse(RequestType.GET, getTokenPath(TEST_OAUTH2_PROVIDER));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(TEST_OAUTH2_ACCESS_TOKEN, node.get("token").getTextValue());
    }

    @Test
    public void iCantGetToken() throws IOException {
        ClientResponse response = getResponse(RequestType.GET, getTokenPath(TEST_OAUTH2_PROVIDER_2));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void iCantGetTokenInvalidProvider() throws IOException {
        ClientResponse response = getResponse(RequestType.GET, getTokenPath("fake"));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("Invalid provider: fake", getErrorMessage(node));
    }

}
