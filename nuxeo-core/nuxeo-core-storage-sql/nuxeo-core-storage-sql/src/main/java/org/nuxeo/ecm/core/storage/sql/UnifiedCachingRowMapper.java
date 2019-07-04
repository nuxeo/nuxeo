/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
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
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.management.ManagementService;
import net.sf.ehcache.transaction.manager.TransactionManagerLookup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.storage.sql.ACLRow.ACLRowPositionComparator;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.ServerLocator;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

/**
 * A {@link RowMapper} that use an unified ehcache.
 * <p>
 * The cache only holds {@link Row}s that are known to be identical to what's in the underlying {@link RowMapper}.
 */
public class UnifiedCachingRowMapper implements RowMapper {

    private static final Log log = LogFactory.getLog(UnifiedCachingRowMapper.class);

    private static final String ABSENT = "__ABSENT__\0\0\0";

    private static CacheManager cacheManager = null;

    protected static boolean isXA;

    private Cache cache;

    private Model model;

    /**
     * The {@link RowMapper} to which operations that cannot be processed from the cache are delegated.
     */
    private RowMapper rowMapper;

    /**
     * The local invalidations due to writes through this mapper that should be propagated to other sessions at
     * post-commit time.
     */
    private final Invalidations localInvalidations;

    /**
     * The queue of invalidations received from other session or from the cluster invalidator, to process at
     * pre-transaction time.
     */
    private final InvalidationsQueue invalidationsQueue;

    /**
     * The propagator of invalidations to other mappers.
     */
    private InvalidationsPropagator invalidationsPropagator;

    private static final String CACHE_NAME = "unifiedVCSCache";

    private static final String EHCACHE_FILE_PROP = "ehcacheFilePath";

    private static AtomicInteger rowMapperCount = new AtomicInteger();

    /**
     * Cache statistics
     *
     * @since 5.7
     */
    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected Counter cacheHitCount;

    protected Timer cacheGetTimer;

    // sor means system of record (database access)
    protected Counter sorRows;

    protected Timer sorGetTimer;

    public UnifiedCachingRowMapper() {
        localInvalidations = new Invalidations();
        invalidationsQueue = new InvalidationsQueue("mapper-" + this);
    }

    synchronized public void initialize(String repositoryName, Model model, RowMapper rowMapper,
            InvalidationsPropagator invalidationsPropagator, Map<String, String> properties) {
        this.model = model;
        this.rowMapper = rowMapper;
        this.invalidationsPropagator = invalidationsPropagator;
        invalidationsPropagator.addQueue(invalidationsQueue);
        if (cacheManager == null) {
            if (properties.containsKey(EHCACHE_FILE_PROP)) {
                String value = properties.get(EHCACHE_FILE_PROP);
                log.info("Creating ehcache manager for VCS, using ehcache file: " + value);
                cacheManager = CacheManager.create(value);
            } else {
                log.info("Creating ehcache manager for VCS, No ehcache file provided");
                cacheManager = CacheManager.create();
            }
            isXA = cacheManager.getConfiguration().getCacheConfigurations().get(CACHE_NAME).isXaTransactional();
            // Exposes cache to JMX
            MBeanServer mBeanServer = Framework.getService(ServerLocator.class).lookupServer();
            ManagementService.registerMBeans(cacheManager, mBeanServer, true, true, true, true);
        }
        rowMapperCount.incrementAndGet();
        cache = cacheManager.getCache(CACHE_NAME);
        setMetrics(repositoryName);
    }

