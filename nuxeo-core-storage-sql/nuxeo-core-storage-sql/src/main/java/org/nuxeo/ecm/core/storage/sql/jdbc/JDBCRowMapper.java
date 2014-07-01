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
 */
package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.io.Serializable;
import java.sql.Array;
import java.sql.BatchUpdateException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import java.util.Map.Entry;
import java.util.Set;

import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Invalidations;
import org.nuxeo.ecm.core.storage.sql.Invalidations.InvalidationsPair;
import org.nuxeo.ecm.core.storage.sql.InvalidationsQueue;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.PropertyType;
import org.nuxeo.ecm.core.storage.sql.Row;
import org.nuxeo.ecm.core.storage.sql.RowId;
import org.nuxeo.ecm.core.storage.sql.RowMapper;
import org.nuxeo.ecm.core.storage.sql.SelectionType;
import org.nuxeo.ecm.core.storage.sql.SimpleFragment;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo.SQLInfoSelect;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo.SQLInfoSelection;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Update;

/**
 * A {@link JDBCRowMapper} maps {@link Row}s to and from a JDBC database.
 */
public class JDBCRowMapper extends JDBCConnection implements RowMapper {

    public static final int UPDATE_BATCH_SIZE = 100; // also insert/delete

    public static final int DEBUG_MAX_TREE = 50;

    /**
     * Cluster node handler, or {@code null} if this {@link Mapper} is not the
     * cluster node mapper.
     */
    private final ClusterNodeHandler clusterNodeHandler;

    /**
     * Queue of invalidations received for this cluster node.
     */
    private final InvalidationsQueue queue;

    public JDBCRowMapper(Model model, SQLInfo sqlInfo,
            XADataSource xadatasource, ClusterNodeHandler clusterNodeHandler,
            JDBCConnectionPropagator connectionPropagator, boolean noSharing)
            throws StorageException {
        super(model, sqlInfo, xadatasource, connectionPropagator, noSharing);
        this.clusterNodeHandler = clusterNodeHandler;
        if (clusterNodeHandler != null) {
            queue = new InvalidationsQueue("cluster-" + this);
            clusterNodeHandler.addQueue(queue);
        } else {
            queue = null;
        }
    }

    @Override
    public void close() {
        super.close();
        if (clusterNodeHandler != null) {
            clusterNodeHandler.removeQueue(queue);
        }
    }

    @Override
    public InvalidationsPair receiveInvalidations() throws StorageException {
        Invalidations invalidations = null;
        if (clusterNodeHandler != null && connection != null) {
            receiveClusterInvalidations();
            invalidations = queue.getInvalidations();
        }
        return invalidations == null ? null : new InvalidationsPair(
                invalidations, null);
    }

    protected void receiveClusterInvalidations() throws StorageException {
        Invalidations invalidations = clusterNodeHandler.receiveClusterInvalidations();
        // send received invalidations to all mappers
        if (invalidations != null && !invalidations.isEmpty()) {
            clusterNodeHandler.propagateInvalidations(invalidations, null);
        }
    }

    @Override
    public void sendInvalidations(Invalidations invalidations)
            throws StorageException {
        if (clusterNodeHandler != null) {
            clusterNodeHandler.sendClusterInvalidations(invalidations);
        }
    }

    @Override
    public void clearCache() {
        // no cache
    }

    @Override
    public long getCacheSize() {
        return 0;
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        try {
            xaresource.rollback(xid);
        } catch (XAException e) {
            logger.error("XA error on rollback: " + e);
            throw e;
        }
    }

    protected CollectionIO getCollectionIO(String tableName) {
        return tableName.equals(model.ACL_TABLE_NAME) ? ACLCollectionIO.INSTANCE
                : ScalarCollectionIO.INSTANCE;
    }

