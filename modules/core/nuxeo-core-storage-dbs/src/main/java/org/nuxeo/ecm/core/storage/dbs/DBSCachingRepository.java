/*
 * (C) Copyright 2016-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.dbs;

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.lock.LockManager;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterService;
import org.nuxeo.runtime.metrics.MetricsService;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;

/**
 * The DBS Cache layer used to cache some method call of real repository
 *
 * @since 8.10
 */
public class DBSCachingRepository implements DBSRepository {

    private static final Log log = LogFactory.getLog(DBSCachingRepository.class);

    protected static final String METRIC_CACHE_NAME = "nuxeo.repositories.repository.cache";

    protected static final String METRIC_CHILD_CACHE_NAME = "nuxeo.repositories.repository.childCache";

    private final DBSRepository repository;

    protected final Cache<String, State> cache;

    protected final Cache<String, String> childCache;

    protected final DBSRepositoryDescriptor descriptor;

    protected final DBSInvalidationsPropagator invalidationsPropagator;

    protected final DBSClusterInvalidator clusterInvalidator;

    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    public DBSCachingRepository(DBSRepository repository, DBSRepositoryDescriptor descriptor) {
        this.repository = repository;
        this.descriptor = descriptor;
        // Init caches
        if (supportsTransactions()) {
            // each connection will have its own cache
            cache = null;
            childCache = null;
        } else {
            // one global cache held by the repository
            cache = newCache(true);
            childCache = newChildCache(true);
        }
        if (log.isInfoEnabled()) {
            log.info(String.format("DBS cache activated on '%s' repository", getName()));
        }
        invalidationsPropagator = initInvalidationsPropagator();
        clusterInvalidator = initClusterInvalidator(descriptor);
    }

    protected Cache<String, State> getCache() {
        return cache;
    }

    protected Cache<String, String> getChildCache() {
        return childCache;
    }

    protected DBSInvalidationsPropagator getInvalidationsPropagator() {
        return invalidationsPropagator;
    }

    protected DBSClusterInvalidator getClusterInvalidator() {
        return clusterInvalidator;
    }

    protected Cache<String, State> newCache(boolean metrics) {
        Cache<String, State> c = newCache(descriptor);
        if (metrics) {
            registry.registerAll(GuavaCacheMetric.of(c,
                    MetricName.build(METRIC_CACHE_NAME).tagged("repository", repository.getName())));
        }
        return c;
    }

    protected Cache<String, String> newChildCache(boolean metrics) {
        Cache<String, String> c = newCache(descriptor);
        if (metrics) {
            registry.registerAll(GuavaCacheMetric.of(c,
                    MetricName.build(METRIC_CHILD_CACHE_NAME).tagged("repository", repository.getName())));
        }
        return c;
    }

    protected void removeCacheMetrics() {
        registry.removeMatching((name, metric) -> name.getKey().startsWith(METRIC_CACHE_NAME)
                || name.getKey().startsWith(METRIC_CHILD_CACHE_NAME));
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

    /**
     * Invalidations need to be propagated between connection caches only if there is such a cache, which is the case
     * only if transactions are used.
     */
    protected DBSInvalidationsPropagator initInvalidationsPropagator() {
        ClusterService clusterService = Framework.getService(ClusterService.class);
        if (clusterService.isEnabled() && supportsTransactions()) {
            return new DBSInvalidationsPropagator();
        } else {
            return null;
        }
    }

    protected DBSClusterInvalidator initClusterInvalidator(DBSRepositoryDescriptor descriptor) {
        ClusterService clusterService = Framework.getService(ClusterService.class);
        if (clusterService.isEnabled()) {
            DBSClusterInvalidator ci = createClusterInvalidator(descriptor);
            ci.initialize(clusterService.getNodeId(), getName());
            return ci;
        } else {
            return null;
        }
    }

    protected DBSClusterInvalidator createClusterInvalidator(DBSRepositoryDescriptor descriptor) {
        Class<? extends DBSClusterInvalidator> klass = descriptor.clusterInvalidatorClass;
        if (klass == null) {
            throw new NuxeoException(
                    "Unable to get cluster invalidator class from descriptor whereas clustering is enabled");
        }
        try {
            return klass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }

    @Override
    public void shutdown() {
        repository.shutdown();
        if (cache != null) {
            // Clear caches
            cache.invalidateAll();
            childCache.invalidateAll();
        }
        removeCacheMetrics();
        if (log.isInfoEnabled()) {
            log.info(String.format("DBS cache deactivated on '%s' repository", getName()));
        }
    }

    @Override
    @SuppressWarnings("resource") // connection closed by DBSCachingConnection.close
    public DBSConnection getConnection() {
        DBSConnection connection = repository.getConnection();
        return new DBSCachingConnection(connection, this);
    }

    @Override
    public boolean supportsTransactions() {
        return repository.supportsTransactions();
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
    public boolean isFulltextStoredInBlob() {
        return repository.isFulltextStoredInBlob();
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
    public LockManager getLockManager() {
        return repository.getLockManager();
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
        return repository.getSession();
    }

    @Override
    public void markReferencedBinaries() {
        repository.markReferencedBinaries();
    }

}
