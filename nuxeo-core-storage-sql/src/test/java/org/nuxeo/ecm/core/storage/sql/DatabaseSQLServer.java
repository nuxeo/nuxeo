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
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Florent Guillaume
 */
public class DatabaseSQLServer extends DatabaseHelper {

    public static DatabaseHelper INSTANCE = new DatabaseSQLServer();

    private static final Log log = LogFactory.getLog(DatabaseSQLServer.class);

    /* Constants mentioned in the ...-mssql-contrib.xml file: */

    private static final String MSSQL_HOST_PROPERTY = "nuxeo.test.mssql.host";

    private static final String MSSQL_HOST = "172.16.245.128";

    private static final String MSSQL_PORT = "1433";

    private static final String MSSQL_DATABASE = "nuxeojunittests";

    private static final String MSSQL_DATABASE_OWNER = "nuxeo";

    private static final String MSSQL_DATABASE_PASSWORD = "nuxeo";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-mssql-contrib.xml";

    @Override
    public void setUp() throws Exception {
        Class.forName("net.sourceforge.jtds.jdbc.Driver");
        String url = String.format(
                "jdbc:jtds:sqlserver://%s:%s/%s;user=%s;password=%s;",
                MSSQL_HOST, MSSQL_PORT, MSSQL_DATABASE, MSSQL_DATABASE_OWNER,
                MSSQL_DATABASE_PASSWORD);
        Connection connection = DriverManager.getConnection(url);
        DatabaseMetaData metadata = connection.getMetaData();
        List<String> tableNames = new LinkedList<String>();
        ResultSet rs = metadata.getTables(null, null, "%",
                new String[] { "TABLE" });
        while (rs.next()) {
            tableNames.add(rs.getString("TABLE_NAME"));
        }
        // remove hierarchy last because of foreign keys
        if (tableNames.remove("hierarchy")) {
            tableNames.add("hierarchy");
        }
        Statement st = connection.createStatement();
        for (String tableName : tableNames) {
            String sql = String.format("DROP TABLE [%s]", tableName);
            log.debug(sql);
            st.execute(sql);

        }
        st.close();
        connection.close();
        System.setProperty(MSSQL_HOST_PROPERTY, MSSQL_HOST);
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
        properties.put("ServerName", MSSQL_HOST);
        properties.put("PortNumber", MSSQL_PORT);
        properties.put("DatabaseName", MSSQL_DATABASE);
        properties.put("User", MSSQL_DATABASE_OWNER);
        properties.put("Password", MSSQL_DATABASE_PASSWORD);
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
}
