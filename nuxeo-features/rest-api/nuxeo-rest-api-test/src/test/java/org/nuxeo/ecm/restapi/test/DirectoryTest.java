/*
 * (C) Copyright 2013-2017 Nuxeo (http://nuxeo.com/) and others.
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

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.io.registry.MarshallerHelper;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext.CtxBuilder;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.io.DirectoryEntryJsonWriter;
import org.nuxeo.ecm.directory.io.DirectoryEntryListJsonWriter;
import org.nuxeo.ecm.directory.io.DirectoryListJsonWriter;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, DirectoryFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(init = RestServerInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.restapi.test:test-directory-contrib.xml")
public class DirectoryTest extends BaseTest {

    @Inject
    DirectoryService ds;

    @Inject
    TransactionalFeature txFeature;

    private static final String TESTDIRNAME = "testdir";

    private static final String INT_ID_TEST_DIR_NAME = "intIdTestDir";

    Session dirSession = null;

    @Override
    @Before
    public void doBefore() throws Exception {
        super.doBefore();
        dirSession = ds.open(TESTDIRNAME);
    }

    @Override
    @After
    public void doAfter() throws Exception {
        if (dirSession != null) {
            dirSession.close();
        }
    }

    protected void nextTransaction() {
        dirSession.close();
        txFeature.nextTransaction();
        dirSession = ds.open(TESTDIRNAME);
    }

    @Test
    public void itCanQueryDirectoryEntry() throws Exception {
        // Given a directoryEntry
        DocumentModel docEntry = dirSession.getEntry("test1");
        // When I call the Rest endpoint
        JsonNode node = getResponseAsJson(RequestType.GET, "/directory/" + TESTDIRNAME + "/test1");

        assertEquals(DirectoryEntryJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(TESTDIRNAME, node.get("directoryName").asText());
        assertEquals(docEntry.getPropertyValue("vocabulary:label"),
                node.get("properties").get("label").asText());

    }

    /**
     * @since 8.4
     */
    @Test
    public void itCanQueryDirectoryNames() throws Exception {
        // When I call the Rest endpoint
        JsonNode node = getResponseAsJson(RequestType.GET, "/directory");

        // It should not return system directories
        assertEquals(DirectoryListJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(3, node.get("entries").size());
        assertEquals("continent", node.get("entries").get(0).get("name").textValue());
        assertEquals("country", node.get("entries").get(1).get("name").textValue());
        assertEquals("nature", node.get("entries").get(2).get("name").textValue());

        // It should not retrieve directory with unknown type
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.put("types", Collections.singletonList("notExistingType"));
        node = getResponseAsJson(RequestType.GET, "/directory", queryParams);
        assertEquals(DirectoryListJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(0, node.get("entries").size());

        // It should not retrieve system directories
        queryParams = new MultivaluedMapImpl();
        queryParams.put("types", Collections.singletonList(DirectoryService.SYSTEM_DIRECTORY_TYPE));
        node = getResponseAsJson(RequestType.GET, "/directory", queryParams);
        assertEquals(DirectoryListJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(0, node.get("entries").size());

        // It should be able to retrieve a single type
        queryParams = new MultivaluedMapImpl();
        queryParams.put("types", Collections.singletonList("toto"));
        node = getResponseAsJson(RequestType.GET, "/directory", queryParams);
        assertEquals(DirectoryListJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(1, node.get("entries").size());

        // It should be able to retrieve many types
        queryParams = new MultivaluedMapImpl();
        queryParams.put("types", Arrays.asList("toto", "pouet"));
        node = getResponseAsJson(RequestType.GET, "/directory", queryParams);
        assertEquals(DirectoryListJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(2, node.get("entries").size());
    }

    /**
     * @since 8.4
     */
    @Test
    public void itCannotDeleteDirectoryEntryWithConstraints() throws Exception {
        // When I try to delete an entry which has contraints
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "/directory/continent/europe")) {
            // It should fail
            assertEquals(SC_BAD_REQUEST, response.getStatus());
        }

        // When I remove all the contraints
        JsonNode node = getResponseAsJson(RequestType.GET, "/directory/country");
        ArrayNode jsonEntries = (ArrayNode) node.get("entries");
        Iterator<JsonNode> it = jsonEntries.elements();
        while (it.hasNext()) {
            JsonNode props = it.next().get("properties");
            if ("europe".equals(props.get("parent").textValue())) {
                try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                        "/directory/country/" + props.get("id").textValue())) {
                    assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
                }
            }
        }
        // I should be able to delete the entry
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "/directory/continent/europe")) {
            assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void itCanQueryDirectoryEntries() throws Exception {
        // Given a directory
        DocumentModelList entries = dirSession.query(Collections.emptyMap());

        // When i do a request on the directory endpoint
        JsonNode node = getResponseAsJson(RequestType.GET, "/directory/" + TESTDIRNAME);

        // Then i receive the response as json
        assertEquals(DirectoryEntryListJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
        ArrayNode jsonEntries = (ArrayNode) node.get("entries");
        assertEquals(entries.size(), jsonEntries.size());
    }

    @Test
    public void itCanUpdateADirectoryEntry() throws Exception {
        // Given a directory modified entry as Json
        DocumentModel docEntry = dirSession.getEntry("test1");
        docEntry.setPropertyValue("vocabulary:label", "newlabel");
        String jsonEntry = getDirectoryEntryAsJson(docEntry);

        // When i do an update request on the entry endpoint
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "/directory/" + TESTDIRNAME + "/" + docEntry.getId(), jsonEntry)) {
            // Then the entry is updated
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        nextTransaction(); // see committed changes
        docEntry = dirSession.getEntry("test1");
        assertEquals("newlabel", docEntry.getPropertyValue("vocabulary:label"));

        // update an entry without the `id` field at the root
        String compatJSONEntry = "{\"entity-type\":\"directoryEntry\",\"directoryName\":\"testdir\",\"properties\":{\"id\":\"test1\",\"label\":\"another label\"}}";
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "/directory/" + TESTDIRNAME + "/" + docEntry.getId(), compatJSONEntry)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        }

        nextTransaction(); // see committed changes
        docEntry = dirSession.getEntry(docEntry.getId());
        assertEquals("another label", docEntry.getPropertyValue("vocabulary:label"));

        // The document should not be updated if the id is missing at the root and in the properties
        String missingIdEntry = "{\"entity-type\":\"directoryEntry\",\"directoryName\":\"testdir\",\"properties\":{\"label\":\"different label\"}}";
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "/directory/" + TESTDIRNAME + "/" + docEntry.getId(), missingIdEntry)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        }

        nextTransaction(); // see committed changes
        docEntry = dirSession.getEntry(docEntry.getId());
        assertEquals("another label", docEntry.getPropertyValue("vocabulary:label"));
    }

    @Test
    public void itCanUpdateADirectoryEntryWithAnIntId() throws IOException {
        try (Session dirSession = ds.open(INT_ID_TEST_DIR_NAME)) {
            DocumentModel docEntry = dirSession.createEntry(Collections.singletonMap("label", "test label"));
            nextTransaction(); // see committed changes

            docEntry.setPropertyValue("intIdSchema:label", "new label");
            String jsonEntry = getDirectoryEntryAsJson(INT_ID_TEST_DIR_NAME, docEntry);
            try (CloseableClientResponse response = getResponse(RequestType.PUT,
                    "/directory/" + INT_ID_TEST_DIR_NAME + "/" + docEntry.getId(), jsonEntry)) {
                assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            }

            nextTransaction(); // see committed changes
            docEntry = dirSession.getEntry(docEntry.getId());
            assertEquals("new label", docEntry.getPropertyValue("intIdSchema:label"));
        }
    }

    @Test
    public void itCanCreateADirectoryEntry() throws Exception {
        // Given a directory modified entry as Json
        DocumentModel docEntry = dirSession.getEntry("test1");
        docEntry.setPropertyValue("vocabulary:id", "newtest");
        docEntry.setPropertyValue("vocabulary:label", "newlabel");
        assertNull(dirSession.getEntry("newtest"));
        String jsonEntry = getDirectoryEntryAsJson(docEntry);

        // When i do an update request on the entry endpoint
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/directory/" + TESTDIRNAME, jsonEntry)) {
            // Then the entry is updated
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }

        nextTransaction(); // see committed changes
        docEntry = dirSession.getEntry("newtest");
        assertEquals("newlabel", docEntry.getPropertyValue("vocabulary:label"));

    }

    @Test
    public void itCanDeleteADirectoryEntry() throws Exception {
        // Given an existent entry
        DocumentModel docEntry = dirSession.getEntry("test2");
        assertNotNull(docEntry);

        // When i do a DELETE request on the entry endpoint
        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                "/directory/" + TESTDIRNAME + "/" + docEntry.getId())) {
            // Then the entry is deleted
            nextTransaction(); // see committed changes
            assertNull(dirSession.getEntry("test2"));
        }
    }

    @Test
    public void itSends404OnnotExistentDirectory() throws Exception {
        try (CloseableClientResponse response = getResponse(RequestType.GET, "/directory/nonexistendirectory")) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void itSends404OnnotExistentDirectoryEntry() throws Exception {
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "/directory/" + TESTDIRNAME + "/nonexistententry")) {
            assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        }
    }

    @Test
    public void genericUserCanNotEditDirectories() throws Exception {
        // As a generic user
        service = getServiceFor("user1", "user1");

        // Given a directory entry as Json
        DocumentModel docEntry = dirSession.getEntry("test1");
        String jsonEntry = getDirectoryEntryAsJson(docEntry);

        // When i do an update request on the entry endpoint
        try (CloseableClientResponse response = getResponse(RequestType.PUT,
                "/directory/" + TESTDIRNAME + "/" + docEntry.getId(), jsonEntry)) {
            // Then it is forbidden
            assertEquals(SC_FORBIDDEN, response.getStatus());
        }

        // When i do an create request on the entry endpoint
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/directory/" + TESTDIRNAME, jsonEntry)) {
            // Then it is forbidden
            assertEquals(SC_FORBIDDEN, response.getStatus());
        }

        // When i do an delete request on the entry endpoint
        try (CloseableClientResponse response = getResponse(RequestType.DELETE,
                "/directory/" + TESTDIRNAME + "/" + docEntry.getId())) {
            // Then it is forbidden
            assertEquals(SC_FORBIDDEN, response.getStatus());
        }
    }

    @Test
    public void userDirectoryAreNotEditable() throws Exception {

        // Given a user directory entry
        UserManager um = Framework.getService(UserManager.class);
        DocumentModel model = um.getUserModel("user1");
        String userDirectoryName = um.getUserDirectoryName();
        String jsonEntry = getDirectoryEntryAsJson(userDirectoryName, model);

        // When i do an update request on it
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/directory/" + userDirectoryName,
                jsonEntry)) {
            // Then it is unauthorized
            assertEquals(SC_BAD_REQUEST, response.getStatus());
        }
    }

    @Test
    public void itShouldNotWritePasswordFieldInResponse() throws Exception {
        // Given a user directory entry
        UserManager um = Framework.getService(UserManager.class);
        String userDirectoryName = um.getUserDirectoryName();

        // When i do an update request on it
        JsonNode node = getResponseAsJson(RequestType.GET, "/directory/" + userDirectoryName + "/user1");

        assertEquals("", node.get("properties").get("password").asText());
    }

    @Test
    public void groupDirectoryAreNotEditable() throws Exception {

        // Given a user directory entry
        UserManager um = Framework.getService(UserManager.class);
        DocumentModel model = um.getGroupModel("group1");
        String groupDirectoryName = um.getGroupDirectoryName();
        String jsonEntry = getDirectoryEntryAsJson(groupDirectoryName, model);

        // When i do an create request on it
        try (CloseableClientResponse response = getResponse(RequestType.POST, "/directory/" + groupDirectoryName,
                jsonEntry)) {
            // Then it is unauthorized
            assertEquals(SC_BAD_REQUEST, response.getStatus());
        }
    }

    @Test
    public void itCanQueryDirectoryEntryWithIdContainingSlashes() throws Exception {
        DocumentModel docEntry = dirSession.getEntry("id/with/slash");
        JsonNode node = getResponseAsJson(RequestType.GET, "/directory/" + TESTDIRNAME + "/id/with/slash");

        assertEquals(DirectoryEntryJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
        assertEquals(TESTDIRNAME, node.get("directoryName").asText());
        assertEquals(docEntry.getPropertyValue("vocabulary:label"),
                node.get("properties").get("label").asText());
    }

    @Test
    public void itReturnsProperPagination() throws Exception {
        DocumentModel docEntry = dirSession.getEntry("foo");
        String jsonEntry = getDirectoryEntryAsJson(docEntry);
        assertNotNull(jsonEntry);

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        int maxResults = 5;
        queryParams.putSingle("pageSize", String.valueOf(maxResults));
        queryParams.putSingle("maxResults", String.valueOf(maxResults));

        try (CloseableClientResponse response = getResponse(RequestType.GET, "/directory/" + TESTDIRNAME,
                queryParams)) {
            String json = response.getEntity(String.class);
            JsonNode jsonNode = mapper.readTree(json);
            JsonNode entriesNode = jsonNode.get("entries");

            assertTrue(entriesNode.isArray());
            ArrayNode entriesArrayNode = (ArrayNode) entriesNode;
            assertEquals(maxResults, entriesArrayNode.size());
        }
    }

    private String getDirectoryEntryAsJson(DocumentModel dirEntry) throws IOException {
        return getDirectoryEntryAsJson(TESTDIRNAME, dirEntry);
    }

    private String getDirectoryEntryAsJson(String dirName, DocumentModel dirEntry) throws IOException {
        return MarshallerHelper.objectToJson(new DirectoryEntry(dirName, dirEntry), CtxBuilder.get());
    }

}
