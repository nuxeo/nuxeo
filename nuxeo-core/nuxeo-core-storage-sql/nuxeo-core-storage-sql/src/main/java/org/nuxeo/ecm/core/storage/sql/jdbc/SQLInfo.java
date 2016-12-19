/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.core.storage.FulltextConfiguration;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.Selection;
import org.nuxeo.ecm.core.storage.sql.SelectionType;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Database;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Delete;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Insert;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Join;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Select;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table.IndexType;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Update;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.SQLStatement;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.SQLStatement.ListCollector;

/**
 * This singleton generates and holds the actual SQL DDL and DML statements for the operations needed by the
 * {@link Mapper}, given a {@link Model}.
 * <p>
 * It is specific to one SQL dialect.
 */
public class SQLInfo {

    private static final String ORDER_DESC = "DESC";

    private static final String ORDER_ASC = "ASC";

    public final Database database;

    public final Dialect dialect;

    public final boolean softDeleteEnabled;

    public final boolean proxiesEnabled;

    private final Model model;

    private String selectRootIdSql;

    private Column selectRootIdWhatColumn;

    private final Map<String, String> insertSqlMap; // statement

    private final Map<String, List<Column>> insertColumnsMap;

    private final Map<String, String> deleteSqlMap; // statement

    private Map<SelectionType, SQLInfoSelection> selections;

    private String selectChildrenIdsAndTypesSql;

    private String selectComplexChildrenIdsAndTypesSql;

    private List<Column> selectChildrenIdsAndTypesWhatColumns;

    private String selectDescendantsInfoSql;

    private List<Column> selectDescendantsInfoWhatColumns;

    private final Map<String, String> copySqlMap;

    private final Map<String, Column> copyIdColumnMap;

    protected final Map<String, SQLInfoSelect> selectFragmentById;

    protected String createClusterNodeSql;

    protected List<Column> createClusterNodeColumns;

    protected String deleteClusterNodeSql;

    protected Column deleteClusterNodeColumn;

    protected String deleteClusterInvalsSql;

    protected Column deleteClusterInvalsColumn;

    protected List<Column> clusterInvalidationsColumns;

    protected Map<String, List<SQLStatement>> sqlStatements;

    protected Map<String, Serializable> sqlStatementsProperties;

    protected List<String> getBinariesSql;

    protected List<Column> getBinariesColumns;

    /**
     * Generates and holds the needed SQL statements given a {@link Model} and a {@link Dialect}.
     *
     * @param model the model
     * @param dialect the SQL dialect
     */
    public SQLInfo(Model model, Dialect dialect) {
        this.model = model;
        this.dialect = dialect;
        RepositoryDescriptor repositoryDescriptor = model.getRepositoryDescriptor();
        softDeleteEnabled = repositoryDescriptor.getSoftDeleteEnabled();
        proxiesEnabled = repositoryDescriptor.getProxiesEnabled();

        database = new Database(dialect);

        selectRootIdSql = null;
        selectRootIdWhatColumn = null;

        selectFragmentById = new HashMap<>();

        selections = new HashMap<>();

        selectChildrenIdsAndTypesSql = null;
        selectChildrenIdsAndTypesWhatColumns = null;
        selectComplexChildrenIdsAndTypesSql = null;

        insertSqlMap = new HashMap<>();
        insertColumnsMap = new HashMap<>();

        deleteSqlMap = new HashMap<>();

        copySqlMap = new HashMap<>();
        copyIdColumnMap = new HashMap<>();

        getBinariesSql = new ArrayList<>(1);
        getBinariesColumns = new ArrayList<>(1);

        initSQL();
        initSelections();

        try {
            initSQLStatements(JDBCMapper.testProps, repositoryDescriptor.sqlInitFiles);
        } catch (IOException e) {
            throw new NuxeoException(e);
        }
    }

    public Database getDatabase() {
        return database;
    }

    // ----- select -----

    public String getSelectRootIdSql() {
        return selectRootIdSql;
    }

    public Column getSelectRootIdWhatColumn() {
        return selectRootIdWhatColumn;
    }

    public String getInsertRootIdSql() {
        return insertSqlMap.get(Model.REPOINFO_TABLE_NAME);
    }

    public List<Column> getInsertRootIdColumns() {
        return insertColumnsMap.get(Model.REPOINFO_TABLE_NAME);
    }

    public SQLInfoSelection getSelection(SelectionType type) {
        return selections.get(type);

    }

    public String getSelectChildrenIdsAndTypesSql(boolean onlyComplex) {
        return onlyComplex ? selectComplexChildrenIdsAndTypesSql : selectChildrenIdsAndTypesSql;
    }

    public List<Column> getSelectChildrenIdsAndTypesWhatColumns() {
        return selectChildrenIdsAndTypesWhatColumns;
    }

    public String getSelectDescendantsInfoSql() {
        return selectDescendantsInfoSql;
    }

    public List<Column> getSelectDescendantsInfoWhatColumns() {
        return selectDescendantsInfoWhatColumns;
    }

    // ----- cluster -----

    public String getCreateClusterNodeSql() {
        return createClusterNodeSql;
    }

    public List<Column> getCreateClusterNodeColumns() {
        return createClusterNodeColumns;
    }

    public String getDeleteClusterNodeSql() {
        return deleteClusterNodeSql;
    }

    public Column getDeleteClusterNodeColumn() {
        return deleteClusterNodeColumn;
    }

    public String getDeleteClusterInvalsSql() {
        return deleteClusterInvalsSql;
    }

    public Column getDeleteClusterInvalsColumn() {
        return deleteClusterInvalsColumn;
    }

