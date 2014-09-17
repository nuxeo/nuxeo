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
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Arrays;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.storage.sql.DatabaseHelper;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.server.jaxrs.QueryObject;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.ChildrenAdapter;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.PageProviderAdapter;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.SearchAdapter;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.google.common.base.Joiner;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Test the various ways to query for document lists.
 *
 * @since 5.7.2
 */
@RunWith(FeaturesRunner.class)
@Features(RestServerFeature.class)
@Jetty(port = 18090)
@LocalDeploy("org.nuxeo.ecm.platform.restapi.test:pageprovider-test-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class DocumentListTest extends BaseTest {

    @Test
    public void iCanGetTheChildrenOfADocument() throws Exception {
        // Given a folder
        DocumentModel folder = RestServerInit.getFolder(1, session);

        // When I query for it children
        ClientResponse response = getResponse(RequestType.GET,
                "id/" + folder.getId() + "/@" + ChildrenAdapter.NAME);

        // Then I get its children as JSON
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(session.getChildren(folder.getRef()).size(),
                getLogEntries(node).size());
    }

    @Test
    public void iCanSearchInFullTextForDocuments() throws Exception {
        // Given a note with "nuxeo" in its description
        DocumentModel note = RestServerInit.getNote(0, session);
        note.setPropertyValue("dc:description",
                "nuxeo one platform to rule them all");
        session.saveDocument(note);
        session.save();

        // Waiting for all async events work for indexing content before
        // executing fulltext search
        Framework.getLocalService(EventService.class).waitForAsyncCompletion();
        DatabaseHelper.DATABASE.sleepForFulltext();

        // When I search for "nuxeo"
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("fullText", "nuxeo");
        ClientResponse response = getResponse(RequestType.GET, "path/@"
                + SearchAdapter.NAME, queryParams);

        // Then I get the document in the result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, getLogEntries(node).size());

    }

    @Test
    public void iCanPerformQueriesOnRepository() throws IOException {
        // Given a repository, when I perform a query in NXQL on it
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("query", "SELECT * FROM Document");
        ClientResponse response = getResponse(RequestType.GET,
                "query", queryParams);

        // Then I get document listing as result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(15, getLogEntries(node).size());

        // Given a repository, when I perform a query in NXQL on it
        response = getResponse(RequestType.GET, QueryObject.PATH + "/" +
                QueryObject.NXQL, queryParams);

        // Then I get document listing as result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(15, getLogEntries(node).size());

        // Given parameters as page size and ordered parameters
        queryParams.clear();
        queryParams.add("pageSize", "2");
        queryParams.add("queryParams", "$currentUser");
        queryParams.add("query", "select * from Document where " +
                "dc:creator = ?");

        // Given a repository, when I perform a query in NXQL on it
        response = getResponse(RequestType.GET, QueryObject.PATH + "/" +
                QueryObject.NXQL, queryParams);

        // Then I get document listing as result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getLogEntries(node).size());
    }

    @Test
    public void iCanPerformQueriesWithNamedParametersOnRepository() throws
            IOException {
        // Given a repository and named parameters, when I perform a query in
        // NXQL on it
        DocumentModel folder = RestServerInit.getFolder(1, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("query", "SELECT * FROM Document WHERE " +
                "ecm:parentId = :parentIdVar AND\n" +
                "        ecm:mixinType != 'HiddenInNavigation' AND dc:title " +
                "IN (:note1,:note2)\n" +
                "        AND ecm:isCheckedInVersion = 0 AND " +
                "ecm:currentLifeCycleState !=\n" +
                "        'deleted'");
        queryParams.add("note1", "Note 1");
        queryParams.add("note2", "Note 2");
        queryParams.add("parentIdVar", folder.getId());
        ClientResponse response = getResponse(RequestType.GET,
                QueryObject.PATH, queryParams);

        // Then I get document listing as result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getLogEntries(node).size());
    }

    @Test
    public void iCanPerformPageProviderOnRepository() throws IOException {
        // Given a repository, when I perform a pageprovider on it
        DocumentModel folder = RestServerInit.getFolder(1, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("queryParams", folder.getId());
        ClientResponse response = getResponse(RequestType.GET,
                QueryObject.PATH + "/TEST_PP", queryParams);

        // Then I get document listing as result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getLogEntries(node).size());
    }

    @Test
    public void iCanPerformPageProviderWithNamedParametersOnRepository() throws
            IOException {
        // Given a repository, when I perform a pageprovider on it
        DocumentModel folder = RestServerInit.getFolder(1, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("note1", "Note 1");
        queryParams.add("note2", "Note 2");
        queryParams.add("parentIdVar", folder.getId());
        ClientResponse response = getResponse(RequestType.GET,
                QueryObject.PATH + "/TEST_PP_PARAM", queryParams);

        // Then I get document listing as result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getLogEntries(node).size());
    }

    @Test
    public void itCanGetAdapterForRootDocument() throws Exception {
        //Given the root document
        DocumentModel rootDocument = session.getRootDocument();

        //When i ask for an adapter
        JsonNode node = getResponseAsJson(RequestType.GET, "path"
                + rootDocument.getPathAsString() + "@children");

        //The it return a response
        ArrayNode jsonNode = (ArrayNode) node.get("entries");
        assertEquals(session.getChildren(rootDocument.getRef()).size(),
                jsonNode.size());
    }

    @Test
    public void iCanUseAPageProvider() throws Exception {
        // Given a note with "nuxeo" in its description
        DocumentModel folder = RestServerInit.getFolder(1, session);

        // When I search for "nuxeo"
        ClientResponse response = getResponse(RequestType.GET,
                "path" + folder.getPathAsString() + "/@"
                        + PageProviderAdapter.NAME + "/TEST_PP");

        // Then I get the two document in the result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getLogEntries(node).size());

    }

    @Test
    public void iCanDeleteAListOfDocuments() throws Exception {
        // Given two notes
        DocumentModel note1 = RestServerInit.getNote(1, session);
        DocumentModel folder0 = RestServerInit.getFolder(0, session);

        // When i call a bulk delete
        String data = Joiner.on(";").join(
                Arrays.asList(new String[] { "id=" + note1.getId(),
                        "id=" + folder0.getId() }));
        ClientResponse response = getResponse(RequestType.DELETE, "/bulk;"
                + data);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // Then the documents are removed from repository
        fetchInvalidations();

        assertFalse(session.exists(note1.getRef()));
        assertFalse(session.exists(folder0.getRef()));

    }

    @Test
    public void iCanUpdateDocumentLists() throws Exception {
        // Given two notes
        DocumentModel note1 = RestServerInit.getNote(1, session);
        DocumentModel note2 = RestServerInit.getNote(2, session);

        String data = "{\"entity-type\":\"document\"," + "\"type\":\"Note\","
                + "\"properties\":{"
                + "    \"dc:description\":\"bulk description\"" + "  }" + "}";

        // When i call a bulk update
        String ids = Joiner.on(";").join(
                Arrays.asList(new String[] { "id=" + note1.getId(),
                        "id=" + note2.getId() }));
        ClientResponse response = getResponse(RequestType.PUT, "/bulk;" + ids,
                data);

        // Then the documents are updated accordingly
        fetchInvalidations();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        for (int i : new int[] { 1, 2 }) {
            note1 = RestServerInit.getNote(i, session);
            assertEquals("bulk description",
                    note1.getPropertyValue("dc:description"));
        }

    }

}
