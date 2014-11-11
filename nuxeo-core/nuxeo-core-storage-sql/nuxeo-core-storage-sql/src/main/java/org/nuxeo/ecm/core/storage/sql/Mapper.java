/*
 * (C) Copyright 2007-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql;

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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
import org.nuxeo.ecm.core.storage.sql.CollectionFragment.CollectionFragmentIterator;
import org.nuxeo.ecm.core.storage.sql.Fragment.State;
import org.nuxeo.ecm.core.storage.sql.SQLInfo.SQLInfoSelect;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.Table;
import org.nuxeo.ecm.core.storage.sql.db.Update;
import org.nuxeo.ecm.core.storage.sql.db.dialect.ConditionalStatement;
import org.nuxeo.ecm.core.storage.sql.db.dialect.Dialect;
import org.nuxeo.ecm.core.storage.sql.db.dialect.DialectOracle;

/**
 * A {@link Mapper} maps objects to and from the database. It is specific to a
 * given database connection, as it computes statements.
 * <p>
 * The {@link Mapper} does the mapping according to the policy defined by a
 * {@link Model}, and generates SQL statements recoreded in the {@link SQLInfo}.
 *
 * @author Florent Guillaume
 */
public class Mapper {

    private static final Log log = LogFactory.getLog(Mapper.class);

    protected static boolean debugTestUpgrade;

    /** The repository from which this was built. */
    private final RepositoryImpl repository;

    /** The model used to do the mapping. */
    private final Model model;

    /** The SQL information. */
    private final SQLInfo sqlInfo;

    /** The xa datasource. */
    private final XADataSource xadatasource;

    /** The xa pooled connection. */
    private XAConnection xaconnection;

    /** The actual connection. */
    private Connection connection;

    private XAResource xaresource;

    // for debug
    private static final AtomicLong instanceCounter = new AtomicLong(0);

    // for debug
    protected final long instanceNumber = instanceCounter.incrementAndGet();

    private static final int DEBUG_MAX_ARRAY = 10;

    private static final int DEBUG_MAX_STRING = 100;

