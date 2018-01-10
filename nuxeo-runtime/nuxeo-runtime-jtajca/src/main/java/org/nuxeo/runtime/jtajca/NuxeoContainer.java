/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 */
package org.nuxeo.runtime.jtajca;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.spi.NamingManager;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.connector.outbound.AbstractConnectionManager;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.LocalTransactions;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.PoolingSupport;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.TransactionSupport;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.XATransactions;
import org.apache.geronimo.transaction.manager.NamedXAResourceFactory;
import org.apache.geronimo.transaction.manager.RecoverableTransactionManager;
import org.apache.geronimo.transaction.manager.TransactionImpl;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.apache.xbean.naming.reference.SimpleReference;
import org.nuxeo.common.logging.SequenceTracer;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManager.ActiveMonitor;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

/**
 * Internal helper for the Nuxeo-defined transaction manager and connection manager.
 * <p>
 * This code is called by the factories registered through JNDI, or by unit tests mimicking JNDI bindings.
 */
public class NuxeoContainer {

    protected static final Log log = LogFactory.getLog(NuxeoContainer.class);

    protected static RecoverableTransactionManager tmRecoverable;

    protected static TransactionManager tm;

    protected static TransactionSynchronizationRegistry tmSynchRegistry;

    protected static UserTransaction ut;

    protected static Map<String, ConnectionManagerWrapper> connectionManagers = new ConcurrentHashMap<>(8, 0.75f, 2);

    private static final List<NuxeoContainerListener> listeners = new ArrayList<>();

    private static volatile InstallContext installContext;

    protected static Context rootContext;

    protected static Context parentContext;

    protected static String jndiPrefix = "java:comp/env/";

    // @since 5.7
    protected static final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected static final Counter rollbackCount = registry.counter(
            MetricRegistry.name("nuxeo", "transactions", "rollbacks"));

    protected static final Counter concurrentCount = registry.counter(
            MetricRegistry.name("nuxeo", "transactions", "concurrents", "count"));

    protected static final Counter concurrentMaxCount = registry.counter(
            MetricRegistry.name("nuxeo", "transactions", "concurrents", "max"));

    protected static final Timer transactionTimer = registry.timer(
            MetricRegistry.name("nuxeo", "transactions", "duration"));

    protected static final ConcurrentHashMap<Transaction, Timer.Context> timers = new ConcurrentHashMap<>();

    private NuxeoContainer() {
    }

    public static class InstallContext extends Throwable {
        private static final long serialVersionUID = 1L;

        public final String threadName;

        InstallContext() {
            super("Container installation context (" + Thread.currentThread().getName() + ")");
            threadName = Thread.currentThread().getName();
        }
    }

    /**
     * Install naming and bind transaction and connection management factories "by hand".
     */
    protected static synchronized void install() throws NamingException {
        if (installContext != null) {
            throw new RuntimeException("Nuxeo container already installed");
        }
        installContext = new InstallContext();
        rootContext = new NamingContext();
        parentContext = InitialContextAccessor.getInitialContext();
        if (parentContext != null && parentContext != rootContext) {
            installTransactionManager(parentContext);
        } else {
            addDeepBinding(nameOf("TransactionManager"), new Reference(TransactionManager.class.getName(),
                    NuxeoTransactionManagerFactory.class.getName(), null));
            installTransactionManager(rootContext);
        }
    }

    protected static void installTransactionManager(TransactionManagerConfiguration config) throws NamingException {
        initTransactionManager(config);
        addDeepBinding(rootContext, new CompositeName(nameOf("TransactionManager")), getTransactionManagerReference());
        addDeepBinding(rootContext, new CompositeName(nameOf("UserTransaction")), getUserTransactionReference());
    }

    /**
     * Creates and installs in the container a new ConnectionManager.
     *
     * @param name the repository name
     * @param config the pool configuration
     * @return the created connection manager
     */
    public static synchronized ConnectionManagerWrapper installConnectionManager(
            NuxeoConnectionManagerConfiguration config) {
        String name = config.getName();
        ConnectionManagerWrapper cm = connectionManagers.get(name);
        if (cm != null) {
            return cm;
        }
        cm = initConnectionManager(config);
        // also bind it in JNDI
        if (rootContext != null) {
            String jndiName = nameOf("ConnectionManager/".concat(name));
            try {
                addDeepBinding(rootContext, new CompositeName(jndiName), getConnectionManagerReference(name));
            } catch (NamingException e) {
                log.error("Cannot bind in JNDI connection manager " + config.getName() + " to name " + jndiName);
            }
        }
        return cm;
    }

    public static boolean isInstalled() {
        return installContext != null;
    }

