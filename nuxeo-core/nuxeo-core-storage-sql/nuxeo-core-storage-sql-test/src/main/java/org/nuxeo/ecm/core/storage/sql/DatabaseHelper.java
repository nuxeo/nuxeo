/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */

package org.nuxeo.ecm.core.storage.sql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.JDBCUtils;
import org.nuxeo.ecm.core.blob.binary.BinaryManager;
import org.nuxeo.ecm.core.blob.binary.DefaultBinaryManager;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;

public abstract class DatabaseHelper {

    private static final Log log = LogFactory.getLog(DatabaseHelper.class);

    public static final String DB_PROPERTY = "nuxeo.test.vcs.db";

    public static final String DB_DEFAULT = "H2";

    public static final String DEF_ID_TYPE = "varchar"; // "varchar", "uuid", "sequence"

    public static DatabaseHelper DATABASE;

    public static final String DB_CLASS_NAME_BASE = "org.nuxeo.ecm.core.storage.sql.Database";

    protected static final Class<? extends BinaryManager> defaultBinaryManager = DefaultBinaryManager.class;

    static {
        setSystemProperty(DB_PROPERTY, DB_DEFAULT);
        String className = System.getProperty(DB_PROPERTY);
        if (className.indexOf('.') < 0) {
            className = DB_CLASS_NAME_BASE + className;
        }
        setDatabaseForTests(className);
    }

    // available for JDBC tests
    public static final String DRIVER_PROPERTY = "nuxeo.test.vcs.driver";

    // available for JDBC tests
    public static final String URL_PROPERTY = "nuxeo.test.vcs.url";

    public static final String SERVER_PROPERTY = "nuxeo.test.vcs.server";

    public static final String PORT_PROPERTY = "nuxeo.test.vcs.port";

    public static final String DATABASE_PROPERTY = "nuxeo.test.vcs.database";

    public static final String USER_PROPERTY = "nuxeo.test.vcs.user";

    public static final String PASSWORD_PROPERTY = "nuxeo.test.vcs.password";

    public static final String ID_TYPE_PROPERTY = "nuxeo.test.vcs.idtype";

    protected Error owner;

    public static String setSystemProperty(String name, String def) {
        String value = System.getProperty(name);
        if (value == null || value.equals("") || value.equals("${" + name + "}")) {
            System.setProperty(name, def);
        }
        return value;
    }

    public static String setProperty(String name, String def) {
        String value = System.getProperty(name);
        if (value == null || value.equals("") || value.equals("${" + name + "}")) {
            value = def;
        }
        Framework.getProperties().setProperty(name, value);
        return value;
    }

    public static final String DEFAULT_DATABASE_NAME = "nuxeojunittests";

    public String databaseName = DEFAULT_DATABASE_NAME;

    public void setDatabaseName(String name) {
        databaseName = name;
    }

    /**
     * Sets the database backend used for VCS unit tests.
     */
    public static void setDatabaseForTests(String className) {
        try {
            DATABASE = (DatabaseHelper) Class.forName(className).newInstance();
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError("Database class not found: " + className);
        }
    }

    /**
     * Gets a database connection, retrying if the server says it's overloaded.
     *
     * @since 5.9.3
     */
    public static Connection getConnection(String url, String user, String password) throws SQLException {
        return JDBCUtils.getConnection(url, user, password);
    }

