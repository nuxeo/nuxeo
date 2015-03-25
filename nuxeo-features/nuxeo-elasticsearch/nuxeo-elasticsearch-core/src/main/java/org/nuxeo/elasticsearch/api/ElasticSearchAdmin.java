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
 *     tdelprat
 *     bdelbosc
 */

package org.nuxeo.elasticsearch.api;

import java.util.List;

import com.google.common.util.concurrent.ListenableFuture;
import org.elasticsearch.client.Client;
import org.nuxeo.elasticsearch.core.IndexingMonitor;

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
    Client getClient();

    /**
     * Initialize Elasticsearch indexes. Setup the index settings and mapping for each index that has been registered.
     *
     * @param dropIfExists if {true} remove an existing index
     * @since 5.9.3
     */
    void initIndexes(boolean dropIfExists);

    /**
     * Reinitialize the index of a repository. This will drop the existing index, recreate it with its settings and
     * mapping, the index will be empty.
     *
     * @since 7.1
     */
    void dropAndInitRepositoryIndex(String repositoryName);

    /**
     * List repository names that have Elasticsearch support.
     *
     * @since 7.1
     */
    List<String> getRepositoryNames();

    /**
     * Get the index name associated with the repository name.

     * throws NoSuchElementException if there is no Elasticsearch index associated with the requested repository.
     *
     * @since 7.2
     */
    String getIndexNameForRepository(String repositoryName);

    /**
     * Returns true if there are indexing activities scheduled or running.
     *
     * @since 5.9.5
     */
    boolean isIndexingInProgress();

    /**
     * A {@link java.util.concurrent.Future} that accepts callback on completion when all the indexing
     * worker are done.
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
     * Returns the number of command scheduled for indexing.
     *
     * @since 7.1
     */
    int getPendingCommandCount();

    /**
     * Returns the number of indexing worker pending waiting to be executed.
     *
     * @since 7.1
     */
    int getPendingWorkerCount();

    /**
     * Returns the number of indexing worker that are currently running.
     *
     * @since 7.1
     */
    int getRunningWorkerCount();

    /**
     * Returns the total number of command processed by Elasticsearch. Useful for test assertion.
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
     * Return the indexing job synchronizer, for internal use only.
     *
     * @since 7.3
     */
    IndexingMonitor getIndexingMonitor();

}
