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
import java.util.Arrays;
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
import org.nuxeo.ecm.core.storage.sql.db.Column;

/**
 * A {@link Mapper} maps objects to and from the database. It is specific to a
 * given database connection, as it computes statements.
 * <p>
 * The {@link Mapper} does the mapping according to the policy defined by a
 * {@link Model}, and generates SQL statements recoreded in the
 * {@link SQLInfo}.
 *
 * @author Florent Guillaume
 */
public class Mapper implements XAResource {

    private static final Log log = LogFactory.getLog(Mapper.class);

    /** The SQL information. */
    private final SQLInfo sqlInfo;

    /** The model used to do the mapping. */
    private final Model model;

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
        } catch (SQLException e) {
            // nothing much we can do...
            e.printStackTrace();
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
    private String paramsToString(SingleRow row, List<Column> columns) {
        List<String> res = new LinkedList<String>();
        for (Column column : columns) {
            String key = column.getKey();
            Serializable v;
            if (key.equals(model.MAIN_KEY)) {
                v = row.getId();
            } else {
                v = row.get(key);
            }
            res.add(key + "=" + String.valueOf(v));
        }
        return StringUtils.join(res, ", ");
    }

    // for debug
    private String resultsToString(ResultSet rs, List<Column> columns)
            throws SQLException {
        List<String> res = new LinkedList<String>();
        int i = 0;
        for (Column column : columns) {
            i++;
            Serializable v = column.getFromResultSet(rs, i);
            res.add(column.getKey() + "=" + String.valueOf(v));
        }
        return StringUtils.join(res, ", ");
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
                e.printStackTrace();
            }
        }
    }

    /**
     * Inserts a new {@link SingleRow} in the storage. Depending on the type,
     * the id may be generated by the database (in which case it must not be
     * provided in the {@link SingleRow}), or already assigned (which is the
     * case for non-main tables).
     *
     * @param row the row
     * @return the id (generated or not)
     */
    public Serializable insertSingleRow(SingleRow row) throws StorageException {
        String tableName = row.getTableName();
        PreparedStatement ps = null;
        try {
            // insert the row
            // XXX statement should be already prepared
            String sql = sqlInfo.getInsertSql(tableName);
            List<Column> columns = sqlInfo.getInsertColumns(tableName);
            try {
                if (log.isDebugEnabled()) {
                    log.debug("SQL: " + sql);
                    log.debug("SQL:   " + paramsToString(row, columns));
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
                    if (v == null) {
                        ps.setNull(i, column.getSqlType());
                    } else {
                        ps.setObject(i, v);
                    }
                }
                ps.execute();
            } catch (SQLException e) {
                throw newStorageException(e, "Could not insert", sql);
            }

            // post insert fetch idrow
            // TODO PG 8.2 has INSERT ... RETURNING ... which can avoid this
            // separate query
            String isql = sqlInfo.getIdentityFetchSql(tableName);
            if (isql != null) {
                Column icolumn = sqlInfo.getIdentityFetchColumn(tableName);
                try {
                    log.debug("SQL: " + isql);
                    ps = connection.prepareStatement(isql);
                    ResultSet rs;
                    try {
                        rs = ps.executeQuery();
                    } catch (SQLException e) {
                        throw newStorageException(e, "Could not select", isql);
                    }
                    rs.next();
                    Serializable iv = icolumn.getFromResultSet(rs, 1);
                    row.setId(iv);
                    if (log.isDebugEnabled()) {
                        log.debug("SQL:   -> " + icolumn.getKey() + '=' + iv);
                    }
                } catch (SQLException e) {
                    throw newStorageException(e, "Could not fetch", isql);
                }
            }
        } finally {
            if (ps != null) {
                try {
                    ps.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
        return row.getId();
    }

    /**
     * Inserts a new {@link CollectionRows} in the storage.
     *
     * @param fragment the fragment
     */
    public void insertCollectionRows(CollectionRows fragment)
            throws StorageException {
        String tableName = fragment.getTableName();
        PreparedStatement ps = null;
        try {
            String sql = sqlInfo.getCollectionInsertSql(tableName);
            List<Column> columns = sqlInfo.getCollectionInsertColumns(tableName);
            try {
                Serializable id = fragment.getId();
                if (log.isDebugEnabled()) {
                    log.debug("SQL: " + sql);
                    log.debug("SQL:   " + model.MAIN_KEY + '=' + id + ", " +
                            model.COLL_TABLE_VALUE_KEY + "=" +
                            Arrays.asList(fragment.get()));
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
                        if (v == null) {
                            ps.setNull(i, column.getSqlType());
                        } else {
                            ps.setObject(i, v);
                        }
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
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Gets a {@link SingleRow} from the database, given its table name and id.
     * If the row doesn't exist, {@code null} is returned.
     *
     * @param tableName the type name
     * @param id the id
     * @param context the persistence context to which the read row is tied
     * @return the row, or {@code null}
     */
    public SingleRow readSingleRow(String tableName, Serializable id,
            PersistenceContextByTable context) throws StorageException {
        String sql = sqlInfo.getSelectByIdSql(tableName);
        try {
            // XXX statement should be already prepared
            if (log.isDebugEnabled()) {
                log.debug("SQL: " + sql);
                log.debug("SQL:   " + model.MAIN_KEY + '=' + id);
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                // List<String> keys = mapping.getPrimaryKeys(tableName));
                ps.setObject(1, id); // assumes only one primary column
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    // no match, row doesn't exist
                    return null;
                }
                // construct the row
                Map<String, Serializable> map = new HashMap<String, Serializable>();
                int i = 0;
                List<Column> columns = sqlInfo.getSelectByIdColumns(tableName);
                for (Column column : columns) {
                    i++;
                    map.put(column.getKey(), column.getFromResultSet(rs, i));
                }
                SingleRow row = new SingleRow(tableName, id, context, false,
                        map);
                if (log.isDebugEnabled()) {
                    log.debug("SQL:   -> " + resultsToString(rs, columns));
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
     * Reads the hierarchy {@link SingleRow} for a child, given its parent id
     * and the child name.
     *
     * @param parentId
     * @param childName
     * @param context the persistence context to which the read row is tied
     * @return the child hierarchy row, or {@code null}
     */
    public SingleRow readChildHierRow(Serializable parentId, String childName,
            PersistenceContextByTable context) throws StorageException {
        String sql = sqlInfo.getSelectByChildNameSql();
        try {
            // XXX statement should be already prepared
            if (log.isDebugEnabled()) {
                log.debug("SQL: " + sql);
                log.debug("SQL:   " + model.HIER_PARENT_KEY + '=' + parentId +
                        ", " + model.HIER_CHILD_NAME_KEY + '=' + childName);
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                // compute where part
                int i = 0;
                for (Column column : sqlInfo.getSelectByChildNameWhereColumns()) {
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
                    ps.setObject(i, v);
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
                List<Column> columns = sqlInfo.getSelectByChildNameWhatColumns();
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
                SingleRow row = new SingleRow(model.HIER_TABLE_NAME, id,
                        context, false, map);
                if (log.isDebugEnabled()) {
                    log.debug("SQL:   -> " + resultsToString(rs, columns));
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
     * Gets a {@link CollectionRows} from the database, given its table name and
     * id. If now rows are found, a fragment for an empty collection is
     * returned.
     *
     * @param tableName the type name
     * @param id the id
     * @param context the persistence context to which the read collection is
     *            tied
     * @return the fragment
     */
    public CollectionRows readCollectionRows(String tableName, Serializable id,
            PersistenceContextByTable context) throws StorageException {
        String sql = sqlInfo.getSelectCollectionByIdSql(tableName);
        try {
            // XXX statement should be already prepared
            if (log.isDebugEnabled()) {
                log.debug("SQL: " + sql);
                log.debug("SQL:   " + model.MAIN_KEY + '=' + id);
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
                // XXXX deal with different types
                String[] array = new String[list.size()];
                CollectionRows fragment = new CollectionRows(tableName, id,
                        context, false, list.toArray(array));
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
    public void updateSingleRow(SingleRow row) throws StorageException {
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
                    log.debug("SQL: " + sql);
                    log.debug("SQL:   " + paramsToString(row, columns));
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
                    if (v == null) {
                        ps.setNull(i, column.getSqlType());
                    } else {
                        ps.setObject(i, v);
                    }
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
     * Updates a {@link CollectionRows} in the database.
     * <p>
     * Does a simple delete + insert for now.
     *
     * @param fragment the fragment
     * @throws StorageException
     */
    public void updateCollectionRows(CollectionRows fragment)
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
                log.debug("SQL: " + sql);
                log.debug("SQL:   " + model.MAIN_KEY + '=' + id);
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
     * ----- javax.transaction.xa.XAResource -----
     */

    public boolean isSameRM(XAResource xar) throws XAException {
        return xaresource.isSameRM(xar);
    }

    public void start(Xid xid, int flags) throws XAException {
        xaresource.start(xid, flags);
    }

    public int prepare(Xid xid) throws XAException {
        return xaresource.prepare(xid);
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        xaresource.commit(xid, onePhase);
    }

    public void end(Xid xid, int flags) throws XAException {
        xaresource.end(xid, flags);
    }

    public void rollback(Xid xid) throws XAException {
        xaresource.rollback(xid);
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

}
