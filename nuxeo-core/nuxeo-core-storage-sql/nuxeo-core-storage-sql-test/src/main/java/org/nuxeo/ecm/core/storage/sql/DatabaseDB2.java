/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

public class DatabaseDB2 extends DatabaseHelper {

    private static final Log log = LogFactory.getLog(DatabaseDB2.class);

    private static final String DEF_SERVER = "localhost";

    private static final String DEF_PORT = "3700";

    // 8 chars max
    private static final String DEFAULT_DATABASE_NAME = "nuxeotst";

    private static final String DEF_USER = "db2inst1";

    private static final String DEF_PASSWORD = "db2inst1pw99";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-db2-contrib.xml";

    private static final String DRIVER = "com.ibm.db2.jcc.DB2Driver";

    protected void setProperties() {
        databaseName = DEFAULT_DATABASE_NAME;
        setProperty(DATABASE_PROPERTY, databaseName);
        setProperty(SERVER_PROPERTY, DEF_SERVER);
        setProperty(PORT_PROPERTY, DEF_PORT);
        setProperty(USER_PROPERTY, DEF_USER);
        setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
        // for sql directory tests
        setProperty(DRIVER_PROPERTY, DRIVER);
        String url = String.format("jdbc:db2://%s:%s/%s", Framework.getProperty(SERVER_PROPERTY),
                Framework.getProperty(PORT_PROPERTY), Framework.getProperty(DATABASE_PROPERTY));
        setProperty(URL_PROPERTY, url);
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
        doOnAllTables(connection, null, Framework.getProperty(USER_PROPERTY).toUpperCase(), "DROP TABLE \"%s\"");
        dropSequences(connection);
        connection.close();
    }

    public void dropSequences(Connection connection) throws SQLException {
        List<String> sequenceNames = new ArrayList<String>();
        try (Statement st = connection.createStatement()) {
            try (ResultSet rs = st.executeQuery("SELECT SEQUENCE_NAME FROM USER_SEQUENCES")) {
                while (rs.next()) {
                    String sequenceName = rs.getString(1);
                    if (sequenceName.indexOf('$') != -1) {
                        continue;
                    }
                    sequenceNames.add(sequenceName);
                }
            }
            for (String sequenceName : sequenceNames) {
                String sql = String.format("DROP SEQUENCE \"%s\"", sequenceName);
                log.trace("SQL: " + sql);
                st.execute(sql);
            }
        }
    }

    @Override
    public String getDeploymentContrib() {
        return CONTRIB_XML;
    }

    @Override
    public RepositoryDescriptor getRepositoryDescriptor() {
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        return descriptor;
    }

    @Override
    public boolean supportsClustering() {
        return true;
    }

}
