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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.DocumentException;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.storage.PartialList;
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
    Map<String, Serializable> readState(String id);

    /**
     * Reads the states of several documents.
     *
     * @param ids the document ids
     * @return the document states, an element by be {@code null} if not found
     */
    List<Map<String, Serializable>> readStates(List<String> ids);

    /**
     * Creates a document.
     *
     * @param state the document state
     * @throws DocumentException if the document already exists
     */
    void createState(Map<String, Serializable> state) throws DocumentException;

    /**
     * Updates a document.
     *
     * @param state the document state
     * @throws DocumentException if the document does not exist
     */
    void updateState(Map<String, Serializable> state) throws DocumentException;

    /**
     * Deletes a document.
     *
     * @param id the document id
     * @throws DocumentException if the document does not exist
     */
    void deleteState(String id) throws DocumentException;

    /**
     * Reads the state of a child document.
     *
     * @param parentId the parent document id
     * @param name the name of the child
     * @param ignored a set of document ids that should not be considered
     * @return the state of the child document, or {@code null} if not found
     */
    Map<String, Serializable> readChildState(String parentId, String name,
            Set<String> ignored);

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
     * @return the document states matching the query
     */
    List<Map<String, Serializable>> readKeyValuedStates(String key, String value);

    /**
     * Queries the repository for documents matching a query.
     *
     * @param evaluator the evaluator
     * @param orderBy an ORDER BY clause
     * @param limit the limit on the number of documents to return
     * @param offset the offset in the list of documents to return
     * @param deepCopy whether returned state should be a copy
     * @param ignored a set of document ids that should not be considered
     * @return
     */
    PartialList<Map<String, Serializable>> queryAndFetch(
            DBSExpressionEvaluator evaluator, OrderByClause orderBy,
            long limit, long offset, boolean deepCopy, Set<String> ignored);

}
