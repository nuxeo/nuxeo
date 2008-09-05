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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.collections.map.ReferenceMap;
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
public class Context {

    private final String tableName;

    // also accessed by Fragment
    protected final Mapper mapper;

    private final Model model;

    private final PersistenceContext persistenceContext;

    /**
     * The pristine fragments. All held data is identical to what is present in
     * the database and could be refetched if needed.
     * <p>
     * This contains fragment that are {@link State#PRISTINE} or
     * {@link State#ABSENT}, or in some cases {@link State#INVALIDATED_MODIFIED}
     * or {@link State#INVALIDATED_DELETED}.
     * <p>
     * This cache is memory-sensitive, a fragment can always be refetched if the
     * GC collects it.
     */
    protected final Map<Serializable, Fragment> pristine;

    /**
     * The fragments changed by the session.
     * <p>
     * This contains fragment that are {@link State#CREATED},
     * {@link State#MODIFIED} or {@link State#DELETED}.
     */
    protected final Map<Serializable, Fragment> modified;

    private final Map<Serializable, Children> childrenRegular;

    private final Map<Serializable, Children> childrenComplexProp;

    /**
     * The set of modified/created fragments that should be invalidated in other
     * sessions at post-commit time.
     */
    private final Set<Serializable> modifiedInTransaction;

    /**
     * The set of deleted fragments that should be invalidated in other sessions
     * at post-commit time.
     */
    private final Set<Serializable> deletedInTransaction;

    /**
     * The set of fragments that have to be invalidated as modified in this
     * session at post-commit time.
     */
    private final Set<Serializable> modifiedInvalidations;

    /**
     * The set of fragments that have to be invalidated as deleted in this
     * session at post-commit time.
     */
    private final Set<Serializable> deletedInvalidations;

    protected final boolean isCollection;

    protected final boolean isHierarchy;

    @SuppressWarnings("unchecked")
    Context(String tableName, Mapper mapper,
            PersistenceContext persistenceContext) {
        this.tableName = tableName;
        this.mapper = mapper;
        this.persistenceContext = persistenceContext;
        model = mapper.getModel();
        pristine = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        modified = new HashMap<Serializable, Fragment>();

        modifiedInTransaction = new HashSet<Serializable>();
        deletedInTransaction = new HashSet<Serializable>();
        modifiedInvalidations = new HashSet<Serializable>();
        deletedInvalidations = new HashSet<Serializable>();

        // TODO use a dedicated subclass for the hierarchy table
        isHierarchy = tableName.equals(model.hierTableName);
        if (isHierarchy) {
            childrenRegular = new HashMap<Serializable, Children>();
            childrenComplexProp = new HashMap<Serializable, Children>();
        } else {
            childrenRegular = null;
            childrenComplexProp = null;
        }
        isCollection = model.isCollectionFragment(tableName);
    }

    public String getTableName() {
        return tableName;
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
        for (Fragment fragment : modified.values()) {
            fragment.setDetached();
        }
        modified.clear();
    }

    /**
     * Creates a new row in the context.
     *
     * @param id the id
     * @param map the fragments map, or {@code null}
     * @return the created row
     * @throws StorageException if the row is already in the context
     */
    public SimpleFragment create(Serializable id, Map<String, Serializable> map)
            throws StorageException {
        if (pristine.containsKey(id) || modified.containsKey(id)) {
            throw new IllegalStateException("Row already registered: " + id);
        }
        SimpleFragment fragment = new SimpleFragment(id, State.CREATED, this,
                map);
        if (isHierarchy) {
            // add as a child of its parent
            addChild(fragment);
            // note that this new row doesn't have children
            addNewParent(id);
        }
        return fragment;
    }

