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
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Fragment.State;
import org.nuxeo.ecm.core.storage.sql.RowMapper.CopyHierarchyResult;
import org.nuxeo.ecm.core.storage.sql.RowMapper.IdWithTypes;
import org.nuxeo.ecm.core.storage.sql.SimpleFragment.PositionComparator;

/**
 * This class holds cached information for children relationships in the
 * hierarchy table.
 */
public class HierarchyContext {

    private static final Log log = LogFactory.getLog(HierarchyContext.class);

    private final Model model;

    private final RowMapper mapper;

    private final SessionImpl session;

    private final PersistenceContext context;

    private final Map<Serializable, Children> childrenRegularSoft;

    // public because used from unit tests
    public final Map<Serializable, Children> childrenRegularHard;

    private final Map<Serializable, Children> childrenComplexPropSoft;

    private final Map<Serializable, Children> childrenComplexPropHard;

    private final Map<Serializable, Children>[] childrenAllMaps;

    /**
     * The parents modified in the transaction, that should be propagated as
     * invalidations to other sessions at post-commit time.
     */
    private final Set<Serializable> modifiedParentsInTransaction;

    private final PositionComparator posComparator;

    @SuppressWarnings("unchecked")
    public HierarchyContext(Model model, RowMapper mapper, SessionImpl session,
            PersistenceContext context) {
        this.model = model;
        this.mapper = mapper;
        this.session = session;
        this.context = context;

        childrenRegularSoft = new ReferenceMap(ReferenceMap.HARD,
                ReferenceMap.SOFT);
        childrenRegularHard = new HashMap<Serializable, Children>();
        childrenComplexPropSoft = new ReferenceMap(ReferenceMap.HARD,
                ReferenceMap.SOFT);
        childrenComplexPropHard = new HashMap<Serializable, Children>();
        childrenAllMaps = new Map[] { childrenRegularSoft, childrenRegularHard,
                childrenComplexPropSoft, childrenComplexPropHard };

        modifiedParentsInTransaction = new HashSet<Serializable>();

        posComparator = new PositionComparator(model.HIER_CHILD_POS_KEY);
    }

    public int clearCaches() {
        // only the soft children are caches, the others hold info
        int n = childrenRegularSoft.size() + childrenComplexPropSoft.size();
        childrenRegularSoft.clear();
        childrenComplexPropSoft.clear();
        modifiedParentsInTransaction.clear();
        return n;
    }

