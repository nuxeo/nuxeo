/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 5.7.3
 */
public class BaseUserTest extends BaseTest {

    /**
     * Returns the json representation of a group
     */
    protected String getGroupAsJson(NuxeoGroup group) throws IOException {
        RenderingContext ctx = CtxBuilder.get();
        ctx.addParameterValues("fetch.group", "memberUsers");
        ctx.addParameterValues("fetch.group", "memberGroups");
        ctx.addParameterValues("fetch.group", "parentGroups");
        return MarshallerHelper.objectToJson(group, ctx);
    }

    /**
     * Assert that the given node represents a group which properties are given in parameters
     */
    protected void assertEqualsGroup(String groupName, String groupLabel, JsonNode node) {
        assertEquals("group", node.get("entity-type").asText());
        assertEquals(groupName, node.get("id").asText());
        assertEquals(groupName, node.get("groupname").asText());
        assertEquals(groupLabel, node.get("grouplabel").asText());
        JsonNode properties = node.get("properties");
        assertEquals(groupName, properties.get("groupname").asText());
        assertEquals(groupLabel, properties.get("grouplabel").asText());
        assertEquals("description of " + groupName, properties.get("description").asText());
    }

    /**
     * Returns the Json representation of a user.
     */
    protected String getPrincipalAsJson(NuxeoPrincipal user) throws IOException {
        return MarshallerHelper.objectToJson(user, CtxBuilder.get());
    }

    /**
     * Assert that the given node represents a user which properties are given in parameters.
     */
    protected void assertEqualsUser(String username, String firstname, String lastname, JsonNode node) {
        assertEqualsUser(username, firstname, lastname, null, node);
    }

    protected void assertEqualsUser(String username, String firstname, String lastname, String email, JsonNode node) {
        assertEquals("user", node.get("entity-type").asText());
        assertEquals(username, node.get("id").asText());
        JsonNode properties = node.get("properties");
        assertEquals(firstname, properties.get("firstName").asText());
        assertEquals(lastname, properties.get("lastName").asText());
        if (email != null) {
            assertEquals(email, properties.get("email").asText());
        }
    }

}
