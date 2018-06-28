/*
 * (C) Copyright 2014-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.dbs;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.storage.FulltextConfiguration;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.StateDiff;

/**
 * Interface for a {@link Repository} for Document-Based Storage.
 *
 * @since 5.9.4
 */
public interface DBSRepository extends Repository, LockManager {

    /**
     * Gets the blob manager.
     *
     * @return the blob manager.
     */
    BlobManager getBlobManager();

    /**
     * Gets the fulltext configuration.
     *
     * @return the fulltext configuration
     * @since 7.10-HF04, 8.1
     */
    FulltextConfiguration getFulltextConfiguration();

    /**
     * Checks if fulltext indexing (and search) is disabled.
     *
     * @return {@code true} if fulltext indexing is disabled, {@code false} if it is enabled
     * @since 7.1, 6.0-HF02
     */
    boolean isFulltextDisabled();

    /**
     * Checks if fulltext search is disabled.
     *
     * @return {@code true} if fulltext search is disabled, {@code false} if it is enabled
     * @since 10.2
     */
    boolean isFulltextSearchDisabled();

    /**
     * Gets the root id.
     *
     * @return the root id.
     */
    String getRootId();

    /**
     * Generates a new id for a document.
     *
     * @return the new id
     */
    String generateNewId();

    /**
     * Reads the state of a document.
     *
     * @param id the document id
     * @return the document state, or {@code null} if not found
     */
    State readState(String id);

    /**
     * Reads the partial state of a document.
     *
     * @param id the document id
     * @param keys the keys to read
     * @return the document partial state, or {@code null} if not found
     * @since 9.10
     */
    default State readPartialState(String id, Collection<String> keys) {
        // overrides should optimize to only return the required keys and nothing more
        return readState(id);
    }

    /**
     * Reads the states of several documents.
     * <p>
     * The returned states may be in a different order than the ids.
     *
     * @param ids the document ids
     * @return the document states, an element by be {@code null} if not found
     */
    List<State> readStates(List<String> ids);

    /**
     * Creates a document.
     *
     * @param state the document state
     */
    void createState(State state);

    /**
     * Creates documents.
     *
     * @param states the document states
     */
    default void createStates(List<State> states) {
        states.forEach(this::createState);
    }

    /**
     * Updates a document.
     *
     * @param id the document id
     * @param diff the diff to apply
     */
    void updateState(String id, StateDiff diff);

    /**
     * Deletes a set of document.
     *
     * @param ids the document ids
     */
    void deleteStates(Set<String> ids);

    /**
     * Reads the state of a child document.
     *
     * @param parentId the parent document id
     * @param name the name of the child
     * @param ignored a set of document ids that should not be considered
     * @return the state of the child document, or {@code null} if not found
     */
    State readChildState(String parentId, String name, Set<String> ignored);

    /**
     * Checks if a document has a child with the given name
     *
     * @param parentId the parent document id
     * @param name the name of the child
     * @param ignored a set of document ids that should not be considered
     * @return {@code true} if the child exists, {@code false} if not
     */
    boolean hasChild(String parentId, String name, Set<String> ignored);

    /**
     * Queries the repository for documents having key = value.
     *
     * @param key the key
     * @param value the value
     * @param ignored a set of document ids that should not be considered
     * @return the document states matching the query
     */
    List<State> queryKeyValue(String key, Object value, Set<String> ignored);

    /**
     * Queries the repository for documents having key1 = value1 and key2 = value2.
     *
     * @param key1 the first key
     * @param value1 the first value
     * @param key2 the second key
     * @param value2 the second value
     * @param ignored a set of document ids that should not be considered
     * @return the document states matching the query
     */
    List<State> queryKeyValue(String key1, Object value1, String key2, Object value2, Set<String> ignored);

    /**
     * Queries the repository for document ids having value in key (an array).
     *
     * @param key the key
     * @param value the value
     * @param ids the set which receives the documents ids
     * @param proxyTargets returns a map of proxy to target among the documents found
     * @param targetProxies returns a map of target to proxies among the document found
     */
    void queryKeyValueArray(String key, Object value, Set<String> ids, Map<String, String> proxyTargets,
            Map<String, Object[]> targetProxies);

    /**
     * Queries the repository for document ids having value in key (an array).
     *
     * @param key the key
     * @param value the value
     * @param ids the set which receives the documents ids
     * @param proxyTargets returns a map of proxy to target among the documents found
     * @param targetProxies returns a map of target to proxies among the document found
     * @param limit the maximum number of ids to return
     * @since 9.10
     */
    default void queryKeyValueArray(String key, Object value, Set<String> ids, Map<String, String> proxyTargets,
            Map<String, Object[]> targetProxies, int limit) {
        // limit unused by default, override for a more efficient implementation
        queryKeyValueArray(key, value, ids, proxyTargets, targetProxies);
    }

    /**
     * Queries the repository to check if there are documents having key = value.
     *
     * @param key the key
     * @param value the value
     * @param ignored a set of document ids that should not be considered
     * @return {@code true} if the query matches at least one document, {@code false} if the query matches nothing
     */
    boolean queryKeyValuePresence(String key, String value, Set<String> ignored);

    /**
     * Queries the repository for documents matching a NXQL query, and returns a projection of the documents.
     *
     * @param evaluator the map-based evaluator for the query
     * @param orderByClause an ORDER BY clause
     * @param distinctDocuments {@code true} if the projection should return a maximum of one row per document
     * @param limit the limit on the number of documents to return
     * @param offset the offset in the list of documents to return
     * @param countUpTo if {@code -1}, count the total size without offset/limit.<br>
     *            If {@code 0}, don't count the total size, set it to {@code -1} .<br>
     *            If {@code n}, count the total number if there are less than n documents otherwise set the total size
     *            to {@code -2}.
     * @return a partial list of maps containing the NXQL projections requested, and the total size according to
     *         countUpTo
     */
    PartialList<Map<String, Serializable>> queryAndFetch(DBSExpressionEvaluator evaluator, OrderByClause orderByClause,
            boolean distinctDocuments, int limit, int offset, int countUpTo);

    /**
     * Gets the lock manager for this repository.
     *
     * @return the lock manager
     * @since 7.4
     */
    LockManager getLockManager();

    /**
     * Executes the given query and returns the first batch of results, next batch must be requested
     * within the {@code keepAliveSeconds} delay.
     *
     * @since 8.4
     */
    ScrollResult scroll(DBSExpressionEvaluator evaluator, int batchSize, int keepAliveSeconds);

    /**
     * Get the next batch of result, the {@code scrollId} is part of the previous {@link ScrollResult} response.
     *
     * @since 8.4
     */
    ScrollResult scroll(String scrollId);

    /**
     * Called when created a transaction.
     *
     * @since 8.10
     */
    default void begin() {

    }

    /**
     * Saves and flushes to database.
     *
     * @since 8.10
     */
    default void commit() {

    }

    /**
     * Rolls back the save state by applying the undo log.
     *
     * @since 8.10
     */
    default void rollback() {

    }

}