    protected void setMetrics(String repositoryName) {
        cacheHitCount = registry.counter(MetricRegistry.name("nuxeo", "repositories", repositoryName, "caches",
                "unified", "hits"));
        cacheGetTimer = registry.timer(MetricRegistry.name("nuxeo", "repositories", repositoryName, "caches",
                "unified", "get"));
        sorRows = registry.counter(MetricRegistry.name("nuxeo", "repositories", repositoryName, "caches", "unified",
                "sor", "rows"));
        sorGetTimer = registry.timer(MetricRegistry.name("nuxeo", "repositories", repositoryName, "caches", "unified",
                "sor", "get"));
        String gaugeName = MetricRegistry.name("nuxeo", "repositories", repositoryName, "caches", "unified",
                "cache-size");
        @SuppressWarnings("rawtypes")
        SortedMap<String, Gauge> gauges = registry.getGauges();
        if (!gauges.containsKey(gaugeName)) {
            registry.register(gaugeName, new Gauge<Integer>() {
                @Override
                public Integer getValue() {
                    if (cacheManager != null) {
                        return cacheManager.getCache(CACHE_NAME).getSize();
                    }
                    return 0;
                }
            });
        }
    }

    public void close() {
        invalidationsPropagator.removeQueue(invalidationsQueue);
        rowMapperCount.decrementAndGet();
    }

    @Override
    public Serializable generateNewId() {
        return rowMapper.generateNewId();
    }

    /*
     * ----- ehcache -----
     */

