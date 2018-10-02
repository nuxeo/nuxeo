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
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.repository.FulltextConfiguration;
import org.nuxeo.ecm.core.model.LockManager;
import org.nuxeo.ecm.core.storage.lock.LockManagerService;
import org.nuxeo.ecm.core.storage.sql.Session.PathResolver;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCBackend;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCClusterInvalidator;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

/**
 * {@link Repository} implementation, to be extended by backend-specific initialization code.
 *
 * @see RepositoryBackend
 */
public class RepositoryImpl implements Repository {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(RepositoryImpl.class);

    private static final Random RANDOM = new SecureRandom();

    protected final RepositoryDescriptor repositoryDescriptor;

    private RepositoryBackend backend;

    private final Collection<SessionImpl> sessions;

    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final Counter repositoryUp;

    protected final Counter sessionCount;

    private LockManager lockManager;

    /**
     * @since 7.4 : used to know if the LockManager was provided by this repository or externally
     */
    protected boolean selfRegisteredLockManager = false;

    /** Propagator of invalidations to all mappers' caches. */
    protected final InvalidationsPropagator invalidationsPropagator;

    private Model model;

    /**
     * Transient id for this repository assigned by the server on first connection. This is not persisted.
     */
    public String repositoryId;

    public RepositoryImpl(RepositoryDescriptor repositoryDescriptor) {
        this.repositoryDescriptor = repositoryDescriptor;
        sessions = new CopyOnWriteArrayList<>();
        invalidationsPropagator = new InvalidationsPropagator();

        repositoryUp = registry.counter(MetricRegistry.name("nuxeo", "repositories", repositoryDescriptor.name,
                "instance-up"));
        repositoryUp.inc();
        sessionCount = registry.counter(MetricRegistry.name("nuxeo", "repositories", repositoryDescriptor.name,
                "sessions"));
        createMetricsGauges();

        initRepository();
    }

    protected void createMetricsGauges() {
        String gaugeName = MetricRegistry.name("nuxeo", "repositories", repositoryDescriptor.name, "caches", "size");
        registry.remove(gaugeName);
        registry.register(gaugeName, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getCacheSize();
            }
        });
        gaugeName = MetricRegistry.name("nuxeo", "repositories", repositoryDescriptor.name, "caches", "pristines");
        registry.remove(gaugeName);
        registry.register(gaugeName, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getCachePristineSize();
            }
        });
        gaugeName = MetricRegistry.name("nuxeo", "repositories", repositoryDescriptor.name, "caches", "selections");
        registry.remove(gaugeName);
        registry.register(gaugeName, new Gauge<Long>() {
            @Override
            public Long getValue() {
                return getCacheSelectionSize();
            }
        });
        gaugeName = MetricRegistry.name("nuxeo", "repositories", repositoryDescriptor.name, "caches", "mappers");
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
            RepositoryBackend backend = backendClass.newInstance();
            return backend;
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
            CachingMapper cachingMapper = cachingMapperClass.newInstance();
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

    public InvalidationsPropagator getInvalidationsPropagator() {
        return invalidationsPropagator;
    }

    public boolean isChangeTokenEnabled() {
        return repositoryDescriptor.isChangeTokenEnabled();
    }

    /*
     * ----- javax.resource.cci.ConnectionFactory -----
     */

    /**
     * Gets a new connection.
     *
     * @param connectionSpec the parameters to use to connect (unused)
     * @return the session
     */
    @Override
    public SessionImpl getConnection(ConnectionSpec connectionSpec) {
        return getConnection();
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
        model = backend.initialize(this);
        initLockManager();

        // create the cluster invalidator
        if (repositoryDescriptor.getClusteringEnabled()) {
            initClusterInvalidator();
        }

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
        try {
            // access a session once so that SessionImpl.computeRootNode can create the root node
            getConnection().close();
        } catch (ResourceException e) {
            throw new RuntimeException(e);
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

    protected void initClusterInvalidator() {
        String nodeId = repositoryDescriptor.getClusterNodeId();
        if (StringUtils.isBlank(nodeId)) {
            // need a smallish int because of SQL Server legacy node ids
            nodeId = String.valueOf(RANDOM.nextInt(32768));
            log.warn("Missing cluster node id configuration, please define it explicitly (usually through repository.clustering.id). "
                    + "Using random cluster node id instead: " + nodeId);
        } else {
            nodeId = nodeId.trim();
        }
        ClusterInvalidator clusterInvalidator = createClusterInvalidator();
        clusterInvalidator.initialize(nodeId, this);
        backend.setClusterInvalidator(clusterInvalidator);
    }

    protected ClusterInvalidator createClusterInvalidator() {
        Class<? extends ClusterInvalidator> klass = repositoryDescriptor.clusterInvalidatorClass;
        if (klass == null) {
            klass = JDBCClusterInvalidator.class;
        }
        try {
            return klass.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new NuxeoException(e);
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
     * -----
     */

    @Override
    public ResourceAdapterMetaData getMetaData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public RecordFactory getRecordFactory() {
        throw new UnsupportedOperationException();
    }

    /*
     * ----- javax.resource.Referenceable -----
     */

    private Reference reference;

    @Override
    public void setReference(Reference reference) {
        this.reference = reference;
    }

    @Override
    public Reference getReference() {
        return reference;
    }

    /*
     * ----- Repository -----
     */

    @Override
    public synchronized void close() {
        closeAllSessions();
        model = null;
        backend.shutdown();

        registry.remove(MetricRegistry.name(RepositoryImpl.class, getName(), "cache-size"));
        registry.remove(MetricRegistry.name(PersistenceContext.class, getName(), "cache-size"));
        registry.remove(MetricRegistry.name(SelectionContext.class, getName(), "cache-size"));

        if (selfRegisteredLockManager) {
            LockManagerService lms = Framework.getService(LockManagerService.class);
            if (lms != null) {
                lms.unregisterLockManager(getLockManagerName());
            }
        }
    }

    protected synchronized void closeAllSessions() {
        for (SessionImpl session : sessions) {
            if (!session.isLive()) {
                continue;
            }
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
    public int getActiveSessionsCount() {
        return sessions.size();
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
        try {
            SessionImpl conn = getConnection();
            try {
                conn.markReferencedBinaries();
            } finally {
                conn.close();
            }
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int cleanupDeletedDocuments(int max, Calendar beforeTime) {
        if (!repositoryDescriptor.getSoftDeleteEnabled()) {
            return 0;
        }
        try {
            SessionImpl conn = getConnection();
            try {
                return conn.cleanupDeletedDocuments(max, beforeTime);
            } finally {
                conn.close();
            }
        } catch (ResourceException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FulltextConfiguration getFulltextConfiguration() {
        return model.getFulltextConfiguration();
    }

    /*
     * ----- -----
     */

    // callback by session at close time
    protected void closeSession(SessionImpl session) {
        sessions.remove(session);
        sessionCount.dec();
    }

}
