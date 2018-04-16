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
 *     Nelson Silva <nsilva@nuxeo.com>
 */

package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.tokenauth.service.TokenAuthenticationService;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@ServletContainer(port = 18090)
@Deploy("org.nuxeo.ecm.platform.login.token")
@Deploy("org.nuxeo.ecm.platform.restapi.server.login.tokenauth")
public class AuthenticationTokensTest extends BaseTest {

    @Inject
    TokenAuthenticationService tokenAuthenticationService;

    @Inject
    protected CoreFeature coreFeature;

    @Test
    public void itCanQueryTokens() throws Exception {
        // Check empty token list
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/token")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(0, getEntries("tokens", node).size());
        }

        // acquire some tokens
        String token1 = tokenAuthenticationService.acquireToken("Administrator", "app1", "device1", "", "rw");
        coreFeature.getStorageConfiguration().maybeSleepToNextSecond();
        String token2 = tokenAuthenticationService.acquireToken("Administrator", "app2", "device2", "", "rw");

        nextTransaction();

        // query tokens for current user
        List<JsonNode> tokens = getTokens();
        assertEquals(2, tokens.size());
        assertEquals(token2, tokens.get(0).get("id").textValue());
        assertEquals(token1, tokens.get(1).get("id").textValue());

        // filter tokens by application
        tokens = getTokens("app1");
        assertEquals(1, tokens.size());
        assertEquals(token1, tokens.get(0).get("id").textValue());
    }

    @Test
    public void itCanRevokeTokens() throws Exception {
        // acquire a token
        String token1 = tokenAuthenticationService.acquireToken("Administrator", "app1", "device1", "", "rw");
        nextTransaction();

        // delete it
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "/token/" + token1)) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        // check no tokens
        List<JsonNode> tokens = getTokens();
        assertEquals(0, tokens.size());
    }

    @Test
    public void itCanCreateTokens() throws Exception {
        // acquire a token
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        params.put("application", Collections.singletonList("app"));
        params.put("deviceId", Collections.singletonList("device"));
        params.put("permission", Collections.singletonList("rw"));
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/token", null, params, null, null)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            String tokenId = response.getEntity(String.class);
            assertFalse(tokenId.isEmpty());
        }

        // check tokens for current user
        List<JsonNode> tokens = getTokens();
        assertEquals(1, tokens.size());
        JsonNode token = tokens.get(0);
        assertEquals("app", token.get("application").textValue());
        assertEquals("device", token.get("deviceId").textValue());
        assertEquals("rw", token.get("permission").textValue());
        assertFalse(token.get("creationDate").textValue().isEmpty());
        assertFalse(token.get("username").textValue().isEmpty());
    }

    private List<JsonNode> getTokens() throws IOException {
        return getTokens(null);
    }

    private List<JsonNode> getTokens(String application) throws IOException {
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();
        if (application != null) {
            params.put("application", Collections.singletonList(application));
        }
        JsonNode node = getResponseAsJson(RequestType.GET, "/token", params);
        return getEntries("tokens", node);
    }

    private List<JsonNode> getEntries(String entityType, JsonNode node) {
        assertEquals(entityType, node.get("entity-type").asText());
        assertTrue(node.get("entries").isArray());
        List<JsonNode> result = new ArrayList<>();
        Iterator<JsonNode> elements = node.get("entries").elements();
        while (elements.hasNext()) {
            result.add(elements.next());
        }
        return result;
    }

    private void nextTransaction() {
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
    }
}
