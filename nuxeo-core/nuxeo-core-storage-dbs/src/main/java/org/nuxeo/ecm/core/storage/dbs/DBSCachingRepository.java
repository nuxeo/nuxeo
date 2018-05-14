/*
 * (C) Copyright 2016-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.storage.dbs;

import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_ID;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_NAME;
import static org.nuxeo.ecm.core.storage.dbs.DBSDocument.KEY_PARENT_ID;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PartialList;
import org.nuxeo.ecm.core.api.ScrollResult;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.query.sql.model.OrderByClause;
import org.nuxeo.ecm.core.storage.FulltextConfiguration;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.ecm.core.storage.State.StateDiff;
import org.nuxeo.ecm.core.storage.dbs.DBSTransactionState.ChangeTokenUpdater;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Ordering;

/**
 * The DBS Cache layer used to cache some method call of real repository
 *
 * @since 8.10
 */
public class DBSCachingRepository implements DBSRepository {

    private static final Log log = LogFactory.getLog(DBSCachingRepository.class);

    private static final Random RANDOM = new Random();

    private final DBSRepository repository;

    private final Cache<String, State> cache;

    private final Cache<String, String> childCache;

    private DBSClusterInvalidator clusterInvalidator;

    private final DBSInvalidations invalidations;

    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    public DBSCachingRepository(DBSRepository repository, DBSRepositoryDescriptor descriptor) {
        this.repository = repository;
        // Init caches
        cache = newCache(descriptor);
        registry.registerAll(GuavaCacheMetric.of(cache, "nuxeo", "repositories", repository.getName(), "cache"));
        childCache = newCache(descriptor);
        registry.registerAll(
                GuavaCacheMetric.of(childCache, "nuxeo", "repositories", repository.getName(), "childCache"));
        if (log.isInfoEnabled()) {
            log.info(String.format("DBS cache activated on '%s' repository", repository.getName()));
        }
        invalidations = new DBSInvalidations();
        if (descriptor.isClusteringEnabled()) {
            initClusterInvalidator(descriptor);
        }
    }

    protected <T> Cache<String, T> newCache(DBSRepositoryDescriptor descriptor) {
        CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder();
        builder = builder.expireAfterWrite(descriptor.cacheTTL.longValue(), TimeUnit.MINUTES).recordStats();
        if (descriptor.cacheConcurrencyLevel != null) {
            builder = builder.concurrencyLevel(descriptor.cacheConcurrencyLevel.intValue());
        }
        if (descriptor.cacheMaxSize != null) {
            builder = builder.maximumSize(descriptor.cacheMaxSize.longValue());
        }
        return builder.build();
    }

    protected void initClusterInvalidator(DBSRepositoryDescriptor descriptor) {
        String nodeId = descriptor.clusterNodeId;
        if (StringUtils.isBlank(nodeId)) {
            nodeId = String.valueOf(RANDOM.nextInt(Integer.MAX_VALUE));
            log.warn("Missing cluster node id configuration, please define it explicitly "
                    + "(usually through repository.clustering.id). Using random cluster node id instead: " + nodeId);
        } else {
            nodeId = nodeId.trim();
        }
        clusterInvalidator = createClusterInvalidator(descriptor);
        clusterInvalidator.initialize(nodeId, getName());
    }

    protected DBSClusterInvalidator createClusterInvalidator(DBSRepositoryDescriptor descriptor) {
        Class<? extends DBSClusterInvalidator> klass = descriptor.clusterInvalidatorClass;
        if (klass == null) {
            throw new NuxeoException(
                    "Unable to get cluster invalidator class from descriptor whereas clustering is enabled");
        }
        try {
            return klass.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }

    }

    public void begin() {
        repository.begin();
        processReceivedInvalidations();
    }

    @Override
    public void commit() {
        repository.commit();
        sendInvalidationsToOther();
        processReceivedInvalidations();
    }

    @Override
    public void rollback() {
        repository.rollback();
    }

