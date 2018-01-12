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
 */

package org.nuxeo.ecm.core.storage.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.nuxeo.runtime.api.Framework;

/**
 * @author Florent Guillaume
 */
public class DatabaseSQLServer extends DatabaseHelper {

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
    private static final boolean MSFT = true;

    private static final String DRIVER = MSFT ? "com.microsoft.sqlserver.jdbc.SQLServerDriver"
            : "net.sourceforge.jtds.jdbc.Driver";

    private void setProperties() {
        setProperty(SERVER_PROPERTY, DEF_SERVER);
        setProperty(PORT_PROPERTY, DEF_PORT);
        setProperty(DATABASE_PROPERTY, DEF_DATABASE);
        setProperty(USER_PROPERTY, DEF_USER);
        setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
        // for sql directory tests
        setProperty(DRIVER_PROPERTY, DRIVER);
        String url;
        if (DRIVER.startsWith("com.microsoft")) {
            url = String.format("jdbc:sqlserver://%s:%s;databaseName=%s;user=%s;password=%s;selectMethod=cursor",
                    Framework.getProperty(SERVER_PROPERTY), Framework.getProperty(PORT_PROPERTY),
                    Framework.getProperty(DATABASE_PROPERTY), Framework.getProperty(USER_PROPERTY),
                    Framework.getProperty(PASSWORD_PROPERTY));

        } else {
            url = String.format("jdbc:jtds:sqlserver://%s:%s;databaseName=%s;user=%s;password=%s;useCursors=true",
                    Framework.getProperty(SERVER_PROPERTY), Framework.getProperty(PORT_PROPERTY),
                    Framework.getProperty(DATABASE_PROPERTY), Framework.getProperty(USER_PROPERTY),
                    Framework.getProperty(PASSWORD_PROPERTY));
        }
        setProperty(URL_PROPERTY, url);
        setProperty(ID_TYPE_PROPERTY, DEF_ID_TYPE);
    }

    @Override
    public void setUp() throws SQLException {
        super.setUp();
        try {
            Class.forName(DRIVER);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        setProperties();
        Connection connection = DriverManager.getConnection(Framework.getProperty(URL_PROPERTY));
        try {
            doOnAllTables(connection, null, null, "DROP TABLE [%s]"); // no CASCADE...
            checkSupports(connection);
            // SEQUENCE in SQL Server 2012, but not Azure
            if (supportsSequences) {
                Statement st = connection.createStatement();
                executeSql(st, "IF EXISTS (SELECT 1 FROM sys.sequences WHERE name = 'hierarchy_seq')"
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
        descriptor.setFulltextAnalyzer("French");
        descriptor.setFulltextCatalog("nuxeo");
        descriptor.idType = Framework.getProperty(ID_TYPE_PROPERTY);
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
            ResultSet rs = st.executeQuery("SELECT CONVERT(NVARCHAR(100),SERVERPROPERTY('ProductVersion')), CONVERT(NVARCHAR(100), SERVERPROPERTY('EngineEdition'))");
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