    /**
     * Gets a fragment. Not applicable to hierarchy fragments.
     * <p>
     * If it's not in the context, fetch it from the mapper. If it's not in the
     * database, returns {@code null} or an absent fragment.
     *
     * @param id the fragment id
     * @param allowAbsent {@code true} to return an absent fragment as an object
     *            instead of {@code null}
     * @return the fragment, or {@code null} if none is found and {@value
     *         allowAbsent} was {@code false}
     * @throws StorageException
     */
    public Fragment get(Serializable id, boolean allowAbsent)
            throws StorageException {
        Fragment fragment = getIfPresent(id);
        if (fragment != null) {
            if (fragment.getState() == State.DELETED) {
                return null;
            }
            return fragment;
        }

        // read it through the mapper
        if (persistenceContext.isIdNew(id)) {
            // the id has not been saved, so nothing exists yet in the database
            if (isCollection) {
                fragment = model.newEmptyCollectionFragment(id, this);
            } else {
                if (allowAbsent) {
                    fragment = new SimpleFragment(id, State.ABSENT, this, null);
                } else {
                    fragment = null;
                }
            }
        } else {
            if (isCollection) {
                Serializable[] array = mapper.readCollectionArray(id, this);
                fragment = model.newCollectionFragment(id, array, this);
            } else {
                // fragment = mapper.readSingleRow(tableName, id, allowAbsent,
                // this);
                Map<String, Serializable> map = mapper.readSingleRowMap(
                        tableName, id, this);
                if (map == null) {
                    if (allowAbsent) {
                        fragment = new SimpleFragment(id, State.ABSENT, this,
                                null);
                    } else {
                        fragment = null;
                    }
                } else {
                    fragment = new SimpleFragment(id, State.PRISTINE, this, map);

                }
            }
        }
        return fragment;
    }

    /**
     * Gets a hierarchy fragment.
     * <p>
     * If it's not in the context, fetch it from the mapper. If it's not in the
     * database, returns {@code null} or an absent fragment.
     *
     * @param id the fragment id
     * @param allowAbsent {@code true} to return an absent fragment as an object
     *            instead of {@code null}
     * @return the fragment, or {@code null} if none is found and {@value
     *         allowAbsent} was {@code false}
     * @throws StorageException
     */
    public SimpleFragment getChildById(Serializable id, boolean allowAbsent)
            throws StorageException {
        SimpleFragment fragment = (SimpleFragment) getIfPresent(id);
        if (fragment != null) {
            if (fragment.getState() == State.DELETED) {
                return null;
            }
            return fragment;
        }

        // read it through the mapper
        if (persistenceContext.isIdNew(id)) {
            // the id has not been saved, so nothing exists yet in the database
            if (allowAbsent) {
                fragment = new SimpleFragment(id, State.ABSENT, this, null);
            } else {
                fragment = null;
            }
        } else {
            // fragment = mapper.readSingleRow(tableName, id, allowAbsent,
            // this);
            Map<String, Serializable> map = mapper.readSingleRowMap(tableName,
                    id, this);
            if (map == null) {
                if (allowAbsent) {
                    fragment = new SimpleFragment(id, State.ABSENT, this, null);
                } else {
                    fragment = null;
                }
            } else {
                fragment = new SimpleFragment(id, State.PRISTINE, this, map);

            }
        }
        if (fragment != null) {
            // add as a child of its parent
            addChild((SimpleFragment) fragment);
        }
        return fragment;
    }

    /**
     * Gets a fragment, if present.
     * <p>
     * If it's not in the context, returns {@code null}.
     * <p>
     * Called by {@link #get}, and by the {@link Mapper} to reuse known
     * hierarchy fragments in lists of children.
     */
    protected Fragment getIfPresent(Serializable id) {
        Fragment fragment;
        fragment = pristine.get(id);
        if (fragment != null) {
            return fragment;
        }
        return modified.get(id);
    }

    /**
     * Gets the proper children cache. Creates one if missing, unless {@code
     * complete} is {@code null}.
     */
    protected Children getChildrenCache(Serializable parentId,
            boolean complexProp, Boolean complete) {
        Map<Serializable, Children> childrenCaches = complexProp ? childrenComplexProp
                : childrenRegular;
        Children children = childrenCaches.get(parentId);
        if (children == null) {
            if (complete != null) {
                children = new Children(complete.booleanValue());
                childrenCaches.put(parentId, children);
            }
        }
        return children;
    }

    /**
     * Adds a hierarchy row to the known children of its parent.
     */
    protected void addChild(SimpleFragment row) throws StorageException {
        boolean complexProp = ((Boolean) row.get(model.HIER_CHILD_ISPROPERTY_KEY)).booleanValue();
        addChild(row, complexProp);
    }

