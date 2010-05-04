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
import org.nuxeo.ecm.core.storage.sql.Invalidations;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.PropertyType;
import org.nuxeo.ecm.core.storage.sql.Row;
import org.nuxeo.ecm.core.storage.sql.Session;
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
    private final long instanceNumber = instanceCounter.incrementAndGet();

    // for debug
    protected final JDBCMapperLogger logger = new JDBCMapperLogger(
            instanceNumber);

    private final QueryMakerService queryMakerService;

    /**
     * Creates a new Mapper.
     *
     * @param model the model
     * @param sqlInfo the sql info
     * @param xadatasource the XA datasource to use to get connections
     */
    public JDBCMapper(Model model, SQLInfo sqlInfo, XADataSource xadatasource)
            throws StorageException {
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

    public String getMapperId() {
        return "M"+instanceNumber;
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
            int kind = Invalidations.MODIFIED;
            while (true) {
                Map<String, Set<Serializable>> map = invalidations.getKindMap(kind);
                // turn fragment-based map into id-based map
                Map<Serializable, Set<String>> m = invertMap(map);
                for (Entry<Serializable, Set<String>> e : m.entrySet()) {
                    Serializable id = e.getKey();
                    String fragments = join(e.getValue(), ' ');
                    if (logger.isLogEnabled()) {
                        logger.logSQL(sql, Arrays.<Serializable> asList(id,
                                fragments, Long.valueOf(kind)));
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
                if (kind == Invalidations.MODIFIED) {
                    kind = Invalidations.DELETED;
                } else {
                    break;
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
                    logger.log("  -> " + model.MAIN_KEY + '=' + id);
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
                    if (key.equals(model.MAIN_KEY)) {
                        v = id;
                    } else if (key.equals(model.REPOINFO_REPONAME_KEY)) {
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
        return tableName.equals(model.ACL_TABLE_NAME) ? ACLCollectionIO.INSTANCE
                : ScalarCollectionIO.INSTANCE;
    }

    public Serializable insertSingleRow(String tableName, Row row)
            throws StorageException {
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
                int i = 1;
                for (Column column : columns) {
                    column.setToPreparedStatement(ps, i++,
                            row.get(column.getKey()));
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
        return row.id;
    }

    public void insertCollectionRows(String tableName, Serializable id,
            Serializable[] array) throws StorageException {
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
                getCollectionIO(tableName).setToPreparedStatement(id, array,
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
     * Fetch the rows for a select with fixed criteria given as a map.
     */
    protected List<Row> getSelectRows(SQLInfoSelect select,
            Map<String, Serializable> criteriaMap,
            Map<String, Serializable> joinMap, boolean limitToOne)
            throws StorageException {
        List<Row> list = new LinkedList<Row>();
        if (select.whatColumns.isEmpty()) {
            // happens when we fetch a fragment whose columns are all opaque
            // check it's a by-id query
            if (select.whereColumns.size() == 1
                    && select.whereColumns.get(0).getKey() == model.MAIN_KEY
                    && joinMap == null) {
                Row row = new Row(criteriaMap);
                // if (select.opaqueColumns != null) {
                // for (Column column : select.opaqueColumns) {
                // map.put(column.getKey(), Row.OPAQUE);
                // }
                // }
                list.add(row);
                return list;
            }
            // else do a useless select but the criteria are more complex and we
            // can't shortcut
        }
        if (joinMap == null) {
            joinMap = Collections.emptyMap();
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
                Row row = new Row(criteriaMap);
                i = 1;
                for (Column column : select.whatColumns) {
                    row.put(column.getKey(), column.getFromResultSet(rs, i++));
                }
                // if (select.opaqueColumns != null) {
                // for (Column column : select.opaqueColumns) {
                // row.putNew(column.getKey(), Row.OPAQUE);
                // }
                // }
                if (logger.isLogEnabled()) {
                    logger.logResultSet(rs, select.whatColumns);
                }
                list.add(row);
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

    public Row readSingleRow(String tableName, Serializable id)
            throws StorageException {
        SQLInfoSelect select = sqlInfo.selectFragmentById.get(tableName);
        Map<String, Serializable> criteriaMap = Collections.singletonMap(
                model.MAIN_KEY, id);
        List<Row> maps = getSelectRows(select, criteriaMap, null, true);
        return maps == null ? null : maps.get(0);
    }

    public List<Row> readMultipleRows(String tableName, List<Serializable> ids)
            throws StorageException {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        SQLInfoSelect select = sqlInfo.getSelectFragmentsByIds(tableName,
                ids.size());
        Map<String, Serializable> criteriaMap = Collections.singletonMap(
                model.MAIN_KEY, (Serializable) ids);
        return getSelectRows(select, criteriaMap, null, false);
    }

    public Row readChildHierRow(Serializable parentId, String childName,
            boolean complexProp) throws StorageException {
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
                    if (key.equals(model.HIER_PARENT_KEY)) {
                        v = parentId;
                    } else if (key.equals(model.HIER_CHILD_NAME_KEY)) {
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
                Row row = new Row((Serializable) null);
                i = 1;
                List<Column> columns = sqlInfo.getSelectByChildNameWhatColumns(complexProp);
                for (Column column : columns) {
                    row.put(column.getKey(), column.getFromResultSet(rs, i++));
                }
                row.put(model.HIER_PARENT_KEY, parentId);
                row.put(model.HIER_CHILD_NAME_KEY, childName);
                row.put(model.HIER_CHILD_ISPROPERTY_KEY,
                        Boolean.valueOf(complexProp));
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
                        if (column.getKey().equals(model.MAIN_KEY)) {
                            childId = column.getFromResultSet(rs, i);
                        }
                    }
                    log.error(String.format(
                            "Child '%s' appeared twice as child of %s "
                                    + "(%s and %s), renaming second to '%s'",
                            childName, parentId, row.id, childId, newName));
                    Row rename = new Row(childId);
                    rename.putNew(model.HIER_CHILD_NAME_KEY, newName);
                    updateSingleRowWithValues(model.HIER_TABLE_NAME, rename);
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

    public List<Row> readChildHierRows(Serializable parentId,
            boolean complexProp) throws StorageException {
        if (parentId == null) {
            throw new IllegalArgumentException("Illegal null parentId");
        }
        SQLInfoSelect select = sqlInfo.selectChildrenByIsProperty;
        Map<String, Serializable> criteriaMap = new HashMap<String, Serializable>();
        criteriaMap.put(model.HIER_PARENT_KEY, parentId);
        criteriaMap.put(model.HIER_CHILD_ISPROPERTY_KEY,
                Boolean.valueOf(complexProp));
        return getSelectRows(select, criteriaMap, null, false);
    }

    public Serializable[] readCollectionArray(String tableName, Serializable id)
            throws StorageException {
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

    public Map<Serializable, Serializable[]> readCollectionsArrays(
            String tableName, List<Serializable> ids) throws StorageException {
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
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

    public void updateSingleRow(String tableName, Row row, List<String> keys)
            throws StorageException {
        if (keys.isEmpty()) {
            return;
        }
        SQLInfoSelect update = sqlInfo.getUpdateById(tableName, keys);
        try {
            PreparedStatement ps = connection.prepareStatement(update.sql);
            try {
                if (logger.isLogEnabled()) {
                    logger.logSQL(update.sql, update.whatColumns, row);
                }
                int i = 1;
                for (Column column : update.whatColumns) {
                    column.setToPreparedStatement(ps, i++,
                            row.get(column.getKey()));
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
    }

    /**
     * Updates a row in the database with given explicit values.
     */
    protected void updateSingleRowWithValues(String tableName, Row row)
            throws StorageException {
        Update update = sqlInfo.getUpdateByIdForKeys(tableName, row.getKeys());
        Table table = update.getTable();
        String sql = update.getStatement();
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                if (logger.isLogEnabled()) {
                    List<Serializable> values = new LinkedList<Serializable>();
                    values.addAll(row.getValues());
                    values.add(row.id); // id last in SQL
                    logger.logSQL(sql, values);
                }
                int i = 1;
                Serializable[] data = row.getData();
                int size = row.getDataSize();
                for (int r = 0; r < size; r += 2) {
                    String key = (String) data[r];
                    Serializable value = data[r + 1];
                    table.getColumn(key).setToPreparedStatement(ps, i++, value);
                }
                ps.setObject(i, row.id); // id last in SQL
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

    public void updateCollectionRows(String tableName, Serializable id,
            Serializable[] array) throws StorageException {
        deleteRows(tableName, id);
        insertCollectionRows(tableName, id, array);
    }

    public void deleteRows(String tableName, Serializable id)
            throws StorageException {
        try {
            String sql = sqlInfo.getDeleteSql(tableName);
            if (logger.isLogEnabled()) {
                logger.logSQL(sql, Collections.singletonList(id));
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                ps.setObject(1, id);
                int count = ps.executeUpdate();
                logger.logCount(count);
            } finally {
                closePreparedStatement(ps);
            }
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Could not delete: " + id.toString(), e);
        }
    }

    public CopyHierarchyResult copyHierarchy(Serializable sourceId,
            String typeName, Serializable destParentId, String destName,
            Row overwriteRow) throws StorageException {
        // assert !model.separateMainTable; // other case not implemented
        Invalidations invalidations = new Invalidations();
        try {
            Map<Serializable, Serializable> idMap = new LinkedHashMap<Serializable, Serializable>();
            Map<Serializable, String> idToType = new HashMap<Serializable, String>();
            // copy the hierarchy fragments recursively
            Serializable overwriteId = overwriteRow == null ? null
                    : overwriteRow.id;
            if (overwriteId != null) {
                // overwrite hier root with explicit values
                String tableName = model.hierTableName;
                updateSingleRowWithValues(tableName, overwriteRow);
                idMap.put(sourceId, overwriteId);
                // invalidate
                invalidations.addModified(tableName,
                        Collections.singleton(overwriteId));
            }
            // create the new hierarchy by copy
            Serializable newRootId = copyHierRecursive(sourceId, typeName,
                    destParentId, destName, overwriteId, idMap, idToType);
            // invalidate children
            Serializable invalParentId = overwriteId == null ? destParentId
                    : overwriteId;
            if (invalParentId != null) { // null for a new version
                invalidations.addModified(Invalidations.PARENT,
                        Collections.singleton(invalParentId));
            }
            // copy all collected fragments
            for (Entry<String, Set<Serializable>> entry : model.getPerFragmentIds(
                    idToType).entrySet()) {
                String tableName = entry.getKey();
                // TODO move ACL skip logic higher
                if (tableName.equals(model.ACL_TABLE_NAME)) {
                    continue;
                }
                Set<Serializable> ids = entry.getValue();
                // boolean overwrite = overwriteId != null
                // && !tableName.equals(model.hierTableName);
                // overwrite ? overwriteId : null
                Boolean invalidation = copyRows(tableName, ids, idMap,
                        overwriteId);
                // TODO XXX check code:
                if (invalidation != null) {
                    // overwrote something
                    // make sure things are properly invalidated in this and
                    // other sessions
                    if (Boolean.TRUE.equals(invalidation)) {
                        invalidations.addModified(tableName,
                                Collections.singleton(overwriteId));
                    } else {
                        invalidations.addDeleted(tableName,
                                Collections.singleton(overwriteId));
                    }
                }
            }
            CopyHierarchyResult res = new CopyHierarchyResult();
            res.copyId = newRootId;
            res.invalidations = invalidations;
            return res;
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
            Map<Serializable, String> idToType) throws SQLException {
        idToType.put(id, type);
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
                    idToType);
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
                if (key.equals(model.HIER_PARENT_KEY)) {
                    v = parentId;
                } else if (key.equals(model.HIER_CHILD_NAME_KEY)) {
                    // present if name explicitely set (first iteration)
                    v = name;
                } else if (key.equals(model.MAIN_KEY)) {
                    // present if APP_UUID generation
                    v = newId;
                } else if (createVersion
                        && (key.equals(model.MAIN_BASE_VERSION_KEY) || key.equals(model.MAIN_CHECKED_IN_KEY))) {
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
                    if (key.equals(model.MAIN_KEY)) {
                        childId = value;
                    } else if (key.equals(model.MAIN_PRIMARY_TYPE_KEY)) {
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
     * Copy the rows from tableName with given ids into new ones with new ids
     * given by idMap.
     * <p>
     * A new row with id {@code overwriteId} is first deleted.
     *
     * @return {@link Boolean#TRUE} for a modification or creation,
     *         {@link Boolean#FALSE} for a deletion, {@code null} otherwise
     *         (still absent)
     * @throws SQLException
     */
    protected Boolean copyRows(String tableName, Set<Serializable> ids,
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

    public Serializable getVersionIdByLabel(Serializable versionableId,
            String label) throws StorageException {
        SQLInfoSelect select = sqlInfo.selectVersionsByLabel;
        Map<String, Serializable> criteriaMap = new HashMap<String, Serializable>();
        criteriaMap.put(model.VERSION_VERSIONABLE_KEY, versionableId);
        criteriaMap.put(model.VERSION_LABEL_KEY, label);
        List<Row> rows = getSelectRows(select, criteriaMap, null, true);
        return rows == null ? null : rows.get(0).id;
    }

    public Serializable getLastVersionId(Serializable versionableId)
            throws StorageException {
        SQLInfoSelect select = sqlInfo.selectVersionsByVersionableLastFirst;
        Map<String, Serializable> criteriaMap = Collections.singletonMap(
                model.VERSION_VERSIONABLE_KEY, versionableId);
        List<Row> maps = getSelectRows(select, criteriaMap, null, true);
        return maps == null ? null : maps.get(0).id;
    }

    public List<Row> getVersionsRows(Serializable versionableId)
            throws StorageException {
        SQLInfoSelect select = sqlInfo.selectVersionsByVersionable;
        Map<String, Serializable> criteriaMap = Collections.singletonMap(
                model.VERSION_VERSIONABLE_KEY, versionableId);
        return getSelectRows(select, criteriaMap, null, false);
    }

    public List<Row> getProxyRows(Serializable searchId, boolean byTarget,
            Serializable parentId) throws StorageException {
        Map<String, Serializable> criteriaMap = Collections.singletonMap(
                byTarget ? model.PROXY_TARGET_KEY : model.PROXY_VERSIONABLE_KEY,
                searchId);
        SQLInfoSelect select;
        Map<String, Serializable> joinMap;
        if (parentId == null) {
            select = byTarget ? sqlInfo.selectProxiesByTarget
                    : sqlInfo.selectProxiesByVersionable;
            joinMap = null;
        } else {
            select = byTarget ? sqlInfo.selectProxiesByTargetAndParent
                    : sqlInfo.selectProxiesByVersionableAndParent;
            joinMap = Collections.singletonMap(model.HIER_PARENT_KEY, parentId);
        }
        return getSelectRows(select, criteriaMap, joinMap, false);
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
            log.error("XA error on start: " + e);
            throw e;
        }
    }

    public void end(Xid xid, int flags) throws XAException {
        try {
            xaresource.end(xid, flags);
        } catch (NullPointerException e) {
            // H2 when no active transaction
            log.error("XA error on end: " + e, e);
            throw (XAException) new XAException(XAException.XAER_RMERR).initCause(e);
        } catch (XAException e) {
            log.error("XA error on end: " + e, e);
            throw e;
        }
    }

    public int prepare(Xid xid) throws XAException {
        try {
            return xaresource.prepare(xid);
        } catch (XAException e) {
            log.error("XA error on prepare: " + e);
            throw e;
        }
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        try {
            xaresource.commit(xid, onePhase);
        } catch (XAException e) {
            log.error("XA error on commit: " + e);
            throw e;
        }
    }

    public void rollback(Xid xid) throws XAException {
        try {
            xaresource.rollback(xid);
        } catch (XAException e) {
            log.error("XA error on rollback: " + e);
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
