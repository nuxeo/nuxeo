/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.automation.rest.io.NuxeoGroupWriter;
import org.nuxeo.ecm.automation.rest.io.NuxeoPrincipalWriter;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;

/**
 *
 *
 * @since 5.7.3
 */
public class BaseUserTest extends BaseTest {

    /**
     * Returns the json representation of a group
     * @param group
     * @return
     * @throws IOException
     *
     */
    protected String getGroupAsJson(NuxeoGroup group) throws IOException {
        NuxeoGroupWriter npw = new NuxeoGroupWriter();
        OutputStream out = new ByteArrayOutputStream();
        npw.writeTo(group, NuxeoGroup.class, null, null, null, null, out);
        return out.toString();
    }

    /**
     * Assert that the given node represents a group which properties are given
     * in parameters
     *
     * @param groupName
     * @param groupLabel
     * @param node
     *
     */
    protected void assertEqualsGroup(String groupName, String groupLabel,
            JsonNode node) {
        assertEquals("group", node.get("entity-type").getValueAsText());
        assertEquals(groupName, node.get("groupname").getValueAsText());
        assertEquals(groupLabel, node.get("label").getValueAsText());
    }

    /**
     * Returns the Json representation of a user.
     *
     * @param user
     * @return
     * @throws IOException
     *
     */
    protected String getPrincipalAsJson(NuxeoPrincipal user) throws IOException {
        NuxeoPrincipalWriter npw = new NuxeoPrincipalWriter();
        OutputStream out = new ByteArrayOutputStream();
        npw.writeTo(user, NuxeoPrincipal.class, null, null, null, null, out);
        String userJson = out.toString();
        return userJson;
    }

    /**
     * Assert that the given node represents a user which properties are given
     * in parameters.
     *
     * @param username
     * @param firstname
     * @param lastname
     * @param node
     *
     */
    protected void assertEqualsUser(String username, String firstname,
            String lastname, JsonNode node) {
        assertEquals("user", node.get("entity-type").getValueAsText());
        assertEquals(username, node.get("id").getValueAsText());
        assertEquals(firstname,
                node.get("properties").get("firstName").getValueAsText());
        assertEquals(lastname,
                node.get("properties").get("lastName").getValueAsText());
    }

}
