/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.ecm.core.query.QueryFilter;
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
    Query buildQuery(SQLInfo sqlInfo, Model model, PathResolver pathResolver, String query, QueryFilter queryFilter,
            Object... params);

    /**
     * A SQL query that can be executed by the backend.
     */
    public static class Query {

        public SQLInfoSelect selectInfo;

        public List<Serializable> selectParams = new LinkedList<>();

    }

    public static class QueryCannotMatchException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

}
