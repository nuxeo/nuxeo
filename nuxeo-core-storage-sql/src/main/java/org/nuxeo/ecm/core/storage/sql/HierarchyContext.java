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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.nuxeo.ecm.core.storage.StorageException;

/**
 * This class holds persistence context information for the hierarchy table, and
 * adds specialized methods over {@link Context}.
 *
 * @author Florent Guillaume
 */
public class HierarchyContext extends Context {

    protected final Map<Serializable, Children> childrenRegular;

    protected final Map<Serializable, Children> childrenComplexProp;

    HierarchyContext(Mapper mapper, PersistenceContext persistenceContext) {
        super(mapper.getModel().hierTableName, mapper, persistenceContext);
        childrenRegular = new HashMap<Serializable, Children>();
        childrenComplexProp = new HashMap<Serializable, Children>();
    }

    /**
     * Gets the proper children cache. Creates one if missing and {@code create}
     * is {@code true}.
     */
    protected Children getChildrenCache(Serializable parentId,
            boolean complexProp, boolean create) {
        Map<Serializable, Children> childrenCaches = complexProp ? childrenComplexProp
                : childrenRegular;
        Children children = childrenCaches.get(parentId);
        if (children == null) {
            if (create) {
                children = new Children(false);
                childrenCaches.put(parentId, children);
            }
        }
        return children;
    }

    protected boolean complexProp(SimpleFragment row) throws StorageException {
        return ((Boolean) row.get(model.HIER_CHILD_ISPROPERTY_KEY)).booleanValue();
    }

    /**
     * Adds a hierarchy row to the known children of its parent.
     */
    protected void addChild(SimpleFragment row) throws StorageException {
        addChild(row, complexProp(row));
    }

    protected void addChild(SimpleFragment row, boolean complexProp)
            throws StorageException {
        Serializable parentId = row.get(model.HIER_PARENT_KEY);
        if (parentId == null) {
            // don't maintain all versions
            return;
        }
        getChildrenCache(parentId, complexProp, true).add(row);
    }

    @Override
    public SimpleFragment create(Serializable id, Map<String, Serializable> map)
            throws StorageException {
        SimpleFragment fragment = super.create(id, map);
        // add as a child of its parent
        addChild(fragment);
        // note that this new row doesn't have children
        addNewParent(id);
        return fragment;
    }

    /**
     * Notes the fact that a new row was created without children.
     */
    protected void addNewParent(Serializable parentId) {
        childrenRegular.put(parentId, new Children(true));
        childrenComplexProp.put(parentId, new Children(true));
    }

    @Override
    protected Fragment getFromMapper(Serializable id, boolean allowAbsent)
            throws StorageException {
        Fragment fragment = super.getFromMapper(id, allowAbsent);
        if (fragment != null) {
            // add as a child of its parent
            addChild((SimpleFragment) fragment);
        }
        return fragment;
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
        Children children = getChildrenCache(parentId, complexProp, false);
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

        Children children = getChildrenCache(parentId, complexProp, true);
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
            SimpleFragment p = (SimpleFragment) get(pid, false);
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
        boolean complexProp = complexProp(hierFragment);
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
        checkFreeName(hierFragment, parentId, name, complexProp(hierFragment));
        /*
         * Do the copy.
         */
        String typeName = source.getPrimaryType();
        Serializable newId = mapper.copyHierarchy(id, typeName, parentId, name,
                null, null, persistenceContext);
        get(newId, false); // adds it as a new child of its parent
        return newId;
    }

    @Override
    public void remove(Fragment fragment) throws StorageException {
        // remove from parent
        removeChild((SimpleFragment) fragment);
        super.remove(fragment);
    }

    /**
     * Removes a row from the known children of its parent.
     */
    protected void removeChild(SimpleFragment row) throws StorageException {
        removeChild(row, complexProp(row));
    }

    protected void removeChild(SimpleFragment row, boolean complexProp)
            throws StorageException {
        Serializable parentId = row.get(model.HIER_PARENT_KEY);
        Children children = getChildrenCache(parentId, complexProp, false);
        if (children != null) {
            children.remove(row);
        }
    }

    @Override
    protected void remapFragmentOnSave(Fragment fragment,
            Map<Serializable, Serializable> idMap) throws StorageException {
        SimpleFragment row = (SimpleFragment) fragment;
        // map hierarchy parent column
        Serializable newParentId = idMap.get(row.get(model.HIER_PARENT_KEY));
        if (newParentId != null) {
            row.put(model.HIER_PARENT_KEY, newParentId);
        }
    }

    /**
     * Saves the created main rows, and returns the map of temporary ids to
     * final ids.
     * <p>
     * The parent ids of created children have to be mapped on the fly from
     * previously generated parent ids. This means that parents have to be
     * created before children, which is the case because "modified" is a linked
     * hashmap.
     *
     * @param createdIds the created ids to save
     * @return the map of created ids to final ids (when different)
     * @throws StorageException
     */
    public Map<Serializable, Serializable> saveCreated(
            Set<Serializable> createdIds) throws StorageException {
        Map<Serializable, Serializable> idMap = null;
        for (Serializable id : createdIds) {
            SimpleFragment row = (SimpleFragment) modified.remove(id);
            if (row == null) {
                throw new AssertionError(id);
            }
            if (idMap != null) {
                remapFragmentOnSave(row, idMap);
            }
            Serializable newId = mapper.insertSingleRow(row);
            row.setPristine();
            pristine.put(id, row);
            // save in translation map, if different
            // only happens for DB_IDENTITY id generation policy
            if (!newId.equals(id)) {
                if (idMap == null) {
                    idMap = new HashMap<Serializable, Serializable>();
                }
                idMap.put(id, newId);
            }
        }
        return idMap == null ? Collections.<Serializable, Serializable> emptyMap()
                : idMap;
    }

    @Override
    public void save(Map<Serializable, Serializable> idMap)
            throws StorageException {
        super.save(idMap);
        // map temporary parent ids for created parents
        for (Entry<Serializable, Serializable> entry : idMap.entrySet()) {
            Serializable id = entry.getKey();
            Children children = childrenRegular.remove(id);
            if (children != null) {
                childrenRegular.put(entry.getValue(), children);
            }
            children = childrenComplexProp.remove(id);
            if (children != null) {
                childrenComplexProp.put(entry.getValue(), children);
            }
        }
    }

    /**
     * Called when the database has added new children to a node.
     */
    protected void markChildrenInvalidatedAdded(Serializable id,
            boolean complexProp) {
        Children children = getChildrenCache(id, complexProp, false);
        if (children != null) {
            children.invalidateAdded();
        }
    }

}
