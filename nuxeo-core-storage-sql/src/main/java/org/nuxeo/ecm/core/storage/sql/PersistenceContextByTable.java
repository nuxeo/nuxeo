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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
 * Internally a fragment can be in at most one of the caches: pristine, created,
 * modified, deleted. The pristine cache survives a save(), and may be partially
 * invalidated after commit by other local or clustered contexts that committed
 * too.
 * <p>
 * Depending on the table, the context may hold {@link SimpleFragment}s, which
 * represent one row, {@link CollectionFragment}s, which represent several rows,
 * or {@link HierarchyFragment}s, which holds children information for a parent.
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

    private final Model model;

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

    /** The children info, for the hierarchy table. */
    private final Map<Serializable, Children> knownChildren;

    @SuppressWarnings("unchecked")
    PersistenceContextByTable(String tableName, Mapper mapper) {
        this.tableName = tableName;
        this.mapper = mapper;
        model = mapper.getModel();
        pristine = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        // linked map to keep ids in the same order as creation (cosmetic)
        created = new LinkedHashMap<Serializable, Fragment>();
        modified = new HashMap<Serializable, Fragment>();
        deleted = new HashMap<Serializable, Fragment>();

        // TODO use a dedicated subclass for the hierarchy table
        if (tableName.equals(model.HIER_TABLE_NAME)) {
            knownChildren = new HashMap<Serializable, Children>();
        } else {
            knownChildren = null;
        }
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
     * @param id the temporary id
     * @param map the fragments map, or {@code null}
     * @return the created row
     * @throws StorageException if the row is already in the context
     */
    public SimpleFragment create(Serializable id, Map<String, Serializable> map)
            throws StorageException {
        if (pristine.containsKey(id) || created.containsKey(id) ||
                modified.containsKey(id) || deleted.containsKey(id)) {
            throw new StorageException("Row already registered: " + id);
        }
        SimpleFragment fragment = new SimpleFragment(tableName, id, this, true,
                map);
        if (knownChildren != null) {
            // add as a child of its parent
            addChild(fragment);
            // note that this new row doesn't have children
            addNewParent(id);
        }
        created.put(id, fragment);
        return fragment;
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
        if (isDeleted(id)) {
            return null; // XXX
        }
        Fragment fragment = getIfPresent(id);
        if (fragment != null) {
            return fragment;
        }

        // read it through the mapper
        boolean isTemporaryId = id instanceof String &&
                ((String) id).startsWith("T");
        boolean isCollection = tableName.startsWith("ARRAY_"); // XXX hack
        if (isTemporaryId) {
            if (isCollection) {
                fragment = new CollectionFragment(tableName, id, this, true,
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
                if (knownChildren != null) {
                    // add as a child of its parent
                    addChild((SimpleFragment) fragment);
                }
                pristine.put(id, fragment);
            }
        }
        return fragment;
    }

    /**
     * Checks if a fragment is deleted.
     */
    protected boolean isDeleted(Serializable id) {
        return deleted.containsKey(id);
    }

    /**
     * Gets a fragment. If it's not in the context, returns {@code null}.
     */
    protected Fragment getIfPresent(Serializable id) throws StorageException {
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
        return pristine.get(id);
    }

    /**
     * Called by the mapper when a new pristine hierarchy fragment for a child
     * is retrieved.
     */
    protected void newPristine(Fragment fragment) {
        pristine.put(fragment.getId(), fragment);
    }

    /**
     * Adds a row to the known children of its parent.
     */
    private void addChild(SimpleFragment row) throws StorageException {
        Serializable parentId = row.get(model.HIER_PARENT_KEY);
        Children children = knownChildren.get(parentId);
        if (children == null) {
            children = new Children(false);
            knownChildren.put(parentId, children);
        }
        children.add(row, row.getString(model.HIER_CHILD_NAME_KEY),
                (Long) row.get(model.HIER_CHILD_POS_KEY));
    }

    /**
     * Notes the fact that a new row was created without children.
     */
    private void addNewParent(Serializable parentId) {
        assert !knownChildren.containsKey(parentId);
        Children children = new Children(true); // complete
        knownChildren.put(parentId, children);
    }

    /**
     * Removes a row from the known children of its parent.
     */
    private void removeChild(SimpleFragment row) throws StorageException {
        Serializable parentId = row.get(model.HIER_PARENT_KEY);
        Children children = knownChildren.get(parentId);
        if (children != null) {
            children.remove(row.getString(model.HIER_CHILD_NAME_KEY));
        }
    }

    /**
     * Find a row in the hierarchy schema given its parent id and name. If the
     * row is not in the context, fetch it from the mapper.
     *
     * @param parentId the parent id
     * @param name the name
     * @return the row, or {@code null} if none is found
     * @throws StorageException
     */
    public SimpleFragment getByHier(Serializable parentId, String name)
            throws StorageException {
        SimpleFragment fragment;

        // check in the known children
        Children children = knownChildren.get(parentId);
        if (children != null) {
            fragment = children.get(name);
            if (fragment != SimpleFragment.UNKNOWN) {
                return fragment;
            }
        }

        // read it through the mapper
        fragment = mapper.readChildHierRow(parentId, name, this);

        // add as know child
        if (fragment != null) {
            addChild(fragment);
        }

        return fragment;
    }

    /**
     * Gets the list of children for a given parent id.
     *
     * @param parentId the parent id
     * @return the list of children.
     * @throws StorageException
     */
    public Collection<SimpleFragment> getHierChildren(Serializable parentId)
            throws StorageException {

        // check in the known children
        Children children = knownChildren.get(parentId);
        if (children != null) {
            if (children.isComplete()) {
                return children.getFragments();
            }
        } else {
            children = new Children(false);
            knownChildren.put(parentId, children);
        }

        // ask the children to the mapper
        Collection<SimpleFragment> fragments = mapper.readChildHierRows(
                parentId, this);

        // we now know the full children for this parent
        children.addComplete(fragments, model);

        // the children may include newly-created ones
        return children.getFragments();
    }

    /**
     * Removes a row from the context.
     *
     * @param fragment
     * @throws StorageException
     */
    public void remove(Fragment fragment) throws StorageException {
        if (knownChildren != null) {
            // remove from parent
            removeChild((SimpleFragment) fragment);
        }

        // TODO check the row state to find in which map it is
        Serializable id = fragment.getId();
        Fragment oldRow;
        oldRow = pristine.remove(id);
        if (oldRow != null) {
            deleted.put(id, fragment);
            fragment.markDeleted();
            return;
        }
        oldRow = modified.remove(id);
        if (oldRow != null) {
            deleted.put(id, fragment);
            fragment.markDeleted();
            return;
        }
        oldRow = created.remove(id);
        if (oldRow != null) {
            fragment.markDeleted(); // also detached
            return;
        }
        if (deleted.containsKey(id)) {
            throw new StorageException("Already deleted: " + fragment);
        } else {
            throw new StorageException("Already detached: " + fragment);
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
        assert tableName.equals(model.MAIN_TABLE_NAME);
        Map<Serializable, Serializable> idMap = new HashMap<Serializable, Serializable>();
        for (Fragment fragment : created.values()) {
            Serializable id = fragment.getId();
            Serializable newId = mapper.insertSingleRow((SimpleFragment) fragment);
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
            /*
             * Map temporary to persistent ids.
             */
            for (Fragment fragment : created.values()) {
                Serializable id = fragment.getId();
                Serializable newId = idMap.get(id);
                if (newId != null) {
                    fragment.setId(newId);
                    id = newId;
                }
                if (tableName.equals(model.HIER_TABLE_NAME)) {
                    /*
                     * The hierarchy table stores a foreign key to id in the
                     * parent column, it has to be mapped too.
                     */
                    SimpleFragment row = (SimpleFragment) fragment;
                    Serializable newParentId = idMap.get(row.get(model.HIER_PARENT_KEY));
                    if (newParentId != null) {
                        row.put(model.HIER_PARENT_KEY, newParentId);
                    }
                }
                if (fragment instanceof SimpleFragment) {
                    mapper.insertSingleRow((SimpleFragment) fragment);
                } else {
                    mapper.insertCollectionRows((CollectionFragment) fragment);
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
            if (fragment instanceof SimpleFragment) {
                mapper.updateSingleRow((SimpleFragment) fragment);
            } else {
                mapper.updateCollectionRows((CollectionFragment) fragment);
            }
            fragment.markPristine();
        }
        modified.clear();
        for (Fragment fragment : deleted.values()) {
            mapper.deleteFragment(fragment);
            fragment.detach();
        }
        deleted.clear();

        /*
         * Map temporary parent ids for created parents.
         */
        if (knownChildren != null) {
            Iterator<Serializable> it = knownChildren.keySet().iterator();
            Map<Serializable, Children> mappedChildren = null;
            for (; it.hasNext();) {
                Serializable parentId = it.next();
                Serializable newParentId = idMap.get(parentId);
                if (newParentId != null) {
                    if (mappedChildren == null) {
                        mappedChildren = new HashMap<Serializable, Children>();
                    }
                    mappedChildren.put(newParentId, knownChildren.get(parentId));
                    // remove temporary id
                    it.remove();
                }
            }
            if (mappedChildren != null) {
                // put back with mapped ids
                knownChildren.putAll(mappedChildren);
                mappedChildren = null;
            }
        }
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