    /**
     * Creates a new Mapper.
     *
     * @param repository the repository
     * @param model the model
     * @param sqlInfo the sql info
     * @param xadatasource the XA datasource to use to get connections
     */
    public Mapper(RepositoryImpl repository, Model model, SQLInfo sqlInfo,
            XADataSource xadatasource) throws StorageException {
        this.repository = repository;
        this.model = model;
        this.sqlInfo = sqlInfo;
        this.xadatasource = xadatasource;
        resetConnection();
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

    protected Model getModel() {
        return model;
    }

    protected int getTableSize(String tableName) {
        return sqlInfo.getDatabase().getTable(tableName).getColumns().size();
    }

    // for debug
    private static boolean isLogEnabled() {
        return log.isTraceEnabled();
    }

    // for debug
    private void logCount(int count) {
        if ((count > 0) && isLogEnabled()) {
            log("  -> " + count + " row" + (count > 1 ? "s" : ""));
        }
    }

    // for debug
    private void log(String string) {
        log.trace("(" + instanceNumber + ") SQL: " + string);
    }

    // for debug
    private void logResultSet(ResultSet rs, List<Column> columns)
            throws SQLException {
        List<String> res = new LinkedList<String>();
        int i = 0;
        for (Column column : columns) {
            i++;
            Serializable v = column.getFromResultSet(rs, i);
            res.add(column.getKey() + "=" + loggedValue(v));
        }
        log("  -> " + StringUtils.join(res, ", "));
    }

    // for debug
    private void logSQL(String sql, List<Column> columns, SimpleFragment row) {
        List<Serializable> values = new ArrayList<Serializable>(columns.size());
        for (Column column : columns) {
            String key = column.getKey();
            Serializable value;
            if (key.equals(Model.MAIN_KEY)) {
                value = row.getId();
            } else {
                try {
                    value = row.get(key);
                } catch (StorageException e) {
                    // cannot happen
                    value = "ACCESSFAILED";
                }
            }
            values.add(value);
        }
        logSQL(sql, values);
    }

    // for debug
    private void logSQL(String sql, List<Serializable> values) {
        StringBuilder buf = new StringBuilder();
        int start = 0;
        for (Serializable v : values) {
            int index = sql.indexOf('?', start);
            if (index == -1) {
                // mismatch between number of ? and number of values
                break;
            }
            buf.append(sql, start, index);
            buf.append(loggedValue(v));
            start = index + 1;
        }
        buf.append(sql, start, sql.length());
        log(buf.toString());
    }

    /**
     * Returns a loggable value using pseudo-SQL syntax.
     */
    @SuppressWarnings("boxing")
    private static String loggedValue(Serializable value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            String v = (String) value;
            if (v.length() > DEBUG_MAX_STRING) {
                v = v.substring(0, DEBUG_MAX_STRING) + "...(" + v.length()
                        + " chars)...";
            }
            return "'" + v.replace("'", "''") + "'";
        }
        if (value instanceof Calendar) {
            Calendar cal = (Calendar) value;
            char sign;
            int offset = cal.getTimeZone().getOffset(cal.getTimeInMillis()) / 60000;
            if (offset < 0) {
                offset = -offset;
                sign = '-';
            } else {
                sign = '+';
            }
            return String.format(
                    "TIMESTAMP '%04d-%02d-%02dT%02d:%02d:%02d.%03d%c%02d:%02d'",
                    cal.get(Calendar.YEAR), //
                    cal.get(Calendar.MONTH) + 1, //
                    cal.get(Calendar.DAY_OF_MONTH), //
                    cal.get(Calendar.HOUR_OF_DAY), //
                    cal.get(Calendar.MINUTE), //
                    cal.get(Calendar.SECOND), //
                    cal.get(Calendar.MILLISECOND), //
                    sign, offset / 60, offset % 60);
        }
        if (value instanceof Binary) {
            return "'" + ((Binary) value).getDigest() + "'";
        }
        if (value.getClass().isArray()) {
            Serializable[] v = (Serializable[]) value;
            StringBuilder b = new StringBuilder();
            b.append('[');
            for (int i = 0; i < v.length; i++) {
                if (i > 0) {
                    b.append(',');
                    if (i > DEBUG_MAX_ARRAY) {
                        b.append("...(" + v.length + " items)...");
                        break;
                    }
                }
                b.append(loggedValue(v[i]));
            }
            b.append(']');
            return b.toString();
        }
        return value.toString();
    }

    // ---------- low-level JDBC methods ----------

    protected static void closePreparedStatement(PreparedStatement ps)
            throws SQLException {
        try {
            ps.close();
        } catch (IllegalArgumentException e) {
            // ignore
            // http://bugs.mysql.com/35489 with JDBC 4 and driver <= 5.1.6
        }
    }

