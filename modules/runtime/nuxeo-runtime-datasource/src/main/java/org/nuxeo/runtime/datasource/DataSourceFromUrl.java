/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     George Lefter
 *     Florent Guillaume
 */
package org.nuxeo.runtime.datasource;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.nuxeo.common.utils.JDBCUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * This class is used for tests, where datasources are set up from a jdbc: url and user/password instead of a JNDI name.
 */
public class DataSourceFromUrl implements DataSource {

    private final String url;

    private final String user;

    private final String password;

    public DataSourceFromUrl(String url, String user, String password) {
        this.url = Framework.expandVars(url);
        this.user = Framework.expandVars(user);
        this.password = Framework.expandVars(password);
    }

    public DataSourceFromUrl(String url, String user, String password, String driver) {
        this(url, user, password);
        if (driver != null) {
            driver = Framework.expandVars(driver);
            try {
                // Driver registration is automatic since Java 6, provided
                // the JDBC library is correctly written. If it's not, then
                // this can still be useful.
                Class.forName(driver);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Driver class not found: " + driver, e);
            }
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection con = JDBCUtils.getConnection(url, user, password);
        con.setAutoCommit(true); // default
        return con;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
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
    @Override
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
