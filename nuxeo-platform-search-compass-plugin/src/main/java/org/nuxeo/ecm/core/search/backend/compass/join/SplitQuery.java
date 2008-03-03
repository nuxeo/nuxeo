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
 * $Id$
 */

package org.nuxeo.ecm.core.search.backend.compass.join;

import java.util.List;

import org.nuxeo.ecm.core.query.sql.model.SQLQuery;

/**
 * A splitted query holds a main, document centric NXQL query together
 * with a bunch of {@link SubQuery} objects.
 * <p>
 * This is good enough for star-like graphs of joins.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public class SplitQuery {

    private final SQLQuery mainQuery;

    private final List<SubQuery> subQueries;

    /**
     * @param mainQuery The main (document-centric) query
     * @param subQueries The sub queries. Can be null or empty in case
     *        the represented full query isn't a join query.
     */
    public SplitQuery(SQLQuery mainQuery, List<SubQuery> subQueries) {
        this.mainQuery = mainQuery;
        this.subQueries = subQueries;
    }

    public SQLQuery getMainQuery() {
        return mainQuery;
    }

    public List<SubQuery> getSubQueries() {
        return subQueries;
    }

    public boolean isJoinQuery() {
        return subQueries != null && !subQueries.isEmpty();
    }

}
