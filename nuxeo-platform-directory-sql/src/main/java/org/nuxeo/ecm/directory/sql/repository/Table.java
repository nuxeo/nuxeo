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
 * $Id: Table.java 18035 2007-05-01 03:34:19Z fguillaume $
 */

package org.nuxeo.ecm.directory.sql.repository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.dialect.Dialect;

/**
 * A SQL table.
 *
 * @author Florent Guillaume
 */
public class Table implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;

    private final Map<String, Column> columnMap;

    private final List<Column> columns;

    private Column primaryColumn;

    /**
     * Creates a new empty table.
     *
     * @param name the table name.
     */
    public Table(String name) {
        // we use a LinkedHasMap to have deterministic ordering
        columnMap = new HashMap<String, Column>();
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

    /**
     * Adds a {@link Column} to the table.
     *
     * @param column the column
     */
    public void addColumn(Column column) throws ConfigurationException {
        if (column == null) {
            throw new IllegalArgumentException(
                    "addColumn: column cannot be null");
        }
        String name = column.getName();
        if (columnMap.keySet().contains(name)) {
            throw new ConfigurationException("duplicate column " + column);
        }
        if (column.isPrimary()) {
            if (primaryColumn != null) {
                throw new ConfigurationException("Identity column "
                        + primaryColumn + " redefined as " + column);
            }
            primaryColumn = column;
            // put identity column first // XXX needed?
            columns.add(0, column);
            columnMap.put(name, column);
        } else {
            columnMap.put(name, column);
            columns.add(column);
        }
    }

    /**
     * Computes the SQL statement to create the table.
     *
     * @param dialect the dialect.
     * @return the SQL create string.
     */
    public String getCreateSql(Dialect dialect) {
        StringBuilder buf = new StringBuilder();
        char openQuote = dialect.openQuote();
        char closeQuote = dialect.closeQuote();

        buf.append("create table");
        buf.append(' ');
        buf.append(openQuote);
        buf.append(name);
        buf.append(closeQuote);
        buf.append(" (");

        boolean first = true;
        for (Column column : columns) {
            if (first) {
                first = false;
            } else {
                buf.append(", ");
            }
            buf.append(openQuote);
            buf.append(column.getName());
            buf.append(closeQuote);
            buf.append(' ');
            if (column.isIdentity()) {
                if (dialect.hasDataTypeInIdentityColumn()) {
                    buf.append(column.getSqlTypeString(dialect));
                    buf.append(' ');
                }
                buf.append(dialect.getIdentityColumnString(column.getSqlType()));
            } else {
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
            // unique
            // check
        }
        // unique
        // check
        buf.append(')');
        buf.append(dialect.getTableTypeString());
        return buf.toString();
    }

    /**
     * Computes the SQL statement to drop the table.
     *
     * @param dialect the dialect.
     * @return the SQL drop string.
     */
    public String getDropSql(Dialect dialect) {
        StringBuilder buf = new StringBuilder();
        buf.append("drop table ");
        if (dialect.supportsIfExistsBeforeTableName()) {
            buf.append("if exists ");
        }
        buf.append(dialect.openQuote());
        buf.append(name);
        buf.append(dialect.closeQuote());
        buf.append(dialect.getCascadeConstraintsString());
        if (dialect.supportsIfExistsAfterTableName()) {
            buf.append(" if exists");
        }
        return buf.toString();
    }

    public Column getColumn(String name) {
        if (name == null) {
            throw new IllegalArgumentException("getColumn: name cannot be null");
        }

        return columnMap.get(name);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name + ')';
    }

    public Column getPrimaryColumn() {
        return primaryColumn;
    }

}
