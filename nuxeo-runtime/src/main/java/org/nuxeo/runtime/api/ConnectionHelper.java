/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
import java.util.Date;
import java.util.List;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This helper can be used to get a thread-local reference to a single
 * connection, whatever the number of calls to getConnection. This is used to
 * implement consistent resource management in a non-XA context.
 *
 * @since 5.7
 */
public class ConnectionHelper {

    protected static final Log log = LogFactory.getLog(ConnectionHelper.class);

    protected static ThreadLocal<ConnectionInfo> threadConnectionInfo = new ThreadLocal<ConnectionInfo>();

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
     * Information about reference counting for an underlying JDBC connection.
     *
     * @since 5.7
     */
    protected static class ConnectionInfo {

        /** The JDBC connection. */
        public final Connection connection;

        /** The number of references to the JDBC connection. */
        protected int ref;

        private static final int LEAK_REF_MIN = 20;

        private static final int LEAK_REF_MAX = LEAK_REF_MIN + 5;

        protected List<Exception> stacktraces;

        public static ConnectionInfo getCurrent() {
            return threadConnectionInfo.get();
        }

        public ConnectionInfo(Connection connection) {
            this.connection = connection;
            threadConnectionInfo.set(this);
            if (log.isDebugEnabled()) {
                log.debug("Opening single connection to: "
                        + Framework.getProperty(SINGLE_DS));
            }
            if (log.isTraceEnabled()) {
                log.trace("Opening single connection stacktrace",
                        new Exception("debug"));
            }
        }

        /**
         * Adds a reference to the connection.
         */
        public void ref() {
            ref++;
            if (ref >= LEAK_REF_MIN && ref < LEAK_REF_MAX) {
                if (stacktraces == null) {
                    stacktraces = new ArrayList<Exception>();
                }
                stacktraces.add(new Exception(new Date().toString()));
                if (ref == LEAK_REF_MAX - 1) {
                    log.error("Probable leak of connections, listing the last callers:");
                    for (Exception e : stacktraces) {
                        log.error("Caller", e);
                    }
                    stacktraces = null;
                }
            }
        }

        /**
         * Removes a reference to the connection, and does the final close if
         * needed.
         */
        public void unref() throws SQLException {
            ref--;
            if (ref == 0) {
                threadConnectionInfo.remove();
                connection.close();
                if (log.isDebugEnabled()) {
                    log.debug("Closing single connection to: "
                            + Framework.getProperty(SINGLE_DS));
                }
                if (log.isTraceEnabled()) {
                    log.trace("Closing single connection stacktrace",
                            new Exception("debug"));
                }
            }
        }

        /**
         * Gets a new wrapped connection.
         */
        public Connection getNewConnection() {
            return (Connection) Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class[] { Connection.class },
                    new ConnectionInvocationHandler(this));
        }
    }

    /**
     * Wrapper for a connection to delegate close() to a reference counting
     * mechanism.
     *
     * @since 5.7
     */
    protected static class ConnectionInvocationHandler implements
            InvocationHandler {

        public final ConnectionInfo connectionInfo;

        public boolean closed;

        public ConnectionInvocationHandler(ConnectionInfo connectionInfo) {
            this.connectionInfo = connectionInfo;
            connectionInfo.ref();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            final String name = method.getName();
            if (name.equals("isClosed")) {
                return Boolean.valueOf(closed);
            }
            if (closed) {
                return new SQLException("Connection is closed", "08003");
            }
            if (name.equals("close")) {
                return close();
            }
            try {
                return method.invoke(connectionInfo.connection, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        protected Object close() throws SQLException {
            closed = true;
            connectionInfo.unref();
            return null;
        }
    }

    /**
     * Gets a new reference to the thread-local JDBC connection for the given
     * dataSource. The connection <strong>MUST</strong> be closed in a finally
     * block when code is done using it.
     * <p>
     * If the passed dataSource name is in the exclusion list, null will be
     * returned.
     *
     * @return a new reference to the connection, or {@code null} if single
     *         datasource connection sharing is not in effect
     */
    public static Connection getConnection(String dataSourceName)
            throws SQLException {
        if (dataSourceName == null) {
            return getConnection();
        }
        String excludes = Framework.getProperty(EXCLUDE_DS);
        if (StringUtils.isBlank(excludes)) {
            return getConnection();
        }
        for (String exclude : excludes.split("[, ] *")) {
            if (dataSourceName.equals(exclude)
                    || dataSourceName.equals(DataSourceHelper.getDataSourceJNDIName(exclude))) {
                return null;
            }
        }
        return getConnection();
    }

    /**
     * Gets a new reference to the thread-local JDBC connection. The connection
     * <strong>MUST</strong> be closed in a finally block when code is done
     * using it.
     *
     * @return a new reference to the connection, or {@code null} if single
     *         datasource connection sharing is not in effect
     */
    public static Connection getConnection() throws SQLException {
        ConnectionInfo info = ConnectionInfo.getCurrent();
        if (info == null) {
            String dataSourceName = Framework.getProperty(SINGLE_DS);
            if (StringUtils.isBlank(dataSourceName)) {
                return null;
            }
            // get a new physical connection using the single datasource
            DataSource dataSource;
            try {
                dataSource = DataSourceHelper.getDataSource(dataSourceName);
            } catch (NamingException e) {
                throw new SQLException("Cannot find datasource: "
                        + dataSourceName, e);
            }
            info = new ConnectionInfo(getConnection(dataSource));
        }
        return info.getNewConnection();
    }

    private static Connection getConnection(DataSource dataSource)
            throws SQLException {
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
                log.warn(String.format(
                        "Connections open too fast, retrying in %ds: %s",
                        Integer.valueOf(tryNo),
                        e.getMessage().replace("\n", " ")));
                try {
                    Thread.sleep(1000 * tryNo);
                } catch (InterruptedException ie) {
                    // restore interrupted status
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
