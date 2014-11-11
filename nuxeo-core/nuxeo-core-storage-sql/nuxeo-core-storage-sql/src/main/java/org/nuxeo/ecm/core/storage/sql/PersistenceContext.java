/*
 * (C) Copyright 2007-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Fragment.State;
import org.nuxeo.ecm.core.storage.sql.Invalidations.InvalidationsPair;
import org.nuxeo.ecm.core.storage.sql.RowMapper.RowBatch;
import org.nuxeo.ecm.core.storage.sql.RowMapper.RowUpdate;

/**
 * This class holds persistence context information.
 * <p>
 * All non-saved modified data is referenced here. At save time, the data is
 * sent to the database by the {@link Mapper}. The database will at some time
 * later be committed by the external transaction manager in effect.
 * <p>
 * Internally a fragment can be in at most one of the "pristine" or "modified"
 * map. After a save() all the fragments are pristine, and may be partially
 * invalidated after commit by other local or clustered contexts that committed
 * too.
 * <p>
 * Depending on the table, the context may hold {@link SimpleFragment}s, which
 * represent one row, {@link CollectionFragment}s, which represent several rows.
 * <p>
 * This class is not thread-safe, it should be tied to a single session and the
 * session itself should not be used concurrently.
 */
public class PersistenceContext {

    private static final Log log = LogFactory.getLog(PersistenceContext.class);

    protected final Model model;

    // protected because accessed by Fragment.refetch()
    protected final RowMapper mapper;

    private final SessionImpl session;

    // public because used by unit tests
    public final HierarchyContext hierContext;

    /**
     * The pristine fragments. All held data is identical to what is present in
     * the database and could be refetched if needed.
     * <p>
     * This contains fragment that are {@link State#PRISTINE} or
     * {@link State#ABSENT}, or in some cases {@link State#INVALIDATED_MODIFIED}
     * or {@link State#INVALIDATED_DELETED}.
     * <p>
     * Pristine fragments must be kept here when referenced by the application,
     * because the application must get the same fragment object if asking for
     * it twice, even in two successive transactions.
     * <p>
     * This is memory-sensitive, a fragment can always be refetched if nobody
     * uses it and the GC collects it. Use a weak reference for the values, we
     * don't hold them longer than they need to be referenced, as the underlying
     * mapper also has its own cache.
     */
    protected final Map<RowId, Fragment> pristine;

    /**
     * The fragments changed by the session.
     * <p>
     * This contains fragment that are {@link State#CREATED},
     * {@link State#MODIFIED} or {@link State#DELETED}.
     */
    protected final Map<RowId, Fragment> modified;

    /**
     * Fragment ids generated but not yet saved. We know that any fragment with
     * one of these ids cannot exist in the database.
     */
    private final Set<Serializable> createdIds;

