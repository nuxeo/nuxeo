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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Florent Guillaume
 */
public class DatabaseMySQL extends DatabaseHelper {

    public static DatabaseHelper INSTANCE = new DatabaseMySQL();

    private static final Log log = LogFactory.getLog(DatabaseMySQL.class);

    // CREATE USER 'nuxeo' IDENTIFIED BY 'nuxeo';
    private static final String MYSQL_HOST = "localhost";

    private static final String MYSQL_PORT = "3306";

    private static final String MYSQL_SUPER_USER = "root";

    private static final String MYSQL_SUPER_PASSWORD = "";

    private static final String MYSQL_SUPER_DATABASE = "mysql";

    /* Constants mentioned in the ...-mysql-contrib.xml file: */

    private static final String MYSQL_DATABASE = "nuxeojunittests";

    private static final String MYSQL_DATABASE_OWNER = "nuxeo";

    private static final String MYSQL_DATABASE_PASSWORD = "nuxeo";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-mysql-contrib.xml";

    @Override
    public void setUp() throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        String url = String.format("jdbc:mysql://%s:%s/%s", MYSQL_HOST,
                MYSQL_PORT, MYSQL_SUPER_DATABASE);
        Connection connection = DriverManager.getConnection(url,
                MYSQL_SUPER_USER, MYSQL_SUPER_PASSWORD);
        Statement st = connection.createStatement();
        String sql;
        sql = String.format("DROP DATABASE IF EXISTS `%s`", MYSQL_DATABASE);
        log.debug(sql);
        st.execute(sql);
        sql = String.format("CREATE DATABASE `%s`", MYSQL_DATABASE);
        log.debug(sql);
        st.execute(sql);
        sql = String.format(
                "GRANT ALL PRIVILEGES ON `%s`.* TO '%s'@'localhost' IDENTIFIED BY '%s'",
                MYSQL_DATABASE, MYSQL_DATABASE_OWNER, MYSQL_DATABASE_PASSWORD);
        log.debug(sql);
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
        descriptor.xaDataSourceName = "com.mysql.jdbc.jdbc2.optional.MysqlXADataSource";
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("ServerName", MYSQL_HOST);
        properties.put("PortNumber", MYSQL_PORT);
        properties.put("DatabaseName", MYSQL_DATABASE);
        properties.put("User", MYSQL_DATABASE_OWNER);
        properties.put("Password", MYSQL_DATABASE_PASSWORD);
        descriptor.properties = properties;
        return descriptor;
    }

    @Override
    public void maybeSleepToNextSecond() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public int getRecursiveRemovalDepthLimit() {
        // Stupid MySQL limitations:
        // "Cascading operations may not be nested more than 15 levels deep."
        // "Currently, triggers are not activated by cascaded foreign key
        // actions."
        return 15;
    }
}
