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

import java.io.Serializable;
import java.sql.Array;
import java.sql.BatchUpdateException;
import java.sql.CallableStatement;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.model.Delta;
import org.nuxeo.ecm.core.storage.sql.ClusterInvalidator;
import org.nuxeo.ecm.core.storage.sql.Invalidations;
import org.nuxeo.ecm.core.storage.sql.InvalidationsPropagator;
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
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.services.config.ConfigurationService;

/**
 * A {@link JDBCRowMapper} maps {@link Row}s to and from a JDBC database.
 */
public class JDBCRowMapper extends JDBCConnection implements RowMapper {

    public static final int UPDATE_BATCH_SIZE = 100; // also insert/delete

    public static final int DEBUG_MAX_TREE = 50;

    /** Property to determine whether collection appends delete all then re-insert, or are optimized for append. */
    public static final String COLLECTION_DELETE_BEFORE_APPEND_PROP = "org.nuxeo.vcs.list-delete-before-append";

    /**
     * Cluster invalidator, or {@code null} if this mapper does not participate in invalidation propagation (cluster
     * invalidator, lock manager).
     */
    private final ClusterInvalidator clusterInvalidator;

    private final InvalidationsPropagator invalidationsPropagator;

    private final boolean collectionDeleteBeforeAppend;

    private final CollectionIO aclCollectionIO;

    private final CollectionIO scalarCollectionIO;

    public JDBCRowMapper(Model model, SQLInfo sqlInfo, ClusterInvalidator clusterInvalidator,
            InvalidationsPropagator invalidationsPropagator) {
        super(model, sqlInfo);
        this.clusterInvalidator = clusterInvalidator;
        this.invalidationsPropagator = invalidationsPropagator;
        ConfigurationService configurationService = Framework.getService(ConfigurationService.class);
        collectionDeleteBeforeAppend = configurationService.isBooleanPropertyTrue(COLLECTION_DELETE_BEFORE_APPEND_PROP);
        aclCollectionIO = new ACLCollectionIO(collectionDeleteBeforeAppend);
        scalarCollectionIO = new ScalarCollectionIO(collectionDeleteBeforeAppend);
    }

    @Override
    public Invalidations receiveInvalidations() {
        if (clusterInvalidator != null) {
            Invalidations invalidations = clusterInvalidator.receiveInvalidations();
            // send received invalidations to all mappers
            if (invalidations != null && !invalidations.isEmpty()) {
                invalidationsPropagator.propagateInvalidations(invalidations, null);
            }
            return invalidations;
        } else {
            return null;
        }
    }


