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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is a simplified version of the Nuxeo 5.7 wrapper allowing use of a
 * "physical" JDBC connection coming from a datasource configured globally
 * <p>
 * The physical connections are created from the datasource configured using the
 * framework property {@value #SINGLE_DS}.
 *
 * @since 5.7
 */
public class ConnectionHelper {

    private static final Log log = LogFactory.getLog(ConnectionHelper.class);

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
     * Checks if single-datasource mode will be used for the given datasource
     * name.
     *
     * @return {@code true} if using a single datasource
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
     * Gets a new connection.
     * <p>
     * If the passed dataSource name is in the exclusion list, null will be
     * returned.
     *
     * @param dataSourceName the datasource for which the connection is
     *            requested
     * @return a new connection, or {@code null} if single datasource connection
     *         sharing is not in effect
     */
    public static Connection getConnection(String dataSourceName)
            throws SQLException {
        if (!useSingleConnection(dataSourceName)) {
            return null;
        }
        dataSourceName = Framework.getProperty(SINGLE_DS);
        if (StringUtils.isBlank(dataSourceName)) {
            return null;
        }
        return getPhysicalConnection(dataSourceName);
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
        } catch (NoSuchMethodException e) {
            // ignore missing method, connection not coming from Apache pool
        } catch (IllegalAccessException e) {
            // ignore missing method, connection not coming from Apache pool
        } catch (InvocationTargetException e) {
            // ignore missing method, connection not coming from Apache pool
        }
        return connection;
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

}
