/*
 * (C) Copyright 2007-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.SQLInfo.SQLInfoSelect;

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
     * @param session the session
     * @param query the query
     * @param queryFilter the query filter
     * @param params additional parameters, maker-specific
     */
    Query buildQuery(SQLInfo sqlInfo, Model model, Session session,
            String query, QueryFilter queryFilter, Object... params)
            throws StorageException;

    /**
     * A SQL query that can be executed by the backend.
     */
    public static class Query {

        public SQLInfoSelect selectInfo;

        public List<Serializable> selectParams = new LinkedList<Serializable>();

    }
}
