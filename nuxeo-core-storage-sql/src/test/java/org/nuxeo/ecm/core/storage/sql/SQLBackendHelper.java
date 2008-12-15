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

package org.nuxeo.ecm.core.storage.sql;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;

/**
 * Helper to set up and tear down a test database.
 * <p>
 * This can be used also to use another test database than Derby, for instance
 * PostgreSQL.
 *
 * @author Florent Guillaume
 */
public class SQLBackendHelper {

    private static final Log log = LogFactory.getLog(SQLBackendHelper.class);

    protected static enum Database {
        DERBY, //
        H2, //
        MYSQL, //
        POSTGRESQL
    }

    /**
     * Change this to use another SQL database for tests.
     */
    public static final Database DATABASE = Database.H2;

    protected static String REPOSITORY_NAME = "test";

    /*
     * ----- Derby configuration -----
     */

    /* Constant mentioned in the ...-derby-contrib.xml file: */

    /** This directory will be deleted and recreated. */
    protected static final String DERBY_DIRECTORY = "target/test/derby";

    protected static final String DERBY_LOG = "target/test/derby.log";

    /*
     * ----- H2 configuration -----
     */

    /* Constants mentioned in the ...-h2-contrib.xml file: */

    /** This directory will be deleted and recreated. */
    protected static final String H2_PATH = "/tmp/nxsqltests-h2/nuxeo";

    protected static final String H2_DATABASE_USER = "sa";

    protected static final String H2_DATABASE_PASSWORD = "";

    /*
     * ----- MySQL configuration -----
     */

    // CREATE USER 'nuxeo' IDENTIFIED BY 'nuxeo';
    protected static final String MYSQL_HOST = "localhost";

    protected static final String MYSQL_PORT = "3306";

    protected static final String MYSQL_SUPER_USER = "root";

    protected static final String MYSQL_SUPER_PASSWORD = "";

    protected static final String MYSQL_SUPER_DATABASE = "mysql";

    /* Constants mentioned in the ...-mysql-contrib.xml file: */

    public static final String MYSQL_DATABASE = "nuxeojunittests";

    public static final String MYSQL_DATABASE_OWNER = "nuxeo";

    public static final String MYSQL_DATABASE_PASSWORD = "nuxeo";

    /*
     * ----- PostgreSQL configuration -----
     */

    protected static final String PG_HOST = "localhost";

    protected static final String PG_PORT = "5432";

    /** Superuser that can create and drop databases. */
    protected static final String PG_SUPER_USER = "postgres";

    /** Superusers's password. */
    protected static final String PG_SUPER_PASSWORD = "";

    /** Database to connect to to issue CREATE DATABASE commands. */
    protected static final String PG_SUPER_DATABASE = "postgres";

    /* Constants mentioned in the ...-postgresql-contrib.xml file: */

    /** The name of the database where tests take place. */
    public static final String PG_DATABASE = "nuxeojunittests";

    /** The owner of the database where tests take place. */
    public static final String PG_DATABASE_OWNER = "nuxeo";

    /** The password of the {@link #PG_DATABASE_OWNER} user. */
    public static final String PG_DATABASE_PASSWORD = "nuxeo";

    /*
     * ----- API -----
     */

    /**
     * Deploy the repository, returns an array of deployment contribs to do,
     * with two elements per contrib, first is the bundle, and second is the
     * filename.
     */
    public static void setUpRepository() throws Exception {
        switch (DATABASE) {
        case DERBY:
            setUpRepositoryDerby();
            return;
        case H2:
            setUpRepositoryH2();
            return;
        case MYSQL:
            setUpRepositoryMySQL();
            return;
        case POSTGRESQL:
            setUpRepositoryPostgreSQL();
            return;
        }
        throw new RuntimeException(); // not reached
    }

    public static void tearDownRepository() throws Exception {
        switch (DATABASE) {
        case DERBY:
            tearDownRepositoryDerby();
            return;
        case H2:
            tearDownRepositoryH2();
            return;
        case MYSQL:
            tearDownRepositoryMySQL();
            return;
        case POSTGRESQL:
            tearDownRepositoryPostgreSQL();
            return;
        }
        throw new RuntimeException(); // not reached
    }

    /*
     * ----- Derby -----
     */

    protected static void setUpRepositoryDerby() {
        File dbdir = new File(DERBY_DIRECTORY);
        File parent = dbdir.getParentFile();
        FileUtils.deleteTree(dbdir);
        parent.mkdirs();
        System.setProperty("derby.stream.error.file",
                new File(DERBY_LOG).getAbsolutePath());
        // the following noticeably improves performance
        System.setProperty("derby.system.durability", "test");
    }

    protected static void tearDownRepositoryDerby() throws Exception {
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException e) {
            if ("Derby system shutdown.".equals(e.getMessage())) {
                return;
            }
        }
        throw new RuntimeException("Expected Derby shutdown exception");
    }

    /*
     * ----- H2 -----
     */

    protected static void setUpRepositoryH2() {
        File parent = new File(H2_PATH).getParentFile();
        FileUtils.deleteTree(parent);
        parent.mkdirs();
    }

    protected static void tearDownRepositoryH2() throws Exception {
        Connection connection = DriverManager.getConnection(String.format(
                "jdbc:h2:%s", H2_PATH), H2_DATABASE_USER, H2_DATABASE_PASSWORD);
        Statement st = connection.createStatement();
        String sql = "SHUTDOWN";
        log.debug(sql);
        st.execute(sql);
        st.close();
        // no connection.close() as everything was shutdown
    }

    /*
     * ----- MySQL -----
     */

    protected static void setUpRepositoryMySQL() throws Exception {
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

    protected static void tearDownRepositoryMySQL() throws Exception {
    }

    /*
     * ----- PostgreSQL -----
     */

    protected static void setUpRepositoryPostgreSQL() throws Exception {
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

    protected static void tearDownRepositoryPostgreSQL() {
    }

}
