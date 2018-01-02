/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

/**
 * An {@code UPDATE} statement.
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

    /** Alternative to {@link #setNewValues} */
    public void setUpdatedColumns(List<Column> columns) {
        setUpdatedColumns(columns, Collections.emptySet());
    }

    /**
     * Alternative to {@link #setNewValues}
     *
     * @param columns the columns
     * @param deltas which of the columns are delta updates
     */
    public void setUpdatedColumns(List<Column> columns, Set<String> deltas) {
        List<String> updatedColumns = new LinkedList<>();
        for (Column column : columns) {
            if (column.isIdentity()) {
                // identity column is never inserted
                continue;
            }
            String col = column.getQuotedName();
            String fvs = column.getFreeVariableSetter();
            String update;
            if (deltas.contains(column.getKey())) {
                update = col + " = " + col + " + " + fvs;
            } else {
                update = col + " = " + fvs;
            }
            updatedColumns.add(update);
        }
        newValues = StringUtils.join(updatedColumns, ", ");
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