    /** Gets the proper children cache. Creates one if missing. */
    protected Children childrenCache(Serializable parentId, boolean complexProp) {
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

    protected boolean complexProp(SimpleFragment fragment)
            throws StorageException {
        return ((Boolean) fragment.get(model.HIER_CHILD_ISPROPERTY_KEY)).booleanValue();
    }

    protected void addExistingChild(SimpleFragment fragment,
            boolean complexProp, boolean invalidate) throws StorageException {
        Serializable parentId = fragment.get(model.HIER_PARENT_KEY);
        if (parentId == null) {
            return;
        }
        childrenCache(parentId, complexProp).addExisting(fragment.getId());
        if (invalidate) {
            modifiedParentsInTransaction.add(parentId);
        }
    }

    protected void addCreatedChild(SimpleFragment fragment, boolean complexProp)
            throws StorageException {
        Serializable parentId = fragment.get(model.HIER_PARENT_KEY);
        if (parentId == null) {
            return;
        }
        childrenCache(parentId, complexProp).addCreated(fragment.getId());
        modifiedParentsInTransaction.add(parentId);
    }

    protected void removeChild(SimpleFragment fragment, boolean complexProp)
            throws StorageException {
        Serializable parentId = fragment.get(model.HIER_PARENT_KEY);
        if (parentId == null) {
            return;
        }
        childrenCache(parentId, complexProp).remove(fragment.getId());
        modifiedParentsInTransaction.add(parentId);
    }

    public void createdSimpleFragment(SimpleFragment fragment)
            throws StorageException {
        if (!model.HIER_TABLE_NAME.equals(fragment.row.tableName)) {
            return;
        }
        // add as a child of its parent
        addCreatedChild(fragment, complexProp(fragment));
        // note that this new row doesn't have children
        Serializable parentId = fragment.getId();
        new Children(this, model.HIER_CHILD_NAME_KEY, true, parentId,
                childrenRegularSoft, childrenRegularHard);
        new Children(this, model.HIER_CHILD_NAME_KEY, true, parentId,
                childrenComplexPropSoft, childrenComplexPropHard);
    }

    /**
     * Find a fragment in the hierarchy schema given its parent id and name. If
     * the fragment is not in the context, fetch it from the mapper.
     *
     * @param parentId the parent id
     * @param name the name
     * @param complexProp whether to get complex properties or regular children
     * @return the fragment, or {@code null} if not found
     */
    public SimpleFragment getChildHierByName(Serializable parentId,
            String name, boolean complexProp) throws StorageException {
        SimpleFragment fragment = childrenCache(parentId, complexProp).getFragmentByValue(
                name);
        if (fragment == SimpleFragment.UNKNOWN) {
            // read it through the mapper
            Row row = mapper.readChildHierRow(parentId, name, complexProp);
            fragment = (SimpleFragment) context.getFragmentFromFetchedRow(row,
                    false);
        }
        return fragment;
    }

    /**
     * Gets the list of children main fragments for a given parent id.
     * <p>
     * Complex properties and children of ordered folders are returned in the
     * proper order.
     *
     * @param parentId the parent id
     * @param name the name of the children, or {@code null} for all
     * @param complexProp whether to get complex properties or regular children
     * @return the list of children main fragments
     */
    public List<SimpleFragment> getChildren(Serializable parentId, String name,
            boolean complexProp) throws StorageException {
        Children children = childrenCache(parentId, complexProp);
        List<SimpleFragment> fragments = children.getFragmentsByValue(name);
        if (fragments == null) {
            // no complete list is known
            // ask the actual children to the mapper
            List<Row> rows = mapper.readChildHierRows(parentId, complexProp);
            List<Fragment> frags = context.getFragmentsFromFetchedRows(rows,
                    false);
            fragments = new ArrayList<SimpleFragment>(frags.size());
            List<Serializable> ids = new ArrayList<Serializable>(frags.size());
            for (Fragment fragment : frags) {
                fragments.add((SimpleFragment) fragment);
                ids.add(fragment.getId());
            }
            children.addExistingComplete(ids);

            // redo the query, as the children may include newly-created ones,
            // and we also filter by name
            fragments = children.getFragmentsByValue(name);
        }
        if (isOrderable(parentId, complexProp)) {
            // sort children in order
            Collections.sort(fragments, posComparator);
        }
        return fragments;
    }

    protected boolean isOrderable(Serializable parentId, boolean complexProp)
            throws StorageException {
        if (complexProp) {
            return true;
        }
        SimpleFragment parent = getHier(parentId, true);
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
    public Serializable getContainingDocument(Serializable id)
            throws StorageException {
        Serializable pid = id;
        while (true) {
            if (pid == null) {
                // no parent
                return null;
            }
            SimpleFragment p = getHier(pid, false);
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

    /** Checks that we don't move/copy under ourselves. */
    protected void checkNotUnder(Serializable parentId, Serializable id,
            String op) throws StorageException {
        Serializable pid = parentId;
        do {
            if (pid.equals(id)) {
                throw new StorageException("Cannot " + op
                        + " a node under itself: " + parentId + " is under "
                        + id);
            }
            SimpleFragment p = getHier(pid, false);
            if (p == null) {
                // cannot happen
                throw new StorageException("No parent: " + pid);
            }
            pid = p.get(model.HIER_PARENT_KEY);
        } while (pid != null);
    }

    /** Checks that a name is free. Cannot check concurrent sessions though. */
    protected void checkFreeName(Serializable parentId, String name,
            boolean complexProp) throws StorageException {
        Fragment fragment = getChildHierByName(parentId, name, complexProp);
        if (fragment != null) {
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
     */
    public Long getNextPos(Serializable nodeId, boolean complexProp)
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
    public void move(Node source, Serializable parentId, String name)
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
        checkFreeName(parentId, name, complexProp);
        /*
         * Do the move.
         */
        if (!oldName.equals(name)) {
            hierFragment.put(model.HIER_CHILD_NAME_KEY, name);
        }
        removeChild(hierFragment, complexProp);
        hierFragment.put(model.HIER_PARENT_KEY, parentId);
        addExistingChild(hierFragment, complexProp, true);
    }

    /**
     * Copy a child to a new parent with a new name.
     *
     * @param source the source of the copy
     * @param parentId the destination parent id
     * @param name the new name
     * @return the id of the copy
     */
    public Serializable copy(Node source, Serializable parentId, String name)
            throws StorageException {
        Serializable id = source.getId();
        SimpleFragment hierFragment = source.getHierFragment();
        Serializable oldParentId = hierFragment.get(model.HIER_PARENT_KEY);
        if (!oldParentId.equals(parentId)) {
            checkNotUnder(parentId, id, "copy");
        }
        checkFreeName(parentId, name, complexProp(hierFragment));
        // do the copy

        CopyHierarchyResult res = mapper.copyHierarchy(new IdWithTypes(source),
                parentId, name, null);
        Serializable newId = res.copyId;
        context.markInvalidated(res.invalidations);
        // adds it as a new child of its parent:
        getHier(newId, false);
        return newId;
    }

    public void removeNode(Fragment hierFragment) throws StorageException {
        if (hierFragment.getState() == State.CREATED) {
            // only case where we can recurse in children,
            // it's safe to do as they're in memory as well
            Serializable id = hierFragment.getId();
            for (SimpleFragment f : getChildren(id, null, true)) {
                context.removeNode(f);
            }
            for (SimpleFragment f : getChildren(id, null, false)) {
                context.removeNode(f);
            }
        }
        // We cannot recursively delete the children from the cache as we don't
        // know all their ids and it would be costly to obtain them. Instead we
        // do a check on getNodeById using isDeleted() to see if there's a
        // deleted parent.
    }

    /** Deletes a fragment from the context. */
    public void removeFragment(Fragment fragment) throws StorageException {
        if (!model.HIER_TABLE_NAME.equals(fragment.row.tableName)) {
            return;
        }
        removeChild((SimpleFragment) fragment,
                complexProp((SimpleFragment) fragment));
    }

    public void postSave() {
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

    // called by Children
    public SimpleFragment getHierIfPresent(Serializable id) {
        RowId rowId = new RowId(model.HIER_TABLE_NAME, id);
        return (SimpleFragment) context.getIfPresent(rowId);
    }

    // also called by Children
    public SimpleFragment getHier(Serializable id, boolean allowAbsent)
            throws StorageException {
        RowId rowId = new RowId(model.HIER_TABLE_NAME, id);
        return (SimpleFragment) context.get(rowId, allowAbsent);
    }

    public void recordFragment(Fragment fragment) throws StorageException {
        if (!model.HIER_TABLE_NAME.equals(fragment.row.tableName)) {
            return;
        }
        // add as a child of its parent
        addExistingChild((SimpleFragment) fragment,
                complexProp((SimpleFragment) fragment), false);
    }

    /** Recursively checks if any of a fragment's parents has been deleted. */
    // needed because we don't recursively clear caches when doing a delete
    public boolean isDeleted(Serializable id) throws StorageException {
        while (id != null) {
            SimpleFragment fragment = getHier(id, false);
            State state;
            if (fragment == null
                    || (state = fragment.getState()) == State.ABSENT
                    || state == State.DELETED
                    || state == State.INVALIDATED_DELETED) {
                return true;
            }
            id = fragment.get(model.HIER_PARENT_KEY);
        }
        return false;
    }

    /**
     * Checks in a node (creates a version).
     *
     * @param node the node to check in
     * @param label the version label
     * @param checkinComment the version description
     * @return the created version id
     */
    public Serializable checkIn(Node node, String label, String checkinComment)
            throws StorageException {
        Boolean checkedIn = (Boolean) node.hierFragment.get(model.MAIN_CHECKED_IN_KEY);
        if (Boolean.TRUE.equals(checkedIn)) {
            throw new StorageException("Already checked in");
        }
        if (label == null) {
            // use version major + minor as label
            try {
                Serializable major = node.getSimpleProperty(
                        model.MAIN_MAJOR_VERSION_PROP).getValue();
                Serializable minor = node.getSimpleProperty(
                        model.MAIN_MINOR_VERSION_PROP).getValue();
                if (major == null || minor == null) {
                    label = "";
                } else {
                    label = major + "." + minor;
                }
            } catch (StorageException e) {
                log.error("Cannot get version", e);
                label = "";
            }
        }

        /*
         * Do the copy without non-complex children, with null parent.
         */
        Serializable id = node.getId();
        CopyHierarchyResult res = mapper.copyHierarchy(new IdWithTypes(node),
                null, null, null);
        Serializable newId = res.copyId;
        context.markInvalidated(res.invalidations);
        // add version as a new child of its parent
        SimpleFragment verHier = getHier(newId, false);
        verHier.put(model.MAIN_IS_VERSION_KEY, Boolean.TRUE);
        boolean isMajor = Long.valueOf(0).equals(
                verHier.get(model.MAIN_MINOR_VERSION_KEY));

        // create a "version" row for our new version
        Row row = new Row(model.VERSION_TABLE_NAME, newId);
        row.putNew(model.VERSION_VERSIONABLE_KEY, id);
        row.putNew(model.VERSION_CREATED_KEY, new GregorianCalendar()); // now
        row.putNew(model.VERSION_LABEL_KEY, label);
        row.putNew(model.VERSION_DESCRIPTION_KEY, checkinComment);
        row.putNew(model.VERSION_IS_LATEST_KEY, Boolean.TRUE);
        row.putNew(model.VERSION_IS_LATEST_MAJOR_KEY, Boolean.valueOf(isMajor));
        context.createSimpleFragment(row);

        // update the original node to reflect that it's checked in
        node.hierFragment.put(model.MAIN_CHECKED_IN_KEY, Boolean.TRUE);
        node.hierFragment.put(model.MAIN_BASE_VERSION_KEY, newId);

        recomputeVersionSeries(id);

        return newId;
    }

    // recompute isLatest / isLatestMajor on all versions
    protected void recomputeVersionSeries(Serializable versionSeriesId)
            throws StorageException {
        session.flush(); // needed by following search
        List<Fragment> versFrags = context.getVersionFragments(versionSeriesId);
        Collections.reverse(versFrags);
        boolean isLatest = true;
        boolean isLatestMajor = true;
        for (Fragment vf : versFrags) {
            SimpleFragment vsf = (SimpleFragment) vf;

            // isLatestVersion
            vsf.put(model.VERSION_IS_LATEST_KEY, Boolean.valueOf(isLatest));
            isLatest = false;

            // isLatestMajorVersion
            SimpleFragment vh = getHier(vsf.getId(), true);
            boolean isMajor = Long.valueOf(0).equals(
                    vh.get(model.MAIN_MINOR_VERSION_KEY));
            vsf.put(model.VERSION_IS_LATEST_MAJOR_KEY,
                    Boolean.valueOf(isMajor && isLatestMajor));
            if (isMajor) {
                isLatestMajor = false;
            }
        }
    }

    /**
     * Checks out a node.
     *
     * @param node the node to check out
     */
    public void checkOut(Node node) throws StorageException {
        Boolean checkedIn = (Boolean) node.hierFragment.get(model.MAIN_CHECKED_IN_KEY);
        if (!Boolean.TRUE.equals(checkedIn)) {
            throw new StorageException("Already checked out");
        }
        // update the node to reflect that it's checked out
        node.hierFragment.put(model.MAIN_CHECKED_IN_KEY, Boolean.FALSE);
    }

    /**
     * Restores a node to a given version.
     * <p>
     * The restored node is checked in.
     *
     * @param node the node
     * @param version the version to restore on this node
     */
    public void restoreVersion(Node node, Node version) throws StorageException {
        Serializable versionableId = node.getId();
        Serializable versionId = version.getId();

        // clear complex properties
        List<SimpleFragment> children = getChildren(versionableId, null, true);
        // copy to avoid concurrent modifications
        for (Fragment child : children.toArray(new Fragment[children.size()])) {
            context.removeFragment(child); // will cascade deletes
        }
        session.flush(); // flush deletes

        // copy the version values
        Row overwriteRow = new Row(model.HIER_TABLE_NAME, versionableId);
        SimpleFragment versionHier = version.getHierFragment();
        for (String key : model.getFragmentKeysType(model.HIER_TABLE_NAME).keySet()) {
            // keys we don't copy from version when restoring
            if (key.equals(model.HIER_PARENT_KEY)
                    || key.equals(model.HIER_CHILD_NAME_KEY)
                    || key.equals(model.HIER_CHILD_POS_KEY)
                    || key.equals(model.HIER_CHILD_ISPROPERTY_KEY)
                    || key.equals(model.MAIN_PRIMARY_TYPE_KEY)
                    || key.equals(model.MAIN_CHECKED_IN_KEY)
                    || key.equals(model.MAIN_BASE_VERSION_KEY)
                    || key.equals(model.MAIN_IS_VERSION_KEY)) {
                continue;
            }
            overwriteRow.putNew(key, versionHier.get(key));
        }
        overwriteRow.putNew(model.MAIN_CHECKED_IN_KEY, Boolean.TRUE);
        overwriteRow.putNew(model.MAIN_BASE_VERSION_KEY, versionId);
        overwriteRow.putNew(model.MAIN_IS_VERSION_KEY, null);
        CopyHierarchyResult res = mapper.copyHierarchy(
                new IdWithTypes(version), node.getParentId(), null,
                overwriteRow);
        context.markInvalidated(res.invalidations);
    }

    /**
     * Marks locally all the invalidations gathered by a {@link Mapper}
     * operation (like a version restore).
     */
    public void markInvalidated(Set<RowId> modified) {
        for (RowId rowId : modified) {
            if (Invalidations.PARENT.equals(rowId.tableName)) {
                Serializable parentId = rowId.id;
                for (Map<Serializable, Children> map : childrenAllMaps) {
                    Children children = map.get(parentId);
                    if (children != null) {
                        children.setIncomplete();
                    }
                }
                modifiedParentsInTransaction.add(parentId);
            }
        }
    }

    /**
     * Gathers invalidations from this session.
     * <p>
     * Called post-transaction to gathers invalidations to be sent to others.
     */
    public void gatherInvalidations(Invalidations invalidations) {
        for (Serializable id : modifiedParentsInTransaction) {
            invalidations.addModified(new RowId(Invalidations.PARENT, id));
        }
        modifiedParentsInTransaction.clear();
    }

    /**
     * Processes all invalidations accumulated.
     * <p>
     * Called pre-transaction.
     */
    public void processReceivedInvalidations(Set<RowId> modified)
            throws StorageException {
        for (RowId rowId : modified) {
            if (Invalidations.PARENT.equals(rowId.tableName)) {
                Serializable parentId = rowId.id;
                for (Map<Serializable, Children> map : childrenAllMaps) {
                    map.remove(parentId);
                }
            }
        }
    }

}
