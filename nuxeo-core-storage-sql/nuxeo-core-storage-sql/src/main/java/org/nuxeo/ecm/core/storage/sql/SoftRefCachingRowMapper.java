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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.javasimon.Counter;
import org.javasimon.SimonManager;
import org.javasimon.Split;
import org.javasimon.Stopwatch;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.ACLRow.ACLRowPositionComparator;
import org.nuxeo.ecm.core.storage.sql.Invalidations.InvalidationsPair;
import org.nuxeo.runtime.api.Framework;

/**
 * A {@link RowMapper} that has an internal cache.
 * <p>
 * The cache only holds {@link Row}s that are known to be identical to what's in
 * the underlying {@link RowMapper}.
 */
public class SoftRefCachingRowMapper implements RowMapper {

    private static final Log log = LogFactory.getLog(SoftRefCachingRowMapper.class);

    private static final String ABSENT = "__ABSENT__\0\0\0";

    /**
     * The cached rows. All held data is identical to what is present in the
     * underlying {@link RowMapper} and could be refetched if needed.
     * <p>
     * The values are either {@link Row} for fragments present in the database,
     * or a row with tableName {@link #ABSENT} to denote a fragment known to be
     * absent from the database.
     * <p>
     * This cache is memory-sensitive (all values are soft-referenced), a
     * fragment can always be refetched if the GC collects it.
     */
    // we use a new Row instance for the absent case to avoid keeping other
    // references to it which would prevent its GCing
    private final Map<RowId, Row> cache;

    private Model model;

    /**
     * The {@link RowMapper} to which operations that cannot be processed from
     * the cache are delegated.
     */
    private RowMapper rowMapper;

    /**
     * The local invalidations due to writes through this mapper that should be
     * propagated to other sessions at post-commit time.
     */
    private final Invalidations localInvalidations;

    /**
     * The queue of cache invalidations received from other session, to process
     * at pre-transaction time.
     */
    private final InvalidationsQueue cacheQueue;

    /**
     * The propagator of invalidations to other mappers.
     */
    private InvalidationsPropagator cachePropagator;

    /**
     * The queue of invalidations used for events, a single queue is shared by
     * all mappers corresponding to the same client repository.
     */
    private InvalidationsQueue eventQueue;

    /**
     * The propagator of event invalidations to all event queues.
     */
    private InvalidationsPropagator eventPropagator;

    /**
     * The session, used for event propagation.
     */
    private SessionImpl session;

    protected boolean forRemoteClient;

    /**
     * Cache statistics
     */
    // JavaSimpon Counter Names
    private static final String CN_ACCESS = "org.nuxeo.ecm.core.storage.sql.cache.access";

    private static final String CN_HITS = "org.nuxeo.ecm.core.storage.sql.cache.hits";

    private static final String CN_SIZE = "org.nuxeo.ecm.core.storage.sql.cache.size";

    // Stop watch for cache access
    private static final String SW_CACHE = "org.nuxeo.ecm.core.storage.sql.cache.get";

    // Stop watch for SOR access (System Of Record i.e the db access)
    private static final String SW_SOR = "org.nuxeo.ecm.core.storage.sql.sor.gets";

    // Property to enable stop watch
    private static final String CACHE_STATS_PROP = "org.nuxeo.vcs.cache.statistics";

    private long accessCount;

    private long hitsCount;

    private long cacheSize;

    private boolean cacheStatistics;

