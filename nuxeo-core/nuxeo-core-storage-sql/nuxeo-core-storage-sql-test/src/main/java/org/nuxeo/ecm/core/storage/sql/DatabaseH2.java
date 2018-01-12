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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Florent Guillaume
 */
public class DatabaseH2 extends DatabaseHelper {

    private static final Log log = LogFactory.getLog(DatabaseH2.class);

    /** This directory will be deleted and recreated. */
    protected static final String DIRECTORY = "target";

    protected static final String DEF_USER = "sa";

    protected static final String DEF_PASSWORD = "";

    protected static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-h2-contrib.xml";

    protected static final String DRIVER = "org.h2.Driver";

    protected static final String URL_FORMAT = "jdbc:h2:mem:%s;DB_CLOSE_DELAY=-1";

    protected String url;

    protected String user;

    protected String password;

    protected void setProperties() {
        url = setProperty(URL_PROPERTY, String.format(URL_FORMAT, databaseName));

        setProperty(DATABASE_PROPERTY, databaseName);
        user = setProperty(USER_PROPERTY, DEF_USER);
        password = setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
        // for sql directory tests
        setProperty(DRIVER_PROPERTY, DRIVER);
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
        checkDatabaseLive();
    }

    protected void checkDatabaseLive() throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, Framework.getProperty(USER_PROPERTY, "sa"),
                Framework.getProperty(PASSWORD_PROPERTY, null))) {
            try (Statement st = connection.createStatement()) {
                st.execute("SELECT 1");
            }
        }
    }

    protected String getId() {
        return "nuxeo";
    }

    @Override
    public void tearDown() throws SQLException {
        if (owner == null) {
            return;
        }
        try {
            tearDownDatabase(url);
        } finally {
            super.tearDown();
        }
    }

    protected void tearDownDatabase(String url) throws SQLException {
        Connection connection = DriverManager.getConnection(url, user, password);
        try {
            Statement st = connection.createStatement();
            try {
                String sql = "SHUTDOWN";
                log.trace(sql);
                st.execute(sql);
            } finally {
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
        return descriptor;
    }

    @Override
    public boolean supportsClustering() {
        return true;
    }

}
