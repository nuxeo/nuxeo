/*
 * (C) Copyright 2008-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

    private static void setProperties() {
        setProperty(SERVER_PROPERTY, DEF_SERVER);
        setProperty(PORT_PROPERTY, DEF_PORT);
        setProperty(DATABASE_PROPERTY, DEF_DATABASE);
        setProperty(USER_PROPERTY, DEF_USER);
        setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
    }

    @Override
    public void setUp() throws Exception {
        Class.forName("net.sourceforge.jtds.jdbc.Driver");
        setProperties();
        String url = String.format(
                "jdbc:jtds:sqlserver://%s:%s/%s;user=%s;password=%s",
                System.getProperty(SERVER_PROPERTY),
                System.getProperty(PORT_PROPERTY),
                System.getProperty(DATABASE_PROPERTY),
                System.getProperty(USER_PROPERTY),
                System.getProperty(PASSWORD_PROPERTY));
        Connection connection = DriverManager.getConnection(url);
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

}
