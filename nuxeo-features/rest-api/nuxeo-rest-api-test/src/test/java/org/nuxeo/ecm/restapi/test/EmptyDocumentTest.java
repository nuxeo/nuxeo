/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.restapi.test;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LogFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, LogFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy("org.nuxeo.ecm.platform.restapi.test.test:test-defaultvalue-docTypes.xml")
@Deploy("org.nuxeo.ecm.platform.restapi.test.test:test-dummy-listener-contrib.xml")
public class EmptyDocumentTest extends BaseTest {

    protected static final Map<String, String> HEADERS = Collections.singletonMap("properties", "*");

    @Test
    public void testEmptyDocumentCreationWithParent() throws IOException {
        DocumentModel folder = RestServerInit.getFolder(0, session);

        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + folder.getId() + "/@emptyWithDefault")) {
            assertError(response);
        }

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("type", "DocDefaultValue");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + folder.getId() + "/@emptyWithDefault", null, queryParams, null, HEADERS)) {
            assertEmptyDocument(folder.getPathAsString() + "/null", response);
        }

        queryParams.putSingle("name", "foo");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + folder.getId() + "/@emptyWithDefault", null, queryParams, null, HEADERS)) {
            assertEmptyDocument(folder.getPathAsString() + "/foo", response);
        }
    }

    protected void assertError(ClientResponse response) throws IOException {
        assertEquals(SC_BAD_REQUEST, response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertNotNull(node);
        assertEquals("exception", node.get("entity-type").textValue());
        assertEquals("Missing type parameter", node.get("message").textValue());
    }

    @Test
    public void testEmptyDocumentCreationWithoutParent() throws IOException {
        try (CloseableClientResponse response = getResponse(RequestType.GET, "@emptyWithDefault")) {
            assertError(response);
        }

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("type", "DocDefaultValue");
        try (CloseableClientResponse response = getResponse(RequestType.GET, "@emptyWithDefault", null, queryParams,
                null, HEADERS)) {
            assertEmptyDocument(null, response);
        }

        queryParams.putSingle("name", "foo");
        try (CloseableClientResponse response = getResponse(RequestType.GET, "@emptyWithDefault", null, queryParams,
                null, HEADERS)) {
            assertEmptyDocument("foo", response);
        }
    }

    protected void assertEmptyDocument(String expectedPath, ClientResponse response) throws IOException {
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertNotNull(node);
        assertEquals("document", node.get("entity-type").textValue());
        assertEquals("DocDefaultValue", node.get("type").textValue());
        assertEquals(expectedPath, node.get("path").textValue());

        JsonNode properties = node.get("properties");
        assertNull(properties.get("dv:simpleWithoutDefault").textValue());
        assertEquals("value", properties.get("dv:simpleWithDefault").textValue());
        JsonNode multiWithDefault = properties.get("dv:multiWithDefault");
        assertEquals(0, properties.get("dv:multiWithoutDefault").size());
        assertEquals("value1", multiWithDefault.get(0).textValue());
        assertEquals("value2", multiWithDefault.get(1).textValue());
        assertEquals("dummy source", properties.get("dc:source").textValue());

        JsonNode complexWithoutDefault = properties.get("dv:complexWithoutDefault");
        assertTrue(complexWithoutDefault.get("foo").isNull());
        assertTrue(complexWithoutDefault.get("bar").isNull());

        JsonNode complexWithDefault = properties.get("dv:complexWithDefault");
        assertTrue(complexWithDefault.get("foo").isNull());
        assertEquals("value", complexWithDefault.get("bar").textValue());
    }

    @Test
    public void testOverrideEmptyDocumentListenerValues() throws IOException {
        String data = "{" //
                + "         \"entity-type\": \"document\"," //
                + "         \"type\": \"DocDefaultValue\"," //
                + "         \"name\": \"foo\"," //
                + "         \"properties\": {" //
                + "             \"dc:source\": null," //
                + "             \"dc:title\": null" //
                + "           }" //
                + "       }";

        DocumentModel folder = RestServerInit.getFolder(0, session);
        try (CloseableClientResponse response = getResponse(RequestType.POST, "path" + folder.getPathAsString(),
                data)) {
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
            JsonNode jsonNode = mapper.readTree(response.getEntityInputStream());

            // dc:source is set by DummyEmptyDocumentListener and overridden to be null
            assertNull(jsonNode.get("properties").get("dc:source").textValue());
            // dc:subjects is set by DummyEmptyDocumentListener and not overridden
            JsonNode subjects = jsonNode.get("properties").get("dc:subjects");
            assertTrue(subjects.isArray());
            assertEquals("dummy subject", subjects.get(0).textValue());
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.restapi.test.test:test-operation-getdocumentparent-contrib.xml")
    public void testEmptyDocumentCreationAndCallGetDocumentParentOperation() throws IOException {
        DocumentModel folder = RestServerInit.getFolder(0, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("type", "File");
        queryParams.putSingle("name", "foo");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + folder.getId() + "/@emptyWithDefault", null, queryParams, null, HEADERS)) {
            assertEquals(SC_OK, response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("/folder_0", node.get("parentRef").textValue());
        }
    }

}
