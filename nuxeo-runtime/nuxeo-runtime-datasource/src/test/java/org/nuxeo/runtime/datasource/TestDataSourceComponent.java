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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.test.runner.ConditionalIgnoreRule;
import org.nuxeo.runtime.test.runner.ContainerFeature;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.RuntimeFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@Features({ ContainerFeature.class, RuntimeFeature.class})
public class TestDataSourceComponent {

    protected static final ClassLoader LOADER = TestDataSourceComponent.class.getClassLoader();

    private static final String DATASOURCE_CONTRIB = "org.nuxeo.runtime.datasource:datasource-contrib.xml";

    private static final String XADATASOURCE_CONTRIB = "org.nuxeo.runtime.datasource:xadatasource-contrib.xml";

    private static final String XADATASOURCE_PG_CONTRIB = "org.nuxeo.runtime.datasource:xadatasource-pg-contrib.xml";

    /** This directory will be deleted and recreated. */
    private static final String DIRECTORY = "target/test/h2";

    /** Property used in the datasource URL. */
    private static final String PROP_NAME = "ds.test.home";

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS";

    private static final String COUNT_SQL_PG = "SELECT COUNT(*) FROM PG_STAT_ACTIVITY";

    @Test
    public void testJNDIName() throws Exception {
        assertEquals("java:comp/env/jdbc/foo", DataSourceHelper.getDataSourceJNDIName("foo"));
    }

    protected static void checkDataSourceOk(String name, boolean autocommit) throws Exception {
        DataSource ds = DataSourceHelper.getDataSource(name);
        Connection conn = ds.getConnection();
        assertEquals(autocommit, conn.getAutoCommit());
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery("SELECT 123");
        assertNotNull(rs);
        assertTrue(rs.next());
        assertEquals(123, rs.getInt(1));
        st.close();
        conn.close();
    }

    @Test
    @LocalDeploy(DATASOURCE_CONTRIB)
    public void testNonXANoTM() throws Exception {
        checkDataSourceOk("foo", true);
        checkDataSourceOk("alias", true);
    }

    @Test
    @LocalDeploy(DATASOURCE_CONTRIB)
    public void testNonXA() throws Exception {
        checkDataSourceOk("foo", true);
        checkDataSourceOk("alias", true);
    }

    @Test
    @LocalDeploy(XADATASOURCE_CONTRIB)
    public void testXANoTx() throws Exception {
        checkDataSourceOk("foo", true);
    }

    @Test
    @LocalDeploy(XADATASOURCE_CONTRIB)
    public void testXA() throws Exception {
        TransactionHelper.startTransaction();
        try {
            checkDataSourceOk("foo", false);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    public static class NXP12086 implements ConditionalIgnoreRule.Condition {

        @Override
        public boolean shouldIgnore() {
            return true;
        }

    }

    @Test
    @Ignore
    // NXP12086
    @LocalDeploy(XADATASOURCE_CONTRIB)
    public void testXANoLeak() throws Exception {
        dotestXANoLeak(COUNT_SQL);
    }

    // PostgreSQL test, see pg XML contrib for connection parameters
    @Ignore
    @Test
    @LocalDeploy(XADATASOURCE_PG_CONTRIB)
    public void testXANoLeakPostgreSQL() throws Exception {
        dotestXANoLeak(COUNT_SQL_PG);
    }

    // without PatchedDataSourceXAConnectionFactory we leaked
    // connections on close (PoolableConnectionFactory.destroyObject)
    public void dotestXANoLeak(String countStatement) throws Exception {
        // in contrib, pool is configured with maxIdle = 1
        DataSource ds = DataSourceHelper.getDataSource("foo");
        Connection conn1 = ds.getConnection();
        int n = countPhysicalConnections(conn1, countStatement) - 1;
        Connection conn2 = ds.getConnection();
        assertEquals(n + 2, countPhysicalConnections(conn1, countStatement));
        Connection conn3 = ds.getConnection();
        assertEquals(n + 3, countPhysicalConnections(conn1, countStatement));

        conn3.close();
        // conn3 idle in pool, conn1+conn2 active
        assertEquals(n + 3, countPhysicalConnections(conn1, countStatement));
        conn2.close();
        // conn2 closed, conn3 idle in pool, conn1 active
        assertEquals(n + 2, countPhysicalConnections(conn1, countStatement));
        // conn1 closed, conn3 idle in pool
        conn1.close();

        Connection conn4 = ds.getConnection(); // reuses from pool
        assertEquals(n + 1, countPhysicalConnections(conn4, countStatement));
        conn4.close();

    }

    public int countPhysicalConnections(Connection conn, String statement) throws SQLException {
        Statement st = conn.createStatement();
        try {
            ResultSet rs = st.executeQuery(statement);
            rs.next();
            return rs.getInt(1);
        } finally {
            st.close();
        }
    }

}
