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

import org.nuxeo.runtime.api.Framework;

/**
 * @author Florent Guillaume
 */
public class DatabaseMySQL extends DatabaseHelper {

    public static DatabaseHelper INSTANCE = new DatabaseMySQL();

    private static final String DEF_URL = "jdbc:mysql://localhost:3306/nuxeojunittests";

    private static final String DEF_USER = "nuxeo";

    private static final String DEF_PASSWORD = "nuxeo";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-mysql-contrib.xml";

    private static final String DRIVER = "com.mysql.jdbc.Driver";

    private void setProperties() {
        setProperty(URL_PROPERTY, DEF_URL);
        setProperty(USER_PROPERTY, DEF_USER);
        setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
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
        Connection connection = DriverManager.getConnection(Framework.getProperty(URL_PROPERTY),
                Framework.getProperty(USER_PROPERTY), Framework.getProperty(PASSWORD_PROPERTY));
        doOnAllTables(connection, null, null, "DROP TABLE `%s` CASCADE");
        connection.close();
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
    public int getRecursiveRemovalDepthLimit() {
        // Stupid MySQL limitations:
        // "Cascading operations may not be nested more than 15 levels deep."
        // "Currently, triggers are not activated by cascaded foreign key
        // actions."
        // Use a bit less that 15 to cater for complex properties
        return 13;
    }

    @Override
    public boolean supportsClustering() {
        return true;
    }

}
