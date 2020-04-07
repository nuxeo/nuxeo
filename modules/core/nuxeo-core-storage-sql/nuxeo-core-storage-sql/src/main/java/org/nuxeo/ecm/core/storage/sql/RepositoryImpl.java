/*
 * (C) Copyright 2006-2018 Nuxeo (http://nuxeo.com/) and others.
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
import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.storage.lock.LockManagerService;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLSession;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCBackend;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterService;
import org.nuxeo.runtime.metrics.MetricsService;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.Gauge;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;

/**
 * {@link Repository} implementation, to be extended by backend-specific initialization code.
 *
 * @see RepositoryBackend
 */
public class RepositoryImpl implements Repository, org.nuxeo.ecm.core.model.Repository {

    private static final Log log = LogFactory.getLog(RepositoryImpl.class);

    protected final RepositoryDescriptor repositoryDescriptor;

    private RepositoryBackend backend;

    private final Collection<SessionImpl> sessions;

    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final Counter sessionCount;

    private LockManager lockManager;

    /**
     * @since 7.4 : used to know if the LockManager was provided by this repository or externally
     */
    protected boolean selfRegisteredLockManager = false;

    /** Propagator of invalidations to all mappers' caches. */
    // public for tests
    public final VCSInvalidationsPropagator invalidationsPropagator;

    protected VCSClusterInvalidator clusterInvalidator;

    public boolean requiresClusterSQL;

    private Model model;

    /**
     * Transient id for this repository assigned by the server on first connection. This is not persisted.
     */
    public String repositoryId;

    public RepositoryImpl(RepositoryDescriptor repositoryDescriptor) {
        this.repositoryDescriptor = repositoryDescriptor;
        sessions = new CopyOnWriteArrayList<>();
        invalidationsPropagator = new VCSInvalidationsPropagator();

        sessionCount = registry.counter(MetricName.build("nuxeo", "repositories", "repository", "sessions")
                                                  .tagged("repository", repositoryDescriptor.name));
        createMetricsGauges();

        initRepository();
    }

