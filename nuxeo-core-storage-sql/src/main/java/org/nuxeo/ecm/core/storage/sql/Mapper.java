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
 */

package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.exception.JDBCExceptionHelper;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Fragment.State;
import org.nuxeo.ecm.core.storage.sql.db.Column;

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

    /** The xa pooled connection. */
    private final XAConnection xaconnection;

    /** The actual connection. */
    private final Connection connection;

    private final XAResource xaresource;

    /**
     * Creates a new Mapper.
     *
     * @param model the model
     * @param sqlInfo the sql info
     * @param xaconnection the XA connection to use
     */
    public Mapper(Model model, SQLInfo sqlInfo, XAConnection xaconnection)
            throws StorageException {
        this.model = model;
        this.sqlInfo = sqlInfo;
        this.xaconnection = xaconnection;
        try {
            connection = xaconnection.getConnection();
            xaresource = xaconnection.getXAResource();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public void close() {
        try {
            connection.close();
            xaconnection.close();
        } catch (SQLException e) {
            // nothing much we can do...
            log.error("Cannot close connection", e);
        }
    }

    private StorageException newStorageException(SQLException e,
            String message, String sql) {
        return new StorageException(JDBCExceptionHelper.convert(
                sqlInfo.getSqlExceptionConverter(), e, message, sql));
    }

    protected Model getModel() {
        return model;
    }

    // for debug
    protected static void logResultSet(ResultSet rs, List<Column> columns)
            throws SQLException {
        List<String> res = new LinkedList<String>();
        int i = 0;
        for (Column column : columns) {
            i++;
            Serializable v = column.getFromResultSet(rs, i);
            res.add(column.getKey() + "=" + loggedValue(v));
        }
        log.debug("SQL:   -> " + StringUtils.join(res, ", "));
    }

    // for debug
    protected void logSQL(String sql, List<Column> columns, SimpleFragment row) {
        List<Serializable> values = new ArrayList<Serializable>(columns.size());
        for (Column column : columns) {
            String key = column.getKey();
            values.add(key.equals(model.MAIN_KEY) ? row.getId() : row.get(key));
        }
        logSQL(sql, values);
    }

    // for debug
    protected static void logSQL(String sql, List<Serializable> values) {
        for (Serializable v : values) {
            String value;
            value = loggedValue(v);
            sql = sql.replaceFirst("\\?", value);
        }
        log.debug("SQL: " + sql);
    }

    /**
     * Returns a loggable value using pseudo-SQL syntax.
     */
    @SuppressWarnings("boxing")
    protected static String loggedValue(Serializable value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof String) {
            return "'" + ((String) value).replace("'", "''") + "'";
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
        return value.toString();
    }

    // ---------- low-level JDBC methods ----------

    /**
     * Creates all the tables and sequences in the database.
     */
    // TODO make private
    protected void createDatabase() throws StorageException {
        log.debug("Creating database");
        Statement s;
        try {
            s = connection.createStatement();
        } catch (SQLException e) {
            throw newStorageException(e, "Could not create statement", "");
        }
        try {
            for (String sql : sqlInfo.getDatabaseCreateSql()) {
                try {
                    log.debug("SQL: (batch) " + sql);
                    s.addBatch(sql);
                } catch (SQLException e) {
                    throw newStorageException(e, "Could not add batch", sql);
                }
            }
            try {
                log.debug("SQL: (batch execution)");
                s.executeBatch();
            } catch (SQLException e) {
                throw newStorageException(e, "Could not execute", "batch");
            }
        } finally {
            try {
                s.close();
            } catch (SQLException e) {
                log.error("Cannot close connection", e);
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
            if (log.isDebugEnabled()) {
                logSQL(sql, Collections.singletonList(repositoryId));
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                ps.setObject(1, repositoryId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    if (log.isDebugEnabled()) {
                        log.debug("SQL:   -> (none)");
                    }
                    return null;
                }
                Column column = sqlInfo.getSelectRootIdWhatColumn();
                Serializable id = column.getFromResultSet(rs, 1);
                if (log.isDebugEnabled()) {
                    log.debug("SQL:   -> " + model.MAIN_KEY + '=' + id);
                }
                // check that we didn't get several rows
                if (rs.next()) {
                    throw new StorageException("Row query for " + repositoryId +
                            " returned several rows: " + sql);
                }
                return id;
            } finally {
                ps.close();
            }
        } catch (SQLException e) {
            throw newStorageException(e, "Could not select", sql);
        }
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
                if (log.isDebugEnabled()) {
                    debugValues = new ArrayList<Serializable>(2);
                }
                int i = 0;
                for (Column column : columns) {
                    i++;
                    String key = column.getKey();
                    Serializable v;
                    if (key.equals(model.MAIN_KEY)) {
                        v = id;
                    } else if (key.equals(model.REPOINFO_REPOID_KEY)) {
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
            throw newStorageException(e, "Could not insert", sql);
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
                if (log.isDebugEnabled()) {
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
                throw newStorageException(e, "Could not insert", sql);
            }

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
                        // log.debug("SQL: " + isql);
                        ps = connection.prepareStatement(isql);
                        ResultSet rs;
                        try {
                            rs = ps.executeQuery();
                        } catch (SQLException e) {
                            throw newStorageException(e, "Could not select",
                                    isql);
                        }
                        rs.next();
                        Serializable iv = icolumn.getFromResultSet(rs, 1);
                        row.setId(iv);
                        if (log.isDebugEnabled()) {
                            log.debug("SQL:   -> " + icolumn.getKey() + '=' +
                                    iv);
                        }
                    } catch (SQLException e) {
                        throw newStorageException(e, "Could not fetch", isql);
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
            String sql = sqlInfo.getCollectionInsertSql(tableName);
            List<Column> columns = sqlInfo.getCollectionInsertColumns(tableName);
            try {
                Serializable id = fragment.getId();
                List<Serializable> debugValues = null;
                if (log.isDebugEnabled()) {
                    debugValues = new ArrayList<Serializable>(3);
                }
                ps = connection.prepareStatement(sql);
                int pos = -1;
                for (Serializable value : fragment.get()) {
                    pos++;
                    int i = 0;
                    for (Column column : columns) {
                        i++;
                        String key = column.getKey();
                        Serializable v;
                        if (key.equals(model.MAIN_KEY)) {
                            v = id;
                        } else if (key.equals(model.COLL_TABLE_POS_KEY)) {
                            v = Long.valueOf(pos);
                        } else if (key.equals(model.COLL_TABLE_VALUE_KEY)) {
                            v = value;
                        } else {
                            throw new AssertionError(
                                    "Invalid collection column: " + key);
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
                }
            } catch (SQLException e) {
                throw newStorageException(e, "Could not insert", sql);
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
     * Gets a {@link SimpleFragment} from the database, given its table name and
     * id. If the row doesn't exist, an absent row or {@code null} is returned.
     *
     * @param tableName the type name
     * @param id the id
     * @param createAbsent {@code true} if an absent row may be created
     * @param context the persistence context to which the read row is tied
     * @return the row, or an absent row, or {@code null}
     */
    public SimpleFragment readSingleRow(String tableName, Serializable id,
            boolean createAbsent, PersistenceContextByTable context)
            throws StorageException {
        String sql = sqlInfo.getSelectByIdSql(tableName);
        try {
            // XXX statement should be already prepared
            if (log.isDebugEnabled()) {
                logSQL(sql, Collections.singletonList(id));
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                // List<String> keys = mapping.getPrimaryKeys(tableName));
                ps.setObject(1, id); // assumes only one primary column
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    // no match, row doesn't exist
                    if (createAbsent) {
                        if (log.isDebugEnabled()) {
                            log.debug("SQL:   -> (absent)");
                        }
                        SimpleFragment row = new SimpleFragment(tableName, id,
                                State.ABSENT, context, null);
                        return row;
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("SQL:   -> (none)");
                        }
                        return null;
                    }
                }
                // construct the row
                Map<String, Serializable> map = new HashMap<String, Serializable>();
                int i = 0;
                List<Column> columns = sqlInfo.getSelectByIdColumns(tableName);
                for (Column column : columns) {
                    i++;
                    map.put(column.getKey(), column.getFromResultSet(rs, i));
                }
                SimpleFragment row = new SimpleFragment(tableName, id,
                        State.PRISTINE, context, map);
                if (log.isDebugEnabled()) {
                    logResultSet(rs, columns);
                }
                // check that we didn't get several rows
                if (rs.next()) {
                    throw new StorageException("Row query for " + id +
                            " returned several rows: " + sql);
                }
                return row;
            } finally {
                ps.close();
            }
        } catch (SQLException e) {
            throw newStorageException(e, "Could not select", sql);
        }
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
            String childName, boolean complexProp,
            PersistenceContextByTable context) throws StorageException {
        String sql = sqlInfo.getSelectByChildNameSql(complexProp);
        try {
            // XXX statement should be already prepared
            List<Serializable> debugValues = null;
            if (log.isDebugEnabled()) {
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
                        throw new RuntimeException("Null value for key: " + key);
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
                Serializable id = null;
                Map<String, Serializable> map = new HashMap<String, Serializable>();
                i = 0;
                List<Column> columns = sqlInfo.getSelectByChildNameWhatColumns(complexProp);
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
                SimpleFragment row = new SimpleFragment(model.HIER_TABLE_NAME,
                        id, State.PRISTINE, context, map);
                if (log.isDebugEnabled()) {
                    logResultSet(rs, columns);
                }
                // check that we didn't get several rows
                if (rs.next()) {
                    throw new StorageException("Row query for " + parentId +
                            " child " + childName + " returned several rows: " +
                            sql);
                }
                return row;
            } finally {
                ps.close();
            }
        } catch (SQLException e) {
            throw newStorageException(e, "Could not select", sql);
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
    public Collection<SimpleFragment> readChildHierRows(Serializable parentId,
            boolean complexProp, PersistenceContextByTable context)
            throws StorageException {
        if (parentId == null) {
            throw new IllegalArgumentException("Illegal null parentId");
        }
        String sql = sqlInfo.getSelectChildrenSql(complexProp);
        try {
            // XXX statement should be already prepared
            if (log.isDebugEnabled()) {
                logSQL(sql, Collections.singletonList(parentId));
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                // compute where part
                int i = 0;
                for (Column column : sqlInfo.getSelectChildrenWhereColumns(complexProp)) {
                    i++;
                    String key = column.getKey();
                    if (!key.equals(model.HIER_PARENT_KEY)) {
                        throw new AssertionError("Invalid hier column: " + key);
                    }
                    column.setToPreparedStatement(ps, i, parentId);
                }
                ResultSet rs = ps.executeQuery();
                List<SimpleFragment> rows = new LinkedList<SimpleFragment>();
                while (rs.next()) {
                    // construct the row from the results
                    Serializable id = null;
                    Map<String, Serializable> map = new HashMap<String, Serializable>();
                    i = 0;
                    List<Column> columns = sqlInfo.getSelectChildrenWhatColumns(complexProp);
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
                    if (context.isDeleted(id)) {
                        // row has been deleted in the persistent context,
                        // ignore it
                        if (log.isDebugEnabled()) {
                            log.debug("SQL:   -> deleted id=" + id);
                        }
                        continue;
                    }
                    // TODO what if row is "absent" in context?
                    SimpleFragment row = (SimpleFragment) context.getIfPresent(id);
                    if (row == null) {
                        map.put(model.HIER_PARENT_KEY, parentId);
                        map.put(model.HIER_CHILD_ISPROPERTY_KEY,
                                Boolean.valueOf(complexProp));
                        row = new SimpleFragment(model.HIER_TABLE_NAME, id,
                                State.PRISTINE, context, map);
                        if (log.isDebugEnabled()) {
                            logResultSet(rs, columns);
                        }
                    } else {
                        // row is already known in the persistent context,
                        // use it
                        if (log.isDebugEnabled()) {
                            log.debug("SQL:   -> known id=" + id);
                        }
                    }
                    rows.add(row);
                }
                return rows;
            } finally {
                ps.close();
            }
        } catch (SQLException e) {
            throw newStorageException(e, "Could not select", sql);
        }
    }

    /**
     * Gets a {@link CollectionFragment} from the database, given its table name
     * and id. If now rows are found, a fragment for an empty collection is
     * returned.
     *
     * @param tableName the type name
     * @param id the id
     * @param context the persistence context to which the read collection is
     *            tied
     * @return the fragment
     */
    public CollectionFragment readCollectionRows(String tableName,
            Serializable id, PersistenceContextByTable context)
            throws StorageException {
        String sql = sqlInfo.getSelectCollectionByIdSql(tableName);
        try {
            // XXX statement should be already prepared
            if (log.isDebugEnabled()) {
                logSQL(sql, Collections.singletonList(id));
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                ArrayList<Serializable> list = new ArrayList<Serializable>();
                List<Column> columns = sqlInfo.getSelectByIdColumns(tableName);
                ps.setObject(1, id); // assumes only one primary column
                ResultSet rs = ps.executeQuery();
                // construct the list using each row
                while (rs.next()) {
                    int i = 0;
                    Serializable value = null;
                    for (Column column : columns) {
                        i++;
                        if (model.COLL_TABLE_VALUE_KEY.equals(column.getKey())) {
                            value = column.getFromResultSet(rs, i);
                        }
                    }
                    list.add(value);
                }
                if (log.isDebugEnabled()) {
                    log.debug("SQL:   -> " + list);
                }
                // XXX deal with different types
                String[] array = new String[list.size()];
                CollectionFragment fragment = new CollectionFragment(tableName,
                        id, State.PRISTINE, context, list.toArray(array));
                return fragment;
            } finally {
                ps.close();
            }
        } catch (SQLException e) {
            throw newStorageException(e, "Could not select", sql);
        }
    }

    /**
     * Updates a row in the database.
     *
     * @param row the row
     * @throws StorageException
     */
    public void updateSingleRow(SimpleFragment row) throws StorageException {
        String tableName = row.getTableName();
        // XXX more fined grained SQL updating only changed columns
        String sql = sqlInfo.getUpdateByIdSql(tableName);
        try {
            // update the row
            // XXX statement should be already prepared
            List<Column> columns = sqlInfo.getUpdateByIdColumns(tableName);
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                if (log.isDebugEnabled()) {
                    logSQL(sql, columns, row);
                }
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
                int count = ps.executeUpdate();
                if (log.isDebugEnabled()) {
                    log.debug("SQL:   -> " + count + " rows");
                }
                // XXX check number of changed rows
                // if 1 -> ok
                // if 0 -> must do an insert (for missing fragments)
            } finally {
                ps.close();
            }
        } catch (SQLException e) {
            throw newStorageException(e, "Could not select", sql);
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
        String tableName = fragment.getTableName();
        String sql = sqlInfo.getDeleteSql(tableName);
        Serializable id = fragment.getId();
        try {
            if (log.isDebugEnabled()) {
                logSQL(sql, Collections.singletonList(id));
            }
            // XXX statement should be already prepared
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                ps.setObject(1, id); // FIXME assumes only one primary column
                int count = ps.executeUpdate();
                if (log.isDebugEnabled()) {
                    log.debug("SQL:   -> " + count + " rows");
                }
            } finally {
                ps.close();
            }
        } catch (SQLException e) {
            throw newStorageException(e, "Could not delete", sql);
        }
    }

    /*
     * ----- part of javax.transaction.xa.XAResource -----
     */

    protected void start(Xid xid, int flags) throws XAException {
        xaresource.start(xid, flags);
    }

    protected int prepare(Xid xid) throws XAException {
        return xaresource.prepare(xid);
    }

    protected void commit(Xid xid, boolean onePhase) throws XAException {
        xaresource.commit(xid, onePhase);
    }

    protected void end(Xid xid, int flags) throws XAException {
        xaresource.end(xid, flags);
    }

    protected void rollback(Xid xid) throws XAException {
        xaresource.rollback(xid);
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