    @Override
    public void sendInvalidations(Invalidations invalidations) {
        if (clusterInvalidator != null) {
            clusterInvalidator.sendInvalidations(invalidations);
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
        return tableName.equals(Model.ACL_TABLE_NAME) ? aclCollectionIO : scalarCollectionIO;
    }

    @Override
    public Serializable generateNewId() {
        try {
            return dialect.getGeneratedId(connection);
        } catch (SQLException e) {
            throw new NuxeoException(e);
        }
    }

    /*
     * ----- RowIO -----
     */

    @Override
    public List<? extends RowId> read(Collection<RowId> rowIds, boolean cacheOnly) {
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
                    List<Serializable> chunkIds = new ArrayList<Serializable>(idList.subList(start, end));
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
     * Gets a list of rows for {@link SimpleFragment}s from the database, given the table name and the ids.
     *
     * @param tableName the table name
     * @param ids the ids
     * @return the list of rows, without the missing ones
     */
    protected List<Row> readSimpleRows(String tableName, Collection<Serializable> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        SQLInfoSelect select = sqlInfo.getSelectFragmentsByIds(tableName, ids.size());
        Map<String, Serializable> criteriaMap = Collections.singletonMap(Model.MAIN_KEY, (Serializable) ids);
        return getSelectRows(tableName, select, criteriaMap, null, false);
    }

    /**
     * Reads several collection rows, given a table name and the ids.
     *
     * @param tableName the table name
     * @param ids the ids
     */
    protected List<Row> readCollectionArrays(String tableName, Collection<Serializable> ids) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        String[] orderBys = { Model.MAIN_KEY, Model.COLL_TABLE_POS_KEY }; // clusters
                                                                          // results
        Set<String> skipColumns = new HashSet<String>(Arrays.asList(Model.COLL_TABLE_POS_KEY));
        SQLInfoSelect select = sqlInfo.getSelectFragmentsByIds(tableName, ids.size(), orderBys, skipColumns);

        String sql = select.sql;
        if (logger.isLogEnabled()) {
            logger.logSQL(sql, ids);
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            for (Serializable id : ids) {
                dialect.setId(ps, i++, id);
            }
            try (ResultSet rs = ps.executeQuery()) {
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
                    Serializable value = io.getCurrentFromResultSet(rs, select.whatColumns, model, returnId, returnPos);
                    Serializable newId = returnId[0];
                    if (newId != null && !newId.equals(curId)) {
                        // flush old list
                        if (list != null) {
                            res.add(new Row(tableName, curId, type.collectionToArray(list)));
                            remainingIds.remove(curId);
                        }
                        curId = newId;
                        list = new ArrayList<Serializable>();
                    }
                    list.add(value);
                }
                if (curId != null && list != null) {
                    // flush last list
                    res.add(new Row(tableName, curId, type.collectionToArray(list)));
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
            }
        } catch (SQLException e) {
            throw new NuxeoException("Could not select: " + sql, e);
        }
    }

    /**
     * Fetches the rows for a select with fixed criteria given as two maps (a criteriaMap whose values and up in the
     * returned rows, and a joinMap for other criteria).
     */
    protected List<Row> getSelectRows(String tableName, SQLInfoSelect select, Map<String, Serializable> criteriaMap,
            Map<String, Serializable> joinMap, boolean limitToOne) {
        List<Row> list = new LinkedList<Row>();
        if (select.whatColumns.isEmpty()) {
            // happens when we fetch a fragment whose columns are all opaque
            // check it's a by-id query
            if (select.whereColumns.size() == 1 && select.whereColumns.get(0).getKey() == Model.MAIN_KEY
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
        try (PreparedStatement ps = connection.prepareStatement(select.sql)) {

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
                    throw new NuxeoException("Null value for key: " + key);
                }
                if (v instanceof Collection<?>) {
                    // allow insert of several values, for the IN (...) case
                    for (Object vv : (Collection<?>) v) {
                        column.setToPreparedStatement(ps, i++, (Serializable) vv);
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
            try (ResultSet rs = ps.executeQuery()) {
                countExecute();

                /*
                 * Construct the maps from the result set.
                 */
                while (rs.next()) {
                    // TODO using criteriaMap is wrong if it contains a Collection
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
            }
            if (limitToOne) {
                return Collections.emptyList();
            }
            return list;
        } catch (SQLException e) {
            checkConcurrentUpdate(e);
            throw new NuxeoException("Could not select: " + select.sql, e);
        }
    }

    @Override
    public void write(RowBatch batch) {
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

    protected void writeCreates(List<Row> creates) {
        // reorganize by table
        Map<String, List<Row>> tableRows = new LinkedHashMap<String, List<Row>>();
        // hierarchy table first because there are foreign keys to it
        tableRows.put(Model.HIER_TABLE_NAME, new LinkedList<Row>());
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
                List<RowUpdate> rowus = rows.stream().map(RowUpdate::new).collect(Collectors.toList());
                insertCollectionRows(tableName, rowus);
            } else {
                insertSimpleRows(tableName, rows);
            }
        }
    }

    protected void writeUpdates(Set<RowUpdate> updates) {
        // reorganize by table
        Map<String, List<RowUpdate>> tableRows = new HashMap<String, List<RowUpdate>>();
        for (RowUpdate rowu : updates) {
            List<RowUpdate> rows = tableRows.get(rowu.row.tableName);
            if (rows == null) {
                tableRows.put(rowu.row.tableName, rows = new LinkedList<RowUpdate>());
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

    protected void writeDeletes(Collection<RowId> deletes) {
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
    protected void insertSimpleRows(String tableName, List<Row> rows) {
        if (rows.isEmpty()) {
            return;
        }
        String sql = sqlInfo.getInsertSql(tableName);
        if (sql == null) {
            throw new NuxeoException("Unknown table: " + tableName);
        }
        boolean batched = supportsBatchUpdates && rows.size() > 1;
        String loggedSql = batched ? sql + " -- BATCHED" : sql;
        List<Column> columns = sqlInfo.getInsertColumns(tableName);
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int batch = 0;
            for (Iterator<Row> rowIt = rows.iterator(); rowIt.hasNext();) {
                Row row = rowIt.next();
                if (logger.isLogEnabled()) {
                    logger.logSQL(loggedSql, columns, row);
                }
                int i = 1;
                for (Column column : columns) {
                    column.setToPreparedStatement(ps, i++, row.get(column.getKey()));
                }
                if (batched) {
                    ps.addBatch();
                    batch++;
                    if (batch % UPDATE_BATCH_SIZE == 0 || !rowIt.hasNext()) {
                        ps.executeBatch();
                        countExecute();
                    }
                } else {
                    ps.execute();
                    countExecute();
                }
            }
        } catch (SQLException e) {
            if (e instanceof BatchUpdateException) {
                BatchUpdateException bue = (BatchUpdateException) e;
                if (e.getCause() == null && bue.getNextException() != null) {
                    // provide a readable cause in the stack trace
                    e.initCause(bue.getNextException());
                }
            }
            checkConcurrentUpdate(e);
            throw new NuxeoException("Could not insert: " + sql, e);
        }
    }

    /**
     * Updates multiple collection rows, all for the same table.
     */
    protected void insertCollectionRows(String tableName, List<RowUpdate> rowus) {
        if (rowus.isEmpty()) {
            return;
        }
        String sql = sqlInfo.getInsertSql(tableName);
        List<Column> columns = sqlInfo.getInsertColumns(tableName);
        CollectionIO io = getCollectionIO(tableName);
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            io.executeInserts(ps, rowus, columns, supportsBatchUpdates, sql, this);
        } catch (SQLException e) {
            throw new NuxeoException("Could not insert: " + sql, e);
        }
    }

    /**
     * Updates multiple simple rows, all for the same table.
     */
    protected void updateSimpleRows(String tableName, List<RowUpdate> rows) {
        if (rows.isEmpty()) {
            return;
        }

        // reorganize by identical queries to allow batching
        Map<String, SQLInfoSelect> sqlToInfo = new HashMap<>();
        Map<String, List<RowUpdate>> sqlRowUpdates = new HashMap<>();
        for (RowUpdate rowu : rows) {
            SQLInfoSelect update = sqlInfo.getUpdateById(tableName, rowu);
            String sql = update.sql;
            sqlToInfo.put(sql, update);
            sqlRowUpdates.computeIfAbsent(sql, k -> new ArrayList<RowUpdate>()).add(rowu);
        }

        for (Entry<String, List<RowUpdate>> en : sqlRowUpdates.entrySet()) {
            String sql = en.getKey();
            List<RowUpdate> rowUpdates = en.getValue();
            SQLInfoSelect update = sqlToInfo.get(sql);
            boolean changeTokenEnabled = model.getRepositoryDescriptor().isChangeTokenEnabled();
            boolean batched = supportsBatchUpdates && rowUpdates.size() > 1
                    && (dialect.supportsBatchUpdateCount() || !changeTokenEnabled);
            String loggedSql = batched ? update.sql + " -- BATCHED" : update.sql;
            try (PreparedStatement ps = connection.prepareStatement(update.sql)) {
                int batch = 0;
                for (Iterator<RowUpdate> rowIt = rowUpdates.iterator(); rowIt.hasNext();) {
                    RowUpdate rowu = rowIt.next();
                    if (logger.isLogEnabled()) {
                        logger.logSQL(loggedSql, update.whatColumns, rowu.row, update.whereColumns, rowu.conditions);
                    }
                    int i = 1;
                    for (Column column : update.whatColumns) {
                        Serializable value = rowu.row.get(column.getKey());
                        if (value instanceof Delta) {
                            value = ((Delta) value).getDeltaValue();
                        }
                        column.setToPreparedStatement(ps, i++, value);
                    }
                    boolean hasConditions = false;
                    for (Column column : update.whereColumns) {
                        // id or condition
                        String key = column.getKey();
                        Serializable value;
                        if (key.equals(Model.MAIN_KEY)) {
                            value = rowu.row.get(key);
                        } else {
                            hasConditions = true;
                            value = rowu.conditions.get(key);
                        }
                        column.setToPreparedStatement(ps, i++, value);
                    }
                    if (batched) {
                        ps.addBatch();
                        batch++;
                        if (batch % UPDATE_BATCH_SIZE == 0 || !rowIt.hasNext()) {
                            int[] counts = ps.executeBatch();
                            countExecute();
                            if (changeTokenEnabled && hasConditions) {
                                for (int j = 0; j < counts.length; j++) {
                                    int count = counts[j];
                                    if (count != Statement.SUCCESS_NO_INFO && count != 1) {
                                        Serializable id = rowUpdates.get(j).row.id;
                                        logger.log("  -> CONCURRENT UPDATE: " + id);
                                        throw new ConcurrentUpdateException(id.toString());
                                    }
                                }
                            }
                        }
                    } else {
                        int count = ps.executeUpdate();
                        countExecute();
                        if (changeTokenEnabled && hasConditions) {
                            if (count != Statement.SUCCESS_NO_INFO && count != 1) {
                                Serializable id = rowu.row.id;
                                logger.log("  -> CONCURRENT UPDATE: " + id);
                                throw new ConcurrentUpdateException(id.toString());
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                checkConcurrentUpdate(e);
                throw new NuxeoException("Could not update: " + update.sql, e);
            }
        }
    }

    protected void updateCollectionRows(String tableName, List<RowUpdate> rowus) {
        Set<Serializable> deleteIds = new HashSet<>();
        for (RowUpdate rowu : rowus) {
            if (rowu.pos == -1 || collectionDeleteBeforeAppend) {
                deleteIds.add(rowu.row.id);
            }
        }
        deleteRows(tableName, deleteIds);
        insertCollectionRows(tableName, rowus);
    }

    /**
     * Deletes multiple rows, all for the same table.
     */
    protected void deleteRows(String tableName, Set<Serializable> ids) {
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
                List<Serializable> chunkIds = new ArrayList<Serializable>(idList.subList(start, end));
                deleteRowsDirect(tableName, chunkIds);
            }
        } else {
            deleteRowsDirect(tableName, ids);
        }
    }

    protected void deleteRowsSoft(List<NodeInfo> nodeInfos) {
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
        } catch (SQLException e) {
            throw new NuxeoException("Could not soft delete", e);
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
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            setToPreparedStatementIdArray(ps, 1, whereIds);
            dialect.setToPreparedStatementTimestamp(ps, 2, now, null);
            ps.execute();
            countExecute();
            return;
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

    protected void setToPreparedStatementIdArray(PreparedStatement ps, int index, Serializable idArray)
            throws SQLException {
        if (idArray instanceof String) {
            ps.setString(index, (String) idArray);
        } else {
            Array array = dialect.createArrayOf(Types.OTHER, (Object[]) idArray, connection);
            ps.setArray(index, array);
        }
    }

    /**
     * Clean up soft-deleted rows.
     * <p>
     * Rows deleted more recently than the beforeTime are left alone. Only a limited number of rows may be deleted, to
     * prevent transaction during too long.
     *
     * @param max the maximum number of rows to delete at a time
     * @param beforeTime the maximum deletion time of the rows to delete
     * @return the number of rows deleted
     */
    public int cleanupDeletedRows(int max, Calendar beforeTime) {
        if (max < 0) {
            max = 0;
        }
        String sql = sqlInfo.getSoftDeleteCleanupSql();
        if (logger.isLogEnabled()) {
            logger.logSQL(sql, Arrays.<Serializable> asList(beforeTime, Long.valueOf(max)));
        }
        try {
            if (sql.startsWith("{")) {
                // callable statement
                boolean outFirst = sql.startsWith("{?=");
                int outIndex = outFirst ? 1 : 3;
                int inIndex = outFirst ? 2 : 1;
                try (CallableStatement cs = connection.prepareCall(sql)) {
                    cs.setInt(inIndex, max);
                    dialect.setToPreparedStatementTimestamp(cs, inIndex + 1, beforeTime, null);
                    cs.registerOutParameter(outIndex, Types.INTEGER);
                    cs.execute();
                    int count = cs.getInt(outIndex);
                    logger.logCount(count);
                    return count;
                }
            } else {
                // standard prepared statement with result set
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setInt(1, max);
                    dialect.setToPreparedStatementTimestamp(ps, 2, beforeTime, null);
                    try (ResultSet rs = ps.executeQuery()) {
                        countExecute();
                        if (!rs.next()) {
                            throw new NuxeoException("Cannot get result");
                        }
                        int count = rs.getInt(1);
                        logger.logCount(count);
                        return count;
                    }
                }
            }
        } catch (SQLException e) {
            throw new NuxeoException("Could not purge soft delete", e);
        }
    }

    protected void deleteRowsDirect(String tableName, Collection<Serializable> ids) {
        String sql = sqlInfo.getDeleteSql(tableName, ids.size());
        if (logger.isLogEnabled()) {
            logger.logSQL(sql, ids);
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int i = 1;
            for (Serializable id : ids) {
                dialect.setId(ps, i++, id);
            }
            int count = ps.executeUpdate();
            countExecute();
            logger.logCount(count);
        } catch (SQLException e) {
            checkConcurrentUpdate(e);
            throw new NuxeoException("Could not delete: " + tableName, e);
        }
    }

    @Override
    public Row readSimpleRow(RowId rowId) {
        SQLInfoSelect select = sqlInfo.selectFragmentById.get(rowId.tableName);
        Map<String, Serializable> criteriaMap = Collections.singletonMap(Model.MAIN_KEY, rowId.id);
        List<Row> maps = getSelectRows(rowId.tableName, select, criteriaMap, null, true);
        return maps.isEmpty() ? null : maps.get(0);
    }

    @Override
    public Map<String, String> getBinaryFulltext(RowId rowId) {
        ArrayList<String> columns = new ArrayList<String>();
        for (String index : model.getFulltextConfiguration().indexesAllBinary) {
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
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            dialect.setId(ps, 1, id);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    for (int i = 1; i <= columns.size(); i++) {
                        ret.put(columns.get(i - 1), rs.getString(i));
                    }
                }
                if (logger.isLogEnabled()) {
                    logger.log("  -> " + ret);
                }
            }
            return ret;
        } catch (SQLException e) {
            throw new NuxeoException("Could not select: " + sql, e);
        }
    }

    @Override
    public Serializable[] readCollectionRowArray(RowId rowId) {
        String tableName = rowId.tableName;
        Serializable id = rowId.id;
        String sql = sqlInfo.selectFragmentById.get(tableName).sql;
        if (logger.isLogEnabled()) {
            logger.logSQL(sql, Collections.singletonList(id));
        }
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            List<Column> columns = sqlInfo.selectFragmentById.get(tableName).whatColumns;
            dialect.setId(ps, 1, id); // assumes only one primary column
            try (ResultSet rs = ps.executeQuery()) {
                countExecute();

                // construct the resulting collection using each row
                CollectionIO io = getCollectionIO(tableName);
                List<Serializable> list = new ArrayList<Serializable>();
                Serializable[] returnId = new Serializable[1];
                int[] returnPos = { -1 };
                while (rs.next()) {
                    list.add(io.getCurrentFromResultSet(rs, columns, model, returnId, returnPos));
                }
                PropertyType type = model.getCollectionFragmentType(tableName).getArrayBaseType();
                Serializable[] array = type.collectionToArray(list);

                if (logger.isLogEnabled()) {
                    logger.log("  -> " + Arrays.asList(array));
                }
                return array;
            }
        } catch (SQLException e) {
            throw new NuxeoException("Could not select: " + sql, e);
        }
    }

    @Override
    public List<Row> readSelectionRows(SelectionType selType, Serializable selId, Serializable filter,
            Serializable criterion, boolean limitToOne) {
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
        return getSelectRows(selType.tableName, select, criteriaMap, null, limitToOne);
    }

    @Override
    public Set<Serializable> readSelectionsIds(SelectionType selType, List<Serializable> values) {
        SQLInfoSelection selInfo = sqlInfo.getSelection(selType);
        Map<String, Serializable> criteriaMap = new HashMap<String, Serializable>();
        Set<Serializable> ids = new HashSet<>();
        int size = values.size();
        int chunkSize = sqlInfo.getMaximumArgsForIn();
        if (size > chunkSize) {
            for (int start = 0; start < size; start += chunkSize) {
                int end = start + chunkSize;
                if (end > size) {
                    end = size;
                }
                // needs to be Serializable -> copy
                List<Serializable> chunkTodo = new ArrayList<Serializable>(values.subList(start, end));
                criteriaMap.put(selType.selKey, (Serializable) chunkTodo);
                SQLInfoSelect select = selInfo.getSelectSelectionIds(chunkTodo.size());
                List<Row> rows = getSelectRows(selType.tableName, select, criteriaMap, null, false);
                rows.forEach(row -> ids.add(row.id));
            }
        } else {
            criteriaMap.put(selType.selKey, (Serializable) values);
            SQLInfoSelect select = selInfo.getSelectSelectionIds(values.size());
            List<Row> rows = getSelectRows(selType.tableName, select, criteriaMap, null, false);
            rows.forEach(row -> ids.add(row.id));
        }
        return ids;
    }

    @Override
    public CopyResult copy(IdWithTypes source, Serializable destParentId, String destName, Row overwriteRow) {
        // assert !model.separateMainTable; // other case not implemented
        Invalidations invalidations = new Invalidations();
        try {
            Map<Serializable, Serializable> idMap = new LinkedHashMap<Serializable, Serializable>();
            Map<Serializable, IdWithTypes> idToTypes = new HashMap<Serializable, IdWithTypes>();
            // copy the hierarchy fragments recursively
            Serializable overwriteId = overwriteRow == null ? null : overwriteRow.id;
            if (overwriteId != null) {
                // overwrite hier root with explicit values
                String tableName = Model.HIER_TABLE_NAME;
                updateSimpleRowWithValues(tableName, overwriteRow);
                idMap.put(source.id, overwriteId);
                // invalidate
                invalidations.addModified(new RowId(tableName, overwriteId));
            }
            // create the new hierarchy by copy
            boolean resetVersion = destParentId != null;
            Serializable newRootId = copyHierRecursive(source, destParentId, destName, overwriteId, resetVersion,
                    idMap, idToTypes);
            // invalidate children
            Serializable invalParentId = overwriteId == null ? destParentId : overwriteId;
            if (invalParentId != null) { // null for a new version
                invalidations.addModified(new RowId(Invalidations.PARENT, invalParentId));
            }
            // copy all collected fragments
            Set<Serializable> proxyIds = new HashSet<Serializable>();
            for (Entry<String, Set<Serializable>> entry : model.getPerFragmentIds(idToTypes).entrySet()) {
                String tableName = entry.getKey();
                if (tableName.equals(Model.HIER_TABLE_NAME)) {
                    // already done
                    continue;
                }
                if (tableName.equals(Model.VERSION_TABLE_NAME)) {
                    // versions not fileable
                    // restore must not copy versions either
                    continue;
                }
                Set<Serializable> ids = entry.getValue();
                if (tableName.equals(Model.PROXY_TABLE_NAME)) {
                    for (Serializable id : ids) {
                        proxyIds.add(idMap.get(id)); // copied ids
                    }
                }
                Boolean invalidation = copyRows(tableName, ids, idMap, overwriteId);
                if (invalidation != null) {
                    // overwrote something
                    // make sure things are properly invalidated in this and
                    // other sessions
                    if (Boolean.TRUE.equals(invalidation)) {
                        invalidations.addModified(new RowId(tableName, overwriteId));
                    } else {
                        invalidations.addDeleted(new RowId(tableName, overwriteId));
                    }
                }
            }
            return new CopyResult(newRootId, invalidations, proxyIds);
        } catch (SQLException e) {
            throw new NuxeoException("Could not copy: " + source.id.toString(), e);
        }
    }

    /**
     * Updates a row in the database with given explicit values.
     */
    protected void updateSimpleRowWithValues(String tableName, Row row) {
        Update update = sqlInfo.getUpdateByIdForKeys(tableName, row.getKeys());
        Table table = update.getTable();
        String sql = update.getStatement();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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
        } catch (SQLException e) {
            throw new NuxeoException("Could not update: " + sql, e);
        }
    }

    /**
     * Copies hierarchy from id to parentId, and recurses.
     * <p>
     * If name is {@code null}, then the original name is kept.
     * <p>
     * {@code idMap} is filled with info about the correspondence between original and copied ids. {@code idType} is
     * filled with the type of each (source) fragment.
     * <p>
     * TODO: this should be optimized to use a stored procedure.
     *
     * @param overwriteId when not {@code null}, the copy is done onto this existing node (skipped)
     * @return the new root id
     */
    protected Serializable copyHierRecursive(IdWithTypes source, Serializable parentId, String name,
            Serializable overwriteId, boolean resetVersion, Map<Serializable, Serializable> idMap,
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
     * {@code idMap} is filled with info about the correspondence between original and copied ids. {@code idType} is
     * filled with the type of each (source) fragment.
     *
     * @return the new id
     */
    protected Serializable copyHier(Serializable id, Serializable parentId, String name, boolean resetVersion,
            Map<Serializable, Serializable> idMap) throws SQLException {
        boolean explicitName = name != null;

        SQLInfoSelect copy = sqlInfo.getCopyHier(explicitName, resetVersion);
        try (PreparedStatement ps = connection.prepareStatement(copy.sql)) {
            Serializable newId = generateNewId();

            List<Serializable> debugValues = null;
            if (logger.isLogEnabled()) {
                debugValues = new ArrayList<Serializable>(4);
            }
            int i = 1;
            for (Column column : copy.whatColumns) {
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
                } else if (key.equals(Model.MAIN_BASE_VERSION_KEY) || key.equals(Model.MAIN_CHECKED_IN_KEY)) {
                    v = null;
                } else if (key.equals(Model.MAIN_MINOR_VERSION_KEY) || key.equals(Model.MAIN_MAJOR_VERSION_KEY)) {
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

            // TODO DB_IDENTITY
            // post insert fetch idrow

            idMap.put(id, newId);
            return newId;
        }
    }

    /**
     * Gets the children ids and types of a node.
     */
    protected List<IdWithTypes> getChildrenIdsWithTypes(Serializable id, boolean onlyComplex) throws SQLException {
        List<IdWithTypes> children = new LinkedList<IdWithTypes>();
        String sql = sqlInfo.getSelectChildrenIdsAndTypesSql(onlyComplex);
        if (logger.isLogEnabled()) {
            logger.logSQL(sql, Collections.singletonList(id));
        }
        List<Column> columns = sqlInfo.getSelectChildrenIdsAndTypesWhatColumns();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            List<String> debugValues = null;
            if (logger.isLogEnabled()) {
                debugValues = new LinkedList<String>();
            }
            dialect.setId(ps, 1, id); // parent id
            try (ResultSet rs = ps.executeQuery()) {
                countExecute();
                while (rs.next()) {
                    Serializable childId = null;
                    String childPrimaryType = null;
                    String[] childMixinTypes = null;
                    int i = 1;
                    for (Column column : columns) {
                        String key = column.getKey();
                        Serializable value = column.getFromResultSet(rs, i++);
                        if (key.equals(Model.MAIN_KEY)) {
                            childId = value;
                        } else if (key.equals(Model.MAIN_PRIMARY_TYPE_KEY)) {
                            childPrimaryType = (String) value;
                        } else if (key.equals(Model.MAIN_MIXIN_TYPES_KEY)) {
                            childMixinTypes = (String[]) value;
                        }
                    }
                    children.add(new IdWithTypes(childId, childPrimaryType, childMixinTypes));
                    if (debugValues != null) {
                        debugValues.add(childId + "/" + childPrimaryType + "/" + Arrays.toString(childMixinTypes));
                    }
                }
            }
            if (debugValues != null) {
                logger.log("  -> " + debugValues);
            }
            return children;
        }
    }

    /**
     * Copy the rows from tableName with given ids into new ones with new ids given by idMap.
     * <p>
     * A new row with id {@code overwriteId} is first deleted.
     *
     * @return {@link Boolean#TRUE} for a modification or creation, {@link Boolean#FALSE} for a deletion, {@code null}
     *         otherwise (still absent)
     * @throws SQLException
     */
    protected Boolean copyRows(String tableName, Set<Serializable> ids, Map<Serializable, Serializable> idMap,
            Serializable overwriteId) throws SQLException {
        String copySql = sqlInfo.getCopySql(tableName);
        Column copyIdColumn = sqlInfo.getCopyIdColumn(tableName);
        String deleteSql = sqlInfo.getDeleteSql(tableName);
        try (PreparedStatement copyPs = connection.prepareStatement(copySql);
                PreparedStatement deletePs = connection.prepareStatement(deleteSql)) {
            boolean before = false;
            boolean after = false;
            for (Serializable id : ids) {
                Serializable newId = idMap.get(id);
                boolean overwrite = newId.equals(overwriteId);
                if (overwrite) {
                    // remove existing first
                    if (logger.isLogEnabled()) {
                        logger.logSQL(deleteSql, Collections.singletonList(newId));
                    }
                    dialect.setId(deletePs, 1, newId);
                    int delCount = deletePs.executeUpdate();
                    countExecute();
                    before = delCount > 0;
                }
                copyIdColumn.setToPreparedStatement(copyPs, 1, newId);
                copyIdColumn.setToPreparedStatement(copyPs, 2, id);
                if (logger.isLogEnabled()) {
                    logger.logSQL(copySql, Arrays.asList(newId, id));
                }
                int copyCount = copyPs.executeUpdate();
                countExecute();
                if (overwrite) {
                    after = copyCount > 0;
                }
            }
            // * , n -> mod (TRUE)
            // n , 0 -> del (FALSE)
            // 0 , 0 -> null
            return after ? Boolean.TRUE : (before ? Boolean.FALSE : null);
        }
    }

    @Override
    public void remove(Serializable rootId, List<NodeInfo> nodeInfos) {
        if (sqlInfo.softDeleteEnabled) {
            deleteRowsSoft(nodeInfos);
        } else {
            deleteRowsDirect(Model.HIER_TABLE_NAME, Collections.singleton(rootId));
        }
    }

    @Override
    public List<NodeInfo> getDescendantsInfo(Serializable rootId) {
        if (!dialect.supportsFastDescendants()) {
            return getDescendantsInfoIterative(rootId);
        }
        List<NodeInfo> descendants = new LinkedList<NodeInfo>();
        String sql = sqlInfo.getSelectDescendantsInfoSql();
        if (logger.isLogEnabled()) {
            logger.logSQL(sql, Collections.singletonList(rootId));
        }
        List<Column> columns = sqlInfo.getSelectDescendantsInfoWhatColumns();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            List<String> debugValues = null;
            if (logger.isLogEnabled()) {
                debugValues = new LinkedList<String>();
            }
            dialect.setId(ps, 1, rootId); // parent id
            try (ResultSet rs = ps.executeQuery()) {
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
                        if (key.equals(Model.MAIN_KEY)) {
                            id = value;
                        } else if (key.equals(Model.HIER_PARENT_KEY)) {
                            parentId = value;
                        } else if (key.equals(Model.MAIN_PRIMARY_TYPE_KEY)) {
                            primaryType = (String) value;
                        } else if (key.equals(Model.HIER_CHILD_ISPROPERTY_KEY)) {
                            isProperty = (Boolean) value;
                        } else if (key.equals(Model.PROXY_TARGET_KEY)) {
                            targetId = value;
                        } else if (key.equals(Model.PROXY_VERSIONABLE_KEY)) {
                            versionableId = value;
                        }
                        // no mixins (not useful to caller)
                        // no versions (not fileable)
                    }
                    descendants.add(new NodeInfo(id, parentId, primaryType, isProperty, versionableId, targetId));
                    if (debugValues != null) {
                        if (debugValues.size() < DEBUG_MAX_TREE) {
                            debugValues.add(id + "/" + primaryType);
                        }
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
        } catch (SQLException e) {
            throw new NuxeoException("Failed to get descendants", e);
        }
    }

    protected List<NodeInfo> getDescendantsInfoIterative(Serializable rootId) {
        Set<Serializable> done = new HashSet<>();
        List<Serializable> todo = new ArrayList<>(Collections.singleton(rootId));
        List<NodeInfo> descendants = new ArrayList<NodeInfo>();
        while (!todo.isEmpty()) {
            List<NodeInfo> infos;
            int size = todo.size();
            int chunkSize = sqlInfo.getMaximumArgsForIn();
            if (size > chunkSize) {
                infos = new ArrayList<>();
                for (int start = 0; start < size; start += chunkSize) {
                    int end = start + chunkSize;
                    if (end > size) {
                        end = size;
                    }
                    // needs to be Serializable -> copy
                    List<Serializable> chunkTodo = new ArrayList<Serializable>(todo.subList(start, end));
                    List<NodeInfo> chunkInfos = getChildrenNodeInfos(chunkTodo);
                    infos.addAll(chunkInfos);
                }
            } else {
                infos = getChildrenNodeInfos(todo);
            }
            todo = new ArrayList<>();
            for (NodeInfo info : infos) {
                Serializable id = info.id;
                if (!done.add(id)) {
                    continue;
                }
                todo.add(id);
                descendants.add(info);
            }
        }
        return descendants;
    }

    /**
     * Gets the children of a node as a list of NodeInfo.
     */
    protected List<NodeInfo> getChildrenNodeInfos(Collection<Serializable> ids) {
        List<NodeInfo> children = new LinkedList<NodeInfo>();
        SQLInfoSelect select = sqlInfo.getSelectChildrenNodeInfos(ids.size());
        if (logger.isLogEnabled()) {
            logger.logSQL(select.sql, ids);
        }
        Column where = select.whereColumns.get(0);
        try (PreparedStatement ps = connection.prepareStatement(select.sql)) {
            List<String> debugValues = null;
            if (logger.isLogEnabled()) {
                debugValues = new LinkedList<String>();
            }
            int ii = 1;
            for (Serializable id : ids) {
                where.setToPreparedStatement(ps, ii++, id);
            }
            try (ResultSet
            rs = ps.executeQuery()) {
                countExecute();
                while (rs.next()) {
                    Serializable id = null;
                    Serializable parentId = null;
                    String primaryType = null;
                    Boolean isProperty = Boolean.FALSE;
                    Serializable targetId = null;
                    Serializable versionableId = null;
                    int i = 1;
                    for (Column column : select.whatColumns) {
                        String key = column.getKey();
                        Serializable value = column.getFromResultSet(rs, i++);
                        if (key.equals(Model.MAIN_KEY)) {
                            id = value;
                        } else if (key.equals(Model.HIER_PARENT_KEY)) {
                            parentId = value;
                        } else if (key.equals(Model.MAIN_PRIMARY_TYPE_KEY)) {
                            primaryType = (String) value;
                        } else if (key.equals(Model.PROXY_TARGET_KEY)) {
                            targetId = value;
                        } else if (key.equals(Model.PROXY_VERSIONABLE_KEY)) {
                            versionableId = value;
                        }
                    }
                    children.add(new NodeInfo(id, parentId, primaryType, isProperty, versionableId, targetId));
                    if (debugValues != null) {
                        if (debugValues.size() < DEBUG_MAX_TREE) {
                            debugValues.add(id + "/" + primaryType);
                        }
                    }
                }
            }
            if (debugValues != null) {
                if (debugValues.size() >= DEBUG_MAX_TREE) {
                    debugValues.add("... (" + children.size() + ") results");
                }
                logger.log("  -> " + debugValues);
            }
            return children;
        } catch (SQLException e) {
            throw new NuxeoException("Failed to get descendants", e);
        }
    }

}
