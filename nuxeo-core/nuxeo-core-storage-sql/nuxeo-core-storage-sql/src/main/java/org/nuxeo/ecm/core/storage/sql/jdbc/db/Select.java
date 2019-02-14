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

package org.nuxeo.ecm.core.storage.sql.jdbc.db;

import java.io.Serializable;

/**
 * A {@code SELECT} statement.
 *
 * @author Florent Guillaume
 */
public class Select implements Serializable {

    private static final long serialVersionUID = 1L;

    private String with;

    private String what;

    private String from;

    private String where;

    private String groupBy;

    private String orderBy;

    public Select(Table table) {
        // table unused
    }

    public void setWith(String with) {
        this.with = with;
    }

    public void setWhat(String what) {
        this.what = what;
    }

    public String getWhat() {
        return what;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFrom() {
        return from;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public String getWhere() {
        return where;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getStatement() {
        StringBuilder sb = new StringBuilder(128);
        if (with != null && with.length() != 0) {
            sb.append("WITH ");
            sb.append(with);
            sb.append(' ');
        }
        sb.append("SELECT ");
        sb.append(what);
        sb.append(" FROM ");
        sb.append(from);
        if (where != null && where.length() != 0) {
            sb.append(" WHERE ");
            sb.append(where);
        }
        if (groupBy != null && groupBy.length() != 0) {
            sb.append(" GROUP BY ");
            sb.append(groupBy);
        }
        if (orderBy != null && orderBy.length() != 0) {
            sb.append(" ORDER BY ");
            sb.append(orderBy);
        }
        // ... "for update" in some cases, see dialect.getForUpdateString and
        // lock modes
        return sb.toString();
    }

}