    public int getClusterNodeIdType() {
        return dialect.getJDBCTypeAndString(ColumnType.CLUSTERNODE).jdbcType;
    }

    public List<Column> getClusterInvalidationsColumns() {
        return clusterInvalidationsColumns;
    }

    // ----- insert -----

    /**
     * Returns the SQL {@code INSERT} to add a row. The columns that represent sequences that are implicitly
     * auto-incremented aren't included.
     *
     * @param tableName the table name
     * @return the SQL {@code INSERT} statement
     */
    public String getInsertSql(String tableName) {
        return insertSqlMap.get(tableName);
    }

    /**
     * Returns the list of columns to use for an {@INSERT} statement {@link #getInsertSql}.
     *
     * @param tableName the table name
     * @return the list of columns
     */
    public List<Column> getInsertColumns(String tableName) {
        return insertColumnsMap.get(tableName);
    }

    // -----

    /**
     * Returns the clause used to match a given row by id in the given table.
     * <p>
     * Takes into account soft deletes.
     *
     * @param tableName the table name
     * @return the clause, like {@code table.id = ?}
     */
    public String getIdEqualsClause(String tableName) {
        return database.getTable(tableName).getColumn(Model.MAIN_KEY).getQuotedName() + " = ?"
                + getSoftDeleteClause(tableName);
    }

    /**
     * Returns {@code AND isdeleted IS NULL} if this is the hierarchy table and soft delete is activated.
     *
     * @param tableName the table name
     * @return the clause
     */
    public String getSoftDeleteClause(String tableName) {
        if (Model.HIER_TABLE_NAME.equals(tableName) && softDeleteEnabled) {
            return " AND " + getSoftDeleteClause();
        } else {
            return "";
        }
    }

    /**
     * Returns null or {@code AND isdeleted IS NULL} if soft delete is activated.
     *
     * @return the clause, or null
     */
    public String getSoftDeleteClause() {
        if (softDeleteEnabled) {
            return database.getTable(Model.HIER_TABLE_NAME).getColumn(Model.MAIN_IS_DELETED_KEY).getFullQuotedName()
                    + " IS NULL";
        } else {
            return null;
        }
    }

    // ----- update -----

    // TODO these two methods are redundant with one another

    public SQLInfoSelect getUpdateById(String tableName, Collection<String> keys, Set<String> deltas) {
        Table table = database.getTable(tableName);
        List<Column> columns = new LinkedList<>();
        for (String key : keys) {
            columns.add(table.getColumn(key));
        }
        Update update = new Update(table);
        update.setUpdatedColumns(columns, deltas);
        update.setWhere(getIdEqualsClause(tableName));
        columns.add(table.getColumn(Model.MAIN_KEY));
        return new SQLInfoSelect(update.getStatement(), columns, null, null);
    }

    public Update getUpdateByIdForKeys(String tableName, List<String> keys) {
        Table table = database.getTable(tableName);
        List<Column> columns = new LinkedList<>();
        for (String key : keys) {
            columns.add(table.getColumn(key));
        }
        Update update = new Update(table);
        update.setUpdatedColumns(columns);
        update.setWhere(getIdEqualsClause(tableName));
        return update;
    }

    /**
     * Select by ids for all values of several fragments.
     */
    public SQLInfoSelect getSelectFragmentsByIds(String tableName, int nids) {
        return getSelectFragmentsByIds(tableName, nids, null, null);
    }

    /**
     * Select by ids for all values of several fragments (maybe ordered along columns -- for collection fragments
     * retrieval).
     */
    public SQLInfoSelect getSelectFragmentsByIds(String tableName, int nids, String[] orderBys,
            Set<String> skipColumns) {
        Table table = database.getTable(tableName);
        List<Column> whatColumns = new LinkedList<>();
        List<String> whats = new LinkedList<>();
        List<Column> opaqueColumns = new LinkedList<>();
        for (Column column : table.getColumns()) {
            if (column.isOpaque()) {
                opaqueColumns.add(column);
            } else if (skipColumns == null || !skipColumns.contains(column.getKey())) {
                whatColumns.add(column);
                whats.add(column.getQuotedName());
            }
        }
        Column whereColumn = table.getColumn(Model.MAIN_KEY);
        StringBuilder wherebuf = new StringBuilder(whereColumn.getQuotedName());
        wherebuf.append(" IN (");
        for (int i = 0; i < nids; i++) {
            if (i != 0) {
                wherebuf.append(", ");
            }
            wherebuf.append('?');
        }
        wherebuf.append(')');
        wherebuf.append(getSoftDeleteClause(tableName));
        Select select = new Select(table);
        select.setWhat(String.join(", ", whats));
        select.setFrom(table.getQuotedName());
        select.setWhere(wherebuf.toString());
        if (orderBys != null) {
            List<String> orders = new LinkedList<>();
            for (String orderBy : orderBys) {
                orders.add(table.getColumn(orderBy).getQuotedName());
            }
            select.setOrderBy(String.join(", ", orders));
        }
        return new SQLInfoSelect(select.getStatement(), whatColumns, Collections.singletonList(whereColumn),
                opaqueColumns.isEmpty() ? null : opaqueColumns);
    }

    /**
     * Select all ancestors ids for several fragments.
     * <p>
     * Fast alternative to the slowest iterative {@link #getSelectParentIds}.
     *
     * @return null if it's not possible in one call in this dialect
     */
    public SQLInfoSelect getSelectAncestorsIds() {
        String sql = dialect.getAncestorsIdsSql();
        if (sql == null) {
            return null;
        }
        Table table = database.getTable(Model.HIER_TABLE_NAME);
        Column mainColumn = table.getColumn(Model.MAIN_KEY);
        // no soft-delete check needed, as ancestors of a non-deleted doc
        // aren't deleted either
        return new SQLInfoSelect(sql, Collections.singletonList(mainColumn), null, null);
    }

