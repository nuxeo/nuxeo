/*
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.transaction.Transaction;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.jdbc.JDBCConnection;
import org.nuxeo.ecm.core.storage.sql.jdbc.XAResourceConnectionAdapter;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.ConnectionHelper;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.osgi.OSGiRuntimeService;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Test that transaction management does the right thing with single-datasource
 * mode.
 */
public class TestSingleDataSource extends SQLRepositoryTestCase {

    @Override
    protected OSGiRuntimeService handleNewRuntime(OSGiRuntimeService runtime) {
        runtime = super.handleNewRuntime(runtime);
        Framework.getProperties().setProperty(ConnectionHelper.SINGLE_DS, "jdbc/NuxeoTestDS");
        return runtime;
    }

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp(); // database setUp deletes all tables
        fireFrameworkStarted();
        NuxeoContainer.install();
        TransactionHelper.startTransaction();

        // no openSession() done here
    }

    @After
    @Override
    public void tearDown() throws Exception {
        try {
            if (session != null) {
                session.cancel();
                closeSession();
            }
            if (TransactionHelper.isTransactionActiveOrMarkedRollback()) {
                TransactionHelper.setTransactionRollbackOnly();
                TransactionHelper.commitOrRollbackTransaction();
            }
        } finally {
            System.getProperties().remove(ConnectionHelper.SINGLE_DS);
            if (NuxeoContainer.isInstalled()) {
                NuxeoContainer.uninstall();
            }
            super.tearDown();
        }
    }

    /**
     * H2 cannot have one connection doing an insert in a tx and annother using
     * the same table, as it waits for a lock.
     */
    protected boolean canUseTwoConnections() {
        return !(database instanceof DatabaseH2 //
        || database instanceof DatabaseDerby);
    }

    protected String getValidationQuery(Connection connection)
            throws StorageException {
        return Dialect.createDialect(connection, null, null).getValidationQuery();
    }

    protected static void assertEqualsInt(int expected, ResultSet rs)
            throws SQLException {
        assertTrue(rs.next());
        int actual = rs.getInt(1);
        assertEquals(expected, actual);
        assertFalse(rs.next());
        rs.close();
    }

    protected static void assertSharedConnectionCount(int n) {
        assertEquals(n, ConnectionHelper.countConnectionReferences());
    }

    @Test
    public void testNoTxNoBegin() throws Exception {
        TransactionHelper.commitOrRollbackTransaction(); // end tx
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            assertTrue(connection.getAutoCommit());
            connection.setAutoCommit(true); // already true, no effect
            connection.setAutoCommit(false);
            connection.setAutoCommit(true);
            Statement st = connection.createStatement();
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertSharedConnectionCount(0);
        } finally {
            assertFalse(connection.isClosed());
            connection.close();
            assertTrue(connection.isClosed());
            connection.close(); // close twice ok
            assertTrue(connection.isClosed());
        }
        // use after close is forbidden
        try {
            connection.createStatement();
            fail("use after close should fail");
        } catch (SQLException e) {
            assertTrue(e.getMessage().contains("Connection is closed"));
        }
    }

    @Test
    public void testNoTxBegin() throws Exception {
        TransactionHelper.commitOrRollbackTransaction(); // end tx
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            // first thing set autoCommit=false, but no tx -> no sharing
            connection.setAutoCommit(false);
            connection.setAutoCommit(false); // already false, no effect
            Statement st = connection.createStatement();
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertSharedConnectionCount(0);
            connection.commit(); // needed for DB2 before close
            connection.setAutoCommit(true);
        } finally {
            connection.close();
        }
    }

    /*
     * Transaction in non-ACTIVE state.
     */
    @Test
    public void testBadTxBegin() throws Exception {
        TransactionHelper.setTransactionRollbackOnly(); // not ACTIVE
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            // first thing set autoCommit=false, but no tx -> no sharing
            connection.setAutoCommit(false);
            connection.setAutoCommit(false); // already false, no effect
            Statement st = connection.createStatement();
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertSharedConnectionCount(0);
            connection.commit(); // needed for DB2 before close
        } finally {
            connection.close();
        }
    }

    @Test
    public void testNoTxSwitchAutoCommit() throws Exception {
        TransactionHelper.commitOrRollbackTransaction(); // end tx
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            // use connection with autoCommit=true
            connection.createStatement();
            // then set autoCommit=false, but no tx -> no sharing
            connection.setAutoCommit(false);
            connection.setAutoCommit(false); // already false, no effect
            Statement st = connection.createStatement();
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertSharedConnectionCount(0);
            connection.commit(); // needed for DB2 before close
            connection.setAutoCommit(true);
        } finally {
            connection.close();
        }
    }

    @Test
    public void testNoBegin() throws Exception {
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            Statement st = connection.createStatement();
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertSharedConnectionCount(0);
        } finally {
            assertFalse(connection.isClosed());
            connection.close();
            assertTrue(connection.isClosed());
        }
    }

    /*
     * Does begin as the first thing the connection does.
     */
    @Test
    public void testManualBegin1() throws Exception {
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            assertSharedConnectionCount(0);
            // first thing set autoCommit=false => starts sharing
            connection.setAutoCommit(false);
            // lazy, still not created
            assertSharedConnectionCount(0);
            Statement st = connection.createStatement();
            // shared connection created
            assertSharedConnectionCount(1);
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertSharedConnectionCount(1);
            connection.commit();
            // shared connection kept around, may have other uses
            assertSharedConnectionCount(1);
        } finally {
            assertFalse(connection.isClosed());
            connection.close();
            assertTrue(connection.isClosed());
        }
        assertSharedConnectionCount(1);
        TransactionHelper.commitOrRollbackTransaction();
        // tx synchronizer removes the shared connection
        assertSharedConnectionCount(0);
    }

    /*
     * Does begin after the connection has already been used.
     */
    @Test
    public void testManualBegin2() throws Exception {
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            assertSharedConnectionCount(0);
            // first thing use connection with autoCommit=true
            String sql = getValidationQuery(connection);
            assertSharedConnectionCount(0);
            // switch to shared
            connection.setAutoCommit(false);
            assertSharedConnectionCount(1);
            Statement st = connection.createStatement();
            st.execute(sql);
            assertSharedConnectionCount(1);
            connection.commit();
            // shared connection kept around, may have other uses
            assertSharedConnectionCount(1);
        } finally {
            assertFalse(connection.isClosed());
            connection.close();
            assertTrue(connection.isClosed());
        }
        assertSharedConnectionCount(1);
        TransactionHelper.commitOrRollbackTransaction();
        // tx synchronizer removes the shared connection
        assertSharedConnectionCount(0);
    }

    /*
     * Does work, commit, then more work.
     */
    @Test
    public void testCommitThenMoreWork() throws Exception {
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            assertSharedConnectionCount(0);
            connection.setAutoCommit(false);
            assertSharedConnectionCount(0);
            connection.createStatement();
            assertSharedConnectionCount(1);
            connection.commit();
            assertSharedConnectionCount(1);
            // keep working in transaction mode after commit
            connection.createStatement();
            assertSharedConnectionCount(1);
        } finally {
            assertFalse(connection.isClosed());
            connection.close();
            assertTrue(connection.isClosed());
        }
        assertSharedConnectionCount(1);
        TransactionHelper.commitOrRollbackTransaction();
        // tx synchronizer removes the shared connection
        assertSharedConnectionCount(0);
    }

    /*
     * Does begin, no commit, then close; checks that close auto-commits.
     */
    @Test
    public void testCloseWithoutCommit() throws Exception {
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            connection.setAutoCommit(false);
            Statement st = connection.createStatement();
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertSharedConnectionCount(1);
            // don't commit, close() will do it automatically
        } finally {
            assertFalse(connection.isClosed());
            connection.close();
            assertTrue(connection.isClosed());
        }
        assertSharedConnectionCount(1);
        TransactionHelper.commitOrRollbackTransaction();
        // tx synchronizer removes the shared connection
        assertSharedConnectionCount(0);
    }

    /*
     * Does begin, no commit, then setAutoCommit=true; checks that autoCommit
     * change auto-commits.
     */
    @Test
    public void testEndWithoutCommit() throws Exception {
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            connection.setAutoCommit(false);
            Statement st = connection.createStatement();
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertSharedConnectionCount(1);
            // don't commit
            connection.setAutoCommit(true); // commits automatically
        } finally {
            assertFalse(connection.isClosed());
            connection.close();
            assertTrue(connection.isClosed());
        }
        assertSharedConnectionCount(1);
        TransactionHelper.commitOrRollbackTransaction();
        // tx synchronizer removes the shared connection
        assertSharedConnectionCount(0);
    }

    /*
     * Test shared connection use after the transaction has ended.
     */
    @Test
    public void testUseAfterTxEnd() throws Exception {
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            connection.setAutoCommit(false);
            Statement st = connection.createStatement();
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertSharedConnectionCount(1);
            // tx just commits now
            // this will log an ERROR
            TransactionHelper.commitOrRollbackTransaction();
            assertSharedConnectionCount(0);
            // now keep using the connection
            connection.createStatement();
        } finally {
            assertFalse(connection.isClosed());
            connection.close();
            assertTrue(connection.isClosed());
        }
        assertSharedConnectionCount(0);
    }

    /*
     * Re-begin in a new transaction after a previous use and commit in a
     * previous transaction.
     */
    @Test
    public void testSeveralTx() throws Exception {
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            connection.setAutoCommit(false);
            Statement st = connection.createStatement();
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertSharedConnectionCount(1);
            connection.commit();
            connection.setAutoCommit(true);
            TransactionHelper.commitOrRollbackTransaction();
            assertSharedConnectionCount(0);

            // new tx

            TransactionHelper.startTransaction();
            connection.setAutoCommit(false);
            st = connection.createStatement();
            st.execute(sql);
            assertSharedConnectionCount(1);
            connection.commit();
            connection.setAutoCommit(true);
            TransactionHelper.commitOrRollbackTransaction();
            assertSharedConnectionCount(0);
        } finally {
            assertFalse(connection.isClosed());
            connection.close();
            assertTrue(connection.isClosed());
        }
    }

    /*
     * Test through XAResource wrapper and transaction enlisting.
     *
     * As a side effect the connection commit is called while the transaction is
     * in STATUS_COMMITTING.
     */
    @Test
    public void testXAResourceBeginDoStuffCommit() throws Exception {
        JDBCConnection jdbc = new JDBCConnection();
        jdbc.connection = ConnectionHelper.getConnection(null);
        try {
            Transaction transaction = TransactionHelper.lookupTransactionManager().getTransaction();
            XAResourceConnectionAdapter xaresource = new XAResourceConnectionAdapter(
                    jdbc);
            transaction.enlistResource(xaresource);
            // lazy so not yet allocated
            assertSharedConnectionCount(0);
            // use connection
            jdbc.connection.createStatement();
            assertSharedConnectionCount(1);
            // then commit
            TransactionHelper.commitOrRollbackTransaction();
        } finally {
            assertFalse(jdbc.connection.isClosed());
            jdbc.connection.close();
            assertTrue(jdbc.connection.isClosed());
        }
    }

    /*
     * Test through XAResource wrapper and transaction enlisting.
     *
     * But do nothing between tx start and end.
     */
    @Test
    public void testXAResourceBeginDoNothingCommit() throws Exception {
        JDBCConnection jdbc = new JDBCConnection();
        jdbc.connection = ConnectionHelper.getConnection(null);
        try {
            Transaction transaction = TransactionHelper.lookupTransactionManager().getTransaction();
            XAResourceConnectionAdapter xaresource = new XAResourceConnectionAdapter(
                    jdbc);
            transaction.enlistResource(xaresource);
            // lazy so not yet allocated
            assertSharedConnectionCount(0);
            // do nothing between start and end
            TransactionHelper.commitOrRollbackTransaction();
        } finally {
            assertFalse(jdbc.connection.isClosed());
            jdbc.connection.close();
            assertTrue(jdbc.connection.isClosed());
        }
    }

    /*
     * Test through XAResource wrapper and transaction enlisting.
     *
     * But do nothing between tx start and end, and do a rollback.
     */
    @Test
    public void testXAResourceBeginDoNothingRollback() throws Exception {
        JDBCConnection jdbc = new JDBCConnection();
        jdbc.connection = ConnectionHelper.getConnection(null);
        try {
            Transaction transaction = TransactionHelper.lookupTransactionManager().getTransaction();
            XAResourceConnectionAdapter xaresource = new XAResourceConnectionAdapter(
                    jdbc);
            transaction.enlistResource(xaresource);
            // lazy so not yet allocated
            assertSharedConnectionCount(0);
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

    @Test
    public void testTwoConnections() throws Exception {
        assumeTrue(canUseTwoConnections());

        // separate connection to check results
        Connection checker = ConnectionHelper.getConnection(null, true);
        Statement chst = checker.createStatement();
        chst.execute("CREATE TABLE foo (i INTEGER)");

        Connection connection = ConnectionHelper.getConnection(null);
        Connection connection2 = ConnectionHelper.getConnection(null);
        try {
            assertSharedConnectionCount(0);
            // first thing set autoCommit=false => starts sharing
            connection.setAutoCommit(false);
            connection2.setAutoCommit(false);
            // lazy, still not created
            assertSharedConnectionCount(0);

            Statement st = connection.createStatement();
            // shared connection created
            assertSharedConnectionCount(1);
            st.execute("INSERT INTO foo (i) VALUES (1)");

            // not committed yet
            assertEqualsInt(0, chst.executeQuery("SELECT COUNT(*) FROM foo"));

            Statement st2 = connection2.createStatement();
            // really shared
            assertSharedConnectionCount(1);
            st2.execute("INSERT INTO foo (i) VALUES (2)");

            connection.commit();
            assertSharedConnectionCount(1);
            // still not committed yet
            assertEqualsInt(0, chst.executeQuery("SELECT COUNT(*) FROM foo"));

            connection2.commit();
            assertSharedConnectionCount(1);
            // last commit() committed all statements
            assertEqualsInt(2, chst.executeQuery("SELECT COUNT(*) FROM foo"));
        } finally {
            connection.close();
            connection2.close();
            checker.close();
        }
        assertSharedConnectionCount(1);
        TransactionHelper.commitOrRollbackTransaction();
        // tx synchronizer removes the shared connection
        assertSharedConnectionCount(0);
    }

    /*
     * First connection does a rollback.
     */
    @Test
    public void testTwoConnectionsWithFirstRollback() throws Exception {
        assumeTrue(canUseTwoConnections());

        // separate connection to check results
        Connection checker = ConnectionHelper.getConnection(null, true);
        Statement chst = checker.createStatement();
        chst.execute("CREATE TABLE foo (i INTEGER)");

        Connection connection = ConnectionHelper.getConnection(null);
        Connection connection2 = ConnectionHelper.getConnection(null);
        try {
            assertSharedConnectionCount(0);
            // first thing set autoCommit=false => starts sharing
            connection.setAutoCommit(false);
            connection2.setAutoCommit(false);
            // lazy, still not created
            assertSharedConnectionCount(0);

            Statement st = connection.createStatement();
            // shared connection created
            assertSharedConnectionCount(1);
            st.execute("INSERT INTO foo (i) VALUES (1)");

            // not committed yet
            assertEqualsInt(0, chst.executeQuery("SELECT COUNT(*) FROM foo"));

            Statement st2 = connection2.createStatement();
            // really shared
            assertSharedConnectionCount(1);
            st2.execute("INSERT INTO foo (i) VALUES (2)");

            connection.rollback();
            assertSharedConnectionCount(1);
            // still not committed yet
            assertEqualsInt(0, chst.executeQuery("SELECT COUNT(*) FROM foo"));

            // connection 2 can still be used, even though in the end
            // it will rollback
            connection2.createStatement();

            connection2.commit();
            assertSharedConnectionCount(1);
            // last commit() does actually rollback
            assertEqualsInt(0, chst.executeQuery("SELECT COUNT(*) FROM foo"));
        } finally {
            connection.close();
            connection2.close();
            checker.close();
        }
        assertSharedConnectionCount(1);
        TransactionHelper.commitOrRollbackTransaction();
        // tx synchronizer removes the shared connection
        assertSharedConnectionCount(0);
    }

    /*
     * Second connection does a rollback.
     */
    @Test
    public void testTwoConnectionsWithSecondRollback() throws Exception {
        assumeTrue(canUseTwoConnections());

        // separate connection to check results
        Connection checker = ConnectionHelper.getConnection(null, true);
        Statement chst = checker.createStatement();
        chst.execute("CREATE TABLE foo (i INTEGER)");

        Connection connection = ConnectionHelper.getConnection(null);
        Connection connection2 = ConnectionHelper.getConnection(null);
        try {
            assertSharedConnectionCount(0);
            // first thing set autoCommit=false => starts sharing
            connection.setAutoCommit(false);
            connection2.setAutoCommit(false);
            // lazy, still not created
            assertSharedConnectionCount(0);

            Statement st = connection.createStatement();
            // shared connection created
            assertSharedConnectionCount(1);
            st.execute("INSERT INTO foo (i) VALUES (1)");

            // not committed yet
            assertEqualsInt(0, chst.executeQuery("SELECT COUNT(*) FROM foo"));

            Statement st2 = connection2.createStatement();
            // really shared
            assertSharedConnectionCount(1);
            st2.execute("INSERT INTO foo (i) VALUES (2)");

            connection.commit();
            assertSharedConnectionCount(1);
            // still not committed yet
            assertEqualsInt(0, chst.executeQuery("SELECT COUNT(*) FROM foo"));

            connection2.rollback();
            assertSharedConnectionCount(1);
            // last rollback() does actually rollback
            assertEqualsInt(0, chst.executeQuery("SELECT COUNT(*) FROM foo"));
        } finally {
            connection.close();
            connection2.close();
            checker.close();
        }
        assertSharedConnectionCount(1);
        TransactionHelper.commitOrRollbackTransaction();
        // tx synchronizer removes the shared connection
        assertSharedConnectionCount(0);
    }

}
