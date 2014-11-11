/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.elasticsearch.test;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.util.Date;

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

import com.google.inject.Inject;

/**
 * Check controller for Elasticsearch
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 *
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

        XContentBuilder builder = jsonBuilder().startObject().field("name",
                "test1").field("type", "File").field("yo", "man").field(
                "dc:title", "Yohou").field("dc:created", new Date()).endObject();

        IndexResponse response = elasticSearchClient.prepareIndex("nxutest",
                "doc", "1").setSource(builder).execute().actionGet();

        Assert.assertNotNull(response.getId());

        // do the refresh
        elasticSearchClient.admin().indices().prepareRefresh().execute().actionGet();

        SearchResponse searchResponse = elasticSearchClient.prepareSearch(
                "nxutest").setTypes("doc").setSearchType(
                SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.matchQuery("name", "test1")) // Query
        .setFrom(0).setSize(60).execute().actionGet();

        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());
        //System.out.println(searchResponse.getHits().getAt(0).sourceAsString());

        searchResponse = elasticSearchClient.prepareSearch("nxutest").setTypes(
                "doc").setSearchType(SearchType.DFS_QUERY_THEN_FETCH).setQuery(
                QueryBuilders.matchQuery("dc:title", "YoHou")) // Query
        .setFrom(0).setSize(60).execute().actionGet();

        Assert.assertEquals(1, searchResponse.getHits().getTotalHits());

    }

}
