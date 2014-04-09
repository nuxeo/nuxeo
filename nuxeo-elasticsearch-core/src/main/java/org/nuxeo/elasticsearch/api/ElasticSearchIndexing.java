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

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.elasticsearch.commands.IndexingCommand;

/**
 * Interface to process indexing of documents
 *
 * @since 5.9.3
 */
public interface ElasticSearchIndexing {

    /**
     * Ask to process the {@link IndexingCommand}.
     *
     * @since 5.9.3
     */
    void indexNow(IndexingCommand cmd) throws ClientException;

    /**
     * Ask to process a list of {@link IndexingCommand}.
     *
     * @since 5.9.3
     */
    void indexNow(List<IndexingCommand> cmds) throws ClientException;

    /**
     * Schedule indexing command and return.
     *
     * @since 5.9.3
     */
    void scheduleIndexing(IndexingCommand cmd) throws ClientException;

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
