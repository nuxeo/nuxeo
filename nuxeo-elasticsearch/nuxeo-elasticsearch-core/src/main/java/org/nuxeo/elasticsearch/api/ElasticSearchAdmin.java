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

import org.elasticsearch.client.Client;

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
     * Initialize Elasticsearch indexes.
     *
     * Setup the index settings and mapping for each index that has been
     * registered.
     *
     * @param dropIfExists
     *            if {true} remove an existing index
     *
     * @since 5.9.3
     */
    void initIndexes(boolean dropIfExists);

    /**
     * Returns the number of documents that are waiting for being indexed.
     *
     * @since 5.9.3
     */
    int getPendingDocs();

    /**
     * Returns the number of indexing command that are waiting to be processed.
     *
     * This include the recursive asynchronous activities.
     *
     * @since 5.9.3
     */
    int getPendingCommands();

    /**
     * Returns the number of indexing command that are currently running
     *
     * @since 5.9.5
     */
    int getRunningCommands();

    /**
     * Returns the total number of command processed by Elasticsearch.
     *
     * Useful for test assertion.
     *
     * @since 5.9.4
     */
    int getTotalCommandProcessed();


    /**
     * Returns true if there are indexing activities.
     *
     * This include currently running, scheduled and asynchronous recursive jobs.
     *
     * @since 5.9.5
     */
    boolean isIndexingInProgress();

    /**
     * Refresh all document indexes, immediately after the operation occurs,
     * so that the updated document appears in search results immediately.
     *
     * There is no fsync thus doesn't guarantee durability.
     *
     * @since 5.9.3
     */
    void refresh();

    /**
     * Refresh document index for the specific repository, immediately after
     * the operation occurs, so that the updated document appears in search
     * results immediately.
     *
     * There is no fsync thus doesn't guarantee durability.
     *
     * @since 5.9.4
     */
    void refreshRepositoryIndex(String repositoryName);

    /**
     * Elasticsearch flush on all document indexes, triggers a lucene commit,
     * empties the transaction log. Data is flushed to disk.
     *
     * @since 5.9.3
     */
    void flush();

    /**
     * Elasticsearch flush on document index for a specific repository,
     * triggers a lucene commit, empties the transaction log. Data is flushed
     * to disk.
     *
     * @since 5.9.4
     */
    void flushRepositoryIndex(String repositoryName);
}
