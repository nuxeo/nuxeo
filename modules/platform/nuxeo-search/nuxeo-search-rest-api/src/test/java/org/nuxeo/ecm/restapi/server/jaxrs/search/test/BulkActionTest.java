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

import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.nuxeo.ecm.core.bulk.message.BulkStatus.State.COMPLETED;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.function.ThrowableConsumer;
import org.nuxeo.common.function.ThrowableFunction;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.bulk.BulkService;
import org.nuxeo.ecm.core.bulk.CoreBulkFeature;
import org.nuxeo.ecm.core.bulk.action.SetPropertiesAction;
import org.nuxeo.ecm.core.bulk.message.BulkStatus;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.restapi.server.jaxrs.search.test.bulk.RemoveDocumentAction;
import org.nuxeo.ecm.restapi.test.JsonNodeHelper;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @since 10.3
 */
@RunWith(FeaturesRunner.class)
@Features({ CoreBulkFeature.class, RestServerFeature.class })
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
@Deploy("org.nuxeo.ecm.platform.userworkspace")
@Deploy("org.nuxeo.ecm.platform.search.core")
@Deploy("org.nuxeo.ecm.platform.restapi.server.search")
@Deploy("org.nuxeo.ecm.platform.restapi.test:pageprovider-test-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.restapi.test:bulk-actions-test-contrib.xml")
public class BulkActionTest {

    @Inject
    protected CoreFeature coreFeature;

    @Inject
    protected RestServerFeature restServerFeature;

    @Inject
    protected BulkService bulkService;

    @Inject
    protected CoreSession session;

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.builder()
                                                                   .url(() -> restServerFeature.getRestApiUrl())
                                                                   .adminCredentials()
                                                                   .accept(MediaType.APPLICATION_JSON)
                                                                   .contentType(MediaType.APPLICATION_JSON)
                                                                   .header("X-NXDocumentProperties", "dublincore")
                                                                   .build();

    @Test
    public void testExecuteBulkActionWithQuery() {
        Map<String, String> queryParams = Map.of("query", "SELECT * FROM Document WHERE ecm:isVersion = 0");
        testExecuteBulkAction("search", queryParams);
    }

    @Test
    public void testExecuteBulkActionWithQueryAndNamedParams() {
        DocumentModel folder = RestServerInit.getFolder(1, session);
        Map<String, String> queryParams = Map.of( //
                "query", """
                        SELECT * FROM Document WHERE ecm:parentId = :parentIdVar AND
                                ecm:mixinType != 'HiddenInNavigation' AND dc:title IN (:note1,:note2)
                                AND ecm:isVersion = 0 AND ecm:isTrashed = 0""", //
                "note1", "Note 1", //
                "note2", "Note 2", //
                "parentIdVar", folder.getId());
        testExecuteBulkAction("search", queryParams);
    }

    @Test
    public void testExecuteBulkActionWithPageProvider() {
        DocumentModel folder = RestServerInit.getFolder(1, session);
        Map<String, String> queryParams = Map.of("queryParams", folder.getId());
        testExecuteBulkAction("search/pp/TEST_PP", queryParams);
    }

    /**
     * @since 10.3
     */
    @Test
    public void testExecuteBulkActionWithPageProviderAndEmptyParams() {
        httpClient.buildPostRequest("search/pp/TEST_PP_ALL_NOTE/bulk/" + RemoveDocumentAction.ACTION_NAME)
                  .executeAndConsume(new JsonNodeHandler(SC_ACCEPTED), ThrowableConsumer.asConsumer(node -> {
                      String commandId = node.get("commandId").textValue();

                      assertTrue("Bulk action didn't finish", bulkService.await(commandId, Duration.ofSeconds(10)));

                      BulkStatus status = bulkService.getStatus(commandId);
                      assertNotNull(status);
                      assertEquals(COMPLETED, status.getState());
                  }));

        httpClient.buildGetRequest("search/pp/TEST_PP_ALL_NOTE/execute")
                  .executeAndConsume(new JsonNodeHandler(),
                          node -> assertTrue(JsonNodeHelper.getEntries(node).isEmpty()));
    }

    @Test
    public void testExecuteBulkActionWithPageProviderAndNamedParams() {
        DocumentModel folder = RestServerInit.getFolder(1, session);
        Map<String, String> queryParams = Map.of( //
                "note1", "Note 1", //
                "note2", "Note 2", //
                "parentIdVar", folder.getId());
        testExecuteBulkAction("search/pp/TEST_PP_PARAM", queryParams);
    }

    @Test
    public void testExecuteBulkActionWithPageProviderAndWhereClause() {
        Map<String, String> queryParams = Map.of("parameter1", "Folder 0");
        testExecuteBulkAction("search/pp/namedParamProviderWithWhereClause", queryParams);
    }

    @Test
    public void testExecuteBulkActionWithPageProviderAndQuickFilter() {
        DocumentModel folder = RestServerInit.getFolder(1, session);
        Map<String, String> queryParams = Map.of( //
                "quickFilters", "testQF", //
                "parentIdVar", folder.getId());
        testExecuteBulkAction("search/pp/TEST_PP_QUICK_FILTER", queryParams);
    }

    @Test
    public void testExecuteBulkActionWithSavedSearch() {
        assumeTrue("fulltext search not supported", coreFeature.getStorageConfiguration().supportsFulltextSearch());

        String savedSearchId = RestServerInit.getSavedSearchId(3, session);
        testExecuteBulkAction("search/saved/" + savedSearchId, Map.of());
    }

    protected void testExecuteBulkAction(String searchEndpoint, Map<String, String> queryParams) {

        executeBulkAction(searchEndpoint, queryParams);

        httpClient.buildGetRequest(searchEndpoint + "/execute")
                  .addQueryParameters(queryParams)
                  .executeAndConsume(new JsonNodeHandler(), node -> {
                      List<JsonNode> noteNodes = JsonNodeHelper.getEntries(node);
                      for (JsonNode noteNode : noteNodes) {
                          assertEquals("bulk desc", noteNode.get("properties").get("dc:description").textValue());
                      }
                  });
    }

    protected BulkStatus executeBulkAction(String searchEndpoint, Map<String, String> queryParams) {
        return httpClient.buildPostRequest(searchEndpoint + "/bulk/" + SetPropertiesAction.ACTION_NAME)
                         .addQueryParameters(queryParams)
                         .entity("{\"dc:description\":\"bulk desc\"}")
                         .executeAndThen(new JsonNodeHandler(SC_ACCEPTED), ThrowableFunction.asFunction(node -> {
                             String commandId = node.get("commandId").textValue();

                             assertTrue("Bulk action didn't finish",
                                     bulkService.await(commandId, Duration.ofSeconds(10)));

                             BulkStatus status = bulkService.getStatus(commandId);
                             assertNotNull(status);
                             assertEquals(COMPLETED, status.getState());
                             return status;
                         }));
    }

    @Test
    public void testExecuteBulkWithAScroller() {
        Map<String, String> queryParams = Map.of( //
                "query", "SELECT * FROM Document WHERE ecm:isVersion = 0", //
                "scroll", "repository");
        testExecuteBulkAction("search", queryParams);
    }

    @Test
    public void testExecuteBulkWithAQueryLimit() {
        Map<String, String> queryParams = Map.of( //
                "query", "SELECT * FROM Document WHERE ecm:isVersion = 0", //
                "queryLimit", "1");
        BulkStatus status = executeBulkAction("search", queryParams);
        assertEquals(1, status.getTotal());
        assertEquals(true, status.isQueryLimitReached());
    }

}