    @Override
    public void shutdown() {
        repository.shutdown();
        // Clear caches
        cache.invalidateAll();
        childCache.invalidateAll();
        // Remove metrics
        String cacheName = MetricRegistry.name("nuxeo", "repositories", repository.getName(), "cache");
        String childCacheName = MetricRegistry.name("nuxeo", "repositories", repository.getName(), "childCache");
        registry.removeMatching((name, metric) -> name.startsWith(cacheName) || name.startsWith(childCacheName));
        if (log.isInfoEnabled()) {
            log.info(String.format("DBS cache deactivated on '%s' repository", repository.getName()));
        }
        // Send invalidations
        if (clusterInvalidator != null) {
            clusterInvalidator.sendInvalidations(new DBSInvalidations(true));
        }

    }

    @Override
    public State readState(String id) {
        State state = cache.getIfPresent(id);
        if (state == null) {
            state = repository.readState(id);
            if (state != null) {
                putInCache(state);
            }
        }
        return state;
    }

    @Override
    public State readPartialState(String id, Collection<String> keys) {
        // bypass caches, as the goal of this method is to not trash caches for one-shot reads
        return repository.readPartialState(id, keys);
    }

    @Override
    public List<State> readStates(List<String> ids) {
        ImmutableMap<String, State> statesMap = cache.getAllPresent(ids);
        List<String> idsToRetrieve = new ArrayList<>(ids);
        idsToRetrieve.removeAll(statesMap.keySet());
        // Read missing states from repository
        List<State> states = repository.readStates(idsToRetrieve);
        // Cache them
        states.forEach(this::putInCache);
        // Add previous cached one
        states.addAll(statesMap.values());
        // Sort them
        states.sort(Comparator.comparing(state -> state.get(KEY_ID).toString(), Ordering.explicit(ids)));
        return states;
    }

    @Override
    public void createState(State state) {
        repository.createState(state);
        // don't cache new state, it is inefficient on mass import
    }

    @Override
    public void createStates(List<State> states) {
        repository.createStates(states);
        // don't cache new states, it is inefficient on mass import
    }

    @Override
    public void updateState(String id, StateDiff diff, ChangeTokenUpdater changeTokenUpdater) {
        repository.updateState(id, diff, changeTokenUpdater);
        invalidate(id);
    }

    @Override
    public void deleteStates(Set<String> ids) {
        repository.deleteStates(ids);
        invalidateAll(ids);
    }

    @Override
    public State readChildState(String parentId, String name, Set<String> ignored) {
        processReceivedInvalidations();

        String childCacheKey = computeChildCacheKey(parentId, name);
        String stateId = childCache.getIfPresent(childCacheKey);
        if (stateId != null) {
            State state = cache.getIfPresent(stateId);
            if (state != null) {
                // As we don't have invalidation for childCache we need to check if retrieved state is the right one
                // and not a previous document which was moved or renamed
                if (parentId.equals(state.get(KEY_PARENT_ID)) && name.equals(state.get(KEY_NAME))) {
                    return state;
                } else {
                    // We can invalidate the entry in cache as the document seemed to be moved or renamed
                    childCache.invalidate(childCacheKey);
                }
            }
        }
        State state = repository.readChildState(parentId, name, ignored);
        putInCache(state);
        return state;
    }

    private void putInCache(State state) {
        if (state != null) {
            String stateId = state.get(KEY_ID).toString();
            cache.put(stateId, state);
            Object stateParentId = state.get(KEY_PARENT_ID);
            if (stateParentId != null) {
                childCache.put(computeChildCacheKey(stateParentId.toString(), state.get(KEY_NAME).toString()), stateId);
            }
        }
    }

    private String computeChildCacheKey(String parentId, String name) {
        return parentId + '_' + name;
    }

    private void invalidate(String id) {
        invalidateAll(Collections.singleton(id));
    }

    private void invalidateAll(Collection<String> ids) {
        cache.invalidateAll(ids);
        if (clusterInvalidator != null) {
            synchronized (invalidations) {
                invalidations.addAll(ids);
            }
        }
    }