    protected static void uninstall() throws NamingException {
        if (installContext == null) {
            throw new RuntimeException("Nuxeo container not installed");
        }
        try {
            NamingException errors = new NamingException("Cannot shutdown connection managers");
            for (ConnectionManagerWrapper cm : connectionManagers.values()) {
                try {
                    cm.dispose();
                } catch (RuntimeException cause) {
                    errors.addSuppressed(cause);
                }
            }
            if (errors.getSuppressed().length > 0) {
                log.error("Cannot shutdown some pools", errors);
                throw errors;
            }
        } finally {
            log.trace("Uninstalling nuxeo container", installContext);
            installContext = null;
            rootContext = null;
            tm = null;
            tmRecoverable = null;
            tmSynchRegistry = null;
            ut = null;
            connectionManagers.clear();
        }
    }

    /**
     * @since 5.8
     */
    public static void addListener(NuxeoContainerListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
        for (Map.Entry<String, ConnectionManagerWrapper> entry : connectionManagers.entrySet()) {
            listener.handleNewConnectionManager(entry.getKey(), entry.getValue().cm);
        }
    }

    /**
     * @since 5.8
     */
    public static void removeListener(NuxeoContainerListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    protected static String detectJNDIPrefix(Context context) {
        String name = context.getClass().getName();
        if ("org.jnp.interfaces.NamingContext".equals(name)) { // JBoss
            return "java:";
        } else if ("org.jboss.as.naming.InitialContext".equals(name)) { // Wildfly
            return "java:jboss/";
        } else if ("org.mortbay.naming.local.localContextRoot".equals(name)) { // Jetty
            return "jdbc/";
        }
        // Standard JEE containers (Nuxeo-Embedded, Tomcat, GlassFish,
        // ...
        return "java:comp/env/";
    }

    public static String nameOf(String name) {
        return jndiPrefix.concat(name);
    }

    /**
     * Exposes the {@link #rootContext}.
     *
     * @since 5.7
     * @see https://jira.nuxeo.com/browse/NXP-10331
     */
    public static Context getRootContext() {
        return rootContext;
    }

    /**
     * Bind object in root context. Create needed sub contexts. since 5.6
     */
    public static void addDeepBinding(String name, Object obj) throws NamingException {
        addDeepBinding(rootContext, new CompositeName(name), obj);
    }

    protected static void addDeepBinding(Context dir, CompositeName comp, Object obj) throws NamingException {
        Name name = comp.getPrefix(1);
        if (comp.size() == 1) {
            addBinding(dir, name, obj);
            return;
        }
        Context subdir;
        try {
            subdir = (Context) dir.lookup(name);
        } catch (NamingException e) {
            subdir = dir.createSubcontext(name);
        }
        addDeepBinding(subdir, (CompositeName) comp.getSuffix(1), obj);
    }

    protected static void addBinding(Context dir, Name name, Object obj) throws NamingException {
        try {
            dir.rebind(name, obj);
        } catch (NamingException e) {
            dir.bind(name, obj);
        }
    }

    protected static void removeBinding(String name) throws NamingException {
        rootContext.unbind(name);
    }

    /**
     * Gets the transaction manager used by the container.
     *
     * @return the transaction manager
     */
    public static TransactionManager getTransactionManager() {
        return tm;
    }

    protected static Reference getTransactionManagerReference() {
        return new SimpleReference() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getContent() throws NamingException {
                return NuxeoContainer.getTransactionManager();
            }
        };
    }

    /**
     * Gets the user transaction used by the container.
     *
     * @return the user transaction
     */
    public static UserTransaction getUserTransaction() {
        return ut;
    }

