/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * This class holds persistence context information for one table.
 * <p>
 * All non-saved modified data is referenced here. At save time, the data is
 * sent to the database by the {@link Mapper}. The database will at some time
 * later be committed by the external transaction manager in effect.
 * <p>
 * Internally a row can be in at most one of the caches: pristine, created,
 * modified, deleted. The pristine cache survives a save(), and may be partially
 * invalidated after commit by other local or clustered contexts that committed
 * too.
 * <p>
 * This class is not thread-safe, it should be tied to a single session and the
 * session itself should not be used concurrently.
 *
 * @author Florent Guillaume
 */
public class PersistenceContextByTable {

    private static final Log log = LogFactory.getLog(PersistenceContextByTable.class);

    private final String tableName;

    private final Mapper mapper;

    /**
     * The pristine fragments. All held data is identical to what is present in
     * the database and could be refetched if needed. This cache is also managed
     * externally.
     * <p>
     * Upon session save, this cache is updated with new pristine fragments.
     * After a commit, the data in this cache may be invalidated according to
     * the other caches (which may be clustered), and conversely changes to this
     * cache invalidate other caches.
     * <p>
     * If an external process modifies the database directly, it has to notify
     * the clustering mechanism (TODO) so that this cache can be invalidated
     * appropriately.
     * <p>
     * This cache is memory-sensitive, a fragment can always be refetched if the
     * GC collects it.
     */
    private final Map<Serializable, Fragment> pristine;

    /** The created fragments. Their id is a temporary one. */
    private final Map<Serializable, Fragment> created;

    /** The modified fragments. */
    private final Map<Serializable, Fragment> modified;

    /** The deleted fragments. */
    private final Map<Serializable, Fragment> deleted;

    private final Model model;

    /* TODO collections too */

    @SuppressWarnings("unchecked")
    PersistenceContextByTable(String schemaName, Mapper mapper) {
        this.tableName = schemaName;
        this.mapper = mapper;
        model = mapper.getModel();
        pristine = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        // linked map to keep ids in the same order as creation (cosmetic)
        created = new LinkedHashMap<Serializable, Fragment>();
        modified = new HashMap<Serializable, Fragment>();
        deleted = new HashMap<Serializable, Fragment>();
    }

    /**
     * Closes the context. Keeps around the pristine cache (to avoid costly
     * refills). This cache is nevertheless still invalidatable.
     */
    public void close() {
        detachAll();
    }

    private void detachAll() {
        for (Fragment fragment : created.values()) {
            fragment.detach();
        }
        created.clear();
        for (Fragment fragment : modified.values()) {
            fragment.detach();
        }
        modified.clear();
        for (Fragment fragment : deleted.values()) {
            fragment.detach();
        }
        deleted.clear();
    }

    /**
     * Creates a new row in the context.
     *
     * @param id the id
     * @param map the fragments map, or {@code null}
     * @return the created row
     * @throws StorageException if the row is already in the context
     */
    public Fragment create(Serializable id, Map<String, Serializable> map)
            throws StorageException {
        if (pristine.containsKey(id) || created.containsKey(id) ||
                modified.containsKey(id) || deleted.containsKey(id)) {
            throw new StorageException("Row already registered: " + id);
        }
        Fragment row = new SingleRow(tableName, id, this, true, map);
        created.put(id, row);
        return row;
    }

    /**
     * Gets a fragment. If it's not in the context, fetch it from the mapper.
     * <p>
     * XXX differentiate unknown vs deleted
     *
     * @param id the fragment id
     * @return the fragment, or {@code null} if none is found
     * @throws StorageException
     */
    public Fragment get(Serializable id) throws StorageException {
        // TODO check the row state to find in which map it is
        Fragment fragment;
        fragment = modified.get(id);
        if (fragment != null) {
            return fragment;
        }
        fragment = created.get(id);
        if (fragment != null) {
            return fragment;
        }
        if (deleted.containsKey(id)) {
            return null; // XXX
        }
        fragment = pristine.get(id);
        if (fragment != null) {
            return fragment;
        }
        // read it through the mapper
        boolean isTemporaryId = id instanceof String &&
                ((String) id).startsWith("T");
        boolean isCollection = tableName.startsWith("ARRAY_"); // XXX hack
        if (isTemporaryId) {
            if (isCollection) {
                fragment = new CollectionRows(tableName, id, this, true,
                        new Serializable[] {});
                created.put(id, fragment);
            } else {
                fragment = null;
            }
        } else {
            if (isCollection) {
                fragment = mapper.readCollectionRows(tableName, id, this);
            } else {
                fragment = mapper.readSingleRow(tableName, id, this);
            }
            if (fragment != null) {
                pristine.put(id, fragment);
            }
        }
        return fragment;
    }

