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
import java.sql.SQLException;
import java.sql.Statement;

import org.nuxeo.runtime.api.Framework;

/**
 * @author Florent Guillaume
 */
public class DatabasePostgreSQL extends DatabaseHelper {

    private static final String DEF_SERVER = "localhost";

    private static final String DEF_PORT = "5432";

    private static final String DEF_USER = "nuxeo";

    private static final String DEF_PASSWORD = "nuxeo";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-postgresql-contrib.xml";

    private static final String DRIVER = "org.postgresql.Driver";

    protected void setProperties() {
        String db = setProperty(DATABASE_PROPERTY, databaseName);
        String server = setProperty(SERVER_PROPERTY, DEF_SERVER);
        String port = setProperty(PORT_PROPERTY, DEF_PORT);
        String user = setProperty(USER_PROPERTY, DEF_USER);
        String password = setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
        // for sql directory tests
        String driver = setProperty(DRIVER_PROPERTY, DRIVER);
        String url = String.format("jdbc:postgresql://%s:%s/%s", server, port, db);
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
        Connection connection = DriverManager.getConnection(Framework.getProperty(URL_PROPERTY),
                Framework.getProperty(USER_PROPERTY), Framework.getProperty(PASSWORD_PROPERTY));
        try {
            doOnAllTables(connection, null, "public", "DROP TABLE \"%s\" CASCADE");
            Statement st = connection.createStatement();
            executeSql(st, "DROP SEQUENCE IF EXISTS hierarchy_seq");
            st.close();
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
        descriptor.setFulltextAnalyzer("french");
        descriptor.setPathOptimizationsEnabled(true);
        descriptor.setAclOptimizationsEnabled(true);
        descriptor.idType = Framework.getProperty(ID_TYPE_PROPERTY);
        return descriptor;
    }

    @Override
    public boolean supportsClustering() {
        return true;
    }

    @Override
    public boolean supportsSoftDelete() {
        return true;
    }

    @Override
    public boolean supportsSequenceId() {
        return true;
    }

    @Override
    public boolean supportsArrayColumns() {
        return true;
    }

}