    protected static Reference getUserTransactionReference() {
        return new SimpleReference() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getContent() throws NamingException {
                return getUserTransaction();
            }
        };
    }

    /**
     * Gets the Nuxeo connection manager used by the container.
     *
     * @return the connection manager
     */
    public static NuxeoConnectionManager getConnectionManager(String repositoryName) {
        ConnectionManagerWrapper wrapper = connectionManagers.get(repositoryName);
        if (wrapper == null) {
            return null;
        }
        return wrapper.cm;
    }

    public static void installConnectionManager(ConnectionManagerWrapper wrapper) {
        String name = wrapper.config.getName();
        if (connectionManagers.containsKey(name)) {
            log.error("Connection manager " + name + " already set up", new Exception());
        }
        connectionManagers.put(name, wrapper);
        for (NuxeoContainerListener listener : listeners) {
            listener.handleNewConnectionManager(name, wrapper.cm);
        }
    }

    protected static Reference getConnectionManagerReference(final String name) {
        return new SimpleReference() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getContent() throws NamingException {
                return getConnectionManager(name);
            }
        };
    }

    protected static synchronized TransactionManager initTransactionManager(TransactionManagerConfiguration config) {
        TransactionManagerImpl impl = createTransactionManager(config);
        tm = impl;
        tmRecoverable = impl;
        tmSynchRegistry = impl;
        ut = new UserTransactionImpl(tm);
        return tm;
    }

    protected static TransactionManagerWrapper wrapTransactionManager(TransactionManager tm) {
        if (tm == null) {
            return null;
        }
        if (tm instanceof TransactionManagerWrapper) {
            return (TransactionManagerWrapper) tm;
        }
        return new TransactionManagerWrapper(tm);
    }

    public static synchronized ConnectionManagerWrapper initConnectionManager(
            NuxeoConnectionManagerConfiguration config) {
        NuxeoConnectionTrackingCoordinator coordinator = new NuxeoConnectionTrackingCoordinator();
        NuxeoConnectionManager cm = createConnectionManager(coordinator, config);
        ConnectionManagerWrapper cmw = new ConnectionManagerWrapper(coordinator, cm, config);
        installConnectionManager(cmw);
        return cmw;
    }

    public static synchronized void disposeConnectionManager(ConnectionManager mgr) {
        ((ConnectionManagerWrapper) mgr).dispose();
    }

    // called by reflection from RepositoryReloader
    public static synchronized void resetConnectionManager() {
        RuntimeException errors = new RuntimeException("Cannot reset connection managers");
        for (ConnectionManagerWrapper wrapper : connectionManagers.values()) {
            try {
                wrapper.reset();
            } catch (RuntimeException cause) {
                errors.addSuppressed(cause);
            }
        }
        if (errors.getSuppressed().length > 0) {
            throw errors;
        }
    }

    public static synchronized void resetConnectionManager(String name) {
        connectionManagers.get(name).reset();
    }

    public static <T> T lookup(String name, Class<T> type) throws NamingException {
        if (rootContext == null) {
            throw new NamingException("no naming context available");
        }
        return lookup(rootContext, name, type);
    }

    public static <T> T lookup(Context context, String name, Class<T> type) throws NamingException {
        Object resolved;
        try {
            resolved = context.lookup(detectJNDIPrefix(context).concat(name));
        } catch (NamingException cause) {
            if (parentContext == null) {
                throw cause;
            }
            return type.cast(parentContext.lookup(detectJNDIPrefix(parentContext).concat(name)));
        }
        if (resolved instanceof Reference) {
            try {
                resolved = NamingManager.getObjectInstance(resolved, new CompositeName(name), rootContext, null);
            } catch (NamingException e) {
                throw e;
            } catch (Exception e) { // stupid JNDI API throws Exception
                throw ExceptionUtils.runtimeException(e);
            }
        }
        return type.cast(resolved);
    }

    protected static void installTransactionManager(Context context) throws NamingException {
        TransactionManager actual = lookup(context, "TransactionManager", TransactionManager.class);
        if (tm != null) {
            return;
        }
        tm = actual;
        tmRecoverable = wrapTransactionManager(tm);
        ut = new UserTransactionImpl(tm);
        tmSynchRegistry = (TransactionSynchronizationRegistry) tm;
    }

    protected static ConnectionManagerWrapper lookupConnectionManager(String repositoryName) throws NamingException {
        ConnectionManager cm = lookup(rootContext, "ConnectionManager/".concat(repositoryName),
                ConnectionManager.class);
        if (cm instanceof ConnectionManagerWrapper) {
            return (ConnectionManagerWrapper) cm;
        }
        log.warn("Connection manager not a wrapper, check your configuration");
        throw new RuntimeException(
                "Connection manager of " + repositoryName + " not a wrapper, check your configuration");
    }

    protected static TransactionManagerImpl createTransactionManager(TransactionManagerConfiguration config) {
        if (config == null) {
            config = new TransactionManagerConfiguration();
        }
        try {
            return new TransactionManagerImpl(config.transactionTimeoutSeconds);
        } catch (XAException e) {
            // failed in recovery somewhere
            throw new RuntimeException(e.toString(), e);
        }
    }

    /**
     * User transaction that uses this container's transaction manager.
     *
     * @since 5.6
     */
    public static class UserTransactionImpl implements UserTransaction {

        protected final TransactionManager transactionManager;

        public UserTransactionImpl(TransactionManager manager) {
            transactionManager = manager;
        }

        @Override
        public int getStatus() throws SystemException {
            return transactionManager.getStatus();
        }

        @Override
        public void setRollbackOnly() throws IllegalStateException, SystemException {
            transactionManager.setRollbackOnly();
        }

        @Override
        public void setTransactionTimeout(int seconds) throws SystemException {
            transactionManager.setTransactionTimeout(seconds);
        }

        @Override
        public void begin() throws NotSupportedException, SystemException {
            SequenceTracer.start("tx begin", "#DarkSalmon");
            transactionManager.begin();
            timers.put(transactionManager.getTransaction(), transactionTimer.time());
            concurrentCount.inc();
            if (concurrentCount.getCount() > concurrentMaxCount.getCount()) {
                concurrentMaxCount.inc();
            }
        }

        @Override
        public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException,
                RollbackException, SecurityException, SystemException {
            SequenceTracer.start("tx commiting", "#de6238");
            Transaction transaction = transactionManager.getTransaction();
            if (transaction == null) {
                throw new IllegalStateException("No transaction associated with current thread");
            }
            Timer.Context timerContext = timers.remove(transaction);
            transactionManager.commit();
            if (timerContext != null) {
                long elapsed = timerContext.stop();
                SequenceTracer.stop("tx commited");
                SequenceTracer.stop("tx end " + elapsed / 1000000 + " ms");
            }
            concurrentCount.dec();
        }

        @Override
        public void rollback() throws IllegalStateException, SecurityException, SystemException {
            SequenceTracer.mark("tx rollbacking");
            Transaction transaction = transactionManager.getTransaction();
            if (transaction == null) {
                throw new IllegalStateException("No transaction associated with current thread");
            }
            Timer.Context timerContext = timers.remove(transaction);
            transactionManager.rollback();
            concurrentCount.dec();
            if (timerContext != null) {
                long elapsed = timerContext.stop();
                SequenceTracer.destroy("tx rollbacked " + elapsed / 1000000 + " ms");
            }
            rollbackCount.inc();
        }
    }

    /**
     * Creates a Geronimo pooled connection manager using a Geronimo transaction manager.
     * <p>
     * The pool uses the transaction manager for recovery, and when using XATransactions for cache + enlist/delist.
     *
     * @throws NamingException
     */
    public static NuxeoConnectionManager createConnectionManager(NuxeoConnectionTrackingCoordinator coordinator,
            NuxeoConnectionManagerConfiguration config) {
        TransactionSupport transactionSupport = createTransactionSupport(config);
        PoolingSupport poolingSupport = createPoolingSupport(config);
        NuxeoValidationSupport validationSupport = createValidationSupport(config);
        return new NuxeoConnectionManager(config.getActiveTimeoutMinutes() * 60 * 1000, validationSupport,
                transactionSupport, poolingSupport, null, coordinator, tmRecoverable, config.getName(),
                Thread.currentThread().getContextClassLoader());
    }

    protected static PoolingSupport createPoolingSupport(NuxeoConnectionManagerConfiguration config) {
        return new NuxeoPool(config);
    }

    protected static TransactionSupport createTransactionSupport(NuxeoConnectionManagerConfiguration config) {
        if (config.getXAMode()) {
            // note: XATransactions -> TransactionCachingInterceptor ->
            // ConnectorTransactionContext casts transaction to Geronimo's
            // TransactionImpl (from TransactionManagerImpl)
            return new XATransactions(config.getUseTransactionCaching(), config.getUseThreadCaching());
        }
        return LocalTransactions.INSTANCE;
    }

    protected static NuxeoValidationSupport createValidationSupport(NuxeoConnectionManagerConfiguration config) {
        return new NuxeoValidationSupport(config.testOnBorrow, config.testOnReturn);
    }

    public static class TransactionManagerConfiguration {
        public int transactionTimeoutSeconds = 600;

        public void setTransactionTimeoutSeconds(int transactionTimeoutSeconds) {
            this.transactionTimeoutSeconds = transactionTimeoutSeconds;
        }
    }

    /**
     * Wraps a transaction manager for providing a dummy recoverable interface.
     *
     * @author matic
     */
    public static class TransactionManagerWrapper implements RecoverableTransactionManager {

        protected TransactionManager tm;

        public TransactionManagerWrapper(TransactionManager tm) {
            this.tm = tm;
        }

        @Override
        public Transaction suspend() throws SystemException {
            return tm.suspend();
        }

        @Override
        public void setTransactionTimeout(int seconds) throws SystemException {
            tm.setTransactionTimeout(seconds);
        }

        @Override
        public void setRollbackOnly() throws IllegalStateException, SystemException {
            tm.setRollbackOnly();
        }

        @Override
        public void rollback() throws IllegalStateException, SecurityException, SystemException {
            tm.rollback();
        }

        @Override
        public void resume(Transaction tobj)
                throws IllegalStateException, InvalidTransactionException, SystemException {
            tm.resume(tobj);
        }

        @Override
        public int getStatus() throws SystemException {
            return tm.getStatus();
        }

        @Override
        public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException,
                RollbackException, SecurityException, SystemException {
            tm.commit();
        }

        @Override
        public void begin() throws SystemException {
            try {
                tm.begin();
            } catch (javax.transaction.NotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void recoveryError(Exception e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void registerNamedXAResourceFactory(NamedXAResourceFactory factory) {
            if (!RecoverableTransactionManager.class.isAssignableFrom(tm.getClass())) {
                throw new UnsupportedOperationException();
            }
            ((RecoverableTransactionManager) tm).registerNamedXAResourceFactory(factory);
        }

        @Override
        public void unregisterNamedXAResourceFactory(String factory) {
            if (!RecoverableTransactionManager.class.isAssignableFrom(tm.getClass())) {
                throw new UnsupportedOperationException();
            }
            ((RecoverableTransactionManager) tm).unregisterNamedXAResourceFactory(factory);
        }

        @Override
        public Transaction getTransaction() throws SystemException {
            final Transaction tx = tm.getTransaction();
            if (tx instanceof TransactionImpl) {
                return tx;
            }
            return new TransactionImpl(null, null) {
                @Override
                public void commit() throws HeuristicMixedException, HeuristicRollbackException, RollbackException,
                        SecurityException, SystemException {
                    tx.commit();
                }

                @Override
                public void rollback() throws IllegalStateException, SystemException {
                    tx.rollback();
                }

                @Override
                public synchronized boolean enlistResource(XAResource xaRes)
                        throws IllegalStateException, RollbackException, SystemException {
                    return tx.enlistResource(xaRes);
                }

                @Override
                public synchronized boolean delistResource(XAResource xaRes, int flag)
                        throws IllegalStateException, SystemException {
                    return super.delistResource(xaRes, flag);
                }

                @Override
                public synchronized void setRollbackOnly() throws IllegalStateException {
                    try {
                        tx.setRollbackOnly();
                    } catch (SystemException e) {
                        throw new IllegalStateException(e);
                    }
                }

                @Override
                public void registerInterposedSynchronization(javax.transaction.Synchronization synchronization) {
                    try {
                        TransactionHelper.lookupSynchronizationRegistry()
                                         .registerInterposedSynchronization(synchronization);
                    } catch (NamingException e) {
                        ;
                    }
                }
            };
        }
    }

    /**
     * Wraps a Geronimo ConnectionManager and adds a {@link #reset} method to flush the pool.
     */
    public static class ConnectionManagerWrapper implements ConnectionManager {

        private static final long serialVersionUID = 1L;

        protected NuxeoConnectionTrackingCoordinator coordinator;

        protected volatile NuxeoConnectionManager cm;

        protected final NuxeoConnectionManagerConfiguration config;

        public ConnectionManagerWrapper(NuxeoConnectionTrackingCoordinator coordinator, NuxeoConnectionManager cm,
                NuxeoConnectionManagerConfiguration config) {
            this.coordinator = coordinator;
            this.cm = cm;
            this.config = config;
        }

        @Override
        public Object allocateConnection(ManagedConnectionFactory managedConnectionFactory,
                ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
            return cm.allocateConnection(managedConnectionFactory, connectionRequestInfo);
        }

        public void reset() {
            AbstractConnectionManager last = cm;
            cm = createConnectionManager(coordinator, config);
            try {
                last.doStop();
            } catch (Exception e) { // stupid Geronimo API throws Exception
                throw ExceptionUtils.runtimeException(e);
            }
            for (NuxeoContainerListener listener : listeners) {
                listener.handleConnectionManagerReset(config.getName(), cm);
            }
        }

        public List<ActiveMonitor.TimeToLive> killActiveTimedoutConnections(long clock) {
            return cm.activemonitor.killTimedoutConnections(clock);
        }

        public void dispose() {
            for (NuxeoContainerListener listener : listeners) {
                listener.handleConnectionManagerDispose(config.getName(), cm);
            }
            cm.activemonitor.cancelCleanups();
            NuxeoContainer.connectionManagers.remove(config.getName());
            try {
                cm.doStop();
            } catch (Exception e) { // stupid Geronimo API throws Exception
                throw ExceptionUtils.runtimeException(e);
            }
        }

        public NuxeoConnectionManagerConfiguration getConfiguration() {
            return config;
        }

        public NuxeoConnectionManager getManager() {
            return cm;
        }

    }

    public static TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return tmSynchRegistry;
    }

}
