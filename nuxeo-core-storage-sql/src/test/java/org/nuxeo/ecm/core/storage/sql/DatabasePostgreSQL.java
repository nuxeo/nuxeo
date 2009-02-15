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
public class DatabasePostgreSQL extends DatabaseHelper {

    public static DatabaseHelper INSTANCE = new DatabasePostgreSQL();

    private static final Log log = LogFactory.getLog(DatabasePostgreSQL.class);

    private static final String PG_HOST = "localhost";

    private static final String PG_PORT = "5432";

    /** Superuser that can create and drop databases. */
    private static final String PG_SUPER_USER = "postgres";

    /** Superusers's password. */
    private static final String PG_SUPER_PASSWORD = "";

    /** Database to connect to to issue CREATE DATABASE commands. */
    private static final String PG_SUPER_DATABASE = "postgres";

    /* Constants mentioned in the ...-postgresql-contrib.xml file: */

    /** The name of the database where tests take place. */
    private static final String PG_DATABASE = "nuxeojunittests";

    /** The owner of the database where tests take place. */
    private static final String PG_DATABASE_OWNER = "nuxeo";

    /** The password of the {@link #PG_DATABASE_OWNER} user. */
    private static final String PG_DATABASE_PASSWORD = "nuxeo";

    private static final String CONTRIB_XML = "OSGI-INF/test-repo-repository-postgresql-contrib.xml";

    @Override
    public void setUp() throws Exception {
        Class.forName("org.postgresql.Driver");
        String url = String.format("jdbc:postgresql://%s:%s/%s", PG_HOST,
                PG_PORT, PG_SUPER_DATABASE);
        Connection connection = DriverManager.getConnection(url, PG_SUPER_USER,
                PG_SUPER_PASSWORD);
        Statement st = connection.createStatement();
        String sql = String.format("DROP DATABASE IF EXISTS \"%s\"",
                PG_DATABASE);
        log.debug(sql);
        st.execute(sql);
        sql = String.format("CREATE DATABASE \"%s\" OWNER \"%s\"", PG_DATABASE,
                PG_DATABASE_OWNER);
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
        descriptor.xaDataSourceName = "org.postgresql.xa.PGXADataSource";
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("ServerName", PG_HOST);
        properties.put("PortNumber", PG_PORT);
        properties.put("DatabaseName", PG_DATABASE);
        properties.put("User", PG_DATABASE_OWNER);
        properties.put("Password", PG_DATABASE_PASSWORD);
        descriptor.properties = properties;
        descriptor.fulltextAnalyzer = "french";
        return descriptor;
    }

}
