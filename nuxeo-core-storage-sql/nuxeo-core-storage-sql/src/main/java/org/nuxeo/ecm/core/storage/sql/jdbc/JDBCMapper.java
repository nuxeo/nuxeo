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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.io.Serializable;
import java.security.MessageDigest;
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
import java.util.concurrent.Callable;

import javax.resource.ResourceException;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.ConnectionResetException;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.binary.BinaryGarbageCollector;
import org.nuxeo.ecm.core.storage.sql.ColumnType;
import org.nuxeo.ecm.core.storage.sql.ColumnType.WrappedId;
import org.nuxeo.ecm.core.storage.sql.Invalidations;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Model;
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
import org.nuxeo.runtime.api.Framework;

/**
 * A {@link JDBCMapper} maps objects to and from a JDBC database. It is specific
 * to a given database connection, as it computes statements.
 * <p>
 * The {@link JDBCMapper} does the mapping according to the policy defined by a
 * {@link Model}, and generates SQL statements recorded in the {@link SQLInfo}.
 */
public class JDBCMapper extends JDBCRowMapper implements Mapper {

    private static final Log log = LogFactory.getLog(JDBCMapper.class);

    public static Map<String, Serializable> testProps = new HashMap<String, Serializable>();

    public static final String TEST_UPGRADE = "testUpgrade";

    // property in sql.txt file
    public static final String TEST_UPGRADE_VERSIONS = "testUpgradeVersions";

    public static final String TEST_UPGRADE_LAST_CONTRIBUTOR = "testUpgradeLastContributor";

    public static final String TEST_UPGRADE_LOCKS = "testUpgradeLocks";

    public static final String TEST_UPGRADE_FULLTEXT = "testUpgradeFulltext";

    protected TableUpgrader tableUpgrader;

    private final QueryMakerService queryMakerService;

    private final PathResolver pathResolver;

    private final RepositoryImpl repository;

    protected boolean clusteringEnabled;

    /**
     * Creates a new Mapper.
     *
     * @param model the model
     * @param pathResolver the path resolver (used for startswith queries)
     * @param sqlInfo the sql info
     * @param xadatasource the XA datasource to use to get connections
     * @param clusterNodeHandler the cluster node handler
     * @param connectionPropagator the connection propagator
     * @param repository
     */
    public JDBCMapper(Model model, PathResolver pathResolver, SQLInfo sqlInfo,
            XADataSource xadatasource, ClusterNodeHandler clusterNodeHandler,
            JDBCConnectionPropagator connectionPropagator, boolean noSharing,
            RepositoryImpl repository) throws StorageException {
        super(model, sqlInfo, xadatasource, clusterNodeHandler,
                connectionPropagator, noSharing);
        this.pathResolver = pathResolver;
        this.repository = repository;
        clusteringEnabled = clusterNodeHandler != null;
        try {
            queryMakerService = Framework.getService(QueryMakerService.class);
        } catch (Exception e) {
            throw new StorageException(e);
        }

        tableUpgrader = new TableUpgrader(this);
        tableUpgrader.add(Model.VERSION_TABLE_NAME,
                Model.VERSION_IS_LATEST_KEY, "upgradeVersions",
                TEST_UPGRADE_VERSIONS);
        tableUpgrader.add("dublincore", "lastContributor",
                "upgradeLastContributor", TEST_UPGRADE_LAST_CONTRIBUTOR);
        tableUpgrader.add(Model.LOCK_TABLE_NAME, Model.LOCK_OWNER_KEY,
                "upgradeLocks", TEST_UPGRADE_LOCKS);

    }

    @Override
    public int getTableSize(String tableName) {
        return sqlInfo.getDatabase().getTable(tableName).getColumns().size();
    }

    /*
     * ----- Root -----
     */

