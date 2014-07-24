/*
 * Copyright (c) 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.dbs;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.sql.model.Expression;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.binary.BinaryManager;

/**
 * Interface for a {@link Repository} for Document-Based Storage.
 *
 * @since 5.9.4
 */
public interface DBSRepository extends Repository {

    /**
     * Gets the binary manager.
     *
     * @return the binary manager.
     */
    BinaryManager getBinaryManager();

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
     * @throws DocumentException if the document already exists
     */
    void createState(State state) throws DocumentException;

    /**
     * Updates a document.
     *
     * @param id the document id
     * @param diff the diff to apply
     * @throws DocumentException if the document does not exist
     */
    void updateState(String id, StateDiff diff) throws DocumentException;

    /**
     * Deletes a set of document.
     *
     * @param ids the document ids
     */
    void deleteStates(Set<String> ids) throws DocumentException;

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
    List<State> queryKeyValue(String key, String value, Set<String> ignored);

    /**
     * Queries the repository for document ids having value in key (an array).
     *
     * @param key the key
     * @param value the value
     * @param ids the set which receives the documents ids
     * @param proxyTargets returns a map of proxy to target among the documents
     *            found
     * @param targetProxies returns a map of target to proxies among the
     *            document found
     */
    void queryKeyValueArray(String key, Object value, Set<String> ids,
            Map<String, String> proxyTargets,
            Map<String, Object[]> targetProxies);

    /**
     * Queries the repository to check if there are documents having key =
     * value.
     *
     * @param key the key
     * @param value the value
     * @param ignored a set of document ids that should not be considered
     * @return {@code true} if the query matches at least one document,
     *         {@code false} if the query matches nothing
     */
    boolean queryKeyValuePresence(String key, String value, Set<String> ignored);

    /**
     * Queries the repository for documents matching a query.
     *
     * @param expression the query expression
     * @param evaluator the map-based evaluator for the query
     * @param orderByClause an ORDER BY clause
     * @param limit the limit on the number of documents to return
     * @param offset the offset in the list of documents to return
     * @param countUpTo if {@code -1}, count the total size without
     *            offset/limit.<br>
     *            If {@code 0}, don't count the total size, set it to {@code -1}
     *            .<br>
     *            If {@code n}, count the total number if there are less than n
     *            documents otherwise set the total size to {@code -2}.
     * @param deepCopy whether returned state should be a copy
     * @return a partial list containing the limited documents required, and the
     *         total size according to countUpTo
     */
    PartialList<State> queryAndFetch(Expression expression,
            DBSExpressionEvaluator evaluator, OrderByClause orderByClause,
            int limit, int offset, int countUpTo, boolean deepCopy);

}
