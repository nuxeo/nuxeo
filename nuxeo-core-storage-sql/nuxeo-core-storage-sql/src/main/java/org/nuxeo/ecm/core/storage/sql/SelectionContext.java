/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.map.ReferenceMap;
import org.nuxeo.ecm.core.storage.StorageException;

/**
 * A {@link SelectionContext} holds information for a set {@link Selection}
 * objects, mostly acting as a cache.
 * <p>
 * Some of the information is identical to what's in the database and can be
 * safely be GC'ed, so it lives in a memory-sensitive map (softMap), otherwise
 * it's moved to a normal map (hardMap) (creation or deletion).
 */
public class SelectionContext {

    private final SelectionType selType;

    private final Serializable criterion;

    private final RowMapper mapper;

    private final PersistenceContext context;

    private final Map<Serializable, Selection> softMap;

    // public because used from unit tests
    public final Map<Serializable, Selection> hardMap;

    /**
     * The selections modified in the transaction, that should be propagated as
     * invalidations to other sessions at post-commit time.
     */
    private final Set<Serializable> modifiedInTransaction;

    @SuppressWarnings("unchecked")
    public SelectionContext(SelectionType selType, Serializable criterion,
            RowMapper mapper, PersistenceContext context) {
        this.selType = selType;
        this.criterion = criterion;
        this.mapper = mapper;
        this.context = context;
        softMap = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        hardMap = new HashMap<Serializable, Selection>();
        modifiedInTransaction = new HashSet<Serializable>();
    }

    public int clearCaches() {
        // only the soft selections are caches, the others hold info
        int n = softMap.size();
        softMap.clear();
        modifiedInTransaction.clear();
        return n;
    }

