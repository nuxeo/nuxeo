/*
 * (C) Copyright 2013-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ecm.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.inject.Inject;
import javax.transaction.Transaction;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCConnection;
import org.nuxeo.ecm.core.storage.sql.jdbc.XAResourceConnectionAdapter;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.StorageConfiguration;
import org.nuxeo.runtime.datasource.ConnectionHelper;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Test that transaction management does the right thing with the connection.
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestConnectionManagement {

    @Inject
    protected CoreFeature coreFeature;

    /**
     * H2 cannot have one connection doing an insert in a tx and another using the same table, as it waits for a lock.
     */
    protected boolean canUseTwoConnections() {
        StorageConfiguration database = coreFeature.getStorageConfiguration();
        return !(database.isVCSH2() || database.isVCSDerby());
    }

    protected String getValidationQuery(Connection connection) {
        return Dialect.createDialect(connection, null).getValidationQuery();
    }

    protected static void assertEqualsInt(int expected, ResultSet rs) throws SQLException {
        assertTrue(rs.next());
        int actual = rs.getInt(1);
        assertEquals(expected, actual);
        assertFalse(rs.next());
        rs.close();
    }

    protected Connection getConnection() throws SQLException {
        String repositoryName = coreFeature.getRepositoryName();
        return ConnectionHelper.getConnection(JDBCConnection.getDataSourceName(repositoryName));
    }

    protected Connection getConnectionNoSharing() throws SQLException {
        String repositoryName = coreFeature.getRepositoryName();
        return ConnectionHelper.getConnection(JDBCConnection.getDataSourceName(repositoryName), true);
    }

    @Test
    public void testNoTxNoBegin() throws Exception {
        TransactionHelper.commitOrRollbackTransaction(); // end tx
        try (Connection connection = getConnection()) {
            assertTrue(connection.getAutoCommit());
            connection.setAutoCommit(true); // already true, no effect
            connection.setAutoCommit(false);
            connection.setAutoCommit(true);
            try (Statement st = connection.createStatement()) {
                String sql = getValidationQuery(connection);
                st.execute(sql);
            }
        }
    }

    @Test
    public void testNoTxBegin() throws Exception {
        TransactionHelper.commitOrRollbackTransaction(); // end tx
        try (Connection connection = getConnection()) {
            // first thing set autoCommit=false, but no tx
            connection.setAutoCommit(false);
            connection.setAutoCommit(false); // already false, no effect
            try (Statement st = connection.createStatement()) {
                String sql = getValidationQuery(connection);
                st.execute(sql);
            }
            connection.commit(); // needed for DB2 before close
            connection.setAutoCommit(true);
        }
    }

    /*
     * Transaction in non-ACTIVE state.
     */
    @Test
    public void testBadTxBegin() throws Exception {
        TransactionHelper.setTransactionRollbackOnly(); // not ACTIVE
        try (Connection connection = getConnection()) {
            // first thing set autoCommit=false, but no tx
            connection.setAutoCommit(false);
            connection.setAutoCommit(false); // already false, no effect
            try (Statement st = connection.createStatement()) {
                String sql = getValidationQuery(connection);
                st.execute(sql);
            }
            connection.commit(); // needed for DB2 before close
        }
    }

    @Test
    public void testNoTxSwitchAutoCommit() throws Exception {
        TransactionHelper.commitOrRollbackTransaction(); // end tx
        try (Connection connection = getConnection()) {
            // use connection with autoCommit=true
            connection.createStatement();
            // then set autoCommit=false, but no tx
            connection.setAutoCommit(false);
            connection.setAutoCommit(false); // already false, no effect
            try (Statement st = connection.createStatement()) {
                String sql = getValidationQuery(connection);
                st.execute(sql);
            }
            connection.commit(); // needed for DB2 before close
            connection.setAutoCommit(true);
        }
    }

    @Test
    public void testNoBegin() throws Exception {
        try (Connection connection = getConnection(); //
                Statement st = connection.createStatement()) {
            String sql = getValidationQuery(connection);
            st.execute(sql);
        }
    }

    /*
     * Does begin as the first thing the connection does.
     */
    @Test
    public void testManualBegin1() throws Exception {
        try (Connection connection = getConnection()) {
            // first thing set autoCommit=false
            connection.setAutoCommit(false);
            try (Statement st = connection.createStatement()) {
                String sql = getValidationQuery(connection);
                st.execute(sql);
            }
            connection.commit();
        }
        TransactionHelper.commitOrRollbackTransaction();
    }

    /*
     * Does begin after the connection has already been used.
     */
    @Test
    public void testManualBegin2() throws Exception {
        try (Connection connection = getConnection()) {
            // first thing use connection with autoCommit=true
            String sql = getValidationQuery(connection);
            // switch to no autocommit
            connection.setAutoCommit(false);
            try (Statement st = connection.createStatement()) {
                st.execute(sql);
            }
            connection.commit();
        }
        TransactionHelper.commitOrRollbackTransaction();
    }

    /*
     * Does work, commit, then more work.
     */
    @Test
    public void testCommitThenMoreWork() throws Exception {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            connection.createStatement();
            connection.commit();
            // keep working in transaction mode after commit
            connection.createStatement();
        }
        TransactionHelper.commitOrRollbackTransaction();
    }

    /*
     * Does begin, no commit, then close; checks that close auto-commits.
     */
    @Test
    public void testCloseWithoutCommit() throws Exception {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (Statement st = connection.createStatement()) {
                String sql = getValidationQuery(connection);
                st.execute(sql);
            }
            // don't commit, close() will do it automatically
        }
        TransactionHelper.commitOrRollbackTransaction();
    }

    /*
     * Does begin, no commit, then setAutoCommit=true; checks that autoCommit change auto-commits.
     */
    @Test
    public void testEndWithoutCommit() throws Exception {
        Connection connection = getConnection();
        try {
            connection.setAutoCommit(false);
            try (Statement st = connection.createStatement()) {
                String sql = getValidationQuery(connection);
                st.execute(sql);
            }
            // don't commit
            connection.setAutoCommit(true); // commits automatically
        } finally {
            assertFalse(connection.isClosed());
            connection.setAutoCommit(false);
            connection.close();
            assertTrue(connection.isClosed());
        }
        TransactionHelper.commitOrRollbackTransaction();
    }

    /*
     * Test shared connection use after the transaction has ended.
     */
    @Test
    public void testUseAfterTxEnd() throws Exception {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try (Statement st = connection.createStatement()) {
                String sql = getValidationQuery(connection);
                st.execute(sql);
            }
            // tx just commits now
            // this will log an ERROR
            TransactionHelper.commitOrRollbackTransaction();
            // now keep using the connection
            connection.createStatement();
        }
    }

    /*
     * Re-begin in a new transaction after a previous use and commit in a previous transaction.
     */
    @Test
    public void testSeveralTx() throws Exception {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            String sql = getValidationQuery(connection);
            try (Statement st = connection.createStatement()) {
                st.execute(sql);
            }
            connection.commit();
            TransactionHelper.commitOrRollbackTransaction();

            // new tx

            TransactionHelper.startTransaction();
            connection.setAutoCommit(false);
            try (Statement st = connection.createStatement()) {
                st.execute(sql);
            }
            connection.commit();
            connection.setAutoCommit(true);
            TransactionHelper.commitOrRollbackTransaction();
        }
    }

    /*
     * Test through XAResource wrapper and transaction enlisting. As a side effect the connection commit is called while
     * the transaction is in STATUS_COMMITTING.
     */
    @Test
    public void testXAResourceBeginDoStuffCommit() throws Exception {
        JDBCConnection jdbc = new JDBCConnection();
        jdbc.connection = getConnection();
        try {
            Transaction transaction = TransactionHelper.lookupTransactionManager().getTransaction();
            XAResourceConnectionAdapter xaresource = new XAResourceConnectionAdapter(jdbc);
            transaction.enlistResource(xaresource);
            // use connection
            jdbc.connection.createStatement();
            // then commit
            TransactionHelper.commitOrRollbackTransaction();
        } finally {
            assertFalse(jdbc.connection.isClosed());
            jdbc.connection.close();
            assertTrue(jdbc.connection.isClosed());
        }
    }

    /*
     * Test through XAResource wrapper and transaction enlisting. But do nothing between tx start and end.
     */
    @Test
    public void testXAResourceBeginDoNothingCommit() throws Exception {
        JDBCConnection jdbc = new JDBCConnection();
        jdbc.connection = getConnection();
        try {
            Transaction transaction = TransactionHelper.lookupTransactionManager().getTransaction();
            XAResourceConnectionAdapter xaresource = new XAResourceConnectionAdapter(jdbc);
            transaction.enlistResource(xaresource);
            // do nothing between start and end
            TransactionHelper.commitOrRollbackTransaction();
        } finally {
            assertFalse(jdbc.connection.isClosed());
            jdbc.connection.close();
            assertTrue(jdbc.connection.isClosed());
        }
    }

    /*
     * Test through XAResource wrapper and transaction enlisting. But do nothing between tx start and end, and do a
     * rollback.
     */
    @Test
    public void testXAResourceBeginDoNothingRollback() throws Exception {
        JDBCConnection jdbc = new JDBCConnection();
        jdbc.connection = getConnection();
        try {
            Transaction transaction = TransactionHelper.lookupTransactionManager().getTransaction();
            XAResourceConnectionAdapter xaresource = new XAResourceConnectionAdapter(jdbc);
            transaction.enlistResource(xaresource);
            // do nothing between start and end
            // provoke rollback
            TransactionHelper.setTransactionRollbackOnly();
            TransactionHelper.commitOrRollbackTransaction();
        } finally {
            assertFalse(jdbc.connection.isClosed());
            jdbc.connection.close();
            assertTrue(jdbc.connection.isClosed());
        }
    }

}
