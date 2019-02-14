/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
        columns = new LinkedHashMap<>();
        indexedColumns = new LinkedList<>();
        indexNames = new HashMap<>();
        indexTypes = new HashMap<>();
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
     * Adds a column without dialect physical name canonicalization (for directories).
     */
    public Column addColumn(String name, Column column) {
        if (columns.containsKey(name)) {
            throw new IllegalArgumentException("duplicate column " + name);
        }
        columns.put(name, column);
        return column;
    }

    @Override
    public Column addColumn(String name, ColumnType type, String key, Model model) {
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
    public void addIndex(String indexName, IndexType indexType, String... columnNames) {
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
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ");
        sb.append(getQuotedName());
        sb.append(" (");
        String custom = dialect.getCustomColumnDefinition(this);
        if (custom != null) {
            sb.append(custom);
            sb.append(", ");
        }
        for (Iterator<Column> it = columns.values().iterator(); it.hasNext();) {
            addOneColumn(sb, it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        // unique
        // check
        sb.append(')');
        sb.append(dialect.getTableTypeString(this));
        return sb.toString();
    }

    /**
     * Computes the SQL statement to alter a table and add a column to it.
     *
     * @param column the column to add
     * @return the SQL alter table string
     */
    @Override
    public String getAddColumnSql(Column column) {
        StringBuilder sb = new StringBuilder();
        sb.append("ALTER TABLE ");
        sb.append(getQuotedName());
        sb.append(' ');
        sb.append(dialect.getAddColumnString());
        sb.append(' ');
        addOneColumn(sb, column);
        return sb.toString();
    }

    /**
     * Adds to buf the column name and its type and constraints for create / alter.
     */
    protected void addOneColumn(StringBuilder sb, Column column) {
        sb.append(column.getQuotedName());
        sb.append(' ');
        sb.append(column.getSqlTypeString());
        String defaultValue = column.getDefaultValue();
        if (defaultValue != null) {
            sb.append(" DEFAULT ");
            sb.append(defaultValue);
        }
        if (column.isNullable()) {
            sb.append(dialect.getNullColumnString());
        } else {
            sb.append(" NOT NULL");
        }
    }

    @Override
    public List<String> getPostCreateSqls(Model model) {
        List<String> sqls = new LinkedList<>();
        List<String> custom = dialect.getCustomPostCreateSqls(this, model);
        sqls.addAll(custom);
        for (Column column : columns.values()) {
            postAddColumn(column, sqls, model);
        }
        return sqls;
    }

    @Override
    public List<String> getPostAddSqls(Column column, Model model) {
        List<String> sqls = new LinkedList<>();
        postAddColumn(column, sqls, model);
        return sqls;
    }

    protected void postAddColumn(Column column, List<String> sqls, Model model) {
        if (column.isPrimary() && !(column.isIdentity() && dialect.isIdentityAlreadyPrimary())) {
            StringBuilder sb = new StringBuilder();
            String constraintName = dialect.openQuote() + dialect.getPrimaryKeyConstraintName(key)
                    + dialect.closeQuote();
            sb.append("ALTER TABLE ");
            sb.append(getQuotedName());
            sb.append(dialect.getAddPrimaryKeyConstraintString(constraintName));
            sb.append('(');
            sb.append(column.getQuotedName());
            sb.append(')');
            sqls.add(sb.toString());
        }
        if (column.isIdentity()) {
            // Oracle needs a sequence + trigger
            sqls.addAll(dialect.getPostCreateIdentityColumnSql(column));
        }
        Table ft = column.getForeignTable();
        if (ft != null) {
            Column fc = ft.getColumn(column.getForeignKey());
            String constraintName = dialect.openQuote()
                    + dialect.getForeignKeyConstraintName(key, column.getPhysicalName(), ft.getPhysicalName())
                    + dialect.closeQuote();
            StringBuilder sb = new StringBuilder();
            sb.append("ALTER TABLE ");
            sb.append(getQuotedName());
            sb.append(dialect.getAddForeignKeyConstraintString(constraintName,
                    new String[] { column.getQuotedName() }, ft.getQuotedName(), new String[] { fc.getQuotedName() },
                    true));
            if (dialect.supportsCircularCascadeDeleteConstraints()
                    || (Model.MAIN_KEY.equals(fc.getPhysicalName()) && Model.MAIN_KEY.equals(column.getPhysicalName()))) {
                // MS SQL Server can't have circular ON DELETE CASCADE.
                // Use a trigger INSTEAD OF DELETE to cascade deletes
                // recursively for:
                // - hierarchy.parentid
                // - proxies.targetid
                sb.append(" ON DELETE CASCADE");
            }
            sqls.add(sb.toString());
        }
        // add indexes for this column
        String columnName = column.getKey();
        INDEXES: //
        for (String[] columnNames : indexedColumns) {
            List<String> names = new ArrayList<>(Arrays.asList(columnNames));
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
            List<Column> cols = new ArrayList<>(columnNames.length);
            for (String name : columnNames) {
                Column col = getColumn(name);
                cols.add(col);
            }
            String indexName = indexNames.get(columnNames);
            IndexType indexType = indexTypes.get(columnNames);
            String createIndexSql = dialect.getCreateIndexSql(indexName, indexType, this, cols, model);
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
        StringBuilder sb = new StringBuilder();
        sb.append("DROP TABLE ");
        if (dialect.supportsIfExistsBeforeTableName()) {
            sb.append("IF EXISTS ");
        }
        sb.append(getQuotedName());
        sb.append(dialect.getCascadeDropConstraintsString());
        if (dialect.supportsIfExistsAfterTableName()) {
            sb.append(" IF EXISTS");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Table(" + name + ')';
    }

}