    /**
     * Find a row in the hierarchy schema given its parent id and name. If the
     * row is not in the context, fetch it from the mapper.
     * <p>
     * TODO optimize this simplistic scanning implementation.
     * <p>
     * TODO check that this is coherent when (if) we rename nodes.
     *
     * @param parentId the parent id
     * @param name the name
     * @return the row, or {@code null} if none is found
     * @throws StorageException
     */
    public SingleRow getByHier(Serializable parentId, String name)
            throws StorageException {
        SingleRow row;
        row = getByHierInMap(modified, parentId, name);
        if (row != null) {
            return row;
        }
        row = getByHierInMap(created, parentId, name);
        if (row != null) {
            return row;
        }
        row = getByHierInMap(pristine, parentId, name);
        if (row != null) {
            return row;
        }
        // read it through the mapper
        row = mapper.readChildHierRow(parentId, name, this);
        if (row != null) {
            pristine.put(row.getId(), row);
        }
        return row;
    }

    private static String PARENT = "parent"; // XXX hierarchyParentKey

    private static String NAME = "name"; // XXX hierarchyChildNameKey

    // TODO replace linear search with some kind of index
    private SingleRow getByHierInMap(Map<Serializable, Fragment> map,
            Serializable parentId, String name) {
        for (Fragment fragment : map.values()) {
            SingleRow row = (SingleRow) fragment;
            if (parentId.equals(row.get(PARENT)) && name.equals(row.get(NAME))) {
                return (SingleRow) row;
            }
        }
        return null;
    }

    /**
     * Removes a row from the context.
     *
     * @param row
     * @throws StorageException
     */
    public void remove(Fragment row) throws StorageException {
        // TODO check the row state to find in which map it is
        Serializable id = row.getId();
        Fragment oldRow;
        oldRow = pristine.remove(id);
        if (oldRow != null) {
            deleted.put(id, row);
            row.markDeleted();
            return;
        }
        oldRow = modified.remove(id);
        if (oldRow != null) {
            deleted.put(id, row);
            row.markDeleted();
            return;
        }
        oldRow = created.remove(id);
        if (oldRow != null) {
            row.markDeleted(); // also detached
            return;
        }
        if (deleted.containsKey(id)) {
            throw new StorageException("Already deleted: " + row);
        } else {
            throw new StorageException("Already detached: " + row);
        }
    }

    /**
     * Saves the created main rows, and returns the map of temporary ids to
     * final ids.
     *
     * @return the map of temporary ids to final ids
     * @throws StorageException
     */
    public Map<Serializable, Serializable> saveMain() throws StorageException {
        Map<Serializable, Serializable> idMap = new HashMap<Serializable, Serializable>();
        for (Fragment fragment : created.values()) {
            Serializable id = fragment.getId();
            Serializable newId = mapper.insertSingleRow((SingleRow) fragment);
            if (pristine.containsKey(id)) {
                throw new StorageException("Row already in cache: " + fragment);
            }
            fragment.markPristine();
            pristine.put(newId, fragment);
            idMap.put(id, newId);
            // log.debug("mapping temporary id " + id + " to " + newId);
        }
        created.clear();
        return idMap;
    }

    /**
     * Saves all the created, modified or deleted rows, except for the created
     * main rows which have already been done.
     *
     * @param idMap the map of temporary ids to final ids to use in translating
     *            secondary created rows
     * @throws StorageException
     */
    public void save(Map<Serializable, Serializable> idMap)
            throws StorageException {
        if (!tableName.equals(model.MAIN_TABLE_NAME)) {
            for (Fragment fragment : created.values()) {
                /*
                 * Map temporary to persistent ids.
                 */
                Serializable id = fragment.getId();
                Serializable newId = idMap.get(id);
                if (newId != null) {
                    fragment.setId(newId);
                    id = newId;
                }
                if (tableName.equals(model.HIER_TABLE_NAME)) {
                    // The hierarchy table stores a foreign key to id in the
                    // parent column, it has to be mapped too.
                    SingleRow row = (SingleRow) fragment;
                    Serializable newParentId = idMap.get(row.get(model.HIER_PARENT_KEY));
                    if (newParentId != null) {
                        row.put(model.HIER_PARENT_KEY, newParentId);
                    }
                }
                if (fragment instanceof SingleRow) {
                    mapper.insertSingleRow((SingleRow) fragment);
                } else {
                    mapper.insertCollectionRows((CollectionRows) fragment);
                }
                if (pristine.containsKey(id)) {
                    throw new StorageException("Row already in cache: " +
                            fragment);
                }
                fragment.markPristine();
                pristine.put(id, fragment);
            }
            created.clear();
        }
        for (Fragment fragment : modified.values()) {
            if (fragment instanceof SingleRow) {
                mapper.updateSingleRow((SingleRow) fragment);
            } else {
                mapper.updateCollectionRows((CollectionRows) fragment);
            }
            fragment.markPristine();
        }
        modified.clear();
        for (Fragment fragment : deleted.values()) {
            mapper.deleteFragment(fragment);
            fragment.detach();
        }
        deleted.clear();
    }

    /**
     * Callback from a {@link Fragment}, used when the row changes from the
     * pristine to the modified state.
     * <p>
     * This context then moves the row from the pristine to the modified map.
     *
     * @param dataRow
     */
    protected void markModified(Fragment row) {
        Serializable id = row.getId();
        Fragment old = pristine.remove(id);
        if (old == null) {
            log.error("Should be pristine: " + row);
        }
        old = modified.put(id, row);
        if (old != null) {
            log.error("Should not be modified: " + row);
        }
    }

}