    @Override
    public Serializable generateNewId() throws StorageException {
        try {
            return generateNewIdInternal();
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    // same but throwing SQLException
    protected Serializable generateNewIdInternal() throws SQLException {
        return dialect.getGeneratedId(connection);
    }

    /*
     * ----- RowIO -----
     */

    @Override
    public List<? extends RowId> read(Collection<RowId> rowIds,
            boolean cacheOnly) throws StorageException {
        List<RowId> res = new ArrayList<RowId>(rowIds.size());
        if (cacheOnly) {
            // return no data
            for (RowId rowId : rowIds) {
                res.add(new RowId(rowId));
            }
            return res;
        }
        // reorganize by table
        Map<String, Set<Serializable>> tableIds = new HashMap<String, Set<Serializable>>();
        for (RowId rowId : rowIds) {
            Set<Serializable> ids = tableIds.get(rowId.tableName);
            if (ids == null) {
                tableIds.put(rowId.tableName, ids = new HashSet<Serializable>());
            }
            ids.add(rowId.id);
        }
        // read on each table
        for (Entry<String, Set<Serializable>> en : tableIds.entrySet()) {
            String tableName = en.getKey();
            Set<Serializable> ids = new HashSet<Serializable>(en.getValue());
            int size = ids.size();
            int chunkSize = sqlInfo.getMaximumArgsForIn();
            List<Row> rows;
            if (size > chunkSize) {
                List<Serializable> idList = new ArrayList<Serializable>(ids);
                rows = new ArrayList<Row>(size);
                for (int start = 0; start < size; start += chunkSize) {
                    int end = start + chunkSize;
                    if (end > size) {
                        end = size;
                    }
                    // needs to be Serializable -> copy
                    List<Serializable> chunkIds = new ArrayList<Serializable>(
                            idList.subList(start, end));
                    List<Row> chunkRows;
                    if (model.isCollectionFragment(tableName)) {
                        chunkRows = readCollectionArrays(tableName, chunkIds);
                    } else {
                        chunkRows = readSimpleRows(tableName, chunkIds);
                    }
                    rows.addAll(chunkRows);
                }
            } else {
                if (model.isCollectionFragment(tableName)) {
                    rows = readCollectionArrays(tableName, ids);
                } else {
                    rows = readSimpleRows(tableName, ids);
                }
            }
            // check we have all the ids (readSimpleRows may have some
            // missing)
            for (Row row : rows) {
                res.add(row);
                ids.remove(row.id);
            }
            // for the missing ids record an empty RowId
            for (Serializable id : ids) {
                res.add(new RowId(tableName, id));
            }
        }
        return res;
    }

    /**
     * Gets a list of rows for {@link SimpleFragment}s from the database, given
     * the table name and the ids.
     *
     * @param tableName the table name
     * @param ids the ids
     * @return the list of rows, without the missing ones
     */
    protected List<Row> readSimpleRows(String tableName,
            Collection<Serializable> ids) throws StorageException {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        SQLInfoSelect select = sqlInfo.getSelectFragmentsByIds(tableName,
                ids.size());
        Map<String, Serializable> criteriaMap = Collections.singletonMap(
                model.MAIN_KEY, (Serializable) ids);
        return getSelectRows(tableName, select, criteriaMap, null, false);
    }

    /**
     * Reads several collection rows, given a table name and the ids.
     *
     * @param tableName the table name
     * @param ids the ids
     */
    protected List<Row> readCollectionArrays(String tableName,
            Collection<Serializable> ids) throws StorageException {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        String[] orderBys = { model.MAIN_KEY, model.COLL_TABLE_POS_KEY }; // clusters
                                                                          // results
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
                    dialect.setId(ps, i++, id);
                }
                ResultSet rs = ps.executeQuery();
                countExecute();

                // get all values from result set, separate by ids
                // the result set is ordered by id, pos
                CollectionIO io = getCollectionIO(tableName);
                PropertyType ftype = model.getCollectionFragmentType(tableName);
                PropertyType type = ftype.getArrayBaseType();
                Serializable curId = null;
                List<Serializable> list = null;
                Serializable[] returnId = new Serializable[1];
                int[] returnPos = { -1 };
                List<Row> res = new LinkedList<Row>();
                Set<Serializable> remainingIds = new HashSet<Serializable>(ids);
                while (rs.next()) {
                    Serializable value = io.getCurrentFromResultSet(rs,
                            select.whatColumns, model, returnId, returnPos);
                    Serializable newId = returnId[0];
                    if (newId != null && !newId.equals(curId)) {
                        // flush old list
                        if (list != null) {
                            res.add(new Row(tableName, curId,
                                    type.collectionToArray(list)));
                            remainingIds.remove(curId);
                        }
                        curId = newId;
                        list = new ArrayList<Serializable>();
                    }
                    list.add(value);
                }
                if (curId != null && list != null) {
                    // flush last list
                    res.add(new Row(tableName, curId,
                            type.collectionToArray(list)));
                    remainingIds.remove(curId);
                }

                // fill empty ones
                if (!remainingIds.isEmpty()) {
                    Serializable[] emptyArray = ftype.getEmptyArray();
                    for (Serializable id : remainingIds) {
                        res.add(new Row(tableName, id, emptyArray));
                    }
                }
                if (logger.isLogEnabled()) {
                    for (Row row : res) {
                        logger.log("  -> " + row);
                    }
                }
                return res;
            } finally {
                closeStatement(ps);
            }
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Could not select: " + sql, e);
        }
    }

    /**
     * Fetches the rows for a select with fixed criteria given as two maps (a
     * criteriaMap whose values and up in the returned rows, and a joinMap for
     * other criteria).
     */
    protected List<Row> getSelectRows(String tableName, SQLInfoSelect select,
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
                Row row = new Row(tableName, criteriaMap);
                if (select.opaqueColumns != null) {
                    for (Column column : select.opaqueColumns) {
                        row.putNew(column.getKey(), Row.OPAQUE);
                    }
                }
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
                if (v instanceof Collection<?>) {
                    // allow insert of several values, for the IN (...) case
                    for (Object vv : (Collection<?>) v) {
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
            countExecute();

            /*
             * Construct the maps from the result set.
             */
            while (rs.next()) {
                Row row = new Row(tableName, criteriaMap);
                i = 1;
                for (Column column : select.whatColumns) {
                    row.put(column.getKey(), column.getFromResultSet(rs, i++));
                }
                if (select.opaqueColumns != null) {
                    for (Column column : select.opaqueColumns) {
                        row.putNew(column.getKey(), Row.OPAQUE);
                    }
                }
                if (logger.isLogEnabled()) {
                    logger.logResultSet(rs, select.whatColumns);
                }
                list.add(row);
                if (limitToOne) {
                    return list;
                }
            }
            if (limitToOne) {
                return Collections.emptyList();
            }
            return list;
        } catch (Exception e) {
            checkConnectionReset(e, true);
            checkConcurrentUpdate(e);
            throw new StorageException("Could not select: " + select.sql, e);
        } finally {
            if (ps != null) {
                try {
                    closeStatement(ps);
                } catch (SQLException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public void write(RowBatch batch) throws StorageException {
        if (!batch.creates.isEmpty()) {
            writeCreates(batch.creates);
        }
        if (!batch.updates.isEmpty()) {
            writeUpdates(batch.updates);
        }
        if (!batch.deletes.isEmpty()) {
            writeDeletes(batch.deletes);
        }
        // batch.deletesDependent not executed
    }

    protected void writeCreates(List<Row> creates) throws StorageException {
        // reorganize by table
        Map<String, List<Row>> tableRows = new LinkedHashMap<String, List<Row>>();
        // hierarchy table first because there are foreign keys to it
        tableRows.put(model.HIER_TABLE_NAME, new LinkedList<Row>());
        for (Row row : creates) {
            List<Row> rows = tableRows.get(row.tableName);
            if (rows == null) {
                tableRows.put(row.tableName, rows = new LinkedList<Row>());
            }
            rows.add(row);
        }
        // inserts on each table
        for (Entry<String, List<Row>> en : tableRows.entrySet()) {
            String tableName = en.getKey();
            List<Row> rows = en.getValue();
            if (model.isCollectionFragment(tableName)) {
                insertCollectionRows(tableName, rows);
            } else {
                insertSimpleRows(tableName, rows);
            }
        }
    }

    protected void writeUpdates(Set<RowUpdate> updates) throws StorageException {
        // reorganize by table
        Map<String, List<RowUpdate>> tableRows = new HashMap<String, List<RowUpdate>>();
        for (RowUpdate rowu : updates) {
            List<RowUpdate> rows = tableRows.get(rowu.row.tableName);
            if (rows == null) {
                tableRows.put(rowu.row.tableName,
                        rows = new LinkedList<RowUpdate>());
            }
            rows.add(rowu);
        }
        // updates on each table
        for (Entry<String, List<RowUpdate>> en : tableRows.entrySet()) {
            String tableName = en.getKey();
            List<RowUpdate> rows = en.getValue();
            if (model.isCollectionFragment(tableName)) {
                updateCollectionRows(tableName, rows);
            } else {
                updateSimpleRows(tableName, rows);
            }
        }
    }

    protected void writeDeletes(Collection<RowId> deletes)
            throws StorageException {
        // reorganize by table
        Map<String, Set<Serializable>> tableIds = new HashMap<String, Set<Serializable>>();
        for (RowId rowId : deletes) {
            Set<Serializable> ids = tableIds.get(rowId.tableName);
            if (ids == null) {
                tableIds.put(rowId.tableName, ids = new HashSet<Serializable>());
            }
            ids.add(rowId.id);
        }
        // delete on each table
        for (Entry<String, Set<Serializable>> en : tableIds.entrySet()) {
            String tableName = en.getKey();
            Set<Serializable> ids = en.getValue();
            deleteRows(tableName, ids);
        }
    }

    /**
     * Inserts multiple rows, all for the same table.
     */
    protected void insertSimpleRows(String tableName, List<Row> rows)
            throws StorageException {
        if (rows.isEmpty()) {
            return;
        }
        String sql = sqlInfo.getInsertSql(tableName);
        if (sql == null) {
            throw new StorageException("Unknown table: " + tableName);
        }
        String loggedSql = supportsBatchUpdates && rows.size() > 1 ? sql
                + " -- BATCHED" : sql;
        List<Column> columns = sqlInfo.getInsertColumns(tableName);
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                int batch = 0;
                for (Row row : rows) {
                    batch++;
                    if (logger.isLogEnabled()) {
                        logger.logSQL(loggedSql, columns, row);
                    }
                    int i = 1;
                    for (Column column : columns) {
                        column.setToPreparedStatement(ps, i++,
                                row.get(column.getKey()));
                    }
                    if (supportsBatchUpdates) {
                        ps.addBatch();
                        if (batch % UPDATE_BATCH_SIZE == 0) {
                            ps.executeBatch();
                            countExecute();
                        }
                    } else {
                        ps.execute();
                        countExecute();
                    }
                }
                if (supportsBatchUpdates) {
                    ps.executeBatch();
                    countExecute();
                }
            } finally {
                closeStatement(ps);
            }
        } catch (Exception e) {
            checkConnectionReset(e);
            if (e instanceof BatchUpdateException) {
                BatchUpdateException bue = (BatchUpdateException) e;
                if (e.getCause() == null && bue.getNextException() != null) {
                    // provide a readable cause in the stack trace
                    e.initCause(bue.getNextException());
                }
            }
            checkConcurrentUpdate(e);
            throw new StorageException("Could not insert: " + sql, e);
        }
    }

    /**
     * Updates multiple collection rows, all for the same table.
     */
    protected void insertCollectionRows(String tableName, List<Row> rows)
            throws StorageException {
        if (rows.isEmpty()) {
            return;
        }
        String sql = sqlInfo.getInsertSql(tableName);
        List<Column> columns = sqlInfo.getInsertColumns(tableName);
        CollectionIO io = getCollectionIO(tableName);
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                io.executeInserts(ps, rows, columns, supportsBatchUpdates, sql,
                        this);
            } finally {
                closeStatement(ps);
            }
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Could not insert: " + sql, e);
        }
    }

    /**
     * Updates multiple simple rows, all for the same table.
     */
    protected void updateSimpleRows(String tableName, List<RowUpdate> rows)
            throws StorageException {
        if (rows.isEmpty()) {
            return;
        }

        // reorganize by unique sets of keys
        Map<String, List<RowUpdate>> updatesByKeys = new HashMap<String, List<RowUpdate>>();
        for (RowUpdate rowu : rows) {
            List<String> keys = new ArrayList<String>(rowu.keys);
            if (keys.isEmpty()) {
                continue;
            }
            Collections.sort(keys);
            String k = StringUtils.join(keys, ","); // canonical keys
            List<RowUpdate> keysUpdates = updatesByKeys.get(k);
            if (keysUpdates == null) {
                updatesByKeys.put(k, keysUpdates = new LinkedList<RowUpdate>());
            }
            keysUpdates.add(rowu);
        }

        for (List<RowUpdate> keysUpdates : updatesByKeys.values()) {
            Collection<String> keys = keysUpdates.iterator().next().keys;
            SQLInfoSelect update = sqlInfo.getUpdateById(tableName, keys);
            String loggedSql = supportsBatchUpdates && rows.size() > 1 ? update.sql
                    + " -- BATCHED"
                    : update.sql;
            try {
                PreparedStatement ps = connection.prepareStatement(update.sql);
                int batch = 0;
                try {
                    for (RowUpdate rowu : keysUpdates) {
                        batch++;
                        if (logger.isLogEnabled()) {
                            logger.logSQL(loggedSql, update.whatColumns,
                                    rowu.row);
                        }
                        int i = 1;
                        for (Column column : update.whatColumns) {
                            column.setToPreparedStatement(ps, i++,
                                    rowu.row.get(column.getKey()));
                        }
                        if (supportsBatchUpdates) {
                            ps.addBatch();
                            if (batch % UPDATE_BATCH_SIZE == 0) {
                                int[] counts = ps.executeBatch();
                                countExecute();
                                logger.logCounts(counts);
                            }
                        } else {
                            int count = ps.executeUpdate();
                            countExecute();
                            logger.logCount(count);
                        }
                    }
                    if (supportsBatchUpdates) {
                        int[] counts = ps.executeBatch();
                        countExecute();
                        logger.logCounts(counts);
                    }
                } finally {
                    closeStatement(ps);
                }
            } catch (Exception e) {
                checkConnectionReset(e);
                throw new StorageException("Could not update: " + update.sql, e);
            }
        }
    }

    protected void updateCollectionRows(String tableName, List<RowUpdate> rowus)
            throws StorageException {
        Set<Serializable> ids = new HashSet<Serializable>(rowus.size());
        List<Row> rows = new ArrayList<Row>(rowus.size());
        for (RowUpdate rowu : rowus) {
            ids.add(rowu.row.id);
            rows.add(rowu.row);
        }
        deleteRows(tableName, ids);
        insertCollectionRows(tableName, rows);
    }

    /**
     * Deletes multiple rows, all for the same table.
     */
    protected void deleteRows(String tableName, Set<Serializable> ids)
            throws StorageException {
        if (ids.isEmpty()) {
            return;
        }
        int size = ids.size();
        int chunkSize = sqlInfo.getMaximumArgsForIn();
        if (size > chunkSize) {
            List<Serializable> idList = new ArrayList<Serializable>(ids);
            for (int start = 0; start < size; start += chunkSize) {
                int end = start + chunkSize;
                if (end > size) {
                    end = size;
                }
                // needs to be Serializable -> copy
                List<Serializable> chunkIds = new ArrayList<Serializable>(
                        idList.subList(start, end));
                deleteRowsDirect(tableName, chunkIds);
            }
        } else {
            deleteRowsDirect(tableName, ids);
        }
    }

    protected void deleteRowsSoft(List<NodeInfo> nodeInfos)
            throws StorageException {
        try {
            int size = nodeInfos.size();
            List<Serializable> ids = new ArrayList<Serializable>(size);
            for (NodeInfo info : nodeInfos) {
                ids.add(info.id);
            }
            int chunkSize = 100; // max size of ids array
            if (size <= chunkSize) {
                doSoftDeleteRows(ids);
            } else {
                for (int start = 0; start < size;) {
                    int end = start + chunkSize;
                    if (end > size) {
                        end = size;
                    }
                    doSoftDeleteRows(ids.subList(start, end));
                    start = end;
                }
            }
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Could not soft delete", e);
        }
    }

    // not chunked
    protected void doSoftDeleteRows(List<Serializable> ids) throws SQLException {
        Serializable whereIds = newIdArray(ids);
        Calendar now = Calendar.getInstance();
        String sql = sqlInfo.getSoftDeleteSql();
        if (logger.isLogEnabled()) {
            logger.logSQL(sql, Arrays.asList(whereIds, now));
        }
        PreparedStatement ps = connection.prepareStatement(sql);
        try {
            setToPreparedStatementIdArray(ps, 1, whereIds);
            dialect.setToPreparedStatementTimestamp(ps, 2, now, null);
            ps.execute();
            countExecute();
            return;
        } finally {
            closeStatement(ps);
        }
    }

    protected Serializable newIdArray(Collection<Serializable> ids) {
        if (dialect.supportsArrays()) {
            return ids.toArray(); // Object[]
        } else {
            // join with '|'
            StringBuilder b = new StringBuilder();
            for (Serializable id : ids) {
                b.append(id);
                b.append('|');
            }
            b.setLength(b.length() - 1);
            return b.toString();
        }
    }

    protected void setToPreparedStatementIdArray(PreparedStatement ps,
            int index, Serializable idArray) throws SQLException {
        if (idArray instanceof String) {
            ps.setString(index, (String) idArray);
        } else {
            Array array = dialect.createArrayOf(Types.OTHER,
                    (Object[]) idArray, connection);
            ps.setArray(index, array);
        }
    }

    /**
     * Clean up soft-deleted rows.
     * <p>
     * Rows deleted more recently than the beforeTime are left alone. Only a
     * limited number of rows may be deleted, to prevent transaction during too
     * long.
     * @param max the maximum number of rows to delete at a time
     * @param beforeTime the maximum deletion time of the rows to delete
     *
     * @return the number of rows deleted
     * @throws StorageException
     */
    public int cleanupDeletedRows(int max, Calendar beforeTime)
            throws StorageException {
        if (max < 0) {
            max = 0;
        }
        String sql = sqlInfo.getSoftDeleteCleanupSql();
        if (logger.isLogEnabled()) {
            logger.logSQL(
                    sql,
                    Arrays.<Serializable> asList(beforeTime,
                            Long.valueOf(max)));
        }
        try {
            if (sql.startsWith("{")) {
                // callable statement
                boolean outFirst = sql.startsWith("{?=");
                int outIndex = outFirst ? 1 : 3;
                int inIndex = outFirst ? 2 : 1;
                CallableStatement cs = connection.prepareCall(sql);
                try {
                    cs.setInt(inIndex, max);
                    dialect.setToPreparedStatementTimestamp(cs, inIndex + 1,
                            beforeTime, null);
                    cs.registerOutParameter(outIndex, Types.INTEGER);
                    cs.execute();
                    int count = cs.getInt(outIndex);
                    logger.logCount(count);
                    return count;
                } finally {
                    cs.close();
                }
            } else {
                // standard prepared statement with result set
                PreparedStatement ps = connection.prepareStatement(sql);
                try {
                    ps.setInt(1, max);
                    dialect.setToPreparedStatementTimestamp(ps, 2, beforeTime,
                            null);
                    ResultSet rs = ps.executeQuery();
                    countExecute();
                    if (!rs.next()) {
                        throw new StorageException("Cannot get result");
                    }
                    int count = rs.getInt(1);
                    logger.logCount(count);
                    return count;
                } finally {
                    closeStatement(ps);
                }
            }
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Could not purge soft delete", e);
        }
    }

    protected void deleteRowsDirect(String tableName,
            Collection<Serializable> ids) throws StorageException {
        try {
            String sql = sqlInfo.getDeleteSql(tableName, ids.size());
            if (logger.isLogEnabled()) {
                logger.logSQL(sql, ids);
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                int i = 1;
                for (Serializable id : ids) {
                    dialect.setId(ps, i++, id);
                }
                int count = ps.executeUpdate();
                countExecute();
                logger.logCount(count);
            } finally {
                closeStatement(ps);
            }
        } catch (Exception e) {
            checkConnectionReset(e);
            checkConcurrentUpdate(e);
            throw new StorageException("Could not delete: " + tableName, e);
        }
    }

    @Override
    public Row readSimpleRow(RowId rowId) throws StorageException {
        SQLInfoSelect select = sqlInfo.selectFragmentById.get(rowId.tableName);
        Map<String, Serializable> criteriaMap = Collections.singletonMap(
                model.MAIN_KEY, rowId.id);
        List<Row> maps = getSelectRows(rowId.tableName, select, criteriaMap,
                null, true);
        return maps.isEmpty() ? null : maps.get(0);
    }

    @Override
    public Map<String, String> getBinaryFulltext(RowId rowId) throws StorageException {
        ArrayList<String> columns = new ArrayList<String>();
        for (String index: model.getFulltextConfiguration().indexesAllBinary) {
            String col = Model.FULLTEXT_BINARYTEXT_KEY + model.getFulltextIndexSuffix(index);
            columns.add(col);
        }
        Serializable id = rowId.id;
        Map<String, String> ret = new HashMap<String, String>(columns.size());
        String sql = dialect.getBinaryFulltextSql(columns);
        if (sql == null) {
            logger.info("getBinaryFulltextSql not supported for dialect " + dialect);
            return ret;
        }
        if (logger.isLogEnabled()) {
            logger.logSQL(sql, Collections.singletonList(id));
        }
        PreparedStatement ps;
        try {
            ps = connection.prepareStatement(sql);
            try {
                dialect.setId(ps, 1, id);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    for (int i=1; i <= columns.size(); i++) {
                        ret.put(columns.get(i-1), rs.getString(i));
                    }
                }
                if (logger.isLogEnabled()) {
                    logger.log("  -> " + ret);
                }
            } finally {
                closeStatement(ps);
            }
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Could not select: " + sql, e);
        }
        return ret;
    }

    @Override
    public Serializable[] readCollectionRowArray(RowId rowId)
            throws StorageException {
        String tableName = rowId.tableName;
        Serializable id = rowId.id;
        String sql = sqlInfo.selectFragmentById.get(tableName).sql;
        try {
            // XXX statement should be already prepared
            if (logger.isLogEnabled()) {
                logger.logSQL(sql, Collections.singletonList(id));
            }
            PreparedStatement ps = connection.prepareStatement(sql);
            try {
                List<Column> columns = sqlInfo.selectFragmentById.get(tableName).whatColumns;
                dialect.setId(ps, 1, id); // assumes only one primary column
                ResultSet rs = ps.executeQuery();
                countExecute();

                // construct the resulting collection using each row
                CollectionIO io = getCollectionIO(tableName);
                List<Serializable> list = new ArrayList<Serializable>();
                Serializable[] returnId = new Serializable[1];
                int[] returnPos = { -1 };
                while (rs.next()) {
                    list.add(io.getCurrentFromResultSet(rs, columns, model,
                            returnId, returnPos));
                }
                PropertyType type = model.getCollectionFragmentType(tableName).getArrayBaseType();
                Serializable[] array = type.collectionToArray(list);

                if (logger.isLogEnabled()) {
                    logger.log("  -> " + Arrays.asList(array));
                }
                return array;
            } finally {
                closeStatement(ps);
            }
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Could not select: " + sql, e);
        }
    }

    @Override
    public List<Row> readSelectionRows(SelectionType selType,
            Serializable selId, Serializable filter, Serializable criterion,
            boolean limitToOne) throws StorageException {
        SQLInfoSelection selInfo = sqlInfo.getSelection(selType);
        Map<String, Serializable> criteriaMap = new HashMap<String, Serializable>();
        criteriaMap.put(selType.selKey, selId);
        SQLInfoSelect select;
        if (filter == null) {
            select = selInfo.selectAll;
        } else {
            select = selInfo.selectFiltered;
            criteriaMap.put(selType.filterKey, filter);
        }
        if (selType.criterionKey != null) {
            criteriaMap.put(selType.criterionKey, criterion);
        }
        return getSelectRows(selType.tableName, select, criteriaMap, null,
                limitToOne);
    }

    @Override
    public CopyResult copy(IdWithTypes source, Serializable destParentId,
            String destName, Row overwriteRow) throws StorageException {
        // assert !model.separateMainTable; // other case not implemented
        Invalidations invalidations = new Invalidations();
        try {
            Map<Serializable, Serializable> idMap = new LinkedHashMap<Serializable, Serializable>();
            Map<Serializable, IdWithTypes> idToTypes = new HashMap<Serializable, IdWithTypes>();
            // copy the hierarchy fragments recursively
            Serializable overwriteId = overwriteRow == null ? null
                    : overwriteRow.id;
            if (overwriteId != null) {
                // overwrite hier root with explicit values
                String tableName = model.HIER_TABLE_NAME;
                updateSimpleRowWithValues(tableName, overwriteRow);
                idMap.put(source.id, overwriteId);
                // invalidate
                invalidations.addModified(new RowId(tableName, overwriteId));
            }
            // create the new hierarchy by copy
            boolean resetVersion = destParentId != null;
            Serializable newRootId = copyHierRecursive(source, destParentId,
                    destName, overwriteId, resetVersion, idMap, idToTypes);
            // invalidate children
            Serializable invalParentId = overwriteId == null ? destParentId
                    : overwriteId;
            if (invalParentId != null) { // null for a new version
                invalidations.addModified(new RowId(Invalidations.PARENT,
                        invalParentId));
            }
            // copy all collected fragments
            Set<Serializable> proxyIds = new HashSet<Serializable>();
            for (Entry<String, Set<Serializable>> entry : model.getPerFragmentIds(
                    idToTypes).entrySet()) {
                String tableName = entry.getKey();
                if (tableName.equals(model.HIER_TABLE_NAME)) {
                    // already done
                    continue;
                }
                if (tableName.equals(model.VERSION_TABLE_NAME)) {
                    // versions not fileable
                    // restore must not copy versions either
                    continue;
                }
                Set<Serializable> ids = entry.getValue();
                if (tableName.equals(model.PROXY_TABLE_NAME)) {
                    for (Serializable id : ids) {
                        proxyIds.add(idMap.get(id)); // copied ids
                    }
                }
                Boolean invalidation = copyRows(tableName, ids, idMap,
                        overwriteId);
                if (invalidation != null) {
                    // overwrote something
                    // make sure things are properly invalidated in this and
                    // other sessions
                    if (Boolean.TRUE.equals(invalidation)) {
                        invalidations.addModified(new RowId(tableName,
                                overwriteId));
                    } else {
                        invalidations.addDeleted(new RowId(tableName,
                                overwriteId));
                    }
                }
            }
            return new CopyResult(newRootId, invalidations, proxyIds);
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Could not copy: "
                    + source.id.toString(), e);
        }
    }

    /**
     * Updates a row in the database with given explicit values.
     */
    protected void updateSimpleRowWithValues(String tableName, Row row)
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
                List<String> keys = row.getKeys();
                List<Serializable> values = row.getValues();
                int size = keys.size();
                for (int r = 0; r < size; r++) {
                    String key = keys.get(r);
                    Serializable value = values.get(r);
                    table.getColumn(key).setToPreparedStatement(ps, i++, value);
                }
                dialect.setId(ps, i, row.id); // id last in SQL
                int count = ps.executeUpdate();
                countExecute();
                logger.logCount(count);
            } finally {
                closeStatement(ps);
            }
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Could not update: " + sql, e);
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
    protected Serializable copyHierRecursive(IdWithTypes source,
            Serializable parentId, String name, Serializable overwriteId,
            boolean resetVersion, Map<Serializable, Serializable> idMap,
            Map<Serializable, IdWithTypes> idToTypes) throws SQLException {
        idToTypes.put(source.id, source);
        Serializable newId;
        if (overwriteId == null) {
            newId = copyHier(source.id, parentId, name, resetVersion, idMap);
        } else {
            newId = overwriteId;
            idMap.put(source.id, newId);
        }
        // recurse in children
        boolean onlyComplex = parentId == null;
        for (IdWithTypes child : getChildrenIdsWithTypes(source.id, onlyComplex)) {
            copyHierRecursive(child, newId, null, null, resetVersion, idMap, idToTypes);
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
    protected Serializable copyHier(Serializable id, Serializable parentId,
            String name, boolean resetVersion,
            Map<Serializable, Serializable> idMap) throws SQLException {
        boolean explicitName = name != null;

        SQLInfoSelect copy = sqlInfo.getCopyHier(explicitName, resetVersion);
        PreparedStatement ps = connection.prepareStatement(copy.sql);
        try {
            Serializable newId = generateNewIdInternal();

            List<Serializable> debugValues = null;
            if (logger.isLogEnabled()) {
                debugValues = new ArrayList<Serializable>(4);
            }
            int i = 1;
            for (Column column : copy.whatColumns) {
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
                } else if (key.equals(model.MAIN_BASE_VERSION_KEY)
                        || key.equals(model.MAIN_CHECKED_IN_KEY)) {
                    v = null;
                } else if (key.equals(model.MAIN_MINOR_VERSION_KEY)
                        || key.equals(model.MAIN_MAJOR_VERSION_KEY)) {
                    // present if reset version (regular copy, not checkin)
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
            Column whereColumn = copy.whereColumns.get(0);
            whereColumn.setToPreparedStatement(ps, i, id);
            if (debugValues != null) {
                debugValues.add(id);
                logger.logSQL(copy.sql, debugValues);
            }
            int count = ps.executeUpdate();
            countExecute();
            logger.logCount(count);

            // TODO DB_IDENTITY
            // post insert fetch idrow

            idMap.put(id, newId);
            return newId;
        } finally {
            closeStatement(ps);
        }
    }

    /**
     * Gets the children ids and types of a node.
     */
    protected List<IdWithTypes> getChildrenIdsWithTypes(Serializable id,
            boolean onlyComplex) throws SQLException {
        List<IdWithTypes> children = new LinkedList<IdWithTypes>();
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
            dialect.setId(ps, 1, id); // parent id
            ResultSet rs = ps.executeQuery();
            countExecute();
            while (rs.next()) {
                Serializable childId = null;
                String childPrimaryType = null;
                String[] childMixinTypes = null;
                int i = 1;
                for (Column column : columns) {
                    String key = column.getKey();
                    Serializable value = column.getFromResultSet(rs, i++);
                    if (key.equals(model.MAIN_KEY)) {
                        childId = value;
                    } else if (key.equals(model.MAIN_PRIMARY_TYPE_KEY)) {
                        childPrimaryType = (String) value;
                    } else if (key.equals(model.MAIN_MIXIN_TYPES_KEY)) {
                        childMixinTypes = (String[]) value;
                    }
                }
                children.add(new IdWithTypes(childId, childPrimaryType,
                        childMixinTypes));
                if (debugValues != null) {
                    debugValues.add(childId + "/" + childPrimaryType + "/"
                            + Arrays.toString(childMixinTypes));
                }
            }
            if (debugValues != null) {
                logger.log("  -> " + debugValues);
            }
            return children;
        } finally {
            closeStatement(ps);
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
                    dialect.setId(deletePs, 1, newId);
                    int delCount = deletePs.executeUpdate();
                    countExecute();
                    logger.logCount(delCount);
                    before = delCount > 0;
                }
                copyIdColumn.setToPreparedStatement(copyPs, 1, newId);
                copyIdColumn.setToPreparedStatement(copyPs, 2, id);
                if (logger.isLogEnabled()) {
                    logger.logSQL(copySql, Arrays.asList(newId, id));
                }
                int copyCount = copyPs.executeUpdate();
                countExecute();
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
            closeStatement(copyPs);
            closeStatement(deletePs);
        }
    }

    @Override
    public List<NodeInfo> remove(NodeInfo rootInfo) throws StorageException {
        Serializable rootId = rootInfo.id;
        List<NodeInfo> info = getDescendantsInfo(rootId);
        info.add(rootInfo);
        if (sqlInfo.softDeleteEnabled) {
            deleteRowsSoft(info);
        } else {
            deleteRowsDirect(model.HIER_TABLE_NAME, Collections.singleton(rootId));
        }
        return info;
    }

    protected List<NodeInfo> getDescendantsInfo(Serializable rootId)
            throws StorageException {
        List<NodeInfo> descendants = new LinkedList<NodeInfo>();
        String sql = sqlInfo.getSelectDescendantsInfoSql();
        if (logger.isLogEnabled()) {
            logger.logSQL(sql, Collections.singletonList(rootId));
        }
        List<Column> columns = sqlInfo.getSelectDescendantsInfoWhatColumns();
        PreparedStatement ps = null;
        try {
            ps = connection.prepareStatement(sql);
            List<String> debugValues = null;
            if (logger.isLogEnabled()) {
                debugValues = new LinkedList<String>();
            }
            dialect.setId(ps, 1, rootId); // parent id
            ResultSet rs = ps.executeQuery();
            countExecute();
            while (rs.next()) {
                Serializable id = null;
                Serializable parentId = null;
                String primaryType = null;
                Boolean isProperty = null;
                Serializable targetId = null;
                Serializable versionableId = null;
                int i = 1;
                for (Column column : columns) {
                    String key = column.getKey();
                    Serializable value = column.getFromResultSet(rs, i++);
                    if (key.equals(model.MAIN_KEY)) {
                        id = value;
                    } else if (key.equals(model.HIER_PARENT_KEY)) {
                        parentId = value;
                    } else if (key.equals(model.MAIN_PRIMARY_TYPE_KEY)) {
                        primaryType = (String) value;
                    } else if (key.equals(model.HIER_CHILD_ISPROPERTY_KEY)) {
                        isProperty = (Boolean) value;
                    } else if (key.equals(model.PROXY_TARGET_KEY)) {
                        targetId = value;
                    } else if (key.equals(model.PROXY_VERSIONABLE_KEY)) {
                        versionableId = value;
                    }
                    // no mixins (not useful to caller)
                    // no versions (not fileable)
                }
                descendants.add(new NodeInfo(id, parentId, primaryType,
                        isProperty, versionableId, targetId));
                if (debugValues != null) {
                    if (debugValues.size() < DEBUG_MAX_TREE) {
                        debugValues.add(id + "/" + primaryType);
                    }
                }
            }
            if (debugValues != null) {
                if (debugValues.size() >= DEBUG_MAX_TREE) {
                    debugValues.add("... (" + descendants.size() + ") results");
                }
                logger.log("  -> " + debugValues);
            }
            return descendants;
        } catch (Exception e) {
            checkConnectionReset(e);
            throw new StorageException("Failed to get descendants", e);
        } finally {
            if (ps != null) {
                try {
                    closeStatement(ps);
                } catch (SQLException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

}
