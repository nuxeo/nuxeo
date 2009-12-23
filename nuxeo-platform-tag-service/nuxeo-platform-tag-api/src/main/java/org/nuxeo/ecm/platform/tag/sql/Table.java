/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.platform.tag.sql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.dialect.Dialect;

/**
 * Table creation manager.
 *
 * @author mcedica
 */
public class Table implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;

    private final List<Column> columns;

    /**
     * Creates a new empty table.
     *
     * @param name the table name.
     */
    public Table(String name) {
        columns = new ArrayList<Column>();
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getQuotedName(Dialect dialect) {
        return dialect.openQuote() + name + dialect.closeQuote();
    }

    public List<Column> getColumns() {
        return columns;
    }

    public void addColumn(Column column){
            columns.add(column);
    }

    /**
     * Computes the SQL statement to create the table.
     *
     * @param dialect the dialect.
     * @return the SQL create string.
     */
    public String getCreateSql(Dialect dialect) {
        StringBuilder buf = new StringBuilder();

        buf.append("create table");
        buf.append(' ');
        buf.append(name);
        buf.append(" (");

        boolean first = true;
        for (Column column : columns) {
            if (first) {
                first = false;
            } else {
                buf.append(", ");
            }
            buf.append(column.getName());
            buf.append(' ');
            buf.append(column.getSqlTypeString(dialect));
            String defaultValue = column.getDefaultValue();
            if (defaultValue != null) {
                buf.append(" default ");
                buf.append(defaultValue);
            }
            if (column.isNullable()) {
                buf.append(dialect.getNullColumnString());
            } else {
                buf.append(" not null");
            }
        }
        buf.append(')');
        buf.append(dialect.getTableTypeString());
        return buf.toString();
    }

}