    /**
     * Select parentid by ids for all values of several fragments.
     */
    public SQLInfoSelect getSelectParentIds(int nids) {
        Table table = database.getTable(Model.HIER_TABLE_NAME);
        Column whatColumn = table.getColumn(Model.HIER_PARENT_KEY);
        Column whereColumn = table.getColumn(Model.MAIN_KEY);
        StringBuilder wherebuf = new StringBuilder(whereColumn.getQuotedName());
        wherebuf.append(" IN (");
        for (int i = 0; i < nids; i++) {
            if (i != 0) {
                wherebuf.append(", ");
            }
            wherebuf.append('?');
        }
        wherebuf.append(')');
        wherebuf.append(getSoftDeleteClause(Model.HIER_TABLE_NAME));
        Select select = new Select(table);
        select.setWhat("DISTINCT " + whatColumn.getQuotedName());
        select.setFrom(table.getQuotedName());
        select.setWhere(wherebuf.toString());
        return new SQLInfoSelect(select.getStatement(), Collections.singletonList(whatColumn),
                Collections.singletonList(whereColumn), null);
    }

    /**
     * Selects all children (not complex) for several parent ids.
     */
    public SQLInfoSelect getSelectChildrenNodeInfos(int nids) {
        Table hierTable = database.getTable(Model.HIER_TABLE_NAME);
        Column mainColumn = hierTable.getColumn(Model.MAIN_KEY);
        List<Column> whatColumns = new ArrayList<>();
        whatColumns.add(mainColumn);
        whatColumns.add(hierTable.getColumn(Model.HIER_PARENT_KEY));
        whatColumns.add(hierTable.getColumn(Model.MAIN_PRIMARY_TYPE_KEY));
        Table proxyTable = null;
        if (proxiesEnabled) {
            proxyTable = database.getTable(Model.PROXY_TABLE_NAME);
            whatColumns.add(proxyTable.getColumn(Model.PROXY_TARGET_KEY));
            whatColumns.add(proxyTable.getColumn(Model.PROXY_VERSIONABLE_KEY));
        }
        String selectWhats = whatColumns.stream().map(Column::getFullQuotedName).collect(Collectors.joining(", "));
        Select select = new Select(null);
        select.setWhat(selectWhats);
        String from = hierTable.getQuotedName();
        if (proxiesEnabled) {
            from += " LEFT JOIN " + proxyTable.getQuotedName() + " ON " + mainColumn.getFullQuotedName() + " = "
                    + proxyTable.getColumn(Model.MAIN_KEY).getFullQuotedName();
        }
        select.setFrom(from);
        Column whereColumn = hierTable.getColumn(Model.HIER_PARENT_KEY);
        StringBuilder wherebuf = new StringBuilder(whereColumn.getFullQuotedName());
        if (nids == 1) {
            wherebuf.append(" = ?");
        } else {
            wherebuf.append(" IN (");
            for (int i = 0; i < nids; i++) {
                if (i != 0) {
                    wherebuf.append(", ");
                }
                wherebuf.append('?');
            }
            wherebuf.append(')');
        }
        wherebuf.append(" AND ");
        wherebuf.append(hierTable.getColumn(Model.HIER_CHILD_ISPROPERTY_KEY).getFullQuotedName());
        wherebuf.append(" = ").append(dialect.toBooleanValueString(false)); // not complex
        wherebuf.append(getSoftDeleteClause(Model.HIER_TABLE_NAME));
        select.setWhere(wherebuf.toString());
        return new SQLInfoSelect(select.getStatement(), whatColumns, Collections.singletonList(whereColumn), null);
    }

    // ----- delete -----

    /**
     * Returns the SQL {@code DELETE} to delete a row. The primary key columns are free parameters.
     *
     * @param tableName the table name
     * @return the SQL {@code DELETE} statement
     */
    public String getDeleteSql(String tableName) {
        return deleteSqlMap.get(tableName);
    }

    /**
     * Returns the SQL {@code DELETE} to delete several rows. The primary key columns are free parameters.
     *
     * @param tableName the table name
     * @param n the number of rows to delete
     * @return the SQL {@code DELETE} statement with a {@code IN} for the keys
     */
    public String getDeleteSql(String tableName, int n) {
        Table table = database.getTable(tableName);
        Delete delete = new Delete(table);
        String where = null;
        for (Column column : table.getColumns()) {
            if (column.getKey().equals(Model.MAIN_KEY)) {
                StringBuilder buf = new StringBuilder();
                buf.append(column.getQuotedName());
                if (n == 1) {
                    buf.append(" = ?");
                } else {
                    buf.append(" IN (");
                    for (int i = 0; i < n; i++) {
                        if (i > 0) {
                            buf.append(", ");
                        }
                        buf.append("?");
                    }
                    buf.append(")");
                }
                where = buf.toString();
            }
        }
        delete.setWhere(where);
        return delete.getStatement();
    }

    /**
     * Returns the SQL to soft-delete several rows. The array of ids and the time are free parameters.
     *
     * @return the SQL statement
     */
    public String getSoftDeleteSql() {
        return dialect.getSoftDeleteSql();
    }

    /**
     * Returns the SQL to clean (hard-delete) soft-deleted rows. The max and beforeTime are free parameters.
     *
     * @return the SQL statement
     */
    public String getSoftDeleteCleanupSql() {
        return dialect.getSoftDeleteCleanupSql();
    }

