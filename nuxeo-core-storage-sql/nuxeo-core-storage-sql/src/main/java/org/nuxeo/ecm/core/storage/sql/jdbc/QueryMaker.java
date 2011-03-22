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

package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo.SQLInfoSelect;

/**
 * A Query Maker, that can transform a query string into a SQL statement.
 * <p>
 * Must have a zero-arg constructor.
 *
 * @author Florent Guillaume
 */
public interface QueryMaker {

    /**
     * Gets the name for this query maker.
     */
    String getName();

    /**
     * Checks if this query maker accepts a given query.
     * <p>
     * Called first.
     *
     * @param query the query
     * @return {@code true} if the query is accepted
     */
    boolean accepts(String query);

    /**
     * Builds the query.
     *
     * @param sqlInfo the sql info
     * @param model the model
     * @param pathResolver the path resolver
     * @param query the query
     * @param queryFilter the query filter
     * @param params additional parameters, maker-specific
     */
    Query buildQuery(SQLInfo sqlInfo, Model model, PathResolver pathResolver,
            String query, QueryFilter queryFilter, Object... params)
            throws StorageException;

    /**
     * A SQL query that can be executed by the backend.
     */
    public static class Query {

        public SQLInfoSelect selectInfo;

        public List<Serializable> selectParams = new LinkedList<Serializable>();

    }

    public static class QueryMakerException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public QueryMakerException(String message) {
            super(message);
        }

        public QueryMakerException(String message, Throwable cause) {
            super(message, cause);
        }

        public QueryMakerException(Throwable cause) {
            super(cause);
        }
    }

    public static class QueryCannotMatchException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

}
