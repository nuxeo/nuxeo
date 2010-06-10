/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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

import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Invalidations;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.PropertyType;
import org.nuxeo.ecm.core.storage.sql.Row;
import org.nuxeo.ecm.core.storage.sql.RowId;
import org.nuxeo.ecm.core.storage.sql.RowMapper;
import org.nuxeo.ecm.core.storage.sql.SimpleFragment;
import org.nuxeo.ecm.core.storage.sql.jdbc.SQLInfo.SQLInfoSelect;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Column;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Table;
import org.nuxeo.ecm.core.storage.sql.jdbc.db.Update;

/**
 * A {@link JDBCRowMapper} maps {@link Row}s to and from a JDBC database.
 */
public class JDBCRowMapper extends JDBCConnection implements RowMapper {

    public JDBCRowMapper(Model model, SQLInfo sqlInfo, XADataSource xadatasource)
            throws StorageException {
        super(model, sqlInfo, xadatasource);
    }

    public void invalidateCache(Invalidations invalidations) {
        // no cache
    }

    public void clearCache() {
        // no cache
    }

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

    /*
     * ----- RowIO -----
     */

    public List<? extends RowId> read(Collection<RowId> rowIds)
            throws StorageException {
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
        List<RowId> res = new ArrayList<RowId>(rowIds.size());
        for (Entry<String, Set<Serializable>> en : tableIds.entrySet()) {
            String tableName = en.getKey();
            Set<Serializable> ids = en.getValue();
            Collection<Row> rows;
            if (model.isCollectionFragment(tableName)) {
                rows = readCollectionArrays(tableName, ids);
            } else {
                rows = readSimpleRows(tableName, ids);
            }
            // check we have all the ids (readMultipleRows may have some
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
     * @param the rows, including those for empty collections
     */
    protected List<Row> readCollectionArrays(String tableName,
            Collection<Serializable> ids) throws StorageException {
        if (ids.isEmpty()) {
            return Collections.emptyList();
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
                PropertyType ftype = model.getCollectionFragmentType(tableName);
                PropertyType type = ftype.getArrayBaseType();
                Serializable curId = null;
                List<Serializable> list = null;
                Serializable[] returnId = new Serializable[1];
                int[] returnPos = new int[] { -1 };
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
                closePreparedStatement(ps);
            }
        } catch (SQLException e) {
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

            /*
             * Construct the maps from the result set.
             */
            while (rs.next()) {
                Row row = new Row(tableName, criteriaMap);
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
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

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
    }

    protected void writeCreates(List<Row> creates) throws StorageException {
        // reorganize by table
        Map<String, List<Row>> tableRows = new LinkedHashMap<String, List<Row>>();
        // hierarchy table first because there are foreign keys to it
        tableRows.put(model.hierTableName, new LinkedList<Row>());
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
                for (Row row : rows) {
                    // TODO optimize loop
                    insertCollectionRows(row);
                }
            } else {
                for (Row row : rows) {
                    // TODO optimize loop
                    insertSimpleRow(row);
                }
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
                for (RowUpdate rowu : rows) {
                    // TODO optimize loop
                    updateCollectionRows(rowu.row);
                }
            } else {
                for (RowUpdate rowu : rows) {
                    // TODO optimize loop
                    updateSimpleRow(rowu.row, rowu.keys);
                }
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
            for (Serializable id : ids) {
                // TODO optimize loop
                deleteRows(tableName, id);
            }
        }
    }

    protected void insertSimpleRow(Row row) throws StorageException {
        PreparedStatement ps = null;
        try {
            // insert the row
            // XXX statement should be already prepared
            String sql = sqlInfo.getInsertSql(row.tableName);
            if (sql == null) {
                throw new StorageException("Unknown table: " + row.tableName);
            }
            List<Column> columns = sqlInfo.getInsertColumns(row.tableName);
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
                    logger.error(e.getMessage(), e);
                }
            }
        }
        // return row.id;
    }

