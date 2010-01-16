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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.ReferenceMap;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * This class holds persistence context information for the hierarchy table, and
 * adds specialized methods over {@link Context}.
 *
 * @author Florent Guillaume
 */
public class HierarchyContext extends Context {

    protected static final String INVAL_PARENT = "__PARENT__";

    protected final Map<Serializable, Children> childrenRegularSoft;

    protected final Map<Serializable, Children> childrenRegularHard;

    protected final Map<Serializable, Children> childrenComplexPropSoft;

    protected final Map<Serializable, Children> childrenComplexPropHard;

    /**
     * The parents modified in the transaction.
     */
    private final Set<Serializable> modifiedParentsInTransaction;

    /**
     * The set of parents that have to be invalidated in this session at
     * post-commit time.
     */
    private final Set<Serializable> modifiedParentsInvalidations;

    private final PositionComparator posComparator;

    public HierarchyContext(Mapper mapper, PersistenceContext persistenceContext) {
        super(mapper.getModel().hierTableName, mapper, persistenceContext);
        childrenRegularSoft = new ReferenceMap(ReferenceMap.HARD,
                ReferenceMap.SOFT);
        childrenRegularHard = new HashMap<Serializable, Children>();
        childrenComplexPropSoft = new ReferenceMap(ReferenceMap.HARD,
                ReferenceMap.SOFT);
        childrenComplexPropHard = new HashMap<Serializable, Children>();
        modifiedParentsInTransaction = new HashSet<Serializable>();
        modifiedParentsInvalidations = new HashSet<Serializable>();
        posComparator = new PositionComparator(model.HIER_CHILD_POS_KEY);
    }

    /**
     * Comparator of {@link SimpleFragment}s according to their pos field.
     */
    public static class PositionComparator implements
            Comparator<SimpleFragment> {

        protected final String posKey;

        public PositionComparator(String posKey) {
            this.posKey = posKey;
        }

        public int compare(SimpleFragment frag1, SimpleFragment frag2) {
            try {
                Long pos1 = (Long) frag1.get(posKey);
                Long pos2 = (Long) frag2.get(posKey);
                if (pos1 == null && pos2 == null) {
                    // coherent sort
                    return frag1.hashCode() - frag2.hashCode();
                }
                if (pos1 == null) {
                    return 1;
                }
                if (pos2 == null) {
                    return -1;
                }
                return pos1.compareTo(pos2);
            } catch (StorageException e) {
                // shouldn't happen
                return frag1.hashCode() - frag2.hashCode();
            }
        }
    }

    @Override
    protected int clearCaches() {
        // flush allowable children caches
        int n = super.clearCaches() + childrenRegularSoft.size()
                + childrenComplexPropSoft.size();
        childrenRegularSoft.clear();
        childrenComplexPropSoft.clear();
        return n;
    }

    /**
     * Gets the proper children cache. Creates one if missing.
     */
    protected Children getChildrenCache(Serializable parentId,
            boolean complexProp) {
        Map<Serializable, Children> softMap;
        Map<Serializable, Children> hardMap;
        if (complexProp) {
            softMap = childrenComplexPropSoft;
            hardMap = childrenComplexPropHard;
        } else {
            softMap = childrenRegularSoft;
            hardMap = childrenRegularHard;
        }
        Children children = softMap.get(parentId);
        if (children == null) {
            children = hardMap.get(parentId);
            if (children == null) {
                children = new Children(this, model.HIER_CHILD_NAME_KEY, false,
                        parentId, softMap, hardMap);
            }
        }
        return children;
    }

    protected boolean complexProp(SimpleFragment row) throws StorageException {
        return ((Boolean) row.get(model.HIER_CHILD_ISPROPERTY_KEY)).booleanValue();
    }

    protected void addExistingChild(SimpleFragment row, boolean complexProp)
            throws StorageException {
        Serializable parentId = row.get(model.HIER_PARENT_KEY);
        if (parentId == null) {
            return;
        }
        getChildrenCache(parentId, complexProp).addExisting(row.getId());
        modifiedParentsInTransaction.add(parentId);
    }

    protected void addCreatedChild(SimpleFragment row, boolean complexProp)
            throws StorageException {
        Serializable parentId = row.get(model.HIER_PARENT_KEY);
        if (parentId == null) {
            return;
        }
        getChildrenCache(parentId, complexProp).addCreated(row.getId());
        modifiedParentsInTransaction.add(parentId);
    }

    protected void removeChild(SimpleFragment row, boolean complexProp)
            throws StorageException {
        Serializable parentId = row.get(model.HIER_PARENT_KEY);
        if (parentId == null) {
            return;
        }
        getChildrenCache(parentId, complexProp).remove(row.getId());
        modifiedParentsInTransaction.add(parentId);
    }

