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

import org.nuxeo.elasticsearch.commands.IndexingCommand;

/**
 * Interface to process indexing of documents
 *
 * @since 5.9.3
 */
public interface ElasticSearchIndexing {

    /**
     * Run a worker to process the {@link IndexingCommand}.
     * <p>
     * Asynchronous command schedules an indexing job and return.
     * </p>
     * <p>
     * Synchronous command execute an indexing job using a new Tx then refresh the index so the document is searchable
     * immediately. if the command is also recursive the children are processed asynchronously.
     * </p>
     * <p>
     * If there is more than one cmd the elasticsearch request is done in bulk mode.
     * </p>
     *
     * @since 7.1
     */
    void runIndexingWorker(List<IndexingCommand> cmds);

    /**
     * Reindex documents matching the NXQL query, This is done in an asynchronous job.
     *
     * @since 7.1
     */
    void runReindexingWorker(String repositoryName, String nxql);

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