    protected void insertCollectionRows(Row row) throws StorageException {
        PreparedStatement ps = null;
        try {
            String sql = sqlInfo.getInsertSql(row.tableName);
            List<Column> columns = sqlInfo.getInsertColumns(row.tableName);
            try {
                List<Serializable> debugValues = null;
                if (logger.isLogEnabled()) {
                    debugValues = new ArrayList<Serializable>(3);
                }
                ps = connection.prepareStatement(sql);
                getCollectionIO(row.tableName).setToPreparedStatement(row.id,
                        row.values, columns, ps, model, debugValues, sql,
                        logger);
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
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    protected void updateSimpleRow(Row row, Collection<String> keys)
            throws StorageException {
        if (keys.isEmpty()) {
            return;
        }
        SQLInfoSelect update = sqlInfo.getUpdateById(row.tableName, keys);
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

    protected void updateCollectionRows(Row row) throws StorageException {
        deleteRows(row.tableName, row.id);
        insertCollectionRows(row);
    }

    protected void deleteRows(String tableName, Serializable id)
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

    public Row readSimpleRow(RowId rowId) throws StorageException {
        SQLInfoSelect select = sqlInfo.selectFragmentById.get(rowId.tableName);
        Map<String, Serializable> criteriaMap = Collections.singletonMap(
                model.MAIN_KEY, rowId.id);
        List<Row> maps = getSelectRows(rowId.tableName, select, criteriaMap,
                null, true);
        return maps == null ? null : maps.get(0);
    }

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
                ps.setObject(1, id); // assumes only one primary column
                ResultSet rs = ps.executeQuery();

                // construct the resulting collection using each row
                CollectionIO io = getCollectionIO(tableName);
                ArrayList<Serializable> list = new ArrayList<Serializable>();
                Serializable[] returnId = new Serializable[1];
                int[] returnPos = new int[] { -1 };
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
                closePreparedStatement(ps);
            }
        } catch (SQLException e) {
            checkConnectionReset(e);
            throw new StorageException("Could not select: " + sql, e);
        }
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
                Row row = new Row(model.hierTableName, (Serializable) null);
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
                    logger.error(String.format(
                            "Child '%s' appeared twice as child of %s "
                                    + "(%s and %s), renaming second to '%s'",
                            childName, parentId, row.id, childId, newName));
                    Row rename = new Row(model.hierTableName, childId);
                    rename.putNew(model.HIER_CHILD_NAME_KEY, newName);
                    updateSimpleRowWithValues(model.HIER_TABLE_NAME, rename);
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
        return getSelectRows(model.hierTableName, select, criteriaMap, null,
                false);
    }

    public List<Row> getVersionRows(Serializable versionableId)
            throws StorageException {
        SQLInfoSelect select = sqlInfo.selectVersionsByVersionable;
        Map<String, Serializable> criteriaMap = Collections.singletonMap(
                model.VERSION_VERSIONABLE_KEY, versionableId);
        return getSelectRows(model.VERSION_TABLE_NAME, select, criteriaMap,
                null, false);
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
        return getSelectRows(model.PROXY_TABLE_NAME, select, criteriaMap,
                joinMap, false);
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
                updateSimpleRowWithValues(tableName, overwriteRow);
                idMap.put(sourceId, overwriteId);
                // invalidate
                invalidations.addModified(new RowId(tableName, overwriteId));
            }
            // create the new hierarchy by copy
            Serializable newRootId = copyHierRecursive(sourceId, typeName,
                    destParentId, destName, overwriteId, idMap, idToType);
            // invalidate children
            Serializable invalParentId = overwriteId == null ? destParentId
                    : overwriteId;
            if (invalParentId != null) { // null for a new version
                invalidations.addModified(new RowId(Invalidations.PARENT,
                        invalParentId));
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
                        invalidations.addModified(Collections.singleton(new RowId(
                                tableName, overwriteId)));
                    } else {
                        invalidations.addDeleted(Collections.singleton(new RowId(
                                tableName, overwriteId)));
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

}
