/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.repository;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.PoolUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.nuxeo.common.utils.DurationUtils;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.model.Repository;
import org.nuxeo.ecm.core.model.Session;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.cluster.ClusterService;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManagerConfiguration;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.ComponentStartOrders;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Component and service managing low-level repository instances.
 */
public class RepositoryService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName("org.nuxeo.ecm.core.repository.RepositoryService");

    /** @since 11.1 */
    public static final String CLUSTER_START_DURATION_PROP = "org.nuxeo.repository.cluster.start.duration";

    /** @since 11.1 */
    public static final Duration CLUSTER_START_DURATION_DEFAULT = Duration.ofMinutes(1);

    private static final Log log = LogFactory.getLog(RepositoryService.class);

    public static final String XP_REPOSITORY = "repository";

    private final Map<String, Repository> repositories = new ConcurrentHashMap<>();

    // for monitoring
    protected GenericKeyedObjectPool<String, Session> basePool;

    protected KeyedObjectPool<String, Session> pool;

    public void shutdown() {
        log.info("Shutting down repository manager");
        repositories.values().forEach(Repository::shutdown);
        repositories.clear();
    }

    @Override
    public int getApplicationStartedOrder() {
        return ComponentStartOrders.REPOSITORY;
    }

    @Override
    public void start(ComponentContext context) {
        initPool();
        TransactionHelper.runInTransaction(this::doCreateRepositories);
        Framework.getRuntime().getComponentManager().addListener(new ComponentManager.Listener() {
            @Override
            public void afterStart(ComponentManager mgr, boolean isResume) {
                initRepositories(); // call all RepositoryInitializationHandler
            }

            @Override
            public void afterStop(ComponentManager mgr, boolean isStandby) {
                Framework.getRuntime().getComponentManager().removeListener(this);
            }
        });
    }

    @Override
    public void stop(ComponentContext context) {
        TransactionHelper.runInTransaction(this::shutdown);
        shutdownPool();
    }

    protected void initPool() {
        // TODO find descriptor from default repository
        NuxeoConnectionManagerConfiguration ncmc = null;
        if (ncmc == null) {
            ncmc = new NuxeoConnectionManagerConfiguration();
        }
        GenericKeyedObjectPoolConfig<Session> config = new GenericKeyedObjectPoolConfig<>();
        config.setMaxTotal(ncmc.getMaxPoolSize());
        config.setMaxIdlePerKey(8); // default
        config.setMinIdlePerKey(ncmc.getMinPoolSize());
        config.setBlockWhenExhausted(true);
        config.setMaxWaitMillis(ncmc.getBlockingTimeoutMillis());
        basePool = new GenericKeyedObjectPool<>(new SessionFactory(), config);
        // use an eroding pool to avoid keeping idle sessions too long
        pool = PoolUtils.erodingPool(basePool);
    }

    protected void shutdownPool() {
        pool.close();
    }

    public void resetPool() {
        basePool.clear();
    }

    // for monitoring
    public GenericKeyedObjectPool<String, ?> getPool() {
        return basePool;
    }

    /**
     * Start a tx and initialize repositories content. This method is publicly exposed since it is needed by tests to
     * initialize repositories after cleanups (see CoreFeature).
     *
     * @since 8.4
     */
    public void initRepositories() {
        TransactionHelper.runInTransaction(this::doInitRepositories);
    }

    /**
     * Creates all the repositories. Requires an active transaction.
     *
     * @since 9.3
     */
    protected void doCreateRepositories() {
        repositories.clear();
        for (String repositoryName : getRepositoryNames()) {
            RepositoryFactory factory = getFactory(repositoryName);
            if (factory == null) {
                continue;
            }
            createRepository(repositoryName, factory);
        }
    }

    protected void createRepository(String repositoryName, RepositoryFactory factory) {
        ClusterService clusterService = Framework.getService(ClusterService.class);
        String prop = Framework.getProperty(CLUSTER_START_DURATION_PROP);
        Duration duration = DurationUtils.parsePositive(prop, CLUSTER_START_DURATION_DEFAULT);
        Duration pollDelay = Duration.ofSeconds(1);
        clusterService.runAtomically("start-repository-" + repositoryName, duration, pollDelay, () -> {
            Repository repository = (Repository) factory.call();
            repositories.put(repositoryName, repository);
        });
    }

    /**
     * Initializes all the repositories. Requires an active transaction.
     *
     * @since 9.3
     */
    protected void doInitRepositories() {
        // give up if no handler configured
        RepositoryInitializationHandler handler = RepositoryInitializationHandler.getInstance();
        if (handler == null) {
            return;
        }
        // invoke handlers
        for (String name : getRepositoryNames()) {
            initializeRepository(handler, name);
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(getClass())) {
            return adapter.cast(this);
        }
        return null;
    }

    protected void initializeRepository(final RepositoryInitializationHandler handler, String name) {
        new UnrestrictedSessionRunner(name) {
            @Override
            public void run() {
                handler.initializeRepository(session);
            }
        }.runUnrestricted();
    }

    /**
     * Gets a repository given its name.
     * <p>
     * Null is returned if no repository with that name was registered.
     *
     * @param repositoryName the repository name
     * @return the repository instance or null if no repository with that name was registered
     */
    public Repository getRepository(String repositoryName) {
        return repositories.get(repositoryName);
    }

    protected RepositoryFactory getFactory(String repositoryName) {
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        if (repositoryManager == null) {
            // tests with no high-level repository manager
            return null;
        }
        org.nuxeo.ecm.core.api.repository.Repository repo = repositoryManager.getRepository(repositoryName);
        if (repo == null) {
            return null;
        }
        RepositoryFactory repositoryFactory = (RepositoryFactory) repo.getRepositoryFactory();
        if (repositoryFactory == null) {
            throw new NullPointerException("Missing repositoryFactory for repository: " + repositoryName);
        }
        return repositoryFactory;
    }

    public List<String> getRepositoryNames() {
        RepositoryManager repositoryManager = Framework.getService(RepositoryManager.class);
        return repositoryManager.getRepositoryNames();
    }

    public int getActiveSessionsCount() {
        return pool.getNumActive();
    }

    public int getActiveSessionsCount(String repositoryName) {
        return pool.getNumActive(repositoryName);
    }

    /**
     * Thread-local sessions allocated, per repository.
     */
    protected static final Map<String, ThreadLocal<Session>> SESSIONS = new ConcurrentHashMap<>(1);

    /**
     * Gets a session.
     * <p>
     * The session is first looked up in the current transaction, otherwise fetched from a pool.
     *
     * @param repositoryName the repository name
     * @return the session
     * @since 11.1
     */
    public Session getSession(String repositoryName) {
        if (!TransactionHelper.isTransactionActive()) {
            if (TransactionHelper.isTransactionMarkedRollback()) {
                throw new NuxeoException("Cannot use a session when transaction is marked rollback-only");
            } else {
                throw new NuxeoException("Cannot use a session outside a transaction");
            }
        }
        TransactionHelper.checkTransactionTimeout();
        ThreadLocal<Session> threadSessions = SESSIONS.computeIfAbsent(repositoryName, r -> new ThreadLocal<>());
        Session session = threadSessions.get();
        if (session == null) {
            session = getSessionFromPool(repositoryName, threadSessions::remove);
            threadSessions.set(session);
        }
        return session;
    }

    protected Session getSessionFromPool(String repositoryName, Runnable cleanup) {
        Session session;
        try {
            session = pool.borrowObject(repositoryName);
        } catch (NoSuchElementException e) {
            // TODO find descriptor from default repository
            NuxeoConnectionManagerConfiguration ncmc = new NuxeoConnectionManagerConfiguration();
            String err = String.format(
                    "Connection pool is fully used,"
                            + " consider increasing nuxeo.vcs.blocking-timeout-millis (currently %s)"
                            + " or nuxeo.vcs.max-pool-size (currently %s)",
                    ncmc.getBlockingTimeoutMillis(), ncmc.getMaxPoolSize());
            throw new NuxeoException(err, e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) { // NOSONAR
                Thread.currentThread().interrupt();
            }
            throw new NuxeoException(e);
        }
        // register session as XAResource with transaction
        TransactionHelper.enlistResource(session);
        // register cleanup to return to pool and remove from thread-local at end of transaction
        TransactionHelper.registerSynchronization(new SessionCleanup(session, cleanup));
        return session;
    }

    /** @since 11.1 */
    protected class SessionCleanup implements Synchronization {

        protected final Session session;

        protected final Runnable cleanup;

        protected SessionCleanup(Session session, Runnable cleanup) {
            this.session = session;
            this.cleanup = cleanup;
        }

        @Override
        public void beforeCompletion() {
            session.beforeCompletion();
        }

        @Override
        public void afterCompletion(int status) {
            String repositoryName = session.getRepositoryName();
            try {
                if (status == Status.STATUS_COMMITTED) {
                    pool.returnObject(repositoryName, session);
                } else {
                    pool.invalidateObject(repositoryName, session);
                    if (status != Status.STATUS_ROLLEDBACK) {
                        log.error("Unexpected afterCompletion status: " + status);
                    }
                }
            } catch (Exception e) {
                if (e instanceof InterruptedException) { // NOSONAR
                    Thread.currentThread().interrupt();
                }
                log.error(e, e);
            } finally {
                cleanup.run();
            }
        }
    }

    /** @since 11.1 */
    protected class SessionFactory extends BaseKeyedPooledObjectFactory<String, Session> {

        @Override
        public Session create(String repositoryName) throws Exception {
            Repository repository = getRepository(repositoryName);
            if (repository == null) {
                throw new DocumentNotFoundException("No such repository: " + repositoryName);
            }
            return repository.getSession();
        }

        @Override
        public PooledObject<Session> wrap(Session session) {
            return new DefaultPooledObject<>(session);
        }

        @Override
        public void destroyObject(String repositoryName, PooledObject<Session> p) throws Exception {
            p.getObject().destroy();
        }
    }

}
