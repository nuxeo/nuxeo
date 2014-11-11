/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 *     Julien Carsique
 */
package org.nuxeo.runtime.jtajca;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.connector.outbound.AbstractConnectionManager;
import org.apache.geronimo.connector.outbound.GenericConnectionManager;
import org.apache.geronimo.connector.outbound.SubjectSource;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.PartitionedPool;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.PoolingSupport;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.TransactionSupport;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.XATransactions;
import org.apache.geronimo.connector.outbound.connectiontracking.ConnectionTrackingCoordinator;
import org.apache.geronimo.transaction.manager.NamedXAResourceFactory;
import org.apache.geronimo.transaction.manager.RecoverableTransactionManager;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.apache.xbean.naming.reference.SimpleReference;
import org.nuxeo.runtime.api.InitialContextAccessor;
import org.nuxeo.runtime.metrics.MetricsService;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

/**
 * Internal helper for the Nuxeo-defined transaction manager and connection
 * manager.
 * <p>
 * This code is called by the factories registered through JNDI, or by unit
 * tests mimicking JNDI bindings.
 */
public class NuxeoContainer {

    protected static final Log log = LogFactory.getLog(NuxeoContainer.class);

    public static final String JNDI_TRANSACTION_MANAGER = "java:comp/TransactionManager";

    public static final String JNDI_USER_TRANSACTION = "java:comp/UserTransaction";

    public static final String JNDI_NUXEO_CONNECTION_MANAGER_PREFIX = "java:comp/NuxeoConnectionManager/";

    private static TransactionManagerWrapper transactionManager;

    private static final UserTransaction userTransaction = new UserTransactionImpl();

    /**
     * Per-repository connection managers.
     */
    private static Map<String, ConnectionManagerWrapper> connectionManagers = new ConcurrentHashMap<String, ConnectionManagerWrapper>(
            8, 0.75f, 2);

    private static final List<NuxeoContainerListener> listeners = new ArrayList<NuxeoContainerListener>();

    private static InstallContext installContext;

    private static Context parentContext;

    private static Map<String, Object> parentEnvironment = new HashMap<String, Object>();

    protected static Context rootContext;

    // @since 5.7
    protected static final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected static final Counter rollbackCount = registry.counter(MetricRegistry.name(
            "nuxeo", "transactions", "rollbacks"));

    protected static final Counter concurrentCount = registry.counter(MetricRegistry.name(
            "nuxeo", "transactions", "concurrents", "count"));

    protected static final Counter concurrentMaxCount = registry.counter(MetricRegistry.name(
            "nuxeo", "transactions", "concurrents", "max"));

    protected static final Timer transactionTimer  = registry.timer(MetricRegistry.name(
            "nuxeo", "transactions", "duration"));

    protected static final ConcurrentHashMap<Transaction, Timer.Context> timers = new ConcurrentHashMap<Transaction, Timer.Context>();

    private NuxeoContainer() {
    }

    public static class InstallContext extends Throwable {
        private static final long serialVersionUID = 1L;

        public final String threadName;

        InstallContext() {
            super("Container installation context ("
                    + Thread.currentThread().getName() + ")");
            threadName = Thread.currentThread().getName();
        }
    }

    /**
     * Install naming and bind transaction and connection management factories
     * "by hand".
     */
    public static synchronized void install() throws NamingException {
        if (installContext != null) {
            throw new RuntimeException("Nuxeo container already installed");
        }
        install(null);
    }

    /**
     * Install transaction and connection management "by hand" if the container
     * didn't do it using file-based configuration. Binds the names in JNDI.
     *
     * @param txconfig the transaction manager configuration
     *
     * @since 5.4.2
     */
    public static synchronized void install(
            TransactionManagerConfiguration txconfig) throws NamingException {
        installNaming();
        installTransactionManager(txconfig);
        // connection managers are installed lazily by
        // getOrCreateConnectionManager
    }

    protected static void installTransactionManager(
            TransactionManagerConfiguration config) throws NamingException {
        transactionManager = lookupTransactionManager();
        if (transactionManager == null) {
            if (config == null) {
                config = new TransactionManagerConfiguration();
            }
            initTransactionManager(config);
            addDeepBinding(rootContext, new CompositeName(
                    JNDI_TRANSACTION_MANAGER), getTransactionManagerReference());
            addDeepBinding(rootContext,
                    new CompositeName(JNDI_USER_TRANSACTION),
                    getUserTransactionReference());
        }
    }

