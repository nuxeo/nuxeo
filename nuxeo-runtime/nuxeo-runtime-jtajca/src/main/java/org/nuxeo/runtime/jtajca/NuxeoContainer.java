/*
 * (C) Copyright 2009-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume, jcarsique
 */

package org.nuxeo.runtime.jtajca;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.geronimo.connector.outbound.AbstractConnectionManager;
import org.apache.geronimo.connector.outbound.GenericConnectionManager;
import org.apache.geronimo.connector.outbound.SubjectSource;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.PartitionedPool;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.PoolingSupport;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.TransactionSupport;
import org.apache.geronimo.connector.outbound.connectionmanagerconfig.XATransactions;
import org.apache.geronimo.connector.outbound.connectiontracking.ConnectionTrackingCoordinator;
import org.apache.geronimo.transaction.GeronimoUserTransaction;
import org.apache.geronimo.transaction.manager.RecoverableTransactionManager;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Internal helper for the Nuxeo-defined transaction manager and connection
 * manager.
 * <p>
 * This code is called by the factories registered through JNDI, or by unit
 * tests mimicking JNDI bindings.
 */
public class NuxeoContainer {

    private static RecoverableTransactionManager transactionManager;

    private static UserTransaction userTransaction;

    private static ConnectionManagerWrapper connectionManager;

    private NuxeoContainer() {
    }

    public static TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public static UserTransaction getUserTransaction() throws NamingException {
        if (transactionManager == null) {
            initTransactionManager();
        }
        return userTransaction;
    }

    public static ConnectionManager getConnectionManager() {
        return connectionManager;
    }

    protected static void initTransactionManager() throws NamingException {
        // doing a lookup will initialize it with its configuration parameters
        TransactionHelper.lookupTransactionManager();
    }

    public static synchronized void initTransactionManager(
            TransactionManagerConfiguration config) {
        if (transactionManager == null) {
            transactionManager = createTransactionManager(config);
            userTransaction = createUserTransaction(transactionManager);
        }
    }

    public static synchronized void initConnectionManager(
            ConnectionManagerConfiguration config) throws NamingException {
        if (transactionManager == null) {
            initTransactionManager();
        }
        if (connectionManager == null) {
            AbstractConnectionManager cm = createConnectionManager(transactionManager,
                    config);
            connectionManager = new ConnectionManagerWrapper(cm, config);
        }
    }

    public static synchronized void resetConnectionManager() throws Exception {
        ConnectionManagerWrapper cm = connectionManager;
        if (cm == null) {
            return;
        }
        cm.reset();
    }

    protected static RecoverableTransactionManager createTransactionManager(
            TransactionManagerConfiguration config) {
        try {
            return new TransactionManagerImpl(config.transactionTimeoutSeconds);
        } catch (Exception e) {
            // failed in recovery somewhere
            throw new RuntimeException(e.toString(), e);
        }
    }

    protected static UserTransaction createUserTransaction(
            TransactionManager transactionManager) {
        return new GeronimoUserTransaction(transactionManager);
    }

    /**
     * Creates a Geronimo pooled connection manager using a Geronimo transaction
     * manager.
     * <p>
     * The pool uses the transaction manager for recovery, and when using
     * XATransactions for cache + enlist/delist.
     */
    protected static AbstractConnectionManager createConnectionManager(
            RecoverableTransactionManager transactionManager,
            ConnectionManagerConfiguration config) {
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
            public Subject getSubject() {
                return subject;
            }
        };
        ConnectionTrackingCoordinator connectionTracker = new ConnectionTrackingCoordinator();
        ClassLoader classLoader = NuxeoContainer.class.getClassLoader();
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
     * Wrap a geronimo cm to be able to flush the pool
     * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
     *
     */
    public static class ConnectionManagerWrapper implements ConnectionManager {
        private static final long serialVersionUID = 1L;
        protected AbstractConnectionManager cm;
        protected ConnectionManagerConfiguration config;
        public ConnectionManagerWrapper(AbstractConnectionManager cm, ConnectionManagerConfiguration config) {
            this.cm = cm;
            this.config = config;
        }
        public Object allocateConnection(ManagedConnectionFactory managedConnectionFactory,
                ConnectionRequestInfo connectionRequestInfo) throws ResourceException {
            return cm.allocateConnection(managedConnectionFactory, connectionRequestInfo);
        }
        public void reset() throws Exception {
            cm.doStop();
            cm = createConnectionManager(transactionManager, config);
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
    
    public static void initTransactionManagement() throws NamingException {
        initTransactionManager(new TransactionManagerConfiguration());
        initConnectionManager(new ConnectionManagerConfiguration());
        InitialContext initialContext = new InitialContext();
        initialContext.rebind("java:comp/TransactionManager",getTransactionManager());
        initialContext.rebind("java:comp/UserTransaction",getUserTransaction());
        initialContext.rebind("java:comp/NuxeoConnectionManager",getConnectionManager());
    }
}
