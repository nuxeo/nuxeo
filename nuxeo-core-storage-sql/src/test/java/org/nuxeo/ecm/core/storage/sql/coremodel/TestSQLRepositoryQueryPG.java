/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.core.storage.sql.coremodel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.nuxeo.ecm.core.query.test.QueryTestCase;

/**
 * This testcase should be run by hand with suitable PG_* constants.
 *
 * @author Florent Guillaume
 */
public class TestSQLRepositoryQueryPG extends QueryTestCase {

    public static final String PG_HOST = "localhost";

    public static final Integer PG_PORT = Integer.valueOf(5432);

    /** User that can create and drop databases. */
    public static final String PG_USER = "postgres";

    /** Users's password. */
    public static final String PG_PASSWORD = "";

    /** Database to connect to to issue CREATE DATABASE commands. */
    public static final String PG_DATABASE = "postgres";

    /*
     * The following constants are mentioned in the ...pg-contrib.xml file
     */

    /** The database where tests take place. */
    public static final String DATABASE_NAME = "nuxeojunittests";

    public static final String DATABASE_OWNER = "nuxeo";

    protected Connection getConnection() throws ClassNotFoundException,
            SQLException {
        Class.forName("org.postgresql.Driver");
        String url = String.format("jdbc:postgresql://%s:%d/%s", PG_HOST,
                PG_PORT, PG_DATABASE);
        return DriverManager.getConnection(url, PG_USER, PG_PASSWORD);
    }

    @Override
    public void deployRepository() throws Exception {
        Connection baseConnection = getConnection();
        Statement st = baseConnection.createStatement();
        String sql;
        sql = String.format("DROP DATABASE IF EXISTS \"%s\"", DATABASE_NAME);
        st.execute(sql);
        sql = String.format("CREATE DATABASE \"%s\" OWNER \"%s\"",
                DATABASE_NAME, DATABASE_OWNER);
        st.execute(sql);
        st.close();
        baseConnection.close();
        deployRepositoryContribs();
    }

    protected void deployRepositoryContribs() throws Exception {
        deployContrib("org.nuxeo.ecm.core.storage.sql.tests",
                "OSGI-INF/test-repo-repository-pg-contrib.xml");
    }

    @Override
    public void undeployRepository() throws Exception {
    }

}