    /**
     * Creates and installs in the container a new ConnectionManager.
     *
     * @param repositoryName the repository name
     * @param config the pool configuration
     * @return the created connection manager
     */
    public static synchronized ConnectionManagerWrapper installConnectionManager(
            String repositoryName, NuxeoConnectionManagerConfiguration config) {
        ConnectionManagerWrapper cm = connectionManagers.get(repositoryName);
        if (cm != null) {
            return cm;
        }
        if (config == null) {
            config = new NuxeoConnectionManagerConfiguration();
            config.setName(config.name + "/" + repositoryName);
        }
        cm = initConnectionManager(repositoryName, config);
        // also bind it in JNDI
        if (rootContext != null) {
            String jndiName = JNDI_NUXEO_CONNECTION_MANAGER_PREFIX
                    + repositoryName;
            try {
                addDeepBinding(rootContext, new CompositeName(jndiName),
                        getConnectionManagerReference(repositoryName));
            } catch (NamingException e) {
                log.error("Cannot bind in JNDI connection manager "
                        + config.name + " to name " + jndiName);
            }
        }
        for (NuxeoContainerListener listener:listeners) {
            listener.handleNewConnectionManager(repositoryName, cm.cm);
        }
        return cm;
    }

    public static synchronized boolean isInstalled() {
        return installContext != null;
    }

    public static synchronized InstallContext getInstallContext() {
        return installContext;
    }

    public static synchronized void uninstall() throws NamingException {
        if (installContext == null) {
            throw new RuntimeException("Nuxeo container not installed");
        }
        try {
            try {
                removeBinding(JNDI_TRANSACTION_MANAGER);
            } catch (NamingException e) {
                log.error(e, e);
            }
            try {
                removeBinding(JNDI_USER_TRANSACTION);
            } catch (NamingException e) {
                log.error(e, e);
            }
            for (String repositoryName : connectionManagers.keySet()) {
                String jndiName = JNDI_NUXEO_CONNECTION_MANAGER_PREFIX
                        + repositoryName;
                try {
                    removeBinding(jndiName);
                } catch (NamingException e) {
                    log.error(e, e);
                }
            }
        } finally {
            uninstallNaming();
            transactionManager = null;
            connectionManagers.clear();
        }
    }

    /**
     * setup root naming context and install initial naming context factory
     *
     * @since 5.6
     */
    public static synchronized void installNaming() throws NamingException {
        installContext = new InstallContext();
        log.trace("Installing nuxeo container", installContext);
        setupRootContext();
        setAsInitialContext();
    }

    /**
     * release naming context and revert settings
     *
     * @since 5.6
     */
    public static synchronized void uninstallNaming() {
        log.trace("Uninstalling nuxeo container", installContext);
        installContext = null;
        parentContext = null;
        rootContext = null;
        revertSetAsInitialContext();
    }

    /**
     *
     * @since 5.8
     */
    public static void addListener(NuxeoContainerListener listener) {
        synchronized(listeners) {
            listeners.add(listener);
        }
        for(Map.Entry<String, ConnectionManagerWrapper> entry:connectionManagers.entrySet()) {
            listener.handleNewConnectionManager(entry.getKey(), entry.getValue().cm);
        }
    }

    /**
     *
     * @since 5.8
     */
    public static void removeListener(NuxeoContainerListener listener) {
        synchronized(listeners) {
            listeners.remove(listener);
        }
    }

    /**
     * setup root context, use the system defined context if writable, use nuxeo
     * context unless
     *
     * @since 5.6
     */
    protected static void setupRootContext() throws NamingException {
        parentContext = InitialContextAccessor.getInitialContext();
        if (parentContext != null) {
            if (InitialContextAccessor.isWritable(parentContext)) {
                rootContext = parentContext;
                return;
            }
            rootContext = new NamingContextFacade(parentContext);
            log.warn("Chaining naming spaces, can break your application server");
            return;
        }
        rootContext = new NamingContext();
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
     * set naming context factory to nuxeo implementation, backup original
     * settings
     *
     * @since 5.6
     */
    protected static void setAsInitialContext() {
        // Preserve current set system props
        String key = Context.INITIAL_CONTEXT_FACTORY;
        parentEnvironment.put(key, System.getProperty(key));
        key = Context.URL_PKG_PREFIXES;
        parentEnvironment.put(key, System.getProperty(key));

        System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
                NamingContextFactory.class.getName());
        System.setProperty(Context.URL_PKG_PREFIXES, "org.nuxeo.runtime.jtajca");
    }

    /**
     * revert naming factory to original settings
     *
     * @since 5.6
     */
    protected static void revertSetAsInitialContext() {
        Iterator<Entry<String, Object>> iterator = parentEnvironment.entrySet().iterator();
        Properties props = System.getProperties();

        while (iterator.hasNext()) {
            Entry<String, Object> entry = iterator.next();
            iterator.remove();
            String key = entry.getKey();
            String value = (String) entry.getValue();
            if (value == null) {
                props.remove(key);
            } else {
                props.setProperty(key, value);
            }
        }
    }

    /**
     * Bind object in root context. Create needed sub contexts.
     *
     * since 5.6
     */
    public static void addDeepBinding(String name, Object obj)
            throws NamingException {
        addDeepBinding(rootContext, new CompositeName(name), obj);
    }

