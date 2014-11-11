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
        setProperty(DATABASE_PROPERTY, databaseName);
        setProperty(SERVER_PROPERTY, DEF_SERVER);
        setProperty(PORT_PROPERTY, DEF_PORT);
        setProperty(USER_PROPERTY, DEF_USER);
        setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
        // for sql directory tests
        setProperty(DRIVER_PROPERTY, DRIVER);
        String url = String.format("jdbc:postgresql://%s:%s/%s",
                System.getProperty(SERVER_PROPERTY),
                System.getProperty(PORT_PROPERTY),
                System.getProperty(DATABASE_PROPERTY));
        setProperty(URL_PROPERTY, url);
        setProperty(ID_TYPE_PROPERTY, DEF_ID_TYPE);
    }

    @Override
    public void setUp() throws Exception {
        Class.forName(DRIVER);
        setProperties();
        Connection connection = DriverManager.getConnection(
                System.getProperty(URL_PROPERTY),
                System.getProperty(USER_PROPERTY),
                System.getProperty(PASSWORD_PROPERTY));
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
        properties.put("ServerName", System.getProperty(SERVER_PROPERTY));
        properties.put("PortNumber", System.getProperty(PORT_PROPERTY));
        properties.put("DatabaseName", System.getProperty(DATABASE_PROPERTY));
        properties.put("User", System.getProperty(USER_PROPERTY));
        properties.put("Password", System.getProperty(PASSWORD_PROPERTY));
        descriptor.properties = properties;
        descriptor.fulltextAnalyzer = "french";
        descriptor.setPathOptimizationsEnabled(true);
        descriptor.setAclOptimizationsEnabled(true);
        descriptor.idType = System.getProperty(ID_TYPE_PROPERTY);
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
