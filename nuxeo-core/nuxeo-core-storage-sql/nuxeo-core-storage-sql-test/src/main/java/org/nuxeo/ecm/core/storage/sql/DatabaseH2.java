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

/**
 * @author Florent Guillaume
 */
public class DatabaseH2 extends DatabaseHelper {

    public static final DatabaseHelper INSTANCE = new DatabaseH2();

    private static final Log log = LogFactory.getLog(DatabaseH2.class);

    /** This directory will be deleted and recreated. */
    private static final String DIRECTORY = "target/test/h2";

    private static final String DEF_USER = "sa";

    private static final String DEF_PASSWORD = "";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-h2-contrib.xml";

    private static String h2Path;

    private static String origUrl;

    private static void setProperties() {
        String url = String.format("jdbc:h2:%s", h2Path);
        origUrl = setProperty(URL_PROPERTY, url);
        setProperty(USER_PROPERTY, DEF_USER);
        setProperty(PASSWORD_PROPERTY, DEF_PASSWORD);
    }

    @Override
    public void setUp() throws Exception {
        Class.forName("org.h2.Driver");
        File dir = new File(DIRECTORY);
        FileUtils.deleteTree(dir);
        dir.mkdirs();
        h2Path = new File(dir, "nuxeo").getAbsolutePath();
        setProperties();
    }

    @Override
    public void tearDown() throws SQLException {
        String url = System.getProperty(URL_PROPERTY);
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
    public RepositoryDescriptor getRepositoryDescriptor() {
        RepositoryDescriptor descriptor = new RepositoryDescriptor();
        descriptor.xaDataSourceName = "org.h2.jdbcx.JdbcDataSource";
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