    @Override
    public void createDatabase() throws StorageException {
        try {
            createTables();
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException(e);
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
                    sb.append(Dialect.toHexString(digest.digest()).substring(0,
                            12));

                    return sb.toString();

                } catch (Exception e) {
                    throw new RuntimeException("Error", e);
                }
            }
        }

        return origName;
    }

    protected void createTables() throws SQLException {
        sqlInfo.executeSQLStatements(null, this); // for missing category
        sqlInfo.executeSQLStatements("first", this);
        sqlInfo.executeSQLStatements("beforeTableCreation", this);
        if (testProps.containsKey(TEST_UPGRADE)) {
            // create "old" tables
            sqlInfo.executeSQLStatements("testUpgrade", this);
        }

        String schemaName = dialect.getConnectionSchema(connection);
        DatabaseMetaData metadata = connection.getMetaData();
        Set<String> tableNames = findTableNames(metadata, schemaName);
        Database database = sqlInfo.getDatabase();
        Map<String, List<Column>> added = new HashMap<String, List<Column>>();

        Statement st = null;
        try {
            st = connection.createStatement();
            for (Table table : database.getTables()) {
                String tableName = getTableName(table.getPhysicalName());
                if (tableNames.contains(tableName.toUpperCase())) {
                    dialect.existingTableDetected(connection, table,
                            model, sqlInfo.database);
                } else {

                    /*
                     * Create missing table.
                     */

                    boolean create = dialect.preCreateTable(connection,
                            table, model, sqlInfo.database);
                    if (!create) {
                        log.warn("Creation skipped for table: " + tableName);
                        continue;
                    }

                    String sql = table.getCreateSql();
                    logger.log(sql);
                    try {
                        st.execute(sql);
                        countExecute();
                    } catch (SQLException e) {
                        try {
                            closeStatement(st);
                        } finally {
                            throw new SQLException("Error creating table: "
                                    + sql + " : " + e.getMessage(), e);
                        }
                    }

                    for (String s : table.getPostCreateSqls(model)) {
                        logger.log(s);
                        try {
                            st.execute(s);
                            countExecute();
                        } catch (SQLException e) {
                            throw new SQLException(
                                    "Error post creating table: " + s + " : "
                                            + e.getMessage(), e);
                        }
                    }
                    for (String s : dialect.getPostCreateTableSqls(
                            table, model, sqlInfo.database)) {
                        logger.log(s);
                        try {
                            st.execute(s);
                            countExecute();
                        } catch (SQLException e) {
                            throw new SQLException(
                                    "Error post creating table: " + s + " : "
                                            + e.getMessage(), e);
                        }
                    }
                    added.put(table.getKey(), null); // null = table created
                }

                /*
                 * Get existing columns.
                 */

                ResultSet rs = metadata.getColumns(null, schemaName, tableName,
                        "%");
                Map<String, Integer> columnTypes = new HashMap<String, Integer>();
                Map<String, String> columnTypeNames = new HashMap<String, String>();
                Map<String, Integer> columnTypeSizes = new HashMap<String, Integer>();
                while (rs.next()) {
                    String schema = rs.getString("TABLE_SCHEM");
                    if (schema != null) { // null for MySQL, doh!
                        if ("INFORMATION_SCHEMA".equals(schema.toUpperCase())) {
                            // H2 returns some system tables (locks)
                            continue;
                        }
                    }
                    String columnName = rs.getString("COLUMN_NAME").toUpperCase();
                    columnTypes.put(columnName,
                            Integer.valueOf(rs.getInt("DATA_TYPE")));
                    columnTypeNames.put(columnName, rs.getString("TYPE_NAME"));
                    columnTypeSizes.put(columnName,
                            Integer.valueOf(rs.getInt("COLUMN_SIZE")));
                }

                /*
                 * Update types and create missing columns.
                 */

                List<Column> addedColumns = new LinkedList<Column>();
                for (Column column : table.getColumns()) {
                    String upperName = column.getPhysicalName().toUpperCase();
                    Integer type = columnTypes.remove(upperName);
                    if (type == null) {
                        log.warn("Adding missing column in database: "
                                + column.getFullQuotedName());
                        String sql = table.getAddColumnSql(column);
                        logger.log(sql);
                        try {
                            st.execute(sql);
                            countExecute();
                        } catch (SQLException e) {
                            throw new SQLException("Error adding column: "
                                    + sql + " : " + e.getMessage(), e);
                        }
                        for (String s : table.getPostAddSqls(column, model)) {
                            logger.log(s);
                            try {
                                st.execute(s);
                                countExecute();
                            } catch (SQLException e) {
                                throw new SQLException(
                                        "Error post adding column: " + s
                                                + " : " + e.getMessage(), e);
                            }
                        }
                        addedColumns.add(column);
                    } else {
                        int expected = column.getJdbcType();
                        int actual = type.intValue();
                        String actualName = columnTypeNames.get(upperName);
                        Integer actualSize = columnTypeSizes.get(upperName);
                        if (!column.setJdbcType(actual, actualName,
                                actualSize.intValue())) {
                            log.error(String.format(
                                    "SQL type mismatch for %s: expected %s, database has %s / %s (%s)",
                                    column.getFullQuotedName(),
                                    Integer.valueOf(expected), type,
                                    actualName, actualSize));
                        }
                    }
                }
                for (String col : dialect.getIgnoredColumns(table)) {
                    columnTypes.remove(col.toUpperCase());
                }
                if (!columnTypes.isEmpty()) {
                    log.warn("Database contains additional unused columns for table "
                            + table.getQuotedName()
                            + ": "
                            + StringUtils.join(new ArrayList<String>(
                                    columnTypes.keySet()), ", "));
                }
                if (!addedColumns.isEmpty()) {
                    if (added.containsKey(table.getKey())) {
                        throw new AssertionError();
                    }
                    added.put(table.getKey(), addedColumns);
                }
            }
        } finally {
            if (st != null) {
                try {
                    closeStatement(st);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        if (testProps.containsKey(TEST_UPGRADE)) {
            // create "old" content in tables
            sqlInfo.executeSQLStatements("testUpgradeOldTables", this);
        }

        // run upgrade for each table if added columns or test
        for (Entry<String, List<Column>> en : added.entrySet()) {
            List<Column> addedColumns = en.getValue();
            String tableKey = en.getKey();
            upgradeTable(tableKey, addedColumns);
        }
        sqlInfo.executeSQLStatements("afterTableCreation", this);
        sqlInfo.executeSQLStatements("last", this);
        dialect.performAdditionalStatements(connection);
    }

    protected void upgradeTable(String tableKey, List<Column> addedColumns)
            throws SQLException {
        tableUpgrader.upgrade(tableKey, addedColumns);
    }

    /** Finds uppercase table names. */
    protected static Set<String> findTableNames(DatabaseMetaData metadata,
            String schemaName) throws SQLException {
        Set<String> tableNames = new HashSet<String>();
        ResultSet rs = metadata.getTables(null, schemaName, "%",
                new String[] { "TABLE" });
        while (rs.next()) {
            String tableName = rs.getString("TABLE_NAME");
            tableNames.add(tableName.toUpperCase());
        }
        return tableNames;
    }

    @Override
    public String createClusterNode() throws StorageException {
        Statement st = null;
        try {
            // get the cluster node id from the database, if necessary
            String sql = dialect.getClusterNodeIdSql();
            String nodeId;
            if (sql == null) {
                nodeId = null;
            } else {
                st = connection.createStatement();
                if (logger.isLogEnabled()) {
                    logger.log(sql);
                }
                ResultSet rs = st.executeQuery(sql);
                if (!rs.next()) {
                    throw new StorageException("Cannot get cluster node id");
                }
                nodeId = rs.getString(1);
                if (logger.isLogEnabled()) {
                    logger.log("  -> cluster node id: " + nodeId);
                }
            }
            // add the cluster node
            sqlInfo.executeSQLStatements("addClusterNode", this);
            return nodeId;
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException(e);
        } finally {
            if (st != null) {
                try {
                    closeStatement(st);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void removeClusterNode() throws StorageException {
        try {
            sqlInfo.executeSQLStatements("removeClusterNode", this);
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException(e);
        }
    }

    @Override
    public void insertClusterInvalidations(Invalidations invalidations,
            String nodeId) throws StorageException {
        String sql = dialect.getClusterInsertInvalidations();
        if (nodeId != null) {
            sql = String.format(sql, nodeId);
        }
        List<Column> columns = sqlInfo.getClusterInvalidationsColumns();
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
            int kind = Invalidations.MODIFIED;
            while (true) {
                Set<RowId> rowIds = invalidations.getKindSet(kind);

                // reorganize by id
                Map<Serializable, Set<String>> res = new HashMap<Serializable, Set<String>>();
                for (RowId rowId : rowIds) {
                    Set<String> tableNames = res.get(rowId.id);
                    if (tableNames == null) {
                        res.put(rowId.id, tableNames = new HashSet<String>());
                    }
                    tableNames.add(rowId.tableName);
                }

                // do inserts
                for (Entry<Serializable, Set<String>> en : res.entrySet()) {
                    Serializable id = en.getKey();
                    String fragments = join(en.getValue(), ' ');
                    if (logger.isLogEnabled()) {
                        logger.logSQL(sql, Arrays.<Serializable> asList(id,
                                fragments, Long.valueOf(kind)));
                    }
                    Serializable frags;
                    if (dialect.supportsArrays()
                            && columns.get(1).getJdbcType() == Types.ARRAY) {
                        frags = fragments.split(" ");
                    } else {
                        frags = fragments;
                    }
                    columns.get(0).setToPreparedStatement(ps, 1, id);
                    columns.get(1).setToPreparedStatement(ps, 2, frags);
                    columns.get(2).setToPreparedStatement(ps, 3,
                            Long.valueOf(kind));
                    ps.execute();
                    countExecute();
                }
                if (kind == Invalidations.MODIFIED) {
                    kind = Invalidations.DELETED;
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Could not invalidate", e);
        } finally {
            if (ps != null) {
                try {
                    closeStatement(ps);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    // join that works on a set
    protected static final String join(Collection<String> strings, char sep) {
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
    public Invalidations getClusterInvalidations(String nodeId)
            throws StorageException {
        Invalidations invalidations = new Invalidations();
        String sql = dialect.getClusterGetInvalidations();
        String sqldel = dialect.getClusterDeleteInvalidations();
        if (nodeId != null) {
            sql = String.format(sql, nodeId);
            sqldel = String.format(sqldel, nodeId);
        }
        List<Column> columns = sqlInfo.getClusterInvalidationsColumns();
        Statement st = null;
        try {
            st = connection.createStatement();
            if (logger.isLogEnabled()) {
                logger.log(sql);
            }
            ResultSet rs = st.executeQuery(sql);
            countExecute();
            int n = 0;
            while (rs.next()) {
                n++;
                Serializable id = columns.get(0).getFromResultSet(rs, 1);
                Serializable frags = columns.get(1).getFromResultSet(rs, 2);
                int kind = ((Long) columns.get(2).getFromResultSet(rs, 3)).intValue();
                String[] fragments;
                if (dialect.supportsArrays()
                        && frags instanceof String[]) {
                    fragments = (String[]) frags;
                } else {
                    fragments = ((String) frags).split(" ");
                }
                invalidations.add(id, fragments, kind);
            }
            if (logger.isLogEnabled()) {
                // logCount(n);
                logger.log("  -> " + invalidations);
            }
            if (dialect.isClusteringDeleteNeeded()) {
                if (logger.isLogEnabled()) {
                    logger.log(sqldel);
                }
                n = st.executeUpdate(sqldel);
                countExecute();
                if (logger.isLogEnabled()) {
                    logger.logCount(n);
                }
            }
            return invalidations;
        } catch (Exception e) {
            checkConnectionReset(e, true);
            throw new StorageException("Could not invalidate", e);
        } finally {
            if (st != null) {
                try {
                    closeStatement(st);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public Serializable getRootId(String repositoryId) throws StorageException {
        String sql = sqlInfo.getSelectRootIdSql();
        try {
            if (logger.isLogEnabled()) {
                logger.logSQL(sql,
                        Collections.<Serializable> singletonList(repositoryId));
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                ps.setString(1, repositoryId);
                ResultSet rs = ps.executeQuery();
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
                    throw new StorageException("Row query for " + repositoryId
                            + " returned several rows: " + sql);
                }
                return id;
            } finally {
                closeStatement(ps);
            }
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Could not select: " + sql, e);
        }
    }

    @Override
    public void setRootId(Serializable repositoryId, Serializable id)
            throws StorageException {
        String sql = sqlInfo.getInsertRootIdSql();
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                List<Column> columns = sqlInfo.getInsertRootIdColumns();
                List<Serializable> debugValues = null;
                if (logger.isLogEnabled()) {
                    debugValues = new ArrayList<Serializable>(2);
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
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Could not insert: " + sql, e);
        }
    }

    protected QueryMaker findQueryMaker(String queryType)
            throws StorageException {
        for (Class<? extends QueryMaker> klass : queryMakerService.getQueryMakers()) {
            QueryMaker queryMaker;
            try {
                queryMaker = klass.newInstance();
            } catch (Exception e) {
                throw new StorageException(e);
            }
            if (queryMaker.accepts(queryType)) {
                return queryMaker;
            }
        }
        return null;
    }

    protected void prepareUserReadAcls(QueryFilter queryFilter)
            throws StorageException {
        String sql = dialect.getPrepareUserReadAclsSql();
        Serializable principals = queryFilter.getPrincipals();
        if (sql == null || principals == null) {
            return;
        }
        if (!dialect.supportsArrays()) {
            principals = StringUtils.join((String[]) principals,
                    Dialect.ARRAY_SEP);
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
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Failed to prepare user read acl cache",
                    e);
        } finally {
            if (ps != null) {
                try {
                    closeStatement(ps);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public PartialList<Serializable> query(String query, String queryType,
            QueryFilter queryFilter, boolean countTotal)
            throws StorageException {
        return query(query, queryType, queryFilter, countTotal ? -1 : 0);
    }

    @Override
    public PartialList<Serializable> query(String query, String queryType,
            QueryFilter queryFilter, long countUpTo) throws StorageException {
        if (dialect.needsPrepareUserReadAcls()) {
            prepareUserReadAcls(queryFilter);
        }
        QueryMaker queryMaker = findQueryMaker(queryType);
        if (queryMaker == null) {
            throw new StorageException("No QueryMaker accepts query: "
                    + queryType + ": " + query);
        }
        QueryMaker.Query q = queryMaker.buildQuery(sqlInfo, model,
                pathResolver, query, queryFilter);

        if (q == null) {
            logger.log("Query cannot return anything due to conflicting clauses");
            return new PartialList<Serializable>(
                    Collections.<Serializable> emptyList(), 0);
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
            sql = dialect.addPagingClause(sql,
                    Math.max(countUpTo + 1, limit + offset), 0);
        }

        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql,
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            int i = 1;
            for (Serializable object : q.selectParams) {
                 setToPreparedStatement(ps, i++, object);
            }
            ResultSet rs = ps.executeQuery();
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
            List<Serializable> ids = new LinkedList<Serializable>();
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

            return new PartialList<Serializable>(ids, totalSize);
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Invalid query: " + query, e);
        } finally {
            if (ps != null) {
                try {
                    closeStatement(ps);
                } catch (SQLException e) {
                    log.error("Cannot close connection", e);
                }
            }
        }
    }

    public int setToPreparedStatement(PreparedStatement ps, int i,
            Serializable object) throws SQLException {
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
            Array array = dialect.createArrayOf(jdbcType, (Object[]) object,
                    connection);
            ps.setArray(i, array);
        } else {
            ps.setObject(i, object);
        }
        return i;
    }

    // queryFilter used for principals and permissions
    @Override
    public IterableQueryResult queryAndFetch(String query, String queryType,
            QueryFilter queryFilter, Object... params) throws StorageException {
        if (dialect.needsPrepareUserReadAcls()) {
            prepareUserReadAcls(queryFilter);
        }
        QueryMaker queryMaker = findQueryMaker(queryType);
        if (queryMaker == null) {
            throw new StorageException("No QueryMaker accepts query: "
                    + queryType + ": " + query);
        }
        try {
            return new ResultSetQueryResult(queryMaker, query, queryFilter,
                    pathResolver, this, params);
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Invalid query: " + queryType + ": "
                    + query, e);
        }
    }

    @Override
    public Set<Serializable> getAncestorsIds(Collection<Serializable> ids)
            throws StorageException {
        SQLInfoSelect select = sqlInfo.getSelectAncestorsIds();
        if (select == null) {
            return getAncestorsIdsIterative(ids);
        }
        Serializable whereIds = newIdArray(ids);
        Set<Serializable> res = new HashSet<Serializable>();
        PreparedStatement ps = null;
        try {
            if (logger.isLogEnabled()) {
                logger.logSQL(select.sql, Collections.singleton(whereIds));
            }
            Column what = select.whatColumns.get(0);
            ps = connection.prepareStatement(select.sql);
            setToPreparedStatementIdArray(ps, 1, whereIds);
            ResultSet rs = ps.executeQuery();
            countExecute();
            List<Serializable> debugIds = null;
            if (logger.isLogEnabled()) {
                debugIds = new LinkedList<Serializable>();
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
                    Serializable id = what.getFromResultSet(rs,
                            1);
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
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Failed to get ancestors ids", e);
        } finally {
            if (ps != null) {
                try {
                    closeStatement(ps);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Uses iterative parentid selection.
     */
    protected Set<Serializable> getAncestorsIdsIterative(
            Collection<Serializable> ids) throws StorageException {
        PreparedStatement ps = null;
        try {
            LinkedList<Serializable> todo = new LinkedList<Serializable>(ids);
            Set<Serializable> done = new HashSet<Serializable>();
            Set<Serializable> res = new HashSet<Serializable>();
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
                ResultSet rs = ps.executeQuery();
                countExecute();
                todo = new LinkedList<Serializable>();
                List<Serializable> debugIds = null;
                if (logger.isLogEnabled()) {
                    debugIds = new LinkedList<Serializable>();
                }
                while (rs.next()) {
                    Serializable id = what.getFromResultSet(rs,
                            1);
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
                ps.close();
                ps = null;
            }
            return res;
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Failed to get ancestors ids", e);
        } finally {
            if (ps != null) {
                try {
                    closeStatement(ps);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void updateReadAcls() throws StorageException {
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
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Failed to update read acls", e);
        } finally {
            if (st != null) {
                try {
                    closeStatement(st);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("updateReadAcls: done.");
        }
    }

    @Override
    public void rebuildReadAcls() throws StorageException {
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
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Failed to rebuild read acls", e);
        } finally {
            if (st != null) {
                try {
                    closeStatement(st);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        log.debug("rebuildReadAcls: done.");
    }

    /*
     * ----- Locking -----
     */

    protected Connection connection(boolean autocommit) throws StorageException {
        checkConnectionValid();
        try {
            connection.setAutoCommit(autocommit);
        } catch (SQLException e) {
            throw new StorageException("Cannot set auto commit mode onto " + this + "'s connection", e);
        }
        return connection;
    }
    /**
     * Calls the callable, inside a transaction if in cluster mode.
     * <p>
     * Called under {@link #serializationLock}.
     */
    protected Lock callInTransaction(Callable<Lock> callable, boolean tx)
            throws StorageException {
        boolean ok = false;
        checkConnectionValid();
        try {
            if (log.isDebugEnabled()) {
                log.debug("callInTransaction setAutoCommit " + !tx);
            }
            connection.setAutoCommit(!tx);
        } catch (SQLException e) {
            throw new StorageException("Cannot set auto commit mode onto " + this + "'s connection", e);
        }
        try {
            Lock result;
            try {
                result = callable.call();
            } catch (StorageException e) {
                throw e;
            } catch (Exception e) {
                throw new StorageException(e);
            }

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
                    throw new StorageException(e);
                }
            }
        }
    }

    @Override
    public Lock getLock(Serializable id) throws StorageException {
        if (log.isDebugEnabled()) {
            try {
                log.debug("getLock " + id + " while autoCommit="
                        + connection.getAutoCommit());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        RowId rowId = new RowId(Model.LOCK_TABLE_NAME, id);
        Row row;
        try {
            row = readSimpleRow(rowId);
        } catch (ConnectionResetException e) {
            // retry once
            row = readSimpleRow(rowId);
        }
        return row == null ? null : new Lock(
                (String) row.get(Model.LOCK_OWNER_KEY),
                (Calendar) row.get(Model.LOCK_CREATED_KEY));
    }

    @Override
    public Lock setLock(final Serializable id, final Lock lock) throws StorageException {
        if (log.isDebugEnabled()) {
            log.debug("setLock " + id + " owner=" + lock.getOwner());
        }
        SetLock call = new SetLock(id, lock);
        return callInTransaction(call, clusteringEnabled);
    }

    protected class SetLock implements Callable<Lock> {
        protected final Serializable id;

        protected final Lock lock;

        protected SetLock(Serializable id, Lock lock) {
            super();
            this.id = id;
            this.lock = lock;
        }

        @Override
        public Lock call() throws StorageException {
            Lock oldLock = getLock(id);
            if (oldLock == null) {
                Row row = new Row(Model.LOCK_TABLE_NAME, id);
                row.put(Model.LOCK_OWNER_KEY, lock.getOwner());
                row.put(Model.LOCK_CREATED_KEY, lock.getCreated());
                insertSimpleRows(Model.LOCK_TABLE_NAME,
                        Collections.singletonList(row));
            }
            return oldLock;
        }
    }

    @Override
    public Lock removeLock(final Serializable id, final String owner, final boolean force)
            throws StorageException {
        if (log.isDebugEnabled()) {
            log.debug("removeLock " + id + " owner=" + owner + " force=" + force);
        }
        RemoveLock call = new RemoveLock(id, owner, force);
        return callInTransaction(call, !force);
    }

    protected class RemoveLock implements Callable<Lock> {
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
        public Lock call() throws StorageException {
            Lock oldLock = force ? null : getLock(id);
            if (!force && owner != null) {
                if (oldLock == null) {
                    // not locked, nothing to do
                    return null;
                }
                if (!repository.getLockManager().canLockBeRemoved(oldLock,
                        owner)) {
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
    public void markReferencedBinaries(BinaryGarbageCollector gc)
            throws StorageException {
        log.debug("Starting binaries GC mark");
        Statement st = null;
        try {
            st = connection.createStatement();
            int i = -1;
            for (String sql : sqlInfo.getBinariesSql) {
                i++;
                Column col = sqlInfo.getBinariesColumns.get(i);
                if (logger.isLogEnabled()) {
                    logger.log(sql);
                }
                ResultSet rs = st.executeQuery(sql);
                countExecute();
                int n = 0;
                while (rs.next()) {
                    n++;
                    String digest = (String) col.getFromResultSet(rs, 1);
                    if (digest != null) {
                        gc.mark(digest);
                    }
                }
                if (logger.isLogEnabled()) {
                    logger.logCount(n);
                }
            }
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new RuntimeException("Failed to mark binaries for gC", e);
        } finally {
            if (st != null) {
                try {
                    closeStatement(st);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        log.debug("End of binaries GC mark");
    }

    /*
     * ----- XAResource -----
     */

    protected static String systemToString(Object o) {
        return o.getClass().getName() + "@"
                + Integer.toHexString(System.identityHashCode(o));
    }

    @Override
    public void start(Xid xid, int flags) throws XAException {
        try {
            checkConnectionValid();
            xaresource.start(xid, flags);
            if (logger.isLogEnabled()) {
                logger.log("XA start on " + systemToString(xid));
            }
        } catch (StorageException e) {
            throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
        } catch (XAException e) {
            checkConnectionReset(e);
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
    public void connect() throws StorageException {
        try {
            openBaseConnection();
        } catch (SQLException | ResourceException cause) {
            throw new StorageException("Cannot connect to database", cause);
        }
    }

    @Override
    public void disconnect() {
        closeConnections();
    }

}
