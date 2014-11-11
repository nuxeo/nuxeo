/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.CollectionFragment;
import org.nuxeo.ecm.core.storage.sql.Context;
import org.nuxeo.ecm.core.storage.sql.Fragment;
import org.nuxeo.ecm.core.storage.sql.HierarchyContext;
import org.nuxeo.ecm.core.storage.sql.Invalidations;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.PersistenceContext;
import org.nuxeo.ecm.core.storage.sql.PropertyType;
import org.nuxeo.ecm.core.storage.sql.RepositoryImpl;
import org.nuxeo.ecm.core.storage.sql.Session;
import org.nuxeo.ecm.core.storage.sql.SimpleFragment;
import org.nuxeo.ecm.core.storage.sql.Fragment.State;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo.SQLInfoSelect;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Update;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.ConditionalStatement;
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
public class JDBCMapper implements Mapper {

    private static final Log log = LogFactory.getLog(JDBCMapper.class);

    public static boolean debugTestUpgrade;

    /** The repository from which this was built. */
    private final RepositoryImpl repository;

    /** The model used to do the mapping. */
    final Model model;

    /** The SQL information. */
    final SQLInfo sqlInfo;

    /** The xa datasource. */
    private final XADataSource xadatasource;

    /** The xa pooled connection. */
    private XAConnection xaconnection;

    /** The actual connection. */
    Connection connection;

    private XAResource xaresource;

    // for debug
    private static final AtomicLong instanceCounter = new AtomicLong(0);

    // for debug
    protected final JDBCMapperLogger logger = new JDBCMapperLogger(
            instanceCounter.incrementAndGet());

    private final QueryMakerService queryMakerService;