    // ----- copy -----

    public SQLInfoSelect getCopyHier(boolean explicitName, boolean resetVersion) {
        Table table = database.getTable(Model.HIER_TABLE_NAME);
        Collection<Column> columns = table.getColumns();
        List<String> selectWhats = new ArrayList<>(columns.size());
        List<Column> selectWhatColumns = new ArrayList<>(5);
        Insert insert = new Insert(table);
        for (Column column : columns) {
            if (column.isIdentity()) {
                // identity column is never copied
                continue;
            }
            insert.addColumn(column);
            String quotedName = column.getQuotedName();
            String key = column.getKey();
            if (key.equals(Model.MAIN_KEY) //
                    || key.equals(Model.HIER_PARENT_KEY) //
                    || key.equals(Model.MAIN_BASE_VERSION_KEY) //
                    || key.equals(Model.MAIN_CHECKED_IN_KEY) //
                    || (key.equals(Model.MAIN_MINOR_VERSION_KEY) && resetVersion) //
                    || (key.equals(Model.MAIN_MAJOR_VERSION_KEY) && resetVersion) //
                    || (key.equals(Model.HIER_CHILD_NAME_KEY) && explicitName)) {
                // explicit value set
                selectWhats.add("?");
                selectWhatColumns.add(column);
            } else {
                // otherwise copy value
                selectWhats.add(quotedName);
            }
        }
        Column whereColumn = table.getColumn(Model.MAIN_KEY);
        Select select = new Select(null);
        select.setFrom(table.getQuotedName());
        select.setWhat(String.join(", ", selectWhats));
        select.setWhere(whereColumn.getQuotedName() + " = ?");
        insert.setValues(select.getStatement());
        String sql = insert.getStatement();
        return new SQLInfoSelect(sql, selectWhatColumns, Collections.singletonList(whereColumn), null);
    }

    public String getCopySql(String tableName) {
        return copySqlMap.get(tableName);
    }

    public Column getCopyIdColumn(String tableName) {
        return copyIdColumnMap.get(tableName);
    }

    // ----- prepare everything -----

    /**
     * Creates all the sql from the models.
     */
    protected void initSQL() {

        // structural tables
        if (model.getRepositoryDescriptor().getClusteringEnabled()) {
            if (!dialect.isClusteringSupported()) {
                throw new NuxeoException("Clustering not supported for " + dialect.getClass().getSimpleName());
            }
            initClusterSQL();
        }
        initHierarchySQL();
        initRepositorySQL();
        if (dialect.supportsAncestorsTable()) {
            initAncestorsSQL();
        }

        for (String tableName : model.getFragmentNames()) {
            if (tableName.equals(Model.HIER_TABLE_NAME)) {
                continue;
            }
            initFragmentSQL(tableName);
        }

        /*
         * versions
         */

        Table hierTable = database.getTable(Model.HIER_TABLE_NAME);
        Table versionTable = database.getTable(Model.VERSION_TABLE_NAME);
        hierTable.addIndex(Model.MAIN_IS_VERSION_KEY);
        versionTable.addIndex(Model.VERSION_VERSIONABLE_KEY);
        // don't index series+label, a simple label scan will suffice

        /*
         * proxies
         */

        if (proxiesEnabled) {
            Table proxyTable = database.getTable(Model.PROXY_TABLE_NAME);
            proxyTable.addIndex(Model.PROXY_VERSIONABLE_KEY);
            proxyTable.addIndex(Model.PROXY_TARGET_KEY);
        }

        initSelectDescendantsSQL();

        /*
         * fulltext
         */
        if (!model.getRepositoryDescriptor().getFulltextDescriptor().getFulltextSearchDisabled()) {
            Table table = database.getTable(Model.FULLTEXT_TABLE_NAME);
            FulltextConfiguration fulltextConfiguration = model.getFulltextConfiguration();
            if (fulltextConfiguration.indexNames.size() > 1 && !dialect.supportsMultipleFulltextIndexes()) {
                String msg = String.format("SQL database supports only one fulltext index, but %d are configured: %s",
                        fulltextConfiguration.indexNames.size(), fulltextConfiguration.indexNames);
                throw new NuxeoException(msg);
            }
            for (String indexName : fulltextConfiguration.indexNames) {
                String suffix = model.getFulltextIndexSuffix(indexName);
                int ftic = dialect.getFulltextIndexedColumns();
                if (ftic == 1) {
                    table.addIndex(indexName, IndexType.FULLTEXT, Model.FULLTEXT_FULLTEXT_KEY + suffix);
                } else if (ftic == 2) {
                    table.addIndex(indexName, IndexType.FULLTEXT, Model.FULLTEXT_SIMPLETEXT_KEY + suffix,
                            Model.FULLTEXT_BINARYTEXT_KEY + suffix);
                }
            }
        }

        /*
         * binary columns for GC
         */
        for (Entry<String, List<String>> e : model.getBinaryPropertyInfos().entrySet()) {
            String tableName = e.getKey();
            Table table = database.getTable(tableName);
            for (String key : e.getValue()) {
                Select select = new Select(table);
                Column col = table.getColumn(key); // key = name for now
                select.setWhat("DISTINCT " + col.getQuotedName());
                select.setFrom(table.getQuotedName());
                getBinariesSql.add(select.getStatement());
                // in the result column we want the digest, not the binary
                Column resCol = new Column(table, null, ColumnType.STRING, null);
                getBinariesColumns.add(resCol);
            }
        }
    }

