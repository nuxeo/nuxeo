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
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Florent Guillaume
 */
public class DatabaseOracle extends DatabaseHelper {

    public static DatabaseHelper INSTANCE = new DatabaseOracle();

    private static final Log log = LogFactory.getLog(DatabaseOracle.class);

    // ----- change this to test on a different Oracle instance -----

    private static final String URL = "jdbc:oracle:thin:@172.16.245.133:1521:XE";

    private static final String USER = "NUXEO";

    private static final String PASSWORD = "NUXEO";

    // -----

    private static final String URL_PROPERTY = "nuxeo.test.oracle.url";

    private static final String USER_PROPERTY = "nuxeo.test.oracle.user";

    private static final String PASSWORD_PROPERTY = "nuxeo.test.oracle.password";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-oracle-contrib.xml";

    @Override
    public void setUp() throws Exception {
        Class.forName("oracle.jdbc.driver.OracleDriver");
        Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
        DatabaseMetaData metadata = connection.getMetaData();
        List<String> tableNames = new LinkedList<String>();
        ResultSet rs = metadata.getTables(null, USER, "%",
                new String[] { "TABLE" });
        while (rs.next()) {
            String tableName = rs.getString("TABLE_NAME");
            if (tableName.indexOf('$') != -1) {
                // skip Oracle 10g flashback tables
                continue;
            }
            tableNames.add(tableName);
        }
        // remove hierarchy last because of foreign keys
        if (tableNames.remove("HIERARCHY")) {
            tableNames.add("HIERARCHY");
        }
        Statement st = connection.createStatement();
        for (String tableName : tableNames) {
            String sql = String.format("DROP TABLE \"%s\"", tableName);
            log.debug(sql);
            st.execute(sql);
        }
        st.close();
        connection.close();
        System.setProperty(URL_PROPERTY, URL);
        System.setProperty(USER_PROPERTY, USER);
        System.setProperty(PASSWORD_PROPERTY, PASSWORD);
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
        properties.put("URL", URL);
        properties.put("User", USER);
        properties.put("Password", PASSWORD);
        descriptor.properties = properties;
        // descriptor.fulltextAnalyzer = "french";
        // descriptor.fulltextCatalog = "nuxeo";
        return descriptor;
    }

}
