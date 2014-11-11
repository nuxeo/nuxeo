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
import org.nuxeo.ecm.core.query.QueryFilter;
import org.nuxeo.ecm.core.query.sql.model.SQLQuery;
import org.nuxeo.ecm.core.storage.PartialList;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.CollectionFragment.CollectionFragmentIterator;
import org.nuxeo.ecm.core.storage.sql.Fragment.State;
import org.nuxeo.ecm.core.storage.sql.SQLInfo.SQLInfoSelect;
import org.nuxeo.ecm.core.storage.sql.db.Column;
import org.nuxeo.ecm.core.storage.sql.db.Dialect.ConditionalStatement;
import org.nuxeo.ecm.core.storage.sql.db.Table;
import org.nuxeo.ecm.core.storage.sql.db.Update;

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
    private final long instanceNumber = instanceCounter.incrementAndGet();

    private static final int DEBUG_MAX_ARRAY = 10;

    private static final int DEBUG_MAX_STRING = 100;

    /**
     * Creates a new Mapper.
     *
     * @param model the model
     * @param sqlInfo the sql info
     * @param xadatasource the XA datasource to use to get connections
     */
    public Mapper(Model model, SQLInfo sqlInfo, XADataSource xadatasource)
            throws StorageException {
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
            close();
            throw new StorageException(e);
        }
    }

    protected Model getModel() {
        return model;
    }

    // for debug
    private static boolean isLogEnabled() {
        return log.isTraceEnabled();
    }

    // for debug
    private void logCount(int count) {
        if (count > 0 && isLogEnabled()) {
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
            if (key.equals(model.MAIN_KEY)) {
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
                v = v.substring(0, DEBUG_MAX_STRING) + "...";
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
                    "TIMESTAMP '%04d-%02d-%02dT%02d:%02d:%02d%c%02d:%02d'",
                    cal.get(Calendar.YEAR), //
                    cal.get(Calendar.MONTH) + 1, //
                    cal.get(Calendar.DAY_OF_MONTH), //
                    cal.get(Calendar.HOUR_OF_DAY), //
                    cal.get(Calendar.MINUTE), //
                    cal.get(Calendar.SECOND), //
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
                        b.append("...");
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

    /**
     * Creates the necessary structures in the database.
     */
    protected void createDatabase() throws StorageException {
        try {
            Collection<ConditionalStatement> statements = sqlInfo.getConditionalStatements();
            executeConditionalStatements(statements, true);
            createTables();
            executeConditionalStatements(statements, false);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    protected void createTables() throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        Set<String> tableNames = findTableNames(metadata);
        Statement st = connection.createStatement();

        for (Table table : sqlInfo.getDatabase().getTables()) {
            String tableName = table.getName();
            if (!tableNames.contains(tableName)
                    && !tableNames.contains(tableName.toUpperCase())) {
                /*
                 * Create missing table.
                 */
                String sql = table.getCreateSql();
                log(sql);
                st.execute(sql);
                for (String s : table.getPostCreateSqls()) {
                    log(s);
                    st.execute(s);
                }
            }

            /*
             * Get existing columns.
             */
            ResultSet rs = metadata.getColumns(null, null, tableName, "%");
            Map<String, Integer> columnTypes = new HashMap<String, Integer>();
            while (rs.next()) {
                String schema = rs.getString("TABLE_SCHEM").toUpperCase();
                if ("INFORMATION_SCHEMA".equals(schema)) {
                    // H2 returns some system tables (locks)
                    continue;
                }
                String columnName = rs.getString("COLUMN_NAME").toUpperCase();
                int sqlType = rs.getInt("DATA_TYPE");
                columnTypes.put(columnName, Integer.valueOf(sqlType));
            }

            /*
             * Update types and create missing columns.
             */
            for (Column column : table.getColumns()) {
                Integer type = columnTypes.remove(column.getPhysicalName().toUpperCase());
                if (type == null) {
                    log.warn("Adding missing column in database: "
                            + column.getFullQuotedName());
                    String sql = table.getAddColumnSql(column);
                    log(sql);
                    st.execute(sql);
                } else {
                    int sqlType = type.intValue();
                    int t = column.getSqlType();
                    if (t != sqlType) {
                        // type in database is different...
                        // record the actual type
                        column.setSqlType(sqlType);
                        // some databases are known to change requested types
                        if (t == Types.BIT && //
                                (sqlType == Types.SMALLINT // Derby
                                || sqlType == Types.BOOLEAN // H2
                                )) {
                            continue;
                        }
                        if (t == Types.CLOB && //
                                (sqlType == Types.LONGVARCHAR // MySQL
                                || sqlType == Types.VARCHAR // PostgreSQL
                                )) {
                            continue;
                        }
                        // otherwise log this
                        log.error(String.format(
                                "SQL type mismatch for %s: expected %s, database has %s",
                                column.getFullQuotedName(), Integer.valueOf(t),
                                type));
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

    protected static Set<String> findTableNames(DatabaseMetaData metadata)
            throws SQLException {
        Set<String> tableNames = new HashSet<String>();
        ResultSet rs = metadata.getTables(null, null, "%",
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
            throw new StorageException(e);
        }
    }

    /**
     * Inserts the invalidation rows for the other cluster nodes.
     */
    public void insertClusterInvalidations(Invalidations invalidations)
            throws StorageException {
        String sql = sqlInfo.getClusterInsertInvalidationsSql();
        List<Column> columns = sqlInfo.getClusterInsertInvalidationsColumns();
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
                    if (sqlInfo.dialect.supportsArrays()) {
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
            throw new StorageException("Could not invalidate", e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
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
            throw new AssertionError();
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
        String sql = sqlInfo.getClusterGetInvalidationsSql();
        List<Column> columns = sqlInfo.getClusterGetInvalidtionsColumns();
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
                if (sqlInfo.dialect.supportsArrays()) {
                    fragments = (String[]) frags;
                } else {
                    fragments = ((String) frags).split(" ");
                }
                invalidations.add(id, fragments, kind);
            }
            if (isLogEnabled()) {
                logCount(n);
            }
            return invalidations;
        } catch (SQLException e) {
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
                    log("  -> " + model.MAIN_KEY + '=' + id);
                }
                // check that we didn't get several rows
                if (rs.next()) {
                    throw new StorageException("Row query for " + repositoryId
                            + " returned several rows: " + sql);
                }
                return id;
            } finally {
                ps.close();
            }
        } catch (SQLException e) {
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
                    if (key.equals(model.MAIN_KEY)) {
                        v = id;
                    } else if (key.equals(model.REPOINFO_REPONAME_KEY)) {
                        v = repositoryId;
                    } else {
                        throw new AssertionError(key);
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
                ps.close();
            }
        } catch (SQLException e) {
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
                    if (key.equals(model.MAIN_KEY)) {
                        v = row.getId();
                    } else {
                        v = row.get(key);
                    }
                    column.setToPreparedStatement(ps, i, v);
                }
                ps.execute();
            } catch (SQLException e) {
                throw new StorageException("Could not insert: " + sql, e);
            }

            // TODO only do this if the id was not inserted!
            switch (model.idGenPolicy) {
            case APP_UUID:
                // nothing to do, id is already known
                break;
            case DB_IDENTITY:
                // post insert fetch idrow
                // TODO PG 8.2 has INSERT ... RETURNING ... which can avoid this
                // separate query
                String isql = sqlInfo.getIdentityFetchSql(tableName);
                if (isql != null) {
                    Column icolumn = sqlInfo.getIdentityFetchColumn(tableName);
                    try {
                        // logDebug(isql);
                        ps.close();
                        ps = connection.prepareStatement(isql);
                        ResultSet rs;
                        try {
                            rs = ps.executeQuery();
                        } catch (SQLException e) {
                            throw new StorageException("Could not select: "
                                    + isql, e);
                        }
                        rs.next();
                        Serializable iv = icolumn.getFromResultSet(rs, 1);
                        row.setId(iv);
                        if (isLogEnabled()) {
                            log("  -> " + icolumn.getKey() + '=' + iv);
                        }
                    } catch (SQLException e) {
                        throw new StorageException("Could not fetch: " + isql,
                                e);
                    }
                }
                break;
            }
        } finally {
            if (ps != null) {
                try {
                    ps.close();
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
                throw new StorageException("Could not insert: " + sql, e);
            }

        } finally {
            if (ps != null) {
                try {
                    ps.close();
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
            Serializable id = map.remove(model.MAIN_KEY);
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
                } else if (state == State.ABSENT
                        || state == State.INVALIDATED_MODIFIED
                        || state == State.INVALIDATED_DELETED) {
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
            if (select.whereColumns.size() == 1
                    && select.whereColumns.get(0).getKey() == model.MAIN_KEY
                    && joinMap == null) {
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
                    throw new AssertionError(key);
                }
                if (v == null) {
                    throw new StorageException("Null value for key: " + key);
                }
                column.setToPreparedStatement(ps, i++, v);
                if (debugValues != null) {
                    debugValues.add(v);
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
            throw new StorageException("Could not select: " + select.sql, e);
        } finally {
            if (ps != null) {
                try {
                    ps.close();
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
        criteriaMap.put(model.MAIN_KEY, id);
        List<Map<String, Serializable>> maps = getSelectMaps(select,
                criteriaMap, null, true, context);
        return maps == null ? null : maps.get(0);
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
                    if (key.equals(model.HIER_PARENT_KEY)) {
                        v = parentId;
                    } else if (key.equals(model.HIER_CHILD_NAME_KEY)) {
                        v = childName;
                    } else {
                        throw new AssertionError("Invalid hier column: " + key);
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
                    if (key.equals(model.MAIN_KEY)) {
                        id = value;
                    } else {
                        map.put(key, value);
                    }
                }
                map.put(model.HIER_PARENT_KEY, parentId);
                map.put(model.HIER_CHILD_NAME_KEY, childName);
                map.put(model.HIER_CHILD_ISPROPERTY_KEY,
                        Boolean.valueOf(complexProp));
                SimpleFragment row = new SimpleFragment(id, State.PRISTINE,
                        context, map);
                if (isLogEnabled()) {
                    logResultSet(rs, columns);
                }
                // check that we didn't get several rows
                if (rs.next()) {
                    throw new StorageException("Row query for " + parentId
                            + " child " + childName
                            + " returned several rows: " + sql);
                }
                return row;
            } finally {
                ps.close();
            }
        } catch (SQLException e) {
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
        criteriaMap.put(model.HIER_PARENT_KEY, parentId);
        criteriaMap.put(model.HIER_CHILD_ISPROPERTY_KEY,
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
                Serializable[] array = model.newCollectionArray(id, rs,
                        columns, context);
                if (isLogEnabled()) {
                    log("  -> " + Arrays.asList(array));
                }
                return array;
            } finally {
                ps.close();
            }
        } catch (SQLException e) {
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
                    if (key.equals(model.MAIN_KEY)) {
                        v = row.getId();
                    } else {
                        v = row.get(key);
                    }
                    column.setToPreparedStatement(ps, i, v);
                }
                int count = ps.executeUpdate();
                logCount(count);
            } finally {
                ps.close();
            }
        } catch (SQLException e) {
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
                ps.close();
            }
        } catch (SQLException e) {
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
        deleteFragment(fragment);
        insertCollectionRows(fragment);
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
            ps.close();
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
     * @parem destName the new name
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
                if (tableName.equals(model.ACL_TABLE_NAME)) {
                    continue;
                }
                Set<Serializable> ids = entry.getValue();
                boolean overwrite = overwriteId != null
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
            switch (model.idGenPolicy) {
            case APP_UUID:
                newId = model.generateNewId();
                break;
            case DB_IDENTITY:
                newId = null;
                break;
            }

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
                    throw new AssertionError(column);
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

            if (newId == null) {
                // post insert fetch idrow
                // TODO PG 8.2 has INSERT ... RETURNING ... which can avoid this
                // separate query
                String isql = sqlInfo.getIdentityFetchSql(model.hierTableName);
                Column icolumn = sqlInfo.getIdentityFetchColumn(model.hierTableName);
                ps.close();
                ps = connection.prepareStatement(isql);
                ResultSet rs = ps.executeQuery();
                rs.next();
                newId = icolumn.getFromResultSet(rs, 1);
                if (isLogEnabled()) {
                    log("  -> " + icolumn.getKey() + '=' + newId);
                }
            }

            idMap.put(id, newId);
        } finally {
            ps.close();
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
                    if (key.equals(model.MAIN_KEY)) {
                        childId = value;
                    } else if (key.equals(model.MAIN_PRIMARY_TYPE_KEY)) {
                        childType = value;
                    }
                }
                childrenIds.add(new Serializable[] { childId, childType });
                if (debugValues != null) {
                    debugValues.add(String.valueOf(childId) + "/" + childType);
                }
            }
            if (debugValues != null) {
                log("  -> " + debugValues);
            }
            return childrenIds;
        } finally {
            ps.close();
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
            copyPs.close();
            deletePs.close();
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
        criteriaMap.put(model.VERSION_VERSIONABLE_KEY, versionableId);
        criteriaMap.put(model.VERSION_LABEL_KEY, label);
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
        criteriaMap.put(model.VERSION_VERSIONABLE_KEY, versionableId);
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
        criteriaMap.put(model.VERSION_VERSIONABLE_KEY, versionableId);
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
        criteriaMap.put(byTarget ? model.PROXY_TARGET_KEY
                : model.PROXY_VERSIONABLE_KEY, searchId);
        if (parentId == null) {
            SQLInfoSelect select = byTarget ? sqlInfo.selectProxiesByTarget
                    : sqlInfo.selectProxiesByVersionable;
            return getSelectRows(select, criteriaMap, context);
        } else {
            SQLInfoSelect select = byTarget ? sqlInfo.selectProxiesByTargetAndParent
                    : sqlInfo.selectProxiesByVersionableAndParent;
            Map<String, Serializable> joinMap = new HashMap<String, Serializable>();
            joinMap.put(model.HIER_PARENT_KEY, parentId);
            return getSelectRows(select, criteriaMap, joinMap, context);
        }
    }

    /**
     * Makes a NXQL query to the database.
     *
     * @param query the query as a parsed tree
     * @param queryFilter the query filter
     * @param countTotal if {@code true}, count the total size without
     *            limit/offset
     * @param session the current session (to resolve paths)
     * @return the list of matching document ids
     * @throws StorageException
     * @throws SQLException
     */
    public PartialList<Serializable> query(SQLQuery query,
            QueryFilter queryFilter, boolean countTotal, Session session)
            throws StorageException, SQLException {
        QueryMaker queryMaker = new QueryMaker(sqlInfo, model, session, query,
                queryFilter);
        queryMaker.makeQuery();

        if (queryMaker.selectInfo == null) {
            log("Query cannot return anything due to conflicting clauses");
            return new PartialList<Serializable>(
                    Collections.<Serializable> emptyList(), 0);
        }

        if (isLogEnabled()) {
            logSQL(queryMaker.selectInfo.sql, queryMaker.selectParams);
        }
        PreparedStatement ps = connection.prepareStatement(
                queryMaker.selectInfo.sql, ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY);
        try {
            int i = 1;
            for (Object object : queryMaker.selectParams) {
                if (object instanceof Calendar) {
                    Calendar cal = (Calendar) object;
                    Timestamp ts = new Timestamp(cal.getTimeInMillis());
                    ps.setTimestamp(i++, ts, cal); // cal passed for timezone
                } else if (object instanceof String[]) {
                    Array array = sqlInfo.dialect.createArrayOf(Types.VARCHAR,
                            (Object[]) object);
                    ps.setArray(i++, array);
                } else {
                    ps.setObject(i++, object);
                }
            }
            ResultSet rs = ps.executeQuery();

            // limit/offset
            long totalSize = -1;
            long limit = query.getLimit();
            long offset = query.getOffset();
            boolean available;
            if (limit == 0 || offset == 0) {
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

            Column column = queryMaker.selectInfo.whatColumns.get(0);
            List<Serializable> ids = new LinkedList<Serializable>();
            int rowNum = 0;
            while (available && limit != 0) {
                Serializable id = column.getFromResultSet(rs, 1);
                ids.add(id);
                rowNum = rs.getRow();
                available = rs.next();
                limit--;
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
                    end = "...";
                }
                log("  -> " + debugIds + end);
            }

            // total size
            if (countTotal && totalSize == -1) {
                if (!available && rowNum != 0) {
                    // last row read was the actual last
                    totalSize = rowNum;
                } else {
                    // available if limit reached with some left
                    // rowNum == 0 if skipped too far
                    rs.last();
                    totalSize = rs.getRow();
                }
            }

            return new PartialList<Serializable>(ids, totalSize);
        } finally {
            ps.close();
        }
    }

    /**
     * ----- called by {@link TransactionalSession} -----
     */

    protected void start(Xid xid, int flags) throws XAException {
        try {
            xaresource.start(xid, flags);
        } catch (XAException e) {
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