    protected void initClusterSQL() {
        TableMaker maker = new TableMaker(Model.CLUSTER_NODES_TABLE_NAME);
        maker.newColumn(Model.CLUSTER_NODES_NODEID_KEY, ColumnType.CLUSTERNODE);
        maker.newColumn(Model.CLUSTER_NODES_CREATED_KEY, ColumnType.TIMESTAMP);
        maker.postProcessClusterNodes();

        maker = new TableMaker(Model.CLUSTER_INVALS_TABLE_NAME);
        maker.newColumn(Model.CLUSTER_INVALS_NODEID_KEY, ColumnType.CLUSTERNODE);
        maker.newColumn(Model.CLUSTER_INVALS_ID_KEY, ColumnType.NODEVAL);
        maker.newColumn(Model.CLUSTER_INVALS_FRAGMENTS_KEY, ColumnType.CLUSTERFRAGS);
        maker.newColumn(Model.CLUSTER_INVALS_KIND_KEY, ColumnType.TINYINT);
        maker.table.addIndex(Model.CLUSTER_INVALS_NODEID_KEY);
        maker.postProcessClusterInvalidations();
    }

    /**
     * Creates the SQL for the table holding global repository information. This includes the id of the hierarchy root
     * node.
     */
    protected void initRepositorySQL() {
        TableMaker maker = new TableMaker(Model.REPOINFO_TABLE_NAME);
        maker.newColumn(Model.MAIN_KEY, ColumnType.NODEIDFK);
        maker.newColumn(Model.REPOINFO_REPONAME_KEY, ColumnType.SYSNAME);
        maker.postProcessRepository();
    }

    /**
     * Creates the SQL for the table holding hierarchy information.
     */
    protected void initHierarchySQL() {
        TableMaker maker = new TableMaker(Model.HIER_TABLE_NAME);
        // if (separateMainTable)
        // maker.newColumn(model.MAIN_KEY, ColumnType.NODEIDFK);
        maker.newColumn(Model.MAIN_KEY, ColumnType.NODEID);
        Column column = maker.newColumn(Model.HIER_PARENT_KEY, ColumnType.NODEIDFKNULL);
        maker.newColumn(Model.HIER_CHILD_POS_KEY, ColumnType.INTEGER);
        maker.newColumn(Model.HIER_CHILD_NAME_KEY, ColumnType.STRING);
        maker.newColumn(Model.HIER_CHILD_ISPROPERTY_KEY, ColumnType.BOOLEAN); // notnull
        // if (!separateMainTable)
        maker.newFragmentFields();
        maker.postProcess();
        maker.postProcessHierarchy();
        // if (!separateMainTable)
        // maker.postProcessIdGeneration();

        maker.table.addIndex(Model.HIER_PARENT_KEY);
        maker.table.addIndex(Model.HIER_PARENT_KEY, Model.HIER_CHILD_NAME_KEY);
        // don't index parent+name+isprop, a simple isprop scan will suffice
        maker.table.addIndex(Model.MAIN_PRIMARY_TYPE_KEY);

        if (model.getRepositoryDescriptor().getSoftDeleteEnabled()) {
            maker.table.addIndex(Model.MAIN_IS_DELETED_KEY);
        }
    }

    protected void initSelectDescendantsSQL() {
        Table hierTable = database.getTable(Model.HIER_TABLE_NAME);
        Table proxyTable = null;
        if (proxiesEnabled) {
            proxyTable = database.getTable(Model.PROXY_TABLE_NAME);
        }
        Column mainColumn = hierTable.getColumn(Model.MAIN_KEY);
        List<Column> whatCols = new ArrayList<>(Arrays.asList(mainColumn, hierTable.getColumn(Model.HIER_PARENT_KEY),
                hierTable.getColumn(Model.MAIN_PRIMARY_TYPE_KEY),
                hierTable.getColumn(Model.HIER_CHILD_ISPROPERTY_KEY)));
        if (proxiesEnabled) {
            whatCols.add(proxyTable.getColumn(Model.PROXY_VERSIONABLE_KEY));
            whatCols.add(proxyTable.getColumn(Model.PROXY_TARGET_KEY));
        }
        // no mixins, not used to decide if we have a version or proxy
        String whats = whatCols.stream().map(Column::getFullQuotedName).collect(Collectors.joining(", "));
        Select select = new Select(null);
        select.setWhat(whats);
        String from = hierTable.getQuotedName();
        if (proxiesEnabled) {
            from += " LEFT JOIN " + proxyTable.getQuotedName() + " ON " + mainColumn.getFullQuotedName() + " = "
                    + proxyTable.getColumn(Model.MAIN_KEY).getFullQuotedName();
        }
        select.setFrom(from);
        String where = dialect.getInTreeSql(mainColumn.getFullQuotedName(), null);
        where += getSoftDeleteClause(Model.HIER_TABLE_NAME);
        select.setWhere(where);
        selectDescendantsInfoSql = select.getStatement();
        selectDescendantsInfoWhatColumns = whatCols;
    }

    /**
     * Creates the SQL for the table holding ancestors information.
     * <p>
     * This table holds trigger-updated information extracted from the recursive parent-child relationship in the
     * hierarchy table.
     */
    protected void initAncestorsSQL() {
        TableMaker maker = new TableMaker(Model.ANCESTORS_TABLE_NAME);
        maker.newColumn(Model.MAIN_KEY, ColumnType.NODEIDFKMUL);
        maker.newColumn(Model.ANCESTORS_ANCESTOR_KEY, ColumnType.NODEARRAY);
    }

