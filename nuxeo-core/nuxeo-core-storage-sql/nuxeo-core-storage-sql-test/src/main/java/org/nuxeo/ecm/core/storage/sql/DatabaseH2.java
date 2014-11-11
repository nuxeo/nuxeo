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

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Florent Guillaume
 */
public class DatabaseH2 extends DatabaseHelper {

    public static final DatabaseHelper INSTANCE = new DatabaseH2();

    private static final Log log = LogFactory.getLog(DatabaseH2.class);

    /** This directory will be deleted and recreated. */
    protected static final String DIRECTORY = "target/test/h2";

    protected static final String DEF_USER = "sa";

    protected static final String DEF_PASSWORD = "";

    protected static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-h2-contrib.xml";

    protected String h2Path;

    protected String origUrl;

    protected String url;

    protected void setProperties() {
        url = String.format("jdbc:h2:%s/%s", h2Path, databaseName);
        origUrl = setProperty(URL_PROPERTY, url);

        Framework.getRuntime().getProperties().setProperty(REPOSITORY_PROPERTY, repositoryName);
        setProperty(DATABASE_PROPERTY, databaseName);
        setProperty(USER_PROPERTY, DEF_USER);
        setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
    }

    @Override
    public void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        File dir = new File(DIRECTORY);
        FileUtils.deleteTree(dir);
        dir.mkdirs();
        h2Path = new File(dir, getId()).getAbsolutePath();
        setProperties();
    }

    protected String getId() {
        return "nuxeo";
    }

    @Override
    public void tearDown() throws SQLException {
        if (origUrl == null) {
            System.clearProperty(URL_PROPERTY);
        } else {
            System.setProperty(URL_PROPERTY, origUrl);
        }
        Connection connection = DriverManager.getConnection(url,
                System.getProperty(USER_PROPERTY),
                System.getProperty(PASSWORD_PROPERTY));
        Statement st = connection.createStatement();
        String sql = "SHUTDOWN";
        log.trace(sql);
        st.execute(sql);
        st.close();
        connection.close();
    }

    @Override
    public String getDeploymentContrib() {
        return CONTRIB_XML;
    }

    @Override
    public String getPooledDeploymentContrib() {
        return "test-pooling-h2-contrib.xml";
    }

    @Override
    public RepositoryDescriptor getRepositoryDescriptor() {
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        descriptor.xaDataSourceName = "org.h2.jdbcx.JdbcDataSource";
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("URL", url);
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