    protected void addChild(SimpleFragment row, boolean complexProp)
            throws StorageException {
        Serializable parentId = row.get(model.HIER_PARENT_KEY);
        if (parentId == null) {
            // don't maintain all versions
            return;
        }
        Children children = getChildrenCache(parentId, complexProp,
                Boolean.FALSE);
        children.add(row, row.getString(model.HIER_CHILD_NAME_KEY));
    }

    /**
     * Notes the fact that a new row was created without children.
     */
    protected void addNewParent(Serializable parentId) {
        childrenRegular.put(parentId, new Children(true));
        childrenComplexProp.put(parentId, new Children(true));
    }

    /**
     * Removes a row from the known children of its parent.
     */
    protected void removeChild(SimpleFragment row) throws StorageException {
        boolean complexProp = ((Boolean) row.get(model.HIER_CHILD_ISPROPERTY_KEY)).booleanValue();
        removeChild(row, complexProp);
    }

    protected void removeChild(SimpleFragment row, boolean complexProp)
            throws StorageException {
        Serializable parentId = row.get(model.HIER_PARENT_KEY);
        Children children = getChildrenCache(parentId, complexProp, null);
        if (children != null) {
            children.remove(row);
        }
    }

    /**
     * Find a row in the hierarchy schema given its parent id and name. If the
     * row is not in the context, fetch it from the mapper.
     *
     * @param parentId the parent id
     * @param name the name
     * @param complexProp whether to get complex properties or regular children
     * @return the fragment, or {@code null} if none is found
     * @throws StorageException
     */
    public SimpleFragment getChildByName(Serializable parentId, String name,
            boolean complexProp) throws StorageException {
        // check in the known children
        Children children = getChildrenCache(parentId, complexProp, null);
        SimpleFragment fragment;
        if (children != null) {
            fragment = children.get(name, model.HIER_CHILD_NAME_KEY);
            if (fragment != SimpleFragment.UNKNOWN) {
                return fragment;
            }
        }

        // read it through the mapper
        fragment = mapper.readChildHierRow(parentId, name, complexProp, this);
        if (fragment != null) {
            // add as know child
            addChild(fragment, complexProp);
        }
        return fragment;
    }

    /**
     * Gets the list of children for a given parent id.
     *
     * @param parentId the parent id
     * @param name the name of the children, or {@code null} for all
     * @param complexProp whether to get complex properties or regular children
     * @return the list of children
     * @throws StorageException
     */
    public Collection<SimpleFragment> getChildren(Serializable parentId,
            String name, boolean complexProp) throws StorageException {
        List<SimpleFragment> fragments;

        Children children = getChildrenCache(parentId, complexProp,
                Boolean.FALSE);
        fragments = children.getAll(name, model.HIER_CHILD_NAME_KEY);
        if (fragments != null) {
            // we know all the children
            return fragments;
        }

        // ask the actual children to the mapper
        fragments = mapper.readChildHierRows(parentId, complexProp, this);
        children.addComplete(fragments);

        // the children may include newly-created ones, and filter by name
        return children.getAll(name, model.HIER_CHILD_NAME_KEY);
    }

    /**
     * Checks that we don't move/copy under ourselves.
     */
    protected void checkNotUnder(Serializable parentId, Serializable id,
            String op) throws StorageException {
        Serializable pid = parentId;
        do {
            if (pid.equals(id)) {
                throw new StorageException("Cannot " + op +
                        " a node under itself: " + parentId + " is under " + id);
            }
            SimpleFragment p = (SimpleFragment) getChildById(pid, false);
            if (p == null) {
                // cannot happen
                throw new StorageException("No parent: " + pid);
            }
            pid = p.get(model.HIER_PARENT_KEY);
        } while (pid != null);
    }

    /**
     * Checks that a name is free.
     */
    protected void checkFreeName(SimpleFragment row, Serializable parentId,
            String name, boolean complexProp) throws StorageException {
        Fragment prev = getChildByName(parentId, name, complexProp);
        if (prev != null) {
            throw new StorageException("Destination name already exists: " +
                    name);
        }
    }

