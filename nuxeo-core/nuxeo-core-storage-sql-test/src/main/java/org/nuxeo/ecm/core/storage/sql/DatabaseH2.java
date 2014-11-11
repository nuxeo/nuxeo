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
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
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

    /** This property is mentioned in the ...-h2-contrib.xml file. */
    private static final String H2_PATH_PROPERTY = "nuxeo.test.h2.path";

    private static File h2TempDir;

    private static String h2Path;

    private static final boolean H2_DELETE_ON_TEARDOWN = true;

    private static final String H2_DATABASE_USER = "sa";

    private static final String H2_DATABASE_PASSWORD = "";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-h2-contrib.xml";

    @Override
    public void setUp() throws IOException {
        h2TempDir = File.createTempFile("nxsqltests-h2-", null);
        h2TempDir.delete();
        h2TempDir.mkdir();
        h2Path = new File(h2TempDir, "nuxeo").getAbsolutePath();
        // this property is mentioned in the ...-h2-contrib.xml file
        // it will be expanded by RepositoryImpl.getXADataSource
        System.setProperty(H2_PATH_PROPERTY, h2Path);
    }

    @Override
    public void tearDown() throws Exception {
        Connection connection = DriverManager.getConnection(String.format(
                "jdbc:h2:%s", h2Path), H2_DATABASE_USER, H2_DATABASE_PASSWORD);
        Statement st = connection.createStatement();
        String sql = "SHUTDOWN";
        log.trace(sql);
        st.execute(sql);
        st.close();
        connection.close();
        if (H2_DELETE_ON_TEARDOWN) {
            FileUtils.deleteTree(h2TempDir);
        }
        h2TempDir = null;
        h2Path = null;
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
        properties.put("URL", String.format("jdbc:h2:${%s}", H2_PATH_PROPERTY));
        properties.put("User", H2_DATABASE_USER);
        properties.put("Password", H2_DATABASE_PASSWORD);
        descriptor.properties = properties;
        return descriptor;
    }

}
