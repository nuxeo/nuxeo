/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.elasticsearch.client;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.elasticsearch.api.ESClient;

/**
 * @since 9.3
 */
public class ESTransportClient implements ESClient {
    private static final Log log = LogFactory.getLog(ESTransportClient.class);

    protected Client client;

    public ESTransportClient(Client client) {
        this.client = client;
    }

    @Override
    public boolean waitForYellowStatus(String[] indexNames, int timeoutSecond) {
        String timeout = timeoutSecond + "s";
        log.debug("Waiting for cluster yellow health status, indexes: " + Arrays.toString(indexNames));
        try {
            ClusterHealthResponse response = client.admin()
                                                   .cluster()
                                                   .prepareHealth(indexNames)
                                                   .setTimeout(timeout)
                                                   .setWaitForYellowStatus()
                                                   .get();
            if (response.isTimedOut()) {
                throw new NuxeoException(
                        "Elasticsearch Cluster health status not Yellow after " + timeout + " give up: " + response);
            }
            if (response.getStatus() != ClusterHealthStatus.GREEN) {
                log.warn("Elasticsearch Cluster ready but not GREEN: " + response);
                return false;
            }
            log.info("Elasticsearch Cluster ready: " + response);
        } catch (NoNodeAvailableException e) {
            throw new NuxeoException(
                    "Failed to connect to elasticsearch, check addressList and clusterName: " + e.getMessage());
        }
        return true;
    }

    @Override
    public ClusterHealthStatus getHealthStatus(String[] indexNames) {
        return client.admin().cluster().prepareHealth(indexNames).get().getStatus();
    }

    @Override
    public void refresh(String indexName) {
        client.admin().indices().prepareRefresh(indexName).get();
    }

    @Override
    public void flush(String indexName) {
        client.admin().indices().prepareFlush(indexName).get();
    }

    @Override
    public void optimize(String indexName) {
        client.admin().indices().prepareForceMerge(indexName).get();
    }

    @Override
    public boolean indexExists(String indexName) {
        return client.admin().indices().prepareExists(indexName).execute().actionGet().isExists();
    }

    @Override
    public boolean mappingExists(String indexName, String type) {
        GetMappingsResponse mappings = client.admin()
                                             .indices()
                                             .prepareGetMappings(indexName)
                                             .execute()
                                             .actionGet();
        if (mappings == null || mappings.getMappings().isEmpty()) {
            return false;
        }
        // The real index might have another name if indexName is an alias so we check the mapping of the first item.
        return mappings.getMappings().values().iterator().next().value.containsKey(type);
    }

    @Override
    public void deleteIndex(String indexName, int timeoutSecond) {
        TimeValue timeout = new TimeValue(timeoutSecond, TimeUnit.SECONDS);
        client.admin()
              .indices()
              .delete(new DeleteIndexRequest(indexName).timeout(timeout).masterNodeTimeout(timeout))
              .actionGet();
    }

    @Override
    public void createIndex(String indexName, String jsonSettings) {
        client.admin().indices().prepareCreate(indexName).setSettings(jsonSettings, XContentType.JSON).get();
    }

    @Override
    public void createMapping(String indexName, String type, String jsonMapping) {
        client.admin()
              .indices()
              .preparePutMapping(indexName)
              .setType(type)
              .setSource(jsonMapping, XContentType.JSON)
              .get();
    }

    @Override
    public String getNodesInfo() {
        return client.admin().cluster().prepareNodesInfo().get().toString();
    }

    @Override
    public String getNodesStats() {
        return client.admin().cluster().prepareNodesStats().get().toString();
    }

    @Override
    public boolean aliasExists(String aliasName) {
        return client.admin().indices().prepareAliasesExist(aliasName).get().isExists();
    }

    @Override
    public String getFirstIndexForAlias(String aliasName) {
        ImmutableOpenMap<String, List<AliasMetaData>> aliases = client.admin()
                                                                      .indices()
                                                                      .prepareGetAliases(aliasName)
                                                                      .get()
                                                                      .getAliases();
        for (Iterator<String> it = aliases.keysIt(); it.hasNext();) {
            String indexName = it.next();
            if (!aliases.get(indexName).isEmpty()) {
                return indexName;
            }
        }
        return null;
    }

    @Override
    public void updateAlias(String aliasName, String indexName) {
        IndicesAliasesRequestBuilder cmd = client.admin().indices().prepareAliases();
        if (aliasExists(aliasName)) {
            cmd.removeAlias(getFirstIndexForAlias(aliasName), aliasName);
        }
        cmd.addAlias(indexName, aliasName).execute().actionGet();
    }

    @Override
    public BulkResponse bulk(BulkRequest request) {
        return client.bulk(request).actionGet();
    }

    @Override
    public DeleteResponse delete(DeleteRequest request) {
        return client.delete(request).actionGet();
    }

    @Override
    public SearchResponse search(SearchRequest request) {
        return client.search(request).actionGet();
    }

    @Override
    public SearchResponse searchScroll(SearchScrollRequest request) {
        return client.searchScroll(request).actionGet();
    }

    @Override
    public GetResponse get(GetRequest request) {
        return client.get(request).actionGet();
    }

    @Override
    public IndexResponse index(IndexRequest request) {
        return client.index(request).actionGet();
    }

    @Override
    public ClearScrollResponse clearScroll(ClearScrollRequest request) {
        return client.clearScroll(request).actionGet();
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
            client = null;
        }
    }
}
