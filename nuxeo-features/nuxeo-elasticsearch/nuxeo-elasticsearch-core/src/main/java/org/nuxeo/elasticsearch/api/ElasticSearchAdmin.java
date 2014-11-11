/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
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

import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.service.PendingClusterTask;
import org.nuxeo.ecm.core.api.DocumentModel;

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
     * {true} if a doc has already been submited for indexing.
     *
     * @since 5.9.3
     */
    boolean isAlreadyScheduledForIndexing(DocumentModel doc);

    /**
     * Returns the number of documents that are waiting for being indexed.
     *
     * @since 5.9.3
     */
    int getPendingDocs();

    /**
     * Returns the number of indexing command that are waiting to be processed.
     *
     * @since 5.9.3
     */
    int getPendingCommands();

    /**
     * Returns list of {@link PendingClusterTask} not yet processed.
     *
     * @since 5.9.3
     */
    List<PendingClusterTask> getPendingTasks();

    /**
     * Refresh document index, immediately after the operation occurs, so that
     * the updated document appears in search results immediately.
     *
     * There is no fsync thus doesn't guarantee durability.
     *
     * @since 5.9.3
     */
    void refresh();

    /**
     * Elasticsearch flush on document index, triggers a lucene commit, empties
     * the transaction log. Data is flushed to disk.
     *
     * @since 5.9.3
     */
    void flush();
}
