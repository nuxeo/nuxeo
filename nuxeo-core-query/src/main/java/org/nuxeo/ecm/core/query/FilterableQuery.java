/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.query;

/**
 * A Query that can be executed with an additional {@link QueryFilter}.
 *
 * @author Florent Guillaume
 */
public interface FilterableQuery extends Query {

    /**
     * Makes a query to the backend with filtering on the BROWSE permission for
     * the principal, facets, and query transformers.
     * <p>
     * The total number of documents can also be retrieved, it is then stored in
     * the {@link DocumentModelList} returned by
     * {@link QueryResult#getDocumentModels}.
     *
     * @param queryFilter the query filter
     * @param countTotal if {@code true}, also count the total number of
     *            documents when no limit/offset is passed
     * @return a query result object describing the resulting documents
     * @throws QueryException
     */
    QueryResult execute(QueryFilter queryFilter, boolean countTotal)
            throws QueryException;

    /**
     * Makes a query to the backend with filtering on the BROWSE permission for
     * the principal, facets, and query transformers.
     * <p>
     * The total number of documents can also be retrieved, it is then stored in
     * the {@link DocumentModelList} returned by
     * {@link QueryResult#getDocumentModels}.
     *
     * @param queryFilter the query filter
     * @param countUpTo if {@code -1}, also count the total number of documents
     *            when no limit/offset is passed.<br>
     *            If {@code 0}, don't count the total number.<br>
     *            If {@code n}, count the total number if there are less than n
     *            documents otherwise set the size to {@code -1}.
     * @return a query result object describing the resulting documents
     * @throws QueryException
     *
     * @since 5.6
     */
    QueryResult execute(QueryFilter queryFilter, long countUpTo)
            throws QueryException;

}
