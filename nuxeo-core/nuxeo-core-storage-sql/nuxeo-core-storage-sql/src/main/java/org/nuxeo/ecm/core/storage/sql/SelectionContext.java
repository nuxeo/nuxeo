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

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

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

    // @since 5.7
    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final Counter modifiedInTransactionCount;

    protected final Counter cacheHitCount ;

    protected final Timer cacheGetTimer;

    @SuppressWarnings("unchecked")
    public SelectionContext(SelectionType selType, Serializable criterion,
            RowMapper mapper, PersistenceContext context) {
        this.selType = selType;
        this.criterion = criterion;
        this.mapper = mapper;
        this.context = context;
        softMap = new ReferenceMap(AbstractReferenceMap.HARD,
                AbstractReferenceMap.SOFT);
        hardMap = new HashMap<Serializable, Selection>();
        modifiedInTransaction = new HashSet<Serializable>();
        modifiedInTransactionCount = registry.counter(MetricRegistry.name(
                "nuxeo", "repositories", context.session.repository.getName(), "caches", "selections", "modified"));
        cacheHitCount = registry.counter(MetricRegistry.name(
                "nuxeo", "repositories", context.session.repository.getName(), "caches", "selections", "hit"));
        cacheGetTimer = registry.timer(MetricRegistry.name(
                "nuxeo", "repositories", context.session.repository.getName(), "caches", "selections", "get"));
    }

    public int clearCaches() {
        // only the soft selections are caches, the others hold info
        int n = softMap.size();
        softMap.clear();
        modifiedInTransactionCount.dec(modifiedInTransaction.size());
        modifiedInTransaction.clear();
        return n;
    }

    public int getSize() {
        return softMap == null ? 0 : softMap.size();
    }

    /** Gets the proper selection cache. Creates one if missing. */
    private Selection getSelection(Serializable selId) {
        final Timer.Context timerContext = cacheGetTimer.time();
        try {
            Selection selection = softMap.get(selId);
            if (selection != null) {
                cacheHitCount.inc();
                return selection;
            }
            selection = hardMap.get(selId);
            if (selection != null) {
                cacheHitCount.inc();
                return selection;
            }
        } finally {
            timerContext.stop();
        }

        return new Selection(selId, selType.tableName, false,
                selType.filterKey, context, softMap, hardMap);
    }

    public boolean applicable(SimpleFragment fragment) throws StorageException {
        // check table name
        if (!fragment.row.tableName.equals(selType.tableName)) {
            return false;
        }
        // check criterion if there's one
        if (selType.criterionKey != null) {
            Serializable crit = fragment.get(selType.criterionKey);
            if (!criterion.equals(crit)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Records the fragment as a just-created selection member.
     */
    public void recordCreated(SimpleFragment fragment) throws StorageException {
        Serializable id = fragment.getId();
        // add as a new fragment in the selection
        Serializable selId = fragment.get(selType.selKey);
        if (selId != null) {
            getSelection(selId).addCreated(id);
            modifiedInTransaction.add(selId);
            modifiedInTransactionCount.inc();
        }
    }

    /**
     * Notes that a new empty selection should be created.
     */
    public void newSelection(Serializable selId) {
        new Selection(selId, selType.tableName, true, selType.filterKey,
                context, softMap, hardMap);
    }

    /**
     * @param invalidate {@code true} if this is for a fragment newly created by
     *            internal database process (copy, etc.) and must notified to
     *            other session; {@code false} if this is a normal read
     */
    public void recordExisting(SimpleFragment fragment, boolean invalidate)
            throws StorageException {
        Serializable selId = fragment.get(selType.selKey);
        if (selId != null) {
            getSelection(selId).addExisting(fragment.getId());
            if (invalidate) {
                modifiedInTransaction.add(selId);
                modifiedInTransactionCount.inc();
            }
        }
    }

    /** Removes a selection item from the selection. */
    public void recordRemoved(SimpleFragment fragment) throws StorageException {
        recordRemoved(fragment.getId(), fragment.get(selType.selKey));
    }

    /** Removes a selection item from the selection. */
    public void recordRemoved(Serializable id, Serializable selId)
            throws StorageException {
        if (selId != null) {
            getSelection(selId).remove(id);
            modifiedInTransaction.add(selId);
            modifiedInTransactionCount.inc();
        }
    }

    /** Records a selection as removed. */
    public void recordRemovedSelection(Serializable selId)
            throws StorageException {
        softMap.remove(selId);
        hardMap.remove(selId);
        modifiedInTransaction.add(selId);
        modifiedInTransactionCount.inc();
    }

    /**
     * Find a fragment given its selection id and value.
     * <p>
     * If the fragment is not in the context, fetch it from the mapper.
     *
     * @param selId the selection id
     * @param filter the value to filter on
     * @return the fragment, or {@code null} if not found
     */
    public SimpleFragment getSelectionFragment(Serializable selId, String filter)
            throws StorageException {
        SimpleFragment fragment = getSelection(selId).getFragmentByValue(filter);
        if (fragment == SimpleFragment.UNKNOWN) {
            // read it through the mapper
            List<Row> rows = mapper.readSelectionRows(selType, selId, filter,
                    criterion, true);
            Row row = rows.isEmpty() ? null : rows.get(0);
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
     * @param selId the selection id
     * @param filter the value to filter on, or {@code null} for all
     * @return the list of fragments
     */
    public List<SimpleFragment> getSelectionFragments(Serializable selId,
            String filter) throws StorageException {
        Selection selection = getSelection(selId);
        List<SimpleFragment> fragments = selection.getFragmentsByValue(filter);
        if (fragments == null) {
            // no complete list is known
            // ask the actual selection to the mapper
            List<Row> rows = mapper.readSelectionRows(selType, selId, null,
                    criterion, false);
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
                modifiedInTransactionCount.inc();
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
            invalidations.addModified(new RowId(selType.invalidationTableName,
                    id));
        }
        modifiedInTransactionCount.dec(modifiedInTransaction.size());
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
