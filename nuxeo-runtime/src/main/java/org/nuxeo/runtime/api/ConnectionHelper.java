/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.runtime.api;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * This helper provides a way to get a JDBC connection, through
 * {@link #getConnection(String)}, that will return a connection wrapper able to
 * use a shared connection when used in transactional mode and
 * setAutoCommit(false) is called, and otherwise use a normal physical JDBC
 * connection.
 * <p>
 * The physical connections are created from the datasource configured using the
 * framework property {@value #SINGLE_DS}.
 * <p>
 * This helper is used to implement consistent resource management in a non-XA
 * context. Several users of the shared connection can call setAutoCommit(false)
 * then do transactional work and commit(). Only the commit() of the last user
 * will do an actual commit on the physical connection.
 *
 * @since 5.7
 */
public class ConnectionHelper {

    private static final Log log = LogFactory.getLog(ConnectionHelper.class);

    /**
     * Shared connection for each transaction.
     * <p>
     * The shared connection is always in autoCommit=false.
     * <p>
     * Things are removed from this map by a transaction synchronizer when the
     * transaction finishes.
     */
    private static ConcurrentMap<Transaction, SharedConnection> sharedConnections = new ConcurrentHashMap<Transaction, SharedConnection>();

    /**
     * SharedConnectionSynchronization registered for the transaction, when
     * sharing.
     */
    private static ConcurrentMap<Transaction, SharedConnectionSynchronization> sharedSynchronizations = new ConcurrentHashMap<Transaction, SharedConnectionSynchronization>();

    /**
     * Property holding a datasource name to use to replace all database
     * accesses.
     */
    public static final String SINGLE_DS = "nuxeo.db.singleDataSource";

    /**
     * Property holding one ore more datasource names (comma or space separated)
     * for whose connections the single datasource is not used.
     */
    public static final String EXCLUDE_DS = "nuxeo.db.singleDataSource.exclude";

    /**
     * Maximum number of time we retry a connection if the server says it's
     * overloaded.
     */
    public static final int MAX_CONNECTION_TRIES = 3;

    /**
     * Wrapper for a connection that delegates calls to either a private
     * connection, or a per-transaction shared one if a transaction is started.
     * <p>
     * Sharing is started on setAutoCommit(true), and ends on
     * setAutoCommit(false) or close().
     */
    private static class ConnectionHandle implements InvocationHandler {

        private boolean closed;

        /**
         * Expected autoCommit mode by the client for this connection.
         */
        private boolean autoCommit;

        /**
         * The transaction in use at the time where sharing was started
         * (autoCommit was set to false during a transaction).
         * <p>
         * The sharedConnection is allocated on first use after that.
         */
        private Transaction transactionForShare;

        /**
         * A local connection, allocated when the connection is used when
         * sharedInTransaction == null.
         */
        private Connection localConnection;

        /**
         * A shared connection, allocated when the connection is used when
         * sharedInTransaction != null.
         */
        private SharedConnection sharedConnection;

        /**
         * True between the first use and the commit/rollback (in non-autoCommit
         * mode and shared connection).
         */
        private boolean began;

        public ConnectionHandle() {
            autoCommit = true;
            if (log.isDebugEnabled()) {
                log.debug("Construct " + this);
                if (log.isTraceEnabled()) {
                    log.trace("Construct stacktrace " + this, new Exception(
                            "debug"));
                }
            }
        }

        private void logInvoke(String message) {
            if (log.isDebugEnabled()) {
                log.debug("Invoke " + message + " " + this);
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            String methodName = method.getName();
            if (methodName.equals("isClosed")) {
                return isClosed();
            } else if (methodName.equals("close")) {
                close();
                return null;
            }
            if (closed) {
                throw new SQLException("Connection is closed", "08003");
            }

            if (methodName.equals("getAutoCommit")) {
                return getAutoCommit();
            }

            if (methodName.equals("setAutoCommit")) {
                setAutoCommit(((Boolean) args[0]).booleanValue());
                return null;
            }

            Connection connection;
            if (transactionForShare != null) {
                // check that we're still in the same transaction
                // this also enforces single-threaded use of
                // the shared connection
                Transaction transaction = getTransaction();
                if (transaction != transactionForShare) {
                    throw new SQLException("Calling method " + methodName
                            + ", connection sharing started in transaction "
                            + transactionForShare
                            + " but it is now used in transaction "
                            + transaction);
                }

                sharedConnectionAllocate();

                // for begin/commit we don't actually need to allocate
                // the connection
                if (methodName.equals("commit")) {
                    if (autoCommit) {
                        throw new SQLException(
                                "Cannot commit outside of transaction", "25000");
                    }
                    sharedConnectionCommit();
                    return null;
                } else if (methodName.equals("rollback")) {
                    if (autoCommit) {
                        throw new SQLException(
                                "Cannot commit outside of transaction", "25000");
                    }
                    if (args != null && args.length > 0) {
                        throw new SQLException(
                                "Not implemented: rollback(Savepoint)", "0A000");
                    }
                    sharedConnectionRollback();
                    return null;
                } else if (methodName.equals("setSavepoint")
                        || methodName.equals("releaseSavepoint")) {
                    throw new SQLException("Not implemented: " + methodName,
                            "0A000");
                }

                sharedConnectionBegin(methodName);

                connection = sharedConnection.getConnection();
            } else {
                localConnectionAllocate();
                connection = localConnection;
            }

            try {
                if (log.isDebugEnabled()) {
                    if (sharedConnection == null) {
                        logInvoke(methodName);
                    } else {
                        logInvoke(methodName + " " + sharedConnection);
                    }
                }
                return method.invoke(connection, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        private Boolean getAutoCommit() {
            return Boolean.valueOf(autoCommit);
        }

        private void setAutoCommit(boolean setAutoCommit) throws SQLException {
            if (setAutoCommit == autoCommit) {
                return; // no change
            }
            autoCommit = setAutoCommit;
            if (log.isDebugEnabled()) {
                log.debug("setAutoCommit(" + autoCommit + ") " + this);
            }
            if (!autoCommit) {
                // setting autoCommit = false
                if (transactionForShare != null) {
                    throw new AssertionError(
                            "autoCommit=false when already sharing");
                }
                // not yet sharing
                Transaction transaction = getTransaction();
                if (transaction != null
                        && transactionStatus(transaction) == Status.STATUS_ACTIVE) {
                    // start sharing
                    transactionForShare = transaction;
                    if (localConnection != null) {
                        // share using the previous local connection
                        logInvoke("setAutoCommit false");
                        localConnection.setAutoCommit(false);
                        log.debug("Upgrading local connection to shared");
                        sharedConnection = getSharedConnection(localConnection);
                        localConnection = null;
                    } else {
                        // sharedConnection allocated on first use
                    }
                } else {
                    log.debug("No usable transaction");
                    // we're outside a usable transaction
                    // use the local connection
                    if (localConnection != null) {
                        logInvoke("setAutoCommit false");
                        localConnection.setAutoCommit(false);
                    } else {
                        // localConnection allocated on first use
                    }
                }
            } else {
                // setting autoCommit = true
                if (transactionForShare != null) {
                    if (began) {
                        // do automatic commit
                        log.debug("setAutoCommit true committing shared");
                        sharedConnectionCommit();
                    }
                    // stop sharing
                    sharedConnection = null;
                    transactionForShare = null;
                } else if (localConnection != null) {
                    logInvoke("setAutoCommit true");
                    localConnection.setAutoCommit(true);
                }
            }
        }

        // allocation on first use
        private void localConnectionAllocate() throws SQLException {
            if (localConnection == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Constructing physical connection " + this);
                    if (log.isTraceEnabled()) {
                        log.trace(
                                "Constructing physical connection stacktrace",
                                new Exception("debug"));
                    }
                }
                localConnection = getPhysicalConnection();
                logInvoke("setAutoCommit " + autoCommit);
                localConnection.setAutoCommit(autoCommit);
            }
        }

        // allocation on first use
        private void sharedConnectionAllocate() throws SQLException {
            if (sharedConnection == null) {
                if (transactionStatus(transactionForShare) == Status.STATUS_ACTIVE) {
                    sharedConnection = getSharedConnection(null);
                    // autoCommit mode set by SharedConnection.allocate()
                } else {
                    // already committing or rolling back
                    // do not assign a connection at all
                    // only commit or rollback is allowed,
                    // and they will do nothing (began=false)
                }
            }
        }

        private void sharedConnectionBegin(String methodName)
                throws SQLException {
            if (sharedConnection == null) {
                throw new SQLException("Cannot call " + methodName
                        + " with transaction in state "
                        + transactionStatus(transactionForShare), "25000");
            }
            if (!autoCommit && !began) {
                sharedConnection.begin(this);
                began = true;
            }
        }

        private void sharedConnectionCommit() throws SQLException {
            if (began) {
                if (log.isDebugEnabled()) {
                    log.debug("Committing shared " + this);
                }
                sharedConnection.commit(this);
                began = false;
            }
        }

        private void sharedConnectionRollback() throws SQLException {
            if (began) {
                sharedConnection.rollback(this);
                began = false;
            }
        }

        /** Called back from SharedConnection close. */
        protected void closeFromSharedConnection() {
            sharedConnection = null;
            transactionForShare = null;
        }

        private Boolean isClosed() {
            return Boolean.valueOf(closed);
        }

        private void close() throws SQLException {
            if (!closed) {
                if (log.isDebugEnabled()) {
                    log.debug("close() " + this);
                }
                if (transactionForShare != null) {
                    if (sharedConnection != null) {
                        if (began) {
                            // connection closed before commit/rollback
                            // commit it by hand (even though it's unspecified)
                            log.debug("close committing shared");
                            sharedConnectionCommit();
                        }
                        sharedConnection = null;
                    }
                    transactionForShare = null;
                } else {
                    if (localConnection != null) {
                        logInvoke("close");
                        localConnection.close();
                        localConnection = null;
                    }
                }
                closed = true;
            }
        }

        /** Gets the physical connection, use by unwrap. */
        public Connection getUnwrappedConnection() throws SQLException {
            Connection connection;
            if (sharedConnection != null) {
                connection = sharedConnection.getConnection();
            } else {
                connection = localConnection;
            }
            if (connection == null) {
                throw new SQLException("Connection not allocated");
            }
            return connection;
        }

        /**
         * Gets the shared connection for the shared transaction, or allocates a
         * new one. If allocating a new one, registers a synchronizer in order
         * to remove it at transaction completion time.
         *
         * @param connection an existing local connection to reuse, or null
         */
        private SharedConnection getSharedConnection(Connection connection)
                throws SQLException {
            SharedConnection sharedConnection = sharedConnections.get(transactionForShare);
            if (sharedConnection == null) {
                // allocate a new shared connection
                sharedConnection = new SharedConnection(connection);
                if (log.isDebugEnabled()) {
                    log.debug("Allocating new shared connection "
                            + sharedConnection + " for " + this);
                }
                if (sharedConnections.putIfAbsent(transactionForShare,
                        sharedConnection) != null) {
                    // race condition but we are single-threaded in this
                    // transaction!
                    throw new AssertionError(
                            "Race condition in single transaction!");
                }
                // register a synchronizer to clear the map
                SharedConnectionSynchronization.getInstance(transactionForShare);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Reusing shared connection " + sharedConnection
                            + " for " + this);
                }
                if (connection != null) {
                    // the local connection passed is not needed anymore
                    log.debug("Dropping previous local connection");
                    logInvoke("close");
                    connection.close();
                }
            }
            return sharedConnection;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "@"
                    + Integer.toHexString(System.identityHashCode(this));
        }
    }

    /**
     * Shared connection, holding a physical connection use by several pieces of
     * code in the same transaction (so not multi-threaded). It's always in mode
     * autoCommit=false.
     * <p>
     * The last user to commit/rollback will do an actual commit/rollback on the
     * physical connection.
     * <p>
     * If a rollback is done but not by the last user, the connection will be
     * marked rollback only.
     */
    private static class SharedConnection {

        /** The JDBC connection. */
        private Connection connection;

        /** The connection handles associated to this shared connection. */
        private final List<ConnectionHandle> handles;

        /** Whether the final commit must actually do a rollback. */
        private boolean mustRollback;

        public SharedConnection(Connection connection) {
            this.connection = connection;
            handles = new ArrayList<ConnectionHandle>(3);
        }

        private void logInvoke(String message) {
            if (log.isDebugEnabled()) {
                log.debug("Invoke shared " + message + " " + this);
            }
        }

        public Connection getConnection() {
            return connection;
        }

        /** Called just before first use. */
        public void begin(ConnectionHandle handle) throws SQLException {
            ref(handle);
        }

        /** Finishes connection use by commit. */
        public void commit(ConnectionHandle handle) throws SQLException {
            try {
                if (handles.size() == 1) {
                    if (mustRollback) {
                        logInvoke("rollback");
                        connection.rollback();
                        mustRollback = false;
                    } else {
                        logInvoke("commit");
                        connection.commit();
                    }
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("commit not yet closing " + this);
                    }
                }
            } finally {
                unref(handle);
            }
        }

        /** Finishes connection use by rollback. */
        public void rollback(ConnectionHandle handle) throws SQLException {
            try {
                if (handles.size() == 1) {
                    logInvoke("rollback");
                    connection.rollback();
                    mustRollback = false;
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("setting rollback only " + this);
                    }
                    mustRollback = true;
                }
            } finally {
                unref(handle);
            }
        }

        private void ref(ConnectionHandle handle) throws SQLException {
            if (handles.isEmpty()) {
                if (connection == null) {
                    allocate();
                }
            }
            handles.add(handle);
            if (log.isDebugEnabled()) {
                log.debug("Reference added for " + this);
            }
        }

        private void unref(ConnectionHandle handle) throws SQLException {
            handles.remove(handle);
            if (log.isDebugEnabled()) {
                log.debug("Reference removed for " + this);
            }
            if (handles.isEmpty()) {
                deallocate();
            }
        }

        // Note that this is not called when a local connection was upgraded to
        // a shared one, and is reused.
        private void allocate() throws SQLException {
            if (log.isDebugEnabled()) {
                log.debug("Constructing physical connection " + this);
                if (log.isTraceEnabled()) {
                    log.trace("Constructing physical connection stacktrace",
                            new Exception("debug"));
                }
            }
            connection = getPhysicalConnection();
            logInvoke("setAutoCommit false");
            connection.setAutoCommit(false);
        }

        private void deallocate() throws SQLException {
            if (log.isDebugEnabled()) {
                log.debug("Closing physical connection " + this);
                if (log.isTraceEnabled()) {
                    log.trace("Closing physical connection stacktrace",
                            new Exception("debug"));
                }
            }
            close();
        }

        /** Called after transaction completion to free resources. */
        public void closeAfterTransaction() {
            if (!handles.isEmpty()) {
                log.error("Transaction ended with " + handles.size()
                        + " connections not committed " + this + " " + handles);
            }
            if (connection != null) {
                close();
            }
        }

        /** Closes and dereferences from all handles to this. */
        private void close() {
            try {
                logInvoke("close");
                connection.close();
            } catch (SQLException e) {
                log.error(
                        "Could not close leftover connection at transaction end",
                        e);
            } finally {
                connection = null;
                for (ConnectionHandle h : handles) {
                    h.closeFromSharedConnection();
                }
                handles.clear();
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "@"
                    + Integer.toHexString(System.identityHashCode(this));
        }
    }

    /**
     * In addition to closing the shared connection, also acts as a delegate for
     * other synchronizers that must run before it.
     */
    private static class SharedConnectionSynchronization implements
            Synchronization {

        private final Transaction transaction;

        private final List<Synchronization> syncsFirst;

        private final List<Synchronization> syncsLast;

        /**
         * Gets the instance or creates it. If creating, registers with the
         * actual transaction.
         */
        // not synchronized as the transaction is already thread-local
        // and we use a ConcurrentHashMap
        public static SharedConnectionSynchronization getInstance(
                Transaction transaction) {
            SharedConnectionSynchronization scs = sharedSynchronizations.get(transaction);
            if (scs == null) {
                scs = new SharedConnectionSynchronization(transaction);
                try {
                    transaction.registerSynchronization(scs);
                } catch (IllegalStateException | RollbackException
                        | SystemException e) {
                    throw new RuntimeException(
                            "Cannot register synchronization", e);
                }
                sharedSynchronizations.put(transaction, scs);
            }
            return scs;
        }

        public SharedConnectionSynchronization(Transaction transaction) {
            this.transaction = transaction;
            syncsFirst = new ArrayList<Synchronization>(5);
            syncsLast = new ArrayList<Synchronization>(5);
        }

        /**
         * Registers a synchronization that must run before or after us.
         */
        public void registerSynchronization(Synchronization sync, boolean first) {
            if (first) {
                syncsFirst.add(sync);
            } else {
                syncsLast.add(sync);
            }
        }

        @Override
        public void beforeCompletion() {
            beforeCompletion(syncsFirst);
            beforeCompletion(syncsLast);
        }

        private void beforeCompletion(List<Synchronization> syncs) {
            // beforeCompletion hooks may add other syncs,
            // so we must be careful when iterating on the list
            RuntimeException exc = null;
            for (int i = 0; i < syncs.size(); i++) {
                try {
                    syncs.get(i).beforeCompletion();
                } catch (RuntimeException e) {
                    log.error("Exception during beforeCompletion hook", e);
                    if (exc == null) {
                        exc = e;
                        try {
                            transaction.setRollbackOnly();
                        } catch (SystemException se) {
                            log.error("Cannot set rollback only", e);
                        }
                    }
                }
            }
            if (exc != null) {
                throw exc;
            }
        }

        /**
         * After completion, removes the shared connection from the map and
         * closes it.
         */
        @Override
        public void afterCompletion(int status) {
            sharedSynchronizations.remove(transaction);
            afterCompletion(syncsFirst, status);
            closeSharedAfterCompletion();
            afterCompletion(syncsLast, status);
        }

        private void closeSharedAfterCompletion() {
            SharedConnection sharedConnection = sharedConnections.remove(transaction);
            if (sharedConnection != null) {
                sharedConnection.closeAfterTransaction();
            }
        }

        private void afterCompletion(List<Synchronization> syncs, int status) {
            for (Synchronization sync : syncs) {
                try {
                    sync.afterCompletion(status);
                } catch (RuntimeException e) {
                    log.warn(
                            "Unexpected exception from afterCompletion; continuing",
                            e);
                }
            }
        }
    }

    private static Transaction getTransaction() {
        try {
            return TransactionHelper.lookupTransactionManager().getTransaction();
        } catch (NamingException | SystemException e) {
            return null;
        }
    }

    private static int transactionStatus(Transaction transaction) {
        try {
            return transaction.getStatus();
        } catch (SystemException e) {
            log.error("Cannot get transaction status", e);
            return Status.STATUS_UNKNOWN;
        }
    }

    /**
     * Tries to unwrap the connection to get the real physical one (returned by
     * the original datasource).
     * <p>
     * This should only be used by code that needs to cast the connection to a
     * driver-specific class to use driver-specific features.
     *
     * @throws SQLException if no actual physical connection was allocated yet
     */
    public static Connection unwrap(Connection connection) throws SQLException {
        if (Proxy.isProxyClass(connection.getClass())) {
            InvocationHandler handler = Proxy.getInvocationHandler(connection);
            if (handler instanceof ConnectionHandle) {
                ConnectionHandle h = (ConnectionHandle) handler;
                connection = h.getUnwrappedConnection();
            }
        }
        // now try Apache DBCP unwrap (standard or Tomcat), to skip datasource
        // wrapping layers
        // this needs accessToUnderlyingConnectionAllowed=true in the pool
        // config
        try {
            Method m = connection.getClass().getMethod("getInnermostDelegate");
            m.setAccessible(true); // needed, method of inner private class
            Connection delegate = (Connection) m.invoke(connection);
            if (delegate == null) {
                log.error("Cannot access underlying connection, you must use "
                        + "accessToUnderlyingConnectionAllowed=true in the pool configuration");
            } else {
                connection = delegate;
            }
        } catch (NoSuchMethodException | SecurityException
                | IllegalAccessException | InvocationTargetException e) {
            // ignore missing method, connection not coming from Apache pool
        }
        return connection;
    }

    /**
     * Checks if single transaction-local datasource mode will be used for the
     * given datasource name.
     *
     * @return {@code true} if using a single transaction-local connection for
     *         this datasource
     */
    public static boolean useSingleConnection(String dataSourceName) {
        if (dataSourceName != null) {
            String excludes = Framework.getProperty(EXCLUDE_DS);
            if ("*".equals(excludes)) {
                return false;
            }
            if (!StringUtils.isBlank(excludes)) {
                for (String exclude : excludes.split("[, ] *")) {
                    if (dataSourceName.equals(exclude)
                            || dataSourceName.equals(DataSourceHelper.getDataSourceJNDIName(exclude))) {
                        return false;
                    }
                }
            }
        }
        return !StringUtils.isBlank(Framework.getProperty(SINGLE_DS));
    }

    /**
     * Gets the fake name we use to pass to ConnectionHelper.getConnection, in
     * order for exclusions on these connections to be possible.
     */
    public static String getPseudoDataSourceNameForRepository(
            String repositoryName) {
        return "repository_" + repositoryName;
    }

    /**
     * Gets a new reference to the transaction-local JDBC connection for the
     * given dataSource. The connection <strong>MUST</strong> be closed in a
     * finally block when code is done using it.
     * <p>
     * If the passed dataSource name is in the exclusion list, null will be
     * returned.
     *
     * @param dataSourceName the datasource for which the connection is
     *            requested
     * @return a new reference to the connection, or {@code null} if single
     *         datasource connection sharing is not in effect
     */
    public static Connection getConnection(String dataSourceName)
            throws SQLException {
        return getConnection(dataSourceName, false);
    }

    /**
     * Gets a new reference to the transaction-local JDBC connection for the
     * given dataSource. The connection <strong>MUST</strong> be closed in a
     * finally block when code is done using it.
     * <p>
     * If the passed dataSource name is in the exclusion list, null will be
     * returned.
     * <p>
     * If noSharing is requested, the connection will never come from the
     * transaction-local and will always be newly allocated.
     *
     * @param dataSourceName the datasource for which the connection is
     *            requested
     * @param noSharing {@code true} if this connection must not be shared with
     *            others
     * @return a new reference to the connection, or {@code null} if single
     *         datasource connection sharing is not in effect
     */
    public static Connection getConnection(String dataSourceName,
            boolean noSharing) throws SQLException {
        if (!useSingleConnection(dataSourceName)) {
            return null;
        }
        return getConnection(noSharing);
    }

    private static Connection getConnection(boolean noSharing)
            throws SQLException {
        String dataSourceName = Framework.getProperty(SINGLE_DS);
        if (StringUtils.isBlank(dataSourceName)) {
            return null;
        }
        if (noSharing) {
            return getPhysicalConnection(dataSourceName);
        }
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[] { Connection.class }, new ConnectionHandle());
    }

    private static Connection getPhysicalConnection() throws SQLException {
        return getPhysicalConnection(Framework.getProperty(SINGLE_DS));
    }

    /**
     * Gets a physical connection from a datasource name.
     * <p>
     * A few retries are done to work around databases that have problems with
     * many open/close in a row.
     *
     * @param dataSourceName the datasource name
     * @return the connection
     */
    private static Connection getPhysicalConnection(String dataSourceName)
            throws SQLException {
        DataSource dataSource = getDataSource(dataSourceName);
        for (int tryNo = 0;; tryNo++) {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                if (tryNo >= MAX_CONNECTION_TRIES) {
                    throw e;
                }
                if (e.getErrorCode() != 12519) {
                    throw e;
                }
                // Oracle: Listener refused the connection with the
                // following error: ORA-12519, TNS:no appropriate
                // service handler found SQLState = "66000"
                // Happens when connections are open too fast (unit tests)
                // -> retry a few times after a small delay
                if (log.isDebugEnabled()) {
                    log.debug(String.format(
                            "Connections open too fast, retrying in %ds: %s",
                            Integer.valueOf(tryNo),
                            e.getMessage().replace("\n", " ")));
                }
                try {
                    Thread.sleep(1000 * tryNo);
                } catch (InterruptedException ie) {
                    // restore interrupted status
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Gets a datasource from a datasource name, or in test mode use test
     * connection parameters.
     *
     * @param dataSourceName the datasource name
     * @return the datasource
     */
    private static DataSource getDataSource(String dataSourceName)
            throws SQLException {
        if (Framework.isTestModeSet()) {
            String url = Framework.getProperty("nuxeo.test.vcs.url");
            String user = Framework.getProperty("nuxeo.test.vcs.user");
            String password = Framework.getProperty("nuxeo.test.vcs.password");
            if (url != null && user != null) {
                return new DataSourceFromUrl(url, user, password); // driver?
            }
        }
        try {
            return DataSourceHelper.getDataSource(dataSourceName);
        } catch (NamingException e) {
            throw new SQLException("Cannot find datasource: " + dataSourceName,
                    e);
        }
    }

    /**
     * Checks how many references there are to shared connections.
     * <p>
     * USED IN UNIT TESTS OR FOR DEBUGGING.
     */
    public static int countConnectionReferences() {
        return sharedConnections.size();
    }

    /**
     * Clears the remaining connection references for the current thread.
     * <p>
     * USED IN UNIT TESTS ONLY.
     */
    public static void clearConnectionReferences() {
        for (SharedConnection sharedConnection : sharedConnections.values()) {
            sharedConnection.closeAfterTransaction();
        }
        sharedConnections.clear();
    }

    /**
     * If sharing is in effect, registers a synchronization with the current
     * transaction, making sure it runs before the
     * {@link SharedConnectionSynchronization}.
     *
     * @return {@code true}
     */
    public static boolean registerSynchronization(Synchronization sync)
            throws SystemException {
        return registerSynchronization(sync, true);
    }

    /**
     * If sharing is in effect, registers a synchronization with the current
     * transaction, making sure the {@link Synchronization#afterCompletion}
     * method runs after the {@link SharedConnectionSynchronization}.
     *
     * @return {@code true}
     */
    public static boolean registerSynchronizationLast(Synchronization sync)
            throws SystemException {
        return registerSynchronization(sync, false);
    }

    private static boolean registerSynchronization(Synchronization sync,
            boolean first) throws SystemException {
        Transaction transaction = getTransaction();
        if (transaction == null) {
            throw new SystemException(
                    "Cannot register synchronization: no transaction");
        }
        // We always do the lookup and registration to the actual transaction
        // even if there is no shared connection yet.
        SharedConnectionSynchronization scs = SharedConnectionSynchronization.getInstance(transaction);
        scs.registerSynchronization(sync, first);
        return true;
    }

}
