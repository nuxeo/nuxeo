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
    private static class ConnectionDispatcher implements InvocationHandler {

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
        private Transaction sharedInTransaction;

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

        public ConnectionDispatcher() {
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
            if (sharedInTransaction != null) {
                // check that we're still in the same transaction
                // this also enforces single-threaded use of
                // the shared connection
                Transaction transaction = getTransaction();
                if (transaction != sharedInTransaction) {
                    throw new SQLException("Calling method " + methodName
                            + ", connection sharing started in transaction "
                            + sharedInTransaction
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
                if (sharedInTransaction != null) {
                    throw new AssertionError(
                            "autoCommit=false when already sharing");
                }
                // not yet sharing
                Transaction transaction = getTransaction();
                if (transaction != null
                        && transactionStatus(transaction) == Status.STATUS_ACTIVE) {
                    // start sharing
                    sharedInTransaction = transaction;
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
                if (sharedInTransaction != null) {
                    if (began) {
                        // do automatic commit
                        log.debug("setAutoCommit true committing shared");
                        sharedConnectionCommit();
                    }
                    // stop sharing
                    sharedConnection = null;
                    sharedInTransaction = null;
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
                if (transactionStatus(sharedInTransaction) == Status.STATUS_ACTIVE) {
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
                        + transactionStatus(sharedInTransaction), "25000");
            }
            if (!autoCommit && !began) {
                sharedConnection.begin();
                began = true;
            }
        }

        private void sharedConnectionCommit() throws SQLException {
            if (began) {
                if (log.isDebugEnabled()) {
                    log.debug("Committing shared " + this);
                }
                sharedConnection.commit();
                began = false;
            }
        }

        private void sharedConnectionRollback() throws SQLException {
            if (began) {
                sharedConnection.rollback();
                began = false;
            }
        }

        private Boolean isClosed() {
            return Boolean.valueOf(closed);
        }

        private void close() throws SQLException {
            if (!closed) {
                if (log.isDebugEnabled()) {
                    log.debug("close() " + this);
                }
                if (sharedInTransaction != null) {
                    if (sharedConnection != null) {
                        if (began) {
                            // connection closed before commit/rollback
                            // commit it by hand (even though it's unspecified)
                            log.debug("close committing shared");
                            sharedConnectionCommit();
                        }
                        sharedConnection = null;
                    }
                    sharedInTransaction = null;
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

        private static Transaction getTransaction() {
            try {
                return TransactionHelper.lookupTransactionManager().getTransaction();
            } catch (NamingException | SystemException e) {
                return null;
            }
        }

        private int transactionStatus(Transaction transaction) {
            try {
                return transaction.getStatus();
            } catch (SystemException e) {
                log.error("Cannot get transaction status", e);
                return Status.STATUS_UNKNOWN;
            }
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
            SharedConnection sharedConnection = sharedConnections.get(sharedInTransaction);
            if (sharedConnection == null) {
                // allocate a new shared connection
                sharedConnection = new SharedConnection(connection);
                if (log.isDebugEnabled()) {
                    log.debug("Allocating new shared connection "
                            + sharedConnection + " for " + this);
                }
                if (sharedConnections.putIfAbsent(sharedInTransaction,
                        sharedConnection) != null) {
                    // race condition but we are single-threaded in this
                    // transaction!
                    throw new AssertionError(
                            "Race condition in single transaction!");
                }
                // register a synchronizer to clear the map
                try {
                    sharedInTransaction.registerSynchronization(new SharedConnectionCloser(
                            sharedInTransaction));
                } catch (IllegalStateException | RollbackException
                        | SystemException e) {
                    throw new RuntimeException(
                            "Cannot register synchronization", e);
                }
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

        /** Whether the final commit must actually do a rollback. */
        private boolean mustRollback;

        /** The number of references to the JDBC connection. */
        private int ref;

        public SharedConnection(Connection connection) {
            this.connection = connection;
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
        public void begin() throws SQLException {
            ref();
        }

        /** Finishes connection use by commit. */
        public void commit() throws SQLException {
            try {
                if (ref == 1) {
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
                unref();
            }
        }

        /** Finishes connection use by rollback. */
        public void rollback() throws SQLException {
            try {
                if (ref == 1) {
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
                unref();
            }
        }

        private void ref() throws SQLException {
            if (ref == 0) {
                if (connection == null) {
                    allocate();
                }
            }
            ref++;
            if (log.isDebugEnabled()) {
                log.debug("Reference added (" + ref + ") for " + this);
            }
        }

        private void unref() throws SQLException {
            ref--;
            if (log.isDebugEnabled()) {
                log.debug("Reference removed (" + ref + ") for " + this);
            }
            if (ref == 0) {
                deallocate();
            }
        }

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
            logInvoke("close");
            connection.close();
            connection = null;
        }

        /** Called when removing from per-transaction map. */
        public void close() {
            if (connection != null) {
                log.error("Transaction ended with " + ref
                        + " connections not committed " + this);
                try {
                    connection.close();
                } catch (SQLException e) {
                    log.error(
                            "Could not close stray connection at transaction end",
                            e);
                } finally {
                    connection = null;
                }
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "@"
                    + Integer.toHexString(System.identityHashCode(this));
        }
    }

    private static class SharedConnectionCloser implements Synchronization {

        private final Transaction transaction;

        public SharedConnectionCloser(Transaction transaction) {
            this.transaction = transaction;
        }

        @Override
        public void beforeCompletion() {
        }

        /**
         * After completion, remove the shared connection from the map.
         * <p>
         * When this is called, a commit or rollback was already done on each
         * connection, so they already back in local mode and nobody uses the
         * shared connection anymore.
         */
        @Override
        public void afterCompletion(int status) {
            SharedConnection sharedConnection = sharedConnections.remove(transaction);
            if (sharedConnection != null) {
                sharedConnection.close();
            }
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
            if (handler instanceof ConnectionDispatcher) {
                ConnectionDispatcher h = (ConnectionDispatcher) handler;
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
                new Class[] { Connection.class }, new ConnectionDispatcher());
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
            sharedConnection.close();
        }
        sharedConnections.clear();
    }

}
