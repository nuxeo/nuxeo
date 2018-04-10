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
 *     tdelprat
 *     bdelbosc
 */

package org.nuxeo.elasticsearch.api;

import java.util.List;
import java.util.NoSuchElementException;

import org.elasticsearch.client.Client;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Administration interface for Elasticsearch service
 *
 * @since 5.9.3
 */
public interface ElasticSearchAdmin {

    /**
     * Retrieves the {@link Client} that can be used to access Elasticsearch API
     *
     * @since 5.9.3
     */
    ESClient getClient();

    /**
     * Initialize Elasticsearch indexes. Setup the index settings and mapping for each index that has been registered.
     *
     * @param dropIfExists if {true} remove an existing index
     * @since 5.9.3
     */
    void initIndexes(boolean dropIfExists);

    /**
     * Reinitialize an index. This will drop the existing index, recreate it with its settings and mapping, the index
     * will be empty.
     *
     * @since 7.3
     */
    void dropAndInitIndex(String indexName);

    /**
     * Reinitialize the index of a repository. This will drop the existing index, recreate it with its settings and
     * mapping, the index will be empty.
     *
     * @since 7.1
     */
    default void dropAndInitRepositoryIndex(String repositoryName) {
        dropAndInitRepositoryIndex(repositoryName, true);
    }

    /**
     * Reinitialize the index of a repository. This will drop the existing index, recreate it with its settings and
     * mapping, the index will be empty.
     * When syncAlias is false then search alias is not updated with the new index,
     * you need to explicitly call {@link #syncSearchAndWriteAlias(String)}
     *
     * @since 9.3
     */
    void dropAndInitRepositoryIndex(String repositoryName, boolean syncAlias);

    /**
     * List repository names that have Elasticsearch support.
     *
     * @since 7.1
     */
    List<String> getRepositoryNames();

    /**
     * Get the search index name associated with the repository name.
     *
     * @throws NoSuchElementException if there is no Elasticsearch index associated with the requested repository.
     * @since 7.2
     */
    String getIndexNameForRepository(String repositoryName);

    /**
     * Gets the repository name associated with the index.
     *
     * @since 9.10
     */
    String getRepositoryForIndex(String indexName);

    /**
     * Get the index names with the given type.
     *
     * @since 7.10
     */
    List<String> getIndexNamesForType(String type);

    /**
     * Get the first search index name with the given type.
     *
     * @throws NoSuchElementException if there is no Elasticsearch index with the given type.
     * @since 7.10
     */
    String getIndexNameForType(String type);

    /**
     * Returns the index to use for any write operations.
     *
     * @since 9.3
     */
    String getWriteIndexName(String searchIndexName);

    /**
     * Make sure that the search alias point to the same index as the write alias.
     *
     * @since 9.3
     */
    void syncSearchAndWriteAlias(String searchIndexName);

    /**
     * Returns true if there are indexing activities scheduled or running.
     *
     * @since 5.9.5
     */
    boolean isIndexingInProgress();

    /**
     * A {@link java.util.concurrent.Future} that accepts callback on completion when all the indexing worker are done.
     *
     * @since 7.2
     */
    ListenableFuture<Boolean> prepareWaitForIndexing();

    /**
     * Refresh all document indexes, immediately after the operation occurs, so that the updated document appears in
     * search results immediately. There is no fsync thus doesn't guarantee durability.
     *
     * @since 5.9.3
     */
    void refresh();

    /**
     * Refresh document index for the specific repository, immediately after the operation occurs, so that the updated
     * document appears in search results immediately. There is no fsync thus doesn't guarantee durability.
     *
     * @since 5.9.4
     */
    void refreshRepositoryIndex(String repositoryName);

    /**
     * Elasticsearch flush on all document indexes, triggers a lucene commit, empties the transaction log. Data is
     * flushed to disk.
     *
     * @since 5.9.3
     */
    void flush();

    /**
     * Elasticsearch flush on document index for a specific repository, triggers a lucene commit, empties the
     * transaction log. Data is flushed to disk.
     *
     * @since 5.9.4
     */
    void flushRepositoryIndex(String repositoryName);

    /**
     * Elasticsearch run {@link ElasticSearchAdmin#optimizeRepositoryIndex} on all document indexes,
     *
     * @since 7.2
     */
    void optimize();

    /**
     * Elasticsearch optimize operation allows to reduce the number of segments to one, Note that this can potentially
     * be a very heavy operation.
     *
     * @since 7.2
     */
    void optimizeRepositoryIndex(String repositoryName);

    /**
     * Elasticsearch optimize operation allows to reduce the number of segments to one, Note that this can potentially
     * be a very heavy operation.
     *
     * @since 7.3
     */
    void optimizeIndex(String indexName);

    /**
     * Returns the number of indexing worker scheduled waiting to be executed.
     *
     * @since 7.1
     */
    long getPendingWorkerCount();

    /**
     * Returns the number of indexing worker that are currently running.
     *
     * @since 7.1
     */
    long getRunningWorkerCount();

    /**
     * Returns the total number of command processed by Elasticsearch for this Nuxeo instance. Useful for test
     * assertion.
     *
     * @since 5.9.4
     */
    int getTotalCommandProcessed();

    /**
     * Returns true if the Elasticsearch is embedded with Nuxeo, sharing the same JVM.
     *
     * @since 7.2
     */
    boolean isEmbedded();

    /**
     * When true use an external version for Elasticsearch document, this enable an optimistic concurrency control
     * ensuring that an older version of a document never overwrites a newer version.
     *
     * @since 8.3
     */
    boolean useExternalVersion();

    /**
     * Returns true if Elasticsearch is enabled.
     * 
     * @since 10.2
     */
    boolean isElasticsearchEnabled();

}
