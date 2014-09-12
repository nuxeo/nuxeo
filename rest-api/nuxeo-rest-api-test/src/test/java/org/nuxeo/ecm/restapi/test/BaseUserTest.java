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
package org.nuxeo.ecm.restapi.test;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.nuxeo.ecm.automation.jaxrs.io.JsonHelper;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.restapi.jaxrs.io.usermanager.NuxeoGroupWriter;
import org.nuxeo.ecm.restapi.jaxrs.io.usermanager.NuxeoPrincipalWriter;
import org.nuxeo.ecm.restapi.test.BaseTest;

import com.google.inject.Inject;

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
     * @throws ClientException
     *
     */
    protected String getGroupAsJson(NuxeoGroup group) throws IOException, ClientException {
        OutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = JsonHelper.createJsonGenerator(factory, out);
        NuxeoGroupWriter ngw = new NuxeoGroupWriter();
        ngw.writeEntity(jg, group);
        return out.toString();
    }

    /**
     * Assert that the given node represents a group which properties are given
     * in parameters
     *
     * @param groupName
     * @param groupLabel
     * @param node
     */
    protected void assertEqualsGroup(String groupName, String groupLabel,
            JsonNode node) {
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
     * @throws ClientException
     *
     */
    protected String getPrincipalAsJson(NuxeoPrincipal user) throws IOException, ClientException {
        OutputStream out = new ByteArrayOutputStream();
        JsonGenerator jg = JsonHelper.createJsonGenerator(factory, out);
        NuxeoPrincipalWriter npw = new NuxeoPrincipalWriter();
        npw.writeEntity(jg, user);
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
