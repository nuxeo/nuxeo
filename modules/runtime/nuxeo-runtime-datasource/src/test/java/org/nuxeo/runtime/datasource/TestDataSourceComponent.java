/*
 * (C) Copyright 2009-2020 Nuxeo (http://nuxeo.com/) and others.
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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
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

    @Test
    public void testJNDIName() {
        assertEquals("java:comp/env/jdbc/foo", DataSourceHelper.getDataSourceJNDIName("foo"));
    }

    protected static void checkDataSourceOk(String name, boolean autocommit) throws NamingException, SQLException {
        DataSource ds = DataSourceHelper.getDataSource(name);
        try (Connection conn = ds.getConnection(); //
                Statement st = conn.createStatement(); //
                ResultSet rs = st.executeQuery("SELECT 123")) {
            assertEquals(autocommit, conn.getAutoCommit());
            assertNotNull(rs);
            assertTrue(rs.next());
            assertEquals(123, rs.getInt(1));
        }
    }

    @Test
    @Deploy("org.nuxeo.runtime.datasource:datasource-contrib.xml")
    public void testNonXA() throws NamingException, SQLException {
        checkDataSourceOk("foo", true);
        checkDataSourceOk("alias", true);
    }

    @Test
    @Deploy("org.nuxeo.runtime.datasource:datasource-contrib.xml")
    public void testNonShared() throws NamingException, SQLException {
        DataSource ds = DataSourceHelper.getDataSource("foo");
        DataSource dsNoSharing = DataSourceHelper.getDataSource("foo", true);
        TransactionHelper.startTransaction();
        try (Connection c1 = ds.getConnection(); //
                Connection c2 = ds.getConnection(); //
                Connection c3 = dsNoSharing.getConnection(); //
                Connection c4 = dsNoSharing.getConnection()) {
            int s1 = getSessionId(c1);
            int s2 = getSessionId(c2);
            int s3 = getSessionId(c3);
            int s4 = getSessionId(c4);
            assertEquals(s1, s2); // sharing
            assertNotEquals(s1, s3); // no sharing
            assertNotEquals(s1, s4); // no sharing
            assertNotEquals(s3, s4); // no sharing
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    public int getSessionId(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement(); //
                ResultSet rs = st.executeQuery("SELECT SESSION_ID()")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    @Deploy("org.nuxeo.runtime.datasource:xadatasource-contrib.xml")
    public void testXANoTx() throws NamingException, SQLException {
        checkDataSourceOk("foo", true);
    }

    @Test
    @Deploy("org.nuxeo.runtime.datasource:xadatasource-contrib.xml")
    public void testXA() throws NamingException, SQLException {
        TransactionHelper.startTransaction();
        try {
            checkDataSourceOk("foo", false);
        } finally {
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    @Test
    @Deploy("org.nuxeo.runtime.datasource:datasource-contrib.xml")
    public void testLeak() throws SQLException {
        doTestLeak(false);
    }

    @Test
    @Deploy("org.nuxeo.runtime.datasource:datasource-contrib.xml")
    public void testLeakWithSetRollbackOnlyBeforeOpen() throws SQLException {
        // this test is crucial, even in non-XA mode if the transaction
        // is in rollback-only mode, we must still be able to register a Synchronization
        // in DBCP TransactionContext.addTransactionContextListener
        // so that connection cleanup can be done by ManagedConnection.CompletionListener
        doTestLeak(true);
    }

    @Test
    @Deploy("org.nuxeo.runtime.datasource:xadatasource-contrib.xml")
    public void testLeakXA() throws SQLException {
        doTestLeak(false);
    }

    @Test
    @Deploy("org.nuxeo.runtime.datasource:xadatasource-contrib.xml")
    public void testLeakXAWithSetRollbackOnlyBeforeOpen() throws SQLException {
        doTestLeak(true);
    }

    protected void doTestLeak(boolean setRollbackOnlyBeforeOpen) throws SQLException {
        TransactionHelper.startTransaction();
        for (int i = 1; i < 100; i++) {
            if (setRollbackOnlyBeforeOpen) {
                TransactionHelper.setTransactionRollbackOnly();
            }
            try (Connection connection = ConnectionHelper.getConnection("foo")) {
                // nothing, just open then close
            }
            TransactionHelper.commitOrRollbackTransaction();
            TransactionHelper.startTransaction();
        }
    }

}
