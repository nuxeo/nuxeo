/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
import org.nuxeo.runtime.api.Framework;

public class DatabaseDB2 extends DatabaseHelper {

    private static final Log log = LogFactory.getLog(DatabaseDB2.class);

    public static DatabaseHelper INSTANCE = new DatabaseDB2();

    private static final String DEF_SERVER = "localhost";

    private static final String DEF_PORT = "3700";

    // 8 chars max
    private static final String DEFAULT_DATABASE_NAME = "nuxeotst";

    private static final String DEF_USER = "db2inst1";

    private static final String DEF_PASSWORD = "db2inst1pw99";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-db2-contrib.xml";

    private static final String DRIVER = "com.ibm.db2.jcc.DB2Driver";

    private static final String XA_DATASOURCE = "com.ibm.db2.jcc.DB2XADataSource";

    protected void setProperties() {
        databaseName = DEFAULT_DATABASE_NAME;
        Framework.getProperties().setProperty(REPOSITORY_PROPERTY,
                repositoryName);
        setProperty(DATABASE_PROPERTY, databaseName);
        setProperty(SERVER_PROPERTY, DEF_SERVER);
        setProperty(PORT_PROPERTY, DEF_PORT);
        setProperty(USER_PROPERTY, DEF_USER);
        setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
        // for sql directory tests
        setProperty(DRIVER_PROPERTY, DRIVER);
        String url = String.format("jdbc:db2://%s:%s/%s",
                Framework.getProperty(SERVER_PROPERTY),
                Framework.getProperty(PORT_PROPERTY),
                Framework.getProperty(DATABASE_PROPERTY));
        setProperty(URL_PROPERTY, url);
    }

    @Override
    public void setUp() throws Exception {
        Class.forName(DRIVER);
        setProperties();
        Connection connection = DriverManager.getConnection(
                Framework.getProperty(URL_PROPERTY),
                Framework.getProperty(USER_PROPERTY),
                Framework.getProperty(PASSWORD_PROPERTY));
        doOnAllTables(connection, null,
                Framework.getProperty(USER_PROPERTY).toUpperCase(),
                "DROP TABLE \"%s\"");
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
        descriptor.xaDataSourceName = XA_DATASOURCE;
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
