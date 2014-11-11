/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime.datasource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.runtime.api.DataSourceHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.test.NXRuntimeTestCase;
import org.nuxeo.runtime.transaction.TransactionHelper;

public class TestDataSourceComponent extends NXRuntimeTestCase {

    private static final String TEST_BUNDLE = "org.nuxeo.runtime.datasource.tests";

    private static final String DATASOURCE_CONTRIB = "OSGI-INF/datasource-contrib.xml";

    private static final String XADATASOURCE_CONTRIB = "OSGI-INF/xadatasource-contrib.xml";

    private static final String XADATASOURCE_PG_CONTRIB = "OSGI-INF/xadatasource-pg-contrib.xml";

    /** This directory will be deleted and recreated. */
    private static final String DIRECTORY = "target/test/h2";

    /** Property used in the datasource URL. */
    private static final String PROP_NAME = "ds.test.home";

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS";

    private static final String COUNT_SQL_PG = "SELECT COUNT(*) FROM PG_STAT_ACTIVITY";

    private String countPhysicalConnectionsSql;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        NuxeoContainer.installNaming();
        File dir = new File(DIRECTORY);
        FileUtils.deleteQuietly(dir);
        dir.mkdirs();
        Framework.getProperties().put(PROP_NAME, dir.getPath());

        deployBundle("org.nuxeo.runtime.datasource");
    }

    @Override
    @After
    public void tearDown() throws Exception {
        NuxeoContainer.uninstallNaming();
        super.tearDown();
    }

    @Test
    public void testJNDIName() throws Exception {
        assertEquals("java:comp/env/jdbc/foo",
                DataSourceHelper.getDataSourceJNDIName("foo"));
    }

    protected static void checkDataSourceOk() throws Exception {
        DataSource ds = DataSourceHelper.getDataSource("foo");
        Connection conn = ds.getConnection();
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT 123");
        assertNotNull(rs);
        assertTrue(rs.next());
        assertEquals(123, rs.getInt(1));
        st.close();
        conn.close();
    }

    @Test
    public void testNonXANoTM() throws Exception {
        deployContrib(TEST_BUNDLE, DATASOURCE_CONTRIB);
        checkDataSourceOk();
        undeployContrib(TEST_BUNDLE, DATASOURCE_CONTRIB);
    }

    @Test
    public void testNonXA() throws Exception {
        NuxeoContainer.install(null);
        deployContrib(TEST_BUNDLE, DATASOURCE_CONTRIB);
        checkDataSourceOk();
        undeployContrib(TEST_BUNDLE, DATASOURCE_CONTRIB);
    }

    @Test
    public void testXANoTM() throws Exception {
        deployContrib(TEST_BUNDLE, XADATASOURCE_CONTRIB);
        DataSource ds = DataSourceHelper.getDataSource("foo");
        try {
            ds.getConnection();
            fail("Should fail for XA with no TM");
        } catch (RuntimeException e) {
            Throwable t = e.getCause();
            String m = t == null ? e.getMessage() : t.getMessage();
            assertEquals("TransactionManager not found in JNDI", m);
        }
        undeployContrib(TEST_BUNDLE, XADATASOURCE_CONTRIB);
    }

    @Test
    public void testXA() throws Exception {
        NuxeoContainer.install(null);
        deployContrib(TEST_BUNDLE, XADATASOURCE_CONTRIB);
        TransactionHelper.startTransaction();
        try {
            checkDataSourceOk();
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
        undeployContrib(TEST_BUNDLE, XADATASOURCE_CONTRIB);
    }

    @Test
    public void testXANoLeak() throws Exception {
        countPhysicalConnectionsSql = COUNT_SQL;
        dotestXANoLeak(XADATASOURCE_CONTRIB);
    }

    // PostgreSQL test, see pg XML contrib for connection parameters
    @Ignore
    @Test
    public void testXANoLeakPostgreSQL() throws Exception {
        countPhysicalConnectionsSql = COUNT_SQL_PG;
        // not absolute as there may be pre-existing connections
        dotestXANoLeak(XADATASOURCE_PG_CONTRIB);
    }

    // without PatchedDataSourceXAConnectionFactory we leaked
    // connections on close (PoolableConnectionFactory.destroyObject)
    public void dotestXANoLeak(String contrib) throws Exception {
        NuxeoContainer.install(null);
        deployContrib(TEST_BUNDLE, contrib);
        // in contrib, pool is configured with maxIdle = 1
        DataSource ds = DataSourceHelper.getDataSource("foo");
        Connection conn1 = ds.getConnection();
        int n = countPhysicalConnections(conn1) - 1;
        Connection conn2 = ds.getConnection();
        assertEquals(n + 2, countPhysicalConnections(conn1));
        Connection conn3 = ds.getConnection();
        assertEquals(n + 3, countPhysicalConnections(conn1));

        conn3.close();
        // conn3 idle in pool, conn1+conn2 active
        assertEquals(n + 3, countPhysicalConnections(conn1));
        conn2.close();
        // conn2 closed, conn3 idle in pool, conn1 active
        assertEquals(n + 2, countPhysicalConnections(conn1));
        // conn1 closed, conn3 idle in pool
        conn1.close();

        Connection conn4 = ds.getConnection(); // reuses from pool
        assertEquals(n + 1, countPhysicalConnections(conn4));
        conn4.close();

        undeployContrib(TEST_BUNDLE, contrib);
    }

    public int countPhysicalConnections(Connection conn) throws SQLException {
        Statement st = conn.createStatement();
        try {
            ResultSet rs = st.executeQuery(countPhysicalConnectionsSql);
            rs.next();
            return rs.getInt(1);
        } finally {
            st.close();
        }
    }

}
