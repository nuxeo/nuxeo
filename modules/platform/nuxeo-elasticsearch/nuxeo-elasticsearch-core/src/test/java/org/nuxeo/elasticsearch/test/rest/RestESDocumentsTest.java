/*
 * (C) Copyright 2014-2019 Nuxeo SA (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.nuxeo.ecm.collections.api.CollectionConstants.COLLECTION_PAGE_PROVIDER;
import static org.nuxeo.ecm.collections.api.CollectionConstants.COLLECTION_TYPE;
import static org.nuxeo.ecm.platform.dublincore.constants.DublinCoreConstants.DUBLINCORE_TITLE_PROPERTY;
import static org.nuxeo.ecm.platform.tag.TagConstants.TAG_FACET;
import static org.nuxeo.elasticsearch.provider.ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.directory.test.DirectoryFeature;
import org.nuxeo.ecm.automation.jaxrs.io.documents.PaginableDocumentModelListImpl;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.restapi.server.jaxrs.QueryObject;
import org.nuxeo.ecm.restapi.server.jaxrs.adapters.SearchAdapter;
import org.nuxeo.ecm.platform.query.api.PageProviderSpec;
import org.nuxeo.ecm.restapi.test.JsonNodeHelper;
import org.nuxeo.ecm.restapi.test.RestServerFeature;
import org.nuxeo.ecm.restapi.test.RestServerInit;
import org.nuxeo.elasticsearch.api.ElasticSearchAdmin;
import org.nuxeo.elasticsearch.io.marshallers.json.AggregateJsonWriter;
import org.nuxeo.elasticsearch.provider.ElasticSearchNativePageProvider;
import org.nuxeo.elasticsearch.provider.ElasticSearchNxqlPageProvider;
import org.nuxeo.elasticsearch.test.RepositoryElasticSearchFeature;
import org.nuxeo.http.test.HttpClientTestRule;
import org.nuxeo.http.test.handler.JsonNodeHandler;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test the various ways to get elasticsearch Json output.
 *
 * @since 5.9.3
 */
@RunWith(FeaturesRunner.class)
@Features({ RestServerFeature.class, RepositoryElasticSearchFeature.class, DirectoryFeature.class })
@Deploy("org.nuxeo.ecm.platform.tag")
@Deploy("org.nuxeo.ecm.platform.restapi.test:pageprovider-test-contrib.xml")
@Deploy("org.nuxeo.ecm.platform.restapi.test:elasticsearch-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core:pageprovider2-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core:pageprovider2-coretype-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core:pageprovider-search-test-contrib.xml")
@Deploy("org.nuxeo.elasticsearch.core:test-directory-contrib.xml")
@RepositoryConfig(cleanup = Granularity.METHOD, init = RestServerInit.class)
public class RestESDocumentsTest {

    public static final String QUERY = "select * from Document where " + "ecm:isTrashed = 0";

    public static final String TEST_MIME_TYPE = "text/plain";

    /**
     * @since 11.1
     */
    protected static final String ROOT_PATH = "/";

    /**
     * @since 11.1
     */
    protected static final String COLLECTION_NAME = "testCollection";

    @Inject
    protected CoreSession session;

    @Inject
    protected PageProviderService pageProviderService;

    @Inject
    protected RestServerFeature restServerFeature;

    @Inject
    protected TransactionalFeature txFeature;

    @Rule
    public final HttpClientTestRule httpClient = HttpClientTestRule.defaultClient(
            () -> restServerFeature.getRestApiUrl());

    @Test
    public void iCanPerformESQLPageProviderOnRepository() throws InterruptedException {
        // wait for async jobs
        waitForAsync();
        // Given a repository, when I perform a ESQL pageprovider on it
        JsonNode node = httpClient.buildGetRequest(QueryObject.PATH + "/aggregates_2").execute(new JsonNodeHandler());
        // Then I get document listing as result
        // Verify results
        assertEquals(20, JsonNodeHelper.getEntries(node).size());
        // And verify contributed aggregates
        assertEquals("terms", node.get("aggregations").get("coverage").get("type").textValue());
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
        PageProviderDefinition ppdefinition = pageProviderService.getPageProviderDefinition(
                SearchAdapter.pageProviderName);
        ppdefinition.setPattern(QUERY);
        ppdefinition.getProperties().put("maxResults", "1");
        PaginableDocumentModelListImpl res = new PaginableDocumentModelListImpl(
                (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                        PageProviderSpec.builder(ppdefinition)
                                        .pageSize(10000L)
                                        .property(CORE_SESSION_PROPERTY, (Serializable) session)
                                        .build()),
                null);
        if (!(res.getProvider() instanceof ElasticSearchNxqlPageProvider)) {
            fail("Should be an elastic search page provider");
        }
    }

