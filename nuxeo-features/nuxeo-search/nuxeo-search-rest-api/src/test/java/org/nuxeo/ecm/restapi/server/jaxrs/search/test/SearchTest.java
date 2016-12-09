/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Gabriel Barata <gbarata@nuxeo.com
 */
package org.nuxeo.ecm.restapi.server.jaxrs.search.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * Test the various ways to perform queries via search endpoint.
 *
 * @since 8.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, PlatformFeature.class })
@Jetty(port = 18090)
@Deploy({ "org.nuxeo.ecm.platform.userworkspace.core", "org.nuxeo.ecm.platform.userworkspace.types",
        "org.nuxeo.ecm.platform.webapp.types", "org.nuxeo.ecm.platform.contentview.jsf", "org.nuxeo.search.ui",
        "org.nuxeo.ecm.platform.search.core", "org.nuxeo.ecm.platform.restapi.server.search" })
@LocalDeploy("org.nuxeo.ecm.platform.restapi.test:pageprovider-test-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class SearchTest extends BaseTest {

    @Inject
    protected CoreFeature coreFeature;

    protected static final String QUERY_EXECUTE_PATH = "search/lang/NXQL/execute";

    protected static final String SAVED_SEARCH_PATH = "search/saved";

    protected String getSearchPageProviderPath(String providerName) {
        return "search/pp/" + providerName;
    }

    protected String getSavedSearchPath(String id) {
        return "search/saved/" + id;
    }

    protected String getSavedSearchExecutePath(String id) {
        return "search/saved/" + id + "/execute";
    }

    protected String getSearchPageProviderExecutePath(String providerName) {
        return "search/pp/" + providerName + "/execute";
    }

    @Test
    public void iCanPerformQueriesOnRepository() throws IOException {
        // Given a repository, when I perform a query in NXQL on it
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("query", "SELECT * FROM Document");
        ClientResponse response = getResponse(RequestType.GET, QUERY_EXECUTE_PATH, queryParams);

        // Then I get document listing as result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(20, getLogEntries(node).size());

        // Given parameters as page size and ordered parameters
        queryParams.clear();
        queryParams.add("pageSize", "2");
        queryParams.add("queryParams", "$currentUser");
        queryParams.add("query", "select * from Document where " + "dc:creator = ?");

        // Given a repository, when I perform a query in NXQL on it
        response = getResponse(RequestType.GET, QUERY_EXECUTE_PATH, queryParams);

        // Then I get document listing as result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getLogEntries(node).size());
    }

    @Test
    public void iCanPerformQueriesWithNamedParametersOnRepository() throws IOException {
        // Given a repository and named parameters, when I perform a query in
        // NXQL on it
        DocumentModel folder = RestServerInit.getFolder(1, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("query", "SELECT * FROM Document WHERE " + "ecm:parentId = :parentIdVar AND\n"
                + "        ecm:mixinType != 'HiddenInNavigation' AND dc:title " + "IN (:note1,:note2)\n"
                + "        AND ecm:isCheckedInVersion = 0 AND " + "ecm:currentLifeCycleState !=\n"
                + "        'deleted'");
        queryParams.add("note1", "Note 1");
        queryParams.add("note2", "Note 2");
        queryParams.add("parentIdVar", folder.getId());
        ClientResponse response = getResponse(RequestType.GET, QUERY_EXECUTE_PATH, queryParams);

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
        ClientResponse response = getResponse(RequestType.GET, getSearchPageProviderExecutePath("TEST_PP"), queryParams);

        // Then I get document listing as result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getLogEntries(node).size());
    }

    @Test
    public void iCanPerformPageProviderWithNamedParametersOnRepository() throws IOException {
        // Given a repository, when I perform a pageprovider on it
        DocumentModel folder = RestServerInit.getFolder(1, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("note1", "Note 1");
        queryParams.add("note2", "Note 2");
        queryParams.add("parentIdVar", folder.getId());
        ClientResponse response = getResponse(RequestType.GET, getSearchPageProviderExecutePath("TEST_PP_PARAM"),
                queryParams);

        // Then I get document listing as result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getLogEntries(node).size());
    }

    @Test
    public void iCanPerformPageProviderWithQuickFilter() throws IOException {
        // Given a repository, when I perform a pageprovider on it
        DocumentModel folder = RestServerInit.getFolder(1, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("quickFilters", "testQF,testQF2");
        queryParams.add("parentIdVar", folder.getId());
        ClientResponse response = getResponse(RequestType.GET, getSearchPageProviderExecutePath("TEST_PP_QUICK_FILTER"),
                queryParams);

        // Then I get document listing as result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getLogEntries(node).size());

        assertTrue(node.get("quickFilters").isArray());
        assertEquals(3, node.get("quickFilters").size());
        for (JsonNode qf : node.get("quickFilters")) {
            String name = qf.get("name").getTextValue();
            boolean active = qf.get("active").getBooleanValue();
            assertEquals("testQF".equals(name) || "testQF2".equals(name),
                    active);
        }
    }

    /**
     * @since 8.4
     */
    @Test
    public void iDontAlterPageProviderDefWithQuickFilter() throws IOException {
        // Given a repository, when I perform a pageprovider on it
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("quickFilters", "testQF,testQF2");
        ClientResponse response = getResponse(RequestType.GET,
                getSearchPageProviderExecutePath("TEST_PP_QUICK_FILTER2"), queryParams);

        // Then I get document listing as result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getLogEntries(node).size());

        response = getResponse(RequestType.GET, getSearchPageProviderExecutePath("TEST_PP_QUICK_FILTER2"));

        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(20, getLogEntries(node).size());
    }

    /**
     * @since 8.4
     */
    @Test
    public void iCanUseAssociativeQuickFilter() throws IOException {
        // Given a repository, when I perform a pageprovider on it with quick filters
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("quickFilters", "testQF4,testQF");
        ClientResponse response = getResponse(RequestType.GET,
                getSearchPageProviderExecutePath("TEST_PP_QUICK_FILTER2"), queryParams);

        // Then I get document listing as result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        int nbResult = getLogEntries(node).size();

        // When I set the quick filtes the toeher way around
        queryParams = new MultivaluedMapImpl();
        queryParams.add("quickFilters", "testQF,testQF4");
        response = getResponse(RequestType.GET, getSearchPageProviderExecutePath("TEST_PP_QUICK_FILTER2"), queryParams);

        // Then I expect the same number of result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(nbResult, getLogEntries(node).size());
    }

    @Test
    public void iCanPerformPageProviderWithNamedParametersInvalid() throws Exception {
        ClientResponse response = getResponse(RequestType.GET,
                getSearchPageProviderExecutePath("namedParamProviderInvalid"));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(
                "Failed to execute query: SELECT * FROM Document where dc:title=:foo, Lexical Error: Illegal character <:> at offset 38",
                getErrorMessage(node));
    }

    @Test
    public void iCanPerformPageProviderWithNamedParametersAndDoc() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("np:title", "Folder 0");
        ClientResponse response = getResponse(RequestType.GET,
                getSearchPageProviderExecutePath("namedParamProviderWithDoc"), queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, getLogEntries(node).size());
    }

    @Test
    public void iCanPerformPageProviderWithNamedParametersAndDocInvalid() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("np:title", "Folder 0");
        ClientResponse response = getResponse(RequestType.GET,
                getSearchPageProviderExecutePath("namedParamProviderWithDocInvalid"), queryParams);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(
                "Failed to execute query: SELECT * FROM Document where dc:title=:foo, Lexical Error: Illegal character <:> at offset 38",
                getErrorMessage(node));
    }

    @Test
    public void iCanPerformPageProviderWithNamedParametersInWhereClause() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("parameter1", "Folder 0");
        ClientResponse response = getResponse(RequestType.GET,
                getSearchPageProviderExecutePath("namedParamProviderWithWhereClause"), queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, getLogEntries(node).size());

        // retry without params
        response = getResponse(RequestType.GET, getSearchPageProviderExecutePath("namedParamProviderWithWhereClause"));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getLogEntries(node).size());
    }

    @Test
    public void iCanPerformPageProviderWithNamedParametersInWhereClauseWithDoc() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("np:title", "Folder 0");
        ClientResponse response = getResponse(RequestType.GET,
                getSearchPageProviderExecutePath("namedParamProviderWithWhereClauseWithDoc"), queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, getLogEntries(node).size());

        // retry without params
        response = getResponse(RequestType.GET,
                getSearchPageProviderExecutePath("namedParamProviderWithWhereClauseWithDoc"));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getLogEntries(node).size());
    }

    @Test
    public void iCanPerformPageProviderWithNamedParametersComplex() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("parameter1", "Folder 0");
        queryParams.add("np:isCheckedIn", Boolean.FALSE.toString());
        queryParams.add("np:dateMin", "2007-01-30 01:02:03+04:00");
        queryParams.add("np:dateMax", "2007-03-23 01:02:03+04:00");
        ClientResponse response = getResponse(RequestType.GET,
                getSearchPageProviderExecutePath("namedParamProviderComplex"), queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, getLogEntries(node).size());

        // remove filter on dates
        queryParams.remove("np:dateMin");
        queryParams.remove("np:dateMax");
        response = getResponse(RequestType.GET, getSearchPageProviderExecutePath("namedParamProviderComplex"),
                queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(1, getLogEntries(node).size());

        queryParams.remove("parameter1");
        response = getResponse(RequestType.GET, getSearchPageProviderExecutePath("namedParamProviderComplex"),
                queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getLogEntries(node).size());
    }

    @Test
    public void iCanGetPageProviderDefinition() throws IOException {
        ClientResponse response = getResponse(RequestType.GET, getSearchPageProviderPath("namedParamProviderComplex"));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        PageProviderDefinition def = pageProviderService.getPageProviderDefinition("namedParamProviderComplex");
        assertEquals(def.getName(), node.get("name").getTextValue());
    }

    @Test
    public void iCanSaveSearchByQuery() throws IOException {
        String data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"search by query\",\n"
                + "  \"query\": \"select * from Document where dc:creator = ?\",\n"
                + "  \"queryLanguage\": \"NXQL\",\n" + "  \"queryParams\": \"$currentUser\",\n"
                + "  \"pageSize\": \"2\"\n," + "  \"contentViewData\": \"{" + "\\\"viewVar\\\": \\\"value\\\""
                + "}\"\n" + "}";

        ClientResponse response = getResponse(RequestType.POST, SAVED_SEARCH_PATH, data);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("savedSearch", node.get("entity-type").getTextValue());
        assertEquals("search by query", node.get("title").getTextValue());
        assertEquals("select * from Document where dc:creator = ?", node.get("query").getTextValue());
        assertEquals("NXQL", node.get("queryLanguage").getTextValue());
        assertEquals("$currentUser", node.get("queryParams").getTextValue());
        assertEquals("2", node.get("pageSize").getTextValue());
        assertEquals("{\"viewVar\": \"value\"}", node.get("contentViewData").getTextValue());
    }

    @Test
    public void iCanSaveSearchByPageProvider() throws IOException {
        DocumentModel folder = RestServerInit.getFolder(1, session);
        String data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"search by page provider\",\n"
                + "  \"pageProviderName\": \"TEST_PP\",\n" + "  \"queryParams\": \"" + folder.getId() + "\",\n"
                + "  \"contentViewData\": \"{" + "\\\"viewVar\\\": \\\"value\\\"" + "}\"\n" + "}";

        ClientResponse response = getResponse(RequestType.POST, SAVED_SEARCH_PATH, data);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("savedSearch", node.get("entity-type").getTextValue());
        assertEquals("search by page provider", node.get("title").getTextValue());
        assertEquals("TEST_PP", node.get("pageProviderName").getTextValue());
        assertEquals(folder.getId(), node.get("queryParams").getTextValue());
        assertEquals("{\"viewVar\": \"value\"}", node.get("contentViewData").getTextValue());
    }

    @Test
    public void iCanSaveDefaultSearch() throws IOException {
        String data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"search by page provider 2\",\n"
                + "  \"pageProviderName\": \"default_search\",\n" + "  \"pageSize\": \"2\",\n" + "  \"params\": {\n"
                + "    \"ecm_fulltext\": \"Note*\",\n" + "    \"dc_modified_agg\": [\"last24h\"]\n" + "  },\n"
                + "  \"contentViewData\": \"{" + "\\\"viewVar\\\": \\\"value\\\"" + "}\"\n" + "}";
        Map<String, String> headers = new HashMap<>();
        headers.put("x-nxdocumentproperties", "default_search");
        ClientResponse response = getResponse(RequestType.POST, SAVED_SEARCH_PATH, data, headers);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("savedSearch", node.get("entity-type").getTextValue());
        assertEquals("search by page provider 2", node.get("title").getTextValue());
        assertEquals("default_search", node.get("pageProviderName").getTextValue());
        assertEquals("2", node.get("pageSize").getTextValue());
        assertEquals("{\"viewVar\": \"value\"}", node.get("contentViewData").getTextValue());
        assertTrue(node.has("params"));
        node = node.get("params");
        assertEquals("Note*", node.get("defaults:ecm_fulltext").getTextValue());
        assertEquals(1, node.get("defaults:dc_modified_agg").size());
        assertEquals("last24h", node.get("defaults:dc_modified_agg").get(0).getTextValue());
    }

    @Test
    public void iCantSaveSearchInvalidParams() throws IOException {
        String data = "{\n" + "  \"entity-type\": \"savedSearch\",\n"
                + "  \"query\": \"select * from Document where dc:creator = ?\",\n"
                + "  \"queryLanguage\": \"NXQL\",\n" + "  \"queryParams\": \"$currentUser\",\n"
                + "  \"pageSize\": \"2\"\n" + "}";
        ClientResponse response = getResponse(RequestType.POST, SAVED_SEARCH_PATH, data);
        assertInvalidTitle(response);

        data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"search by query\",\n"
                + "  \"query\": \"select * from Document where dc:creator = ?\",\n"
                + "  \"queryLanguage\": \"NXQL\",\n" + "  \"queryParams\": \"$currentUser\",\n"
                + "  \"pageSize\": \"2\",\n" + "  \"pageProviderName\": \"TEST_PP\"\n" + "}";
        response = getResponse(RequestType.POST, SAVED_SEARCH_PATH, data);
        assertMixedQueryAndPageProvider(response);

        data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"search by query\",\n"
                + "  \"query\": \"select * from Document where dc:creator = ?\",\n"
                + "  \"queryParams\": \"$currentUser\",\n" + "  \"pageSize\": \"2\",\n"
                + "  \"pageProviderName\": \"TEST_PP\"\n" + "}";
        response = getResponse(RequestType.POST, SAVED_SEARCH_PATH, data);
        assertMixedQueryAndPageProvider(response);

        data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"search by query\",\n"
                + "  \"queryLanguage\": \"NXQL\",\n" + "  \"queryParams\": \"$currentUser\",\n"
                + "  \"pageSize\": \"2\",\n" + "  \"pageProviderName\": \"TEST_PP\"\n" + "}";
        response = getResponse(RequestType.POST, SAVED_SEARCH_PATH, data);
        assertMixedQueryAndPageProvider(response);

        data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"search by query\",\n"
                + "  \"query\": \"select * from Document where dc:creator = ?\",\n"
                + "  \"queryParams\": \"$currentUser\",\n" + "  \"pageSize\": \"2\"\n" + "}";
        response = getResponse(RequestType.POST, SAVED_SEARCH_PATH, data);
        assertMissingQueryLanguage(response);

        data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"search by query\",\n"
                + "  \"queryLanguage\": \"NXQL\",\n" + "  \"queryParams\": \"$currentUser\",\n"
                + "  \"pageSize\": \"2\"\n" + "}";
        response = getResponse(RequestType.POST, SAVED_SEARCH_PATH, data);
        assertMissingQuery(response);

        data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"search by query\",\n"
                + "  \"queryParams\": \"$currentUser\",\n" + "  \"pageSize\": \"2\"\n" + "}";
        response = getResponse(RequestType.POST, SAVED_SEARCH_PATH, data);
        assertMissingParams(response);
    }

    @Test
    public void iCanGetSavedSearchByQuery() throws IOException {
        ClientResponse response = getResponse(RequestType.GET,
                getSavedSearchPath(RestServerInit.getSavedSearchId(1, session)));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("savedSearch", node.get("entity-type").getTextValue());
        assertEquals("my saved search 1", node.get("title").getTextValue());
        assertEquals("select * from Document where dc:creator = ?", node.get("query").getTextValue());
        assertEquals("NXQL", node.get("queryLanguage").getTextValue());
        assertEquals("$currentUser", node.get("queryParams").getTextValue());
        assertEquals("2", node.get("pageSize").getTextValue());
    }

    @Test
    public void iCanGetSavedSearchByPageProvider() throws IOException {
        ClientResponse response = getResponse(RequestType.GET,
                getSavedSearchPath(RestServerInit.getSavedSearchId(2, session)));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("savedSearch", node.get("entity-type").getTextValue());
        assertEquals("my saved search 2", node.get("title").getTextValue());
        assertEquals("TEST_PP", node.get("pageProviderName").getTextValue());
        DocumentModel folder = RestServerInit.getFolder(1, session);
        assertEquals(folder.getId(), node.get("queryParams").getTextValue());
    }

    @Test
    public void iCantGetSavedSearchInvalidId() throws IOException {
        ClientResponse response = getResponse(RequestType.GET, getSavedSearchPath("-1"));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("unknown id: -1", getErrorMessage(node));
    }

    @Test
    public void iCanUpdateSearchByQuery() throws IOException {
        String data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"my search 1\",\n"
                + "  \"query\": \"select * from Document where dc:creator = ?\",\n"
                + "  \"queryLanguage\": \"NXQL\",\n" + "  \"queryParams\": \"$currentUser\",\n"
                + "  \"pageSize\": \"1\",\n" + "  \"contentViewData\": \"{" + "\\\"viewVar\\\": \\\"another value\\\""
                + "}\"\n" + "}";

        ClientResponse response = getResponse(RequestType.PUT,
                getSavedSearchPath(RestServerInit.getSavedSearchId(1, session)), data);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());

        assertEquals("savedSearch", node.get("entity-type").getTextValue());
        assertEquals("my search 1", node.get("title").getTextValue());
        assertEquals("select * from Document where dc:creator = ?", node.get("query").getTextValue());
        assertEquals("NXQL", node.get("queryLanguage").getTextValue());
        assertEquals("$currentUser", node.get("queryParams").getTextValue());
        assertEquals("1", node.get("pageSize").getTextValue());
        assertEquals("{\"viewVar\": \"another value\"}", node.get("contentViewData").getTextValue());
    }

    @Test
    public void iCanUpdateSearchByPageProvider() throws IOException {
        String data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"my search 2\",\n"
                + "  \"pageProviderName\": \"TEST_PP\",\n" + "  \"contentViewData\": \"{"
                + "\\\"viewVar\\\": \\\"another value\\\"" + "}\"\n" + "}";

        ClientResponse response = getResponse(RequestType.PUT,
                getSavedSearchPath(RestServerInit.getSavedSearchId(2, session)), data);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("savedSearch", node.get("entity-type").getTextValue());
        assertEquals("my search 2", node.get("title").getTextValue());
        assertEquals("TEST_PP", node.get("pageProviderName").getTextValue());
        assertNull(node.get("queryParams").getTextValue());
        assertEquals("{\"viewVar\": \"another value\"}", node.get("contentViewData").getTextValue());
    }

    @Test
    public void iCantUpdateSearchInvalidId() throws IOException {
        String data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"my search 1\",\n"
                + "  \"query\": \"select * from Document where dc:creator = ?\",\n"
                + "  \"queryLanguage\": \"NXQL\",\n" + "  \"queryParams\": \"$currentUser\",\n"
                + "  \"pageSize\": \"1\",\n" + "  \"contentViewData\": \"{" + "\\\"viewVar\\\": \\\"another value\\\""
                + "}\"\n" + "}";

        ClientResponse response = getResponse(RequestType.PUT, getSavedSearchPath("-1"), data);
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("unknown id: -1", getErrorMessage(node));
    }

    @Test
    public void iCantUpdateSearchInvalidQueryOrPageProvider() throws IOException {
        String data = "{\n" + "  \"entity-type\": \"savedSearch\",\n"
                + "  \"query\": \"select * from Document where dc:creator = ?\",\n"
                + "  \"queryLanguage\": \"NXQL\",\n" + "  \"queryParams\": \"$currentUser\",\n"
                + "  \"pageSize\": \"1\"\n" + "}";
        ClientResponse response = getResponse(RequestType.PUT,
                getSavedSearchPath(RestServerInit.getSavedSearchId(1, session)), data);
        assertInvalidTitle(response);

        data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"search by query\",\n"
                + "  \"query\": \"select * from Document where dc:creator = ?\",\n"
                + "  \"queryLanguage\": \"NXQL\",\n" + "  \"queryParams\": \"$currentUser\",\n"
                + "  \"pageSize\": \"2\",\n" + "  \"pageProviderName\": \"TEST_PP\"\n" + "}";
        response = getResponse(RequestType.PUT, getSavedSearchPath(RestServerInit.getSavedSearchId(1, session)), data);
        assertMixedQueryAndPageProvider(response);

        data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"search by query\",\n"
                + "  \"query\": \"select * from Document where dc:creator = ?\",\n"
                + "  \"queryParams\": \"$currentUser\",\n" + "  \"pageSize\": \"2\",\n"
                + "  \"pageProviderName\": \"TEST_PP\"\n" + "}";
        response = getResponse(RequestType.PUT, getSavedSearchPath(RestServerInit.getSavedSearchId(1, session)), data);
        assertMixedQueryAndPageProvider(response);

        data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"search by query\",\n"
                + "  \"queryLanguage\": \"NXQL\",\n" + "  \"queryParams\": \"$currentUser\",\n"
                + "  \"pageSize\": \"2\",\n" + "  \"pageProviderName\": \"TEST_PP\"\n" + "}";
        response = getResponse(RequestType.PUT, getSavedSearchPath(RestServerInit.getSavedSearchId(1, session)), data);
        assertMixedQueryAndPageProvider(response);

        data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"search by query\",\n"
                + "  \"query\": \"select * from Document where dc:creator = ?\",\n"
                + "  \"queryParams\": \"$currentUser\",\n" + "  \"pageSize\": \"2\"\n" + "}";
        response = getResponse(RequestType.PUT, getSavedSearchPath(RestServerInit.getSavedSearchId(1, session)), data);
        assertMissingQueryLanguage(response);

        data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"search by query\",\n"
                + "  \"queryLanguage\": \"NXQL\",\n" + "  \"queryParams\": \"$currentUser\",\n"
                + "  \"pageSize\": \"2\"\n" + "}";
        response = getResponse(RequestType.PUT, getSavedSearchPath(RestServerInit.getSavedSearchId(1, session)), data);
        assertMissingQuery(response);

        data = "{\n" + "  \"entity-type\": \"savedSearch\",\n" + "  \"title\": \"search by query\",\n"
                + "  \"queryParams\": \"$currentUser\",\n" + "  \"pageSize\": \"2\"\n" + "}";
        response = getResponse(RequestType.PUT, getSavedSearchPath(RestServerInit.getSavedSearchId(1, session)), data);
        assertMissingParams(response);
    }

    @Test
    public void iCanDeleteSearch() throws IOException {
        ClientResponse response = getResponse(RequestType.DELETE,
                getSavedSearchPath(RestServerInit.getSavedSearchId(1, session)));
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());
    }

    @Test
    public void iCantDeleteSearchInvalidId() throws IOException {
        ClientResponse response = getResponse(RequestType.DELETE, getSavedSearchPath("-1"));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    }

    @Test
    public void iCanExecuteSavedSearchByQuery() throws IOException {
        ClientResponse response = getResponse(RequestType.GET,
                getSavedSearchExecutePath(RestServerInit.getSavedSearchId(1, session)));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getLogEntries(node).size());
    }

    @Test
    public void iCanExecuteSavedSearchByQueryWithParams() throws IOException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("pageSize", "5");
        ClientResponse response = getResponse(RequestType.GET,
                getSavedSearchExecutePath(RestServerInit.getSavedSearchId(1, session)), queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(5, getLogEntries(node).size());
    }

    @Test
    public void iCanExecuteSavedSearchByPageProvider() throws IOException {
        ClientResponse response = getResponse(RequestType.GET,
                getSavedSearchExecutePath(RestServerInit.getSavedSearchId(2, session)));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getLogEntries(node).size());
    }

    @Test
    public void iCanExecuteDefaultSavedSearch() throws IOException {
        // this saved search uses ecm:fulltext so some databases doing async fulltext indexing will need a pause
        coreFeature.getStorageConfiguration().waitForFulltextIndexing();

        ClientResponse response = getResponse(RequestType.GET,
                getSavedSearchExecutePath(RestServerInit.getSavedSearchId(3, session)));
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals(2, getLogEntries(node).size());
    }

    @Test
    public void iCanSearchSavedSearches() throws IOException {
        ClientResponse response = getResponse(RequestType.GET, SAVED_SEARCH_PATH);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertTrue(node.isContainerNode());
        assertTrue(node.has("entries"));
        assertTrue(node.get("entries").isArray());
        assertEquals(3, node.get("entries").size());
    }

    @Test
    public void iCanSearchSavedSearchesParamPageProvider() throws IOException {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.putSingle("pageProvider", "TEST_PP");
        ClientResponse response = getResponse(RequestType.GET, SAVED_SEARCH_PATH, queryParams);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertTrue(node.isContainerNode());
        assertTrue(node.has("entries"));
        assertTrue(node.get("entries").isArray());
        assertEquals(1, node.get("entries").size());
        node = node.get("entries").get(0);
        assertEquals("my saved search 2", node.get("title").getTextValue());
        assertEquals("TEST_PP", node.get("pageProviderName").getTextValue());
        DocumentModel folder = RestServerInit.getFolder(1, session);
        assertEquals(folder.getId(), node.get("queryParams").getTextValue());
    }

    private void assertInvalidTitle(ClientResponse response) throws IOException {
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("title cannot be empty", getErrorMessage(node));
    }

    private void assertMixedQueryAndPageProvider(ClientResponse response) throws IOException {
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("query and page provider parameters are mutually exclusive"
                + " (query, queryLanguage, pageProviderName)", getErrorMessage(node));
    }

    private void assertMissingParams(ClientResponse response) throws IOException {
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("query or page provider parameters are missing" + " (query, queryLanguage, pageProviderName)",
                getErrorMessage(node));
    }

    private void assertMissingQueryLanguage(ClientResponse response) throws IOException {
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("queryLanguage parameter is missing", getErrorMessage(node));
    }

    private void assertMissingQuery(ClientResponse response) throws IOException {
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        assertEquals("query parameter is missing", getErrorMessage(node));
    }

}