    /**
     * Move a child to a new parent with a new name.
     *
     * @param source the source
     * @param parentId the destination parent id
     * @param name the new name
     * @throws StorageException
     */
    public void moveChild(Node source, Serializable parentId, String name)
            throws StorageException {
        Serializable id = source.getId();
        SimpleFragment hierFragment = source.getHierFragment();
        Serializable oldParentId = hierFragment.get(model.HIER_PARENT_KEY);
        String oldName = hierFragment.getString(model.HIER_CHILD_NAME_KEY);
        if (!oldParentId.equals(parentId)) {
            checkNotUnder(parentId, id, "move");
        } else if (oldName.equals(name)) {
            // null move
            return;
        }
        boolean complexProp = ((Boolean) hierFragment.get(model.HIER_CHILD_ISPROPERTY_KEY)).booleanValue();
        checkFreeName(hierFragment, parentId, name, complexProp);
        /*
         * Do the move.
         */
        removeChild(hierFragment, complexProp);
        hierFragment.put(model.HIER_PARENT_KEY, parentId);
        hierFragment.put(model.HIER_CHILD_NAME_KEY, name);
        addChild(hierFragment, complexProp);
    }

    /**
     * Copy a child to a new parent with a new name.
     *
     * @param source the source of the copy
     * @param parentId the destination parent id
     * @param name the new name
     * @return the id of the copy
     * @throws StorageException
     */
    public Serializable copyChild(Node source, Serializable parentId,
            String name) throws StorageException {
        Serializable id = source.getId();
        SimpleFragment hierFragment = source.getHierFragment();
        Serializable oldParentId = hierFragment.get(model.HIER_PARENT_KEY);
        if (!oldParentId.equals(parentId)) {
            checkNotUnder(parentId, id, "copy");
        }
        boolean complexProp = ((Boolean) hierFragment.get(model.HIER_CHILD_ISPROPERTY_KEY)).booleanValue();
        checkFreeName(hierFragment, parentId, name, complexProp);
        /*
         * Do the copy.
         */
        String typeName = source.getPrimaryType();
        Serializable newId = mapper.copyHierarchy(id, typeName, parentId, name,
                null, null, persistenceContext);
        getChildById(newId, false); // adds it as a new child of its parent
        return newId;
    }

    /**
     * Removes a row from the context.
     *
     * @param fragment
     * @throws StorageException
     */
    public void remove(Fragment fragment) throws StorageException {
        if (isHierarchy) {
            // remove from parent
            removeChild((SimpleFragment) fragment);
        }
        fragment.markDeleted();
    }

    /**
     * Removes a fragment from the database.
     *
     * @param id the fragment id
     * @throws StorageException
     */
    protected void remove(Serializable id) throws StorageException {
        Fragment fragment = getIfPresent(id);
        if (fragment != null) {
            if (fragment.getState() != State.DELETED) {
                remove(fragment);
            }
        } else {
            // this registers it with the "modified" map
            new SimpleFragment(id, State.DELETED, this, null);
        }
    }

