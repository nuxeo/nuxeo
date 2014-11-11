/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.query;

/**
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public interface Query {

    /**
     * Defines general query types.
     * <p>
     * There could be Query implementations for one or another Query Type. If
     * the query factory instantiating a specific implementation of this class
     * does not support a given Query Type than a {@code
     * UnsupportedQueryTypeException} should be thrown.
     */
    enum Type {
        NXQL("NXQL"), XPATH("XPATH");

        private final String name;

        Type(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Query#Type: " + name;
        }
    }

    /**
     * Makes a query to the backend. No fiter, permission or policy filterings
     * are done.
     *
     * @return a query result object describing the resulting documents
     * @throws QueryException
     * @see {@link FilterableQuery#execute(QueryFilter)}
     */
    QueryResult execute() throws QueryException;

}
