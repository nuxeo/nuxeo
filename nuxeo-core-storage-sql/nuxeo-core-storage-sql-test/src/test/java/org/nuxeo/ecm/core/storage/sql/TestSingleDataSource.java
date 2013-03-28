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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.junit.Test;
import org.nuxeo.common.utils.XidImpl;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.jdbc.XAResourceConnectionAdapter;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.runtime.api.ConnectionHelper;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Test that transaction management does the right thing with single-datasource
 * mode.
 */
public class TestSingleDataSource extends SQLRepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        // the name doesn't actually matter, as code in
        // ConnectionHelper.getDataSource ignores it and uses
        // nuxeo.test.vcs.url etc. for connections in test mode
        String dataSourceName = "jdbc/NuxeoTestDS";
        System.setProperty(ConnectionHelper.SINGLE_DS, dataSourceName);

        super.setUp(); // database setUp deletes all tables
        fireFrameworkStarted();
        NuxeoContainer.install();
        TransactionHelper.startTransaction();

        // no openSession() done here
    }

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

    protected boolean useSingleConnectionMode() {
        return ConnectionHelper.useSingleConnection(null);
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

    @Test
    public void testOneConnectionNoTxNoBegin() throws Exception {
        if (!useSingleConnectionMode()) {
            return;
        }
        TransactionHelper.commitOrRollbackTransaction(); // end tx
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            connection.setAutoCommit(true); // already true, no effect
            connection.setAutoCommit(false);
            connection.setAutoCommit(true);
            Statement st = connection.createStatement();
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertEquals(0, ConnectionHelper.countConnectionReferences());
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
    public void testOneConnectionNoTxBegin() throws Exception {
        if (!useSingleConnectionMode()) {
            return;
        }
        TransactionHelper.commitOrRollbackTransaction(); // end tx
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            // first thing set autoCommit=false, but no tx -> no sharing
            connection.setAutoCommit(false);
            connection.setAutoCommit(false); // already false, no effect
            Statement st = connection.createStatement();
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertEquals(0, ConnectionHelper.countConnectionReferences());
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
    public void testOneConnectionBadTxBegin() throws Exception {
        if (!useSingleConnectionMode()) {
            return;
        }
        TransactionHelper.setTransactionRollbackOnly(); // not ACTIVE
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            // first thing set autoCommit=false, but no tx -> no sharing
            connection.setAutoCommit(false);
            connection.setAutoCommit(false); // already false, no effect
            Statement st = connection.createStatement();
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertEquals(0, ConnectionHelper.countConnectionReferences());
            connection.commit(); // needed for DB2 before close
        } finally {
            connection.close();
        }
    }

    @Test
    public void testOneConnectionNoTxSwitchAutoCommit() throws Exception {
        if (!useSingleConnectionMode()) {
            return;
        }
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
            assertEquals(0, ConnectionHelper.countConnectionReferences());
            connection.commit(); // needed for DB2 before close
            connection.setAutoCommit(true);
        } finally {
            connection.close();
        }
    }

    @Test
    public void testOneConnectionNoBegin() throws Exception {
        if (!useSingleConnectionMode()) {
            return;
        }
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            Statement st = connection.createStatement();
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertEquals(0, ConnectionHelper.countConnectionReferences());
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
    public void testOneConnectionManualBegin1() throws Exception {
        if (!useSingleConnectionMode()) {
            return;
        }
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            assertEquals(0, ConnectionHelper.countConnectionReferences());
            // first thing set autoCommit=false => starts sharing
            connection.setAutoCommit(false);
            // lazy, still not created
            assertEquals(0, ConnectionHelper.countConnectionReferences());
            Statement st = connection.createStatement();
            // shared connection created
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            connection.commit();
            // shared connection kept around, may have other uses
            assertEquals(1, ConnectionHelper.countConnectionReferences());
        } finally {
            assertFalse(connection.isClosed());
            connection.close();
            assertTrue(connection.isClosed());
        }
        assertEquals(1, ConnectionHelper.countConnectionReferences());
        TransactionHelper.commitOrRollbackTransaction();
        // tx synchronizer removes the shared connection
        assertEquals(0, ConnectionHelper.countConnectionReferences());
    }

    /*
     * Does begin after the connection has already been used.
     */
    @Test
    public void testOneConnectionManualBegin2() throws Exception {
        if (!useSingleConnectionMode()) {
            return;
        }
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            assertEquals(0, ConnectionHelper.countConnectionReferences());
            // first thing use connection with autoCommit=true
            String sql = getValidationQuery(connection);
            assertEquals(0, ConnectionHelper.countConnectionReferences());
            // switch to shared
            connection.setAutoCommit(false);
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            Statement st = connection.createStatement();
            st.execute(sql);
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            connection.commit();
            // shared connection kept around, may have other uses
            assertEquals(1, ConnectionHelper.countConnectionReferences());
        } finally {
            assertFalse(connection.isClosed());
            connection.close();
            assertTrue(connection.isClosed());
        }
        assertEquals(1, ConnectionHelper.countConnectionReferences());
        TransactionHelper.commitOrRollbackTransaction();
        // tx synchronizer removes the shared connection
        assertEquals(0, ConnectionHelper.countConnectionReferences());
    }

    /*
     * Does begin, no commit, then close; checks that close auto-commits.
     */
    @Test
    public void testOneConnectionCloseWithoutCommit() throws Exception {
        if (!useSingleConnectionMode()) {
            return;
        }
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            connection.setAutoCommit(false);
            Statement st = connection.createStatement();
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            // don't commit, close() will do it automatically
        } finally {
            assertFalse(connection.isClosed());
            connection.close();
            assertTrue(connection.isClosed());
        }
        assertEquals(1, ConnectionHelper.countConnectionReferences());
        TransactionHelper.commitOrRollbackTransaction();
        // tx synchronizer removes the shared connection
        assertEquals(0, ConnectionHelper.countConnectionReferences());
    }

    /*
     * Does begin, no commit, then setAutoCommit=true; checks that autoCommit
     * change auto-commits.
     */
    @Test
    public void testOneConnectionEndWithoutCommit() throws Exception {
        if (!useSingleConnectionMode()) {
            return;
        }
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            connection.setAutoCommit(false);
            Statement st = connection.createStatement();
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            // don't commit
            connection.setAutoCommit(true); // commits automatically
        } finally {
            assertFalse(connection.isClosed());
            connection.close();
            assertTrue(connection.isClosed());
        }
        assertEquals(1, ConnectionHelper.countConnectionReferences());
        TransactionHelper.commitOrRollbackTransaction();
        // tx synchronizer removes the shared connection
        assertEquals(0, ConnectionHelper.countConnectionReferences());
    }

    /*
     * Re-begin in a new transaction after a previous use and commit in a
     * previous transaction.
     */
    @Test
    public void testOneConnectionSeveralTx() throws Exception {
        if (!useSingleConnectionMode()) {
            return;
        }
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            connection.setAutoCommit(false);
            Statement st = connection.createStatement();
            String sql = getValidationQuery(connection);
            st.execute(sql);
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            connection.commit();
            connection.setAutoCommit(true);
            TransactionHelper.commitOrRollbackTransaction();
            assertEquals(0, ConnectionHelper.countConnectionReferences());

            // new tx

            TransactionHelper.startTransaction();
            connection.setAutoCommit(false);
            st = connection.createStatement();
            st.execute(sql);
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            connection.commit();
            connection.setAutoCommit(true);
            TransactionHelper.commitOrRollbackTransaction();
            assertEquals(0, ConnectionHelper.countConnectionReferences());
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
        if (!useSingleConnectionMode()) {
            return;
        }
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            Transaction transaction = TransactionHelper.lookupTransactionManager().getTransaction();
            XAResourceConnectionAdapter xaresource = new XAResourceConnectionAdapter(
                    connection);
            transaction.enlistResource(xaresource);
            // lazy so not yet allocated
            assertEquals(0, ConnectionHelper.countConnectionReferences());
            // use connection
            connection.createStatement();
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            // then commit
            TransactionHelper.commitOrRollbackTransaction();
        } finally {
            assertFalse(connection.isClosed());
            connection.close();
            assertTrue(connection.isClosed());
        }
    }

    /*
     * Test through XAResource wrapper and transaction enlisting.
     *
     * But do nothing between tx start and end.
     */
    @Test
    public void testXAResourceBeginDoNothingCommit() throws Exception {
        if (!useSingleConnectionMode()) {
            return;
        }
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            Transaction transaction = TransactionHelper.lookupTransactionManager().getTransaction();
            XAResourceConnectionAdapter xaresource = new XAResourceConnectionAdapter(
                    connection);
            transaction.enlistResource(xaresource);
            // lazy so not yet allocated
            assertEquals(0, ConnectionHelper.countConnectionReferences());
            // do nothing between start and end
            TransactionHelper.commitOrRollbackTransaction();
        } finally {
            assertFalse(connection.isClosed());
            connection.close();
            assertTrue(connection.isClosed());
        }
    }

    /*
     * Test through XAResource wrapper and transaction enlisting.
     *
     * But do nothing between tx start and end, and do a rollback.
     */
    @Test
    public void testXAResourceBeginDoNothingRollback() throws Exception {
        if (!useSingleConnectionMode()) {
            return;
        }
        Connection connection = ConnectionHelper.getConnection(null);
        try {
            Transaction transaction = TransactionHelper.lookupTransactionManager().getTransaction();
            XAResourceConnectionAdapter xaresource = new XAResourceConnectionAdapter(
                    connection);
            transaction.enlistResource(xaresource);
            // lazy so not yet allocated
            assertEquals(0, ConnectionHelper.countConnectionReferences());
            // do nothing between start and end
            // provoke rollback
            TransactionHelper.setTransactionRollbackOnly();
            TransactionHelper.commitOrRollbackTransaction();
        } finally {
            assertFalse(connection.isClosed());
            connection.close();
            assertTrue(connection.isClosed());
        }
    }

    @Test
    public void testTwoConnections() throws Exception {
        if (!useSingleConnectionMode()) {
            return;
        }
        // separate connection to check results
        Connection checker = ConnectionHelper.getConnection(null, true);
        Statement chst = checker.createStatement();
        chst.execute("CREATE TABLE foo (i INTEGER)");

        Connection connection = ConnectionHelper.getConnection(null);
        Connection connection2 = ConnectionHelper.getConnection(null);
        try {
            assertEquals(0, ConnectionHelper.countConnectionReferences());
            // first thing set autoCommit=false => starts sharing
            connection.setAutoCommit(false);
            connection2.setAutoCommit(false);
            // lazy, still not created
            assertEquals(0, ConnectionHelper.countConnectionReferences());

            Statement st = connection.createStatement();
            // shared connection created
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            st.execute("INSERT INTO foo (i) VALUES (1)");

            // not committed yet
            assertEqualsInt(0, chst.executeQuery("SELECT COUNT(*) FROM foo"));

            Statement st2 = connection2.createStatement();
            // really shared
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            st2.execute("INSERT INTO foo (i) VALUES (2)");

            connection.commit();
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            // still not committed yet
            assertEqualsInt(0, chst.executeQuery("SELECT COUNT(*) FROM foo"));

            connection2.commit();
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            // last commit() committed all statements
            assertEqualsInt(2, chst.executeQuery("SELECT COUNT(*) FROM foo"));
        } finally {
            connection.close();
            connection2.close();
            checker.close();
        }
        assertEquals(1, ConnectionHelper.countConnectionReferences());
        TransactionHelper.commitOrRollbackTransaction();
        // tx synchronizer removes the shared connection
        assertEquals(0, ConnectionHelper.countConnectionReferences());
    }

    /*
     * First connection does a rollback.
     */
    @Test
    public void testTwoConnectionsWithFirstRollback() throws Exception {
        if (!useSingleConnectionMode()) {
            return;
        }
        // separate connection to check results
        Connection checker = ConnectionHelper.getConnection(null, true);
        Statement chst = checker.createStatement();
        chst.execute("CREATE TABLE foo (i INTEGER)");

        Connection connection = ConnectionHelper.getConnection(null);
        Connection connection2 = ConnectionHelper.getConnection(null);
        try {
            assertEquals(0, ConnectionHelper.countConnectionReferences());
            // first thing set autoCommit=false => starts sharing
            connection.setAutoCommit(false);
            connection2.setAutoCommit(false);
            // lazy, still not created
            assertEquals(0, ConnectionHelper.countConnectionReferences());

            Statement st = connection.createStatement();
            // shared connection created
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            st.execute("INSERT INTO foo (i) VALUES (1)");

            // not committed yet
            assertEqualsInt(0, chst.executeQuery("SELECT COUNT(*) FROM foo"));

            Statement st2 = connection2.createStatement();
            // really shared
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            st2.execute("INSERT INTO foo (i) VALUES (2)");

            connection.rollback();
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            // still not committed yet
            assertEqualsInt(0, chst.executeQuery("SELECT COUNT(*) FROM foo"));

            // connection 2 can still be used, even though in the end
            // it will rollback
            connection2.createStatement();

            connection2.commit();
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            // last commit() does actually rollback
            assertEqualsInt(0, chst.executeQuery("SELECT COUNT(*) FROM foo"));
        } finally {
            connection.close();
            connection2.close();
            checker.close();
        }
        assertEquals(1, ConnectionHelper.countConnectionReferences());
        TransactionHelper.commitOrRollbackTransaction();
        // tx synchronizer removes the shared connection
        assertEquals(0, ConnectionHelper.countConnectionReferences());
    }

    /*
     * Second connection does a rollback.
     */
    @Test
    public void testTwoConnectionsWithSecondRollback() throws Exception {
        if (!useSingleConnectionMode()) {
            return;
        }
        // separate connection to check results
        Connection checker = ConnectionHelper.getConnection(null, true);
        Statement chst = checker.createStatement();
        chst.execute("CREATE TABLE foo (i INTEGER)");

        Connection connection = ConnectionHelper.getConnection(null);
        Connection connection2 = ConnectionHelper.getConnection(null);
        try {
            assertEquals(0, ConnectionHelper.countConnectionReferences());
            // first thing set autoCommit=false => starts sharing
            connection.setAutoCommit(false);
            connection2.setAutoCommit(false);
            // lazy, still not created
            assertEquals(0, ConnectionHelper.countConnectionReferences());

            Statement st = connection.createStatement();
            // shared connection created
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            st.execute("INSERT INTO foo (i) VALUES (1)");

            // not committed yet
            assertEqualsInt(0, chst.executeQuery("SELECT COUNT(*) FROM foo"));

            Statement st2 = connection2.createStatement();
            // really shared
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            st2.execute("INSERT INTO foo (i) VALUES (2)");

            connection.commit();
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            // still not committed yet
            assertEqualsInt(0, chst.executeQuery("SELECT COUNT(*) FROM foo"));

            connection2.rollback();
            assertEquals(1, ConnectionHelper.countConnectionReferences());
            // last rollback() does actually rollback
            assertEqualsInt(0, chst.executeQuery("SELECT COUNT(*) FROM foo"));
        } finally {
            connection.close();
            connection2.close();
            checker.close();
        }
        assertEquals(1, ConnectionHelper.countConnectionReferences());
        TransactionHelper.commitOrRollbackTransaction();
        // tx synchronizer removes the shared connection
        assertEquals(0, ConnectionHelper.countConnectionReferences());
    }

}
