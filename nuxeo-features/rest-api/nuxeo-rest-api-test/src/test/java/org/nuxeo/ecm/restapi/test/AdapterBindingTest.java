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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.test.adapter.BusinessBeanAdapter;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.BOAdapter;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class AdapterBindingTest extends BaseTest {

    @Test
    public void iCanGetAnAdapter() throws Exception {

        // Given a note
        DocumentModel note = RestServerInit.getNote(1, session);

        // When i browse the adapter
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "/id/" + note.getId() + "/@" + BOAdapter.NAME + "/BusinessBeanAdapter")) {

            // Then i receive a formatted response
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals("BusinessBeanAdapter", node.get("entity-type").asText());
            assertEquals(note.getPropertyValue("note:note"), node.get("value").get("note").asText());
        }
    }

    @Test
    public void iCanSaveAnAdapter() throws Exception {
        // Given a note and a modified business object representation
        DocumentModel note = RestServerInit.getNote(1, session);
        String ba = String.format(
                "{\"entity-type\":\"BusinessBeanAdapter\",\"value\":{\"type\"" + ":\"Note\",\"id\":\"%s\","
                        + "\"note\":\"Note 1\",\"title\":\"Note 1\",\"description\":\"description\"}}",
                note.getId());
        assertTrue(StringUtils.isBlank((String) note.getPropertyValue("dc:description")));

        // When i do a put request on it
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "/id/" + note.getId() + "/@" + BOAdapter.NAME + "/BusinessBeanAdapter", ba)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // Then it modifies the description
            fetchInvalidations();
            note = session.getDocument(note.getRef());
            assertEquals("description", note.getAdapter(BusinessBeanAdapter.class).getDescription());
        }
    }

    @Test
    public void iCanCreateAnAdapter() throws Exception {
        // Given a note and a modified business object representation
        DocumentModel folder = RestServerInit.getFolder(0, session);
        String ba = String.format("{\"entity-type\":\"BusinessBeanAdapter\",\"value\":{\"type\"" + ":\"Note\","
                + "\"note\":\"Note 1\",\"title\":\"Note 1\",\"description\":\"description\"}}");
        assertTrue(session.getChildren(folder.getRef()).isEmpty());

        // When i do a put request on it
        try (CloseableClientResponse response = getResponse(RequestType.POST,
                "/id/" + folder.getId() + "/@" + BOAdapter.NAME + "/BusinessBeanAdapter/note2", ba)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // Then it modifies the description
            fetchInvalidations();
            assertFalse(session.getChildren(folder.getRef()).isEmpty());
        }
    }

    @Test
    public void iCanGetAdapterOnDocumentLists() throws Exception {
        // Given a folder
        DocumentModel folder = RestServerInit.getFolder(1, session);

        // When i adapt the children of the folder with a BusinessBeanAdapter
        JsonNode node = getResponseAsJson(RequestType.GET,
                "/id/" + folder.getId() + "/@children/@" + BOAdapter.NAME + "/BusinessBeanAdapter");

        // Then i receive a list of businessBeanAdapter
        assertEquals("adapters", node.get("entity-type").asText());
        ArrayNode entries = (ArrayNode) node.get("entries");
        DocumentModelList children = session.getChildren(folder.getRef());
        assertEquals(children.size(), entries.size());

        JsonNode jsonNote = entries.get(0);
        assertEquals("BusinessBeanAdapter", jsonNote.get("entity-type").asText());
        assertEquals("Note", jsonNote.get("value").get("type").asText());
    }

    @Test
    public void adapterListArePaginated() throws Exception {
        // Given a folder
        DocumentModel folder = RestServerInit.getFolder(1, session);

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("currentPageIndex", "1");
        queryParams.putSingle("pageSize", "2");
        queryParams.putSingle("sortBy", "dc:title");
        queryParams.putSingle("sortOrder", "DESC");

        // When i adapt the children of the folder with a BusinessBeanAdapter
        JsonNode node = getResponseAsJson(RequestType.GET,
                "/id/" + folder.getId() + "/@children/@" + BOAdapter.NAME + "/BusinessBeanAdapter", queryParams);

        // Then i receive a list of businessBeanAdapter
        assertEquals("adapters", node.get("entity-type").asText());
        assertTrue(node.get("isPaginable").booleanValue());
        assertEquals(2, ((ArrayNode) node.get("entries")).size());

        JsonNode node1 = ((ArrayNode) node.get("entries")).get(0);
        JsonNode node2 = ((ArrayNode) node.get("entries")).get(1);
        String title1 = node1.get("value").get("title").asText();
        String title2 = node2.get("value").get("title").asText();
        assertTrue(title1.compareTo(title2) > 0);

        // same with multiple sorts
        queryParams.putSingle("sortBy", "dc:description,dc:title");
        queryParams.putSingle("sortOrder", "asc,desc");

        node = getResponseAsJson(RequestType.GET,
                "/id/" + folder.getId() + "/@children/@" + BOAdapter.NAME + "/BusinessBeanAdapter", queryParams);

        // Then i receive a list of businessBeanAdapter
        assertEquals("adapters", node.get("entity-type").asText());
        assertTrue(node.get("isPaginable").booleanValue());
        assertEquals(2, ((ArrayNode) node.get("entries")).size());

        node1 = ((ArrayNode) node.get("entries")).get(0);
        node2 = ((ArrayNode) node.get("entries")).get(1);
        title1 = node1.get("value").get("title").asText();
        title2 = node2.get("value").get("title").asText();
        assertTrue(title1.compareTo(title2) > 0);
    }
}
