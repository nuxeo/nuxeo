/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume, jcarsique
 */

package org.nuxeo.runtime.jtajca;

import java.util.Hashtable;

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
import org.apache.geronimo.transaction.GeronimoUserTransaction;
import org.apache.geronimo.transaction.manager.NamedXAResource;
import org.apache.geronimo.transaction.manager.RecoverableTransactionManager;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.nuxeo.common.jndi.InitContextAccessor;
import org.nuxeo.common.jndi.NamingContextFactory;
import org.nuxeo.runtime.transaction.TransactionHelper;

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

    public static final String JNDI_NUXEO_CONNECTION_MANAGER = "java:comp/NuxeoConnectionManager";

    private static TransactionManagerWrapper transactionManager;

    private static UserTransaction userTransaction;

    private static ConnectionManagerWrapper connectionManager;

    private static boolean isInstalled;
    
    private static boolean isNamingOwner;
    
    private static Context namingContext;
    
    private NuxeoContainer() {
    }

    /**
     * Install transaction and connection management "by hand" if the container
     * didn't do it using file-based configuration. Binds the names in JNDI.
     */
    public static void install() throws NamingException {
        install(new TransactionManagerConfiguration(),
                new ConnectionManagerConfiguration());
    }

    /**
     * Install transaction and connection management "by hand" if the container
     * didn't do it using file-based configuration. Binds the names in JNDI.
     * 
     * @param txconfig the transaction manager configuration
     * @param cmconfig the connection manager configuration
     * 
     * @since 5.4.2
     */
    public static synchronized void install(
            TransactionManagerConfiguration txconfig,
            ConnectionManagerConfiguration cmconfig) throws NamingException {
        isInstalled = true;
        transactionManager = lookupTransactionManager();
        if (transactionManager == null) {
            initTransactionManager(txconfig);
            bind(JNDI_TRANSACTION_MANAGER, getTransactionManagerReference());
            bind(JNDI_USER_TRANSACTION, getUserTransactionReference());
        }
        connectionManager = lookupConnectionManager();
        if (connectionManager == null) {
            initConnectionManager(cmconfig);
            bind(JNDI_NUXEO_CONNECTION_MANAGER, getConnectionManagerReference());
        }
    }

    public static synchronized boolean isInstalled() {
        return isInstalled;
    }

    public static synchronized void uninstall() throws NamingException {
        if (!isInstalled) {
            throw new Error("Nuxeo container not installed");
        }
        try {
            unbind(JNDI_TRANSACTION_MANAGER);
            unbind(JNDI_USER_TRANSACTION);
            unbind(JNDI_NUXEO_CONNECTION_MANAGER);
        } catch (Exception e) {
            // do nothing
        } finally {
            transactionManager = null;
            userTransaction = null;
            connectionManager = null;
        }
        uninstallNaming();
    }

    
    private static void installNaming() throws NamingException {
        Context ctx = InitContextAccessor.getInitCtx();
        if (ctx != null) {
            NamingContextFactory.setDelegateContext(ctx);
            NamingContextFactory.setDelegateEnvironment(ctx.getEnvironment());
            log.warn("Chaining naming spaces, can break your application server");
        }
        NamingContextFactory.install();
        isNamingOwner = true;
        namingContext = new InitialContext();
    }

    private static void uninstallNaming() throws NamingException {
        namingContext = null;
        if (isNamingOwner == false) {
            return;
        }
        NamingContextFactory.revertSetAsInitial();
        isNamingOwner = false;
    }

    protected static void bind(String name, Reference ref)
            throws NamingException {
        if (namingContext == null) {
            namingContext = new InitialContext();
        }
        try {
            namingContext.rebind(name, ref);
        } catch (NamingException error) {
            installNaming();
            namingContext.bind(name, ref);
        }
    }

    protected static void unbind(String name) throws NamingException {
          namingContext.unbind(name);
    }
    
    @SuppressWarnings("unchecked")
    protected static <T> T jndiLookup(String name) throws NamingException {
        InitialContext ctx = new InitialContext();
        return (T) ctx.lookup(name);
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
        return new Reference(TransactionManager.class.getName(),
                NuxeoTransactionManagerFactory.class.getName(), null);
    }

    /**
     * Gets the user transaction used by the container.
     * 
     * @return the user transaction
     */
    public static UserTransaction getUserTransaction() throws NamingException {
        if (transactionManager == null) {
            initTransactionManager(new TransactionManagerConfiguration());
        }
        return userTransaction;
    }

    protected static Reference getUserTransactionReference() {
        return new Reference(UserTransaction.class.getName(),
                NuxeoUserTransactionFactory.class.getName(), null);
    }

    /**
     * Gets the Nuxeo connection manager used by the container.
     * 
     * @return the connection manager
     */
    public static ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    protected static Reference getConnectionManagerReference() {
        return new Reference(ConnectionManager.class.getName(),
                NuxeoConnectionManagerFactory.class.getName(), null);
    }

    public static synchronized void initTransactionManager(
            TransactionManagerConfiguration config) throws NamingException {
        TransactionManager tm = createTransactionManager(config);
        transactionManager = new TransactionManagerWrapper(tm);
        userTransaction = createUserTransaction();
    }

    protected static TransactionManagerWrapper lookupTransactionManager()
            throws NamingException {
        TransactionManager tm;
        try {
            tm = TransactionHelper.lookupTransactionManager();
        } catch (NamingException e) {
            return null;
        }
        if (tm instanceof TransactionManagerWrapper) {
            return (TransactionManagerWrapper) tm;
        }
        return new TransactionManagerWrapper(tm);
    }

    public static synchronized void initConnectionManager(
            ConnectionManagerConfiguration config) throws NamingException {
        GenericConnectionManager cm = createConnectionManager(config);
        connectionManager = new ConnectionManagerWrapper(cm, config);
    }
    
   public static synchronized void resetConnectionManager() throws Exception {
        ConnectionManagerWrapper cm = connectionManager;
        if (cm == null) {
            return;
        }
        cm.reset();
    }

    protected static ConnectionManagerWrapper lookupConnectionManager() {
        ConnectionManager cm;
        try {
            cm = jndiLookup(JNDI_NUXEO_CONNECTION_MANAGER);
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

    protected static UserTransaction createUserTransaction() {
        return new GeronimoUserTransaction(transactionManager);
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
            ConnectionManagerConfiguration config) throws NamingException {
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
                new RuntimeException(e);
            }
        }

        @Override
        public void recoveryError(Exception e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void recoverResourceManager(NamedXAResource xaResource) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Wraps a Geronimo ConnectionManager and adds a {@link #reset} method to
     * flush the pool.
     */
    public static class ConnectionManagerWrapper implements ConnectionManager {
        private static final long serialVersionUID = 1L;

        protected AbstractConnectionManager cm;

        protected final ConnectionManagerConfiguration config;

        public ConnectionManagerWrapper(AbstractConnectionManager cm,
                ConnectionManagerConfiguration config) {
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

    public static class ConnectionManagerConfiguration {

        public String name = "NuxeoConnectionManager";

        // transaction

        public boolean useTransactionCaching = true;

        public boolean useThreadCaching = true;

        // pool

        public boolean matchOne = true; // unused by Geronimo?

        public boolean matchAll = true;

        public boolean selectOneNoMatch = false;

        public boolean partitionByConnectionRequestInfo = false;

        public boolean partitionBySubject = true;

        public int maxPoolSize = 20;

        public int minPoolSize = 0;

        public int blockingTimeoutMillis = 100;

        public int idleTimeoutMinutes = 0; // no timeout

        public void setName(String name) {
            this.name = name;
        }

        public void setUseTransactionCaching(boolean useTransactionCaching) {
            this.useTransactionCaching = useTransactionCaching;
        }

        public void setUseThreadCaching(boolean useThreadCaching) {
            this.useThreadCaching = useThreadCaching;
        }

        public void setMatchOne(boolean matchOne) {
            this.matchOne = matchOne;
        }

        public void setMatchAll(boolean matchAll) {
            this.matchAll = matchAll;
        }

        public void setSelectOneNoMatch(boolean selectOneNoMatch) {
            this.selectOneNoMatch = selectOneNoMatch;
        }

        public void setPartitionByConnectionRequestInfo(
                boolean partitionByConnectionRequestInfo) {
            this.partitionByConnectionRequestInfo = partitionByConnectionRequestInfo;
        }

        public void setPartitionBySubject(boolean partitionBySubject) {
            this.partitionBySubject = partitionBySubject;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public void setMinPoolSize(int minPoolSize) {
            this.minPoolSize = minPoolSize;
        }

        public void setBlockingTimeoutMillis(int blockingTimeoutMillis) {
            this.blockingTimeoutMillis = blockingTimeoutMillis;
        }

        public void setIdleTimeoutMinutes(int idleTimeoutMinutes) {
            this.idleTimeoutMinutes = idleTimeoutMinutes;
        }

    }

}
