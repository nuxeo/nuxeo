/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.DocumentExistsException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.storage.sql.Fragment.State;
import org.nuxeo.ecm.core.storage.sql.RowMapper.CopyResult;
import org.nuxeo.ecm.core.storage.sql.RowMapper.IdWithTypes;
import org.nuxeo.ecm.core.storage.sql.RowMapper.NodeInfo;
import org.nuxeo.ecm.core.storage.sql.RowMapper.RowBatch;
import org.nuxeo.ecm.core.storage.sql.RowMapper.RowUpdate;
import org.nuxeo.ecm.core.storage.sql.SimpleFragment.FieldComparator;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

/**
 * This class holds persistence context information.
 * <p>
 * All non-saved modified data is referenced here. At save time, the data is sent to the database by the {@link Mapper}.
 * The database will at some time later be committed by the external transaction manager in effect.
 * <p>
 * Internally a fragment can be in at most one of the "pristine" or "modified" map. After a save() all the fragments are
 * pristine, and may be partially invalidated after commit by other local or clustered contexts that committed too.
 * <p>
 * Depending on the table, the context may hold {@link SimpleFragment}s, which represent one row,
 * {@link CollectionFragment}s, which represent several rows.
 * <p>
 * This class is not thread-safe, it should be tied to a single session and the session itself should not be used
 * concurrently.
 */
public class PersistenceContext {

    protected static final Log log = LogFactory.getLog(PersistenceContext.class);

    /**
     * Property for threshold at which we warn that a Selection may be too big, with stack trace.
     *
     * @since 7.1
     */
    public static final String SEL_WARN_THRESHOLD_PROP = "org.nuxeo.vcs.selection.warn.threshold";

    public static final String SEL_WARN_THRESHOLD_DEFAULT = "15000";

    protected static final FieldComparator POS_COMPARATOR = new FieldComparator(Model.HIER_CHILD_POS_KEY);

    protected static final FieldComparator VER_CREATED_COMPARATOR = new FieldComparator(Model.VERSION_CREATED_KEY);

    protected final Model model;

    // protected because accessed by Fragment.refetch()
    protected final RowMapper mapper;

    protected final SessionImpl session;

    // selection context for complex properties
    protected final SelectionContext hierComplex;

    // selection context for non-complex properties
    // public because used by unit tests
    public final SelectionContext hierNonComplex;

    // selection context for versions by series
    private final SelectionContext seriesVersions;

    // selection context for proxies by series
    private final SelectionContext seriesProxies;

    // selection context for proxies by target
    private final SelectionContext targetProxies;

    private final List<SelectionContext> selections;

    /**
     * The pristine fragments. All held data is identical to what is present in the database and could be refetched if
     * needed.
     * <p>
     * This contains fragment that are {@link State#PRISTINE} or {@link State#ABSENT}, or in some cases
     * {@link State#INVALIDATED_MODIFIED} or {@link State#INVALIDATED_DELETED}.
     * <p>
     * Pristine fragments must be kept here when referenced by the application, because the application must get the
     * same fragment object if asking for it twice, even in two successive transactions.
     * <p>
     * This is memory-sensitive, a fragment can always be refetched if nobody uses it and the GC collects it. Use a weak
     * reference for the values, we don't hold them longer than they need to be referenced, as the underlying mapper
     * also has its own cache.
     */
    protected final Map<RowId, Fragment> pristine;

    /**
     * The fragments changed by the session.
     * <p>
     * This contains fragment that are {@link State#CREATED}, {@link State#MODIFIED} or {@link State#DELETED}.
     */
    protected final Map<RowId, Fragment> modified;

    /**
     * Fragment ids generated but not yet saved. We know that any fragment with one of these ids cannot exist in the
     * database.
     */
    private final Set<Serializable> createdIds;

    /**
     * Cache statistics
     *
     * @since 5.7
     */
    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final Counter cacheCount;

    protected final Counter cacheHitCount;

    /**
     * Threshold at which we warn that a Selection may be too big, with stack trace.
     */
    protected long bigSelWarnThreshold;