    @SuppressWarnings("unchecked")
    public PersistenceContext(Model model, RowMapper mapper, SessionImpl session)
            throws StorageException {
        this.model = model;
        this.mapper = mapper;
        this.session = session;
        hierContext = new HierarchyContext(model, mapper, session, this);

        // use a weak reference for the values, we don't hold them longer than
        // they need to be referenced, as the underlying mapper also has its own
        // cache
        pristine = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.WEAK);
        modified = new HashMap<RowId, Fragment>();
        // this has to be linked to keep creation order, as foreign keys
        // are used and need this
        createdIds = new LinkedHashSet<Serializable>();
    }

    protected int clearCaches() {
        mapper.clearCache();
        hierContext.clearCaches();
        // TODO there should be a synchronization here
        // but this is a rare operation and we don't call
        // it if a transaction is in progress
        int n = pristine.size();
        pristine.clear();
        modified.clear(); // not empty when rolling back before save
        createdIds.clear();
        return n;
    }

    /**
     * Generates a new id, or used a pre-generated one (import).
     */
    protected Serializable generateNewId(Serializable id) {
        if (id == null) {
            id = model.generateNewId();
        }
        createdIds.add(id);
        return id;
    }

    protected boolean isIdNew(Serializable id) {
        return createdIds.contains(id);
    }

    /**
     * Saves all the created, modified and deleted rows into a batch object, for
     * later execution.
     */
    protected RowBatch getSaveBatch() throws StorageException {
        RowBatch batch = new RowBatch();

        // created main rows are saved first in the batch (in their order of
        // creation), because they are used as foreign keys in all other tables
        for (Serializable id : createdIds) {
            RowId rowId = new RowId(model.HIER_TABLE_NAME, id);
            Fragment fragment = modified.remove(rowId);
            if (fragment == null) {
                // was created and deleted before save
                continue;
            }
            batch.creates.add(fragment.row);
            fragment.clearDirty();
            fragment.setPristine();
            pristine.put(rowId, fragment);
        }
        createdIds.clear();

        // save the rest
        for (Entry<RowId, Fragment> en : modified.entrySet()) {
            RowId rowId = en.getKey();
            Fragment fragment = en.getValue();
            switch (fragment.getState()) {
            case CREATED:
                batch.creates.add(fragment.row);
                fragment.clearDirty();
                fragment.setPristine();
                // modified map cleared at end of loop
                pristine.put(rowId, fragment);
                break;
            case MODIFIED:
                if (fragment.row.isCollection()) {
                    if (((CollectionFragment) fragment).isDirty()) {
                        batch.updates.add(new RowUpdate(fragment.row, null));
                        fragment.clearDirty();
                    }
                } else {
                    Collection<String> keys = ((SimpleFragment) fragment).getDirtyKeys();
                    if (!keys.isEmpty()) {
                        batch.updates.add(new RowUpdate(fragment.row, keys));
                        fragment.clearDirty();
                    }
                }
                fragment.setPristine();
                // modified map cleared at end of loop
                pristine.put(rowId, fragment);
                break;
            case DELETED:
                // TODO deleting non-hierarchy fragments is done by the database
                // itself as their foreign key to hierarchy is ON DELETE CASCADE
                batch.deletes.add(new RowId(rowId));
                fragment.setDetached();
                // modified map cleared at end of loop
                break;
            case PRISTINE:
                // cannot happen, but has been observed :(
                log.error("Found PRISTINE fragment in modified map: "
                        + fragment);
                break;
            default:
                throw new RuntimeException(fragment.toString());
            }
        }
        modified.clear();

        // flush children caches
        hierContext.postSave();

        return batch;
    }

    protected Serializable getContainingDocument(Serializable id)
            throws StorageException {
        return hierContext.getContainingDocument(id);
    }

    /**
     * Finds the documents having dirty text or dirty binaries that have to be
     * reindexed as fulltext.
     *
     * @param dirtyStrings set of ids, updated by this method
     * @param dirtyBinaries set of ids, updated by this method
     */
    protected void findDirtyDocuments(Set<Serializable> dirtyStrings,
            Set<Serializable> dirtyBinaries) throws StorageException {
        for (Fragment fragment : modified.values()) {
            Serializable docId = null;
            switch (fragment.getState()) {
            case CREATED:
                docId = getContainingDocument(fragment.getId());
                dirtyStrings.add(docId);
                dirtyBinaries.add(docId);
                break;
            case MODIFIED:
                String tableName = fragment.row.tableName;
                Collection<String> keys;
                if (model.isCollectionFragment(tableName)) {
                    keys = Collections.singleton(null);
                } else {
                    keys = ((SimpleFragment) fragment).getDirtyKeys();
                }
                for (String key : keys) {
                    PropertyType type = model.getFulltextFieldType(tableName,
                            key);
                    if (type == null) {
                        continue;
                    }
                    if (docId == null) {
                        docId = getContainingDocument(fragment.getId());
                    }
                    if (type == PropertyType.STRING) {
                        dirtyStrings.add(docId);
                    } else if (type == PropertyType.BINARY) {
                        dirtyBinaries.add(docId);
                    }
                }
                break;
            default:
            }
        }
    }

    /**
     * Marks locally all the invalidations gathered by a {@link Mapper}
     * operation (like a version restore).
     */
    protected void markInvalidated(Invalidations invalidations) {
        if (invalidations.modified != null) {
            for (RowId rowId : invalidations.modified) {
                Fragment fragment = getIfPresent(rowId);
                if (fragment != null) {
                    setFragmentPristine(fragment);
                    fragment.setInvalidatedModified();
                }
            }
            hierContext.markInvalidated(invalidations.modified);
        }
        if (invalidations.deleted != null) {
            for (RowId rowId : invalidations.deleted) {
                Fragment fragment = getIfPresent(rowId);
                if (fragment != null) {
                    setFragmentPristine(fragment);
                    fragment.setInvalidatedDeleted();
                }
            }
        }
        // TODO XXX transactionInvalidations.add(invalidations);
    }

    // called from Fragment
    protected void setFragmentModified(Fragment fragment) {
        RowId rowId = fragment.row;
        pristine.remove(rowId);
        modified.put(rowId, fragment);
    }

    // also called from Fragment
    protected void setFragmentPristine(Fragment fragment) {
        RowId rowId = fragment.row;
        modified.remove(rowId);
        pristine.put(rowId, fragment);
    }

    /**
     * Post-transaction invalidations notification.
     * <p>
     * Called post-transaction by session commit/rollback or transactionless
     * save.
     */
    protected void sendInvalidationsToOthers() throws StorageException {
        Invalidations invalidations = new Invalidations();
        hierContext.gatherInvalidations(invalidations);
        mapper.sendInvalidations(invalidations);
        // events sent in mapper
    }

    /**
     * Applies all invalidations accumulated.
     * <p>
     * Called pre-transaction by start or transactionless save;
     */
    protected void processReceivedInvalidations() throws StorageException {
        InvalidationsPair invals = mapper.receiveInvalidations();
        if (invals == null) {
            return;
        }

        processCacheInvalidations(invals.cacheInvalidations);

        session.sendInvalidationEvent(invals);
    }

    protected void processCacheInvalidations(Invalidations invalidations) throws StorageException {
        if (invalidations == null) {
            return;
        }
        if (invalidations.modified != null) {
            for (RowId rowId : invalidations.modified) {
                Fragment fragment = pristine.remove(rowId);
                if (fragment != null) {
                    fragment.setInvalidatedModified();
                }
            }
            hierContext.processReceivedInvalidations(invalidations.modified);
        }
        if (invalidations.deleted != null) {
            for (RowId rowId : invalidations.deleted) {
                Fragment fragment = pristine.remove(rowId);
                if (fragment != null) {
                    fragment.setInvalidatedDeleted();
                }
            }
        }
    }

    protected void checkInvalidationsConflict() {
        // synchronized (receivedInvalidations) {
        // if (receivedInvalidations.modified != null) {
        // for (RowId rowId : receivedInvalidations.modified) {
        // if (transactionInvalidations.contains(rowId)) {
        // throw new ConcurrentModificationException(
        // "Updating a concurrently modified value: "
        // + new RowId(rowId));
        // }
        // }
        // }
        //
        // if (receivedInvalidations.deleted != null) {
        // for (RowId rowId : receivedInvalidations.deleted) {
        // if (transactionInvalidations.contains(rowId)) {
        // throw new ConcurrentModificationException(
        // "Updating a concurrently deleted value: "
        // + new RowId(rowId));
        // }
        // }
        // }
        // }
    }

    /**
     * Gets a fragment, if present in the context.
     * <p>
     * Called by {@link #get}, and by the {@link Mapper} to reuse known
     * hierarchy fragments in lists of children.
     *
     * @param rowId the fragment id
     * @return the fragment, or {@code null} if not found
     */
    protected Fragment getIfPresent(RowId rowId) {
        Fragment fragment = pristine.get(rowId);
        if (fragment != null) {
            return fragment;
        }
        return modified.get(rowId);
    }

    /**
     * Gets a fragment.
     * <p>
     * If it's not in the context, fetch it from the mapper. If it's not in the
     * database, returns {@code null} or an absent fragment.
     * <p>
     * Deleted fragments may be returned.
     *
     * @param rowId the fragment id
     * @param allowAbsent {@code true} to return an absent fragment as an object
     *            instead of {@code null}
     * @return the fragment, or {@code null} if none is found and {@value
     *         allowAbsent} was {@code false}
     */
    protected Fragment get(RowId rowId, boolean allowAbsent)
            throws StorageException {
        Fragment fragment = getIfPresent(rowId);
        if (fragment == null) {
            fragment = getFromMapper(rowId, allowAbsent);
        }
        // if (fragment != null && fragment.getState() == State.DELETED) {
        // fragment = null;
        // }
        return fragment;
    }

    protected Fragment getFromMapper(RowId rowId, boolean allowAbsent)
            throws StorageException {
        List<Fragment> fragments = getFromMapper(Collections.singleton(rowId),
                allowAbsent);
        return fragments.isEmpty() ? null : fragments.get(0);
    }

    /**
     * Gets a collection of fragments from the mapper. No order is kept between
     * the inputs and outputs.
     * <p>
     * Fragments not found are not returned if {@code allowAbsent} is
     * {@code false}.
     */
    protected List<Fragment> getFromMapper(Collection<RowId> rowIds,
            boolean allowAbsent) throws StorageException {
        List<Fragment> res = new ArrayList<Fragment>(rowIds.size());

        // find fragments we really want to fetch
        List<RowId> todo = new ArrayList<RowId>(rowIds.size());
        for (RowId rowId : rowIds) {
            if (isIdNew(rowId.id)) {
                // the id has not been saved, so nothing exists yet in the
                // database
                // rowId is not a row -> will use an absent fragment
                Fragment fragment = getFragmentFromFetchedRow(rowId,
                        allowAbsent);
                if (fragment != null) {
                    res.add(fragment);
                }
            } else {
                todo.add(rowId);
            }
        }
        if (todo.isEmpty()) {
            return res;
        }

        // fetch these fragments in bulk
        List<? extends RowId> rows = mapper.read(todo);
        res.addAll(getFragmentsFromFetchedRows(rows, allowAbsent));

        return res;
    }

    /**
     * Gets a list of fragments.
     * <p>
     * If a fragment is not in the context, fetch it from the mapper. If it's
     * not in the database, use an absent fragment or skip it.
     * <p>
     * Deleted fragments are skipped.
     *
     * @param id the fragment id
     * @param allowAbsent {@code true} to return an absent fragment as an object
     *            instead of skipping it
     * @return the fragments, in arbitrary order (no {@code null}s)
     */
    protected List<Fragment> getMulti(Collection<RowId> rowIds,
            boolean allowAbsent) throws StorageException {
        if (rowIds.isEmpty()) {
            return Collections.emptyList();
        }

        // find those already in the context
        List<Fragment> res = new ArrayList<Fragment>(rowIds.size());
        List<RowId> todo = new LinkedList<RowId>();
        for (RowId rowId : rowIds) {
            Fragment fragment = getIfPresent(rowId);
            if (fragment == null) {
                todo.add(rowId);
            } else {
                if (fragment.getState() != State.DELETED) {
                    res.add(fragment);
                }
            }
        }
        if (todo.isEmpty()) {
            return res;
        }

        // fetch missing ones, return union
        List<Fragment> fetched = getFromMapper(todo, allowAbsent);
        res.addAll(fetched);
        return res;
    }

    /**
     * Turns the given rows (just fetched from the mapper) into fragments and
     * record them in the context.
     * <p>
     * For each row, if the context already contains a fragment with the given
     * id, it is returned instead of building a new one.
     * <p>
     * Deleted fragments are skipped.
     * <p>
     * If a simple {@link RowId} is passed, it means that an absent row was
     * found by the mapper. An absent fragment will be returned, unless
     * {@code allowAbsent} is {@code false} in which case it will be skipped.
     *
     * @param rowIds the list of rows or row ids
     * @param allowAbsent {@code true} to return an absent fragment as an object
     *            instead of {@code null}
     * @return the list of fragments
     */
    protected List<Fragment> getFragmentsFromFetchedRows(
            List<? extends RowId> rowIds, boolean allowAbsent)
            throws StorageException {
        List<Fragment> fragments = new ArrayList<Fragment>(rowIds.size());
        for (RowId rowId : rowIds) {
            Fragment fragment = getFragmentFromFetchedRow(rowId, allowAbsent);
            if (fragment != null) {
                fragments.add(fragment);
            }
        }
        return fragments;
    }

    /**
     * Turns the given row (just fetched from the mapper) into a fragment and
     * record it in the context.
     * <p>
     * If the context already contains a fragment with the given id, it is
     * returned instead of building a new one.
     * <p>
     * If the fragment was deleted, {@code null} is returned.
     * <p>
     * If a simple {@link RowId} is passed, it means that an absent row was
     * found by the mapper. An absent fragment will be returned, unless
     * {@code allowAbsent} is {@code false} in which case {@code null} will be
     * returned.
     *
     * @param rowId the row or row id (may be {@code null})
     * @param allowAbsent {@code true} to return an absent fragment as an object
     *            instead of {@code null}
     * @return the fragment, or {@code null} if it was deleted
     */
    protected Fragment getFragmentFromFetchedRow(RowId rowId,
            boolean allowAbsent) throws StorageException {
        if (rowId == null) {
            return null;
        }
        Fragment fragment = getIfPresent(rowId);
        if (fragment != null) {
            // row is already known in the context, use it
            State state = fragment.getState();
            if (state == State.DELETED) {
                // row has been deleted in the context, ignore it
                return null;
            } else if (state == State.INVALIDATED_MODIFIED
                    || state == State.INVALIDATED_DELETED) {
                // XXX TODO
                throw new IllegalStateException(state.toString());
            } else {
                // keep existing fragment
                return fragment;
            }
        }
        boolean isCollection = model.isCollectionFragment(rowId.tableName);
        if (rowId instanceof Row) {
            Row row = (Row) rowId;
            if (isCollection) {
                fragment = new CollectionFragment(row, State.PRISTINE, this);
            } else {
                fragment = new SimpleFragment(row, State.PRISTINE, this);
            }
            hierContext.recordFragment(fragment);
            return fragment;
        } else {
            if (allowAbsent) {
                if (isCollection) {
                    Serializable[] empty = model.getCollectionFragmentType(
                            rowId.tableName).getEmptyArray();
                    Row row = new Row(rowId.tableName, rowId.id, empty);
                    return new CollectionFragment(row, State.ABSENT, this);
                } else {
                    Row row = new Row(rowId.tableName, rowId.id);
                    return new SimpleFragment(row, State.ABSENT, this);
                }
            } else {
                return null;
            }
        }
    }

    /**
     * Creates a new fragment for a new row, not yet saved.
     *
     * @param row the row
     * @return the created fragment
     * @throws StorageException if the fragment is already in the context
     */
    protected SimpleFragment createSimpleFragment(Row row)
            throws StorageException {
        if (pristine.containsKey(row) || modified.containsKey(row)) {
            throw new StorageException("Row already registered: " + row);
        }
        SimpleFragment fragment = new SimpleFragment(row, State.CREATED, this);
        hierContext.createdSimpleFragment(fragment);
        return fragment;
    }

    protected void removeNode(Fragment hierFragment) throws StorageException {
        hierContext.removeNode(hierFragment);

        // find all the fragments with this id in the maps
        Serializable id = hierFragment.getId();
        List<Fragment> fragments = new LinkedList<Fragment>();
        for (Fragment fragment : pristine.values()) {
            if (id.equals(fragment.getId())) {
                fragments.add(fragment);
            }
        }
        for (Fragment fragment : modified.values()) {
            if (id.equals(fragment.getId())) {
                if (fragment.getState() != State.DELETED) {
                    fragments.add(fragment);
                }
            }
        }
        // remove the fragments
        for (Fragment fragment : fragments) {
            removeFragment(fragment);
        }
    }

    /** Deletes a fragment from the context. */
    protected void removeFragment(Fragment fragment) throws StorageException {
        hierContext.removeFragment(fragment);

        RowId rowId = fragment.row;
        switch (fragment.getState()) {
        case ABSENT:
        case INVALIDATED_DELETED:
            pristine.remove(rowId);
            break;
        case CREATED:
            modified.remove(rowId);
            break;
        case PRISTINE:
        case INVALIDATED_MODIFIED:
            pristine.remove(rowId);
            modified.put(rowId, fragment);
            break;
        case MODIFIED:
            // already in modified
            break;
        case DETACHED:
        case DELETED:
            break;
        }
        fragment.setDeleted();
    }

    public void recomputeVersionSeries(Serializable versionSeriesId)
            throws StorageException {
        hierContext.recomputeVersionSeries(versionSeriesId);
    }

    protected List<Serializable> getVersionIds(Serializable versionSeriesId)
            throws StorageException {
        List<Row> rows = mapper.getVersionRows(versionSeriesId);
        List<Fragment> fragments = getFragmentsFromFetchedRows(rows, false);
        return fragmentsIds(fragments);
    }

    protected List<Fragment> getVersionFragments(Serializable versionSeriesId)
            throws StorageException {
        List<Row> rows = mapper.getVersionRows(versionSeriesId);
        return getFragmentsFromFetchedRows(rows, false);
    }

    protected List<Serializable> getProxyIds(Serializable searchId,
            boolean byTarget, Serializable parentId) throws StorageException {
        List<Row> rows = mapper.getProxyRows(searchId, byTarget, parentId);
        List<Fragment> fragments = getFragmentsFromFetchedRows(rows, false);
        return fragmentsIds(fragments);
    }

    private List<Serializable> fragmentsIds(List<Fragment> fragments) {
        List<Serializable> ids = new ArrayList<Serializable>(fragments.size());
        for (Fragment fragment : fragments) {
            ids.add(fragment.getId());
        }
        return ids;
    }

    /*
     * ----- Pass-through to HierarchyContext -----
     */

    protected boolean isDeleted(Serializable id) throws StorageException {
        return hierContext.isDeleted(id);
    }

    protected Long getNextPos(Serializable nodeId, boolean complexProp)
            throws StorageException {
        return hierContext.getNextPos(nodeId, complexProp);
    }

    protected void orderBefore(Serializable parentId, Serializable sourceId,
            Serializable destId) throws StorageException {
        hierContext.orderBefore(parentId, sourceId, destId);
    }

    protected SimpleFragment getChildHierByName(Serializable parentId,
            String name, boolean complexProp) throws StorageException {
        return hierContext.getChildHierByName(parentId, name, complexProp);
    }

    protected List<SimpleFragment> getChildren(Serializable parentId,
            String name, boolean complexProp) throws StorageException {
        return hierContext.getChildren(parentId, name, complexProp);
    }

    protected void move(Node source, Serializable parentId, String name)
            throws StorageException {
        hierContext.move(source, parentId, name);
    }

    protected Serializable copy(Node source, Serializable parentId, String name)
            throws StorageException {
        return hierContext.copy(source, parentId, name);
    }

    protected Serializable checkIn(Node node, String label,
            String checkinComment) throws StorageException {
        return hierContext.checkIn(node, label, checkinComment);
    }

    protected void checkOut(Node node) throws StorageException {
        hierContext.checkOut(node);
    }

    protected void restoreVersion(Node node, Node version)
            throws StorageException {
        hierContext.restoreVersion(node, version);
    }

}
