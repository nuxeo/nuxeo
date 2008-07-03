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
 * $Id: Table.java 18035 2007-2008-05-01 03:34:19Z fguillaume $
 */

package org.nuxeo.ecm.core.storage.sql.db;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.hibernate.dialect.Dialect;

/**
 * A SQL table.
 *
 * @author Florent Guillaume
 */
public class Table implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private Set<String> columnNames;

    private List<Column> columns;

    private Column primaryColumn;

    /**
     * Creates a new empty table.
     *
     * @param name the table name.
     */
    public Table(String name) {
        this.name = name;
        // we use a LinkedHasMap to have deterministic ordering
        columnNames = new HashSet<String>();
        columns = new LinkedList<Column>();
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
    public void addColumn(Column column) throws IllegalArgumentException {
        String name = column.getName();
        if (columnNames.contains(name)) {
            throw new IllegalArgumentException("duplicate column " + column);
        }
        if (column.isPrimary()) {
            if (primaryColumn != null) {
                throw new IllegalArgumentException("Identity column "
                        + primaryColumn + " redefined as " + column);
            }
            primaryColumn = column;
            // put identity column first // XXX needed?
            columns.add(0, column);
        } else {
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

        buf.append(dialect.getCreateTableString());
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
                    buf.append(" DEFAULT ");
                    buf.append(defaultValue);
                }
                if (column.isNullable()) {
                    buf.append(dialect.getNullColumnString());
                } else {
                    buf.append(" NOT NULL");
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
        buf.append("DROP TABLE ");
        if (dialect.supportsIfExistsBeforeTableName()) {
            buf.append("IF EXISTS ");
        }
        buf.append(dialect.openQuote());
        buf.append(name);
        buf.append(dialect.closeQuote());
        buf.append(dialect.getCascadeConstraintsString());
        if (dialect.supportsIfExistsAfterTableName()) {
            buf.append(" IF EXISTS");
        }
        return buf.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + name + ')';
    }

}
