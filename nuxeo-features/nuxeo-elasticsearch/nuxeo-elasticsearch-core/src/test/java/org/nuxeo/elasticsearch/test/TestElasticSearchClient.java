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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.test;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.util.Date;

import javax.inject.Inject;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Check controller for Elasticsearch
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
@RunWith(FeaturesRunner.class)
@Features(BareElasticSearchFeature.class)
@Deploy({})
public class TestElasticSearchClient {

    @Inject
    protected Node elasticSearchNode;

    @Inject
    protected Client elasticSearchClient;

    @Test
    public void shouldHaveStartedElasticSearch() throws Exception {
        Assert.assertNotNull(elasticSearchNode);
        Assert.assertNotNull(elasticSearchClient);
        Assert.assertFalse(elasticSearchNode.isClosed());

        XContentBuilder builder = jsonBuilder().startObject().field("name", "test1").field("type", "File").field("yo",
                "man").field("dc:title", "Yohou").field("dc:created", new Date()).endObject();

        IndexResponse response = elasticSearchClient.prepareIndex("nxutest", "doc", "1").setSource(builder).execute().actionGet();

        Assert.assertNotNull(response.getId());

        // do the refresh
        elasticSearchClient.admin().indices().prepareRefresh().execute().actionGet();

        SearchResponse searchResponse = elasticSearchClient.prepareSearch("nxutest").setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(QueryBuilders.matchQuery("name", "test1")) // Query
        .setFrom(0).setSize(60).execute().actionGet();

        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());
        // System.out.println(searchResponse.getHits().getAt(0).sourceAsString());

        searchResponse = elasticSearchClient.prepareSearch("nxutest").setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(QueryBuilders.matchQuery("dc:title", "YoHou")) // Query
        .setFrom(0).setSize(60).execute().actionGet();

        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

    }

}