    @Test
    @Deploy("org.nuxeo.ecm.platform.collections.core:OSGI-INF/collection-pageprovider-contrib.xml")
    @Deploy("org.nuxeo.elasticsearch.core.test:pageprovider-replacers-test-contrib.xml")
    public void iCanUseFulltextOperatorWithElasticsearchPageProvider() {
        DocumentModel coll1 = session.createDocumentModel(ROOT_PATH, COLLECTION_NAME + 1, COLLECTION_TYPE);
        coll1.setPropertyValue(DUBLINCORE_TITLE_PROPERTY, coll1.getName());
        DocumentModel coll2 = session.createDocumentModel(ROOT_PATH, COLLECTION_NAME + 2, COLLECTION_TYPE);
        coll2.setPropertyValue(DUBLINCORE_TITLE_PROPERTY, coll2.getName());
        DocumentModel fufu = session.createDocumentModel(ROOT_PATH, "furtiveCollection", COLLECTION_TYPE);
        fufu.setPropertyValue(DUBLINCORE_TITLE_PROPERTY, fufu.getName());

        session.createDocument(coll1);
        session.createDocument(coll2);
        session.createDocument(fufu);

        txFeature.nextTransaction();

        PageProviderDefinition ppdef = pageProviderService.getPageProviderDefinition(COLLECTION_PAGE_PROVIDER);

        Map<String, Serializable> props = Map.of(ElasticSearchNativePageProvider.CORE_SESSION_PROPERTY,
                (Serializable) session);
        @SuppressWarnings("unchecked")
        PageProvider<DocumentModel> pp = (PageProvider<DocumentModel>) pageProviderService.getPageProvider(
                PageProviderSpec.builder(ppdef)
                                .currentPage(0L)
                                .property(CORE_SESSION_PROPERTY, (Serializable) session)
                                .parameters("testCo")
                                .build());

        List<DocumentModel> page = pp.getCurrentPage();
        assertEquals(2, page.size());
        assertEquals(coll1.getName(), page.get(0).getName());
        assertEquals(coll2.getName(), page.get(1).getName());
    }

