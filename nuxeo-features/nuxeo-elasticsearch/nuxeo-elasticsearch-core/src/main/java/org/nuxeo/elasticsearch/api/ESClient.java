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
package org.nuxeo.elasticsearch.api;

import org.elasticsearch.action.bulk.BulkProcessor;
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
import org.elasticsearch.cluster.health.ClusterHealthStatus;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;

/**
 * @since 9.3
 */
public interface ESClient extends AutoCloseable {

    // -------------------------------------------------------------------
    // Admin
    //
    boolean waitForYellowStatus(String[] indexNames, int timeoutSecond);

    ClusterHealthStatus getHealthStatus(String[] indexNames);

    void refresh(String indexName);

    void flush(String indexName);

    void optimize(String indexName);

    boolean indexExists(String indexName);

    boolean mappingExists(String indexName, String type);

    void deleteIndex(String indexName, int timeoutSecond);

    void createIndex(String indexName, String jsonSettings);

    void createMapping(String indexName, String type, String jsonMapping);

    String getNodesInfo();

    String getNodesStats();

    boolean aliasExists(String aliasName);

    /**
     * Returns the name of the index referenced by the alias.
     * Returns null if the alias does not exists.
     */
    String getFirstIndexForAlias(String aliasName);

    void updateAlias(String aliasName, String indexName);

    // -------------------------------------------------------------------
    // Search
    //

    BulkResponse bulk(BulkRequest request);

    DeleteResponse delete(DeleteRequest request);

    SearchResponse search(SearchRequest request);

    SearchResponse searchScroll(SearchScrollRequest request);

    GetResponse get(GetRequest request);

    /**
     * Performs the indexing request.
     *
     * @throws ConcurrentUpdateException if a more recent version of the document exits.
     */
    IndexResponse index(IndexRequest request);

    ClearScrollResponse clearScroll(ClearScrollRequest request);

    /**
     * Creates an elasticsearch BulkProcessor builder.
     *
     * @since 10.3
     */
    BulkProcessor.Builder bulkProcessorBuilder(BulkProcessor.Listener listener);
}
