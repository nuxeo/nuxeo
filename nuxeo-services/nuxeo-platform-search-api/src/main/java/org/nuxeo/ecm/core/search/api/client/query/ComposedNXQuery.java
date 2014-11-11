/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: ComposedNXQuery.java 28480 2008-01-04 14:04:49Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.query;

import org.nuxeo.ecm.core.query.sql.model.SQLQuery;

/**
 * NXQL wrapper.
 * <p>
 * Includes a NXQL query along with additional information such as security
 * checks.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public interface ComposedNXQuery extends BaseQuery {

    /**
     * Returns an NXQL parsed query.
     *
     * @return Query instance from NXQuery.
     */
    SQLQuery getQuery();

    /**
     * Sets the NXQL parsed query.
     *
     * @param query the NXQL based query.
     */
    void setQuery(SQLQuery query);

    /**
     * Parses and update the query associated with this
     * <code>ComposedNXQuery</code>
     *
     * @param nxqlQueryStr an NXQL query string.
     */
    void parseAndUpdateNXQuery(String nxqlQueryStr);

}
