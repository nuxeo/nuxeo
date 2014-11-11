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
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

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

    private static final String DRIVER = "org.postgresql.Driver";

    protected void setProperties() {
        Framework.getProperties().setProperty(REPOSITORY_PROPERTY, repositoryName);
        String db = setProperty(DATABASE_PROPERTY, databaseName);
        String server = setProperty(SERVER_PROPERTY, DEF_SERVER);
        String port = setProperty(PORT_PROPERTY, DEF_PORT);
        String user = setProperty(USER_PROPERTY, DEF_USER);
        String password = setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
        // for sql directory tests
        String driver = setProperty(DRIVER_PROPERTY, DRIVER);
        String url = String.format("jdbc:postgresql://%s:%s/%s",
                server,
                port,
                db);
        setProperty(URL_PROPERTY, url);
        setProperty(ID_TYPE_PROPERTY, DEF_ID_TYPE);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Class.forName(DRIVER);
        setProperties();
        Connection connection = DriverManager.getConnection(
                Framework.getProperty(URL_PROPERTY),
                Framework.getProperty(USER_PROPERTY),
                Framework.getProperty(PASSWORD_PROPERTY));
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
        descriptor.xaDataSourceName = "org.postgresql.xa.PGXADataSource";
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("ServerName", Framework.getProperty(SERVER_PROPERTY));
        properties.put("PortNumber", Framework.getProperty(PORT_PROPERTY));
        properties.put("DatabaseName", Framework.getProperty(DATABASE_PROPERTY));
        properties.put("User", Framework.getProperty(USER_PROPERTY));
        properties.put("Password", Framework.getProperty(PASSWORD_PROPERTY));
        descriptor.properties = properties;
        descriptor.fulltextAnalyzer = "french";
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
