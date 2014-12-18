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

import org.nuxeo.elasticsearch.commands.IndexingCommand;

/**
 * Interface to process indexing of documents
 *
 * @since 5.9.3
 */
public interface ElasticSearchIndexing {

    /**
     * Run a worker to process the {@link IndexingCommand}, the target document must be committed.
     * <p>
     * Asynchronous command schedules an indexing job and return.
     * </p>
     * <p>
     * Synchronous command execute an indexing job using a new Tx then refresh the index so the document is searchable
     * immediately. if the command is also recursive the children are processed asynchronously.
     * </p>
     *
     * @since 7.1
     */
    void runIndexingWorker(IndexingCommand cmd);

    /**
     * Same as {@link ElasticSearchIndexing#runIndexingWorker(org.nuxeo.elasticsearch.commands.IndexingCommand)} but use
     * a bulk request to process a list of commands.
     *
     * @since 7.1
     */
    void runIndexingWorker(List<IndexingCommand> cmds);

    /**
     * Reindex documents matching the NXQL query,
     *
     * @since 7.1
     */
    void runReindexingWorker(String repositoryName, String nxql);

    /**
     * {true} if a command has already been submitted for indexing.
     *
     * @since 5.9.5
     */
    boolean isAlreadyScheduled(IndexingCommand cmd);

    /**
     * Process the {@link IndexingCommand}.
     * <p>
     * Send indexing command to Elasticsearch, if the command is synchronous the index is refreshed so the document is
     * searchable immediately. Recursive indexing is not taken in account except for deletion. This is not a
     * transactional operation, a rollback will not discard the executed commands.
     * </p>
     *
     * @since 7.1
     */
    void indexNonRecursive(IndexingCommand cmd);

    /**
     * Same as {@link ElasticSearchIndexing#indexNonRecursive(org.nuxeo.elasticsearch.commands.IndexingCommand)} but
     * process the list command using a bulk request.</p>
     *
     * @since 7.1
     */
    void indexNonRecursive(List<IndexingCommand> cmds);

}
