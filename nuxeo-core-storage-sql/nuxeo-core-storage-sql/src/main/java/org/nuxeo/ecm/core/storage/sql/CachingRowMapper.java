/*
 * (C) Copyright 2008-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.apache.commons.collections.map.ReferenceMap;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.ACLRow.ACLRowPositionComparator;

/**
 * A {@link RowMapper} that has an internal cache.
 * <p>
 * The cache only holds {@link Row}s that are known to be identical to what's in
 * the underlying {@link RowMapper}.
 */
public class CachingRowMapper implements RowMapper {

    private static final String ABSENT = "__ABSENT__\0\0\0";

    /**
     * The cached rows. All held data is identical to what is present in the
     * underlying {@link RowMapper} and could be refetched if needed.
     * <p>
     * The values are either {@link Row} for fragments present in the database,
     * or a row with tableName {@link #ABSENT} to denote a fragment known to be
     * absent from the database.
     * <p>
     * This cache is memory-sensitive (all values are soft-referenced), a
     * fragment can always be refetched if the GC collects it.
     */
    // we use a new Row instance for the absent case to avoid keeping other
    // references to it which would prevent its GCing
    private final Map<RowId, Row> cache;

    /**
     * The {@link RowMapper} to which operations that cannot be processed from
     * the cache are delegated.
     */
    private final RowMapper rowMapper;

    @SuppressWarnings("unchecked")
    public CachingRowMapper(RowMapper rowMapper) {
        this.rowMapper = rowMapper;
        cache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
    }

    /*
     * ----- Cache -----
     */

    protected static boolean isAbsent(Row row) {
        return row.tableName == ABSENT; // == is ok
    }

    protected void cachePut(Row row) {
        row = row.clone();
        // for ACL collections, make sure the order is correct
        // (without the cache, the query to get a list of collection does an
        // ORDER BY pos, so users of the cache must get the same behavior)
        if (row.isCollection() && row.values.length > 0
                && row.values[0] instanceof ACLRow) {
            row.values = sortACLRows((ACLRow[]) row.values);
        }
        cache.put(new RowId(row), row);
    }

    protected ACLRow[] sortACLRows(ACLRow[] acls) {
        List<ACLRow> list = new ArrayList<ACLRow>(Arrays.asList(acls));
        Collections.sort(list, ACLRowPositionComparator.INSTANCE);
        ACLRow[] res = new ACLRow[acls.length];
        return list.toArray(res);
    }

    protected void cachePutAbsent(RowId rowId) {
        cache.put(new RowId(rowId), new Row(ABSENT, (Serializable) null));
    }

    protected void cachePutAbsentIfNull(RowId rowId, Row row) {
        if (row != null) {
            cachePut(row);
        } else {
            cachePutAbsent(rowId);
        }
    }

    protected void cachePutAbsentIfRowId(RowId rowId) {
        if (rowId instanceof Row) {
            cachePut((Row) rowId);
        } else {
            cachePutAbsent(rowId);
        }
    }

    protected Row cacheGet(RowId rowId) {
        Row row = cache.get(rowId);
        if (row != null && !isAbsent(row)) {
            row = row.clone();
        }
        return row;
    }

    protected void cacheRemove(RowId rowId) {
        cache.remove(rowId);
    }

    /*
     * ----- Cache Management -----
     */

    public void invalidateCache(Invalidations invalidations) {
        if (invalidations.modified != null) {
            for (RowId rowId : invalidations.modified) {
                cacheRemove(rowId);
            }
        }
        if (invalidations.deleted != null) {
            for (RowId rowId : invalidations.deleted) {
                cachePutAbsent(rowId);
            }
        }
        rowMapper.invalidateCache(invalidations);
    }

    public void clearCache() {
        cache.clear();
        rowMapper.clearCache();
    }

    public void rollback(Xid xid) throws XAException {
        cache.clear();
        rowMapper.rollback(xid);
    }

    /*
     * ----- Batch -----
     */