    /**
     * Saves the created main rows, and returns the map of temporary ids to
     * final ids.
     * <p>
     * If hierarchy and main are not separate, then the parent ids of created
     * children have to be mapped on-the-fly, from previously generated parent
     * ids. This means that parents have to be created before children, which is
     * the case because "modified" is a linked hashmap.
     *
     * @param createdIds the created ids to save
     * @return the map of created ids to final ids (when different)
     * @throws StorageException
     */
    public Map<Serializable, Serializable> saveMainCreated(
            Set<Serializable> createdIds) throws StorageException {
        Map<Serializable, Serializable> idMap = new HashMap<Serializable, Serializable>();
        for (Serializable id : createdIds) {
            SimpleFragment row = (SimpleFragment) modified.remove(id);
            if (row == null) {
                throw new AssertionError(id);
            }
            // map hierarchy parent column
            if (isHierarchy) {
                Serializable newParentId = idMap.get(row.get(model.HIER_PARENT_KEY));
                if (newParentId != null) {
                    row.put(model.HIER_PARENT_KEY, newParentId);
                }
            }
            Serializable newId = mapper.insertSingleRow(row);
            row.setPristine();
            pristine.put(id, row);
            if (!newId.equals(id)) {
                // save in translation map, if different
                idMap.put(id, newId);
            }
        }
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
        for (Fragment fragment : modified.values()) {
            Serializable id = fragment.getId();
            switch (fragment.getState()) {
            case CREATED:
                /*
                 * Map temporary to persistent ids.
                 */
                Serializable newId = idMap.get(id);
                if (newId != null) {
                    fragment.setId(newId);
                    id = newId;
                }
                // map hierarchy parent column
                if (isHierarchy) {
                    SimpleFragment row = (SimpleFragment) fragment;
                    Serializable newParentId = idMap.get(row.get(model.HIER_PARENT_KEY));
                    if (newParentId != null) {
                        row.put(model.HIER_PARENT_KEY, newParentId);
                    }
                }
                // TODO XXX there are other references to id (versionableid,
                // targetid, etc).
                /*
                 * Do the creation.
                 */
                if (fragment instanceof SimpleFragment) {
                    mapper.insertSingleRow((SimpleFragment) fragment);
                } else {
                    mapper.insertCollectionRows((CollectionFragment) fragment);
                }
                fragment.setPristine();
                // modified map cleared at end of loop
                pristine.put(id, fragment);
                modifiedInTransaction.add(id);
                break;
            case MODIFIED:
                if (fragment instanceof SimpleFragment) {
                    mapper.updateSingleRow((SimpleFragment) fragment);
                } else {
                    mapper.updateCollectionRows((CollectionFragment) fragment);
                }
                fragment.setPristine();
                // modified map cleared at end of loop
                pristine.put(id, fragment);
                modifiedInTransaction.add(id);
                break;
            case DELETED:
                // TODO deleting non-hierarchy fragments is done by the database
                // itself as their foreign key to hierarchy is ON DELETE CASCADE
                mapper.deleteFragment(fragment);
                fragment.setDetached();
                // modified map cleared at end of loop
                deletedInTransaction.add(id);
                break;
            default:
                throw new AssertionError(fragment);
            }
        }
        modified.clear();

        /*
         * Map temporary parent ids for created parents.
         */
        if (isHierarchy) {
            for (Entry<Serializable, Serializable> e : idMap.entrySet()) {
                Children children = childrenRegular.remove(e.getKey());
                if (children != null) {
                    childrenRegular.put(e.getValue(), children);
                }
                children = childrenComplexProp.remove(e.getKey());
                if (children != null) {
                    childrenComplexProp.put(e.getValue(), children);
                }
            }
        }
    }

    /**
     * Called when the database has added new children to a node.
     */
    protected void markChildrenInvalidatedAdded(Serializable id,
            boolean complexProp) {
        Children children = getChildrenCache(id, complexProp, null);
        if (children != null) {
            children.invalidateAdded();
        }
    }

    /**
     * Called by the mapper when a fragment has been updated in the database.
     *
     * @param id the id
     * @param wasModified {@code true} for a modification, {@code false} for a
     *            deletion
     */
    protected void markInvalidated(Serializable id, boolean wasModified) {
        if (wasModified) {
            Fragment fragment = getIfPresent(id);
            if (fragment != null) {
                fragment.markInvalidatedModified();
            }
            modifiedInTransaction.add(id);
        } else { // deleted
            Fragment fragment = getIfPresent(id);
            if (fragment != null) {
                fragment.markInvalidatedDeleted();
            }
            deletedInTransaction.add(id);
        }
    }

    /**
     * Process invalidations notified from other sessions. Called
     * pre-transaction.
     */
    protected void processInvalidations() {
        synchronized (modifiedInvalidations) {
            for (Serializable id : modifiedInvalidations) {
                Fragment fragment = pristine.remove(id);
                if (fragment != null) {
                    fragment.setInvalidatedModified();
                }
            }
            modifiedInvalidations.clear();
        }
        synchronized (deletedInvalidations) {
            for (Serializable id : deletedInvalidations) {
                Fragment fragment = pristine.remove(id);
                if (fragment != null) {
                    fragment.setInvalidatedDeleted();
                }
            }
            deletedInvalidations.clear();
        }
    }

    /**
     * Notify invalidations to other sessions. Called post-transaction.
     */
    protected void notifyInvalidations() {
        if (!modifiedInTransaction.isEmpty() || !deletedInTransaction.isEmpty()) {
            persistenceContext.invalidateOthers(this);
            modifiedInTransaction.clear();
            deletedInTransaction.clear();
        }
    }

    /**
     * Called by cross-session invalidation when another session just committed.
     */
    protected void invalidate(Context other) {
        synchronized (modifiedInvalidations) {
            modifiedInvalidations.addAll(other.modifiedInTransaction);
        }
        synchronized (deletedInvalidations) {
            deletedInvalidations.addAll(other.deletedInTransaction);
        }
    }

}