    @SuppressWarnings("unchecked")
    public PersistenceContext(Model model, RowMapper mapper, SessionImpl session) {
        this.model = model;
        this.mapper = mapper;
        this.session = session;
        hierComplex = new SelectionContext(SelectionType.CHILDREN, Boolean.TRUE, mapper, this);
        hierNonComplex = new SelectionContext(SelectionType.CHILDREN, Boolean.FALSE, mapper, this);
        seriesVersions = new SelectionContext(SelectionType.SERIES_VERSIONS, null, mapper, this);
        selections = new ArrayList<>(Arrays.asList(hierComplex, hierNonComplex, seriesVersions));
        if (model.proxiesEnabled) {
            seriesProxies = new SelectionContext(SelectionType.SERIES_PROXIES, null, mapper, this);
            targetProxies = new SelectionContext(SelectionType.TARGET_PROXIES, null, mapper, this);
            selections.add(seriesProxies);
            selections.add(targetProxies);
        } else {
            seriesProxies = null;
            targetProxies = null;
        }

        // use a weak reference for the values, we don't hold them longer than
        // they need to be referenced, as the underlying mapper also has its own
        // cache
        pristine = new ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK);
        modified = new HashMap<>();
        // this has to be linked to keep creation order, as foreign keys
        // are used and need this
        createdIds = new LinkedHashSet<>();
        cacheCount = registry.counter(
                MetricRegistry.name("nuxeo", "repositories", session.getRepositoryName(), "caches", "count"));
        cacheHitCount = registry.counter(
                MetricRegistry.name("nuxeo", "repositories", session.getRepositoryName(), "caches", "hit"));
        try {
            bigSelWarnThreshold = Long.parseLong(
                    Framework.getProperty(SEL_WARN_THRESHOLD_PROP, SEL_WARN_THRESHOLD_DEFAULT));
        } catch (NumberFormatException e) {
            log.error("Invalid value for " + SEL_WARN_THRESHOLD_PROP + ": "
                    + Framework.getProperty(SEL_WARN_THRESHOLD_PROP));
        }
    }

    protected int clearCaches() {
        mapper.clearCache();
        // TODO there should be a synchronization here
        // but this is a rare operation and we don't call
        // it if a transaction is in progress
        int n = clearLocalCaches();
        modified.clear(); // not empty when rolling back before save
        createdIds.clear();
        return n;
    }

    protected int clearLocalCaches() {
        for (SelectionContext sel : selections) {
            sel.clearCaches();
        }
        int n = pristine.size();
        pristine.clear();
        return n;
    }

    protected long getCacheSize() {
        return getCachePristineSize() + getCacheSelectionSize() + getCacheMapperSize();
    }

    protected long getCacheMapperSize() {
        return mapper.getCacheSize();
    }

    protected long getCachePristineSize() {
        return pristine.size();
    }

    protected long getCacheSelectionSize() {
        int size = 0;
        for (SelectionContext sel : selections) {
            size += sel.getSize();
        }
        return size;
    }

    /**
     * Generates a new id, or used a pre-generated one (import).
     */
    protected Serializable generateNewId(Serializable id) {
        if (id == null) {
            id = mapper.generateNewId();
        }
        createdIds.add(id);
        return id;
    }

    protected boolean isIdNew(Serializable id) {
        return createdIds.contains(id);
    }

    /**
     * Saves all the created, modified and deleted rows into a batch object, for later execution.
     * <p>
     * Also updates the passed fragmentsToClearDirty list with dirty modified fragments, for later call of clearDirty
     * (it's important to call it later and not now because for delta values we need the delta during batch write, and
     * they are cleared by clearDirty).
     */
    protected RowBatch getSaveBatch(List<Fragment> fragmentsToClearDirty) {
        RowBatch batch = new RowBatch();

        // created main rows are saved first in the batch (in their order of
        // creation), because they are used as foreign keys in all other tables
        for (Serializable id : createdIds) {
            RowId rowId = new RowId(Model.HIER_TABLE_NAME, id);
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
                RowUpdate rowu = fragment.getRowUpdate();
                if (rowu != null) {
                    batch.updates.add(rowu);
                    fragmentsToClearDirty.add(fragment);
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
            case DELETED_DEPENDENT:
                batch.deletesDependent.add(new RowId(rowId));
                fragment.setDetached();
                break;
            case PRISTINE:
                // cannot happen, but has been observed :(
                log.error("Found PRISTINE fragment in modified map: " + fragment);
                break;
            default:
                throw new RuntimeException(fragment.toString());
            }
        }
        modified.clear();

        // flush selections caches
        for (SelectionContext sel : selections) {
            sel.postSave();
        }

        return batch;
    }

    private boolean complexProp(SimpleFragment fragment) {
        return complexProp((Boolean) fragment.get(Model.HIER_CHILD_ISPROPERTY_KEY));
    }

    private boolean complexProp(Boolean isProperty) {
        return Boolean.TRUE.equals(isProperty);
    }

    private SelectionContext getHierSelectionContext(boolean complexProp) {
        return complexProp ? hierComplex : hierNonComplex;
    }

    /**
     * Finds the documents having dirty text or dirty binaries that have to be reindexed as fulltext.
     *
     * @param dirtyStrings set of ids, updated by this method
     * @param dirtyBinaries set of ids, updated by this method
     */
    protected void findDirtyDocuments(Set<Serializable> dirtyStrings, Set<Serializable> dirtyBinaries) {
        // deleted documents, for which we don't need to reindex anything
        Set<Serializable> deleted = null;
        for (Fragment fragment : modified.values()) {
            Serializable docId = getContainingDocument(fragment.getId());
            String tableName = fragment.row.tableName;
            State state = fragment.getState();
            switch (state) {
            case DELETED:
            case DELETED_DEPENDENT:
                if (Model.HIER_TABLE_NAME.equals(tableName) && fragment.getId().equals(docId)) {
                    // deleting the document, record this
                    if (deleted == null) {
                        deleted = new HashSet<>();
                    }
                    deleted.add(docId);
                }
                if (isDeleted(docId)) {
                    break;
                }
                // this is a deleted fragment of a complex property
                // from a document that has not been completely deleted
                //$FALL-THROUGH$
            case CREATED:
                PropertyType t = model.getFulltextInfoForFragment(tableName);
                if (t == null) {
                    break;
                }
                if (t == PropertyType.STRING || t == PropertyType.BOOLEAN) {
                    dirtyStrings.add(docId);
                }
                if (t == PropertyType.BINARY || t == PropertyType.BOOLEAN) {
                    dirtyBinaries.add(docId);
                }
                break;
            case MODIFIED:
                Collection<String> keys;
                if (model.isCollectionFragment(tableName)) {
                    keys = Collections.singleton(null);
                } else {
                    keys = ((SimpleFragment) fragment).getDirtyKeys();
                }
                for (String key : keys) {
                    PropertyType type = model.getFulltextFieldType(tableName, key);
                    if (type == PropertyType.STRING || type == PropertyType.ARRAY_STRING) {
                        dirtyStrings.add(docId);
                    } else if (type == PropertyType.BINARY || type == PropertyType.ARRAY_BINARY) {
                        dirtyBinaries.add(docId);
                    }
                }
                break;
            default:
            }
            if (deleted != null) {
                dirtyStrings.removeAll(deleted);
                dirtyBinaries.removeAll(deleted);
            }
        }
    }

    /**
     * Marks locally all the invalidations gathered by a {@link Mapper} operation (like a version restore).
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
            for (SelectionContext sel : selections) {
                sel.markInvalidated(invalidations.modified);
            }
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
     * Called post-transaction by session commit/rollback or transactionless save.
     */
    public void sendInvalidationsToOthers() {
        Invalidations invalidations = new Invalidations();
        for (SelectionContext sel : selections) {
            sel.gatherInvalidations(invalidations);
        }
        mapper.sendInvalidations(invalidations);
    }

    /**
     * Applies all invalidations accumulated.
     * <p>
     * Called pre-transaction by start or transactionless save;
     */
    public void processReceivedInvalidations() {
        Invalidations invals = mapper.receiveInvalidations();
        if (invals == null) {
            return;
        }

        processCacheInvalidations(invals);
    }

    private void processCacheInvalidations(Invalidations invalidations) {
        if (invalidations == null) {
            return;
        }
        if (invalidations.all) {
            clearLocalCaches();
        }
        if (invalidations.modified != null) {
            for (RowId rowId : invalidations.modified) {
                Fragment fragment = pristine.remove(rowId);
                if (fragment != null) {
                    fragment.setInvalidatedModified();
                }
            }
            for (SelectionContext sel : selections) {
                sel.processReceivedInvalidations(invalidations.modified);
            }
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

    public void checkInvalidationsConflict() {
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
     * Called by {@link #get}, and by the {@link Mapper} to reuse known selection fragments.
     *
     * @param rowId the fragment id
     * @return the fragment, or {@code null} if not found
     */
    protected Fragment getIfPresent(RowId rowId) {
        cacheCount.inc();
        Fragment fragment = pristine.get(rowId);
        if (fragment == null) {
            fragment = modified.get(rowId);
        }
        if (fragment != null) {
            cacheHitCount.inc();
        }
        return fragment;
    }

    /**
     * Gets a fragment.
     * <p>
     * If it's not in the context, fetch it from the mapper. If it's not in the database, returns {@code null} or an
     * absent fragment.
     * <p>
     * Deleted fragments may be returned.
     *
     * @param rowId the fragment id
     * @param allowAbsent {@code true} to return an absent fragment as an object instead of {@code null}
     * @return the fragment, or {@code null} if none is found and {@value allowAbsent} was {@code false}
     */
    protected Fragment get(RowId rowId, boolean allowAbsent) {
        Fragment fragment = getIfPresent(rowId);
        if (fragment == null) {
            fragment = getFromMapper(rowId, allowAbsent, false);
        }
        return fragment;
    }

    /**
     * Gets a fragment from the context or the mapper cache or the underlying database.
     *
     * @param rowId the fragment id
     * @param allowAbsent {@code true} to return an absent fragment as an object instead of {@code null}
     * @param cacheOnly only check memory, not the database
     * @return the fragment, or when {@code allowAbsent} is {@code false}, a {@code null} if not found
     */
    protected Fragment getFromMapper(RowId rowId, boolean allowAbsent, boolean cacheOnly) {
        List<Fragment> fragments = getFromMapper(Collections.singleton(rowId), allowAbsent, cacheOnly);
        return fragments.isEmpty() ? null : fragments.get(0);
    }

    /**
     * Gets a collection of fragments from the mapper. No order is kept between the inputs and outputs.
     * <p>
     * Fragments not found are not returned if {@code allowAbsent} is {@code false}.
     */
    protected List<Fragment> getFromMapper(Collection<RowId> rowIds, boolean allowAbsent, boolean cacheOnly) {
        List<Fragment> res = new ArrayList<>(rowIds.size());

        // find fragments we really want to fetch
        List<RowId> todo = new ArrayList<>(rowIds.size());
        for (RowId rowId : rowIds) {
            if (isIdNew(rowId.id)) {
                // the id has not been saved, so nothing exists yet in the
                // database
                // rowId is not a row -> will use an absent fragment
                Fragment fragment = getFragmentFromFetchedRow(rowId, allowAbsent);
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
        List<? extends RowId> rows = mapper.read(todo, cacheOnly);
        res.addAll(getFragmentsFromFetchedRows(rows, allowAbsent));

        return res;
    }

    /**
     * Gets a list of fragments.
     * <p>
     * If a fragment is not in the context, fetch it from the mapper. If it's not in the database, use an absent
     * fragment or skip it.
     * <p>
     * Deleted fragments are skipped.
     *
     * @param id the fragment id
     * @param allowAbsent {@code true} to return an absent fragment as an object instead of skipping it
     * @return the fragments, in arbitrary order (no {@code null}s)
     */
    public List<Fragment> getMulti(Collection<RowId> rowIds, boolean allowAbsent) {
        if (rowIds.isEmpty()) {
            return Collections.emptyList();
        }

        // find those already in the context
        List<Fragment> res = new ArrayList<>(rowIds.size());
        List<RowId> todo = new LinkedList<>();
        for (RowId rowId : rowIds) {
            Fragment fragment = getIfPresent(rowId);
            if (fragment == null) {
                todo.add(rowId);
            } else {
                State state = fragment.getState();
                if (state != State.DELETED && state != State.DELETED_DEPENDENT
                        && (state != State.ABSENT || allowAbsent)) {
                    res.add(fragment);
                }
            }
        }
        if (todo.isEmpty()) {
            return res;
        }

        // fetch missing ones, return union
        List<Fragment> fetched = getFromMapper(todo, allowAbsent, false);
        res.addAll(fetched);
        return res;
    }

    /**
     * Turns the given rows (just fetched from the mapper) into fragments and record them in the context.
     * <p>
     * For each row, if the context already contains a fragment with the given id, it is returned instead of building a
     * new one.
     * <p>
     * Deleted fragments are skipped.
     * <p>
     * If a simple {@link RowId} is passed, it means that an absent row was found by the mapper. An absent fragment will
     * be returned, unless {@code allowAbsent} is {@code false} in which case it will be skipped.
     *
     * @param rowIds the list of rows or row ids
     * @param allowAbsent {@code true} to return an absent fragment as an object instead of {@code null}
     * @return the list of fragments
     */
    protected List<Fragment> getFragmentsFromFetchedRows(List<? extends RowId> rowIds, boolean allowAbsent) {
        List<Fragment> fragments = new ArrayList<>(rowIds.size());
        for (RowId rowId : rowIds) {
            Fragment fragment = getFragmentFromFetchedRow(rowId, allowAbsent);
            if (fragment != null) {
                fragments.add(fragment);
            }
        }
        return fragments;
    }

    /**
     * Turns the given row (just fetched from the mapper) into a fragment and record it in the context.
     * <p>
     * If the context already contains a fragment with the given id, it is returned instead of building a new one.
     * <p>
     * If the fragment was deleted, {@code null} is returned.
     * <p>
     * If a simple {@link RowId} is passed, it means that an absent row was found by the mapper. An absent fragment will
     * be returned, unless {@code allowAbsent} is {@code false} in which case {@code null} will be returned.
     *
     * @param rowId the row or row id (may be {@code null})
     * @param allowAbsent {@code true} to return an absent fragment as an object instead of {@code null}
     * @return the fragment, or {@code null} if it was deleted
     */
    protected Fragment getFragmentFromFetchedRow(RowId rowId, boolean allowAbsent) {
        if (rowId == null) {
            return null;
        }
        Fragment fragment = getIfPresent(rowId);
        if (fragment != null) {
            // row is already known in the context, use it
            State state = fragment.getState();
            if (state == State.DELETED || state == State.DELETED_DEPENDENT) {
                // row has been deleted in the context, ignore it
                return null;
            } else if (state == State.INVALIDATED_MODIFIED || state == State.INVALIDATED_DELETED) {
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
                // add to applicable selections
                for (SelectionContext sel : selections) {
                    if (sel.applicable((SimpleFragment) fragment)) {
                        sel.recordExisting((SimpleFragment) fragment, false);
                    }
                }
            }
            return fragment;
        } else {
            if (allowAbsent) {
                if (isCollection) {
                    Serializable[] empty = model.getCollectionFragmentType(rowId.tableName).getEmptyArray();
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

    public SimpleFragment createHierarchyFragment(Row row) {
        SimpleFragment fragment = createSimpleFragment(row);
        SelectionContext hierSel = getHierSelectionContext(complexProp(fragment));
        hierSel.recordCreated(fragment);
        // no children for this new node
        Serializable id = fragment.getId();
        hierComplex.newSelection(id);
        hierNonComplex.newSelection(id);
        // could add to seriesProxies and seriesVersions as well
        return fragment;
    }

    private SimpleFragment createVersionFragment(Row row) {
        SimpleFragment fragment = createSimpleFragment(row);
        seriesVersions.recordCreated(fragment);
        // no proxies for this new version
        if (model.proxiesEnabled) {
            targetProxies.newSelection(fragment.getId());
        }
        return fragment;
    }

    public void createdProxyFragment(SimpleFragment fragment) {
        if (model.proxiesEnabled) {
            seriesProxies.recordCreated(fragment);
            targetProxies.recordCreated(fragment);
        }
    }

    public void removedProxyTarget(SimpleFragment fragment) {
        if (model.proxiesEnabled) {
            targetProxies.recordRemoved(fragment);
        }
    }

    public void addedProxyTarget(SimpleFragment fragment) {
        if (model.proxiesEnabled) {
            targetProxies.recordCreated(fragment);
        }
    }

    private SimpleFragment createSimpleFragment(Row row) {
        if (pristine.containsKey(row) || modified.containsKey(row)) {
            throw new NuxeoException("Row already registered: " + row);
        }
        return new SimpleFragment(row, State.CREATED, this);
    }

    /**
     * Removes a property node and its children.
     * <p>
     * There's less work to do than when we have to remove a generic document node (less selections, and we can assume
     * the depth is small so recurse).
     */
    public void removePropertyNode(SimpleFragment hierFragment) {
        // collect children
        Deque<SimpleFragment> todo = new LinkedList<>();
        List<SimpleFragment> children = new LinkedList<>();
        todo.add(hierFragment);
        while (!todo.isEmpty()) {
            SimpleFragment fragment = todo.removeFirst();
            todo.addAll(getChildren(fragment.getId(), null, true)); // complex
            children.add(fragment);
        }
        Collections.reverse(children);
        // iterate on children depth first
        for (SimpleFragment fragment : children) {
            // remove from context
            boolean primary = fragment == hierFragment;
            removeFragmentAndDependents(fragment, primary);
            // remove from selections
            // removed from its parent selection
            hierComplex.recordRemoved(fragment);
            // no children anymore
            hierComplex.recordRemovedSelection(fragment.getId());
        }
    }

    private void removeFragmentAndDependents(SimpleFragment hierFragment, boolean primary) {
        Serializable id = hierFragment.getId();
        for (String fragmentName : model.getTypeFragments(new IdWithTypes(hierFragment))) {
            RowId rowId = new RowId(fragmentName, id);
            Fragment fragment = get(rowId, true); // may read it
            State state = fragment.getState();
            if (state != State.DELETED && state != State.DELETED_DEPENDENT) {
                removeFragment(fragment, primary && hierFragment == fragment);
            }
        }
    }

    /**
     * Removes a document node and its children.
     * <p>
     * Assumes a full flush was done.
     */
    public void removeNode(SimpleFragment hierFragment) {
        Serializable rootId = hierFragment.getId();

        // get root info before deletion. may be a version or proxy
        SimpleFragment versionFragment;
        SimpleFragment proxyFragment;
        if (Model.PROXY_TYPE.equals(hierFragment.getString(Model.MAIN_PRIMARY_TYPE_KEY))) {
            versionFragment = null;
            proxyFragment = (SimpleFragment) get(new RowId(Model.PROXY_TABLE_NAME, rootId), true);
        } else if (Boolean.TRUE.equals(hierFragment.get(Model.MAIN_IS_VERSION_KEY))) {
            versionFragment = (SimpleFragment) get(new RowId(Model.VERSION_TABLE_NAME, rootId), true);
            proxyFragment = null;
        } else {
            versionFragment = null;
            proxyFragment = null;
        }
        NodeInfo rootInfo = new NodeInfo(hierFragment, versionFragment, proxyFragment);

        // remove with descendants, and generate cache invalidations
        List<NodeInfo> infos = mapper.remove(rootInfo);

        // remove from context and selections
        for (NodeInfo info : infos) {
            Serializable id = info.id;
            for (String fragmentName : model.getTypeFragments(new IdWithTypes(id, info.primaryType, null))) {
                RowId rowId = new RowId(fragmentName, id);
                removedFragment(rowId); // remove from context
            }
            removeFromSelections(info);
        }

        // recompute version series if needed
        // only done for root of deletion as versions are not fileable
        Serializable versionSeriesId = versionFragment == null ? null
                : versionFragment.get(Model.VERSION_VERSIONABLE_KEY);
        if (versionSeriesId != null) {
            recomputeVersionSeries(versionSeriesId);
        }
    }

    /**
     * Remove node from children/proxies selections.
     */
    private void removeFromSelections(NodeInfo info) {
        Serializable id = info.id;
        if (Model.PROXY_TYPE.equals(info.primaryType)) {
            seriesProxies.recordRemoved(id, info.versionSeriesId);
            targetProxies.recordRemoved(id, info.targetId);
        }
        if (info.versionSeriesId != null && info.targetId == null) {
            // version
            seriesVersions.recordRemoved(id, info.versionSeriesId);
        }

        hierComplex.recordRemoved(info.id, info.parentId);
        hierNonComplex.recordRemoved(info.id, info.parentId);

        // remove complete selections
        if (complexProp(info.isProperty)) {
            // no more a parent
            hierComplex.recordRemovedSelection(id);
            // is never a parent of non-complex children
        } else {
            // no more a parent
            hierComplex.recordRemovedSelection(id);
            hierNonComplex.recordRemovedSelection(id);
            // no more a version series
            if (model.proxiesEnabled) {
                seriesProxies.recordRemovedSelection(id);
            }
            seriesVersions.recordRemovedSelection(id);
            // no more a target
            if (model.proxiesEnabled) {
                targetProxies.recordRemovedSelection(id);
            }
        }
    }

    /**
     * Deletes a fragment from the context. May generate a database DELETE if primary is {@code true}, otherwise
     * consider that database removal will be a cascade-induced consequence of another DELETE.
     */
    public void removeFragment(Fragment fragment, boolean primary) {
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
        case DELETED_DEPENDENT:
            break;
        }
        fragment.setDeleted(primary);
    }

    /**
     * Cleans up after a fragment has been removed in the database.
     *
     * @param rowId the row id
     */
    private void removedFragment(RowId rowId) {
        Fragment fragment = getIfPresent(rowId);
        if (fragment == null) {
            return;
        }
        switch (fragment.getState()) {
        case ABSENT:
        case PRISTINE:
        case INVALIDATED_MODIFIED:
        case INVALIDATED_DELETED:
            pristine.remove(rowId);
            break;
        case CREATED:
        case MODIFIED:
        case DELETED:
        case DELETED_DEPENDENT:
            // should not happen
            log.error("Removed fragment is in invalid state: " + fragment);
            modified.remove(rowId);
            break;
        case DETACHED:
            break;
        }
        fragment.setDetached();
    }

    /**
     * Recomputes isLatest / isLatestMajor on all versions.
     */
    public void recomputeVersionSeries(Serializable versionSeriesId) {
        List<SimpleFragment> versFrags = seriesVersions.getSelectionFragments(versionSeriesId, null);
        Collections.sort(versFrags, VER_CREATED_COMPARATOR);
        Collections.reverse(versFrags);
        boolean isLatest = true;
        boolean isLatestMajor = true;
        for (SimpleFragment vsf : versFrags) {

            // isLatestVersion
            vsf.put(Model.VERSION_IS_LATEST_KEY, Boolean.valueOf(isLatest));
            isLatest = false;

            // isLatestMajorVersion
            SimpleFragment vh = getHier(vsf.getId(), true);
            boolean isMajor = Long.valueOf(0).equals(vh.get(Model.MAIN_MINOR_VERSION_KEY));
            vsf.put(Model.VERSION_IS_LATEST_MAJOR_KEY, Boolean.valueOf(isMajor && isLatestMajor));
            if (isMajor) {
                isLatestMajor = false;
            }
        }
    }

    /**
     * Gets the version ids for a version series, ordered by creation time.
     */
    public List<Serializable> getVersionIds(Serializable versionSeriesId) {
        List<SimpleFragment> fragments = seriesVersions.getSelectionFragments(versionSeriesId, null);
        Collections.sort(fragments, VER_CREATED_COMPARATOR);
        return fragmentsIds(fragments);
    }

    // called only when proxies enabled
    public List<Serializable> getSeriesProxyIds(Serializable versionSeriesId) {
        List<SimpleFragment> fragments = seriesProxies.getSelectionFragments(versionSeriesId, null);
        return fragmentsIds(fragments);
    }

    // called only when proxies enabled
    public List<Serializable> getTargetProxyIds(Serializable targetId) {
        List<SimpleFragment> fragments = targetProxies.getSelectionFragments(targetId, null);
        return fragmentsIds(fragments);
    }

    private List<Serializable> fragmentsIds(List<? extends Fragment> fragments) {
        return fragments.stream().map(Fragment::getId).collect(Collectors.toList());
    }

    /*
     * ----- Hierarchy -----
     */

    public static class PathAndId {
        public final String path;

        public final Serializable id;

        public PathAndId(String path, Serializable id) {
            this.path = path;
            this.id = id;
        }
    }

    /**
     * Gets the path by recursing up the hierarchy.
     */
    public String getPath(SimpleFragment hierFragment) {
        PathAndId pathAndId = getPathOrMissingParentId(hierFragment, true);
        return pathAndId.path;
    }

    /**
     * Gets the full path, or the closest parent id which we don't have in cache.
     * <p>
     * If {@code fetch} is {@code true}, returns the full path.
     * <p>
     * If {@code fetch} is {@code false}, does not touch the mapper, only the context, therefore may return a missing
     * parent id instead of the path.
     *
     * @param fetch {@code true} if we can use the database, {@code false} if only caches should be used
     */
    public PathAndId getPathOrMissingParentId(SimpleFragment hierFragment, boolean fetch) {
        LinkedList<String> list = new LinkedList<>();
        Serializable parentId;
        while (true) {
            String name = hierFragment.getString(Model.HIER_CHILD_NAME_KEY);
            if (name == null) {
                // (empty string for normal databases, null for Oracle)
                name = "";
            }
            list.addFirst(name);
            parentId = hierFragment.get(Model.HIER_PARENT_KEY);
            if (parentId == null) {
                // root
                break;
            }
            // recurse in the parent
            RowId rowId = new RowId(Model.HIER_TABLE_NAME, parentId);
            hierFragment = (SimpleFragment) getIfPresent(rowId);
            if (hierFragment == null) {
                // try in mapper cache
                hierFragment = (SimpleFragment) getFromMapper(rowId, false, true);
                if (hierFragment == null) {
                    if (!fetch) {
                        return new PathAndId(null, parentId);
                    }
                    hierFragment = (SimpleFragment) getFromMapper(rowId, true, false);
                }
            }
        }
        String path;
        if (list.size() == 1) {
            String name = list.peek();
            if (name.isEmpty()) {
                // root, special case
                path = "/";
            } else {
                // placeless document, no initial slash
                path = name;
            }
        } else {
            path = String.join("/", list);
        }
        return new PathAndId(path, null);
    }

    /**
     * Finds the id of the enclosing non-complex-property node.
     *
     * @param id the id
     * @return the id of the containing document, or {@code null} if there is no parent or the parent has been deleted.
     */
    public Serializable getContainingDocument(Serializable id) {
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
            pid = p.get(Model.HIER_PARENT_KEY);
        }
    }

    // also called by Selection
    protected SimpleFragment getHier(Serializable id, boolean allowAbsent) {
        RowId rowId = new RowId(Model.HIER_TABLE_NAME, id);
        return (SimpleFragment) get(rowId, allowAbsent);
    }

    private boolean isOrderable(Serializable parentId, boolean complexProp) {
        if (complexProp) {
            return true;
        }
        SimpleFragment parent = getHier(parentId, true);
        String typeName = parent.getString(Model.MAIN_PRIMARY_TYPE_KEY);
        return model.getDocumentTypeFacets(typeName).contains(FacetNames.ORDERABLE);
    }

    /** Recursively checks if any of a fragment's parents has been deleted. */
    // needed because we don't recursively clear caches when doing a delete
    public boolean isDeleted(Serializable id) {
        while (id != null) {
            SimpleFragment fragment = getHier(id, false);
            State state;
            if (fragment == null || (state = fragment.getState()) == State.ABSENT || state == State.DELETED
                    || state == State.DELETED_DEPENDENT || state == State.INVALIDATED_DELETED) {
                return true;
            }
            id = fragment.get(Model.HIER_PARENT_KEY);
        }
        return false;
    }

    /**
     * Gets the next pos value for a new child in a folder.
     *
     * @param nodeId the folder node id
     * @param complexProp whether to deal with complex properties or regular children
     * @return the next pos, or {@code null} if not orderable
     */
    public Long getNextPos(Serializable nodeId, boolean complexProp) {
        if (!isOrderable(nodeId, complexProp)) {
            return null;
        }
        long max = -1;
        for (SimpleFragment fragment : getChildren(nodeId, null, complexProp)) {
            Long pos = (Long) fragment.get(Model.HIER_CHILD_POS_KEY);
            if (pos != null && pos.longValue() > max) {
                max = pos.longValue();
            }
        }
        return Long.valueOf(max + 1);
    }

    /**
     * Order a child before another.
     *
     * @param parentId the parent id
     * @param sourceId the node id to move
     * @param destId the node id before which to place the source node, if {@code null} then move the source to the end
     */
    public void orderBefore(Serializable parentId, Serializable sourceId, Serializable destId) {
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
        List<SimpleFragment> fragments = getChildren(parentId, null, complexProp);
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
                    source.put(Model.HIER_CHILD_POS_KEY, destPos);
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
                if (!setPos.equals(fragment.get(Model.HIER_CHILD_POS_KEY))) {
                    fragment.put(Model.HIER_CHILD_POS_KEY, setPos);
                }
            }
            i++;
        }
        if (destId == null) {
            Long setPos = Long.valueOf(i);
            if (!setPos.equals(source.get(Model.HIER_CHILD_POS_KEY))) {
                source.put(Model.HIER_CHILD_POS_KEY, setPos);
            }
        }
    }

    public SimpleFragment getChildHierByName(Serializable parentId, String name, boolean complexProp) {
        return getHierSelectionContext(complexProp).getSelectionFragment(parentId, name);
    }

    /**
     * Gets hier fragments for children.
     */
    public List<SimpleFragment> getChildren(Serializable parentId, String name, boolean complexProp) {
        List<SimpleFragment> fragments = getHierSelectionContext(complexProp).getSelectionFragments(parentId, name);
        if (isOrderable(parentId, complexProp)) {
            // sort children in order
            Collections.sort(fragments, POS_COMPARATOR);
        }
        return fragments;
    }

    /** Checks that we don't move/copy under ourselves. */
    protected void checkNotUnder(Serializable parentId, Serializable id, String op) {
        Serializable pid = parentId;
        do {
            if (pid.equals(id)) {
                throw new DocumentExistsException(
                        "Cannot " + op + " a node under itself: " + parentId + " is under " + id);
            }
            SimpleFragment p = getHier(pid, false);
            if (p == null) {
                // cannot happen
                throw new NuxeoException("No parent: " + pid);
            }
            pid = p.get(Model.HIER_PARENT_KEY);
        } while (pid != null);
    }

    /** Checks that a name is free. Cannot check concurrent sessions though. */
    protected void checkFreeName(Serializable parentId, String name, boolean complexProp) {
        Fragment fragment = getChildHierByName(parentId, name, complexProp);
        if (fragment != null) {
            throw new DocumentExistsException("Destination name already exists: " + name);
        }
    }

    /**
     * Move a child to a new parent with a new name.
     *
     * @param source the source
     * @param parentId the destination parent id
     * @param name the new name
     */
    public void move(Node source, Serializable parentId, String name) {
        // a save() has already been done by the caller when doing
        // an actual move (different parents)
        Serializable id = source.getId();
        SimpleFragment hierFragment = source.getHierFragment();
        Serializable oldParentId = hierFragment.get(Model.HIER_PARENT_KEY);
        String oldName = hierFragment.getString(Model.HIER_CHILD_NAME_KEY);
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
            hierFragment.put(Model.HIER_CHILD_NAME_KEY, name);
        }
        // cache management
        getHierSelectionContext(complexProp).recordRemoved(hierFragment);
        hierFragment.put(Model.HIER_PARENT_KEY, parentId);
        getHierSelectionContext(complexProp).recordExisting(hierFragment, true);
        // path invalidated
        source.path = null;
    }

    /**
     * Copy a child to a new parent with a new name.
     *
     * @param source the source of the copy
     * @param parentId the destination parent id
     * @param name the new name
     * @return the id of the copy
     */
    public Serializable copy(Node source, Serializable parentId, String name) {
        Serializable id = source.getId();
        SimpleFragment hierFragment = source.getHierFragment();
        Serializable oldParentId = hierFragment.get(Model.HIER_PARENT_KEY);
        if (oldParentId != null && !oldParentId.equals(parentId)) {
            checkNotUnder(parentId, id, "copy");
        }
        checkFreeName(parentId, name, complexProp(hierFragment));
        // do the copy
        Long pos = getNextPos(parentId, false);
        CopyResult copyResult = mapper.copy(new IdWithTypes(source), parentId, name, null);
        Serializable newId = copyResult.copyId;
        // read new child in this session (updates children Selection)
        SimpleFragment copy = getHier(newId, false);
        // invalidate child in other sessions' children Selection
        markInvalidated(copyResult.invalidations);
        // read new proxies in this session (updates Selections)
        List<RowId> rowIds = new ArrayList<>();
        for (Serializable proxyId : copyResult.proxyIds) {
            rowIds.add(new RowId(Model.PROXY_TABLE_NAME, proxyId));
        }
        // multi-fetch will register the new fragments with the Selections
        List<Fragment> fragments = getMulti(rowIds, true);
        // invalidate Selections in other sessions
        for (Fragment fragment : fragments) {
            seriesProxies.recordExisting((SimpleFragment) fragment, true);
            targetProxies.recordExisting((SimpleFragment) fragment, true);
        }
        // version copy fixup
        if (source.isVersion()) {
            copy.put(Model.MAIN_IS_VERSION_KEY, null);
        }
        // pos fixup
        copy.put(Model.HIER_CHILD_POS_KEY, pos);
        return newId;
    }

    /**
     * Checks in a node (creates a version).
     *
     * @param node the node to check in
     * @param label the version label
     * @param checkinComment the version description
     * @return the created version id
     */
    public Serializable checkIn(Node node, String label, String checkinComment) {
        Boolean checkedIn = (Boolean) node.hierFragment.get(Model.MAIN_CHECKED_IN_KEY);
        if (Boolean.TRUE.equals(checkedIn)) {
            throw new NuxeoException("Already checked in");
        }
        if (label == null) {
            // use version major + minor as label
            Serializable major = node.getSimpleProperty(Model.MAIN_MAJOR_VERSION_PROP).getValue();
            Serializable minor = node.getSimpleProperty(Model.MAIN_MINOR_VERSION_PROP).getValue();
            if (major == null) {
                major = "0";
            }
            if (minor == null) {
                minor = "0";
            }
            label = major + "." + minor;
        }

        /*
         * Do the copy without non-complex children, with null parent.
         */
        Serializable id = node.getId();
        CopyResult res = mapper.copy(new IdWithTypes(node), null, null, null);
        Serializable newId = res.copyId;
        markInvalidated(res.invalidations);
        // add version as a new child of its parent
        SimpleFragment verHier = getHier(newId, false);
        verHier.put(Model.MAIN_IS_VERSION_KEY, Boolean.TRUE);
        boolean isMajor = Long.valueOf(0).equals(verHier.get(Model.MAIN_MINOR_VERSION_KEY));

        // create a "version" row for our new version
        Row row = new Row(Model.VERSION_TABLE_NAME, newId);
        row.putNew(Model.VERSION_VERSIONABLE_KEY, id);
        row.putNew(Model.VERSION_CREATED_KEY, new GregorianCalendar()); // now
        row.putNew(Model.VERSION_LABEL_KEY, label);
        row.putNew(Model.VERSION_DESCRIPTION_KEY, checkinComment);
        row.putNew(Model.VERSION_IS_LATEST_KEY, Boolean.TRUE);
        row.putNew(Model.VERSION_IS_LATEST_MAJOR_KEY, Boolean.valueOf(isMajor));
        createVersionFragment(row);

        // update the original node to reflect that it's checked in
        node.hierFragment.put(Model.MAIN_CHECKED_IN_KEY, Boolean.TRUE);
        node.hierFragment.put(Model.MAIN_BASE_VERSION_KEY, newId);

        recomputeVersionSeries(id);

        return newId;
    }

    /**
     * Checks out a node.
     *
     * @param node the node to check out
     */
    public void checkOut(Node node) {
        Boolean checkedIn = (Boolean) node.hierFragment.get(Model.MAIN_CHECKED_IN_KEY);
        if (!Boolean.TRUE.equals(checkedIn)) {
            throw new NuxeoException("Already checked out");
        }
        // update the node to reflect that it's checked out
        node.hierFragment.put(Model.MAIN_CHECKED_IN_KEY, Boolean.FALSE);
    }

    /**
     * Restores a node to a given version.
     * <p>
     * The restored node is checked in.
     *
     * @param node the node
     * @param version the version to restore on this node
     */
    public void restoreVersion(Node node, Node version) {
        Serializable versionableId = node.getId();
        Serializable versionId = version.getId();

        // clear complex properties
        List<SimpleFragment> children = getChildren(versionableId, null, true);
        // copy to avoid concurrent modifications
        for (SimpleFragment child : children.toArray(new SimpleFragment[children.size()])) {
            removePropertyNode(child);
        }
        session.flush(); // flush deletes

        // copy the version values
        Row overwriteRow = new Row(Model.HIER_TABLE_NAME, versionableId);
        SimpleFragment versionHier = version.getHierFragment();
        for (String key : model.getFragmentKeysType(Model.HIER_TABLE_NAME).keySet()) {
            // keys we don't copy from version when restoring
            if (key.equals(Model.HIER_PARENT_KEY) || key.equals(Model.HIER_CHILD_NAME_KEY)
                    || key.equals(Model.HIER_CHILD_POS_KEY) || key.equals(Model.HIER_CHILD_ISPROPERTY_KEY)
                    || key.equals(Model.MAIN_PRIMARY_TYPE_KEY) || key.equals(Model.MAIN_CHECKED_IN_KEY)
                    || key.equals(Model.MAIN_BASE_VERSION_KEY) || key.equals(Model.MAIN_IS_VERSION_KEY)) {
                continue;
            }
            overwriteRow.putNew(key, versionHier.get(key));
        }
        overwriteRow.putNew(Model.MAIN_CHECKED_IN_KEY, Boolean.TRUE);
        overwriteRow.putNew(Model.MAIN_BASE_VERSION_KEY, versionId);
        overwriteRow.putNew(Model.MAIN_IS_VERSION_KEY, null);
        CopyResult res = mapper.copy(new IdWithTypes(version), node.getParentId(), null, overwriteRow);
        markInvalidated(res.invalidations);
    }

}
