/*
 * (C) Copyright 2007-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: Select.java 18033 2007-2008-05-01 01:34:45Z fguillaume $
 */

package org.nuxeo.ecm.core.storage.sql.db;

import java.io.Serializable;

import org.nuxeo.common.utils.StringUtils;

/**
 * An {@code UPDATE} statement.
 *
 * @author Florent Guillaume
 */
public class Update implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final Table table;

    protected String newValues;

    protected String[] from;

    protected String where;

    public Update(Table table) {
        this.table = table;
    }

    public Table getTable() {
        return table;
    }

    public void setNewValues(String newValues) {
        this.newValues = newValues;
    }

    /**
     * Sets additional table names with which to join for this update.
     */
    public void setFrom(String... from) {
        this.from = from;
    }

    public void setWhere(String where) {
        if (where == null || where.length() == 0) {
            throw new IllegalArgumentException("unexpected empty WHERE");
        }
        this.where = where;
    }

    public String getStatement() {
        StringBuilder buf = new StringBuilder(128);
        buf.append("UPDATE ");
        buf.append(table.getQuotedName());
        buf.append(" SET ");
        buf.append(newValues);
        if (from != null) {
            buf.append(" FROM ");
            if (table.getDialect().doesUpdateFromRepeatSelf()) {
                buf.append(table.getQuotedName());
                buf.append(", ");
            }
            buf.append(StringUtils.join(from, ", "));
        }
        if (where != null) {
            buf.append(" WHERE ");
            buf.append(where);
        } else {
            throw new IllegalArgumentException("unexpected empty WHERE");
        }
        return buf.toString();
    }
}