    /**
     * Creates the necessary structures in the database.
     */
    protected void createDatabase() throws StorageException {
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
                log(sql);
                st.execute(sql);
                for (String s : table.getPostCreateSqls()) {
                    log(s);
                    st.execute(s);
                }
                for (String s : sqlInfo.dialect.getPostCreateTableSqls(table,
                        model, sqlInfo.database)) {
                    log(s);
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
                    log(sql);
                    st.execute(sql);
                    for (String s : table.getPostAddSqls(column)) {
                        log(s);
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

    /**
     * Informs the cluster that this node exists.
     */
    protected void createClusterNode() throws StorageException {
        try {
            Statement st = connection.createStatement();
            String sql = sqlInfo.getCleanupClusterNodesSql();
            log(sql);
            int n = st.executeUpdate(sql);
            if (isLogEnabled()) {
                logCount(n);
            }
            sql = sqlInfo.getCreateClusterNodeSql();
            log(sql);
            st.execute(sql);
            st.close();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    /**
     * Removes this node from the cluster.
     */
    protected void removeClusterNode() throws StorageException {
        try {
            Statement st = connection.createStatement();
            String sql = sqlInfo.getRemoveClusterNodeSql();
            log(sql);
            st.execute(sql);
            st.close();
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException(e);
        }
    }

    /**
     * Inserts the invalidation rows for the other cluster nodes.
     */
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
                    if (isLogEnabled()) {
                        logSQL(sql, Arrays.<Serializable> asList(id, fragments,
                                Integer.valueOf(kind)));
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
                    log.error(e.getMessage());
                }
            }
        }
    }

    /**
     * Turns a map of fragment -> set-of-ids into a map of id ->
     * set-of-fragments.
     */
    public static Map<Serializable, Set<String>> invertMap(
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

    /**
     * Gets the invalidations from other cluster nodes.
     */
    public Invalidations getClusterInvalidations() throws StorageException {
        Invalidations invalidations = new Invalidations();
        String sql = sqlInfo.dialect.getClusterGetInvalidations();
        String sqldel = sqlInfo.dialect.getClusterDeleteInvalidations();
        List<Column> columns = sqlInfo.getClusterInvalidationsColumns();
        Statement st = null;
        try {
            st = connection.createStatement();
            if (isLogEnabled()) {
                log(sql);
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
            if (isLogEnabled()) {
                // logCount(n);
                log("  -> " + invalidations);
            }
            if (sqlInfo.dialect.isClusteringDeleteNeeded()) {
                if (isLogEnabled()) {
                    log(sqldel);
                }
                n = st.executeUpdate(sqldel);
                if (isLogEnabled()) {
                    logCount(n);
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
                    log.error(e.getMessage());
                }
            }
        }
    }

    /**
     * Gets the root id for a given repository, if registered.
     *
     * @param repositoryId the repository id, usually 0
     * @return the root id, or null if not found
     */
    protected Serializable getRootId(Serializable repositoryId)
            throws StorageException {
        String sql = sqlInfo.getSelectRootIdSql();
        try {
            if (isLogEnabled()) {
                logSQL(sql, Collections.singletonList(repositoryId));
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                ps.setObject(1, repositoryId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    if (isLogEnabled()) {
                        log("  -> (none)");
                    }
                    return null;
                }
                Column column = sqlInfo.getSelectRootIdWhatColumn();
                Serializable id = column.getFromResultSet(rs, 1);
                if (isLogEnabled()) {
                    log("  -> " + Model.MAIN_KEY + '=' + id);
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
                log(s.checkStatement);
                ResultSet rs = st.executeQuery(s.checkStatement);
                if (rs.next()) {
                    // already present
                    log("  -> (present)");
                    doPre = true;
                } else {
                    doPre = false;
                }
            }
            if (doPre) {
                log(s.preStatement);
                st.execute(s.preStatement);
            }
            log(s.statement);
            st.execute(s.statement);
        }
        st.close();
    }

    /**
     * Records the newly generated root id for a given repository.
     *
     * @param repositoryId the repository id, usually 0
     * @param id the root id
     */
    protected void setRootId(Serializable repositoryId, Serializable id)
            throws StorageException {
        String sql = sqlInfo.getInsertRootIdSql();
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                List<Column> columns = sqlInfo.getInsertRootIdColumns();
                List<Serializable> debugValues = null;
                if (isLogEnabled()) {
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
                    logSQL(sql, debugValues);
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

    /**
     * Inserts a new {@link SimpleFragment} in the storage. Depending on the
     * type, the id may be generated by the database (in which case it must not
     * be provided in the {@link SimpleFragment}), or already assigned (which is
     * the case for non-main tables).
     *
     * @param row the row
     * @return the id (generated or not)
     */
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
                if (isLogEnabled()) {
                    logSQL(sql, columns, row);
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
                    log.error("Cannot close connection", e);
                }
            }
        }
        row.clearDirty();
        return row.getId();
    }

    /**
     * Inserts a new {@link CollectionFragment} in the storage.
     *
     * @param fragment the fragment
     */
    public void insertCollectionRows(CollectionFragment fragment)
            throws StorageException {
        String tableName = fragment.getTableName();
        PreparedStatement ps = null;
        try {
            String sql = sqlInfo.getInsertSql(tableName);
            List<Column> columns = sqlInfo.getInsertColumns(tableName);
            try {
                Serializable id = fragment.getId();
                List<Serializable> debugValues = null;
                if (isLogEnabled()) {
                    debugValues = new ArrayList<Serializable>(3);
                }
                ps = connection.prepareStatement(sql);

                CollectionFragmentIterator it = fragment.getIterator();
                while (it.hasNext()) {
                    Serializable n = it.next();
                    it.setToPreparedStatement(columns, ps, model, debugValues);
                    if (debugValues != null) {
                        logSQL(sql, debugValues);
                        debugValues.clear();
                    }
                    ps.execute();
                }
            } catch (SQLException e) {
                checkConnectionReset(e);
                throw new StorageException("Could not insert: " + sql, e);
            }

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
            if (isLogEnabled()) {
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
                logSQL(select.sql, debugValues);
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
                if (isLogEnabled()) {
                    logResultSet(rs, select.whatColumns);
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
                    log.error(e.getMessage());
                }
            }
        }
    }

    /**
     * Gets the state for a {@link SimpleFragment} from the database, given its
     * table name and id. If the row doesn't exist, {@code null} is returned.
     *
     * @param tableName the type name
     * @param id the id
     * @param context the persistence context to which the read row is tied
     * @return the map, or {@code null}
     */
    public Map<String, Serializable> readSingleRowMap(String tableName,
            Serializable id, Context context) throws StorageException {
        SQLInfoSelect select = sqlInfo.selectFragmentById.get(tableName);
        Map<String, Serializable> criteriaMap = new HashMap<String, Serializable>();
        criteriaMap.put(Model.MAIN_KEY, id);
        List<Map<String, Serializable>> maps = getSelectMaps(select,
                criteriaMap, null, true, context);
        return maps == null ? null : maps.get(0);
    }

    /**
     * Gets the states for a list of {@link SimpleFragment}s from the database,
     * given the table name and their ids.
     *
     * @param tableName the type name
     * @param ids the ids
     * @param context the persistence context to which the read rows are tied
     * @return the map of fragment id to fragment values map
     */
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

    /**
     * Reads the hierarchy {@link SimpleFragment} for a child, given its parent
     * id and the child name.
     *
     * @param parentId the parent id
     * @param childName the child name
     * @param complexProp whether to get complex properties ({@code true}) or
     *            regular children({@code false})
     * @param context the persistence context to which the read row is tied
     * @return the child hierarchy row, or {@code null}
     */
    public SimpleFragment readChildHierRow(Serializable parentId,
            String childName, boolean complexProp, Context context)
            throws StorageException {
        String sql = sqlInfo.getSelectByChildNameSql(complexProp);
        try {
            // XXX statement should be already prepared
            List<Serializable> debugValues = null;
            if (isLogEnabled()) {
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
                    logSQL(sql, debugValues);
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
                if (isLogEnabled()) {
                    logResultSet(rs, columns);
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

    /**
     * Reads the hierarchy {@link SimpleFragment}s for all the children of
     * parent.
     * <p>
     * Rows that are already known to the persistence context are returned from
     * it, so as to never have duplicate objects for the same row.
     * <p>
     * Depending on the boolean {@literal complexProp}, only the complex
     * properties or only the regular children are returned.
     *
     * @param parentId the parent id
     * @param complexProp whether to get complex properties ({@code true}) or
     *            regular children({@code false})
     * @param context the persistence context to which the read rows are tied
     * @return the child hierarchy rows, or {@code null}
     */
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

    /**
     * Gets an array for a {@link CollectionFragment} from the database, given
     * its table name and id. If now rows are found, an empty array is returned.
     *
     * @param id the id
     * @param context the persistence context to which the read collection is
     *            tied
     * @return the array
     */
    public Serializable[] readCollectionArray(Serializable id, Context context)
            throws StorageException {
        String tableName = context.getTableName();
        String sql = sqlInfo.selectFragmentById.get(tableName).sql;
        try {
            // XXX statement should be already prepared
            if (isLogEnabled()) {
                logSQL(sql, Collections.singletonList(id));
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                List<Column> columns = sqlInfo.selectFragmentById.get(tableName).whatColumns;
                ps.setObject(1, id); // assumes only one primary column
                ResultSet rs = ps.executeQuery();

                // construct the resulting collection using each row
                Serializable[] array = model.newCollectionArray(rs, columns,
                        context);
                if (isLogEnabled()) {
                    log("  -> " + Arrays.asList(array));
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

    /**
     * Gets a per-id map of arrays for {@link CollectionFragment}s from the
     * database, given a table name and the ids.
     *
     * @param ids the ids
     * @param context the persistence context to which the read collection is
     *            tied
     * @return the map of id to array
     */
    public Map<Serializable, Serializable[]> readCollectionsArrays(
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
            if (isLogEnabled()) {
                logSQL(sql, ids);
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                int i = 1;
                for (Serializable id : ids) {
                    ps.setObject(i++, id);
                }
                ResultSet rs = ps.executeQuery();

                Map<Serializable, Serializable[]> res = model.newCollectionArrays(
                        rs, select.whatColumns, context);
                // fill empty ones
                for (Serializable id : ids) {
                    if (!res.containsKey(id)) {
                        Serializable[] array = model.getCollectionFragmentType(
                                tableName).getEmptyArray();
                        res.put(id, array);
                    }
                }
                if (isLogEnabled()) {
                    for (Entry<Serializable, Serializable[]> entry : res.entrySet()) {
                        log("  -> " + entry.getKey() + " = "
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

    /**
     * Updates a row in the database.
     *
     * @param row the row
     * @throws StorageException
     */
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
                if (isLogEnabled()) {
                    logSQL(update.sql, update.whatColumns, row);
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
                logCount(count);
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
    public void updateSingleRowWithValues(String tableName, Serializable id,
            Map<String, Serializable> map) throws StorageException {
        Update update = sqlInfo.getUpdateByIdForKeys(tableName, map.keySet());
        Table table = update.getTable();
        String sql = update.getStatement();
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                if (isLogEnabled()) {
                    List<Serializable> values = new LinkedList<Serializable>();
                    values.addAll(map.values());
                    values.add(id);
                    logSQL(sql, values);
                }
                int i = 1;
                for (Entry<String, Serializable> entry : map.entrySet()) {
                    String key = entry.getKey();
                    Serializable value = entry.getValue();
                    table.getColumn(key).setToPreparedStatement(ps, i++, value);
                }
                ps.setObject(i, id);
                int count = ps.executeUpdate();
                logCount(count);
            } finally {
                closePreparedStatement(ps);
            }
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Could not update: " + sql, e);
        }
    }

    /**
     * Updates a {@link CollectionFragment} in the database.
     * <p>
     * Does a simple delete + insert for now.
     *
     * @param fragment the fragment
     * @throws StorageException
     */
    public void updateCollectionRows(CollectionFragment fragment)
            throws StorageException {
        if (!fragment.isDirty()) {
            return;
        }
        deleteFragment(fragment);
        insertCollectionRows(fragment);
        fragment.setDirty(false);
    }

    /**
     * Deletes a fragment from the database (one or several rows).
     *
     * @param fragment the fragment
     */
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
        if (isLogEnabled()) {
            logSQL(sql, Collections.singletonList(id));
        }
        PreparedStatement ps = connection.prepareStatement(sql);
        try {
            ps.setObject(1, id);
            int count = ps.executeUpdate();
            logCount(count);
            return count > 0;
        } finally {
            closePreparedStatement(ps);
        }
    }

    /**
     * Copies the hierarchy starting from a given fragment to a new parent with
     * a new name.
     * <p>
     * If the new parent is {@code null}, then this is a version creation, which
     * doesn't recurse in regular children.
     * <p>
     * If {@code overwriteId} and {@code overwriteMap} are passed, the copy is
     * done onto this existing node as its root (version restore) instead of
     * creating a new node in the parent.
     *
     * @param sourceId the id of fragment to copy (with children)
     * @param typeName the type of the fragment to copy (to avoid refetching
     *            known info)
     * @param destParentId the new parent id, or {@code null}
     * @param destName the new name
     * @param overwriteId when not {@code null}, the copy is done onto this
     *            existing root id
     * @param overwriteMap when overwriting, set these hierarchy columns
     * @param persistenceContext the persistence context, to invalidate
     *            fragments when overwriting
     * @return the id of the root of the copy
     * @throws StorageException
     */
    public Serializable copyHierarchy(Serializable sourceId, String typeName,
            Serializable destParentId, String destName,
            Serializable overwriteId, Map<String, Serializable> overwriteMap,
            PersistenceContext persistenceContext) throws StorageException {
        assert !model.separateMainTable; // other case not implemented
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
            if (isLogEnabled()) {
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
                logSQL(sql, debugValues);
            }
            int count = ps.executeUpdate();
            logCount(count);

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
        if (isLogEnabled()) {
            logSQL(sql, Collections.singletonList(id));
        }
        List<Column> columns = sqlInfo.getSelectChildrenIdsAndTypesWhatColumns();
        PreparedStatement ps = connection.prepareStatement(sql);
        try {
            List<String> debugValues = null;
            if (isLogEnabled()) {
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
                log("  -> " + debugValues);
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
                    if (isLogEnabled()) {
                        logSQL(deleteSql, Collections.singletonList(newId));
                    }
                    deletePs.setObject(1, newId);
                    int delCount = deletePs.executeUpdate();
                    logCount(delCount);
                    before = delCount > 0;
                }
                copyIdColumn.setToPreparedStatement(copyPs, 1, newId);
                copyIdColumn.setToPreparedStatement(copyPs, 2, id);
                if (isLogEnabled()) {
                    logSQL(copySql, Arrays.asList(newId, id));
                }
                int copyCount = copyPs.executeUpdate();
                logCount(copyCount);
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

    /**
     * Gets the id of a version given a versionableId and a label.
     *
     * @param versionableId the versionable id
     * @param label the label
     * @param context the versions context
     * @return the id of the version, or {@code null} if not found
     * @throws StorageException
     */
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

    /**
     * Gets id of the last version given a versionable id.
     *
     * @param versionableId the versionable id
     * @param context the version fragment context
     * @return the id of the last version, or {@code null} if not found
     * @throws StorageException
     */
    public SimpleFragment getLastVersion(Serializable versionableId,
            Context context) throws StorageException {
        SQLInfoSelect select = sqlInfo.selectVersionsByVersionableLastFirst;
        Map<String, Serializable> criteriaMap = new HashMap<String, Serializable>();
        criteriaMap.put(Model.VERSION_VERSIONABLE_KEY, versionableId);
        return getSelectRow(select, criteriaMap, context);
    }

    /**
     * Gets the list of version fragments for all the versions having a given
     * versionable id.
     *
     * @param versionableId the versionable id
     * @param context the version fragment context
     * @return the list of version fragments
     * @throws StorageException
     */
    public List<SimpleFragment> getVersions(Serializable versionableId,
            Context context) throws StorageException {
        SQLInfoSelect select = sqlInfo.selectVersionsByVersionable;
        Map<String, Serializable> criteriaMap = new HashMap<String, Serializable>();
        criteriaMap.put(Model.VERSION_VERSIONABLE_KEY, versionableId);
        return getSelectRows(select, criteriaMap, context);
    }

    /**
     * Finds proxies, maybe restricted to the children of a given parent.
     *
     * @param searchId the id to look for
     * @param byTarget {@code true} if the searchId is a proxy target id,
     *            {@code false} if the searchId is a versionable id
     * @param parentId the parent to which to restrict, if not {@code null}
     * @param context the proxies fragment context
     * @return the list of proxies fragments
     * @throws StorageException
     */
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
        List<Class<?>> classes = repository.getRepositoryDescriptor().queryMakerClasses;
        if (classes.isEmpty()) {
            classes.add(NXQLQueryMaker.class);
        }
        QueryMaker queryMaker = null;
        for (Class<?> klass : classes) {
            // build QueryMaker instance
            try {
                queryMaker = (QueryMaker) klass.newInstance();
            } catch (Exception e) {
                throw new StorageException("Cannot instantiate class: "
                        + klass.getName(), e);
            }
            // check if it accepts the query
            if (!queryMaker.accepts(query)) {
                queryMaker = null;
                continue;
            }
            break;
        }
        return queryMaker;
    }

    /**
     * Makes a NXQL query to the database.
     *
     * @param query the query
     * @param queryFilter the query filter
     * @param countTotal if {@code true}, count the total size without
     *            limit/offset
     * @param session the current session (to resolve paths)
     * @return the list of matching document ids
     * @throws StorageException
     * @throws SQLException
     */
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
            log("Query cannot return anything due to conflicting clauses");
            return new PartialList<Serializable>(
                    Collections.<Serializable> emptyList(), 0);
        }

        long limit = queryFilter.getLimit();
        long offset = queryFilter.getOffset();
        if (isLogEnabled()) {
            String sql = q.selectInfo.sql;
            if (limit != 0) {
                sql += " -- LIMIT " + limit + " OFFSET " + offset;
            }
            if (countTotal) {
                sql += " -- COUNT TOTAL";
            }
            logSQL(sql, q.selectParams);
        }
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(q.selectInfo.sql,
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

            if (isLogEnabled()) {
                List<Serializable> debugIds = ids;
                String end = "";
                if (ids.size() > DEBUG_MAX_ARRAY) {
                    debugIds = new ArrayList<Serializable>(DEBUG_MAX_ARRAY);
                    i = 0;
                    for (Serializable id : ids) {
                        debugIds.add(id);
                        i++;
                        if (i == DEBUG_MAX_ARRAY) {
                            break;
                        }
                    }
                    end = "...(" + ids.size() + " ids)...";
                }
                if (countTotal) {
                    end += " (total " + totalSize + ')';
                }
                log("  -> " + debugIds + end);
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
            QueryFilter queryFilter, boolean countTotal, Session session,
            Object... params) throws StorageException {
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

    protected static class ResultSetQueryResult implements IterableQueryResult,
            Iterator<Map<String, Serializable>> {

        private final long instanceNumber;

        private QueryMaker.Query q;

        private PreparedStatement ps;

        private ResultSet rs;

        private Map<String, Serializable> next;

        private boolean eof;

        private long pos;

        private long size = -1;

        protected ResultSetQueryResult(QueryMaker queryMaker, String query,
                QueryFilter queryFilter, Session session, Mapper mapper,
                Object... params) throws StorageException, SQLException {
            instanceNumber = mapper.instanceNumber;
            q = queryMaker.buildQuery(mapper.sqlInfo, mapper.model, session,
                    query, queryFilter, params);
            if (q == null) {
                log("Query cannot return anything due to conflicting clauses");
                ps = null;
                rs = null;
                eof = true;
                return;
            } else {
                eof = false;
            }
            if (isLogEnabled()) {
                mapper.logSQL(q.selectInfo.sql, q.selectParams);
            }
            ps = mapper.connection.prepareStatement(q.selectInfo.sql,
                    ResultSet.TYPE_SCROLL_INSENSITIVE,
                    ResultSet.CONCUR_READ_ONLY);
            int i = 1;
            for (Object object : q.selectParams) {
                if (object instanceof Calendar) {
                    Calendar cal = (Calendar) object;
                    Timestamp ts = new Timestamp(cal.getTimeInMillis());
                    ps.setTimestamp(i++, ts, cal); // cal passed for timezone
                } else if (object instanceof String[]) {
                    Array array = mapper.sqlInfo.dialect.createArrayOf(
                            Types.VARCHAR, (Object[]) object, mapper.connection);
                    ps.setArray(i++, array);
                } else {
                    ps.setObject(i++, object);
                }
            }
            rs = ps.executeQuery();
            // rs.setFetchDirection(ResultSet.FETCH_UNKNOWN); fails in H2
        }

        // for debug
        private void log(String string) {
            log.trace("(" + instanceNumber + ") SQL: " + string);
        }

        // for debug
        private void logMap(Map<String, Serializable> map) throws SQLException {
            List<String> res = new LinkedList<String>();
            for (Entry<String, Serializable> en : map.entrySet()) {
                res.add(en.getKey() + "=" + loggedValue(en.getValue()));
            }
            log("  -> " + StringUtils.join(res, ", "));
        }

        public void close() {
            if (rs != null) {
                try {
                    rs.close();
                    closePreparedStatement(ps);
                } catch (SQLException e) {
                    log.error("Error closing statement: " + e.getMessage(), e);
                } finally {
                    pos = -1;
                    rs = null;
                    ps = null;
                    q = null;
                }
            }
        }

        @Override
        protected void finalize() {
            close();
            log.warn("Closing an IterableQueryResult for you. Please close them yourself.");
        }

        public long size() {
            if (size != -1) {
                return size;
            }
            try {
                // save cursor pos
                int old = rs.isBeforeFirst() ? -1 : rs.getRow();
                // find size
                rs.last();
                size = rs.getRow();
                // set back cursor
                if (old == -1) {
                    rs.beforeFirst();
                } else {
                    rs.absolute(old);
                }
                return size;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        public long pos() {
            return pos;
        }

        public void skipTo(long pos) {
            if (rs == null || pos < 0) {
                this.pos = -1;
                return;
            }
            try {
                boolean available = rs.absolute((int) pos + 1);
                if (available) {
                    next = fetchCurrent();
                    eof = false;
                    this.pos = pos;
                } else {
                    // after last row
                    next = null;
                    eof = true;
                    this.pos = -1; // XXX
                }
            } catch (SQLException e) {
                log.error("Error skipping to: " + pos + ": " + e.getMessage(),
                        e);
            }
        }

        public Iterator<Map<String, Serializable>> iterator() {
            return this;
        }

        protected Map<String, Serializable> fetchNext()
                throws StorageException, SQLException {
            if (rs == null) {
                return null;
            }
            if (!rs.next()) {
                if (isLogEnabled()) {
                    log("  -> END");
                }
                return null;
            }
            return fetchCurrent();
        }

        protected Map<String, Serializable> fetchCurrent() throws SQLException {
            Map<String, Serializable> map = q.selectInfo.mapMaker.makeMap(rs);
            if (isLogEnabled()) {
                logMap(map);
            }
            return map;
        }

        public boolean hasNext() {
            if (next != null) {
                return true;
            }
            if (eof) {
                return false;
            }
            try {
                next = fetchNext();
            } catch (Exception e) {
                log.error("Error fetching next: " + e.getMessage(), e);
            }
            eof = next == null;
            return !eof;
        }

        public Map<String, Serializable> next() {
            if (!hasNext()) {
                pos = -1;
                throw new NoSuchElementException();
            }
            Map<String, Serializable> n = next;
            next = null;
            pos++;
            return n;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * ----- read acls methods -------------------------
     */
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

    /**
     * ----- called by {@link TransactionalSession} -----
     */

    protected void start(Xid xid, int flags) throws XAException {
        try {
            xaresource.start(xid, flags);
        } catch (XAException e) {
            checkConnectionReset(e);
            log.error("XA error on start: " + e.getMessage());
            throw e;
        }
    }

    protected void end(Xid xid, int flags) throws XAException {
        try {
            xaresource.end(xid, flags);
        } catch (XAException e) {
            log.error("XA error on end: " + e.getMessage());
            throw e;
        }
    }

    protected int prepare(Xid xid) throws XAException {
        try {
            return xaresource.prepare(xid);
        } catch (XAException e) {
            log.error("XA error on prepare: " + e.getMessage());
            throw e;
        }
    }

    protected void commit(Xid xid, boolean onePhase) throws XAException {
        try {
            xaresource.commit(xid, onePhase);
        } catch (XAException e) {
            log.error("XA error on commit: " + e.getMessage());
            throw e;
        }
    }

    protected void rollback(Xid xid) throws XAException {
        try {
            xaresource.rollback(xid);
        } catch (XAException e) {
            log.error("XA error on rollback: " + e.getMessage());
            throw e;
        }
    }

    protected void forget(Xid xid) throws XAException {
        xaresource.forget(xid);
    }

    protected Xid[] recover(int flag) throws XAException {
        return xaresource.recover(flag);
    }

    protected boolean setTransactionTimeout(int seconds) throws XAException {
        return xaresource.setTransactionTimeout(seconds);
    }

    protected int getTransactionTimeout() throws XAException {
        return xaresource.getTransactionTimeout();
    }

}
