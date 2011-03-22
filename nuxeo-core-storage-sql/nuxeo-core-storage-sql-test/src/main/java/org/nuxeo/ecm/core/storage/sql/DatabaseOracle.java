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
import java.util.HashMap;
import java.util.Map;

/**
 * @author Florent Guillaume
 */
public class DatabaseOracle extends DatabaseHelper {

    public static DatabaseHelper INSTANCE = new DatabaseOracle();

    private static final String DEF_URL = "jdbc:oracle:thin:@localhost:1521:XE";

    private static final String DEF_USER = "nuxeo";

    private static final String DEF_PASSWORD = "nuxeo";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-oracle-contrib.xml";

    private static void setProperties() {
        setProperty(URL_PROPERTY, DEF_URL);
        setProperty(USER_PROPERTY, DEF_USER);
        setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
    }

    @Override
    public void setUp() throws Exception {
        Class.forName("oracle.jdbc.driver.OracleDriver");
        setProperties();
        Connection connection = DriverManager.getConnection(
                System.getProperty(URL_PROPERTY),
                System.getProperty(USER_PROPERTY),
                System.getProperty(PASSWORD_PROPERTY));
        doOnAllTables(connection, null,
                System.getProperty(USER_PROPERTY).toUpperCase(),
                "DROP TABLE \"%s\" CASCADE CONSTRAINTS PURGE");
        connection.close();
    }

    @Override
    public String getDeploymentContrib() {
        return CONTRIB_XML;
    }

    @Override
    public RepositoryDescriptor getRepositoryDescriptor() {
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        descriptor.xaDataSourceName = "oracle.jdbc.xa.client.OracleXADataSource";
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("URL", System.getProperty(URL_PROPERTY));
        properties.put("User", System.getProperty(USER_PROPERTY));
        properties.put("Password", System.getProperty(PASSWORD_PROPERTY));
        descriptor.properties = properties;
        return descriptor;
    }

    @Override
    public boolean supportsClustering() {
        return true;
    }

}
