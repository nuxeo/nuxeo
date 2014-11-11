/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.repository.RepositoryFactory;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLRepositoryFactory;
import org.nuxeo.runtime.api.ConnectionHelper;

public abstract class DatabaseHelper {

    private static final Log log = LogFactory.getLog(DatabaseHelper.class);

    public static final String DB_PROPERTY = "nuxeo.test.vcs.db";

    public static final String DB_DEFAULT = "H2";

    public static final String DEF_ID_TYPE = "varchar"; // "varchar", "uuid", "sequence"

    private static final boolean SINGLEDS_DEFAULT = false;

    public static DatabaseHelper DATABASE;

    public static final String DB_CLASS_NAME_BASE = "org.nuxeo.ecm.core.storage.sql.Database";

    protected static final Class<? extends RepositoryFactory> defaultRepositoryFactory = SQLRepositoryFactory.class;

    static {
        setProperty(DB_PROPERTY, DB_DEFAULT);
        String className = System.getProperty(DB_PROPERTY);
        if (className.indexOf('.') < 0) {
            className = DB_CLASS_NAME_BASE + className;
        }
        setDatabaseForTests(className);
        setRepositoryFactory(defaultRepositoryFactory);
        setSingleDataSourceMode();
    }

    public static final String REPOSITORY_PROPERTY = "nuxeo.test.vcs.repository";

    // available for JDBC tests
    public static final String DRIVER_PROPERTY = "nuxeo.test.vcs.driver";

    // available for JDBC tests
    public static final String XA_DATASOURCE_PROPERTY = "nuxeo.test.vcs.xadatasource";

    // available for JDBC tests
    public static final String URL_PROPERTY = "nuxeo.test.vcs.url";

    public static final String SERVER_PROPERTY = "nuxeo.test.vcs.server";

    public static final String PORT_PROPERTY = "nuxeo.test.vcs.port";

    public static final String DATABASE_PROPERTY = "nuxeo.test.vcs.database";

    public static final String USER_PROPERTY = "nuxeo.test.vcs.user";

    public static final String PASSWORD_PROPERTY = "nuxeo.test.vcs.password";

    public static final String ID_TYPE_PROPERTY = "nuxeo.test.vcs.idtype";

    // set this to true to activate single datasource for all tests
    public static final String SINGLEDS_PROPERTY = "nuxeo.test.vcs.singleds";

    public static String setProperty(String name, String def) {
        String value = System.getProperty(name);
        if (value == null || value.equals("")
                || value.equals("${" + name + "}")) {
            System.setProperty(name, def);
        }
        return value;
    }

    public static final String DEFAULT_DATABASE_NAME = "nuxeojunittests";

    protected String databaseName = DEFAULT_DATABASE_NAME;

    public void setDatabaseName(String name) {
        this.databaseName = name;
    }

    public static final String DEFAULT_REPOSITORY_NAME = "test";

    public String repositoryName = DEFAULT_REPOSITORY_NAME;

    public void setRepositoryName(String name) {
        this.repositoryName = name;
    }

    /**
     * Sets the database backend used for VCS unit tests.
     */
    public static void setDatabaseForTests(String className) {
        try {
            DATABASE = (DatabaseHelper) Class.forName(className).newInstance();
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Database class not found: "
                    + className);
        }
        String msg = "Database used for VCS tests: " + className;
        // System.out used on purpose, don't remove
        System.out.println(DatabaseHelper.class.getSimpleName() + ": " + msg);
        log.info(msg);
    }

    /**
     * Executes one statement on all the tables in a database.
     */
    public static void doOnAllTables(Connection connection, String catalog,
            String schemaPattern, String statement) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        List<String> tableNames = new LinkedList<String>();
        ResultSet rs = metadata.getTables(catalog, schemaPattern, "%",
                new String[] { "TABLE" });
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
            String sql = String.format(statement, tableName);
            executeSql(st, sql);
        }
        st.close();
    }

    protected static void executeSql(Statement st, String sql)
            throws SQLException {
        log.trace("SQL: " + sql);
        st.execute(sql);
    }

    public void setUp(Class<? extends RepositoryFactory> factoryClass)
            throws Exception {
        setRepositoryFactory(factoryClass);
        setUp();
    }

    public abstract void setUp() throws Exception;

    /**
     * @throws SQLException
     */
    public void tearDown() throws SQLException {
        setDatabaseName(DEFAULT_DATABASE_NAME);
        setRepositoryName(DEFAULT_REPOSITORY_NAME);
        setRepositoryFactory(defaultRepositoryFactory);
    }

    public static void setRepositoryFactory(
            Class<? extends RepositoryFactory> factoryClass) {
        System.setProperty("nuxeo.test.vcs.repository-factory",
                factoryClass.getName());
    }

    public abstract String getDeploymentContrib();

    public abstract RepositoryDescriptor getRepositoryDescriptor();

    public static void setSingleDataSourceMode() {
        if (Boolean.parseBoolean(System.getProperty(SINGLEDS_PROPERTY))
                || SINGLEDS_DEFAULT) {
            // the name doesn't actually matter, as code in
            // ConnectionHelper.getDataSource ignores it and uses
            // nuxeo.test.vcs.url etc. for connections in test mode
            String dataSourceName = "jdbc/NuxeoTestDS";
            System.setProperty(ConnectionHelper.SINGLE_DS, dataSourceName);
        }
    }

    /**
     * For databases that do asynchronous fulltext indexing, sleep a bit.
     */
    public void sleepForFulltext() {
    }

    /**
     * For databases that don't have subsecond resolution, sleep a bit to get to
     * the next second.
     */
    public void maybeSleepToNextSecond() {
        if (!hasSubSecondResolution()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
    }

    /**
     * For databases that don't have subsecond resolution, like MySQL.
     */
    public boolean hasSubSecondResolution() {
        return true;
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

}
