/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.runtime.datasource.PooledDataSourceRegistry.PooledDataSource;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.TransactionalConfig;
import org.nuxeo.runtime.test.runner.TransactionalFeature;
import org.nuxeo.runtime.transaction.TransactionHelper;

@RunWith(FeaturesRunner.class)
@TransactionalConfig(autoStart = false)
@Features(TransactionalFeature.class)
public class TestDataSourceComponent {

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS";

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
    @Deploy("org.nuxeo.runtime.datasource:datasource-contrib.xml")
    public void testNonXANoTM() throws Exception {
        checkDataSourceOk("foo", true);
        checkDataSourceOk("alias", true);
    }

    @Test
    @Deploy("org.nuxeo.runtime.datasource:datasource-contrib.xml")
    public void testNonXA() throws Exception {
        checkDataSourceOk("foo", true);
        checkDataSourceOk("alias", true);
    }

    @Test
    @Deploy("org.nuxeo.runtime.datasource:datasource-contrib.xml")
    public void testNonShared() throws Exception {
        PooledDataSource ds = (PooledDataSource) DataSourceHelper.getDataSource("foo");
        TransactionHelper.startTransaction();
        try (Connection c1 = ds.getConnection()) {
            int n1 = countPhysicalConnections(c1);
            try (Connection c2 = ds.getConnection(false)) {
                int n2 = countPhysicalConnections(c2);
                assertEquals(n1, n2);
            }
            try (Connection c2 = ds.getConnection(true)) {
                int n2 = countPhysicalConnections(c2);
                assertEquals(n1 + 1, n2);
            }
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    public int countPhysicalConnections(Connection conn) throws SQLException {
        Statement st = conn.createStatement();
        try {
            ResultSet rs = st.executeQuery(COUNT_SQL);
            rs.next();
            return rs.getInt(1);
        } finally {
            st.close();
        }
    }

    @Test
    @Deploy("org.nuxeo.runtime.datasource:xadatasource-contrib.xml")
    public void testXANoTx() throws Exception {
        checkDataSourceOk("foo", true);
    }

    @Test
    @Deploy("org.nuxeo.runtime.datasource:xadatasource-contrib.xml")
    public void testXA() throws Exception {
        TransactionHelper.startTransaction();
        try {
            checkDataSourceOk("foo", false);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

}