    /**
     * Executes one statement on all the tables in a database.
     */
    public static void doOnAllTables(Connection connection, String catalog, String schemaPattern, String statement)
            throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        List<String> tableNames = new LinkedList<String>();
        Set<String> truncateFirst = new HashSet<String>();
        ResultSet rs = metadata.getTables(catalog, schemaPattern, "%", new String[] { "TABLE" });
        while (rs.next()) {
            String tableName = rs.getString("TABLE_NAME");
            if (tableName.indexOf('$') != -1) {
                // skip Oracle 10g flashback/fulltext-index tables
                continue;
            }
            if (tableName.toLowerCase().startsWith("trace_xe_")) {
                // Skip mssql 2012 system table
                continue;
            }
            if ("ACLR_USER_USERS".equals(tableName)) {
                // skip nested table that is dropped by the main table
                continue;
            }
            if ("ANCESTORS_ANCESTORS".equals(tableName)) {
                // skip nested table that is dropped by the main table
                continue;
            }
            if ("ACLR_MODIFIED".equals(tableName) && DATABASE instanceof DatabaseOracle) {
                // global temporary table on Oracle, must TRUNCATE before DROP
                truncateFirst.add(tableName);
            }
            tableNames.add(tableName);
        }
        // not all databases can cascade on drop
        // remove hierarchy last because of foreign keys
        if (tableNames.remove("HIERARCHY")) {
            tableNames.add("HIERARCHY");
        }
        // needed for Azure
        if (tableNames.remove("NXP_LOGS")) {
            tableNames.add("NXP_LOGS");
        }
        if (tableNames.remove("NXP_LOGS_EXTINFO")) {
            tableNames.add("NXP_LOGS_EXTINFO");
        }
        // PostgreSQL is lowercase
        if (tableNames.remove("hierarchy")) {
            tableNames.add("hierarchy");
        }
        Statement st = connection.createStatement();
        for (String tableName : tableNames) {
            if (truncateFirst.contains(tableName)) {
                String sql = String.format("TRUNCATE TABLE \"%s\"", tableName);
                executeSql(st, sql);
            }
            String sql = String.format(statement, tableName);
            executeSql(st, sql);
        }
        st.close();
    }

    protected static void executeSql(Statement st, String sql) throws SQLException {
        log.trace("SQL: " + sql);
        st.execute(sql);
    }

    public void setUp() throws SQLException {
        setOwner();
        setDatabaseName(DEFAULT_DATABASE_NAME);
        setBinaryManager(defaultBinaryManager, "");
        Framework.addListener(new RuntimeServiceListener() {

            @Override
            public void handleEvent(RuntimeServiceEvent event) {
                if (RuntimeServiceEvent.RUNTIME_STOPPED == event.id) {
                    try {
                        tearDown();
                    } catch (SQLException cause) {
                        throw new AssertionError("Cannot teardown database", cause);
                    }
                }
            }
        });
    }

    protected void setOwner() {
        if (owner != null) {
            Error e = new Error("Second call to setUp() without tearDown()", owner);
            log.fatal(e.getMessage(), e);
            throw e;
        }
        owner = new Error("Database not released");
    }

    /**
     * @throws SQLException
     */
    public void tearDown() throws SQLException {
        owner = null;
    }

    public static void setBinaryManager(Class<? extends BinaryManager> binaryManagerClass, String key) {
        setProperty("nuxeo.test.vcs.binary-manager", binaryManagerClass.getName());
        setProperty("nuxeo.test.vcs.binary-manager-key", key);
    }

    public abstract String getDeploymentContrib();

    public abstract RepositoryDescriptor getRepositoryDescriptor();

    /**
     * For databases that do asynchronous fulltext indexing, sleep a bit.
     */
    public void sleepForFulltext() {
    }

    /**
     * For databases that fail to cascade deletes beyond a certain depth.
     */
    public int getRecursiveRemovalDepthLimit() {
        return 0;
    }

    /**
     * For databases that don't support clustering.
     */
    public boolean supportsClustering() {
        return false;
    }

    public boolean supportsMultipleFulltextIndexes() {
        return true;
    }

    public boolean supportsXA() {
        return true;
    }

    public boolean supportsSoftDelete() {
        return false;
    }

    /**
     * Whether this database supports "sequence" as an id type.
     *
     * @since 5.9.3
     */
    public boolean supportsSequenceId() {
        return false;
    }

    public boolean supportsArrayColumns() {
        return false;
    }

}
