/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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

import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.ecm.core.storage.State;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterService;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * The DBS Cache layer used to cache some method call of real repository
 *
 * @since 8.10
 */
public class DBSCachingRepository implements DBSRepository {

    private static final Log log = LogFactory.getLog(DBSCachingRepository.class);

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
        initClusterInvalidator(descriptor);
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
        ClusterService clusterService = Framework.getService(ClusterService.class);
        if (clusterService.isEnabled()) {
            clusterInvalidator = createClusterInvalidator(descriptor);
            clusterInvalidator.initialize(clusterService.getNodeId(), getName());
        }
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

    @SuppressWarnings("resource") // connection closed by DBSCachingConnection.close
    @Override
    public DBSConnection getConnection() {
        DBSConnection connection = repository.getConnection();
        return new DBSCachingConnection(connection , this);
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
    public Session<?> getSession() {
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
