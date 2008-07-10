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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Fragment.State;

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
     * The fragments that have been read and found to be absent in the database.
     * They contain default data (usually {@code null}). Upon modification, they
     * are moved to {@link #created}.
     */
    private final Map<Serializable, Fragment> absent;

    /**
     * The created fragments, that will be inserted in the database at the next
     * save. Their id is a temporary one XXX not always.
     */
    private final Map<Serializable, Fragment> created;

    /**
     * The pristine fragments. All held data is identical to what is present in
     * the database and could be refetched if needed. This cache is also managed
     * externally.
     * <p>
     * Upon modification, the fragments are moved to {@link #modified}.
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

    /**
     * The modified fragments.
     * <p>
     * Upon save, the database will be updated and the fragments will be moved
     * to {@link #pristine}.
     */
    private final Map<Serializable, Fragment> modified;

    /**
     * The deleted fragments.
     */
    private final Map<Serializable, Fragment> deleted;

    /**
     * The children info (only for the hierarchy table).
     */
    private final Map<Serializable, Children> knownChildren;

    private final boolean isCollection;

    @SuppressWarnings("unchecked")
    PersistenceContextByTable(String tableName, Mapper mapper) {
        this.tableName = tableName;
        this.mapper = mapper;
        model = mapper.getModel();
        absent = new HashMap<Serializable, Fragment>();
        // linked map to keep ids in the same order as creation (cosmetic)
        created = new LinkedHashMap<Serializable, Fragment>();
        pristine = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        modified = new HashMap<Serializable, Fragment>();
        deleted = new HashMap<Serializable, Fragment>();

        // TODO use a dedicated subclass for the hierarchy table
        if (tableName.equals(model.HIER_TABLE_NAME)) {
            knownChildren = new HashMap<Serializable, Children>();
        } else {
            knownChildren = null;
        }
        isCollection = model.collectionTables.containsKey(tableName);
    }

    /**
     * Closes the context. Keeps around the {@link #pristine} and
     * {@link #absent} caches (to avoid costly refills). These two caches are
     * nevertheless still invalidatable.
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
        if (pristine.containsKey(id) || absent.containsKey(id) ||
                created.containsKey(id) || modified.containsKey(id) ||
                deleted.containsKey(id)) {
            throw new IllegalStateException("Row already registered: " + id);
        }
        SimpleFragment fragment = new SimpleFragment(tableName, id,
                State.CREATED, this, map);
        if (knownChildren != null) {
            // add as a child of its parent
            addChild(fragment);
            // note that this new row doesn't have children
            addNewParent(id);
        }
        return fragment;
    }

    /**
     * Gets a fragment.
     * <p>
     * If it's not in the context, fetch it from the mapper. If it's not in the
     * database, returns {@code null} or an absent fragment.
     *
     * @param id the fragment id
     * @param createAbsent {@code true} to return an absent fragment as an
     *            object instead of {@code null}
     * @return the fragment, or {@code null} if none is found and {@value
     *         createAbsent} was {@code false}
     * @throws StorageException
     */
    public Fragment get(Serializable id, boolean createAbsent)
            throws StorageException {
        if (isDeleted(id)) {
            return null;
        }
        Fragment fragment = getIfPresent(id);
        if (fragment != null) {
            return fragment;
        }
        fragment = absent.get(id);
        if (fragment != null) {
            return fragment;
        }

        // read it through the mapper
        boolean isTemporaryId = id instanceof String &&
                ((String) id).startsWith("T");
        if (isTemporaryId) {
            if (isCollection) {
                fragment = new CollectionFragment(tableName, id, State.CREATED,
                        this, new Serializable[0]);
            } else {
                if (createAbsent) {
                    fragment = new SimpleFragment(tableName, id, State.ABSENT,
                            this, null);
                } else {
                    fragment = null;
                }
            }
        } else {
            if (isCollection) {
                fragment = mapper.readCollectionRows(tableName, id, this);
            } else {
                fragment = mapper.readSingleRow(tableName, id, createAbsent,
                        this);
            }
        }
        if (knownChildren != null && fragment != null) {
            // add as a child of its parent
            addChild((SimpleFragment) fragment);
        }
        return fragment;
    }

    /**
     * Checks if a fragment is deleted.
     * <p>
     * Called by {@link #get}, and by the {@link Mapper} to avoid returning
     * deleted fragments in lists of children.
     */
    protected boolean isDeleted(Serializable id) {
        return deleted.containsKey(id);
    }

    /**
     * Gets a fragment, if present.
     * <p>
     * If it's not in the context, returns {@code null}.
     * <p>
     * Called by {@link #get}, and by the {@link Mapper} to reuse known
     * hierarchy fragments in lists of children.
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
     * Called by the {@link Fragment} when a new absent fragment is constructed.
     */
    protected void newAbsent(Fragment fragment) {
        absent.put(fragment.getId(), fragment);
    }

    /**
     * Called by the {@link Fragment} when a new created fragment is
     * constructed.
     */
    protected void newCreated(Fragment fragment) {
        created.put(fragment.getId(), fragment);
    }

    /**
     * Called by the {@link Fragment} when a new pristine fragment is
     * constructed.
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
                (Long) row.get(model.HIER_CHILD_POS_KEY), model);
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
            children.remove(row.getString(model.HIER_CHILD_NAME_KEY),
                    (Long) row.get(model.HIER_CHILD_POS_KEY), model);
        }
    }

    /**
     * Find a row in the hierarchy schema given its parent id and name. If the
     * row is not in the context, fetch it from the mapper.
     *
     * @param parentId the parent id
     * @param name the name
     * @param complexProp whether to get complex properties or real children, or
     *            both
     * @return the row, or {@code null} if none is found
     * @throws StorageException
     */
    public SimpleFragment getByHier(Serializable parentId, String name,
            Boolean complexProp) throws StorageException {
        SimpleFragment fragment;

        // check in the known children
        Children children = knownChildren.get(parentId);
        if (children != null) {
            fragment = children.get(name);
            if (fragment != SimpleFragment.UNKNOWN) {
                return fragmentIfSamePropertyFlag(fragment, complexProp);
            }
        }

        // read it through the mapper
        fragment = mapper.readChildHierRow(parentId, name, this);

        // add as know child
        if (fragment != null) {
            addChild(fragment);
        }

        return fragmentIfSamePropertyFlag(fragment, complexProp);
    }

    protected SimpleFragment fragmentIfSamePropertyFlag(SimpleFragment fragment,
            Boolean properties) {
        if (properties == null) {
            return fragment;
        }
        return properties.equals((Boolean) fragment.get(model.HIER_CHILD_ISPROPERTY_KEY)) ?
                fragment : null;
    }

    /**
     * Gets the list of children for a given parent id.
     *
     * @param parentId the parent id
     * @param complexProp whether to get complex properties or real children, or
     *            both
     * @return the list of children
     * @throws StorageException
     */
    public Collection<SimpleFragment> getHierChildren(Serializable parentId,
            Boolean complexProp) throws StorageException {
        Collection<SimpleFragment> fragments;

        // check in the known children
        Children children = knownChildren.get(parentId);
        if (children == null) {
            children = new Children(false);
            knownChildren.put(parentId, children);
        } else {
            fragments = children.getFragments(complexProp);
            if (fragments != null) {
                return fragments;
            }
        }

        // ask the children to the mapper
        fragments = mapper.readChildHierRows(parentId, complexProp, this);

        // we now know the full children for this parent
        children.addComplete(fragments, complexProp, model);

        // the children may include newly-created ones
        return children.getFragments(complexProp);
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
        oldRow = absent.remove(id);
        if (oldRow != null) {
            fragment.markDeleted(); // also detached
            return;
        }
        oldRow = created.remove(id);
        if (oldRow != null) {
            fragment.markDeleted(); // also detached
            return;
        }
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
            for (Entry<Serializable, Serializable> e : idMap.entrySet()) {
                Children children = knownChildren.remove(e.getKey());
                if (children != null) {
                    knownChildren.put(e.getValue(), children);
                }
            }
        }
    }

    /**
     * Callback from a {@link Fragment}, used when the row changes from the
     * pristine to the modified state.
     */
    protected void markPristineModified(Fragment row) {
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

    /**
     * Callback from a {@link Fragment}, used when the row changes from the
     * absent to the created state.
     */
    protected void markAbsentCreated(Fragment row) {
        Serializable id = row.getId();
        Fragment old = absent.remove(id);
        if (old == null) {
            log.error("Should be absent: " + row);
        }
        old = created.put(id, row);
        if (old != null) {
            log.error("Should not be created: " + row);
        }
    }

}
