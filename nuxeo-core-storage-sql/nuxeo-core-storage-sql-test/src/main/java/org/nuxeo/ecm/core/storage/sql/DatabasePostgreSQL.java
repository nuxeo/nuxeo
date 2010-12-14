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
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.nuxeo.runtime.api.Framework;

/**
 * @author Florent Guillaume
 */
public class DatabasePostgreSQL extends DatabaseHelper {

    public static DatabaseHelper INSTANCE = new DatabasePostgreSQL();

    private static final String DEF_SERVER = "localhost";

    private static final String DEF_PORT = "5432";

    private static final String DEF_USER = "nuxeo";

    private static final String DEF_PASSWORD = "nuxeo";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-postgresql-contrib.xml";

    protected void setProperties() {
        Properties properties = Framework.getProperties();
        properties.setProperty(REPOSITORY_PROPERTY, repositoryName);
        setProperty(DATABASE_PROPERTY, databaseName);
        setProperty(SERVER_PROPERTY, DEF_SERVER);
        setProperty(PORT_PROPERTY, DEF_PORT);
        setProperty(USER_PROPERTY, DEF_USER);
        setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
    }

    @Override
    public void setUp() throws Exception {
        Class.forName("org.postgresql.Driver");
        setProperties();
        String url = String.format("jdbc:postgresql://%s:%s/%s",
                System.getProperty(SERVER_PROPERTY),
                System.getProperty(PORT_PROPERTY),
                System.getProperty(DATABASE_PROPERTY));
        Connection connection = DriverManager.getConnection(url,
                System.getProperty(USER_PROPERTY),
                System.getProperty(PASSWORD_PROPERTY));
        try {
            doOnAllTables(connection, null, "public", "DROP TABLE \"%s\" CASCADE");
        } finally {
            connection.close();
        }
    }

    @Override
    public String getDeploymentContrib() {
        return CONTRIB_XML;
    }

    @Override
    public String getPooledDeploymentContrib() {
        return "test-pooling-postgres-contrib.xml";
    }

    @Override
    public RepositoryDescriptor getRepositoryDescriptor() {
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        descriptor.xaDataSourceName = "org.postgresql.xa.PGXADataSource";
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("ServerName", System.getProperty(SERVER_PROPERTY));
        properties.put("PortNumber", System.getProperty(PORT_PROPERTY));
        properties.put("DatabaseName", databaseName);
        properties.put("User", System.getProperty(USER_PROPERTY));
        properties.put("Password", System.getProperty(PASSWORD_PROPERTY));
        descriptor.properties = properties;
        descriptor.fulltextAnalyzer = "french";
        descriptor.pathOptimizationsEnabled = true;
        descriptor.aclOptimizationsEnabled = true;
        return descriptor;
    }

    @Override
    public boolean supportsClustering() {
        return true;
    }

}
