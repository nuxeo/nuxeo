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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.api.Framework;

/**
 * @author Florent Guillaume
 */
public class DatabaseSQLServer extends DatabaseHelper {

    public static DatabaseHelper INSTANCE = new DatabaseSQLServer();

    private boolean supportsXA;

    private boolean supportsSequences;

    private static final String DEF_SERVER = "localhost";

    private static final String DEF_PORT = "1433";

    private static final String DEF_DATABASE = "nuxeojunittests";

    private static final String DEF_USER = "nuxeo";

    private static final String DEF_PASSWORD = "nuxeo";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-mssql-contrib.xml";

    // true for the Microsoft JDBC driver
    // false for the jTDS JDBC driver (open source)
    private static final boolean MSFT = false;

    private static final String DRIVER = MSFT ? "com.microsoft.sqlserver.jdbc.SQLServerDriver"
            : "net.sourceforge.jtds.jdbc.Driver";

    private static final String XA_DATASOURCE = MSFT ? "com.microsoft.sqlserver.jdbc.SQLServerXADataSource"
            : "net.sourceforge.jtds.jdbcx.JtdsDataSource";

    private void setProperties() {
        Framework.getProperties().setProperty(REPOSITORY_PROPERTY, repositoryName);
        setProperty(SERVER_PROPERTY, DEF_SERVER);
        setProperty(PORT_PROPERTY, DEF_PORT);
        setProperty(DATABASE_PROPERTY, DEF_DATABASE);
        setProperty(USER_PROPERTY, DEF_USER);
        setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
        setProperty(XA_DATASOURCE_PROPERTY, XA_DATASOURCE);
        // for sql directory tests
        setProperty(DRIVER_PROPERTY, DRIVER);
        String url;
        if (DRIVER.startsWith("com.microsoft")) {
            url = String.format(
                    "jdbc:sqlserver://%s:%s;databaseName=%s;user=%s;password=%s",
                    Framework.getProperty(SERVER_PROPERTY),
                    Framework.getProperty(PORT_PROPERTY),
                    Framework.getProperty(DATABASE_PROPERTY),
                    Framework.getProperty(USER_PROPERTY),
                    Framework.getProperty(PASSWORD_PROPERTY));

        } else {
            url = String.format(
                    "jdbc:jtds:sqlserver://%s:%s;databaseName=%s;user=%s;password=%s",
                    Framework.getProperty(SERVER_PROPERTY),
                    Framework.getProperty(PORT_PROPERTY),
                    Framework.getProperty(DATABASE_PROPERTY),
                    Framework.getProperty(USER_PROPERTY),
                    Framework.getProperty(PASSWORD_PROPERTY));
        }
        setProperty(URL_PROPERTY, url);
        setProperty(ID_TYPE_PROPERTY, DEF_ID_TYPE);
    }

    @Override
    public void setUp() throws Exception {
        Class.forName(DRIVER);
        setProperties();
        Connection connection = DriverManager.getConnection(System.getProperty(URL_PROPERTY));
        try  {
            doOnAllTables(connection, null, null, "DROP TABLE [%s]"); // no CASCADE...
            checkSupports(connection);
            // SEQUENCE in SQL Server 2012, but not Azure
            if (supportsSequences) {
                Statement st = connection.createStatement();
                executeSql(st,
                        "IF EXISTS (SELECT 1 FROM sys.sequences WHERE name = 'hierarchy_seq')"
                                + " DROP SEQUENCE hierarchy_seq");
                st.close();
            }
        } finally {
            connection.close();
        }
    }

    @Override
    public String getDeploymentContrib() {
        return CONTRIB_XML;
    }

    @Override
    public RepositoryDescriptor getRepositoryDescriptor() {
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        descriptor.xaDataSourceName = XA_DATASOURCE;
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("ServerName", System.getProperty(SERVER_PROPERTY));
        properties.put("PortNumber", System.getProperty(PORT_PROPERTY));
        properties.put("DatabaseName", System.getProperty(DATABASE_PROPERTY));
        properties.put("User", System.getProperty(USER_PROPERTY));
        properties.put("Password", System.getProperty(PASSWORD_PROPERTY));
        properties.put("UseCursors", "true");
        descriptor.properties = properties;
        descriptor.fulltextAnalyzer = "French";
        descriptor.fulltextCatalog = "nuxeo";
        descriptor.idType = System.getProperty(ID_TYPE_PROPERTY);
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

    protected void checkSupports(Connection connection) throws SQLException {
        Statement st = connection.createStatement();
        try {
            ResultSet rs = st.executeQuery("SELECT SERVERPROPERTY('ProductVersion'), CONVERT(NVARCHAR(100), SERVERPROPERTY('EngineEdition'))");
            rs.next();
            String productVersion = rs.getString(1);
            /** 9 = SQL Server 2005, 10 = SQL Server 2008, 11 = SQL Server 2012 / Azure */
            int majorVersion = Integer.parseInt(productVersion.split("\\.")[0]);
            /** 5 = Azure */
            int engineEdition = rs.getInt(2);
            boolean azure = engineEdition == 5;
            supportsXA = !azure;
            supportsSequences = majorVersion >= 11 && !azure;
        } finally {
            st.close();
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

    @Override
    public boolean supportsXA() {
        return supportsXA;
    }

    @Override
    public boolean supportsSoftDelete() {
        return true;
    }

    @Override
    public boolean supportsSequenceId() {
        return supportsSequences;
    }

}
