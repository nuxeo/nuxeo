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
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.PropertyType;

/**
 * A SQL table.
 *
 * @author Florent Guillaume
 */
public class Table implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final Database database;

    protected final Dialect dialect;

    protected final String physicalName;

    /** Map of logical names to columns. */
    private final LinkedHashMap<String, Column> columns;

    /** Logical names of indexed columns. */
    private List<String[]> indexedColumns;

    /**
     * Creates a new empty table.
     *
     * @param physicalName the table name
     */
    public Table(Database database, String physicalName) {
        this.database = database;
        this.dialect = database.dialect;
        this.physicalName = physicalName;
        // we use a LinkedHashMap to have deterministic ordering
        columns = new LinkedHashMap<String, Column>();
        indexedColumns = new LinkedList<String[]>();
    }

    public String getPhysicalName() {
        return physicalName;
    }

    public String getQuotedName() {
        return dialect.openQuote() + physicalName + dialect.closeQuote();
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
    public Column addColumn(String name, PropertyType type, int sqlType,
            String key, Model model) throws IllegalArgumentException {
        String physicalName = database.getColumnPhysicalName(name);
        if (columns.containsKey(physicalName)) {
            throw new IllegalArgumentException("duplicate column " +
                    physicalName);
        }
        Column column = new Column(this, physicalName, type, sqlType, key,
                model);
        columns.put(name, column);
        return column;
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
     * @return the SQL create string.
     */
    public String getCreateSql() {
        StringBuilder buf = new StringBuilder();

        buf.append("CREATE TABLE ");
        buf.append(getQuotedName());
        buf.append(" (");

        boolean first = true;
        for (Column column : columns.values()) {
            if (first) {
                first = false;
            } else {
                buf.append(", ");
            }
            buf.append(column.getQuotedName());
            buf.append(' ');
            if (column.isIdentity()) {
                if (dialect.hasDataTypeInIdentityColumn()) {
                    buf.append(column.getSqlTypeString());
                    buf.append(' ');
                }
                buf.append(dialect.getIdentityColumnString(column.getSqlType()));
            } else {
                buf.append(column.getSqlTypeString());
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
     * @return the SQL strings
     */
    public List<String> getPostCreateSqls() {
        List<String> sqls = new LinkedList<String>();
        for (Column column : columns.values()) {
            if (column.isPrimary()) {
                StringBuilder buf = new StringBuilder();
                String constraintName = dialect.openQuote() + physicalName +
                        (dialect.storesUpperCaseIdentifiers() ? "_PK" : "_pk") +
                        dialect.closeQuote();
                buf.append("ALTER TABLE ");
                buf.append(getQuotedName());
                String s = dialect.getAddPrimaryKeyConstraintString(constraintName);
                // cosmetic
                s = s.replace(" add constraint ", " ADD CONSTRAINT ");
                s = s.replace(" primary key ", " PRIMARY KEY ");
                buf.append(s);
                buf.append('(');
                buf.append(column.getQuotedName());
                buf.append(')');
                sqls.add(buf.toString());
            }
            Table ft = column.getForeignTable();
            if (ft != null) {
                Column fc = ft.getColumn(column.getForeignKey());
                String constraintName = dialect.openQuote() + physicalName +
                        "_" + column.getPhysicalName() + "_" +
                        ft.getPhysicalName() +
                        (dialect.storesUpperCaseIdentifiers() ? "_FK" : "_fk") +
                        dialect.closeQuote();
                StringBuilder buf = new StringBuilder();
                buf.append("ALTER TABLE ");
                buf.append(getQuotedName());
                String s = dialect.getAddForeignKeyConstraintString(
                        constraintName,
                        new String[] { column.getQuotedName() },
                        ft.getQuotedName(),
                        new String[] { fc.getQuotedName() }, true);
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
            List<String> qcols = new LinkedList<String>();
            List<String> pcols = new LinkedList<String>();
            for (String name : columnNames) {
                Column col = getColumn(name);
                qcols.add(col.getQuotedName());
                pcols.add(col.getPhysicalName());
            }
            String indexName = dialect.openQuote() +
                    (dialect.qualifyIndexName() ? physicalName + "_" : "") +
                    StringUtils.join(pcols, '_') +
                    (dialect.storesUpperCaseIdentifiers() ? "_IDX" : "_idx") +
                    dialect.closeQuote();
            StringBuilder buf = new StringBuilder();
            buf.append("CREATE");
            // buf.append(unique ? " UNIQUE" : "");
            buf.append(" INDEX ");
            buf.append(indexName);
            buf.append(" ON ");
            buf.append(getQuotedName());
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
     * @return the SQL drop string.
     */
    public String getDropSql() {
        StringBuilder buf = new StringBuilder();
        buf.append("DROP TABLE ");
        if (dialect.supportsIfExistsBeforeTableName()) {
            buf.append("IF EXISTS ");
        }
        buf.append(getQuotedName());
        buf.append(dialect.getCascadeConstraintsString());
        if (dialect.supportsIfExistsAfterTableName()) {
            buf.append(" IF EXISTS");
        }
        return buf.toString();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + physicalName + ')';
    }

}
