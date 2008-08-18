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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.common.utils.StringUtils;

/**
 * A SQL table.
 *
 * @author Florent Guillaume
 */
public class Table implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String name;

    private final LinkedHashMap<String, Column> columns;

    private Column primaryColumn;

    private List<String[]> indexedColumns;

    /**
     * Creates a new empty table.
     *
     * @param name the table name.
     */
    public Table(String name) {
        this.name = name;
        // we use a LinkedHashMap to have deterministic ordering
        columns = new LinkedHashMap<String, Column>();
        indexedColumns = new LinkedList<String[]>();
    }

    public String getName() {
        return name;
    }

    public String getQuotedName(Dialect dialect) {
        return dialect.openQuote() + name + dialect.closeQuote();
    }

    public Column getColumn(String name) {
        return columns.get(name);
    }

    public Collection<Column> getColumns() {
        return columns.values();
    }

    /**
     * Adds a {@link Column} to the table.
     *
     * @param column the column
     */
    public void addColumn(Column column) throws IllegalArgumentException {
        String name = column.getName();
        if (columns.containsKey(name)) {
            throw new IllegalArgumentException("duplicate column " + column);
        }
        if (column.isPrimary()) {
            if (primaryColumn != null) {
                throw new IllegalArgumentException("Identity column " +
                        primaryColumn + " redefined as " + column);
            }
            primaryColumn = column;
        }
        columns.put(name, column);
    }

    /**
     * Adds an index on one or several columns.
     *
     * @param columnNames the column names
     */
    public void addIndex(String... columnNames) {
        indexedColumns.add(columnNames);
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

        buf.append("CREATE TABLE ");
        buf.append(openQuote);
        buf.append(name);
        buf.append(closeQuote);
        buf.append(" (");

        boolean first = true;
        for (Column column : columns.values()) {
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
     * Computes the SQL statement to finish creating the table, usually some
     * ALTER TABLE statements to add constraints.
     *
     * @param dialect the dialect
     * @return the SQL strings
     */
    public List<String> getPostCreateSqls(Dialect dialect) {
        List<String> sqls = new LinkedList<String>();
        for (Column column : columns.values()) {
            if (column.isPrimary()) {
                StringBuilder buf = new StringBuilder();
                String constraintName = dialect.openQuote() + name + "_pk" +
                        dialect.closeQuote();
                buf.append("ALTER TABLE ");
                buf.append(getQuotedName(dialect));
                String s = dialect.getAddPrimaryKeyConstraintString(constraintName);
                // cosmetic
                s = s.replace(" add constraint ", " ADD CONSTRAINT ");
                s = s.replace(" primary key ", " PRIMARY KEY ");
                buf.append(s);
                buf.append('(');
                buf.append(column.getQuotedName(dialect));
                buf.append(')');
                sqls.add(buf.toString());
            }
            Table ft = column.getForeignTable();
            if (ft != null) {
                Column fc = ft.getColumn(column.getForeignKey());
                String constraintName = dialect.openQuote() + name + "_" +
                        column.getName() + "_" + ft.getName() + "_fk" +
                        dialect.closeQuote();
                StringBuilder buf = new StringBuilder();
                buf.append("ALTER TABLE ");
                buf.append(getQuotedName(dialect));
                String s = dialect.getAddForeignKeyConstraintString(
                        constraintName,
                        new String[] { column.getQuotedName(dialect) },
                        ft.getQuotedName(dialect),
                        new String[] { fc.getQuotedName(dialect) }, true);
                // cosmetic
                s = s.replace(" add constraint ", " ADD CONSTRAINT ");
                s = s.replace(" foreign key ", " FOREIGN KEY ");
                s = s.replace(" references ", " REFERENCES ");
                buf.append(s);
                // if (dialect.supportsCascadeDelete())
                buf.append(" ON DELETE CASCADE");
                sqls.add(buf.toString());
            }
        }
        for (String[] columnNames : indexedColumns) {
            String indexName = dialect.openQuote() +
                    (dialect.qualifyIndexName() ? name + "_" : "") +
                    StringUtils.join(columnNames, '_') + "_idx" +
                    dialect.closeQuote();
            List<String> qcols = new LinkedList<String>();
            for (String name : columnNames) {
                qcols.add(getColumn(name).getQuotedName(dialect));
            }
            StringBuilder buf = new StringBuilder();
            buf.append("CREATE");
            // buf.append(unique ? " UNIQUE" : "");
            buf.append(" INDEX ");
            buf.append(indexName);
            buf.append(" ON ");
            buf.append(getQuotedName(dialect));
            buf.append(" (");
            buf.append(StringUtils.join(qcols, ", "));
            buf.append(')');
            sqls.add(buf.toString());
        }
        return sqls;
    }

    /**
     * Computes the SQL statement to drop the table.
     * <p>
     * TODO drop constraints and indexes
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
