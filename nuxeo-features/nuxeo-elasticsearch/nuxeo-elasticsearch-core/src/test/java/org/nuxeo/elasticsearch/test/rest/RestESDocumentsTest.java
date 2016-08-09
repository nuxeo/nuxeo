/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.elasticsearch.test.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

import org.codehaus.jackson.JsonNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.core.operations.services.DocumentPageProviderOperation;
import org.nuxeo.ecm.automation.core.util.Properties;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.query.nxql.CoreQueryDocumentPageProvider;
import org.nuxeo.ecm.restapi.server.jaxrs.QueryObject;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.SearchAdapter;
import org.nuxeo.ecm.restapi.test.BaseTest;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.ecm.restapi.test.RestServerInit;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.provider.ElasticSearchNxqlPageProvider;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.Jetty;
import org.nuxeo.runtime.test.runner.LocalDeploy;

import com.sun.jersey.api.client.ClientResponse;

/**
 * Test the various ways to get elasticsearch Json output.
 *
 * @since 5.9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, RepositoryElasticSearchFeature.class })
@Jetty(port = 18090)
@Deploy("org.nuxeo.ecm.platform.contentview.jsf")
@LocalDeploy({ "org.nuxeo.ecm.platform.restapi.test:pageprovider-test-contrib.xml",
        "org.nuxeo.ecm.platform.restapi.test:elasticsearch-test-contrib.xml",
        "org.nuxeo.elasticsearch.core:contentviews-test-contrib.xml",
        "org.nuxeo.elasticsearch.core:contentviews-coretype-test-contrib.xml",
        "org.nuxeo.elasticsearch.core:pageprovider-search-test-contrib.xml" })
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class RestESDocumentsTest extends BaseTest {

    public static final String QUERY = "select * from Document where " + "ecm:currentLifeCycleState <> 'deleted'";

    @Inject
    PageProviderService pageProviderService;

    @Inject
    AutomationService automationService;

    @Test
    public void iCanBrowseTheRepoByItsId() throws Exception {
        // Given a document
        DocumentModel doc = RestServerInit.getNote(0, session);

        // When i do a GET Request
        ClientResponse response = getResponse(RequestType.GETES, "id/" + doc.getId());

        // Then I get the document as Json will all the properties
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        JsonNode node = mapper.readTree(response.getEntityInputStream());
        // System.err.println(node.toString());
        assertEquals("Note 0", node.get("note:note").getTextValue());
    }

    @Test
    public void iCanPerformESQLPageProviderOnRepository() throws IOException, InterruptedException {
        // wait for async jobs
        ElasticSearchAdmin esa = Framework.getLocalService(ElasticSearchAdmin.class);
        WorkManager wm = Framework.getLocalService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingWorkerCount());
        esa.refresh();
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        // Given a repository, when I perform a ESQL pageprovider on it
        ClientResponse response = getResponse(RequestType.GET, QueryObject.PATH + "/aggregates_2");

        // Then I get document listing as result
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        JsonNode node = mapper.readTree(response.getEntityInputStream());
        // Verify results
        assertEquals(15, getLogEntries(node).size());
        // And verify contributed aggregates
        assertEquals("terms", node.get("aggregations").get("coverage").get("type").getTextValue());
    }

    /**
     * Testing the REST_API_SEARCH_ADAPTER page provider when using elasticsearch.override.pageproviders conf variable
     * to replace the core page provider by ES generic.
     *
     * @since 7.4
     */
    @Test
    @SuppressWarnings("unchecked")
    public void isQueryEndpointCanSwitchToES() {
        Map<String, Serializable> props = new HashMap<>();
        props.put(CoreQueryDocumentPageProvider.CORE_SESSION_PROPERTY, (Serializable) session);
        PageProviderDefinition ppdefinition = pageProviderService.getPageProviderDefinition(
                SearchAdapter.pageProviderName);
        ppdefinition.setPattern(QUERY);
        ppdefinition.getProperties().put("maxResults", "1");
        PaginableDocumentModelListImpl res = new PaginableDocumentModelListImpl(
                (PageProvider<DocumentModel>) pageProviderService.getPageProvider(SearchAdapter.pageProviderName,
                        ppdefinition, null, null, 10000L, null, props, null),
                null);
        if (!(res.getProvider() instanceof ElasticSearchNxqlPageProvider)) {
            fail("Should be an elastic search page provider");
        }
    }

    @Test
    public void iCanPerformESQLPageProviderOperationOnRepository() throws Exception {
        OperationContext ctx = new OperationContext(session);
        Map<String, Object> params = new HashMap<>();
        String providerName = "default_search";

        params.put("providerName", providerName);
        Map<String, String> namedParameters = new HashMap<>();
        namedParameters.put("defaults:dc_nature_agg", "[\"article\"]");
        Properties namedProperties = new Properties(namedParameters);
        params.put("namedParameters", namedProperties);

        PaginableDocumentModelListImpl result = (PaginableDocumentModelListImpl) automationService.run(ctx,
                DocumentPageProviderOperation.ID, params);

        // test page size
        assertEquals(20, result.getPageSize());
        assertEquals(11, result.size());
    }
}
