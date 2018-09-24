/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 */

package org.nuxeo.ecm.restapi.server.jaxrs.search.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.actions.SetPropertiesAction;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.jaxrs.test.CloseableClientResponse;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.ServletContainer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreBulkFeature.class, RestServerFeature.class })
@ServletContainer(port = 18090)
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy("org.nuxeo.ecm.platform.userworkspace.core")
@Deploy("org.nuxeo.ecm.platform.userworkspace.types")
@Deploy("org.nuxeo.ecm.platform.search.core")
@Deploy("org.nuxeo.ecm.platform.restapi.server.search")
@Deploy("org.nuxeo.ecm.platform.restapi.test:pageprovider-test-contrib.xml")
public class BulkActionTest extends BaseTest {

    @Inject
    protected BulkService bulkService;

    @Test
    public void testExecuteBulkActionWithQuery() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("query", "SELECT * FROM Document WHERE ecm:isVersion = 0");
        testExecuteBulkAction("search", queryParams);
    }

    @Test
    public void testExecuteBulkActionWithQueryAndNamedParams() throws Exception {
        DocumentModel folder = RestServerInit.getFolder(1, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("query",
                "SELECT * FROM Document WHERE " + "ecm:parentId = :parentIdVar AND\n"
                        + "        ecm:mixinType != 'HiddenInNavigation' AND dc:title " + "IN (:note1,:note2)\n"
                        + "        AND ecm:isVersion = 0 AND " + "ecm:isTrashed = 0");
        queryParams.add("note1", "Note 1");
        queryParams.add("note2", "Note 2");
        queryParams.add("parentIdVar", folder.getId());
        testExecuteBulkAction("search", queryParams);
    }

    @Test
    public void testExecuteBulkActionWithPageProvider() throws Exception {
        DocumentModel folder = RestServerInit.getFolder(1, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("queryParams", folder.getId());
        testExecuteBulkAction("search/pp/TEST_PP", queryParams);
    }

    @Test
    public void testExecuteBulkActionWithPageProviderAndNamedParams() throws Exception {
        DocumentModel folder = RestServerInit.getFolder(1, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("note1", "Note 1");
        queryParams.add("note2", "Note 2");
        queryParams.add("parentIdVar", folder.getId());
        testExecuteBulkAction("search/pp/TEST_PP_PARAM", queryParams);
    }

    @Test
    public void testExecuteBulkActionWithPageProviderAndWhereClause() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("parameter1", "Folder 0");
        testExecuteBulkAction("search/pp/namedParamProviderWithWhereClause", queryParams);
    }

    @Test
    public void testExecuteBulkActionWithPageProviderAndQuickFilter() throws Exception {
        DocumentModel folder = RestServerInit.getFolder(1, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("quickFilters", "testQF");
        queryParams.add("parentIdVar", folder.getId());
        testExecuteBulkAction("search/pp/TEST_PP_QUICK_FILTER", queryParams);
    }

    @Test
    public void testExecuteBulkActionWithSavedSearch() throws Exception {
        String savedSearchId = RestServerInit.getSavedSearchId(1, session);
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        testExecuteBulkAction("search/saved/" + savedSearchId, queryParams);
    }

    protected void testExecuteBulkAction(String searchEndpoint, MultivaluedMap<String, String> queryParams)
            throws Exception {

        String actionId = SetPropertiesAction.ACTION_NAME;
        Map<String, String> params = Collections.singletonMap("dc:description", "bulk desc");
        String jsonParams = new ObjectMapper().writeValueAsString(params);

        try (CloseableClientResponse response = getResponse(RequestType.POST, searchEndpoint + "/bulk/" + actionId,
                jsonParams, queryParams, null, null)) {

            assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            String commandId = node.get("id").textValue();

            assertTrue("Bulk action didn't finish", bulkService.await(Duration.ofSeconds(10)));

            BulkStatus status = bulkService.getStatus(commandId);
            assertNotNull(status);
            assertEquals(COMPLETED, status.getState());
        }

        try (CloseableClientResponse response = getResponse(RequestType.GET, searchEndpoint + "/execute",
                queryParams)) {

            assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
            JsonNode node = mapper.readTree(response.getEntityInputStream());
            List<JsonNode> noteNodes = getLogEntries(node);
            for (JsonNode noteNode : noteNodes) {
                assertEquals("bulk desc", noteNode.get("properties").get("dc:description").textValue());
            }
        }
    }

}
