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

import javax.inject.Inject;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;

/**
 * @since 5.7.3
 */
public class BaseUserTest extends BaseTest {

    @Inject
    JsonFactory factory;

    /**
     * Returns the json representation of a group
     *
     * @param group
     * @return
     * @throws IOException
     */
    protected String getGroupAsJson(NuxeoGroup group) throws IOException {
        RenderingContext ctx = CtxBuilder.get();
        ctx.addParameterValues("fetch.group", "memberUsers");
        ctx.addParameterValues("fetch.group", "memberGroups");
        return MarshallerHelper.objectToJson(group, ctx);
    }

    /**
     * Assert that the given node represents a group which properties are given in parameters
     *
     * @param groupName
     * @param groupLabel
     * @param node
     */
    protected void assertEqualsGroup(String groupName, String groupLabel, JsonNode node) {
        assertEquals("group", node.get("entity-type").getValueAsText());
        assertEquals(groupName, node.get("groupname").getValueAsText());
        assertEquals(groupLabel, node.get("grouplabel").getValueAsText());
    }

    /**
     * Returns the Json representation of a user.
     *
     * @param user
     * @return
     * @throws IOException
     */
    protected String getPrincipalAsJson(NuxeoPrincipal user) throws IOException {
        return MarshallerHelper.objectToJson(user, CtxBuilder.get());
    }

    /**
     * Assert that the given node represents a user which properties are given in parameters.
     *
     * @param username
     * @param firstname
     * @param lastname
     * @param node
     */
    protected void assertEqualsUser(String username, String firstname, String lastname, JsonNode node) {
        assertEquals("user", node.get("entity-type").getValueAsText());
        assertEquals(username, node.get("id").getValueAsText());
        assertEquals(firstname, node.get("properties").get("firstName").getValueAsText());
        assertEquals(lastname, node.get("properties").get("lastName").getValueAsText());
    }

}
