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

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.PropertyType;
import org.nuxeo.ecm.core.storage.sql.db.dialect.Dialect;

/**
 * The basic implementation of a SQL table.
 *
 * @author Florent Guillaume
 */
public class TableImpl implements Table {

    private static final long serialVersionUID = 1L;

    protected final Database database;

    protected final Dialect dialect;

    protected final String name;

    /** Map of logical names to columns. */
    private final LinkedHashMap<String, Column> columns;

    /** Logical names of indexed columns. */
    private final List<String[]> indexedColumns;

    /** Those of the indexed columns that concern fulltext. */
    private final List<String[]> fulltextIndexedColumns;

    /**
     * Creates a new empty table.
     */
    public TableImpl(Database database, String name) {
        this.database = database;
        dialect = database.dialect;
        this.name = name;
        // we use a LinkedHashMap to have deterministic ordering
        columns = new LinkedHashMap<String, Column>();
        indexedColumns = new LinkedList<String[]>();
        fulltextIndexedColumns = new LinkedList<String[]>();
    }

    public Dialect getDialect() {
        return dialect;
    }

    public String getName() {
        return name;
    }

    public String getQuotedName() {
        return dialect.openQuote() + name + dialect.closeQuote();
    }

    public String getQuotedSuffixedName(String suffix) {
        return dialect.openQuote() + name + suffix + dialect.closeQuote();
    }

    public Column getColumn(String name) {
        return columns.get(name);
    }

    public Collection<Column> getColumns() {
        return columns.values();
    }

    /**
     * Adds a {@link Column} to the table.
     */
    public Column addColumn(String name, PropertyType type, int sqlType,
            String sqlTypeString, String key, Model model) {
        String physicalName = database.getColumnPhysicalName(name);
        if (columns.containsKey(physicalName)) {
            throw new IllegalArgumentException("duplicate column "
                    + physicalName);
        }
        Column column = new Column(this, physicalName, type, sqlType,
                sqlTypeString, key, model);
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

    public void addFulltextIndex(String... columnNames) {
        indexedColumns.add(columnNames);
        fulltextIndexedColumns.add(columnNames);
    }

    public boolean hasFulltextIndex() {
        return !fulltextIndexedColumns.isEmpty();
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
        for (Iterator<Column> it = columns.values().iterator(); it.hasNext();) {
            addOneColumn(buf, it.next());
            if (it.hasNext()) {
                buf.append(", ");
            }
        }
        // unique
        // check
        buf.append(')');
        buf.append(dialect.getTableTypeString(this));
        return buf.toString();
    }

    /**
     * Computes the SQL statement to alter a table and add a column to it.
     *
     * @param column the column to add
     * @return the SQL alter table string
     */
    public String getAddColumnSql(Column column) {
        StringBuilder buf = new StringBuilder();
        buf.append("ALTER TABLE ");
        buf.append(getQuotedName());
        buf.append(' ');
        buf.append(dialect.getAddColumnString());
        buf.append(' ');
        addOneColumn(buf, column);
        return buf.toString();
    }

    /**
     * Adds to buf the column name and its type and constraints for create /
     * alter.
     */
    protected void addOneColumn(StringBuilder buf, Column column) {
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
                String constraintName = dialect.openQuote()
                        + name
                        + (dialect.storesUpperCaseIdentifiers() ? "_PK" : "_pk")
                        + dialect.closeQuote();
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
                String constraintName = dialect.openQuote()
                        + dialect.getForeignKeyConstraintName(name,
                                column.getPhysicalName(), ft.getName())
                        + dialect.closeQuote();
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
                if (dialect.supportsCircularCascadeDeleteConstraints()
                        || (Model.MAIN_KEY.equals(fc.getPhysicalName()) && Model.MAIN_KEY.equals(column.getPhysicalName()))) {
                    // MS SQL Server can't have circular ON DELETE CASCADE.
                    // Use a trigger INSTEAD OF DELETE to cascade deletes
                    // recursively for:
                    // - hierarchy.parentid
                    // - versions.versionableid
                    // - proxies.versionableid
                    buf.append(" ON DELETE CASCADE");
                }
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
            String indexName = dialect.openQuote()
                    + dialect.getIndexName(name, pcols) + dialect.closeQuote();
            String createIndexSql;
            if (fulltextIndexedColumns.contains(columnNames)) {
                createIndexSql = dialect.getCreateFulltextIndexSql(indexName,
                        getQuotedName(), qcols);
            } else {
                createIndexSql = dialect.getCreateIndexSql(indexName,
                        getQuotedName(), qcols);
            }
            sqls.add(createIndexSql);
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
        buf.append(dialect.getCascadeDropConstraintsString());
        if (dialect.supportsIfExistsAfterTableName()) {
            buf.append(" IF EXISTS");
        }
        return buf.toString();
    }

    @Override
    public String toString() {
        return "Table(" + name + ')';
    }

}
