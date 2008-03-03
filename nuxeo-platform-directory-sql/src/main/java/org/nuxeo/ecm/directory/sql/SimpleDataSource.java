/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.directory.sql;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * TODO: OG: please someone explain here the goal of this empty implementation
 * of the DataSource interface. According to the Call hierarchy it is only
 * needed for the getConnection() method.
 *
 * TODO: OG: maybe methods with empty implementation should raise SQLException
 * wrapping UnexpectedOperationException instead.
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class SimpleDataSource implements DataSource {

    private final String url;

    private final String user;

    private final String password;

    public SimpleDataSource(String url, String driver, String user,
            String password) {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("driver class not found", e);
        }
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Connection getConnection() throws SQLException {
        Connection con = DriverManager.getConnection(url, user, password);
        con.setAutoCommit(false);
        return con;
    }

    public Connection getConnection(String username, String password)
            throws SQLException {
        return getConnection();
    }

    public PrintWriter getLogWriter() throws SQLException {
        // not used by Nuxeo SQLDirectory API
        return null;
    }

    public int getLoginTimeout() throws SQLException {
        // not used by Nuxeo SQLDirectory API
        return 0;
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        // not used by Nuxeo SQLDirectory API
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        // not used by Nuxeo SQLDirectory API
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        // not used by Nuxeo SQLDirectory API
        return false;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        // not used by Nuxeo SQLDirectory API
        return null;
    }

}
