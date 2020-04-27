/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.runtime.datasource;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.dbcp2.managed.BasicManagedDataSource;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.JDBCUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Helper to acquire a JDBC {@link Connection} from a datasource name.
 *
 * @since 5.7
 */
public class ConnectionHelper {

    private static final Log log = LogFactory.getLog(ConnectionHelper.class);

    /**
     * Tries to unwrap the connection to get the real physical one (returned by the original datasource).
     * <p>
     * This should only be used by code that needs to cast the connection to a driver-specific class to use
     * driver-specific features.
     *
     * @throws SQLException if no actual physical connection was allocated yet
     */
    public static Connection unwrap(Connection connection) throws SQLException {
        // now try Apache DBCP unwrap (standard or Tomcat), to skip datasource wrapping layers
        // this needs accessToUnderlyingConnectionAllowed=true in the pool config
        try {
            @SuppressWarnings("resource") // not ours to close
            Connection delegate = (Connection) MethodUtils.invokeMethod(connection, true, "getInnermostDelegate");
            if (delegate == null) {
                log.error("Cannot access underlying connection, you must use "
                        + "accessToUnderlyingConnectionAllowed=true in the pool configuration");
            } else {
                connection = delegate;
            }
        } catch (ReflectiveOperationException e) {
            // ignore missing method, connection not coming from Apache pool
        }
        return connection;
    }

    /**
     * Gets a new connection for the given dataSource. The connection <strong>MUST</strong> be closed in a finally block
     * when code is done using it.
     *
     * @param dataSourceName the datasource for which the connection is requested
     * @return a new connection
     */
    public static Connection getConnection(String dataSourceName) throws SQLException {
        return getConnection(dataSourceName, false);
    }

    /**
     * Gets a new connection for the given dataSource. The connection <strong>MUST</strong> be closed in a finally block
     * when code is done using it.
     *
     * @param dataSourceName the datasource for which the connection is requested
     * @param noSharing {@code true} if this connection must not be shared with others
     * @return a new connection
     */
    public static Connection getConnection(String dataSourceName, boolean noSharing) throws SQLException {
        DataSource dataSource = getDataSource(dataSourceName, noSharing);
        if (dataSource instanceof BasicManagedDataSource) {
            return dataSource.getConnection();
        } else {
            return JDBCUtils.getConnection(dataSource);
        }
    }

    /**
     * Gets a datasource from a datasource name, or in test mode use test connection parameters.
     *
     * @param dataSourceName the datasource name
     * @return the datasource
     */
    private static DataSource getDataSource(String dataSourceName, boolean noSharing) throws SQLException {
        try {
            return DataSourceHelper.getDataSource(dataSourceName, noSharing);
        } catch (NamingException e) {
            if (Framework.isTestModeSet()) {
                String url = Framework.getProperty("nuxeo.test.vcs.url");
                String user = Framework.getProperty("nuxeo.test.vcs.user");
                String password = Framework.getProperty("nuxeo.test.vcs.password");
                if (url != null && user != null) {
                    return new DataSourceFromUrl(url, user, password); // driver?
                }
            }
            throw new SQLException("Cannot find datasource: " + dataSourceName, e);
        }
    }

}