    protected void createMetricsGauges() {
        MetricName gaugeName = MetricName.build("nuxeo", "repositories", "repository", "cache", "size")
                                         .tagged("repository", repositoryDescriptor.name);
        registry.remove(gaugeName);
        registry.register(gaugeName, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getCacheSize();
            }
        });
        gaugeName = MetricName.build("nuxeo", "repositories", "repository", "cache", "pristine")
                              .tagged("repository", repositoryDescriptor.name);
        registry.remove(gaugeName);
        registry.register(gaugeName, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getCachePristineSize();
            }
        });
        gaugeName = MetricName.build("nuxeo", "repositories", "repository", "cache", "selection")
                              .tagged("repository", repositoryDescriptor.name);
        registry.remove(gaugeName);
        registry.register(gaugeName, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getCacheSelectionSize();
            }
        });
        gaugeName = MetricName.build("nuxeo", "repositories", "repository", "cache", "mapper")
                .tagged("repository", repositoryDescriptor.name);
        registry.remove(gaugeName);
        registry.register(gaugeName, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getCacheMapperSize();
            }
        });
    }

    protected RepositoryBackend createBackend() {
        Class<? extends RepositoryBackend> backendClass = repositoryDescriptor.backendClass;
        if (backendClass == null) {
            backendClass = JDBCBackend.class;
        }
        try {
            return backendClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }

    protected Mapper createCachingMapper(Model model, Mapper mapper) {
        try {
            Class<? extends CachingMapper> cachingMapperClass = getCachingMapperClass();
            if (cachingMapperClass == null) {
                return mapper;
            }
            CachingMapper cachingMapper = cachingMapperClass.getDeclaredConstructor().newInstance();
            cachingMapper.initialize(getName(), model, mapper, invalidationsPropagator,
                    repositoryDescriptor.cachingMapperProperties);
            return cachingMapper;
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }

    protected Class<? extends CachingMapper> getCachingMapperClass() {
        if (!repositoryDescriptor.getCachingMapperEnabled()) {
            return null;
        }
        Class<? extends CachingMapper> cachingMapperClass = repositoryDescriptor.cachingMapperClass;
        if (cachingMapperClass == null) {
            // default cache
            cachingMapperClass = SoftRefCachingMapper.class;
        }
        return cachingMapperClass;
    }

    public RepositoryDescriptor getRepositoryDescriptor() {
        return repositoryDescriptor;
    }

    public LockManager getLockManager() {
        return lockManager;
    }

    public Model getModel() {
        return model;
    }

    /** @since 11.1 */
    public RepositoryBackend getBackend() {
        return backend;
    }

    public VCSInvalidationsPropagator getInvalidationsPropagator() {
        return invalidationsPropagator;
    }

    public boolean isChangeTokenEnabled() {
        return repositoryDescriptor.isChangeTokenEnabled();
    }

    @Override
    public SQLSession getSession() {
        return new SQLSession(getConnection(), this); // NOSONAR
    }

    /**
     * Gets a new connection.
     *
     * @return the session
     */
    @Override
    public synchronized SessionImpl getConnection() {
        if (Framework.getRuntime().isShuttingDown()) {
            throw new IllegalStateException("Cannot open connection, runtime is shutting down");
        }
        SessionPathResolver pathResolver = new SessionPathResolver();
        Mapper mapper = newMapper(pathResolver, true);
        SessionImpl session = newSession(model, mapper);
        pathResolver.setSession(session);

        sessions.add(session);
        sessionCount.inc();
        return session;
    }

    // callback by session at close time
    protected void closeSession(SessionImpl session) {
        sessions.remove(session);
        sessionCount.dec();
    }

    /**
     * Creates a new mapper.
     *
     * @param pathResolver the path resolver (for regular mappers)
     * @param useInvalidations whether this mapper participates in invalidation propagation (false for lock manager /
     *            cluster invalidator)
     * @return the new mapper.
     * @since 7.4
     */
    public Mapper newMapper(PathResolver pathResolver, boolean useInvalidations) {
        return backend.newMapper(pathResolver, useInvalidations);
    }

    protected void initRepository() {
        log.debug("Initializing");
        backend = createBackend();
        prepareClusterInvalidator(); // sets requiresClusterSQL used by backend init
        model = backend.initialize(this);
        initLockManager();
        initClusterInvalidator();

        // log once which mapper cache is being used
        Class<? extends CachingMapper> cachingMapperClass = getCachingMapperClass();
        if (cachingMapperClass == null) {
            log.warn("VCS Mapper cache is disabled.");
        } else {
            log.info("VCS Mapper cache using: " + cachingMapperClass.getName());
        }

        initRootNode();
    }

    protected void initRootNode() {
        // access a session once so that SessionImpl.computeRootNode can create the root node
        try (SessionImpl session = getConnection()) {
            // nothing
        }
    }

    protected String getLockManagerName() {
        // TODO configure in repo descriptor
        return getName();
    }

    protected void initLockManager() {
        String lockManagerName = getLockManagerName();
        LockManagerService lockManagerService = Framework.getService(LockManagerService.class);
        lockManager = lockManagerService.getLockManager(lockManagerName);
        if (lockManager == null) {
            // no descriptor
            // default to a VCSLockManager
            lockManager = new VCSLockManager(this);
            lockManagerService.registerLockManager(lockManagerName, lockManager);
            selfRegisteredLockManager = true;
        } else {
            selfRegisteredLockManager = false;
        }
        log.info("Repository " + getName() + " using lock manager " + lockManager);
    }

    protected void prepareClusterInvalidator() {
        if (Framework.getService(ClusterService.class).isEnabled()) {
            clusterInvalidator = createClusterInvalidator();
            requiresClusterSQL = clusterInvalidator.requiresClusterSQL();
        }
    }

    protected VCSClusterInvalidator createClusterInvalidator() {
        Class<? extends VCSClusterInvalidator> klass = repositoryDescriptor.clusterInvalidatorClass;
        if (klass == null) {
            klass = VCSPubSubInvalidator.class;
        }
        try {
            return klass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
        }
    }

    protected void initClusterInvalidator() {
        if (clusterInvalidator != null) {
            String nodeId = Framework.getService(ClusterService.class).getNodeId();
            clusterInvalidator.initialize(nodeId, this);
            backend.setClusterInvalidator(clusterInvalidator);
        }
    }

    protected SessionImpl newSession(Model model, Mapper mapper) {
        mapper = createCachingMapper(model, mapper);
        return new SessionImpl(this, model, mapper);
    }

    public static class SessionPathResolver implements PathResolver {

        private Session session;

        protected void setSession(Session session) {
            this.session = session;
        }

        @Override
        public Serializable getIdForPath(String path) {
            Node node = session.getNodeByPath(path, null);
            return node == null ? null : node.getId();
        }
    }

    /*
     * ----- Repository -----
     */

    @Override
    public void shutdown() {
        close();
    }

    @Override
    public synchronized void close() {
        closeAllSessions();
        model = null;
        backend.shutdown();

        if (selfRegisteredLockManager) {
            LockManagerService lms = Framework.getService(LockManagerService.class);
            if (lms != null) {
                lms.unregisterLockManager(getLockManagerName());
            }
        }
    }

    protected synchronized void closeAllSessions() {
        for (SessionImpl session : sessions) {
            session.closeSession();
        }
        sessions.clear();
        sessionCount.dec(sessionCount.getCount());
        if (lockManager != null) {
            lockManager.closeLockManager();
        }
    }

    /*
     * ----- RepositoryManagement -----
     */

    @Override
    public String getName() {
        return repositoryDescriptor.name;
    }

    @Override
    public int clearCaches() {
        int n = 0;
        for (SessionImpl session : sessions) {
            n += session.clearCaches();
        }
        if (lockManager != null) {
            lockManager.clearLockManagerCaches();
        }
        return n;
    }

    @Override
    public long getCacheSize() {
        long size = 0;
        for (SessionImpl session : sessions) {
            size += session.getCacheSize();
        }
        return size;
    }

    public long getCacheMapperSize() {
        long size = 0;
        for (SessionImpl session : sessions) {
            size += session.getCacheMapperSize();
        }
        return size;
    }

    @Override
    public long getCachePristineSize() {
        long size = 0;
        for (SessionImpl session : sessions) {
            size += session.getCachePristineSize();
        }
        return size;
    }

    @Override
    public long getCacheSelectionSize() {
        long size = 0;
        for (SessionImpl session : sessions) {
            size += session.getCacheSelectionSize();
        }
        return size;
    }

    @Override
    public void processClusterInvalidationsNext() {
        // TODO pass through or something
    }

    @Override
    public void markReferencedBinaries() {
        try (SessionImpl session = getConnection()) {
            session.markReferencedBinaries();
        }
    }

    @Override
    public int cleanupDeletedDocuments(int max, Calendar beforeTime) {
        if (!repositoryDescriptor.getSoftDeleteEnabled()) {
            return 0;
        }
        try (SessionImpl session = getConnection()) {
            return session.cleanupDeletedDocuments(max, beforeTime);
        }
    }

    @Override
    public FulltextConfiguration getFulltextConfiguration() {
        return model.getFulltextConfiguration();
    }

}