    /**
     * Creates a new Mapper.
     *
     * @param repository the repository
     * @param model the model
     * @param sqlInfo the sql info
     * @param xadatasource the XA datasource to use to get connections
     */
    public JDBCMapper(RepositoryImpl repository, Model model, SQLInfo sqlInfo,
            XADataSource xadatasource) throws StorageException {
        this.repository = repository;
        this.model = model;
        this.sqlInfo = sqlInfo;
        this.xadatasource = xadatasource;
        resetConnection();
        try {
            queryMakerService = Framework.getService(QueryMakerService.class);
        } catch (Exception e) {
            throw new StorageException(e);
        }

    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
            }
        }
        if (xaconnection != null) {
            try {
                xaconnection.close();
            } catch (SQLException e) {
            }
        }
        xaconnection = null;
        connection = null;
        xaresource = null;
    }

    /**
     * Finds a new connection if the previous ones was broken or timed out.
     */
    protected void resetConnection() throws StorageException {
        close();
        try {
            xaconnection = xadatasource.getXAConnection();
            connection = xaconnection.getConnection();
            xaresource = xaconnection.getXAResource();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    /**
     * Checks the SQL error we got and determine if the low level connection has
     * to be reset.
     */
    protected void checkConnectionReset(SQLException e) throws StorageException {
        if (sqlInfo.dialect.connectionClosedByException(e)) {
            resetConnection();
        }
    }

    /**
     * Checks the XA error we got and determine if the low level connection has
     * to be reset.
     */
    protected void checkConnectionReset(XAException e) {
        if (sqlInfo.dialect.connectionClosedByException(e)) {
            try {
                resetConnection();
            } catch (StorageException ee) {
                // swallow, exception already thrown by caller
            }
        }
    }

    public int getTableSize(String tableName) {
        return sqlInfo.getDatabase().getTable(tableName).getColumns().size();
    }

    protected static void closePreparedStatement(PreparedStatement ps)
            throws SQLException {
        try {
            ps.close();
        } catch (IllegalArgumentException e) {
            // ignore
            // http://bugs.mysql.com/35489 with JDBC 4 and driver <= 5.1.6
        }
    }

    public void createDatabase() throws StorageException {
        try {
            Collection<ConditionalStatement> statements = sqlInfo.getConditionalStatements();
            executeConditionalStatements(statements, true);
            if (debugTestUpgrade) {
                executeConditionalStatements(
                        sqlInfo.getTestConditionalStatements(), true);
            }
            createTables();
            executeConditionalStatements(statements, false);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    protected String getTableName(String origName) {

        if (sqlInfo.dialect instanceof DialectOracle) {
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
        String schemaName = sqlInfo.dialect.getConnectionSchema(connection);
        DatabaseMetaData metadata = connection.getMetaData();
        Set<String> tableNames = findTableNames(metadata, schemaName);
        Statement st = connection.createStatement();

        for (Table table : sqlInfo.getDatabase().getTables()) {

            String tableName = getTableName(table.getName());

            if (tableNames.contains(tableName)
                    || tableNames.contains(tableName.toUpperCase())) {
                sqlInfo.dialect.existingTableDetected(connection, table, model,
                        sqlInfo.database);
            } else {
                /*
                 * Create missing table.
                 */
                boolean create = sqlInfo.dialect.preCreateTable(connection,
                        table, model, sqlInfo.database);
                if (!create) {
                    log.warn("Creation skipped for table: " + table.getName());
                    continue;
                }

                String sql = table.getCreateSql();
                logger.log(sql);
                st.execute(sql);
                for (String s : table.getPostCreateSqls(model)) {
                    logger.log(s);
                    st.execute(s);
                }
                for (String s : sqlInfo.dialect.getPostCreateTableSqls(table,
                        model, sqlInfo.database)) {
                    logger.log(s);
                    st.execute(s);
                }
            }

            /*
             * Get existing columns.
             */
            ResultSet rs = metadata.getColumns(null, schemaName, tableName, "%");
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
            for (Column column : table.getColumns()) {
                String upperName = column.getPhysicalName().toUpperCase();
                Integer type = columnTypes.remove(upperName);
                if (type == null) {
                    log.warn("Adding missing column in database: "
                            + column.getFullQuotedName());
                    String sql = table.getAddColumnSql(column);
                    logger.log(sql);
                    st.execute(sql);
                    for (String s : table.getPostAddSqls(column, model)) {
                        logger.log(s);
                        st.execute(s);
                    }
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
                                Integer.valueOf(expected), type, actualName,
                                actualSize));
                    }
                }
            }
            if (!columnTypes.isEmpty()) {
                log.warn("Database contains additional unused columns for table "
                        + table.getQuotedName()
                        + ": "
                        + StringUtils.join(new ArrayList<String>(
                                columnTypes.keySet()), ", "));
            }
        }

        st.close();
    }

    protected static Set<String> findTableNames(DatabaseMetaData metadata,
            String schemaName) throws SQLException {
        Set<String> tableNames = new HashSet<String>();
        ResultSet rs = metadata.getTables(null, schemaName, "%",
                new String[] { "TABLE" });
        while (rs.next()) {
            String tableName = rs.getString("TABLE_NAME");
            tableNames.add(tableName);
            // normalize to uppercase too
            tableNames.add(tableName.toUpperCase());
        }
        return tableNames;
    }

    protected void executeConditionalStatements(
            Collection<ConditionalStatement> statements, boolean early)
            throws SQLException {
        Statement st = connection.createStatement();
        for (ConditionalStatement s : statements) {
            if (s.early != early) {
                continue;
            }
            boolean doPre;
            if (s.doPre != null) {
                doPre = s.doPre.booleanValue();
            } else {
                logger.log(s.checkStatement);
                ResultSet rs = st.executeQuery(s.checkStatement);
                if (rs.next()) {
                    // already present
                    logger.log("  -> (present)");
                    doPre = true;
                } else {
                    doPre = false;
                }
            }
            if (doPre) {
                logger.log(s.preStatement);
                st.execute(s.preStatement);
            }
            logger.log(s.statement);
            st.execute(s.statement);
        }
        st.close();
    }

    public void createClusterNode() throws StorageException {
        try {
            Statement st = connection.createStatement();
            String sql = sqlInfo.getCleanupClusterNodesSql();
            logger.log(sql);
            int n = st.executeUpdate(sql);
            if (logger.isLogEnabled()) {
                logger.logCount(n);
            }
            sql = sqlInfo.getCreateClusterNodeSql();
            logger.log(sql);
            st.execute(sql);
            st.close();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public void removeClusterNode() throws StorageException {
        try {
            Statement st = connection.createStatement();
            String sql = sqlInfo.getRemoveClusterNodeSql();
            logger.log(sql);
            st.execute(sql);
            st.close();
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException(e);
        }
    }

    public void insertClusterInvalidations(Invalidations invalidations)
            throws StorageException {
        String sql = sqlInfo.dialect.getClusterInsertInvalidations();
        List<Column> columns = sqlInfo.getClusterInvalidationsColumns();
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
            for (int kind = 1; kind <= 2; kind++) {
                Map<String, Set<Serializable>> map = invalidations.getKindMap(kind);
                // turn fragment-based map into id-based map
                Map<Serializable, Set<String>> m = invertMap(map);
                for (Entry<Serializable, Set<String>> e : m.entrySet()) {
                    Serializable id = e.getKey();
                    String fragments = join(e.getValue(), ' ');
                    if (logger.isLogEnabled()) {
                        logger.logSQL(sql, Arrays.<Serializable> asList(id,
                                fragments, Integer.valueOf(kind)));
                    }
                    Serializable frags;
                    if (sqlInfo.dialect.supportsArrays()
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
                }
            }
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Could not invalidate", e);
        } finally {
            if (ps != null) {
                try {
                    closePreparedStatement(ps);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Turns a map of fragment -> set-of-ids into a map of id ->
     * set-of-fragments.
     */
    protected static Map<Serializable, Set<String>> invertMap(
            Map<String, Set<Serializable>> map) {
        Map<Serializable, Set<String>> res = new HashMap<Serializable, Set<String>>();
        for (Entry<String, Set<Serializable>> entry : map.entrySet()) {
            String fragment = entry.getKey();
            for (Serializable id : entry.getValue()) {
                Set<String> set = res.get(id);
                if (set == null) {
                    set = new HashSet<String>();
                    res.put(id, set);
                }
                set.add(fragment);
            }
        }
        return res;
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

    public Invalidations getClusterInvalidations() throws StorageException {
        Invalidations invalidations = new Invalidations();
        String sql = sqlInfo.dialect.getClusterGetInvalidations();
        String sqldel = sqlInfo.dialect.getClusterDeleteInvalidations();
        List<Column> columns = sqlInfo.getClusterInvalidationsColumns();
        Statement st = null;
        try {
            st = connection.createStatement();
            if (logger.isLogEnabled()) {
                logger.log(sql);
            }
            ResultSet rs = st.executeQuery(sql);
            int n = 0;
            while (rs.next()) {
                n++;
                Serializable id = columns.get(0).getFromResultSet(rs, 1);
                Serializable frags = columns.get(1).getFromResultSet(rs, 2);
                int kind = ((Long) columns.get(2).getFromResultSet(rs, 3)).intValue();
                String[] fragments;
                if (sqlInfo.dialect.supportsArrays()
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
            if (sqlInfo.dialect.isClusteringDeleteNeeded()) {
                if (logger.isLogEnabled()) {
                    logger.log(sqldel);
                }
                n = st.executeUpdate(sqldel);
                if (logger.isLogEnabled()) {
                    logger.logCount(n);
                }
            }
            return invalidations;
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Could not invalidate", e);
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public Serializable getRootId(Serializable repositoryId)
            throws StorageException {
        String sql = sqlInfo.getSelectRootIdSql();
        try {
            if (logger.isLogEnabled()) {
                logger.logSQL(sql, Collections.singletonList(repositoryId));
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                ps.setObject(1, repositoryId);
                ResultSet rs = ps.executeQuery();
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
                closePreparedStatement(ps);
            }
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Could not select: " + sql, e);
        }
    }

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
            } finally {
                closePreparedStatement(ps);
            }
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Could not insert: " + sql, e);
        }
    }

    protected CollectionIO getCollectionIO(String tableName) {
        return tableName.equals(Model.ACL_TABLE_NAME) ? ACLCollectionIO.INSTANCE
                : ScalarCollectionIO.INSTANCE;
    }

    public CollectionFragment makeEmptyCollectionRow(Serializable id,
            Context context) {
        Serializable[] empty = model.getCollectionFragmentType(
                context.getTableName()).getEmptyArray();
        return new CollectionFragment(id, State.CREATED, context, empty);
    }

    public Serializable insertSingleRow(SimpleFragment row)
            throws StorageException {
        String tableName = row.getTableName();
        PreparedStatement ps = null;
        try {
            // insert the row
            // XXX statement should be already prepared
            String sql = sqlInfo.getInsertSql(tableName);
            List<Column> columns = sqlInfo.getInsertColumns(tableName);
            try {
                if (logger.isLogEnabled()) {
                    logger.logSQL(sql, columns, row);
                }
                ps = connection.prepareStatement(sql);
                int i = 0;
                for (Column column : columns) {
                    i++;
                    String key = column.getKey();
                    Serializable v;
                    if (key.equals(Model.MAIN_KEY)) {
                        v = row.getId();
                    } else {
                        v = row.get(key);
                    }
                    column.setToPreparedStatement(ps, i, v);
                }
                ps.execute();
            } catch (SQLException e) {
                checkConnectionReset(e);
                throw new StorageException("Could not insert: " + sql, e);
            }
            // TODO DB_IDENTITY : post insert fetch idrow
        } finally {
            if (ps != null) {
                try {
                    closePreparedStatement(ps);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        row.clearDirty();
        return row.getId();
    }

    public void insertCollectionRows(CollectionFragment fragment)
            throws StorageException {
        String tableName = fragment.getTableName();
        PreparedStatement ps = null;
        try {
            String sql = sqlInfo.getInsertSql(tableName);
            List<Column> columns = sqlInfo.getInsertColumns(tableName);
            try {
                List<Serializable> debugValues = null;
                if (logger.isLogEnabled()) {
                    debugValues = new ArrayList<Serializable>(3);
                }
                ps = connection.prepareStatement(sql);
                getCollectionIO(tableName).setToPreparedStatement(fragment,
                        columns, ps, model, debugValues, sql, logger);
            } catch (SQLException e) {
                checkConnectionReset(e);
                throw new StorageException("Could not insert: " + sql, e);
            }

        } finally {
            if (ps != null) {
                try {
                    closePreparedStatement(ps);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Fetch one row for a select of fragments with fixed criteria.
     */
    protected SimpleFragment getSelectRow(SQLInfoSelect select,
            Map<String, Serializable> criteriaMap, Context context)
            throws StorageException {
        Map<String, Serializable> joinMap = Collections.emptyMap();
        List<SimpleFragment> rows = getSelectRows(select, criteriaMap, joinMap,
                true, context);
        if (rows == null) {
            return null;
        } else {
            return rows.get(0);
        }
    }

    /**
     * Fetch the rows for a select of fragments with fixed criteria.
     */
    protected List<SimpleFragment> getSelectRows(SQLInfoSelect select,
            Map<String, Serializable> criteriaMap, Context context)
            throws StorageException {
        Map<String, Serializable> joinMap = Collections.emptyMap();
        return getSelectRows(select, criteriaMap, joinMap, false, context);
    }

    /**
     * Fetch the rows for a JOINed select of fragments with fixed criteria and a
     * joined condition.
     */
    protected List<SimpleFragment> getSelectRows(SQLInfoSelect select,
            Map<String, Serializable> criteriaMap,
            Map<String, Serializable> joinMap, Context context)
            throws StorageException {
        return getSelectRows(select, criteriaMap, joinMap, false, context);
    }

    /**
     * Fetch the rows for a select of fragments with fixed criteria given as a
     * map.
     */
    protected List<SimpleFragment> getSelectRows(SQLInfoSelect select,
            Map<String, Serializable> criteriaMap,
            Map<String, Serializable> joinMap, boolean limitToOne,
            Context context) throws StorageException {
        List<Map<String, Serializable>> maps = getSelectMaps(select,
                criteriaMap, joinMap, limitToOne, context);
        if (maps == null) {
            return null;
        }
        List<SimpleFragment> fragments = new LinkedList<SimpleFragment>();
        for (Map<String, Serializable> map : maps) {
            Serializable id = map.remove(Model.MAIN_KEY);
            SimpleFragment fragment = (SimpleFragment) context.getIfPresent(id);
            if (fragment == null) {
                fragment = new SimpleFragment(id, State.PRISTINE, context, map);
            } else {
                // row is already known in the persistent context,
                // use it
                State state = fragment.getState();
                if (state == State.DELETED) {
                    // row has been deleted in the persistent context,
                    // ignore it
                    continue;
                } else if ((state == State.ABSENT)
                        || (state == State.INVALIDATED_MODIFIED)
                        || (state == State.INVALIDATED_DELETED)) {
                    // XXX TODO
                    throw new IllegalStateException(state.toString());
                }
                // known id
            }
            fragments.add(fragment);
        }
        return fragments;
    }

    /**
     * Fetch the maps for a select of fragments with fixed criteria given as a
     * map.
     */
    protected List<Map<String, Serializable>> getSelectMaps(
            SQLInfoSelect select, Map<String, Serializable> criteriaMap,
            Map<String, Serializable> joinMap, boolean limitToOne,
            Context context) throws StorageException {
        List<Map<String, Serializable>> list = new LinkedList<Map<String, Serializable>>();
        if (select.whatColumns.isEmpty()) {
            // happens when we fetch a fragment whose columns are all opaque
            // check it's a by-id query
            if ((select.whereColumns.size() == 1)
                    && (select.whereColumns.get(0).getKey() == Model.MAIN_KEY)
                    && (joinMap == null)) {
                Map<String, Serializable> map = new HashMap<String, Serializable>(
                        criteriaMap);
                if (select.opaqueColumns != null) {
                    for (Column column : select.opaqueColumns) {
                        map.put(column.getKey(), SimpleFragment.OPAQUE);
                    }
                }
                list.add(map);
                return list;
            }
            // else do a useless select but the criteria are more complex and we
            // can't shortcut
        }
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(select.sql);

            /*
             * Compute where part.
             */
            List<Serializable> debugValues = null;
            if (logger.isLogEnabled()) {
                debugValues = new LinkedList<Serializable>();
            }
            int i = 1;
            for (Column column : select.whereColumns) {
                String key = column.getKey();
                Serializable v;
                if (criteriaMap.containsKey(key)) {
                    v = criteriaMap.get(key);
                } else if (joinMap.containsKey(key)) {
                    v = joinMap.get(key);
                } else {
                    throw new RuntimeException(key);
                }
                if (v == null) {
                    throw new StorageException("Null value for key: " + key);
                }
                if (v instanceof List<?>) {
                    // allow insert of several values, for the IN (...) case
                    for (Object vv : (List<?>) v) {
                        column.setToPreparedStatement(ps, i++,
                                (Serializable) vv);
                        if (debugValues != null) {
                            debugValues.add((Serializable) vv);
                        }
                    }
                } else {
                    column.setToPreparedStatement(ps, i++, v);
                    if (debugValues != null) {
                        debugValues.add(v);
                    }
                }
            }
            if (debugValues != null) {
                logger.logSQL(select.sql, debugValues);
            }

            /*
             * Execute query.
             */
            ResultSet rs = ps.executeQuery();

            /*
             * Construct the maps from the result set.
             */
            while (rs.next()) {
                Map<String, Serializable> map = new HashMap<String, Serializable>(
                        criteriaMap);
                i = 1;
                for (Column column : select.whatColumns) {
                    map.put(column.getKey(), column.getFromResultSet(rs, i++));
                }
                if (select.opaqueColumns != null) {
                    for (Column column : select.opaqueColumns) {
                        map.put(column.getKey(), SimpleFragment.OPAQUE);
                    }
                }
                if (logger.isLogEnabled()) {
                    logger.logResultSet(rs, select.whatColumns);
                }
                list.add(map);
                if (limitToOne) {
                    return list;
                }
            }
            if (limitToOne) {
                return null;
            }
            return list;
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Could not select: " + select.sql, e);
        } finally {
            if (ps != null) {
                try {
                    closePreparedStatement(ps);
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public Map<String, Serializable> readSingleRowMap(String tableName,
            Serializable id, Context context) throws StorageException {
        SQLInfoSelect select = sqlInfo.selectFragmentById.get(tableName);
        Map<String, Serializable> criteriaMap = new HashMap<String, Serializable>();
        criteriaMap.put(Model.MAIN_KEY, id);
        List<Map<String, Serializable>> maps = getSelectMaps(select,
                criteriaMap, null, true, context);
        return maps == null ? null : maps.get(0);
    }

    public Map<Serializable, Map<String, Serializable>> readMultipleRowMaps(
            String tableName, List<Serializable> ids, Context context)
            throws StorageException {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        SQLInfoSelect select = sqlInfo.getSelectFragmentsByIds(tableName,
                ids.size());
        Map<String, Serializable> criteriaMap = new HashMap<String, Serializable>();
        criteriaMap.put(model.MAIN_KEY, (Serializable) ids);
        List<Map<String, Serializable>> maps = getSelectMaps(select,
                criteriaMap, null, false, context);
        Map<Serializable, Map<String, Serializable>> res = new HashMap<Serializable, Map<String, Serializable>>();
        for (Map<String, Serializable> map : maps) {
            res.put(map.get(model.MAIN_KEY), map);
        }
        return res;
    }

    public SimpleFragment readChildHierRow(Serializable parentId,
            String childName, boolean complexProp, Context context)
            throws StorageException {
        String sql = sqlInfo.getSelectByChildNameSql(complexProp);
        try {
            // XXX statement should be already prepared
            List<Serializable> debugValues = null;
            if (logger.isLogEnabled()) {
                debugValues = new ArrayList<Serializable>(2);
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                // compute where part
                int i = 0;
                for (Column column : sqlInfo.getSelectByChildNameWhereColumns(complexProp)) {
                    i++;
                    String key = column.getKey();
                    Serializable v;
                    if (key.equals(Model.HIER_PARENT_KEY)) {
                        v = parentId;
                    } else if (key.equals(Model.HIER_CHILD_NAME_KEY)) {
                        v = childName;
                    } else {
                        throw new RuntimeException("Invalid hier column: "
                                + key);
                    }
                    if (v == null) {
                        throw new IllegalStateException("Null value for key: "
                                + key);
                    }
                    column.setToPreparedStatement(ps, i, v);
                    if (debugValues != null) {
                        debugValues.add(v);
                    }
                }
                if (debugValues != null) {
                    logger.logSQL(sql, debugValues);
                }
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    // no match, row doesn't exist
                    return null;
                }
                // construct the row from the results
                Map<String, Serializable> map = new HashMap<String, Serializable>();
                i = 0;
                List<Column> columns = sqlInfo.getSelectByChildNameWhatColumns(complexProp);
                Serializable id = null;
                for (Column column : columns) {
                    i++;
                    String key = column.getKey();
                    Serializable value = column.getFromResultSet(rs, i);
                    if (key.equals(Model.MAIN_KEY)) {
                        id = value;
                    } else {
                        map.put(key, value);
                    }
                }
                map.put(Model.HIER_PARENT_KEY, parentId);
                map.put(Model.HIER_CHILD_NAME_KEY, childName);
                map.put(Model.HIER_CHILD_ISPROPERTY_KEY,
                        Boolean.valueOf(complexProp));
                SimpleFragment row = new SimpleFragment(id, State.PRISTINE,
                        context, map);
                if (logger.isLogEnabled()) {
                    logger.logResultSet(rs, columns);
                }
                // check that we didn't get several rows
                while (rs.next()) {
                    // detected a duplicate name, which means that user code
                    // wasn't careful enough. We can't go back but at least we
                    // can make the duplicate available under a different name.
                    String newName = childName + '.'
                            + System.currentTimeMillis();
                    i = 0;
                    Serializable childId = null;
                    for (Column column : columns) {
                        i++;
                        if (column.getKey().equals(Model.MAIN_KEY)) {
                            childId = column.getFromResultSet(rs, i);
                        }
                    }
                    log.error(String.format(
                            "Child '%s' appeared twice as child of %s "
                                    + "(%s and %s), renaming second to '%s'",
                            childName, parentId, id, childId, newName));
                    Map<String, Serializable> rename = new HashMap<String, Serializable>();
                    rename.put(Model.HIER_CHILD_NAME_KEY, newName);
                    updateSingleRowWithValues(Model.HIER_TABLE_NAME, childId,
                            rename);
                }
                return row;
            } finally {
                closePreparedStatement(ps);
            }
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Could not select: " + sql, e);
        }
    }

    public List<SimpleFragment> readChildHierRows(Serializable parentId,
            boolean complexProp, Context context) throws StorageException {
        if (parentId == null) {
            throw new IllegalArgumentException("Illegal null parentId");
        }
        SQLInfoSelect select = sqlInfo.selectChildrenByIsProperty;
        Map<String, Serializable> criteriaMap = new HashMap<String, Serializable>();
        criteriaMap.put(Model.HIER_PARENT_KEY, parentId);
        criteriaMap.put(Model.HIER_CHILD_ISPROPERTY_KEY,
                Boolean.valueOf(complexProp));
        return getSelectRows(select, criteriaMap, context);
    }

    public Serializable[] readCollectionArray(Serializable id, Context context)
            throws StorageException {
        String tableName = context.getTableName();
        String sql = sqlInfo.selectFragmentById.get(tableName).sql;
        try {
            // XXX statement should be already prepared
            if (logger.isLogEnabled()) {
                logger.logSQL(sql, Collections.singletonList(id));
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                List<Column> columns = sqlInfo.selectFragmentById.get(tableName).whatColumns;
                ps.setObject(1, id); // assumes only one primary column
                ResultSet rs = ps.executeQuery();

                // construct the resulting collection using each row
                CollectionIO io = getCollectionIO(tableName);
                ArrayList<Serializable> list = new ArrayList<Serializable>();
                while (rs.next()) {
                    list.add(io.getCurrentFromResultSet(rs, columns, model,
                            null));
                }
                PropertyType type = model.getCollectionFragmentType(tableName).getArrayBaseType();
                Serializable[] array = type.collectionToArray(list);

                if (logger.isLogEnabled()) {
                    logger.log("  -> " + Arrays.asList(array));
                }
                return array;
            } finally {
                closePreparedStatement(ps);
            }
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Could not select: " + sql, e);
        }
    }

    public CollectionFragment readCollectionRow(Serializable id, Context context)
            throws StorageException {
        Serializable[] array = readCollectionArray(id, context);
        return new CollectionFragment(id, State.PRISTINE, context, array);
    }

    protected Map<Serializable, Serializable[]> readCollectionsArrays(
            List<Serializable> ids, Context context) throws StorageException {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        String tableName = context.getTableName();
        String[] orderBys = new String[] { model.MAIN_KEY,
                model.COLL_TABLE_POS_KEY }; // clusters results
        Set<String> skipColumns = new HashSet<String>(
                Arrays.asList(model.COLL_TABLE_POS_KEY));
        SQLInfoSelect select = sqlInfo.getSelectFragmentsByIds(tableName,
                ids.size(), orderBys, skipColumns);

        String sql = select.sql;
        try {
            if (logger.isLogEnabled()) {
                logger.logSQL(sql, ids);
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                int i = 1;
                for (Serializable id : ids) {
                    ps.setObject(i++, id);
                }
                ResultSet rs = ps.executeQuery();

                // get all values from result set, separate by ids
                // the result set is ordered by id, pos
                CollectionIO io = getCollectionIO(tableName);
                PropertyType type = model.getCollectionFragmentType(tableName).getArrayBaseType();
                Serializable curId = null;
                List<Serializable> list = null;
                Serializable[] returnId = new Serializable[1];
                Map<Serializable, Serializable[]> res = new HashMap<Serializable, Serializable[]>();
                while (rs.next()) {
                    Serializable value = io.getCurrentFromResultSet(rs,
                            select.whatColumns, model, returnId);
                    Serializable newId = returnId[0];
                    if (newId != null && !newId.equals(curId)) {
                        // flush old list
                        if (list != null) {
                            res.put(curId, type.collectionToArray(list));
                        }
                        curId = newId;
                        list = new ArrayList<Serializable>();
                    }
                    list.add(value);
                }
                if (curId != null && list != null) {
                    // flush last list
                    res.put(curId, type.collectionToArray(list));
                }

                // fill empty ones
                for (Serializable id : ids) {
                    if (!res.containsKey(id)) {
                        Serializable[] array = model.getCollectionFragmentType(
                                tableName).getEmptyArray();
                        res.put(id, array);
                    }
                }
                if (logger.isLogEnabled()) {
                    for (Entry<Serializable, Serializable[]> entry : res.entrySet()) {
                        logger.log("  -> " + entry.getKey() + " = "
                                + Arrays.asList(entry.getValue()));
                    }
                }
                return res;
            } finally {
                closePreparedStatement(ps);
            }
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Could not select: " + sql, e);
        }
    }

    public void readCollectionsRows(List<Serializable> ids, Context context,
            List<Fragment> fragments) throws StorageException {
        Map<Serializable, Serializable[]> arrays = readCollectionsArrays(ids,
                context);
        for (Serializable id : ids) {
            Serializable[] array = arrays.get(id);
            Fragment fragment = new CollectionFragment(id, State.PRISTINE,
                    context, array);
            fragments.add(fragment);
        }
    }

    public void updateSingleRow(SimpleFragment row) throws StorageException {
        Collection<String> dirty = row.getDirty();
        if (dirty.isEmpty()) {
            return;
        }
        String tableName = row.getTableName();
        SQLInfoSelect update = sqlInfo.getUpdateById(tableName, dirty);
        try {
            PreparedStatement ps = connection.prepareStatement(update.sql);
            try {
                if (logger.isLogEnabled()) {
                    logger.logSQL(update.sql, update.whatColumns, row);
                }
                int i = 0;
                for (Column column : update.whatColumns) {
                    i++;
                    String key = column.getKey();
                    Serializable v;
                    if (key.equals(Model.MAIN_KEY)) {
                        v = row.getId();
                    } else {
                        v = row.get(key);
                    }
                    column.setToPreparedStatement(ps, i, v);
                }
                int count = ps.executeUpdate();
                logger.logCount(count);
            } finally {
                closePreparedStatement(ps);
            }
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Could not update: " + update.sql, e);
        }
        row.clearDirty();
    }

    /**
     * Updates a row in the database with given explicit values.
     */
    protected void updateSingleRowWithValues(String tableName, Serializable id,
            Map<String, Serializable> map) throws StorageException {
        Update update = sqlInfo.getUpdateByIdForKeys(tableName, map.keySet());
        Table table = update.getTable();
        String sql = update.getStatement();
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                if (logger.isLogEnabled()) {
                    List<Serializable> values = new LinkedList<Serializable>();
                    values.addAll(map.values());
                    values.add(id);
                    logger.logSQL(sql, values);
                }
                int i = 1;
                for (Entry<String, Serializable> entry : map.entrySet()) {
                    String key = entry.getKey();
                    Serializable value = entry.getValue();
                    table.getColumn(key).setToPreparedStatement(ps, i++, value);
                }
                ps.setObject(i, id);
                int count = ps.executeUpdate();
                logger.logCount(count);
            } finally {
                closePreparedStatement(ps);
            }
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Could not update: " + sql, e);
        }
    }

    public void updateCollectionRows(CollectionFragment fragment)
            throws StorageException {
        if (!fragment.isDirty()) {
            return;
        }
        deleteFragment(fragment);
        insertCollectionRows(fragment);
        fragment.setDirty(false);
    }

    public void deleteFragment(Fragment fragment) throws StorageException {
        try {
            deleteFragment(fragment.getTableName(), fragment.getId());
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Could not delete: "
                    + fragment.getId().toString(), e);
        }
    }

    /**
     * Deletes a fragment, and returns {@code true} if there really were rows
     * deleted.
     */
    protected boolean deleteFragment(String tableName, Serializable id)
            throws SQLException {
        String sql = sqlInfo.getDeleteSql(tableName);
        if (logger.isLogEnabled()) {
            logger.logSQL(sql, Collections.singletonList(id));
        }
        PreparedStatement ps = connection.prepareStatement(sql);
        try {
            ps.setObject(1, id);
            int count = ps.executeUpdate();
            logger.logCount(count);
            return count > 0;
        } finally {
            closePreparedStatement(ps);
        }
    }

    public Serializable copyHierarchy(Serializable sourceId, String typeName,
            Serializable destParentId, String destName,
            Serializable overwriteId, Map<String, Serializable> overwriteMap,
            PersistenceContext persistenceContext) throws StorageException {
        // assert !model.separateMainTable; // other case not implemented
        HierarchyContext hierContext = persistenceContext.getHierContext();
        try {
            Map<Serializable, Serializable> idMap = new LinkedHashMap<Serializable, Serializable>();
            Map<Serializable, String> idType = new HashMap<Serializable, String>();
            // copy the hierarchy fragments recursively
            if (overwriteId != null) {
                // overwrite hier root with explicit values
                updateSingleRowWithValues(model.hierTableName, overwriteId,
                        overwriteMap);
                idMap.put(sourceId, overwriteId);
                // invalidate
                hierContext.markInvalidated(overwriteId, true);
            }
            // create the new hierarchy by copy
            Serializable newRootId = copyHierRecursive(sourceId, typeName,
                    destParentId, destName, overwriteId, idMap, idType);
            // invalidate children
            hierContext.markChildrenAdded(overwriteId == null ? destParentId
                    : overwriteId);
            // copy all collected fragments
            for (Entry<String, Set<Serializable>> entry : model.getPerFragmentIds(
                    idType).entrySet()) {
                String tableName = entry.getKey();
                // TODO move ACL skip logic higher
                if (tableName.equals(Model.ACL_TABLE_NAME)) {
                    continue;
                }
                Set<Serializable> ids = entry.getValue();
                boolean overwrite = (overwriteId != null)
                        && !tableName.equals(model.mainTableName);
                Boolean invalidation = copyFragments(tableName, ids, idMap,
                        overwrite ? overwriteId : null);
                if (invalidation != null) {
                    // make sure things are properly invalidated in this and
                    // other sessions
                    persistenceContext.getContext(tableName).markInvalidated(
                            overwriteId, invalidation.booleanValue());
                }
            }
            return newRootId;
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException(
                    "Could not copy: " + sourceId.toString(), e);
        }
    }

    /**
     * Copies hierarchy from id to parentId, and recurses.
     * <p>
     * If name is {@code null}, then the original name is kept.
     * <p>
     * {@code idMap} is filled with info about the correspondence between
     * original and copied ids. {@code idType} is filled with the type of each
     * (source) fragment.
     * <p>
     * TODO: this should be optimized to use a stored procedure.
     *
     * @param overwriteId when not {@code null}, the copy is done onto this
     *            existing node (skipped)
     * @return the new root id
     */
    protected Serializable copyHierRecursive(Serializable id, String type,
            Serializable parentId, String name, Serializable overwriteId,
            Map<Serializable, Serializable> idMap,
            Map<Serializable, String> idType) throws SQLException {
        idType.put(id, type);
        Serializable newId;
        if (overwriteId == null) {
            newId = copyHier(id, type, parentId, name, idMap);
        } else {
            newId = overwriteId;
            idMap.put(id, newId);
        }
        // recurse in children
        boolean onlyComplex = parentId == null;
        for (Serializable[] info : getChildrenIds(id, onlyComplex)) {
            Serializable childId = info[0];
            String childType = (String) info[1];
            copyHierRecursive(childId, childType, newId, null, null, idMap,
                    idType);
        }
        return newId;
    }

    /**
     * Copies hierarchy from id to a new child of parentId.
     * <p>
     * If name is {@code null}, then the original name is kept.
     * <p>
     * {@code idMap} is filled with info about the correspondence between
     * original and copied ids. {@code idType} is filled with the type of each
     * (source) fragment.
     *
     * @return the new id
     */
    protected Serializable copyHier(Serializable id, String type,
            Serializable parentId, String name,
            Map<Serializable, Serializable> idMap) throws SQLException {
        boolean createVersion = parentId == null;
        boolean explicitName = name != null;
        Serializable newId = null;

        String sql = sqlInfo.getCopyHierSql(explicitName, createVersion);
        PreparedStatement ps = connection.prepareStatement(sql);
        try {
            // TODO DB_IDENTITY
            newId = model.generateNewId();

            List<Serializable> debugValues = null;
            if (logger.isLogEnabled()) {
                debugValues = new ArrayList<Serializable>(4);
            }
            List<Column> columns = sqlInfo.getCopyHierColumns(explicitName,
                    createVersion);
            Column whereColumn = sqlInfo.getCopyHierWhereColumn();
            ps = connection.prepareStatement(sql);
            int i = 1;
            for (Column column : columns) {
                String key = column.getKey();
                Serializable v;
                if (key.equals(Model.HIER_PARENT_KEY)) {
                    v = parentId;
                } else if (key.equals(Model.HIER_CHILD_NAME_KEY)) {
                    // present if name explicitely set (first iteration)
                    v = name;
                } else if (key.equals(Model.MAIN_KEY)) {
                    // present if APP_UUID generation
                    v = newId;
                } else if (createVersion
                        && (key.equals(Model.MAIN_BASE_VERSION_KEY) || key.equals(Model.MAIN_CHECKED_IN_KEY))) {
                    v = null;
                } else {
                    throw new RuntimeException(column.toString());
                }
                column.setToPreparedStatement(ps, i++, v);
                if (debugValues != null) {
                    debugValues.add(v);
                }
            }
            // last parameter is for 'WHERE "id" = ?'
            whereColumn.setToPreparedStatement(ps, i, id);
            if (debugValues != null) {
                debugValues.add(id);
                logger.logSQL(sql, debugValues);
            }
            int count = ps.executeUpdate();
            logger.logCount(count);

            // TODO DB_IDENTITY
            // post insert fetch idrow

            idMap.put(id, newId);
        } finally {
            closePreparedStatement(ps);
        }
        return newId;
    }

    /**
     * Gets the children ids and types of a node.
     */
    protected List<Serializable[]> getChildrenIds(Serializable id,
            boolean onlyComplex) throws SQLException {
        List<Serializable[]> childrenIds = new LinkedList<Serializable[]>();
        String sql = sqlInfo.getSelectChildrenIdsAndTypesSql(onlyComplex);
        if (logger.isLogEnabled()) {
            logger.logSQL(sql, Collections.singletonList(id));
        }
        List<Column> columns = sqlInfo.getSelectChildrenIdsAndTypesWhatColumns();
        PreparedStatement ps = connection.prepareStatement(sql);
        try {
            List<String> debugValues = null;
            if (logger.isLogEnabled()) {
                debugValues = new LinkedList<String>();
            }
            ps.setObject(1, id); // parent id
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Serializable childId = null;
                Serializable childType = null;
                int i = 1;
                for (Column column : columns) {
                    String key = column.getKey();
                    Serializable value = column.getFromResultSet(rs, i++);
                    if (key.equals(Model.MAIN_KEY)) {
                        childId = value;
                    } else if (key.equals(Model.MAIN_PRIMARY_TYPE_KEY)) {
                        childType = value;
                    }
                }
                childrenIds.add(new Serializable[] { childId, childType });
                if (debugValues != null) {
                    debugValues.add(childId + "/" + childType);
                }
            }
            if (debugValues != null) {
                logger.log("  -> " + debugValues);
            }
            return childrenIds;
        } finally {
            closePreparedStatement(ps);
        }
    }

    /**
     * Copy the rows from tableName with ids in fragmentIds into new ones with
     * new ids given by idMap.
     *
     * @return {@link Boolean#TRUE} for a modification or creation,
     *         {@link Boolean#FALSE} for a deletion, {@code null} otherwise
     *         (still absent)
     * @throws SQLException
     */
    protected Boolean copyFragments(String tableName, Set<Serializable> ids,
            Map<Serializable, Serializable> idMap, Serializable overwriteId)
            throws SQLException {
        String copySql = sqlInfo.getCopySql(tableName);
        Column copyIdColumn = sqlInfo.getCopyIdColumn(tableName);
        PreparedStatement copyPs = connection.prepareStatement(copySql);
        String deleteSql = sqlInfo.getDeleteSql(tableName);
        PreparedStatement deletePs = connection.prepareStatement(deleteSql);
        try {
            boolean before = false;
            boolean after = false;
            for (Serializable id : ids) {
                Serializable newId = idMap.get(id);
                boolean overwrite = newId.equals(overwriteId);
                if (overwrite) {
                    // remove existing first
                    if (logger.isLogEnabled()) {
                        logger.logSQL(deleteSql,
                                Collections.singletonList(newId));
                    }
                    deletePs.setObject(1, newId);
                    int delCount = deletePs.executeUpdate();
                    logger.logCount(delCount);
                    before = delCount > 0;
                }
                copyIdColumn.setToPreparedStatement(copyPs, 1, newId);
                copyIdColumn.setToPreparedStatement(copyPs, 2, id);
                if (logger.isLogEnabled()) {
                    logger.logSQL(copySql, Arrays.asList(newId, id));
                }
                int copyCount = copyPs.executeUpdate();
                logger.logCount(copyCount);
                if (overwrite) {
                    after = copyCount > 0;
                }
            }
            // * , n -> mod (TRUE)
            // n , 0 -> del (FALSE)
            // 0 , 0 -> null
            return after ? Boolean.TRUE : (before ? Boolean.FALSE : null);
        } finally {
            closePreparedStatement(copyPs);
            closePreparedStatement(deletePs);
        }
    }

    public Serializable getVersionByLabel(Serializable versionableId,
            String label, Context context) throws StorageException {
        SQLInfoSelect select = sqlInfo.selectVersionsByLabel;
        Map<String, Serializable> criteriaMap = new HashMap<String, Serializable>();
        criteriaMap.put(Model.VERSION_VERSIONABLE_KEY, versionableId);
        criteriaMap.put(Model.VERSION_LABEL_KEY, label);
        List<SimpleFragment> selectRows = getSelectRows(select, criteriaMap,
                context);
        if (selectRows.isEmpty()) {
            return null;
        } else {
            return selectRows.get(0).getId();
        }
    }

    public SimpleFragment getLastVersion(Serializable versionableId,
            Context context) throws StorageException {
        SQLInfoSelect select = sqlInfo.selectVersionsByVersionableLastFirst;
        Map<String, Serializable> criteriaMap = new HashMap<String, Serializable>();
        criteriaMap.put(Model.VERSION_VERSIONABLE_KEY, versionableId);
        return getSelectRow(select, criteriaMap, context);
    }

    public List<SimpleFragment> getVersions(Serializable versionableId,
            Context context) throws StorageException {
        SQLInfoSelect select = sqlInfo.selectVersionsByVersionable;
        Map<String, Serializable> criteriaMap = new HashMap<String, Serializable>();
        criteriaMap.put(Model.VERSION_VERSIONABLE_KEY, versionableId);
        return getSelectRows(select, criteriaMap, context);
    }

    public List<SimpleFragment> getProxies(Serializable searchId,
            boolean byTarget, Serializable parentId, Context context)
            throws StorageException {
        Map<String, Serializable> criteriaMap = new HashMap<String, Serializable>();
        criteriaMap.put(byTarget ? Model.PROXY_TARGET_KEY
                : Model.PROXY_VERSIONABLE_KEY, searchId);
        if (parentId == null) {
            SQLInfoSelect select = byTarget ? sqlInfo.selectProxiesByTarget
                    : sqlInfo.selectProxiesByVersionable;
            return getSelectRows(select, criteriaMap, context);
        } else {
            SQLInfoSelect select = byTarget ? sqlInfo.selectProxiesByTargetAndParent
                    : sqlInfo.selectProxiesByVersionableAndParent;
            Map<String, Serializable> joinMap = new HashMap<String, Serializable>();
            joinMap.put(Model.HIER_PARENT_KEY, parentId);
            return getSelectRows(select, criteriaMap, joinMap, context);
        }
    }

    protected QueryMaker findQueryMaker(String query) throws StorageException {
        for (Class<? extends QueryMaker> klass : queryMakerService.getQueryMakers()) {
            QueryMaker queryMaker;
            try {
                queryMaker = klass.newInstance();
            } catch (Exception e) {
                throw new StorageException(e);
            }
            if (queryMaker.accepts(query)) {
                return queryMaker;
            }
        }
        return null;
    }

    public PartialList<Serializable> query(String query,
            QueryFilter queryFilter, boolean countTotal, Session session)
            throws StorageException {
        QueryMaker queryMaker = findQueryMaker(query);
        if (queryMaker == null) {
            throw new StorageException("No QueryMaker accepts query: " + query);
        }
        QueryMaker.Query q = queryMaker.buildQuery(sqlInfo, model, session,
                query, queryFilter);

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
            if (countTotal) {
                sql += " -- COUNT TOTAL";
            }
            logger.logSQL(sql, q.selectParams);
        }

        String sql = q.selectInfo.sql;

        if (!countTotal && limit > 0 && sqlInfo.dialect.supportsPaging()) {
            // full result set not needed for counting
            sql += " " + sqlInfo.dialect.getPagingClause(limit, offset);
            limit = 0;
            offset = 0;
        }

        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql,
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            int i = 1;
            for (Object object : q.selectParams) {
                if (object instanceof Calendar) {
                    Calendar cal = (Calendar) object;
                    Timestamp ts = new Timestamp(cal.getTimeInMillis());
                    ps.setTimestamp(i++, ts, cal); // cal passed for timezone
                } else if (object instanceof String[]) {
                    Array array = sqlInfo.dialect.createArrayOf(Types.VARCHAR,
                            (Object[]) object, connection);
                    ps.setArray(i++, array);
                } else {
                    ps.setObject(i++, object);
                }
            }
            ResultSet rs = ps.executeQuery();

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
            if (countTotal && (totalSize == -1)) {
                if (!available && (rowNum != 0)) {
                    // last row read was the actual last
                    totalSize = rowNum;
                } else {
                    // available if limit reached with some left
                    // rowNum == 0 if skipped too far
                    rs.last();
                    totalSize = rs.getRow();
                }
            }

            if (logger.isLogEnabled()) {
                logger.logIds(ids, countTotal, totalSize);
            }

            return new PartialList<Serializable>(ids, totalSize);
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Invalid query: " + query, e);
        } finally {
            if (ps != null) {
                try {
                    closePreparedStatement(ps);
                } catch (SQLException e) {
                    log.error("Cannot close connection", e);
                }
            }
        }
    }

    // queryFilter used for principals and permissions
    public IterableQueryResult queryAndFetch(String query, String queryType,
            QueryFilter queryFilter, Session session, Object... params)
            throws StorageException {
        QueryMaker queryMaker = findQueryMaker(queryType);
        if (queryMaker == null) {
            throw new StorageException("No QueryMaker accepts query: "
                    + queryType + ": " + query);
        }
        try {
            return new ResultSetQueryResult(queryMaker, query, queryFilter,
                    session, this, params);
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Invalid query: " + queryType + ": "
                    + query, e);
        }
    }

    public void updateReadAcls() throws StorageException {
        if (!sqlInfo.dialect.supportsReadAcl()) {
            return;
        }
        log.debug("updateReadAcls: updating ...");
        try {
            Statement st = connection.createStatement();
            st.execute(sqlInfo.dialect.getUpdateReadAclsSql());
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Failed to update read acls", e);
        }
        log.debug("updateReadAcls: done.");
    }

    public void rebuildReadAcls() throws StorageException {
        if (!sqlInfo.dialect.supportsReadAcl()) {
            return;
        }
        log.debug("rebuildReadAcls: rebuilding ...");
        try {
            Statement st = connection.createStatement();
            st.execute(sqlInfo.dialect.getRebuildReadAclsSql());
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Failed to rebuild read acls", e);
        }
        log.debug("rebuildReadAcls: done.");
    }

    /*
     * ----- XAResource -----
     */

    public void start(Xid xid, int flags) throws XAException {
        try {
            xaresource.start(xid, flags);
        } catch (XAException e) {
            checkConnectionReset(e);
            log.error("XA error on start: " + e.getMessage());
            throw e;
        }
    }

    public void end(Xid xid, int flags) throws XAException {
        try {
            xaresource.end(xid, flags);
        } catch (XAException e) {
            log.error("XA error on end: " + e.getMessage());
            throw e;
        }
    }

    public int prepare(Xid xid) throws XAException {
        try {
            return xaresource.prepare(xid);
        } catch (XAException e) {
            log.error("XA error on prepare: " + e.getMessage());
            throw e;
        }
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        try {
            xaresource.commit(xid, onePhase);
        } catch (XAException e) {
            log.error("XA error on commit: " + e.getMessage());
            throw e;
        }
    }

    public void rollback(Xid xid) throws XAException {
        try {
            xaresource.rollback(xid);
        } catch (XAException e) {
            log.error("XA error on rollback: " + e.getMessage());
            throw e;
        }
    }

    public void forget(Xid xid) throws XAException {
        xaresource.forget(xid);
    }

    public Xid[] recover(int flag) throws XAException {
        return xaresource.recover(flag);
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        return xaresource.setTransactionTimeout(seconds);
    }

    public int getTransactionTimeout() throws XAException {
        return xaresource.getTransactionTimeout();
    }

    public boolean isSameRM(XAResource xares) throws XAException {
        throw new UnsupportedOperationException();
    }

}