    protected void sendInvalidationsToOther() {
        synchronized (invalidations) {
            if (!invalidations.isEmpty()) {
                if (clusterInvalidator != null) {
                    clusterInvalidator.sendInvalidations(invalidations);
                }
                invalidations.clear();
            }
        }
    }

    protected void processReceivedInvalidations() {
        if (clusterInvalidator != null) {
            DBSInvalidations invalidations = clusterInvalidator.receiveInvalidations();
            if (invalidations.all) {
                cache.invalidateAll();
                childCache.invalidateAll();
            } else if (invalidations.ids != null) {
                cache.invalidateAll(invalidations.ids);
            }
        }
    }

    @Override
    public BlobManager getBlobManager() {
        return repository.getBlobManager();
    }

    @Override
    public FulltextConfiguration getFulltextConfiguration() {
        return repository.getFulltextConfiguration();
    }

    @Override
    public boolean isFulltextDisabled() {
        return repository.isFulltextDisabled();
    }

    @Override
    public boolean isFulltextSearchDisabled() {
        return repository.isFulltextSearchDisabled();
    }

    @Override
    public boolean isChangeTokenEnabled() {
        return repository.isChangeTokenEnabled();
    }

    @Override
    public String getRootId() {
        return repository.getRootId();
    }

    @Override
    public String generateNewId() {
        return repository.generateNewId();
    }

    @Override
    public boolean hasChild(String parentId, String name, Set<String> ignored) {
        return repository.hasChild(parentId, name, ignored);
    }

    @Override
    public List<State> queryKeyValue(String key, Object value, Set<String> ignored) {
        return repository.queryKeyValue(key, value, ignored);
    }

    @Override
    public List<State> queryKeyValue(String key1, Object value1, String key2, Object value2, Set<String> ignored) {
        return repository.queryKeyValue(key1, value1, key2, value2, ignored);
    }

    @Override
    public Stream<State> getDescendants(String id, Set<String> keys) {
        return repository.getDescendants(id, keys);
    }

    @Override
    public Stream<State> getDescendants(String id, Set<String> keys, int limit) {
        return repository.getDescendants(id, keys, limit);
    }

    @Override
    public boolean queryKeyValuePresence(String key, String value, Set<String> ignored) {
        return repository.queryKeyValuePresence(key, value, ignored);
    }

    @Override
    public PartialList<Map<String, Serializable>> queryAndFetch(DBSExpressionEvaluator evaluator,
            OrderByClause orderByClause, boolean distinctDocuments, int limit, int offset, int countUpTo) {
        return repository.queryAndFetch(evaluator, orderByClause, distinctDocuments, limit, offset, countUpTo);
    }

    @Override
    public LockManager getLockManager() {
        return repository.getLockManager();
    }

    @Override
    public ScrollResult<String> scroll(DBSExpressionEvaluator evaluator, int batchSize, int keepAliveSeconds) {
        return repository.scroll(evaluator, batchSize, keepAliveSeconds);
    }

    @Override
    public ScrollResult<String> scroll(String scrollId) {
        return repository.scroll(scrollId);
    }

    @Override
    public Lock getLock(String id) {
        return repository.getLock(id);
    }

    @Override
    public Lock setLock(String id, Lock lock) {
        return repository.setLock(id, lock);
    }

    @Override
    public Lock removeLock(String id, String owner) {
        return repository.removeLock(id, owner);
    }

    @Override
    public void closeLockManager() {
        repository.closeLockManager();
    }

    @Override
    public void clearLockManagerCaches() {
        repository.clearLockManagerCaches();
    }

    @Override
    public String getName() {
        return repository.getName();
    }

    @Override
    public Session getSession() {
        if (repository instanceof DBSRepositoryBase) {
            return ((DBSRepositoryBase) repository).getSession(this);
        }
        return repository.getSession();
    }

    @Override
    public int getActiveSessionsCount() {
        return repository.getActiveSessionsCount();
    }

    @Override
    public void markReferencedBinaries() {
        repository.markReferencedBinaries();
    }

}
