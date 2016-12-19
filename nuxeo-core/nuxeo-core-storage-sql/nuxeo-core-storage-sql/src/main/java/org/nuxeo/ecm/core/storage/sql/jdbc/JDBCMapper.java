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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.storage.sql.jdbc;

import static org.nuxeo.ecm.core.api.ScrollResultImpl.emptyResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.api.ScrollResultImpl;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.sql.ClusterInvalidator;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.ColumnType.WrappedId;
import org.nuxeo.ecm.core.storage.sql.Invalidations;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.RepositoryDescriptor;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.Row;
import org.nuxeo.ecm.core.storage.sql.RowId;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo.SQLInfoSelect;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Database;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.DialectOracle;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.SQLStatement.ListCollector;
import org.nuxeo.runtime.api.Framework;

/**
 * A {@link JDBCMapper} maps objects to and from a JDBC database. It is specific to a given database connection, as it
 * computes statements.
 * <p>
 * The {@link JDBCMapper} does the mapping according to the policy defined by a {@link Model}, and generates SQL
 * statements recorded in the {@link SQLInfo}.
 */
public class JDBCMapper extends JDBCRowMapper implements Mapper {

    private static final Log log = LogFactory.getLog(JDBCMapper.class);

    public static Map<String, Serializable> testProps = new HashMap<>();

    protected static Map<String, CursorResult> cursorResults = new ConcurrentHashMap<>();

    public static final String TEST_UPGRADE = "testUpgrade";

    // property in sql.txt file
    public static final String TEST_UPGRADE_VERSIONS = "testUpgradeVersions";

    public static final String TEST_UPGRADE_LAST_CONTRIBUTOR = "testUpgradeLastContributor";

    public static final String TEST_UPGRADE_LOCKS = "testUpgradeLocks";

    protected TableUpgrader tableUpgrader;

    private final QueryMakerService queryMakerService;

    private final PathResolver pathResolver;

    private final RepositoryImpl repository;

    protected boolean clusteringEnabled;

    protected static final String NOSCROLL_ID = "noscroll";

    /**
     * Creates a new Mapper.
     *
     * @param model the model
     * @param pathResolver the path resolver (used for startswith queries)
     * @param sqlInfo the sql info
     * @param clusterInvalidator the cluster invalidator
     * @param noSharing whether to use no-sharing mode for the connection
     * @param repository the repository
     */
    public JDBCMapper(Model model, PathResolver pathResolver, SQLInfo sqlInfo, ClusterInvalidator clusterInvalidator,
            boolean noSharing, RepositoryImpl repository) {
        super(model, sqlInfo, clusterInvalidator, repository.getInvalidationsPropagator(), noSharing);
        this.pathResolver = pathResolver;
        this.repository = repository;
        clusteringEnabled = clusterInvalidator != null;
        queryMakerService = Framework.getService(QueryMakerService.class);

        tableUpgrader = new TableUpgrader(this);
        tableUpgrader.add(Model.VERSION_TABLE_NAME, Model.VERSION_IS_LATEST_KEY, "upgradeVersions",
                TEST_UPGRADE_VERSIONS);
        tableUpgrader.add("dublincore", "lastContributor", "upgradeLastContributor", TEST_UPGRADE_LAST_CONTRIBUTOR);
        tableUpgrader.add(Model.LOCK_TABLE_NAME, Model.LOCK_OWNER_KEY, "upgradeLocks", TEST_UPGRADE_LOCKS);

    }

    @Override
    public int getTableSize(String tableName) {
        return sqlInfo.getDatabase().getTable(tableName).getColumns().size();
    }

    /*
     * ----- Root -----
     */