    protected boolean hasTransaction() {
        TransactionManagerLookup transactionManagerLookup = cache.getTransactionManagerLookup();
        if (transactionManagerLookup == null) {
            return false;
        }
        TransactionManager transactionManager = transactionManagerLookup.getTransactionManager();
        if (transactionManager == null) {
            return false;
        }
        Transaction transaction;
        try {
            transaction = transactionManager.getTransaction();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
        return transaction != null;
    }

    protected boolean useEhCache() {
        return !isXA || hasTransaction();
    }

    protected void ehCachePut(Element element) {
        if (useEhCache()) {
            cache.put(element);
        }
    }

    protected Element ehCacheGet(Serializable key) {
        if (useEhCache()) {
            return cache.get(key);
        }
        return null;
    }

    protected int ehCacheGetSize() {
        if (useEhCache()) {
            return cache.getSize();
        }
        return 0;
    }

    protected boolean ehCacheRemove(Serializable key) {
        if (useEhCache()) {
            return cache.remove(key);
        }
        return false;
    }

    protected void ehCacheRemoveAll() {
        if (useEhCache()) {
            cache.removeAll();
        }
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
        if (row.isCollection() && row.values.length > 0 && row.values[0] instanceof ACLRow) {
            row.values = sortACLRows((ACLRow[]) row.values);
        }
        Element element = new Element(new RowId(row), row);
        ehCachePut(element);
    }

    protected ACLRow[] sortACLRows(ACLRow[] acls) {
        List<ACLRow> list = new ArrayList<>(Arrays.asList(acls));
        Collections.sort(list, ACLRowPositionComparator.INSTANCE);
        ACLRow[] res = new ACLRow[acls.length];
        return list.toArray(res);
    }

    protected void cachePutAbsent(RowId rowId) {
        Element element = new Element(new RowId(rowId), new Row(ABSENT, (Serializable) null));
        ehCachePut(element);
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

    @SuppressWarnings("resource") // Time.Context closed by stop()
    protected Row cacheGet(RowId rowId) {
        final Context context = cacheGetTimer.time();
        try {
            Element element = ehCacheGet(rowId);
            Row row = null;
            if (element != null) {
                row = (Row) element.getObjectValue();
            }
            if (row != null && !isAbsent(row)) {
                row = row.clone();
            }
            if (row != null) {
                cacheHitCount.inc();
            }
            return row;
        } finally {
            context.stop();
        }
    }

    protected void cacheRemove(RowId rowId) {
        ehCacheRemove(rowId);
    }

    /*
     * ----- Invalidations / Cache Management -----
     */

    @Override
    public Invalidations receiveInvalidations() {
        // invalidations from the underlying mapper (cluster)
        // already propagated to our invalidations queue
        Invalidations remoteInvals = rowMapper.receiveInvalidations();

        Invalidations ret = invalidationsQueue.getInvalidations();

        if (remoteInvals != null) {
            if (!ret.all) {
                // only handle remote invalidations, the cache is shared and transactional
                if (remoteInvals.modified != null) {
                    for (RowId rowId : remoteInvals.modified) {
                        cacheRemove(rowId);
                    }
                }
                if (remoteInvals.deleted != null) {
                    for (RowId rowId : remoteInvals.deleted) {
                        cachePutAbsent(rowId);
                    }
                }
            }
        }

        // invalidate our cache
        if (ret.all) {
            clearCache();
        }

        return ret.isEmpty() ? null : ret;
    }

    // propagate invalidations
    @Override
    public void sendInvalidations(Invalidations invalidations) {
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

            // queue to other mappers' caches
            invalidationsPropagator.propagateInvalidations(invalidations, invalidationsQueue);
        }
    }

    @Override
    public void clearCache() {
        ehCacheRemoveAll();
        localInvalidations.clear();
        rowMapper.clearCache();
    }

    @Override
    public void rollback(Xid xid) throws XAException {
        try {
            rowMapper.rollback(xid);
        } finally {
            ehCacheRemoveAll();
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
    public List<? extends RowId> read(Collection<RowId> rowIds, boolean cacheOnly) {
        List<RowId> res = new ArrayList<>(rowIds.size());
        // find which are in cache, and which not
        List<RowId> todo = new LinkedList<>();
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
            @SuppressWarnings("resource")
            final Context context = sorGetTimer.time();
            try {
                // ask missing ones to underlying row mapper
                List<? extends RowId> fetched = rowMapper.read(todo, cacheOnly);
                // add them to the cache
                for (RowId rowId : fetched) {
                    cachePutAbsentIfRowId(rowId);
                }
                // merge results
                res.addAll(fetched);
                sorRows.inc(fetched.size());
            } finally {
                context.stop();
            }
        }
        return res;
    }

    /*
     * Save in the cache then pass all the writes to the mapper.
     */
    @Override
    public void write(RowBatch batch) {
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
    public Row readSimpleRow(RowId rowId) {
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
    public Map<String, String> getBinaryFulltext(RowId rowId) {
        return rowMapper.getBinaryFulltext(rowId);
    }

    @Override
    public Serializable[] readCollectionRowArray(RowId rowId) {
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
    public List<Row> readSelectionRows(SelectionType selType, Serializable selId, Serializable filter,
            Serializable criterion, boolean limitToOne) {
        List<Row> rows = rowMapper.readSelectionRows(selType, selId, filter, criterion, limitToOne);
        for (Row row : rows) {
            cachePut(row);
        }
        return rows;
    }

    @Override
    public Set<Serializable> readSelectionsIds(SelectionType selType, List<Serializable> values) {
        return rowMapper.readSelectionsIds(selType, values);
    }

    /*
     * ----- Copy -----
     */

    @Override
    public CopyResult copy(IdWithTypes source, Serializable destParentId, String destName, Row overwriteRow, boolean excludeSpecialChildren) {
        CopyResult result = rowMapper.copy(source, destParentId, destName, overwriteRow, excludeSpecialChildren);
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
    public List<NodeInfo> getDescendantsInfo(Serializable rootId) {
        return rowMapper.getDescendantsInfo(rootId);
    }

    @Override
    public void remove(Serializable rootId, List<NodeInfo> nodeInfos) {
        rowMapper.remove(rootId, nodeInfos);
        for (NodeInfo info : nodeInfos) {
            for (String fragmentName : model.getTypeFragments(new IdWithTypes(info))) {
                RowId rowId = new RowId(fragmentName, info.id);
                cacheRemove(rowId);
                localInvalidations.addDeleted(rowId);
            }
        }
        // we only put as absent the root fragment, to avoid polluting the cache
        // with lots of absent info. the rest is removed entirely
        cachePutAbsent(new RowId(Model.HIER_TABLE_NAME, rootId));
    }

    @Override
    public long getCacheSize() {
        // The unified cache is reported by the cache-size gauge
        return 0;
    }

}