    /**
     * Creates the SQL for one fragment (simple or collection).
     */
    protected void initFragmentSQL(String tableName) {
        TableMaker maker = new TableMaker(tableName);
        ColumnType type;
        if (tableName.equals(Model.HIER_TABLE_NAME)) {
            type = ColumnType.NODEID;
        } else if (tableName.equals(Model.LOCK_TABLE_NAME)) {
            type = ColumnType.NODEIDPK; // no foreign key to hierarchy
        } else if (model.isCollectionFragment(tableName)) {
            type = ColumnType.NODEIDFKMUL;
        } else {
            type = ColumnType.NODEIDFK;
        }
        maker.newColumn(Model.MAIN_KEY, type);
        maker.newFragmentFields();
        maker.postProcess();
        // if (isMain)
        // maker.postProcessIdGeneration();
    }

    protected void initSelections() {
        for (SelectionType selType : SelectionType.values()) {
            if (!proxiesEnabled && selType.tableName.equals(Model.PROXY_TABLE_NAME)) {
                continue;
            }
            selections.put(selType, new SQLInfoSelection(selType));
        }
    }

    // ----- prepare one table -----

    protected class TableMaker {

        private final String tableName;

        private final Table table;

        private final String orderBy;

        protected TableMaker(String tableName) {
            this.tableName = tableName;
            table = database.addTable(tableName);
            orderBy = model.getCollectionOrderBy(tableName);
        }

        protected void newFragmentFields() {
            Map<String, ColumnType> keysType = model.getFragmentKeysType(tableName);
            for (Entry<String, ColumnType> entry : keysType.entrySet()) {
                newColumn(entry.getKey(), entry.getValue());
            }
        }

        protected Column newColumn(String columnName, ColumnType type) {
            Column column = table.addColumn(columnName, type, columnName, model);
            if (type == ColumnType.NODEID) {
                // column.setIdentity(true); if idGenPolicy identity
                column.setNullable(false);
                column.setPrimary(true);
            }
            if (type == ColumnType.NODEIDFK || type == ColumnType.NODEIDPK) {
                column.setNullable(false);
                column.setPrimary(true);
            }
            if (type == ColumnType.NODEIDFKMUL) {
                column.setNullable(false);
                table.addIndex(columnName);
            }
            if (type == ColumnType.NODEIDFK || type == ColumnType.NODEIDFKNP || type == ColumnType.NODEIDFKNULL
                    || type == ColumnType.NODEIDFKMUL) {
                column.setReferences(database.getTable(Model.HIER_TABLE_NAME), Model.MAIN_KEY);
            }
            return column;
        }

        // ----------------------- post processing -----------------------

        protected void postProcessClusterNodes() {
            Collection<Column> columns = table.getColumns();
            Insert insert = new Insert(table);
            for (Column column : columns) {
                insert.addColumn(column);
            }
            createClusterNodeSql = insert.getStatement();
            createClusterNodeColumns = new ArrayList<>(columns);

            Delete delete = new Delete(table);
            Column column = table.getColumn(Model.CLUSTER_NODES_NODEID_KEY);
            delete.setWhere(column.getQuotedName() + " = ?");
            deleteClusterNodeSql = delete.getStatement();
            deleteClusterNodeColumn = column;
        }

        protected void postProcessClusterInvalidations() {
            clusterInvalidationsColumns = Arrays.asList(table.getColumn(Model.CLUSTER_INVALS_NODEID_KEY),
                    table.getColumn(Model.CLUSTER_INVALS_ID_KEY), table.getColumn(Model.CLUSTER_INVALS_FRAGMENTS_KEY),
                    table.getColumn(Model.CLUSTER_INVALS_KIND_KEY));

            Delete delete = new Delete(table);
            Column column = table.getColumn(Model.CLUSTER_INVALS_NODEID_KEY);
            delete.setWhere(column.getQuotedName() + " = ?");
            deleteClusterInvalsSql = delete.getStatement();
            deleteClusterInvalsColumn = column;
        }

        protected void postProcessRepository() {
            postProcessRootIdSelect();
            postProcessInsert();
        }

        protected void postProcessRootIdSelect() {
            String what = null;
            String where = null;
            for (Column column : table.getColumns()) {
                String key = column.getKey();
                String qname = column.getQuotedName();
                if (key.equals(Model.MAIN_KEY)) {
                    what = qname;
                    selectRootIdWhatColumn = column;
                } else if (key.equals(Model.REPOINFO_REPONAME_KEY)) {
                    where = qname + " = ?";
                } else {
                    throw new RuntimeException(column.toString());
                }
            }
            Select select = new Select(table);
            select.setWhat(what);
            select.setFrom(table.getQuotedName());
            select.setWhere(where);
            selectRootIdSql = select.getStatement();
        }

        /**
         * Precompute what we can from the information available for a regular schema table, or a collection table.
         */
        protected void postProcess() {
            postProcessSelectById();
            postProcessInsert();
            postProcessDelete();
            postProcessCopy();
        }

        /**
         * Additional SQL for the hierarchy table.
         */
        protected void postProcessHierarchy() {
            postProcessSelectChildrenIdsAndTypes();
        }

        protected void postProcessSelectById() {
            String[] orderBys = orderBy == null ? NO_ORDER_BY : new String[] { orderBy, ORDER_ASC };
            SQLInfoSelect select = makeSelect(table, orderBys, Model.MAIN_KEY);
            selectFragmentById.put(tableName, select);
        }