    /*
     * Use those from the cache if available, read from the mapper for the rest.
     */
    public List<? extends RowId> read(Collection<RowId> rowIds)
            throws StorageException {
        List<RowId> res = new ArrayList<RowId>(rowIds.size());
        // find which are in cache, and which not
        List<RowId> todo = new LinkedList<RowId>();
        for (RowId rowId : rowIds) {
            Row row = cacheGet(rowId);
            if (row == null) {
                todo.add(rowId);
            } else if (isAbsent(row)) {
                res.add(new RowId(rowId));
            } else {
                res.add(row);
            }
        }
        // ask missing ones to underlying row mapper
        List<? extends RowId> fetched = rowMapper.read(todo);
        // add them to the cache
        for (RowId rowId : fetched) {
            cachePutAbsentIfRowId(rowId);
        }
        // merge results
        res.addAll(fetched);
        return res;
    }

    /*
     * Save in the cache then pass all the writes to the mapper.
     */
    public void write(RowBatch batch) throws StorageException {
        for (Row row : batch.creates) {
            cachePut(row);
        }
        for (RowUpdate rowu : batch.updates) {
            cachePut(rowu.row);
        }
        for (RowId rowId : batch.deletes) {
            if (rowId instanceof Row) {
                throw new AssertionError();
            }
            cachePutAbsent(rowId);
        }
        rowMapper.write(batch);
    }

    /*
     * ----- Read -----
     */

    public Row readSimpleRow(RowId rowId) throws StorageException {
        Row row = cacheGet(rowId);
        if (row == null) {
            row = rowMapper.readSimpleRow(rowId);
            cachePutAbsentIfNull(rowId, row);
            return row;
        } else if (isAbsent(row)) {
            return null;
        } else {
            return row;
        }
    }

    public Serializable[] readCollectionRowArray(RowId rowId)
            throws StorageException {
        Row row = cacheGet(rowId);
        if (row == null) {
            Serializable[] array = rowMapper.readCollectionRowArray(rowId);
            assert array != null;
            row = new Row(rowId.tableName, rowId.id, array);
            cachePut(row);
            return row.values;
        } else if (isAbsent(row)) {
            return null;
        } else {
            return row.values;
        }
    }

    // TODO this API isn't cached well...
    public Row readChildHierRow(Serializable parentId, String childName,
            boolean complexProp) throws StorageException {
        Row row = rowMapper.readChildHierRow(parentId, childName, complexProp);
        if (row != null) {
            cachePut(row);
        }
        return row;
    }

    // TODO this API isn't cached well...
    public List<Row> readChildHierRows(Serializable parentId,
            boolean complexProp) throws StorageException {
        List<Row> rows = rowMapper.readChildHierRows(parentId, complexProp);
        for (Row row : rows) {
            cachePut(row);
        }
        return rows;
    }

    // TODO this API isn't cached well...
    public List<Row> getVersionRows(Serializable versionableId)
            throws StorageException {
        List<Row> rows = rowMapper.getVersionRows(versionableId);
        for (Row row : rows) {
            cachePut(row);
        }
        return rows;
    }

    // TODO this API isn't cached well...
    public List<Row> getProxyRows(Serializable searchId, boolean byTarget,
            Serializable parentId) throws StorageException {
        List<Row> rows = rowMapper.getProxyRows(searchId, byTarget, parentId);
        for (Row row : rows) {
            cachePut(row);
        }
        return rows;
    }

    /*
     * ----- Copy -----
     */

    public CopyHierarchyResult copyHierarchy(Serializable sourceId,
            String typeName, Serializable destParentId, String destName,
            Row overwriteRow) throws StorageException {
        CopyHierarchyResult result = rowMapper.copyHierarchy(sourceId,
                typeName, destParentId, destName, overwriteRow);
        Invalidations invalidations = result.invalidations;
        if (invalidations.modified != null) {
            for (RowId rowId : invalidations.modified) {
                cacheRemove(rowId);
            }
        }
        if (invalidations.deleted != null) {
            for (RowId rowId : invalidations.deleted) {
                cacheRemove(rowId);
            }
        }
        return result;
    }

}