    @Override
    public void createDatabase(String ddlMode) {
        // some databases (SQL Server) can't create tables/indexes/etc in a transaction, so suspend it
        try {
            boolean suspend = !connection.getAutoCommit();
            try {
                if (suspend) {
                    connection.setAutoCommit(true);
                }
                createTables(ddlMode);
            } finally {
                if (suspend) {
                    connection.setAutoCommit(false);
                }
            }
        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
    }

    protected String getTableName(String origName) {

        if (dialect instanceof DialectOracle) {
            if (origName.length() > 30) {

                StringBuilder sb = new StringBuilder(origName.length());

                try {
                    MessageDigest digest = MessageDigest.getInstance("MD5");
                    sb.append(origName.substring(0, 15));
                    sb.append('_');

                    digest.update(origName.getBytes());
                    sb.append(Dialect.toHexString(digest.digest()).substring(0, 12));

                    return sb.toString();

                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException("Error", e);
                }
            }
        }

        return origName;
    }

    protected void createTables(String ddlMode) throws SQLException {
        ListCollector ddlCollector = new ListCollector();

        sqlInfo.executeSQLStatements(null, ddlMode, connection, logger, ddlCollector); // for missing category
        sqlInfo.executeSQLStatements("first", ddlMode, connection, logger, ddlCollector);
        sqlInfo.executeSQLStatements("beforeTableCreation", ddlMode, connection, logger, ddlCollector);
        if (testProps.containsKey(TEST_UPGRADE)) {
            // create "old" tables
            sqlInfo.executeSQLStatements("testUpgrade", ddlMode, connection, logger, null); // do not collect
        }

        String schemaName = dialect.getConnectionSchema(connection);
        DatabaseMetaData metadata = connection.getMetaData();
        Set<String> tableNames = findTableNames(metadata, schemaName);
        Database database = sqlInfo.getDatabase();
        Map<String, List<Column>> added = new HashMap<>();

        for (Table table : database.getTables()) {
            String tableName = getTableName(table.getPhysicalName());
            if (!tableNames.contains(tableName.toUpperCase())) {

                /*
                 * Create missing table.
                 */

                ddlCollector.add(table.getCreateSql());
                ddlCollector.addAll(table.getPostCreateSqls(model));

                added.put(table.getKey(), null); // null = table created

                sqlInfo.sqlStatementsProperties.put("create_table_" + tableName.toLowerCase(), Boolean.TRUE);

            } else {

                /*
                 * Get existing columns.
                 */

                Map<String, Integer> columnTypes = new HashMap<>();
                Map<String, String> columnTypeNames = new HashMap<>();
                Map<String, Integer> columnTypeSizes = new HashMap<>();
                try (ResultSet rs = metadata.getColumns(null, schemaName, tableName, "%")) {
                    while (rs.next()) {
                        String schema = rs.getString("TABLE_SCHEM");
                        if (schema != null) { // null for MySQL, doh!
                            if ("INFORMATION_SCHEMA".equals(schema.toUpperCase())) {
                                // H2 returns some system tables (locks)
                                continue;
                            }
                        }
                        String columnName = rs.getString("COLUMN_NAME").toUpperCase();
                        columnTypes.put(columnName, Integer.valueOf(rs.getInt("DATA_TYPE")));
                        columnTypeNames.put(columnName, rs.getString("TYPE_NAME"));
                        columnTypeSizes.put(columnName, Integer.valueOf(rs.getInt("COLUMN_SIZE")));
                    }
                }

                /*
                 * Update types and create missing columns.
                 */

                List<Column> addedColumns = new LinkedList<>();
                for (Column column : table.getColumns()) {
                    String upperName = column.getPhysicalName().toUpperCase();
                    Integer type = columnTypes.remove(upperName);
                    if (type == null) {
                        log.warn("Adding missing column in database: " + column.getFullQuotedName());
                        ddlCollector.add(table.getAddColumnSql(column));
                        ddlCollector.addAll(table.getPostAddSqls(column, model));
                        addedColumns.add(column);
                    } else {
                        int expected = column.getJdbcType();
                        int actual = type.intValue();
                        String actualName = columnTypeNames.get(upperName);
                        Integer actualSize = columnTypeSizes.get(upperName);
                        if (!column.setJdbcType(actual, actualName, actualSize.intValue())) {
                            log.error(String.format("SQL type mismatch for %s: expected %s, database has %s / %s (%s)",
                                    column.getFullQuotedName(), Integer.valueOf(expected), type, actualName,
                                    actualSize));
                        }
                    }
                }
                for (String col : dialect.getIgnoredColumns(table)) {
                    columnTypes.remove(col.toUpperCase());
                }
                if (!columnTypes.isEmpty()) {
                    log.warn("Database contains additional unused columns for table " + table.getQuotedName() + ": "
                            + String.join(", ", columnTypes.keySet()));
                }
                if (!addedColumns.isEmpty()) {
                    if (added.containsKey(table.getKey())) {
                        throw new AssertionError();
                    }
                    added.put(table.getKey(), addedColumns);
                }
            }
        }

        if (testProps.containsKey(TEST_UPGRADE)) {
            // create "old" content in tables
            sqlInfo.executeSQLStatements("testUpgradeOldTables", ddlMode, connection, logger, ddlCollector);
        }

        // run upgrade for each table if added columns or test
        for (Entry<String, List<Column>> en : added.entrySet()) {
            List<Column> addedColumns = en.getValue();
            String tableKey = en.getKey();
            upgradeTable(tableKey, addedColumns, ddlMode, ddlCollector);
        }

        sqlInfo.executeSQLStatements("afterTableCreation", ddlMode, connection, logger, ddlCollector);
        sqlInfo.executeSQLStatements("last", ddlMode, connection, logger, ddlCollector);

        // aclr_permission check for PostgreSQL
        dialect.performAdditionalStatements(connection);

        /*
         * Execute all the collected DDL, or dump it if requested, depending on ddlMode
         */

        // ddlMode may be:
        // ignore (not treated here, nothing done)
        // dump (implies execute)
        // dump,execute
        // dump,ignore (no execute)
        // execute
        // abort (implies dump)
        // compat can be used instead of execute to always recreate stored procedures

        List<String> ddl = ddlCollector.getStrings();
        boolean ignore = ddlMode.contains(RepositoryDescriptor.DDL_MODE_IGNORE);
        boolean dump = ddlMode.contains(RepositoryDescriptor.DDL_MODE_DUMP);
        boolean abort = ddlMode.contains(RepositoryDescriptor.DDL_MODE_ABORT);
        if (dump || abort) {

            /*
             * Dump DDL if not empty.
             */

            if (!ddl.isEmpty()) {
                File dumpFile = new File(Environment.getDefault().getLog(), "ddl-vcs-" + repository.getName() + ".sql");
                try (OutputStream out = new FileOutputStream(dumpFile); PrintStream ps = new PrintStream(out)) {
                    for (String sql : dialect.getDumpStart()) {
                        ps.println(sql);
                    }
                    for (String sql : ddl) {
                        sql = sql.trim();
                        if (sql.endsWith(";")) {
                            sql = sql.substring(0, sql.length() - 1);
                        }
                        ps.println(dialect.getSQLForDump(sql));
                    }
                    for (String sql : dialect.getDumpStop()) {
                        ps.println(sql);
                    }
                } catch (IOException e) {
                    throw new NuxeoException(e);
                }

                /*
                 * Abort if requested.
                 */

                if (abort) {
                    log.error("Dumped DDL to: " + dumpFile);
                    throw new NuxeoException("Database initialization failed for: " + repository.getName()
                            + ", DDL must be executed: " + dumpFile);
                }
            }
        }
        if (!ignore) {

            /*
             * Execute DDL.
             */

            try (Statement st = connection.createStatement()) {
                for (String sql : ddl) {
                    logger.log(sql.replace("\n", "\n    ")); // indented
                    try {
                        st.execute(sql);
                    } catch (SQLException e) {
                        throw new SQLException("Error executing: " + sql + " : " + e.getMessage(), e);
                    }
                    countExecute();
                }
            }

            /*
             * Execute post-DDL stuff.
             */

            try (Statement st = connection.createStatement()) {
                for (String sql : dialect.getStartupSqls(model, sqlInfo.database)) {
                    logger.log(sql.replace("\n", "\n    ")); // indented
                    try {
                        st.execute(sql);
                    } catch (SQLException e) {
                        throw new SQLException("Error executing: " + sql + " : " + e.getMessage(), e);
                    }
                    countExecute();
                }
            }
        }
    }

    protected void upgradeTable(String tableKey, List<Column> addedColumns, String ddlMode, ListCollector ddlCollector)
            throws SQLException {
        tableUpgrader.upgrade(tableKey, addedColumns, ddlMode, ddlCollector);
    }

    /** Finds uppercase table names. */
    protected static Set<String> findTableNames(DatabaseMetaData metadata, String schemaName) throws SQLException {
        Set<String> tableNames = new HashSet<>();
        ResultSet rs = metadata.getTables(null, schemaName, "%", new String[] { "TABLE" });
        while (rs.next()) {
            String tableName = rs.getString("TABLE_NAME");
            tableNames.add(tableName.toUpperCase());
        }
        rs.close();
        return tableNames;
    }

    @Override
    public int getClusterNodeIdType() {
        return sqlInfo.getClusterNodeIdType();
    }

    @Override
    public void createClusterNode(Serializable nodeId) {
        Calendar now = Calendar.getInstance();
        try {
            String sql = sqlInfo.getCreateClusterNodeSql();
            List<Column> columns = sqlInfo.getCreateClusterNodeColumns();
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                if (logger.isLogEnabled()) {
                    logger.logSQL(sql, Arrays.asList(nodeId, now));
                }
                columns.get(0).setToPreparedStatement(ps, 1, nodeId);
                columns.get(1).setToPreparedStatement(ps, 2, now);
                ps.execute();
            } finally {
                closeStatement(ps);
            }
        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public void removeClusterNode(Serializable nodeId) {
        try {
            // delete from cluster_nodes
            String sql = sqlInfo.getDeleteClusterNodeSql();
            Column column = sqlInfo.getDeleteClusterNodeColumn();
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                if (logger.isLogEnabled()) {
                    logger.logSQL(sql, Collections.singletonList(nodeId));
                }
                column.setToPreparedStatement(ps, 1, nodeId);
                ps.execute();
            } finally {
                closeStatement(ps);
            }
            // delete un-processed invals from cluster_invals
            deleteClusterInvals(nodeId);
        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
    }

    protected void deleteClusterInvals(Serializable nodeId) throws SQLException {
        String sql = sqlInfo.getDeleteClusterInvalsSql();
        Column column = sqlInfo.getDeleteClusterInvalsColumn();
        PreparedStatement ps = connection.prepareStatement(sql);
        try {
            if (logger.isLogEnabled()) {
                logger.logSQL(sql, Collections.singletonList(nodeId));
            }
            column.setToPreparedStatement(ps, 1, nodeId);
            int n = ps.executeUpdate();
            countExecute();
            if (logger.isLogEnabled()) {
                logger.logCount(n);
            }
        } finally {
            try {
                closeStatement(ps);
            } catch (SQLException e) {
                log.error("deleteClusterInvals: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public void insertClusterInvalidations(Serializable nodeId, Invalidations invalidations) {
        String sql = dialect.getClusterInsertInvalidations();
        List<Column> columns = sqlInfo.getClusterInvalidationsColumns();
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
            int kind = Invalidations.MODIFIED;
            while (true) {
                Set<RowId> rowIds = invalidations.getKindSet(kind);

                // reorganize by id
                Map<Serializable, Set<String>> res = new HashMap<>();
                for (RowId rowId : rowIds) {
                    Set<String> tableNames = res.get(rowId.id);
                    if (tableNames == null) {
                        res.put(rowId.id, tableNames = new HashSet<>());
                    }
                    tableNames.add(rowId.tableName);
                }

                // do inserts
                for (Entry<Serializable, Set<String>> en : res.entrySet()) {
                    Serializable id = en.getKey();
                    String fragments = join(en.getValue(), ' ');
                    if (logger.isLogEnabled()) {
                        logger.logSQL(sql, Arrays.<Serializable> asList(nodeId, id, fragments, Long.valueOf(kind)));
                    }
                    Serializable frags;
                    if (dialect.supportsArrays() && columns.get(2).getJdbcType() == Types.ARRAY) {
                        frags = fragments.split(" ");
                    } else {
                        frags = fragments;
                    }
                    columns.get(0).setToPreparedStatement(ps, 1, nodeId);
                    columns.get(1).setToPreparedStatement(ps, 2, id);
                    columns.get(2).setToPreparedStatement(ps, 3, frags);
                    columns.get(3).setToPreparedStatement(ps, 4, Long.valueOf(kind));
                    ps.execute();
                    countExecute();
                }
                if (kind == Invalidations.MODIFIED) {
                    kind = Invalidations.DELETED;
                } else {
                    break;
                }
            }
        } catch (SQLException e) {
            throw new NuxeoException("Could not invalidate", e);
        } finally {
            try {
                closeStatement(ps);
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    // join that works on a set
    protected static String join(Collection<String> strings, char sep) {
        if (strings.isEmpty()) {
            throw new RuntimeException();
        }
        if (strings.size() == 1) {
            return strings.iterator().next();
        }
        int size = 0;
        for (String word : strings) {
            size += word.length() + 1;
        }
        StringBuilder buf = new StringBuilder(size);
        for (String word : strings) {
            buf.append(word);
            buf.append(sep);
        }
        buf.setLength(size - 1);
        return buf.toString();
    }

    @Override
    public Invalidations getClusterInvalidations(Serializable nodeId) {
        Invalidations invalidations = new Invalidations();
        String sql = dialect.getClusterGetInvalidations();
        List<Column> columns = sqlInfo.getClusterInvalidationsColumns();
        try {
            if (logger.isLogEnabled()) {
                logger.logSQL(sql, Collections.singletonList(nodeId));
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = null;
            try {
                setToPreparedStatement(ps, 1, nodeId);
                rs = ps.executeQuery();
                countExecute();
                while (rs.next()) {
                    // first column ignored, it's the node id
                    Serializable id = columns.get(1).getFromResultSet(rs, 1);
                    Serializable frags = columns.get(2).getFromResultSet(rs, 2);
                    int kind = ((Long) columns.get(3).getFromResultSet(rs, 3)).intValue();
                    String[] fragments;
                    if (dialect.supportsArrays() && frags instanceof String[]) {
                        fragments = (String[]) frags;
                    } else {
                        fragments = ((String) frags).split(" ");
                    }
                    invalidations.add(id, fragments, kind);
                }
            } finally {
                closeStatement(ps, rs);
            }
            if (logger.isLogEnabled()) {
                // logCount(n);
                logger.log("  -> " + invalidations);
            }
            if (dialect.isClusteringDeleteNeeded()) {
                deleteClusterInvals(nodeId);
            }
            return invalidations;
        } catch (SQLException e) {
            throw new NuxeoException("Could not invalidate", e);
        }
    }

    @Override
    public Serializable getRootId(String repositoryId) {
        String sql = sqlInfo.getSelectRootIdSql();
        try {
            if (logger.isLogEnabled()) {
                logger.logSQL(sql, Collections.<Serializable> singletonList(repositoryId));
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            ResultSet rs = null;
            try {
                ps.setString(1, repositoryId);
                rs = ps.executeQuery();
                countExecute();
                if (!rs.next()) {
                    if (logger.isLogEnabled()) {
                        logger.log("  -> (none)");
                    }
                    return null;
                }
                Column column = sqlInfo.getSelectRootIdWhatColumn();
                Serializable id = column.getFromResultSet(rs, 1);
                if (logger.isLogEnabled()) {
                    logger.log("  -> " + Model.MAIN_KEY + '=' + id);
                }
                // check that we didn't get several rows
                if (rs.next()) {
                    throw new NuxeoException("Row query for " + repositoryId + " returned several rows: " + sql);
                }
                return id;
            } finally {
                closeStatement(ps, rs);
            }
        } catch (SQLException e) {
            throw new NuxeoException("Could not select: " + sql, e);
        }
    }

    @Override
    public void setRootId(Serializable repositoryId, Serializable id) {
        String sql = sqlInfo.getInsertRootIdSql();
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                List<Column> columns = sqlInfo.getInsertRootIdColumns();
                List<Serializable> debugValues = null;
                if (logger.isLogEnabled()) {
                    debugValues = new ArrayList<>(2);
                }
                int i = 0;
                for (Column column : columns) {
                    i++;
                    String key = column.getKey();
                    Serializable v;
                    if (key.equals(Model.MAIN_KEY)) {
                        v = id;
                    } else if (key.equals(Model.REPOINFO_REPONAME_KEY)) {
                        v = repositoryId;
                    } else {
                        throw new RuntimeException(key);
                    }
                    column.setToPreparedStatement(ps, i, v);
                    if (debugValues != null) {
                        debugValues.add(v);
                    }
                }
                if (debugValues != null) {
                    logger.logSQL(sql, debugValues);
                    debugValues.clear();
                }
                ps.execute();
                countExecute();
            } finally {
                closeStatement(ps);
            }
        } catch (SQLException e) {
            throw new NuxeoException("Could not insert: " + sql, e);
        }
    }

    protected QueryMaker findQueryMaker(String queryType) {
        for (Class<? extends QueryMaker> klass : queryMakerService.getQueryMakers()) {
            QueryMaker queryMaker;
            try {
                queryMaker = klass.newInstance();
            } catch (ReflectiveOperationException e) {
                throw new NuxeoException(e);
            }
            if (queryMaker.accepts(queryType)) {
                return queryMaker;
            }
        }
        return null;
    }

    protected void prepareUserReadAcls(QueryFilter queryFilter) {
        String sql = dialect.getPrepareUserReadAclsSql();
        Serializable principals = queryFilter.getPrincipals();
        if (sql == null || principals == null) {
            return;
        }
        if (!dialect.supportsArrays()) {
            principals = String.join(Dialect.ARRAY_SEP, (String[]) principals);
        }
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
            if (logger.isLogEnabled()) {
                logger.logSQL(sql, Collections.singleton(principals));
            }
            setToPreparedStatement(ps, 1, principals);
            ps.execute();
            countExecute();
        } catch (SQLException e) {
            throw new NuxeoException("Failed to prepare user read acl cache", e);
        } finally {
            try {
                closeStatement(ps);
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public PartialList<Serializable> query(String query, String queryType, QueryFilter queryFilter,
            boolean countTotal) {
        return query(query, queryType, queryFilter, countTotal ? -1 : 0);
    }

    @Override
    public PartialList<Serializable> query(String query, String queryType, QueryFilter queryFilter, long countUpTo) {
        if (dialect.needsPrepareUserReadAcls()) {
            prepareUserReadAcls(queryFilter);
        }
        QueryMaker queryMaker = findQueryMaker(queryType);
        if (queryMaker == null) {
            throw new NuxeoException("No QueryMaker accepts query: " + queryType + ": " + query);
        }
        QueryMaker.Query q = queryMaker.buildQuery(sqlInfo, model, pathResolver, query, queryFilter);

        if (q == null) {
            logger.log("Query cannot return anything due to conflicting clauses");
            return new PartialList<>(Collections.<Serializable> emptyList(), 0);
        }
        long limit = queryFilter.getLimit();
        long offset = queryFilter.getOffset();

        if (logger.isLogEnabled()) {
            String sql = q.selectInfo.sql;
            if (limit != 0) {
                sql += " -- LIMIT " + limit + " OFFSET " + offset;
            }
            if (countUpTo != 0) {
                sql += " -- COUNT TOTAL UP TO " + countUpTo;
            }
            logger.logSQL(sql, q.selectParams);
        }

        String sql = q.selectInfo.sql;

        if (countUpTo == 0 && limit > 0 && dialect.supportsPaging()) {
            // full result set not needed for counting
            sql = dialect.addPagingClause(sql, limit, offset);
            limit = 0;
            offset = 0;
        } else if (countUpTo > 0 && dialect.supportsPaging()) {
            // ask one more row
            sql = dialect.addPagingClause(sql, Math.max(countUpTo + 1, limit + offset), 0);
        }

        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = connection.prepareStatement(sql, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            int i = 1;
            for (Serializable object : q.selectParams) {
                setToPreparedStatement(ps, i++, object);
            }
            rs = ps.executeQuery();
            countExecute();

            // limit/offset
            long totalSize = -1;
            boolean available;
            if ((limit == 0) || (offset == 0)) {
                available = rs.first();
                if (!available) {
                    totalSize = 0;
                }
                if (limit == 0) {
                    limit = -1; // infinite
                }
            } else {
                available = rs.absolute((int) offset + 1);
            }

            Column column = q.selectInfo.whatColumns.get(0);
            List<Serializable> ids = new LinkedList<>();
            int rowNum = 0;
            while (available && (limit != 0)) {
                Serializable id = column.getFromResultSet(rs, 1);
                ids.add(id);
                rowNum = rs.getRow();
                available = rs.next();
                limit--;
            }

            // total size
            if (countUpTo != 0 && (totalSize == -1)) {
                if (!available && (rowNum != 0)) {
                    // last row read was the actual last
                    totalSize = rowNum;
                } else {
                    // available if limit reached with some left
                    // rowNum == 0 if skipped too far
                    rs.last();
                    totalSize = rs.getRow();
                }
                if (countUpTo > 0 && totalSize > countUpTo) {
                    // the result where truncated we don't know the total size
                    totalSize = -2;
                }
            }

            if (logger.isLogEnabled()) {
                logger.logIds(ids, countUpTo != 0, totalSize);
            }

            return new PartialList<>(ids, totalSize);
        } catch (SQLException e) {
            throw new NuxeoException("Invalid query: " + query, e);
        } finally {
            try {
                closeStatement(ps, rs);
            } catch (SQLException e) {
                log.error("Cannot close connection", e);
            }
        }
    }

    public int setToPreparedStatement(PreparedStatement ps, int i, Serializable object) throws SQLException {
        if (object instanceof Calendar) {
            Calendar cal = (Calendar) object;
            ps.setTimestamp(i, dialect.getTimestampFromCalendar(cal), cal);
        } else if (object instanceof java.sql.Date) {
            ps.setDate(i, (java.sql.Date) object);
        } else if (object instanceof Long) {
            ps.setLong(i, ((Long) object).longValue());
        } else if (object instanceof WrappedId) {
            dialect.setId(ps, i, object.toString());
        } else if (object instanceof Object[]) {
            int jdbcType;
            if (object instanceof String[]) {
                jdbcType = dialect.getJDBCTypeAndString(ColumnType.STRING).jdbcType;
            } else if (object instanceof Boolean[]) {
                jdbcType = dialect.getJDBCTypeAndString(ColumnType.BOOLEAN).jdbcType;
            } else if (object instanceof Long[]) {
                jdbcType = dialect.getJDBCTypeAndString(ColumnType.LONG).jdbcType;
            } else if (object instanceof Double[]) {
                jdbcType = dialect.getJDBCTypeAndString(ColumnType.DOUBLE).jdbcType;
            } else if (object instanceof java.sql.Date[]) {
                jdbcType = Types.DATE;
            } else if (object instanceof java.sql.Clob[]) {
                jdbcType = Types.CLOB;
            } else if (object instanceof Calendar[]) {
                jdbcType = dialect.getJDBCTypeAndString(ColumnType.TIMESTAMP).jdbcType;
                object = dialect.getTimestampFromCalendar((Calendar) object);
            } else if (object instanceof Integer[]) {
                jdbcType = dialect.getJDBCTypeAndString(ColumnType.INTEGER).jdbcType;
            } else {
                jdbcType = dialect.getJDBCTypeAndString(ColumnType.CLOB).jdbcType;
            }
            Array array = dialect.createArrayOf(jdbcType, (Object[]) object, connection);
            ps.setArray(i, array);
        } else {
            ps.setObject(i, object);
        }
        return i;
    }

    // queryFilter used for principals and permissions
    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType, QueryFilter queryFilter,
            boolean distinctDocuments, Object... params) {
        if (dialect.needsPrepareUserReadAcls()) {
            prepareUserReadAcls(queryFilter);
        }
        QueryMaker queryMaker = findQueryMaker(queryType);
        if (queryMaker == null) {
            throw new NuxeoException("No QueryMaker accepts query: " + queryType + ": " + query);
        }
        if (distinctDocuments) {
            String q = query.toLowerCase();
            if (q.startsWith("select ") && !q.startsWith("select distinct ")) {
                query = "SELECT DISTINCT " + query.substring("SELECT ".length());
            }
        }
        try {
            return new ResultSetQueryResult(queryMaker, query, queryFilter, pathResolver, this, params);
        } catch (SQLException e) {
            throw new NuxeoException("Invalid query: " + queryType + ": " + query, e);
        }
    }

    @Override
    public ScrollResult scroll(String query, int batchSize, int keepAliveSeconds) {
        if (!dialect.supportsScroll()) {
            return defaultScroll(query);
        }
        checkForTimedoutScroll();
        return scrollSearch(query, batchSize, keepAliveSeconds);
    }

    protected void checkForTimedoutScroll() {
        cursorResults.forEach((id, cursor) -> cursor.timedOut(id));
    }

    protected ScrollResult scrollSearch(String query, int batchSize, int keepAliveSeconds) {
        QueryMaker queryMaker = findQueryMaker("NXQL");
        QueryFilter queryFilter = new QueryFilter(null, null, null, null, Collections.emptyList(), 0, 0);
        QueryMaker.Query q = queryMaker.buildQuery(sqlInfo, model, pathResolver, query, queryFilter, null);
        if (q == null) {
            logger.log("Query cannot return anything due to conflicting clauses");
            throw new NuxeoException("Query cannot return anything due to conflicting clauses");
        }
        if (logger.isLogEnabled()) {
            logger.logSQL(q.selectInfo.sql, q.selectParams);
        }
        try {
            if (connection.getAutoCommit()) {
                throw new NuxeoException("Scroll should be done inside a transaction");
            }
            PreparedStatement ps = connection.prepareStatement(q.selectInfo.sql, ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT);
            ps.setFetchSize(batchSize);
            int i = 1;
            for (Serializable object : q.selectParams) {
                setToPreparedStatement(ps, i++, object);
            }
            ResultSet rs = ps.executeQuery();
            String scrollId = UUID.randomUUID().toString();
            registerCursor(scrollId, ps, rs, batchSize, keepAliveSeconds);
            return scroll(scrollId);
        } catch (SQLException e) {
            throw new NuxeoException("Error on query", e);
        }
    }

    protected class CursorResult {
        protected final int keepAliveSeconds;

        protected final PreparedStatement preparedStatement;

        protected final ResultSet resultSet;

        protected final int batchSize;

        protected long lastCallTimestamp;

        CursorResult(PreparedStatement preparedStatement, ResultSet resultSet, int batchSize, int keepAliveSeconds) {
            this.preparedStatement = preparedStatement;
            this.resultSet = resultSet;
            this.batchSize = batchSize;
            this.keepAliveSeconds = keepAliveSeconds;
            lastCallTimestamp = System.currentTimeMillis();
        }

        boolean timedOut(String scrollId) {
            long now = System.currentTimeMillis();
            if (now - lastCallTimestamp > (keepAliveSeconds * 1000)) {
                if (unregisterCursor(scrollId)) {
                    log.warn("Scroll " + scrollId + " timed out");
                }
                return true;
            }
            return false;
        }

        void touch() {
            lastCallTimestamp = System.currentTimeMillis();
        }

        synchronized void close() throws SQLException {
            if (resultSet != null) {
                resultSet.close();
            }
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    protected void registerCursor(String scrollId, PreparedStatement ps, ResultSet rs, int batchSize,
            int keepAliveSeconds) {
        cursorResults.put(scrollId, new CursorResult(ps, rs, batchSize, keepAliveSeconds));
    }

    protected boolean unregisterCursor(String scrollId) {
        CursorResult cursor = cursorResults.remove(scrollId);
        if (cursor != null) {
            try {
                cursor.close();
                return true;
            } catch (SQLException e) {
                log.error("Failed to close cursor for scroll: " + scrollId, e);
                // do not propagate exception on cleaning
            }
        }
        return false;
    }

    protected ScrollResult defaultScroll(String query) {
        // the database has no proper support for cursor just return everything in one batch
        QueryMaker queryMaker = findQueryMaker("NXQL");
        List<String> ids;
        QueryFilter queryFilter = new QueryFilter(null, null, null, null, Collections.emptyList(), 0, 0);
        try (IterableQueryResult ret = new ResultSetQueryResult(queryMaker, query, queryFilter, pathResolver, this,
                null)) {
            ids = new ArrayList<>((int) ret.size());
            for (Map<String, Serializable> map : ret) {
                ids.add(map.get("ecm:uuid").toString());
            }
        } catch (SQLException e) {
            throw new NuxeoException("Invalid scroll query: " + query, e);
        }
        return new ScrollResultImpl(NOSCROLL_ID, ids);
    }

    @Override
    public ScrollResult scroll(String scrollId) {
        if (NOSCROLL_ID.equals(scrollId) || !dialect.supportsScroll()) {
            // there is only one batch in this case
            return emptyResult();
        }
        CursorResult cursorResult = cursorResults.get(scrollId);
        if (cursorResult == null) {
            throw new NuxeoException("Unknown or timed out scrollId");
        } else if (cursorResult.timedOut(scrollId)) {
            throw new NuxeoException("Timed out scrollId");
        }
        cursorResult.touch();
        List<String> ids = new ArrayList<>(cursorResult.batchSize);
        synchronized (cursorResult) {
            try {
                if (cursorResult.resultSet == null || cursorResult.resultSet.isClosed()) {
                    unregisterCursor(scrollId);
                    return emptyResult();
                }
                while (ids.size() < cursorResult.batchSize) {
                    if (cursorResult.resultSet.next()) {
                        ids.add(cursorResult.resultSet.getString(1));
                    } else {
                        cursorResult.close();
                        if (ids.isEmpty()) {
                            unregisterCursor(scrollId);
                        }
                        break;
                    }
                }
            } catch (SQLException e) {
                throw new NuxeoException("Error during scroll", e);
            }
        }
        return new ScrollResultImpl(scrollId, ids);
    }

    @Override
    public Set<Serializable> getAncestorsIds(Collection<Serializable> ids) {
        SQLInfoSelect select = sqlInfo.getSelectAncestorsIds();
        if (select == null) {
            return getAncestorsIdsIterative(ids);
        }
        Serializable whereIds = newIdArray(ids);
        Set<Serializable> res = new HashSet<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            if (logger.isLogEnabled()) {
                logger.logSQL(select.sql, Collections.singleton(whereIds));
            }
            Column what = select.whatColumns.get(0);
            ps = connection.prepareStatement(select.sql);
            setToPreparedStatementIdArray(ps, 1, whereIds);
            rs = ps.executeQuery();
            countExecute();
            List<Serializable> debugIds = null;
            if (logger.isLogEnabled()) {
                debugIds = new LinkedList<>();
            }
            while (rs.next()) {
                if (dialect.supportsArraysReturnInsteadOfRows()) {
                    Serializable[] resultIds = dialect.getArrayResult(rs.getArray(1));
                    for (Serializable id : resultIds) {
                        if (id != null) {
                            res.add(id);
                            if (logger.isLogEnabled()) {
                                debugIds.add(id);
                            }
                        }
                    }
                } else {
                    Serializable id = what.getFromResultSet(rs, 1);
                    if (id != null) {
                        res.add(id);
                        if (logger.isLogEnabled()) {
                            debugIds.add(id);
                        }
                    }
                }
            }
            if (logger.isLogEnabled()) {
                logger.logIds(debugIds, false, 0);
            }
            return res;
        } catch (SQLException e) {
            throw new NuxeoException("Failed to get ancestors ids", e);
        } finally {
            try {
                closeStatement(ps, rs);
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Uses iterative parentid selection.
     */
    protected Set<Serializable> getAncestorsIdsIterative(Collection<Serializable> ids) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            LinkedList<Serializable> todo = new LinkedList<>(ids);
            Set<Serializable> done = new HashSet<>();
            Set<Serializable> res = new HashSet<>();
            while (!todo.isEmpty()) {
                done.addAll(todo);
                SQLInfoSelect select = sqlInfo.getSelectParentIds(todo.size());
                if (logger.isLogEnabled()) {
                    logger.logSQL(select.sql, todo);
                }
                Column what = select.whatColumns.get(0);
                Column where = select.whereColumns.get(0);
                ps = connection.prepareStatement(select.sql);
                int i = 1;
                for (Serializable id : todo) {
                    where.setToPreparedStatement(ps, i++, id);
                }
                rs = ps.executeQuery();
                countExecute();
                todo = new LinkedList<>();
                List<Serializable> debugIds = null;
                if (logger.isLogEnabled()) {
                    debugIds = new LinkedList<>();
                }
                while (rs.next()) {
                    Serializable id = what.getFromResultSet(rs, 1);
                    if (id != null) {
                        res.add(id);
                        if (!done.contains(id)) {
                            todo.add(id);
                        }
                        if (logger.isLogEnabled()) {
                            debugIds.add(id);
                        }
                    }
                }
                if (logger.isLogEnabled()) {
                    logger.logIds(debugIds, false, 0);
                }
                rs.close();
                ps.close();
            }
            return res;
        } catch (SQLException e) {
            throw new NuxeoException("Failed to get ancestors ids", e);
        } finally {
            try {
                closeStatement(ps, rs);
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void updateReadAcls() {
        if (!dialect.supportsReadAcl()) {
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("updateReadAcls: updating");
        }
        Statement st = null;
        try {
            st = connection.createStatement();
            String sql = dialect.getUpdateReadAclsSql();
            if (logger.isLogEnabled()) {
                logger.log(sql);
            }
            st.execute(sql);
            countExecute();
        } catch (SQLException e) {
            checkConcurrentUpdate(e);
            throw new NuxeoException("Failed to update read acls", e);
        } finally {
            try {
                closeStatement(st);
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("updateReadAcls: done.");
        }
    }

    @Override
    public void rebuildReadAcls() {
        if (!dialect.supportsReadAcl()) {
            return;
        }
        log.debug("rebuildReadAcls: rebuilding ...");
        Statement st = null;
        try {
            st = connection.createStatement();
            String sql = dialect.getRebuildReadAclsSql();
            logger.log(sql);
            st.execute(sql);
            countExecute();
        } catch (SQLException e) {
            throw new NuxeoException("Failed to rebuild read acls", e);
        } finally {
            try {
                closeStatement(st);
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
        log.debug("rebuildReadAcls: done.");
    }

    /*
     * ----- Locking -----
     */

    protected Connection connection(boolean autocommit) {
        try {
            connection.setAutoCommit(autocommit);
        } catch (SQLException e) {
            throw new NuxeoException("Cannot set auto commit mode onto " + this + "'s connection", e);
        }
        return connection;
    }

    /**
     * Calls the callable, inside a transaction if in cluster mode.
     * <p>
     * Called under {@link #serializationLock}.
     */
    protected Lock callInTransaction(LockCallable callable, boolean tx) {
        boolean ok = false;
        try {
            if (log.isDebugEnabled()) {
                log.debug("callInTransaction setAutoCommit " + !tx);
            }
            connection.setAutoCommit(!tx);
        } catch (SQLException e) {
            throw new NuxeoException("Cannot set auto commit mode onto " + this + "'s connection", e);
        }
        try {
            Lock result = callable.call();
            ok = true;
            return result;
        } finally {
            if (tx) {
                try {
                    try {
                        if (ok) {
                            if (log.isDebugEnabled()) {
                                log.debug("callInTransaction commit");
                            }
                            connection.commit();
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("callInTransaction rollback");
                            }
                            connection.rollback();
                        }
                    } finally {
                        // restore autoCommit=true
                        if (log.isDebugEnabled()) {
                            log.debug("callInTransaction restoring autoCommit=true");
                        }
                        connection.setAutoCommit(true);
                    }
                } catch (SQLException e) {
                    throw new NuxeoException(e);
                }
            }
        }
    }

    public interface LockCallable extends Callable<Lock> {
        @Override
        Lock call();
    }

    @Override
    public Lock getLock(Serializable id) {
        if (log.isDebugEnabled()) {
            try {
                log.debug("getLock " + id + " while autoCommit=" + connection.getAutoCommit());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        RowId rowId = new RowId(Model.LOCK_TABLE_NAME, id);
        Row row = readSimpleRow(rowId);
        return row == null ? null
                : new Lock((String) row.get(Model.LOCK_OWNER_KEY), (Calendar) row.get(Model.LOCK_CREATED_KEY));
    }

    @Override
    public Lock setLock(final Serializable id, final Lock lock) {
        if (log.isDebugEnabled()) {
            log.debug("setLock " + id + " owner=" + lock.getOwner());
        }
        SetLock call = new SetLock(id, lock);
        return callInTransaction(call, clusteringEnabled);
    }

    protected class SetLock implements LockCallable {
        protected final Serializable id;

        protected final Lock lock;

        protected SetLock(Serializable id, Lock lock) {
            super();
            this.id = id;
            this.lock = lock;
        }

        @Override
        public Lock call() {
            Lock oldLock = getLock(id);
            if (oldLock == null) {
                Row row = new Row(Model.LOCK_TABLE_NAME, id);
                row.put(Model.LOCK_OWNER_KEY, lock.getOwner());
                row.put(Model.LOCK_CREATED_KEY, lock.getCreated());
                insertSimpleRows(Model.LOCK_TABLE_NAME, Collections.singletonList(row));
            }
            return oldLock;
        }
    }

    @Override
    public Lock removeLock(final Serializable id, final String owner, final boolean force) {
        if (log.isDebugEnabled()) {
            log.debug("removeLock " + id + " owner=" + owner + " force=" + force);
        }
        RemoveLock call = new RemoveLock(id, owner, force);
        return callInTransaction(call, !force);
    }

    protected class RemoveLock implements LockCallable {
        protected final Serializable id;

        protected final String owner;

        protected final boolean force;

        protected RemoveLock(Serializable id, String owner, boolean force) {
            super();
            this.id = id;
            this.owner = owner;
            this.force = force;
        }

        @Override
        public Lock call() {
            Lock oldLock = force ? null : getLock(id);
            if (!force && owner != null) {
                if (oldLock == null) {
                    // not locked, nothing to do
                    return null;
                }
                if (!LockManager.canLockBeRemoved(oldLock.getOwner(), owner)) {
                    // existing mismatched lock, flag failure
                    return new Lock(oldLock, true);
                }
            }
            if (force || oldLock != null) {
                deleteRows(Model.LOCK_TABLE_NAME, Collections.singleton(id));
            }
            return oldLock;
        }
    }

    @Override
    public void markReferencedBinaries() {
        log.debug("Starting binaries GC mark");
        Statement st = null;
        ResultSet rs = null;
        BlobManager blobManager = Framework.getService(BlobManager.class);
        String repositoryName = getRepositoryName();
        try {
            st = connection.createStatement();
            int i = -1;
            for (String sql : sqlInfo.getBinariesSql) {
                i++;
                Column col = sqlInfo.getBinariesColumns.get(i);
                if (logger.isLogEnabled()) {
                    logger.log(sql);
                }
                rs = st.executeQuery(sql);
                countExecute();
                int n = 0;
                while (rs.next()) {
                    n++;
                    String key = (String) col.getFromResultSet(rs, 1);
                    if (key != null) {
                        blobManager.markReferencedBinary(key, repositoryName);
                    }
                }
                if (logger.isLogEnabled()) {
                    logger.logCount(n);
                }
                rs.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to mark binaries for gC", e);
        } finally {
            try {
                closeStatement(st, rs);
            } catch (SQLException e) {
                log.error(e.getMessage(), e);
            }
        }
        log.debug("End of binaries GC mark");
    }

    /*
     * ----- XAResource -----
     */

    protected static String systemToString(Object o) {
        return o.getClass().getName() + "@" + Integer.toHexString(System.identityHashCode(o));
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        try {
            xaresource.start(xid, flags);
            if (logger.isLogEnabled()) {
                logger.log("XA start on " + systemToString(xid));
            }
        } catch (NuxeoException e) {
            throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
        } catch (XAException e) {
            logger.error("XA start error on " + systemToString(xid), e);
            throw e;
        }
    }

    @Override
    public void end(Xid xid, int flags) throws XAException {
        try {
            xaresource.end(xid, flags);
            if (logger.isLogEnabled()) {
                logger.log("XA end on " + systemToString(xid));
            }
        } catch (NullPointerException e) {
            // H2 when no active transaction
            logger.error("XA end error on " + systemToString(xid), e);
            throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
        } catch (XAException e) {
            if (flags != XAResource.TMFAIL) {
                logger.error("XA end error on " + systemToString(xid), e);
            }
            throw e;
        }
    }

    @Override
    public int prepare(Xid xid) throws XAException {
        try {
            return xaresource.prepare(xid);
        } catch (XAException e) {
            logger.error("XA prepare error on  " + systemToString(xid), e);
            throw e;
        }
    }

    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {
        try {
            xaresource.commit(xid, onePhase);
        } catch (XAException e) {
            logger.error("XA commit error on  " + systemToString(xid), e);
            throw e;
        }
    }

    // rollback interacts with caches so is in RowMapper

    @Override
    public void forget(Xid xid) throws XAException {
        xaresource.forget(xid);
    }

    @Override
    public Xid[] recover(int flag) throws XAException {
        return xaresource.recover(flag);
    }

    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        return xaresource.setTransactionTimeout(seconds);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return xaresource.getTransactionTimeout();
    }

    @Override
    public boolean isSameRM(XAResource xares) throws XAException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConnected() {
        return connection != null;
    }

    @Override
    public void connect() {
        openConnections();
    }

    @Override
    public void disconnect() {
        closeConnections();
    }

}