        protected void postProcessSelectChildrenIdsAndTypes() {
            List<Column> whatColumns = new ArrayList<>(2);
            List<String> whats = new ArrayList<>(2);
            Column column = table.getColumn(Model.MAIN_KEY);
            whatColumns.add(column);
            whats.add(column.getQuotedName());
            column = table.getColumn(Model.MAIN_PRIMARY_TYPE_KEY);
            whatColumns.add(column);
            whats.add(column.getQuotedName());
            column = table.getColumn(Model.MAIN_MIXIN_TYPES_KEY);
            whatColumns.add(column);
            whats.add(column.getQuotedName());
            Select select = new Select(table);
            select.setWhat(String.join(", ", whats));
            select.setFrom(table.getQuotedName());
            String where = table.getColumn(Model.HIER_PARENT_KEY).getQuotedName() + " = ?"
                    + getSoftDeleteClause(tableName);
            select.setWhere(where);
            selectChildrenIdsAndTypesSql = select.getStatement();
            selectChildrenIdsAndTypesWhatColumns = whatColumns;
            // now only complex properties
            where += " AND " + table.getColumn(Model.HIER_CHILD_ISPROPERTY_KEY).getQuotedName() + " = "
                    + dialect.toBooleanValueString(true);
            select.setWhere(where);
            selectComplexChildrenIdsAndTypesSql = select.getStatement();
        }

        // TODO optimize multiple inserts into one statement for collections
        protected void postProcessInsert() {
            // insert (implicitly auto-generated sequences not included)
            Collection<Column> columns = table.getColumns();
            List<Column> insertColumns = new ArrayList<>(columns.size());
            Insert insert = new Insert(table);
            for (Column column : columns) {
                if (column.isIdentity()) {
                    // identity column is never inserted
                    continue;
                }
                insertColumns.add(column);
                insert.addColumn(column);
            }
            insertSqlMap.put(tableName, insert.getStatement());
            insertColumnsMap.put(tableName, insertColumns);
        }

        protected void postProcessDelete() {
            Delete delete = new Delete(table);
            String wheres = table.getColumns()
                                 .stream()
                                 .filter(col -> Model.MAIN_KEY.equals(col.getKey()))
                                 .map(col -> col.getQuotedName() + " = ?")
                                 .collect(Collectors.joining(" AND "));
            delete.setWhere(wheres);
            deleteSqlMap.put(tableName, delete.getStatement());
        }

        // copy of a fragment
        // INSERT INTO foo (id, x, y) SELECT ?, x, y FROM foo WHERE id = ?
        protected void postProcessCopy() {
            Collection<Column> columns = table.getColumns();
            List<String> selectWhats = new ArrayList<>(columns.size());
            Column copyIdColumn = table.getColumn(Model.MAIN_KEY);
            Insert insert = new Insert(table);
            for (Column column : columns) {
                if (column.isIdentity()) {
                    // identity column is never copied
                    continue;
                }
                insert.addColumn(column);
                if (column == copyIdColumn) {
                    // explicit value
                    selectWhats.add("?");
                } else {
                    // otherwise copy value
                    selectWhats.add(column.getQuotedName());
                }
            }
            Select select = new Select(table);
            select.setWhat(String.join(", ", selectWhats));
            select.setFrom(table.getQuotedName());
            select.setWhere(copyIdColumn.getQuotedName() + " = ?");
            insert.setValues(select.getStatement());
            copySqlMap.put(tableName, insert.getStatement());
            copyIdColumnMap.put(tableName, copyIdColumn);
        }

    }

    public static class SQLInfoSelect {

        public final String sql;

        public final List<Column> whatColumns;

        public final MapMaker mapMaker;

        public final List<Column> whereColumns;

        public final List<Column> opaqueColumns;

        /**
         * Standard select for given columns.
         */
        public SQLInfoSelect(String sql, List<Column> whatColumns, List<Column> whereColumns,
                List<Column> opaqueColumns) {
            this(sql, whatColumns, null, whereColumns, opaqueColumns);
        }

        /**
         * Select where some column keys may be aliased, and some columns may be computed. The {@link MapMaker} is used
         * by the queryAndFetch() method.
         */
        public SQLInfoSelect(String sql, MapMaker mapMaker) {
            this(sql, null, mapMaker, null, null);
        }

        public SQLInfoSelect(String sql, List<Column> whatColumns, MapMaker mapMaker, List<Column> whereColumns,
                List<Column> opaqueColumns) {
            this.sql = sql;
            this.whatColumns = whatColumns;
            this.mapMaker = mapMaker;
            this.whereColumns = whereColumns == null ? null : new ArrayList<>(whereColumns);
            this.opaqueColumns = opaqueColumns == null ? null : new ArrayList<>(opaqueColumns);
        }
    }

    /**
     * Info about how to do the query to get a {@link Selection}.
     */
    public class SQLInfoSelection {

        public final SelectionType type;

        public final SQLInfoSelect selectAll;

        public final SQLInfoSelect selectFiltered;

        public SQLInfoSelection(SelectionType selType) {
            this.type = selType;
            Table table = database.getTable(selType.tableName);
            SQLInfoSelect selectAll;
            SQLInfoSelect selectFiltered;
            String from = table.getQuotedName();
            List<String> clauses;
            if (selType.tableName.equals(Model.HIER_TABLE_NAME)) {
                // clause already added by makeSelect
                clauses = null;
            } else {
                Table hierTable = database.getTable(Model.HIER_TABLE_NAME);
                Join join = new Join(Join.INNER, hierTable.getQuotedName(), null, null,
                        hierTable.getColumn(Model.MAIN_KEY), table.getColumn(Model.MAIN_KEY));
                from += join.toSql(dialect);
                String clause = getSoftDeleteClause();
                clauses = clause == null ? null : Collections.singletonList(clause);
            }
            if (selType.criterionKey == null) {
                selectAll = makeSelect(table, from, clauses, NO_ORDER_BY, selType.selKey);
                selectFiltered = makeSelect(table, from, clauses, NO_ORDER_BY, selType.selKey, selType.filterKey);
            } else {
                selectAll = makeSelect(table, from, clauses, NO_ORDER_BY, selType.selKey, selType.criterionKey);
                selectFiltered = makeSelect(table, from, clauses, NO_ORDER_BY, selType.selKey, selType.filterKey,
                        selType.criterionKey);
            }
            this.selectAll = selectAll;
            this.selectFiltered = selectFiltered;
        }
    }

