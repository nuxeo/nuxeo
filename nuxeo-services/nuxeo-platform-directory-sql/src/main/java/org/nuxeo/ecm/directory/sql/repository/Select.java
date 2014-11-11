/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id: Select.java 18033 2007-05-01 01:34:45Z fguillaume $
 */

package org.nuxeo.ecm.directory.sql.repository;

import java.io.Serializable;

import org.hibernate.dialect.Dialect;

/**
 * A {@code SELECT} statement.
 *
 * @author Florent Guillaume
 */
public class Select implements Serializable {

    private static final long serialVersionUID = 1L;

    private String what;

    private String from;

    private String where;

    private String groupBy;

    private String orderBy;

    public Select(Dialect dialect) {
        // dialect unused
    }

    public void setWhat(String what) {
        this.what = what;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public void setGroupBy(String groupBy) {
        this.groupBy = groupBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getStatement() {
        StringBuilder buf = new StringBuilder(128);
        buf.append("select ");
        buf.append(what);
        buf.append(" from ");
        buf.append(from);
        if (where != null && where.length() != 0) {
            buf.append(" where ");
            buf.append(where);
        }
        if (groupBy != null && groupBy.length() != 0) {
            buf.append(" group by ");
            buf.append(groupBy);
        }
        if (orderBy != null && orderBy.length() != 0) {
            buf.append(" order by ");
            buf.append(orderBy);
        }
        // ... "for update" in some cases, see dialect.getForUpdateString and lock modes
        return buf.toString();
    }

}
