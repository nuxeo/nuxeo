/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     George Lefter
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.sql;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * This class is used for tests, where datasources are set up from a driver, url
 * and user/password instead of a JNDI name.
 */
public class SimpleDataSource implements DataSource {

    private static final Log log = LogFactory.getLog(SimpleDataSource.class);

    /**
     * Maximum number of time we retry a connection if the server says it's
     * overloaded.
     */
    public static final int MAX_CONNECTION_TRIES = 5;

    private final String url;

    private final String user;

    private final String password;

    public SimpleDataSource(String url, String driver, String user,
            String password) {
        url = Framework.expandVars(url);
        driver = Framework.expandVars(driver);
        user = Framework.expandVars(user);
        password = Framework.expandVars(password);
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("driver class not found", e);
        }
        this.url = url;
        this.user = user;
        this.password = password;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection con = null;
        int tryNo = 0;
        for (;;) {
            try {
                con = DriverManager.getConnection(url, user, password);
                break;
            } catch (SQLException e) {
                if (++tryNo >= MAX_CONNECTION_TRIES) {
                    throw e;
                }
                if (e.getErrorCode() != 12519) {
                    throw e;
                }
                // Oracle: Listener refused the connection with the
                // following error: ORA-12519, TNS:no appropriate
                // service handler found
                // SQLState = "66000"
                // Happens when connections are open too fast (unit tests)
                // -> retry a few times after a small delay
                log.warn(String.format(
                        "Connections open too fast, retrying in %ds: %s",
                        tryNo, e.getMessage().replace("\n", " ")));
                try {
                    Thread.sleep(1000 * tryNo);
                } catch (InterruptedException ie) {
                    // restore interrupted status
                    Thread.currentThread().interrupt();
                }
            }
        }
        con.setAutoCommit(false);
        return con;
    }

    @Override
    public Connection getConnection(String username, String password)
            throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        throw new UnsupportedOperationException();
    }

    // @Override in CommonDataSource for Java SE 7 / JDBC 4.1
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

}