    /**
     * @since 8.10
     */
    @Test
    public void iCanQueryESQLPageProviderAndFetchAggregateKeys() throws Exception {
        // Updating a note automatically creates a version of it
        for (int i = 0; i < RestServerInit.MAX_NOTE; i++) {
            DocumentModel doc = RestServerInit.getNote(i, session);
            doc.setPropertyValue("dc:coverage", "europe/France");
            doc.setPropertyValue("dc:subjects", new String[] { "art/cinema" });
            doc = session.saveDocument(doc);
        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        waitForAsync();

        // Given a repository, when I perform a ESQL pageprovider on it
        JsonNode node = httpClient.buildGetRequest(QueryObject.PATH + "/aggregates_3")
                                  .addHeader("fetch." + AggregateJsonWriter.ENTITY_TYPE, AggregateJsonWriter.FETCH_KEY)
                                  .execute(new JsonNodeHandler());
        // Then I get document listing as result
        // And verify contributed aggregates
        assertEquals("terms", node.get("aggregations").get("coverage").get("type").textValue());
        JsonNode bucket = node.get("aggregations").get("coverage").get("buckets").get(0);
        int docCount = bucket.get("docCount").intValue();
        assertEquals(RestServerInit.MAX_NOTE, docCount);
        // Check that the key of the bucket which is a l10ncoverage vocabulary entry has been fetch
        String keyText = bucket.get("key").textValue();
        assertEquals("europe/France", keyText);
        String fetchedkeyIdText = bucket.get("fetchedKey").get("properties").get("id").textValue();
        assertEquals("France", fetchedkeyIdText);

        // And verify contributed aggregates
        assertEquals("terms", node.get("aggregations").get("subjects").get("type").textValue());
        JsonNode firstBucket = node.get("aggregations").get("subjects").get("buckets").get(0);
        docCount = firstBucket.get("docCount").intValue();
        assertEquals(RestServerInit.MAX_NOTE, docCount);
        // Check that the key of the bucket which is a l10nsubjects vocabulary entry has been fetch
        keyText = firstBucket.get("key").textValue();
        assertEquals("art/cinema", keyText);
        fetchedkeyIdText = firstBucket.get("fetchedKey").get("properties").get("id").textValue();
        assertEquals("cinema", fetchedkeyIdText);

        JsonNode primaryTypeNode = node.get("aggregations").get("primaryType");
        assertEquals("terms", primaryTypeNode.get("type").textValue());
        JsonNode noteTypeNode = StreamSupport.stream(primaryTypeNode.get("buckets").spliterator(), false)
                                             .filter(n -> "Note".equals(n.get("key").textValue()))
                                             .findFirst()
                                             .orElse(null);
        assertNotNull(noteTypeNode);

        JsonNode isVersionNode = node.get("aggregations").get("isVersion");
        assertEquals("terms", isVersionNode.get("type").textValue());

        JsonNode mixinTypeNode = node.get("aggregations").get("mixinType");
        assertEquals("terms", mixinTypeNode.get("type").textValue());
        JsonNode tagFacetNode = StreamSupport.stream(mixinTypeNode.get("buckets").spliterator(), false)
                                             .filter(n -> TAG_FACET.equals(n.get("key").textValue()))
                                             .findFirst()
                                             .orElse(null);
        assertNotNull(tagFacetNode);

        JsonNode level1Node = node.get("aggregations").get("level1");
        assertEquals("terms", level1Node.get("type").textValue());
        JsonNode noteLevel1Node = StreamSupport.stream(level1Node.get("buckets").spliterator(), false)
                                               .filter(n -> "folder_1".equals(n.get("key").textValue()))
                                               .findFirst()
                                               .orElse(null);
        assertNotNull(noteLevel1Node);

        // Test invalid system property as page provider aggregate
        httpClient.buildGetRequest(QueryObject.PATH + "/invalid_system_prop_aggregate")
                  .addHeader("fetch." + AggregateJsonWriter.ENTITY_TYPE, AggregateJsonWriter.FETCH_KEY)
                  .executeAndConsume(new JsonNodeHandler(),
                          jsonNode -> assertNull(jsonNode.get("aggregations").get("path").get("buckets")));
    }

    /**
     * @since 10.3
     */
    protected void waitForAsync() throws InterruptedException {
        // wait for async jobs
        ElasticSearchAdmin esa = Framework.getService(ElasticSearchAdmin.class);
        WorkManager wm = Framework.getService(WorkManager.class);
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
        Assert.assertEquals(0, esa.getPendingWorkerCount());
        esa.refresh();
        Assert.assertTrue(wm.awaitCompletion(20, TimeUnit.SECONDS));
    }

    /**
     * @since 10.3
     */
    @Test
    public void iCanQueryESQLPageProviderAndFetchVariousAggregates() throws Exception {
        for (int i = 0; i < 50; i++) {
            DocumentModel doc = session.createDocumentModel(ROOT_PATH, "aggTest" + i, "File");
            doc.setPropertyValue("dc:coverage", "europe/Spain");
            doc.setPropertyValue(DUBLINCORE_TITLE_PROPERTY, "tight_" + i % 2);
            if (i % 3 == 0) {
                doc.setPropertyValue("dc:description", "subs" + i % 4);
            }
            if (i % 5 == 0) {
                Blob blob = Blobs.createBlob("My text isn't very long." + i, TEST_MIME_TYPE);
                doc.setPropertyValue("file:content", (Serializable) blob);
            }
            doc = session.createDocument(doc);
            session.saveDocument(doc);
        }

        TransactionHelper.commitOrRollbackTransaction();
        TransactionHelper.startTransaction();
        waitForAsync();

        JsonNode node = httpClient.buildGetRequest(QueryObject.PATH + "/aggregates_4")
                                  .addHeader("fetch." + AggregateJsonWriter.ENTITY_TYPE, AggregateJsonWriter.FETCH_KEY)
                                  .execute(new JsonNodeHandler());
        JsonNode aggregations = node.get("aggregations");
        assertEquals(258, aggregations.get("sum").get("value").intValue());
        assertEquals("25.8", aggregations.get("avg").get("value").asText());
        assertEquals("0.0", aggregations.get("min").get("value").asText());
        assertEquals("0.0", aggregations.get("max").get("value").asText());
        assertEquals(17, aggregations.get("count_desc").get("value").intValue());
        assertEquals(50, aggregations.get("count_title").get("value").intValue());
        assertEquals(40, aggregations.get("missing_content_length").get("value").intValue());
        assertEquals(33, aggregations.get("missing_description").get("value").intValue());
        assertEquals(0, aggregations.get("missing_title").get("value").intValue());
        assertEquals(2, aggregations.get("cardinality_title").get("value").intValue());
        assertEquals(4, aggregations.get("cardinality_description").get("value").intValue());
    }
}
