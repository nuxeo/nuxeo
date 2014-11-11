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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Florent Guillaume
 */
public class DatabaseOracle extends DatabaseHelper {

    private static final Log log = LogFactory.getLog(DatabaseOracle.class);

    public static DatabaseHelper INSTANCE = new DatabaseOracle();

    private static final String DEF_SERVER = "localhost";

    private static final String DEF_URL = "jdbc:oracle:thin:@" + DEF_SERVER
            + ":1521:XE";

    private static final String DEF_USER = "nuxeo";

    private static final String DEF_PASSWORD = "nuxeo";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-oracle-contrib.xml";

    private static final String DRIVER = "oracle.jdbc.driver.OracleDriver";

    private static void setProperties() {
        setProperty(URL_PROPERTY, DEF_URL);
        setProperty(USER_PROPERTY, DEF_USER);
        setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
        setProperty(DRIVER_PROPERTY, DRIVER);
    }

    @Override
    public void setUp() throws Exception {
        Class.forName(DRIVER);
        setProperties();
        Connection connection = DriverManager.getConnection(
                System.getProperty(URL_PROPERTY),
                System.getProperty(USER_PROPERTY),
                System.getProperty(PASSWORD_PROPERTY));
        doOnAllTables(connection, null,
                System.getProperty(USER_PROPERTY).toUpperCase(),
                "DROP TABLE \"%s\" CASCADE CONSTRAINTS PURGE");
        dropSequences(connection);
        connection.close();
    }

    public void dropSequences(Connection connection) throws Exception {
        List<String> sequenceNames = new ArrayList<String>();
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery("SELECT SEQUENCE_NAME FROM USER_SEQUENCES");
        while (rs.next()) {
            String sequenceName = rs.getString(1);
            if (sequenceName.indexOf('$') != -1) {
                continue;
            }
            sequenceNames.add(sequenceName);
        }
        rs.close();
        for (String sequenceName : sequenceNames) {
            String sql = String.format("DROP SEQUENCE \"%s\"", sequenceName);
            log.trace("SQL: " + sql);
            st.execute(sql);
        }
        st.close();
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