    /**
     * Reads from the mapper a selection row given a filter value.
     *
     * @param id the selection id
     * @param filter the value to filter on
     * @return the row
     */
    protected Row readSelectionRow(Serializable id, Serializable filter)
            throws StorageException {
        List<Row> rows = mapper.readSelectionRows(selType, id, filter,
                criterion, true);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * Reads from the mapper all the selection rows.
     *
     * @param id the selection id
     * @return the list of rows
     */
    protected List<Row> readSelectionRows(Serializable id)
            throws StorageException {
        return mapper.readSelectionRows(selType, id, null, criterion, false);
    }

    /** Gets the proper selection cache. Creates one if missing. */
    protected Selection getSelection(Serializable id) {
        Selection selection = softMap.get(id);
        if (selection != null) {
            return selection;
        }
        selection = hardMap.get(id);
        if (selection != null) {
            return selection;
        }
        return new Selection(id, selType.tableName, false, selType.filterKey,
                context, softMap, hardMap);
    }

    protected void addExisting(SimpleFragment fragment, boolean invalidate)
            throws StorageException {
        Serializable id = fragment.get(selType.selKey);
        if (id == null) {
            return;
        }
        getSelection(id).addExisting(fragment.getId());
        if (invalidate) {
            modifiedInTransaction.add(id);
        }
    }

    protected void addCreated(SimpleFragment fragment) throws StorageException {
        Serializable id = fragment.get(selType.selKey);
        if (id == null) {
            return;
        }
        getSelection(id).addCreated(fragment.getId());
        modifiedInTransaction.add(id);
    }

    protected void remove(SimpleFragment fragment) throws StorageException {
        Serializable id = fragment.get(selType.selKey);
        if (id == null) {
            return;
        }
        getSelection(id).remove(fragment.getId());
        modifiedInTransaction.add(id);
    }

    /**
     * Records the fragment as a new selection member.
     *
     * @param fragment the fragment
     */
    public void recordFragment(Fragment fragment) throws StorageException {
        if (!selType.tableName.equals(fragment.row.tableName)) {
            return;
        }
        // add existing fragment to its selection
        addExisting((SimpleFragment) fragment, false);
    }

    /**
     * Records the fragment as a just-created selection member, and creates a
     * new selection for it.
     *
     * @param fragment the fragment
     */
    public void createdFragment(SimpleFragment fragment)
            throws StorageException {
        if (!selType.tableName.equals(fragment.row.tableName)) {
            return;
        }
        // add as a new fragment in the selection
        addCreated(fragment);
        // note that this new fragment doesn't have a selection
        Serializable id = fragment.getId();
        new Selection(id, selType.tableName, true, selType.filterKey, context,
                softMap, hardMap);
    }

    /**
     * Find a fragment given its selection id and value.
     * <p>
     * If the fragment is not in the context, fetch it from the mapper.
     *
     * @param id the selection id
     * @param filter the value to filter on
     * @return the fragment, or {@code null} if not found
     */
    public SimpleFragment getSelectionFragment(Serializable id, String filter)
            throws StorageException {
        SimpleFragment fragment = getSelection(id).getFragmentByValue(filter);
        if (fragment == SimpleFragment.UNKNOWN) {
            // read it through the mapper
            Row row = readSelectionRow(id, filter);
            fragment = (SimpleFragment) context.getFragmentFromFetchedRow(row,
                    false);
        }
        return fragment;
    }

    /**
     * Finds all the selection fragments for a given id.
     * <p>
     * No sorting on value is done.
     *
     * @param id the selection id
     * @param filter the value to filter on, or {@code null} for all
     * @return the list of fragments
     */
    public List<SimpleFragment> getSelectionFragments(Serializable id,
            String filter) throws StorageException {
        Selection selection = getSelection(id);
        List<SimpleFragment> fragments = selection.getFragmentsByValue(filter);
        if (fragments == null) {
            // no complete list is known
            // ask the actual selection to the mapper
            List<Row> rows = readSelectionRows(id);
            List<Fragment> frags = context.getFragmentsFromFetchedRows(rows,
                    false);
            fragments = new ArrayList<SimpleFragment>(frags.size());
            List<Serializable> ids = new ArrayList<Serializable>(frags.size());
            for (Fragment fragment : frags) {
                fragments.add((SimpleFragment) fragment);
                ids.add(fragment.getId());
            }
            selection.addExistingComplete(ids);

            // redo the query, as the selection may include newly-created ones,
            // and we also filter by name
            fragments = selection.getFragmentsByValue(filter);
        }
        return fragments;
    }

    /** Deletes a fragment from the context. */
    public void removeFragment(Fragment fragment) throws StorageException {
        if (!selType.tableName.equals(fragment.row.tableName)) {
            return;
        }
        remove((SimpleFragment) fragment);
    }

    public void postSave() {
        // flush selection caches (moves from hard to soft)
        for (Selection selection : hardMap.values()) {
            selection.flush(); // added to soft map
        }
        hardMap.clear();
    }

    /**
     * Marks locally all the invalidations gathered by a {@link Mapper}
     * operation (like a version restore).
     */
    public void markInvalidated(Set<RowId> modified) {
        for (RowId rowId : modified) {
            if (selType.invalidationTableName.equals(rowId.tableName)) {
                Serializable id = rowId.id;
                Selection selection = softMap.get(id);
                if (selection != null) {
                    selection.setIncomplete();
                }
                selection = hardMap.get(id);
                if (selection != null) {
                    selection.setIncomplete();
                }
                modifiedInTransaction.add(id);
            }
        }
    }

    /**
     * Gathers invalidations from this session.
     * <p>
     * Called post-transaction to gathers invalidations to be sent to others.
     */
    public void gatherInvalidations(Invalidations invalidations) {
        for (Serializable id : modifiedInTransaction) {
            invalidations.addModified(new RowId(selType.invalidationTableName, id));
        }
        modifiedInTransaction.clear();
    }

    /**
     * Processes all invalidations accumulated.
     * <p>
     * Called pre-transaction.
     */
    public void processReceivedInvalidations(Set<RowId> modified)
            throws StorageException {
        for (RowId rowId : modified) {
            if (selType.invalidationTableName.equals(rowId.tableName)) {
                Serializable id = rowId.id;
                softMap.remove(id);
                hardMap.remove(id);
            }
        }
    }

}