    @Override
    public SimpleFragment create(Serializable id, Map<String, Serializable> map)
            throws StorageException {
        SimpleFragment fragment = super.create(id, map);
        // add as a child of its parent
        addCreatedChild(fragment, complexProp(fragment));
        // note that this new row doesn't have children
        addNewParent(id);
        return fragment;
    }

    /**
     * Notes the fact that a new row was created without children.
     */
    protected void addNewParent(Serializable parentId) {
        new Children(this, model.HIER_CHILD_NAME_KEY, true, parentId,
                childrenRegularSoft, childrenRegularHard);
        new Children(this, model.HIER_CHILD_NAME_KEY, true, parentId,
                childrenComplexPropSoft, childrenComplexPropHard);
    }

    @Override
    protected Fragment getFromMapper(Serializable id, boolean allowAbsent)
            throws StorageException {
        SimpleFragment fragment = (SimpleFragment) super.getFromMapper(id,
                allowAbsent);
        if (fragment != null) {
            // add as a child of its parent
            addExistingChild(fragment, complexProp(fragment));
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
        SimpleFragment fragment = getChildrenCache(parentId, complexProp).getFragmentByValue(
                name);
        if (fragment == SimpleFragment.UNKNOWN) {
            // read it through the mapper
            fragment = mapper.readChildHierRow(parentId, name, complexProp,
                    this);
            if (fragment != null) {
                // add as know child
                addExistingChild(fragment, complexProp);
            }
        }
        return fragment;
    }

    /**
     * Gets the list of children for a given parent id.
     * <p>
     * Complex properties and children of ordered folders are returned in the
     * proper order.
     *
     * @param parentId the parent id
     * @param name the name of the children, or {@code null} for all
     * @param complexProp whether to get complex properties or regular children
     * @return the list of children
     * @throws StorageException
     */
    public List<SimpleFragment> getChildren(Serializable parentId, String name,
            boolean complexProp) throws StorageException {
        Children children = getChildrenCache(parentId, complexProp);
        List<SimpleFragment> fragments = children.getFragmentsByValue(name);
        if (fragments == null) {
            // ask the actual children to the mapper
            fragments = mapper.readChildHierRows(parentId, complexProp, this);
            List<Serializable> ids = new ArrayList<Serializable>(
                    fragments.size());
            for (Fragment fragment : fragments) {
                ids.add(fragment.getId());
            }
            children.addExistingComplete(ids);

            // the children may include newly-created ones, and filter by name
            fragments = children.getFragmentsByValue(name);
        }
        if (isOrderable(parentId, complexProp)) {
            // sort children in order
            Collections.sort(fragments, posComparator);
        }
        return fragments;
    }

    private boolean isOrderable(Serializable parentId, boolean complexProp)
            throws StorageException {
        if (complexProp) {
            return true;
        }
        SimpleFragment parent = (SimpleFragment) get(parentId, true);
        String typeName = parent.getString(model.MAIN_PRIMARY_TYPE_KEY);
        return model.getDocumentTypeFacets(typeName).contains(
                FacetNames.ORDERABLE);
    }

    /**
     * Finds the id of the enclosing non-complex-property node.
     *
     * @param id the id
     * @return the id of the containing document, or {@code null} if there is no
     *         parent or the parent has been deleted.
     */
    protected Serializable getContainingDocument(Serializable id)
            throws StorageException {
        Serializable pid = id;
        while (true) {
            if (pid == null) {
                // no parent
                return null;
            }
            SimpleFragment p = (SimpleFragment) get(pid, false);
            if (p == null) {
                // can happen if the fragment has been deleted
                return null;
            }
            if (!complexProp(p)) {
                return pid;
            }
            pid = p.get(model.HIER_PARENT_KEY);
        }
    }

    /**
     * Checks that we don't move/copy under ourselves.
     */
    protected void checkNotUnder(Serializable parentId, Serializable id,
            String op) throws StorageException {
        Serializable pid = parentId;
        do {
            if (pid.equals(id)) {
                throw new StorageException("Cannot " + op
                        + " a node under itself: " + parentId + " is under "
                        + id);
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
            throw new StorageException("Destination name already exists: "
                    + name);
        }
    }

    /**
     * Order a child before another.
     *
     * @param parentId the parent id
     * @param sourceId the node id to move
     * @param destId the node id before which to place the source node, if
     *            {@code null} then move the source to the end
     * @throws StorageException
     */
    public void orderBefore(Serializable parentId, Serializable sourceId,
            Serializable destId) throws StorageException {
        boolean complexProp = false;
        if (!isOrderable(parentId, complexProp)) {
            // TODO throw exception?
            return;
        }
        if (sourceId.equals(destId)) {
            return;
        }
        // This is optimized by assuming the number of children is small enough
        // to be manageable in-memory.
        // fetch children and relevant nodes
        List<SimpleFragment> fragments = getChildren(parentId, null,
                complexProp);
        // renumber fragments
        int i = 0;
        SimpleFragment source = null; // source if seen
        Long destPos = null;
        for (SimpleFragment fragment : fragments) {
            Serializable id = fragment.getId();
            if (id.equals(destId)) {
                destPos = Long.valueOf(i);
                i++;
                if (source != null) {
                    source.put(model.HIER_CHILD_POS_KEY, destPos);
                }
            }
            Long setPos;
            if (id.equals(sourceId)) {
                i--;
                source = fragment;
                setPos = destPos;
            } else {
                setPos = Long.valueOf(i);
            }
            if (setPos != null) {
                if (!setPos.equals(fragment.get(model.HIER_CHILD_POS_KEY))) {
                    fragment.put(model.HIER_CHILD_POS_KEY, setPos);
                }
            }
            i++;
        }
        if (destId == null) {
            Long setPos = Long.valueOf(i);
            if (!setPos.equals(source.get(model.HIER_CHILD_POS_KEY))) {
                source.put(model.HIER_CHILD_POS_KEY, setPos);
            }
        }
    }

    /**
     * Gets the next pos value for a new child in a folder.
     *
     * @param nodeId the folder node id
     * @param complexProp whether to deal with complex properties or regular
     *            children
     * @return the next pos, or {@code null} if not orderable
     * @throws StorageException
     */
    protected Long getNextPos(Serializable nodeId, boolean complexProp)
            throws StorageException {
        if (!isOrderable(nodeId, complexProp)) {
            return null;
        }
        long max = -1;
        for (SimpleFragment fragment : getChildren(nodeId, null, complexProp)) {
            Long pos = (Long) fragment.get(model.HIER_CHILD_POS_KEY);
            if (pos != null && pos.longValue() > max) {
                max = pos.longValue();
            }
        }
        return Long.valueOf(max + 1);
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
        // a save() has already been done by the caller
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
        if (!oldName.equals(name)) {
            hierFragment.put(model.HIER_CHILD_NAME_KEY, name);
        }
        removeChild(hierFragment, complexProp);
        hierFragment.put(model.HIER_PARENT_KEY, parentId);
        addExistingChild(hierFragment, complexProp);
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
        removeChild((SimpleFragment) fragment,
                complexProp((SimpleFragment) fragment));
        super.remove(fragment);
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
                // was created and deleted before save
                continue;
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
        for (Serializable id : idMap.keySet()) {
            Serializable newId = idMap.get(id);
            for (Map<Serializable, Children> map : new Map[] {
                    childrenRegularSoft, childrenRegularHard,
                    childrenComplexPropSoft, childrenComplexPropHard }) {
                Children children = map.remove(id);
                if (children != null) {
                    map.put(newId, children);
                }
            }
        }
        // flush children caches (moves from hard to soft)
        for (Children children : childrenRegularHard.values()) {
            children.flush(); // added to soft map
        }
        childrenRegularHard.clear();
        for (Children children : childrenComplexPropHard.values()) {
            children.flush(); // added to soft map
        }
        childrenComplexPropHard.clear();
    }

    /**
     * Called by the mapper when it has added new children (of unknown ids) to a
     * node.
     */
    protected void markChildrenAdded(Serializable parentId) {
        for (Map<Serializable, Children> map : new Map[] { childrenRegularSoft,
                childrenRegularHard, childrenComplexPropSoft,
                childrenComplexPropHard }) {
            Children children = map.get(parentId);
            if (children != null) {
                children.setIncomplete();
            }
        }
        modifiedParentsInTransaction.add(parentId);
    }

    @Override
    protected void gatherInvalidations(Invalidations invalidations) {
        super.gatherInvalidations(invalidations);
        invalidations.addModified(INVAL_PARENT, modifiedParentsInTransaction);
        modifiedParentsInTransaction.clear();
    }

    @Override
    protected void processReceivedInvalidations() {
        super.processReceivedInvalidations();
        synchronized (modifiedParentsInvalidations) {
            for (Serializable parentId : modifiedParentsInvalidations) {
                childrenRegularSoft.remove(parentId);
                childrenRegularHard.remove(parentId);
                childrenComplexPropSoft.remove(parentId);
                childrenComplexPropHard.remove(parentId);
            }
            modifiedParentsInvalidations.clear();
        }
    }

    @Override
    protected void invalidate(Invalidations invalidations) {
        super.invalidate(invalidations);
        Set<Serializable> set = invalidations.modified.get(INVAL_PARENT);
        if (set != null) {
            synchronized (modifiedParentsInvalidations) {
                modifiedParentsInvalidations.addAll(set);
            }
        }
    }

}
