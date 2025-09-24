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
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
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
import org.nuxeo.ecm.directory.api.DirectoryConstants;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.io.DirectoryEntryJsonWriter;
import org.nuxeo.ecm.directory.io.DirectoryEntryListJsonWriter;
import org.nuxeo.ecm.directory.io.DirectoryListJsonWriter;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.http.test.handler.HttpStatusCodeHandler;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

/**
 * @since 5.7.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, DirectoryFeature.class })
@RepositoryConfig(init = RestServerInit.class, cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.restapi.test:test-directory-contrib.xml")
public class DirectoryTest {

    private static final String TESTDIRNAME = "testdir";

    private static final String INT_ID_TEST_DIR_NAME = "intIdTestDir";

    @Inject
    protected DirectoryService ds;

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected RestServerFeature restServerFeature;

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.defaultClient(
            () -> restServerFeature.getRestApiUrl());

    protected Session dirSession = null;

    @Before
    public void doBefore() {
        dirSession = ds.open(TESTDIRNAME);
    }

    @After
    public void doAfter() {
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
    public void itCanQueryDirectoryEntry() {
        // Given a directoryEntry
        DocumentModel docEntry = dirSession.getEntry("test1");
        // When I call the Rest endpoint
        httpClient.buildGetRequest("/directory/" + TESTDIRNAME + "/test1")
                  .executeAndConsume(new JsonNodeHandler(), node -> {
                      assertEquals(DirectoryEntryJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
                      assertEquals(TESTDIRNAME, node.get("directoryName").asText());
                      assertEquals(docEntry.getPropertyValue("vocabulary:label"),
                              node.get("properties").get("label").asText());
                  });
    }

    /**
     * @since 8.4
     */
    @Test
    public void itCanQueryDirectoryNames() {
        // When I call the Rest endpoint
        httpClient.buildGetRequest("/directory").executeAndConsume(new JsonNodeHandler(), node -> {
            // It should not return system directories
            assertEquals(DirectoryListJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
            assertEquals(3, node.get("entries").size());
            assertEquals("continent", node.get("entries").get(0).get("name").textValue());
            assertEquals("country", node.get("entries").get(1).get("name").textValue());
            assertEquals("nature", node.get("entries").get(2).get("name").textValue());
        });
        // It should not retrieve directory with unknown type
        httpClient.buildGetRequest("/directory")
                  .addQueryParameter("types", "notExistingType")
                  .executeAndConsume(new JsonNodeHandler(), node -> {
                      assertEquals(DirectoryListJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
                      assertEquals(0, node.get("entries").size());
                  });

        // It should not retrieve system directories
        httpClient.buildGetRequest("/directory")
                  .addQueryParameter("types", DirectoryConstants.SYSTEM_DIRECTORY_TYPE)
                  .executeAndConsume(new JsonNodeHandler(), node -> {
                      assertEquals(DirectoryListJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
                      assertEquals(0, node.get("entries").size());
                  });

        // It should be able to retrieve a single type
        httpClient.buildGetRequest("/directory")
                  .addQueryParameter("types", "toto")
                  .executeAndConsume(new JsonNodeHandler(), node -> {
                      assertEquals(DirectoryListJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
                      assertEquals(1, node.get("entries").size());
                  });

        // It should be able to retrieve many types
        httpClient.buildGetRequest("/directory")
                  .addQueryParameter("types", "toto")
                  .addQueryParameter("types", "pouet")
                  .executeAndConsume(new JsonNodeHandler(), node -> {
                      assertEquals(DirectoryListJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
                      assertEquals(2, node.get("entries").size());
                  });
    }

    /**
     * @since 8.4
     */
    @Test
    public void itCannotDeleteDirectoryEntryWithConstraints() {
        // When I try to delete an entry which has constraints
        httpClient.buildDeleteRequest("/directory/continent/europe")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_CONFLICT, status.intValue()));

        // When I remove all the constraints
        ArrayNode jsonEntries = httpClient.buildGetRequest("/directory/country")
                                          .executeAndThen(new JsonNodeHandler(),
                                                  node -> (ArrayNode) node.get("entries"));
        Iterator<JsonNode> it = jsonEntries.elements();
        while (it.hasNext()) {
            JsonNode props = it.next().get("properties");
            if ("europe".equals(props.get("parent").textValue())) {
                httpClient.buildDeleteRequest("/directory/country/" + props.get("id").textValue())
                          .executeAndConsume(new HttpStatusCodeHandler(),
                                  status -> assertEquals(SC_NO_CONTENT, status.intValue()));
            }
        }
        // I should be able to delete the entry
        httpClient.buildDeleteRequest("/directory/continent/europe")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NO_CONTENT, status.intValue()));
    }

    @Test
    public void itCanQueryDirectoryEntries() {
        // Given a directory
        DocumentModelList entries = dirSession.query(Collections.emptyMap());

        // When i do a request on the directory endpoint
        httpClient.buildGetRequest("/directory/" + TESTDIRNAME).executeAndConsume(new JsonNodeHandler(), node -> {
            // Then i receive the response as json
            assertEquals(DirectoryEntryListJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
            ArrayNode jsonEntries = (ArrayNode) node.get("entries");
            assertEquals(entries.size(), jsonEntries.size());
        });
    }

    @Test
    public void itCanUpdateADirectoryEntry() throws Exception {
        // Given a directory modified entry as Json
        DocumentModel docEntry = dirSession.getEntry("test1");
        docEntry.setPropertyValue("vocabulary:label", "newlabel");
        String jsonEntry = getDirectoryEntryAsJson(docEntry);

        // When i do an update request on the entry endpoint
        httpClient.buildPutRequest("/directory/" + TESTDIRNAME + "/" + docEntry.getId())
                  .entity(jsonEntry)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          // Then the entry is updated
                          status -> assertEquals(SC_OK, status.intValue()));

        nextTransaction(); // see committed changes
        docEntry = dirSession.getEntry("test1");
        assertEquals("newlabel", docEntry.getPropertyValue("vocabulary:label"));

        // update an entry without the `id` field at the root
        String compatJSONEntry = """
                {
                  "entity-type": "directoryEntry",
                  "directoryName": "testdir",
                  "properties": {
                    "id": "test1",
                    "label": "another label"
                  }
                }
                """;
        httpClient.buildPutRequest("/directory/" + TESTDIRNAME + "/" + docEntry.getId())
                  .entity(compatJSONEntry)
                  .executeAndConsume(new HttpStatusCodeHandler(), status -> assertEquals(SC_OK, status.intValue()));

        nextTransaction(); // see committed changes
        docEntry = dirSession.getEntry(docEntry.getId());
        assertEquals("another label", docEntry.getPropertyValue("vocabulary:label"));

        // The document should not be updated if the id is missing at the root and in the properties
        String missingIdEntry = """
                {
                  "entity-type": "directoryEntry",
                  "directoryName": "testdir",
                  "properties": {
                    "label": "different label"
                  }
                }
                """;
        httpClient.buildPutRequest("/directory/" + TESTDIRNAME + "/" + docEntry.getId())
                  .entity(missingIdEntry)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_BAD_REQUEST, status.intValue()));

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
            httpClient.buildPutRequest("/directory/" + INT_ID_TEST_DIR_NAME + "/" + docEntry.getId())
                      .entity(jsonEntry)
                      .executeAndConsume(new HttpStatusCodeHandler(),
                              // Then the entry is updated
                              status -> assertEquals(SC_OK, status.intValue()));

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
        httpClient.buildPostRequest("/directory/" + TESTDIRNAME)
                  .entity(jsonEntry)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          // Then the entry is updated
                          status -> assertEquals(SC_CREATED, status.intValue()));

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
        httpClient.buildDeleteRequest("/directory/" + TESTDIRNAME + "/" + docEntry.getId())
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NO_CONTENT, status.intValue()));
        // Then the entry is deleted
        nextTransaction(); // see committed changes
        assertNull(dirSession.getEntry("test2"));
    }

    @Test
    public void itSends404OnNotExistentDirectory() {
        httpClient.buildGetRequest("/directory/nonexistendirectory")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NOT_FOUND, status.intValue()));
    }

    @Test
    public void itSends404OnNotExistentDirectoryEntry() {
        httpClient.buildGetRequest("/directory/" + TESTDIRNAME + "/nonexistendirectory")
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          status -> assertEquals(SC_NOT_FOUND, status.intValue()));
    }

    @Test
    public void genericUserCanNotEditDirectories() throws Exception {
        // Given a directory entry as Json
        DocumentModel docEntry = dirSession.getEntry("test1");
        String jsonEntry = getDirectoryEntryAsJson(docEntry);

        var statusCodeHandler = new HttpStatusCodeHandler();

        // When i do an update request on the entry endpoint
        httpClient.buildPutRequest("/directory/" + TESTDIRNAME + "/" + docEntry.getId())
                  .credentials("user1", "user1")
                  .entity(jsonEntry)
                  .executeAndConsume(statusCodeHandler,
                          // Then it is forbidden
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));

        // When i do an create request on the entry endpoint
        httpClient.buildPostRequest("/directory/" + TESTDIRNAME)
                  .credentials("user1", "user1")
                  .entity(jsonEntry)
                  .executeAndConsume(statusCodeHandler,
                          // Then it is forbidden
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));

        // When i do an delete request on the entry endpoint
        httpClient.buildDeleteRequest("/directory/" + TESTDIRNAME + "/" + docEntry.getId())
                  .credentials("user1", "user1")
                  .executeAndConsume(statusCodeHandler,
                          // Then it is forbidden
                          status -> assertEquals(SC_FORBIDDEN, status.intValue()));
    }

    @Test
    public void userDirectoryAreNotEditable() throws Exception {

        // Given a user directory entry
        UserManager um = Framework.getService(UserManager.class);
        DocumentModel model = um.getUserModel("user1");
        String userDirectoryName = um.getUserDirectoryName();
        String jsonEntry = getDirectoryEntryAsJson(userDirectoryName, model);

        // When i do an update request on it
        httpClient.buildPostRequest("/directory/" + userDirectoryName)
                  .entity(jsonEntry)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          // Then it is unauthorized
                          status -> assertEquals(SC_BAD_REQUEST, status.intValue()));
    }

    @Test
    public void itShouldNotWritePasswordFieldInResponse() {
        // Given a user directory entry
        UserManager um = Framework.getService(UserManager.class);
        String userDirectoryName = um.getUserDirectoryName();

        // When i do an update request on it
        httpClient.buildGetRequest("/directory/" + userDirectoryName + "/user1")
                  .executeAndConsume(new JsonNodeHandler(),
                          node -> assertEquals("", node.get("properties").get("password").asText()));
    }

    @Test
    public void groupDirectoryAreNotEditable() throws Exception {

        // Given a user directory entry
        UserManager um = Framework.getService(UserManager.class);
        DocumentModel model = um.getGroupModel("group1");
        String groupDirectoryName = um.getGroupDirectoryName();
        String jsonEntry = getDirectoryEntryAsJson(groupDirectoryName, model);

        // When i do an create request on it
        httpClient.buildPostRequest("/directory/" + groupDirectoryName)
                  .entity(jsonEntry)
                  .executeAndConsume(new HttpStatusCodeHandler(),
                          // Then it is unauthorized
                          status -> assertEquals(SC_BAD_REQUEST, status.intValue()));
    }

    @Test
    public void itCanQueryDirectoryEntryWithIdContainingSlashes() {
        DocumentModel docEntry = dirSession.getEntry("id/with/slash");
        httpClient.buildGetRequest("/directory/" + TESTDIRNAME + "/id/with/slash")
                  .executeAndConsume(new JsonNodeHandler(), node -> {
                      assertEquals(DirectoryEntryJsonWriter.ENTITY_TYPE, node.get("entity-type").asText());
                      assertEquals(TESTDIRNAME, node.get("directoryName").asText());
                      assertEquals(docEntry.getPropertyValue("vocabulary:label"),
                              node.get("properties").get("label").asText());
                  });
    }

    @Test
    public void itReturnsProperPagination() throws Exception {
        DocumentModel docEntry = dirSession.getEntry("foo");
        String jsonEntry = getDirectoryEntryAsJson(docEntry);
        assertNotNull(jsonEntry);

        int maxResults = 5;

        var entriesArrayNode = httpClient.buildGetRequest("/directory/" + TESTDIRNAME)
                                         .addQueryParameter("pageSize", String.valueOf(maxResults))
                                         .addQueryParameter("maxResults", String.valueOf(maxResults))
                                         .executeAndThen(new JsonNodeHandler(), jsonNode -> {
                                             JsonNode entriesNode = jsonNode.get("entries");

                                             assertTrue(entriesNode.isArray());
                                             assertEquals(maxResults, entriesNode.size());
                                             return (ArrayNode) entriesNode;
                                         });

        // Check you can retrieve the same directory entries 1 by 1 with offset param.
        for (int offset = 0; offset < entriesArrayNode.size(); offset++) {
            String entryId = entriesArrayNode.get(offset).get("id").asText();

            httpClient.buildGetRequest("/directory/" + TESTDIRNAME)
                      .addQueryParameter("pageSize", "1")
                      .addQueryParameter("offset", String.valueOf(offset))
                      .executeAndConsume(new JsonNodeHandler(), jsonNode -> {
                          JsonNode entriesNode = jsonNode.get("entries");

                          assertTrue(entriesNode.isArray());
                          ArrayNode entriesArrayNodeOffset = (ArrayNode) entriesNode;
                          assertEquals(1, entriesArrayNodeOffset.size());
                          assertEquals(entryId, entriesArrayNodeOffset.get(0).get("id").asText());
                      });
        }
    }

    private String getDirectoryEntryAsJson(DocumentModel dirEntry) throws IOException {
        return getDirectoryEntryAsJson(TESTDIRNAME, dirEntry);
    }

    private String getDirectoryEntryAsJson(String dirName, DocumentModel dirEntry) throws IOException {
        return MarshallerHelper.objectToJson(new DirectoryEntry(dirName, dirEntry), CtxBuilder.get());
    }

}
