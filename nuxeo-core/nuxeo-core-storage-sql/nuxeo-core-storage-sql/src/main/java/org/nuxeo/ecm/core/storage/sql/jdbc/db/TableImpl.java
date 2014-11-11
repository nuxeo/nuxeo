/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.jdbc.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;

/**
 * The basic implementation of a SQL table.
 */
public class TableImpl implements Table {

    private static final long serialVersionUID = 1L;

    protected final Dialect dialect;

    protected final String key;

    protected final String name;

    /** Map of logical names to columns. */
    private final LinkedHashMap<String, Column> columns;

    private Column primaryColumn;

    /** Logical names of indexed columns. */
    private final List<String[]> indexedColumns;

    /** Index names. */
    private final Map<String[], String> indexNames;

    /** Index types. */
    private final Map<String[], IndexType> indexTypes;

    private boolean hasFulltextIndex;

    /**
     * Creates a new empty table.
     */
    public TableImpl(Dialect dialect, String name, String key) {
        this.dialect = dialect;
        this.key = key; // Model table name
        this.name = name;
        // we use a LinkedHashMap to have deterministic ordering
        columns = new LinkedHashMap<String, Column>();
        indexedColumns = new LinkedList<String[]>();
        indexNames = new HashMap<String[], String>();
        indexTypes = new HashMap<String[], IndexType>();
    }

    @Override
    public boolean isAlias() {
        return false;
    }

    @Override
    public Table getRealTable() {
        return this;
    }

    @Override
    public Dialect getDialect() {
        return dialect;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getPhysicalName() {
        return name;
    }

    @Override
    public String getQuotedName() {
        return dialect.openQuote() + name + dialect.closeQuote();
    }

    @Override
    public String getQuotedSuffixedName(String suffix) {
        return dialect.openQuote() + name + suffix + dialect.closeQuote();
    }

    @Override
    public Column getColumn(String name) {
        return columns.get(name);
    }

    @Override
    public Column getPrimaryColumn() {
        if (primaryColumn == null) {
            for (Column column : columns.values()) {
                if (column.isPrimary()) {
                    primaryColumn = column;
                    break;
                }
            }
        }
        return primaryColumn;
    }

    @Override
    public Collection<Column> getColumns() {
        return columns.values();
    }

    /**
     * Adds a column without dialect physical name canonicalization (for
     * directories).
     */
    public Column addColumn(String name, Column column) {
        if (columns.containsKey(name)) {
            throw new IllegalArgumentException("duplicate column " + name);
        }
        columns.put(name, column);
        return column;
    }

    @Override
    public Column addColumn(String name, ColumnType type, String key,
            Model model) {
        String physicalName = dialect.getColumnName(name);
        Column column = new Column(this, physicalName, type, key);
        return addColumn(name, column);
    }

    /**
     * Adds an index on one or several columns.
     *
     * @param columnNames the column names
     */
    @Override
    public void addIndex(String... columnNames) {
        indexedColumns.add(columnNames);
    }

    @Override
    public void addIndex(String indexName, IndexType indexType,
            String... columnNames) {
        addIndex(columnNames);
        indexNames.put(columnNames, indexName);
        indexTypes.put(columnNames, indexType);
        if (indexType == IndexType.FULLTEXT) {
            hasFulltextIndex = true;
        }
    }

    @Override
    public boolean hasFulltextIndex() {
        return hasFulltextIndex;
    }

    /**
     * Computes the SQL statement to create the table.
     *
     * @return the SQL create string.
     */
    @Override
    public String getCreateSql() {
        StringBuilder buf = new StringBuilder();
        buf.append("CREATE TABLE ");
        buf.append(getQuotedName());
        buf.append(" (");
        String custom = dialect.getCustomColumnDefinition(this);
        if (custom != null) {
            buf.append(custom);
            buf.append(", ");
        }
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
    @Override
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

    @Override
    public List<String> getPostCreateSqls(Model model) {
        List<String> sqls = new LinkedList<String>();
        List<String> custom = dialect.getCustomPostCreateSqls(this);
        sqls.addAll(custom);
        for (Column column : columns.values()) {
            postAddColumn(column, sqls, model);
        }
        return sqls;
    }

    @Override
    public List<String> getPostAddSqls(Column column, Model model) {
        List<String> sqls = new LinkedList<String>();
        postAddColumn(column, sqls, model);
        return sqls;
    }

    protected void postAddColumn(Column column, List<String> sqls, Model model) {
        if (column.isPrimary()
                && !(column.isIdentity() && dialect.isIdentityAlreadyPrimary())) {
            StringBuilder buf = new StringBuilder();
            String constraintName = dialect.openQuote()
                    + dialect.getPrimaryKeyConstraintName(key)
                    + dialect.closeQuote();
            buf.append("ALTER TABLE ");
            buf.append(getQuotedName());
            buf.append(dialect.getAddPrimaryKeyConstraintString(constraintName));
            buf.append('(');
            buf.append(column.getQuotedName());
            buf.append(')');
            sqls.add(buf.toString());
        }
        if (column.isIdentity()) {
            // Oracle needs a sequence + trigger
            sqls.addAll(dialect.getPostCreateIdentityColumnSql(column));
        }
        Table ft = column.getForeignTable();
        if (ft != null) {
            Column fc = ft.getColumn(column.getForeignKey());
            String constraintName = dialect.openQuote()
                    + dialect.getForeignKeyConstraintName(key,
                            column.getPhysicalName(), ft.getPhysicalName())
                    + dialect.closeQuote();
            StringBuilder buf = new StringBuilder();
            buf.append("ALTER TABLE ");
            buf.append(getQuotedName());
            buf.append(dialect.getAddForeignKeyConstraintString(constraintName,
                    new String[] { column.getQuotedName() },
                    ft.getQuotedName(), new String[] { fc.getQuotedName() },
                    true));
            if (dialect.supportsCircularCascadeDeleteConstraints()
                    || (Model.MAIN_KEY.equals(fc.getPhysicalName()) && Model.MAIN_KEY.equals(column.getPhysicalName()))) {
                // MS SQL Server can't have circular ON DELETE CASCADE.
                // Use a trigger INSTEAD OF DELETE to cascade deletes
                // recursively for:
                // - hierarchy.parentid
                // - proxies.targetid
                buf.append(" ON DELETE CASCADE");
            }
            sqls.add(buf.toString());
        }
        // add indexes for this column
        String columnName = column.getKey();
        INDEXES: //
        for (String[] columnNames : indexedColumns) {
            List<String> names = new ArrayList<String>(
                    Arrays.asList(columnNames));
            // check that column is part of this index
            if (!names.contains(columnName)) {
                continue;
            }
            // check that column is the last one mentioned
            for (Column c : getColumns()) {
                String key = c.getKey();
                names.remove(key);
                if (names.isEmpty()) {
                    // last one?
                    if (!columnName.equals(key)) {
                        continue INDEXES;
                    }
                    break;
                }
            }
            // add this index now, as all columns have been created
            List<Column> cols = new ArrayList<Column>(columnNames.length);
            for (String name : columnNames) {
                Column col = getColumn(name);
                cols.add(col);
            }
            String indexName = indexNames.get(columnNames);
            IndexType indexType = indexTypes.get(columnNames);
            String createIndexSql = dialect.getCreateIndexSql(indexName,
                    indexType, this, cols, model);
            sqls.add(createIndexSql);
        }
    }

    /**
     * Computes the SQL statement to drop the table.
     * <p>
     * TODO drop constraints and indexes
     *
     * @return the SQL drop string.
     */
    @Override
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