    /**
     * Knows how to build a result map for a row given a {@link ResultSet}. This abstraction may be used to compute some
     * values on the fly.
     */
    public interface MapMaker {
        Map<String, Serializable> makeMap(ResultSet rs) throws SQLException;
    }

    /**
     * Builds the map from a result set given a list of columns and column keys.
     */
    public static class ColumnMapMaker implements MapMaker {
        public final List<Column> columns;

        public final List<String> keys;

        public ColumnMapMaker(List<Column> columns) {
            this.columns = columns;
            this.keys = columns.stream().map(Column::getKey).collect(Collectors.toList());
        }

        public ColumnMapMaker(List<Column> columns, List<String> keys) {
            this.columns = columns;
            this.keys = keys;
        }

        @Override
        public Map<String, Serializable> makeMap(ResultSet rs) throws SQLException {
            Map<String, Serializable> map = new HashMap<>();
            int i = 1;
            for (Column column : columns) {
                String key = keys.get(i - 1);
                Serializable value = column.getFromResultSet(rs, i++);
                if (NXQL.ECM_UUID.equals(key) || NXQL.ECM_PARENTID.equals(key)) {
                    value = String.valueOf(value); // idToString
                }
                map.put(key, value);
            }
            return map;
        }
    }

    private static String[] NO_ORDER_BY = new String[0];

    /**
     * Basic SELECT x, y, z FROM table WHERE a = ? AND b = ?
     * <p>
     * with optional ORDER BY x, y DESC
     */
    public SQLInfoSelect makeSelect(Table table, String[] orderBys, String... freeColumns) {
        return makeSelect(table, null, null, orderBys, freeColumns);
    }

    /**
     * Same as above but the FROM can be passed in, to allow JOINs.
     */
    public SQLInfoSelect makeSelect(Table table, String from, List<String> clauses, String[] orderBys,
            String... freeColumns) {
        boolean fullQuotedName = from != null;
        List<String> freeColumnsList = Arrays.asList(freeColumns);
        List<Column> whatColumns = new LinkedList<>();
        List<Column> whereColumns = new LinkedList<>();
        List<Column> opaqueColumns = new LinkedList<>();
        List<String> whats = new LinkedList<>();
        List<String> wheres = new LinkedList<>();
        for (Column column : table.getColumns()) {
            String qname = fullQuotedName ? column.getFullQuotedName() : column.getQuotedName();
            if (freeColumnsList.contains(column.getKey())) {
                whereColumns.add(column);
                wheres.add(qname + " = ?");
            } else if (column.isOpaque()) {
                opaqueColumns.add(column);
            } else {
                whatColumns.add(column);
                whats.add(qname);
            }
        }
        if (whats.isEmpty()) {
            // only opaque columns, don't generate an illegal SELECT
            whats.add(table.getColumn(Model.MAIN_KEY).getQuotedName());
        }
        if (clauses != null) {
            wheres.addAll(clauses);
        }
        Select select = new Select(table);
        select.setWhat(String.join(", ", whats));
        if (from == null) {
            from = table.getQuotedName();
        }
        select.setFrom(from);
        String where = String.join(" AND ", wheres) + getSoftDeleteClause(table.getKey());
        select.setWhere(where);
        List<String> orders = new LinkedList<>();
        for (int i = 0; i < orderBys.length; i++) {
            String name = orderBys[i++];
            String ascdesc = orderBys[i].equals(ORDER_DESC) ? " " + ORDER_DESC : "";
            Column col = table.getColumn(name);
            String qcol = fullQuotedName ? col.getFullQuotedName() : col.getQuotedName();
            orders.add(qcol + ascdesc);
        }
        select.setOrderBy(String.join(", ", orders));
        return new SQLInfoSelect(select.getStatement(), whatColumns, whereColumns,
                opaqueColumns.isEmpty() ? null : opaqueColumns);
    }

    public void initSQLStatements(Map<String, Serializable> testProps, List<String> sqlInitFiles) throws IOException {
        sqlStatements = new HashMap<>();
        SQLStatement.read(dialect.getSQLStatementsFilename(), sqlStatements);
        if (sqlInitFiles != null) {
            for (String filename : sqlInitFiles) {
                SQLStatement.read(filename, sqlStatements);
            }
        }
        if (!testProps.isEmpty()) {
            SQLStatement.read(dialect.getTestSQLStatementsFilename(), sqlStatements, true); // DDL time
        }
        sqlStatementsProperties = dialect.getSQLStatementsProperties(model, database);
        if (!testProps.isEmpty()) {
            sqlStatementsProperties.putAll(testProps);
        }
    }

    /**
     * Executes the SQL statements for the given category.
     */
    public void executeSQLStatements(String category, String ddlMode, Connection connection, JDBCLogger logger,
            ListCollector ddlCollector) throws SQLException {
        List<SQLStatement> statements = sqlStatements.get(category);
        if (statements != null) {
            SQLStatement.execute(statements, ddlMode, sqlStatementsProperties, dialect, connection, logger,
                    ddlCollector);
        }
    }

    public int getMaximumArgsForIn() {
        return dialect.getMaximumArgsForIn();
    }

}
