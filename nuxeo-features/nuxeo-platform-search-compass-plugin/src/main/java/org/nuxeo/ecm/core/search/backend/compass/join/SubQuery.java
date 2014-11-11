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

import org.nuxeo.ecm.core.query.sql.model.SQLQuery;

/**
 * A subquery is made of an nxql query that is pure in terms of involved
 * resource types, together with the joining criteria to the main
 * (document centric) query.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 *
 */
public class SubQuery {

    private final SQLQuery query;

    private String joinFieldInMain;

    private String joinFieldInSub;

    private final String resourceType;

    private final String resourceName;

    /**
     * @param query The sub query to store
     * @param joinFieldInSub The field in specified sub query to join on
     * @param joinFieldInMain The field from main query to join to
     * @param resourceType
     * @param resourceName
     */
    public SubQuery(SQLQuery query, String joinFieldInSub,
            String joinFieldInMain, String resourceType, String resourceName) {
        this.query = query;
        this.joinFieldInMain = joinFieldInMain;
        this.joinFieldInSub = joinFieldInSub;
        this.resourceType = resourceType;
        this.resourceName = resourceName;
    }

    /**
     * Constructor to use if join information is not known yet.
     *
     * @param query The sub query to store
     * @param resourceType
     * @param resourceName
     */
    public SubQuery(SQLQuery query, String resourceType, String resourceName) {
        this.query = query;
        this.resourceType = resourceType;
        this.resourceName = resourceName;
    }

    public SQLQuery getQuery() {
        return query;
    }

    public String getJoinFieldInMain() {
        return joinFieldInMain;
    }

    public String getJoinFieldInSub() {
        return joinFieldInSub;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setJoinInfo(String inMain, String inSub) {
        joinFieldInMain = inMain;
        joinFieldInSub = inSub;
    }

}
