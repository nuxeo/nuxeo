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
 */

package org.nuxeo.ecm.core.storage.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Florent Guillaume
 */
public class DatabaseSQLServer extends DatabaseHelper {

    public static DatabaseHelper INSTANCE = new DatabaseSQLServer();

    private static final String DEF_SERVER = "localhost";

    private static final String DEF_PORT = "1433";

    private static final String DEF_DATABASE = "nuxeojunittests";

    private static final String DEF_USER = "nuxeo";

    private static final String DEF_PASSWORD = "nuxeo";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-mssql-contrib.xml";

    private static final String DRIVER = "net.sourceforge.jtds.jdbc.Driver";

    private static void setProperties() {
        setProperty(SERVER_PROPERTY, DEF_SERVER);
        setProperty(PORT_PROPERTY, DEF_PORT);
        setProperty(DATABASE_PROPERTY, DEF_DATABASE);
        setProperty(USER_PROPERTY, DEF_USER);
        setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
        // for sql directory tests
        setProperty(DRIVER_PROPERTY, DRIVER);
        String url = String.format(
                "jdbc:jtds:sqlserver://%s:%s/%s;user=%s;password=%s",
                System.getProperty(SERVER_PROPERTY),
                System.getProperty(PORT_PROPERTY),
                System.getProperty(DATABASE_PROPERTY),
                System.getProperty(USER_PROPERTY),
                System.getProperty(PASSWORD_PROPERTY));
        setProperty(URL_PROPERTY, url);
    }

    @Override
    public void setUp() throws Exception {
        Class.forName(DRIVER);
        setProperties();
        Connection connection = DriverManager.getConnection(System.getProperty(URL_PROPERTY));
        doOnAllTables(connection, null, null, "DROP TABLE [%s]"); // no CASCADE...
        connection.close();
    }

    @Override
    public String getDeploymentContrib() {
        return CONTRIB_XML;
    }

    @Override
    public RepositoryDescriptor getRepositoryDescriptor() {
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        descriptor.xaDataSourceName = "net.sourceforge.jtds.jdbcx.JtdsDataSource";
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("ServerName", System.getProperty(SERVER_PROPERTY));
        properties.put("PortNumber", System.getProperty(PORT_PROPERTY));
        properties.put("DatabaseName", System.getProperty(DATABASE_PROPERTY));
        properties.put("User", System.getProperty(USER_PROPERTY));
        properties.put("Password", System.getProperty(PASSWORD_PROPERTY));
        properties.put("UseCursors", "true");
        descriptor.properties = properties;
        descriptor.fulltextAnalyzer = "french";
        descriptor.fulltextCatalog = "nuxeo";
        return descriptor;
    }

    // MS SQL Server has asynchronous indexing of fulltext
    @Override
    public void sleepForFulltext() {
        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public boolean supportsMultipleFulltextIndexes() {
        return false;
    }

    @Override
    public boolean supportsClustering() {
        return true;
    }

}
