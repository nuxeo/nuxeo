/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.common.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Callable;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper for common JDBC-related operations.
 *
 * @since 7.3
 */
public class JDBCUtils {

    private static final Log log = LogFactory.getLog(JDBCUtils.class);

    /**
     * Maximum number of times we retry a call if the server says it's overloaded.
     */
    public static final int MAX_TRIES = 5;

    /**
     * Tries to do a JDBC call even when the server is overloaded.
     * <p>
     * Oracle has problems opening and closing many connections in a short time span (ORA-12516, ORA-12519). It seems to
     * have something to do with how closed sessions are not immediately accounted for by Oracle's PMON (process
     * monitor). When we get these errors, we retry a few times with exponential backoff.
     *
     * @param callable the callable
     * @return the returned value
     */
    public static <V> V callWithRetry(Callable<V> callable) throws SQLException {
        for (int tryNo = 1;; tryNo++) {
            try {
                return callable.call();
            } catch (SQLException e) {
                if (tryNo >= MAX_TRIES) {
                    throw e;
                }
                int errorCode = e.getErrorCode();
                if (errorCode != 12516 && errorCode != 12519) {
                    throw e;
                }
                // Listener refused the connection with the following error:
                // ORA-12519, TNS:no appropriate service handler found
                // ORA-12516, TNS:listener could not find available handler with matching protocol stack
                if (log.isDebugEnabled()) {
                    log.debug(String.format("Connections open too fast, retrying in %ds: %s", Integer.valueOf(tryNo),
                            e.getMessage().replace("\n", " ")));
                }
                try {
                    Thread.sleep(1000 * tryNo);
                } catch (InterruptedException ie) { // deals with interrupt below
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(ie);
                }
            } catch (Exception e) { // deals with interrupt below
                throw ExceptionUtils.runtimeException(e);
            }
        }
    }

    /**
     * Tries to acquire a {@link Connection} through the {@link DriverManager} even when the server is overloaded.
     *
     * @param url a database url of the form <code>jdbc:<em>subprotocol</em>:<em>subname</em></code>
     * @param user the database user on whose behalf the connection is being made
     * @param password the user's password
     * @return a connection to the URL
     */
    public static Connection getConnection(final String url, final String user, final String password)
            throws SQLException {
        return callWithRetry(() -> DriverManager.getConnection(url, user, password));
    }

    /**
     * Tries to acquire a {@link Connection} through a {@link DataSource} even when the server is overloaded.
     *
     * @param dataSource the data source
     * @return a connection to the data source
     */
    public static Connection getConnection(final DataSource dataSource) throws SQLException {
        return callWithRetry(dataSource::getConnection);
    }

    /**
     * Tries to acquire a {@link XAConnection} through a {@link XADataSource} even when the server is overloaded.
     *
     * @param xaDataSource the XA data source
     * @return a XA connection to the XA data source
     */
    public static XAConnection getXAConnection(final XADataSource xaDataSource) throws SQLException {
        return callWithRetry(xaDataSource::getXAConnection);
    }

}