    protected static void addDeepBinding(Context dir, CompositeName comp,
            Object obj) throws NamingException {
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

    protected static void addBinding(Context dir, Name name, Object obj)
            throws NamingException {
        try {
            dir.rebind(name, obj);
        } catch (NamingException e) {
            dir.bind(name, obj);
        }
    }

    protected static void removeBinding(String name) throws NamingException {
        rootContext.unbind(name);
    }

    @SuppressWarnings("unchecked")
    protected static <T> T resolveBinding(String name) throws NamingException {
        InitialContext ctx = new InitialContext();
        try {
            return (T) ctx.lookup("java:comp/" + name);
        } catch (NamingException compe) {
            try {
                return (T) ctx.lookup("java:comp/env/" + name);
            } catch (NamingException enve) {
                return (T) ctx.lookup(name);
            }
        }
    }

    /**
     * Gets the transaction manager used by the container.
     *
     * @return the transaction manager
     */
    public static TransactionManager getTransactionManager() {
        return transactionManager;
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
    public static UserTransaction getUserTransaction() throws NamingException {
        return userTransaction;
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
    public static ConnectionManager getConnectionManager(String repositoryName) {
        return connectionManagers.get(repositoryName);
    }

    protected static void setConnectionManager(String repositoryName,
            ConnectionManagerWrapper cm) {
        if (connectionManagers.containsKey(repositoryName)) {
            log.error("Repository " + repositoryName + " already set up",
                    new Exception());
        }
        connectionManagers.put(repositoryName, cm);
    }

    protected static Reference getConnectionManagerReference(
            final String repositoryName) {
        return new SimpleReference() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getContent() throws NamingException {
                return getConnectionManager(repositoryName);
            }
        };
    }

    public static synchronized void initTransactionManager(
            TransactionManagerConfiguration config) throws NamingException {
        TransactionManager tm = createTransactionManager(config);
        transactionManager = new TransactionManagerWrapper(tm);
    }

    protected static TransactionManagerWrapper lookupTransactionManager() {
        TransactionManager tm;
        try {
            tm = resolveBinding("TransactionManager");
        } catch (NamingException e) {
            return null;
        }
        if (tm instanceof TransactionManagerWrapper) {
            return (TransactionManagerWrapper) tm;
        }
        return new TransactionManagerWrapper(tm);
    }

    public static synchronized ConnectionManagerWrapper initConnectionManager(
            String repositoryName, NuxeoConnectionManagerConfiguration config) {
        GenericConnectionManager cm = createConnectionManager(config);
        ConnectionManagerWrapper cmw = new ConnectionManagerWrapper(cm, config);
        setConnectionManager(repositoryName, cmw);
        return cmw;
    }

    // called by reflection from RepositoryReloader
    public static synchronized void resetConnectionManager() throws Exception {
        for (Map.Entry<String,ConnectionManagerWrapper> entry : connectionManagers.entrySet()) {
            String repositoryName = entry.getKey();
            ConnectionManagerWrapper mgr = entry.getValue();
            mgr.reset();
            for (NuxeoContainerListener listener:listeners) {
                listener.handleConnectionManagerReset(repositoryName, mgr.cm);
            }
        }
    }

    protected static ConnectionManagerWrapper lookupConnectionManager(
            String repositoryName) {
        ConnectionManager cm;
        try {
            String jndiName = JNDI_NUXEO_CONNECTION_MANAGER_PREFIX
                    + repositoryName;
            Object o = resolveBinding(jndiName);
            cm = (ConnectionManager) o;
        } catch (NamingException e) {
            return null;
        }
        if (cm instanceof ConnectionManagerWrapper) {
            return (ConnectionManagerWrapper) cm;
        }
        log.warn("Connection manager not a wrapper, check your configuration");
        return null;
    }

    protected static TransactionManager createTransactionManager(
            TransactionManagerConfiguration config) {
        try {
            return new TransactionManagerImpl(config.transactionTimeoutSeconds);
        } catch (Exception e) {
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

        protected boolean checked;

        protected void check() throws SystemException {
            if (transactionManager != null) {
                return;
            }
            if (!checked) {
                checked = true;
                transactionManager = lookupTransactionManager();
            }
            if (transactionManager == null) {
                throw new SystemException("No active transaction manager");
            }
        }

        @Override
        public int getStatus() throws SystemException {
            check();
            return transactionManager.getStatus();
        }

        @Override
        public void setRollbackOnly() throws IllegalStateException,
                SystemException {
            check();
            transactionManager.setRollbackOnly();
        }

        @Override
        public void setTransactionTimeout(int seconds) throws SystemException {
            check();
            transactionManager.setTransactionTimeout(seconds);
        }

        @Override
        public void begin() throws NotSupportedException, SystemException {
            check();
            transactionManager.begin();
            timers.put(transactionManager.getTransaction(),
                    transactionTimer.time());
            concurrentCount.inc();
            if (concurrentCount.getCount() > concurrentMaxCount.getCount()) {
                concurrentMaxCount.inc();
            }
        }

        @Override
        public void commit() throws HeuristicMixedException,
                HeuristicRollbackException, IllegalStateException,
                RollbackException, SecurityException, SystemException {
            check();
            Timer.Context timerContext = timers.remove(transactionManager.getTransaction());
            transactionManager.commit();
            if (timerContext != null) {
                timerContext.stop();
            }
            concurrentCount.dec();
        }

        @Override
        public void rollback() throws IllegalStateException, SecurityException,
                SystemException {
            check();
            Timer.Context timerContext = timers.remove(transactionManager.getTransaction());
            transactionManager.rollback();
            concurrentCount.dec();
            if (timerContext != null) {
                timerContext.stop();
            }
            rollbackCount.inc();
        }
    }

    /**
     * Creates a Geronimo pooled connection manager using a Geronimo transaction
     * manager.
     * <p>
     * The pool uses the transaction manager for recovery, and when using
     * XATransactions for cache + enlist/delist.
     *
     * @throws NamingException
     */
    protected static GenericConnectionManager createConnectionManager(
            NuxeoConnectionManagerConfiguration config) {
        TransactionSupport transactionSupport = new XATransactions(
                config.useTransactionCaching, config.useThreadCaching);
        // note: XATransactions -> TransactionCachingInterceptor ->
        // ConnectorTransactionContext casts transaction to Geronimo's
        // TransactionImpl (from TransactionManagerImpl)
        PoolingSupport poolingSupport = new PartitionedPool(config.maxPoolSize,
                config.minPoolSize, config.blockingTimeoutMillis,
                config.idleTimeoutMinutes, config.matchOne, config.matchAll,
                config.selectOneNoMatch,
                config.partitionByConnectionRequestInfo,
                config.partitionBySubject);

        final Subject subject = new Subject();
        SubjectSource subjectSource = new SubjectSource() {
            @Override
            public Subject getSubject() {
                return subject;
            }
        };
        ConnectionTrackingCoordinator connectionTracker = new ConnectionTrackingCoordinator();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader(); // NuxeoContainer.class.getClassLoader();

        return new GenericConnectionManager(transactionSupport, poolingSupport,
                subjectSource, connectionTracker, transactionManager,
                config.name, classLoader);
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
     *
     */
    public static class TransactionManagerWrapper implements
            RecoverableTransactionManager {

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
        public void setRollbackOnly() throws IllegalStateException,
                SystemException {
            tm.setRollbackOnly();
        }

        @Override
        public void rollback() throws IllegalStateException, SecurityException,
                SystemException {
            tm.rollback();
        }

        @Override
        public void resume(Transaction tobj) throws IllegalStateException,
                InvalidTransactionException, SystemException {
            tm.resume(tobj);
        }

        @Override
        public Transaction getTransaction() throws SystemException {
            return tm.getTransaction();
        }

        @Override
        public int getStatus() throws SystemException {
            return tm.getStatus();
        }

        @Override
        public void commit() throws HeuristicMixedException,
                HeuristicRollbackException, IllegalStateException,
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
        public void registerNamedXAResourceFactory(
                NamedXAResourceFactory factory) {
            if (RecoverableTransactionManager.class.isAssignableFrom(tm.getClass())) {
                ((RecoverableTransactionManager) tm).registerNamedXAResourceFactory(factory);
            }
        }

        @Override
        public void unregisterNamedXAResourceFactory(String factory) {
            if (RecoverableTransactionManager.class.isAssignableFrom(tm.getClass())) {
                ((RecoverableTransactionManager) tm).unregisterNamedXAResourceFactory(factory);
            }
        }

    }

    /**
     * Wraps a Geronimo ConnectionManager and adds a {@link #reset} method to
     * flush the pool.
     */
    public static class ConnectionManagerWrapper implements ConnectionManager {
        private static final long serialVersionUID = 1L;

        protected AbstractConnectionManager cm;

        protected final NuxeoConnectionManagerConfiguration config;

        public ConnectionManagerWrapper(AbstractConnectionManager cm,
                NuxeoConnectionManagerConfiguration config) {
            this.cm = cm;
            this.config = config;
        }

        @Override
        public Object allocateConnection(
                ManagedConnectionFactory managedConnectionFactory,
                ConnectionRequestInfo connectionRequestInfo)
                throws ResourceException {
            return cm.allocateConnection(managedConnectionFactory,
                    connectionRequestInfo);
        }

        public void reset() throws Exception {
            cm.doStop();
            cm = createConnectionManager(config);
        }
    }

}