    @SuppressWarnings("unchecked")
    public SoftRefCachingRowMapper() {
        cache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);
        localInvalidations = new Invalidations();
        cacheQueue = new InvalidationsQueue("mapper-" + this);
        forRemoteClient = false;
        String prop = Framework.getProperty(CACHE_STATS_PROP, "false");
        cacheStatistics = Boolean.parseBoolean(prop);
    }

    public void initialize(Model model, RowMapper rowMapper,
            InvalidationsPropagator cachePropagator,
            InvalidationsPropagator eventPropagator,
            InvalidationsQueue repositoryEventQueue) {
        this.model = model;
        this.rowMapper = rowMapper;
        this.cachePropagator = cachePropagator;
        cachePropagator.addQueue(cacheQueue);
        eventQueue = repositoryEventQueue;
        this.eventPropagator = eventPropagator;
        eventPropagator.addQueue(repositoryEventQueue);
    }

    public void close() throws StorageException {
        cachePropagator.removeQueue(cacheQueue);
        eventPropagator.removeQueue(eventQueue); // TODO can be overriden
        logCacheStat();
    }

    /*
     * ----- Cache -----
     */

    protected static boolean isAbsent(Row row) {
        return row.tableName == ABSENT; // == is ok
    }

    protected void cachePut(Row row) {
        row = row.clone();
        // for ACL collections, make sure the order is correct
        // (without the cache, the query to get a list of collection does an
        // ORDER BY pos, so users of the cache must get the same behavior)
        if (row.isCollection() && row.values.length > 0
                && row.values[0] instanceof ACLRow) {
            row.values = sortACLRows((ACLRow[]) row.values);
        }
        cache.put(new RowId(row), row);
    }

    protected ACLRow[] sortACLRows(ACLRow[] acls) {
        List<ACLRow> list = new ArrayList<ACLRow>(Arrays.asList(acls));
        Collections.sort(list, ACLRowPositionComparator.INSTANCE);
        ACLRow[] res = new ACLRow[acls.length];
        return list.toArray(res);
    }

    protected void cachePutAbsent(RowId rowId) {
        cache.put(new RowId(rowId), new Row(ABSENT, (Serializable) null));
    }

    protected void cachePutAbsentIfNull(RowId rowId, Row row) {
        if (row != null) {
            cachePut(row);
        } else {
            cachePutAbsent(rowId);
        }
    }

    protected void cachePutAbsentIfRowId(RowId rowId) {
        if (rowId instanceof Row) {
            cachePut((Row) rowId);
        } else {
            cachePutAbsent(rowId);
        }
    }

    protected Row cacheGet(RowId rowId) {
        Split split = null;
        if (cacheStatistics) {
            Stopwatch stopWatch = SimonManager.getStopwatch(SW_CACHE);
            split = stopWatch.start();
        }

        Row row = cache.get(rowId);
        if (row != null && !isAbsent(row)) {
            row = row.clone();
        }

        if (split != null) {
            split.stop();
        }
        updateCacheStat(row);
        return row;
    }

    private void updateCacheStat(Row row) {
        if (row != null) {
            hitsCount++;
        }
        if (accessCount++ % 200 == 0) {
            Counter accessCounter = SimonManager.getCounter(CN_ACCESS);
            accessCounter.increase(accessCount);
            accessCount = 0;
            Counter hitsCounter = SimonManager.getCounter(CN_HITS);
            hitsCounter.increase(hitsCount);
            hitsCount = 0;
            Counter sizeCounter = SimonManager.getCounter(CN_SIZE);
            long delta = cache.size() - cacheSize;
            if (delta > 0) {
                sizeCounter.increase(delta);
            } else if (delta < 0) {
                sizeCounter.decrease(-1 * delta);
            }
            cacheSize = cache.size();
        }
    }

    private void logCacheStat() {
        if (cacheStatistics) {
            Stopwatch stopWatch = SimonManager.getStopwatch(SW_CACHE);
            log.info(stopWatch);
            stopWatch = SimonManager.getStopwatch(SW_SOR);
            log.info(stopWatch);
        }
        Counter counter = SimonManager.getCounter(CN_ACCESS);
        log.info(counter);
        counter = SimonManager.getCounter(CN_HITS);
        log.info(counter);
        counter = SimonManager.getCounter(CN_SIZE);
        log.info(counter);
    }

    protected void cacheRemove(RowId rowId) {
        cache.remove(rowId);
    }

    /*
     * ----- Invalidations / Cache Management -----
     */

    @Override
    public InvalidationsPair receiveInvalidations() throws StorageException {
        // invalidations from the underlying mapper (remote, cluster)
        InvalidationsPair invals = rowMapper.receiveInvalidations();

        // add local accumulated invalidations to remote ones
        Invalidations invalidations = cacheQueue.getInvalidations();
        if (invals != null) {
            invalidations.add(invals.cacheInvalidations);
        }

        // add local accumulated events to remote ones
        Invalidations events = eventQueue.getInvalidations();
        if (invals != null) {
            events.add(invals.eventInvalidations);
        }

        // invalidate our cache
        if (invalidations.all) {
            clearCache();
        }
        if (invalidations.modified != null) {
            for (RowId rowId : invalidations.modified) {
                cacheRemove(rowId);
            }
        }
        if (invalidations.deleted != null) {
            for (RowId rowId : invalidations.deleted) {
                cachePutAbsent(rowId);
            }
        }

        if (invalidations.isEmpty() && events.isEmpty()) {
            return null;
        }
        return new InvalidationsPair(invalidations.isEmpty() ? null
                : invalidations, events.isEmpty() ? null : events);
    }

    // propagate invalidations
    @Override
    public void sendInvalidations(Invalidations invalidations)
            throws StorageException {
        // add local invalidations
        if (!localInvalidations.isEmpty()) {
            if (invalidations == null) {
                invalidations = new Invalidations();
            }
            invalidations.add(localInvalidations);
            localInvalidations.clear();
        }

        if (invalidations != null && !invalidations.isEmpty()) {
            // send to underlying mapper
            rowMapper.sendInvalidations(invalidations);

            // queue to other local mappers' caches
            cachePropagator.propagateInvalidations(invalidations, cacheQueue);

            // queue as events for other repositories
            eventPropagator.propagateInvalidations(invalidations, eventQueue);

            // send event to local repository (synchronous)
            // only if not the server-side part of a remote client
            if (!forRemoteClient) {
                session.sendInvalidationEvent(invalidations, true);
            }
        }
    }

    /**
     * Used by the server to associate each mapper to a single event
     * invalidations queue per client repository.
     */
    public void setEventQueue(InvalidationsQueue eventQueue) {
        // don't remove the original global repository queue
        this.eventQueue = eventQueue;
        eventPropagator.addQueue(eventQueue);
        forRemoteClient = true;
    }

    /**
     * Sets the session, used for event propagation.
     */
    public void setSession(SessionImpl session) {
        this.session = session;
    }

    @Override
    public void clearCache() {
        cache.clear();
        localInvalidations.clear();
        rowMapper.clearCache();
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        try {
            rowMapper.rollback(xid);
        } finally {
            cache.clear();
            localInvalidations.clear();
        }
    }

    /*
     * ----- Batch -----
     */

    /*
     * Use those from the cache if available, read from the mapper for the rest.
     */
    @Override
    public List<? extends RowId> read(Collection<RowId> rowIds,
            boolean cacheOnly) throws StorageException {
        List<RowId> res = new ArrayList<RowId>(rowIds.size());
        // find which are in cache, and which not
        List<RowId> todo = new LinkedList<RowId>();
        for (RowId rowId : rowIds) {
            Row row = cacheGet(rowId);
            if (row == null) {
                if (cacheOnly) {
                    res.add(new RowId(rowId));
                } else {
                    todo.add(rowId);
                }
            } else if (isAbsent(row)) {
                res.add(new RowId(rowId));
            } else {
                res.add(row);
            }
        }
        if (!todo.isEmpty()) {
            Split split = null;
            if (cacheStatistics) {
                Stopwatch stopWatch = SimonManager.getStopwatch(SW_SOR);
                split = stopWatch.start();
            }
            // ask missing ones to underlying row mapper
            List<? extends RowId> fetched = rowMapper.read(todo, cacheOnly);
            // add them to the cache
            for (RowId rowId : fetched) {
                cachePutAbsentIfRowId(rowId);
            }
            // merge results
            res.addAll(fetched);
            if (split != null) {
                split.stop();
            }
        }
        return res;
    }

    /*
     * Save in the cache then pass all the writes to the mapper.
     */
    @Override
    public void write(RowBatch batch) throws StorageException {
        // we avoid gathering invalidations for a write-only table: fulltext
        for (Row row : batch.creates) {
            cachePut(row);
            if (!Model.FULLTEXT_TABLE_NAME.equals(row.tableName)) {
                // we need to send modified invalidations for created
                // fragments because other session's ABSENT fragments have
                // to be invalidated
                localInvalidations.addModified(new RowId(row));
            }
        }
        for (RowUpdate rowu : batch.updates) {
            cachePut(rowu.row);
            if (!Model.FULLTEXT_TABLE_NAME.equals(rowu.row.tableName)) {
                localInvalidations.addModified(new RowId(rowu.row));
            }
        }
        for (RowId rowId : batch.deletes) {
            if (rowId instanceof Row) {
                throw new AssertionError();
            }
            cachePutAbsent(rowId);
            if (!Model.FULLTEXT_TABLE_NAME.equals(rowId.tableName)) {
                localInvalidations.addDeleted(rowId);
            }
        }
        for (RowId rowId : batch.deletesDependent) {
            if (rowId instanceof Row) {
                throw new AssertionError();
            }
            cachePutAbsent(rowId);
            if (!Model.FULLTEXT_TABLE_NAME.equals(rowId.tableName)) {
                localInvalidations.addDeleted(rowId);
            }
        }

        // propagate to underlying mapper
        rowMapper.write(batch);
    }

    /*
     * ----- Read -----
     */

    @Override
    public Row readSimpleRow(RowId rowId) throws StorageException {
        Row row = cacheGet(rowId);
        if (row == null) {
            row = rowMapper.readSimpleRow(rowId);
            cachePutAbsentIfNull(rowId, row);
            return row;
        } else if (isAbsent(row)) {
            return null;
        } else {
            return row;
        }
    }

    @Override
    public Serializable[] readCollectionRowArray(RowId rowId)
            throws StorageException {
        Row row = cacheGet(rowId);
        if (row == null) {
            Serializable[] array = rowMapper.readCollectionRowArray(rowId);
            assert array != null;
            row = new Row(rowId.tableName, rowId.id, array);
            cachePut(row);
            return row.values;
        } else if (isAbsent(row)) {
            return null;
        } else {
            return row.values;
        }
    }

    @Override
    public List<Row> readSelectionRows(SelectionType selType,
            Serializable selId, Serializable filter, Serializable criterion,
            boolean limitToOne) throws StorageException {
        List<Row> rows = rowMapper.readSelectionRows(selType, selId, filter,
                criterion, limitToOne);
        for (Row row : rows) {
            cachePut(row);
        }
        return rows;
    }

    /*
     * ----- Copy -----
     */

    @Override
    public CopyResult copy(IdWithTypes source, Serializable destParentId,
            String destName, Row overwriteRow) throws StorageException {
        CopyResult result = rowMapper.copy(source, destParentId, destName,
                overwriteRow);
        Invalidations invalidations = result.invalidations;
        if (invalidations.modified != null) {
            for (RowId rowId : invalidations.modified) {
                cacheRemove(rowId);
                localInvalidations.addModified(new RowId(rowId));
            }
        }
        if (invalidations.deleted != null) {
            for (RowId rowId : invalidations.deleted) {
                cacheRemove(rowId);
                localInvalidations.addDeleted(rowId);
            }
        }
        return result;
    }

    @Override
    public List<NodeInfo> remove(NodeInfo rootInfo) throws StorageException {
        List<NodeInfo> infos = rowMapper.remove(rootInfo);
        for (NodeInfo info : infos) {
            for (String fragmentName : model.getTypeFragments(new IdWithTypes(
                    info.id, info.primaryType, null))) {
                RowId rowId = new RowId(fragmentName, info.id);
                cacheRemove(rowId);
                localInvalidations.addDeleted(rowId);
            }
        }
        // we only put as absent the root fragment, to avoid polluting the cache
        // with lots of absent info. the rest is removed entirely
        cachePutAbsent(new RowId(model.HIER_TABLE_NAME, rootInfo.id));
        return infos;
    }

}
