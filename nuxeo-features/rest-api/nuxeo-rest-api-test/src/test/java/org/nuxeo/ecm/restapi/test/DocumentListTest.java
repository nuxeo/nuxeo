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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.server.jaxrs.QueryObject;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.ChildrenAdapter;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.PageProviderAdapter;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.SearchAdapter;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Joiner;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Test the various ways to query for document lists.
 *
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features(RestServerFeature.class)
@ServletContainer(port = 18090)
@Deploy("org.nuxeo.ecm.platform.restapi.test:pageprovider-test-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class DocumentListTest extends BaseTest {

    @Inject
    protected CoreFeature coreFeature;

    @Test
    public void iCanGetTheChildrenOfADocument() throws Exception {
        // Given a folder
        DocumentModel folder = RestServerInit.getFolder(1, session);

        // When I query for it children
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "id/" + folder.getId() + "/@" + ChildrenAdapter.NAME)) {

            // Then I get its children as JSON
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(session.getChildren(folder.getRef()).size(), getLogEntries(node).size());
        }
    }

    @Test
    public void iCanSearchInFullTextForDocuments() throws Exception {
        // Given a note with "nuxeo" in its description
        DocumentModel note = RestServerInit.getNote(0, session);
        note.setPropertyValue("dc:description", "nuxeo one platform to rule them all");
        session.saveDocument(note);
        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();

        // Waiting for all async events work for indexing content before
        // executing fulltext search
        Framework.getService(EventService.class).waitForAsyncCompletion();
        coreFeature.getStorageConfiguration().sleepForFulltext();

        // When I search for "nuxeo"
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("fullText", "nuxeo");
        try (CloseableClientResponse response = getResponse(RequestType.GET, "path/@" + SearchAdapter.NAME,
                queryParams)) {

            // Then I get the document in the result
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, getLogEntries(node).size());
        }
    }

    @Test
    public void iCanPerformQueriesOnRepository() throws IOException {
        // Given a repository, when I perform a query in NXQL on it
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("query", "SELECT * FROM Document");
        try (CloseableClientResponse response = getResponse(RequestType.GET, "query", queryParams)) {

            // Then I get document listing as result
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(20, getLogEntries(node).size());
        }

        // Given a repository, when I perform a query in NXQL on it
        try (CloseableClientResponse response = getResponse(RequestType.GET, QueryObject.PATH + "/" + QueryObject.NXQL,
                queryParams)) {

            // Then I get document listing as result
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(20, getLogEntries(node).size());
        }

        // Given parameters as page size and ordered parameters
        queryParams.clear();
        queryParams.add("pageSize", "2");
        queryParams.add("queryParams", "$currentUser");
        queryParams.add("query", "select * from Document where dc:creator = ?");

        // Given a repository, when I perform a query in NXQL on it
        try (CloseableClientResponse response = getResponse(RequestType.GET, QueryObject.PATH + "/" + QueryObject.NXQL,
                queryParams)) {

            // Then I get document listing as result
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(2, getLogEntries(node).size());
        }
    }

    @Test
    public void iCanPerformQueriesWithNamedParametersOnRepository() throws IOException {
        // Given a repository and named parameters, when I perform a query in
        // NXQL on it
        DocumentModel folder = RestServerInit.getFolder(1, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("query",
                "SELECT * FROM Document WHERE " + "ecm:parentId = :parentIdVar AND\n"
                        + "        ecm:mixinType != 'HiddenInNavigation' AND dc:title " + "IN (:note1,:note2)\n"
                        + "        AND ecm:isVersion = 0 AND " + "ecm:isTrashed = 0");
        queryParams.add("note1", "Note 1");
        queryParams.add("note2", "Note 2");
        queryParams.add("parentIdVar", folder.getId());
        try (CloseableClientResponse response = getResponse(RequestType.GET, QueryObject.PATH, queryParams)) {

            // Then I get document listing as result
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(2, getLogEntries(node).size());
        }
    }

    @Test
    public void iCanPerformQueriesWithNamedParametersOnRepositoryAndSameVariables() throws IOException {
        // Given a repository and named parameters, when I perform a query in
        // NXQL on it
        DocumentModel folder = RestServerInit.getFolder(1, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("query",
                "SELECT * FROM Document WHERE " + "ecm:parentId = :parentIdVar AND\n"
                        + "        ecm:mixinType != 'HiddenInNavigation' AND dc:title " + "IN (:title,:title2)\n"
                        + "        AND ecm:isVersion = 0 AND " + "ecm:isTrashed = 0");
        queryParams.add("title", "Note 1");
        queryParams.add("title2", "Note 2");
        queryParams.add("parentIdVar", folder.getId());
        try (CloseableClientResponse response = getResponse(RequestType.GET, QueryObject.PATH, queryParams)) {

            // Then I get document listing as result
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(2, getLogEntries(node).size());
        }
    }

    @Test
    public void iCanPerformPageProviderOnRepository() throws IOException {
        // Given a repository, when I perform a pageprovider on it
        DocumentModel folder = RestServerInit.getFolder(1, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("queryParams", folder.getId());
        try (CloseableClientResponse response = getResponse(RequestType.GET, QueryObject.PATH + "/TEST_PP",
                queryParams)) {

            // Then I get document listing as result
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(2, getLogEntries(node).size());
        }
    }

    @Test
    public void iCanPerformPageProviderWithNamedParametersOnRepository() throws IOException {
        // Given a repository, when I perform a pageprovider on it
        DocumentModel folder = RestServerInit.getFolder(1, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("note1", "Note 1");
        queryParams.add("note2", "Note 2");
        queryParams.add("parentIdVar", folder.getId());
        try (CloseableClientResponse response = getResponse(RequestType.GET, QueryObject.PATH + "/TEST_PP_PARAM",
                queryParams)) {

            // Then I get document listing as result
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(2, getLogEntries(node).size());
        }
    }

    /**
     * @since 7.1
     */
    @Test
    public void iCanPerformPageProviderWithNamedParametersInvalid() throws Exception {
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                QueryObject.PATH + "/namedParamProviderInvalid")) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(
                    "Failed to execute query: SELECT * FROM Document where dc:title=:foo ORDER BY dc:title, Lexical Error: Illegal character <:> at offset 38",
                    getErrorMessage(node));
        }
    }

    /**
     * @since 7.1
     */
    @Test
    public void iCanPerformPageProviderWithNamedParametersAndDoc() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("np:title", "Folder 0");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                QueryObject.PATH + "/namedParamProviderWithDoc", queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, getLogEntries(node).size());
        }
    }

    /**
     * @since 7.1
     */
    @Test
    public void iCanPerformPageProviderWithNamedParametersAndDocInvalid() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("np:title", "Folder 0");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                QueryObject.PATH + "/namedParamProviderWithDocInvalid", queryParams)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(
                    "Failed to execute query: SELECT * FROM Document where dc:title=:foo ORDER BY dc:title, Lexical Error: Illegal character <:> at offset 38",
                    getErrorMessage(node));
        }
    }

    /**
     * @since 7.1
     */
    @Test
    public void iCanPerformPageProviderWithNamedParametersInWhereClause() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("parameter1", "Folder 0");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                QueryObject.PATH + "/namedParamProviderWithWhereClause", queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, getLogEntries(node).size());
        }

        // retry without params
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                QueryObject.PATH + "/namedParamProviderWithWhereClause")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(2, getLogEntries(node).size());
        }
    }

    /**
     * @since 7.1
     */
    @Test
    public void iCanPerformPageProviderWithNamedParametersInWhereClauseWithDoc() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("np:title", "Folder 0");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                QueryObject.PATH + "/namedParamProviderWithWhereClauseWithDoc", queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, getLogEntries(node).size());
        }

        // retry without params
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                QueryObject.PATH + "/namedParamProviderWithWhereClauseWithDoc")) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(2, getLogEntries(node).size());
        }
    }

    /**
     * @since 8.4
     */
    @Test
    public void iCanPerformPageProviderWithNamedParametersWithQuickFilter() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("quickFilters", "testQuickFilter");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                QueryObject.PATH + "/namedParamProviderWithQuickFilter", queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, getLogEntries(node).size());
        }
    }

    /**
     * @since 7.1
     */
    @Test
    public void iCanPerformPageProviderWithNamedParametersComplex() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("parameter1", "Folder 0");
        queryParams.add("np:isCheckedIn", Boolean.FALSE.toString());
        queryParams.add("np:dateMin", "2007-01-30 01:02:03+04:00");
        queryParams.add("np:dateMax", "2007-03-23 01:02:03+04:00");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                QueryObject.PATH + "/namedParamProviderComplex", queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, getLogEntries(node).size());
        }

        // remove filter on dates
        queryParams.remove("np:dateMin");
        queryParams.remove("np:dateMax");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                QueryObject.PATH + "/namedParamProviderComplex", queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(1, getLogEntries(node).size());
        }

        queryParams.remove("parameter1");
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                QueryObject.PATH + "/namedParamProviderComplex", queryParams)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(2, getLogEntries(node).size());
        }
    }

    @Test
    public void itCanGetAdapterForRootDocument() throws Exception {
        // Given the root document
        DocumentModel rootDocument = session.getRootDocument();

        // When i ask for an adapter
        JsonNode node = getResponseAsJson(RequestType.GET, "path" + rootDocument.getPathAsString() + "@children");

        // The it return a response
        ArrayNode jsonNode = (ArrayNode) node.get("entries");
        assertEquals(session.getChildren(rootDocument.getRef()).size(), jsonNode.size());
    }

    @Test
    public void iCanUseAPageProvider() throws Exception {
        // Given a note with "nuxeo" in its description
        DocumentModel folder = RestServerInit.getFolder(1, session);

        // When I search for "nuxeo"
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                "path" + folder.getPathAsString() + "/@" + PageProviderAdapter.NAME + "/TEST_PP")) {

            // Then I get the two document in the result
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            assertEquals(2, getLogEntries(node).size());
        }
    }

    @Test
    public void iCanDeleteAListOfDocuments() throws Exception {
        // Given two notes
        DocumentModel note1 = RestServerInit.getNote(1, session);
        DocumentModel folder0 = RestServerInit.getFolder(0, session);

        // When i call a bulk delete
        String data = Joiner.on(";")
                            .join(Arrays.asList(new String[] { "id=" + note1.getId(), "id=" + folder0.getId() }));
        try (CloseableClientResponse response = getResponse(RequestType.DELETE, "/bulk;" + data)) {
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

            // Then the documents are removed from repository
            fetchInvalidations();

            assertFalse(session.exists(note1.getRef()));
            assertFalse(session.exists(folder0.getRef()));
        }
    }

    @Test
    public void iCanUpdateDocumentLists() throws Exception {
        // Given two notes
        DocumentModel note1 = RestServerInit.getNote(1, session);
        DocumentModel note2 = RestServerInit.getNote(2, session);

        String data = "{\"entity-type\":\"document\"," + "\"type\":\"Note\"," + "\"properties\":{"
                + "    \"dc:description\":\"bulk description\"" + "  }" + "}";

        // When i call a bulk update
        String ids = Joiner.on(";").join(Arrays.asList(new String[] { "id=" + note1.getId(), "id=" + note2.getId() }));
        try (CloseableClientResponse response = getResponse(RequestType.PUT, "/bulk;" + ids, data);) {

            // Then the documents are updated accordingly
            fetchInvalidations();
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            for (int i : new int[] { 1, 2 }) {
                note1 = RestServerInit.getNote(i, session);
                assertEquals("bulk description", note1.getPropertyValue("dc:description"));
            }
        }
    }

    /**
     * @since 8.10
     */
    @Test
    public void iCanPerformPageProviderWithDefinitionDefaultSorting() throws IOException {
        // Given a repository, when I perform a pageprovider on it with
        // default sorting in its definition on dc:title desc
        try (CloseableClientResponse response = getResponse(RequestType.GET,
                QueryObject.PATH + "/TEST_NOTE_PP_WITH_TITLE_ORDER")) {

            // Then I get document listing as result
            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> noteNodes = getLogEntries(node);
            assertEquals(RestServerInit.MAX_NOTE, noteNodes.size());
            for (int i = 0; i < noteNodes.size(); i++) {
                assertEquals("Note " + (RestServerInit.MAX_NOTE - (i + 1)),
                        noteNodes.get(i).get("title").textValue());
            }
        }
    }

}
