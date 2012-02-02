/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.nuxeo.ecm.core.storage.ConnectionResetException;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Mapper.Identification;
import org.nuxeo.ecm.core.storage.sql.Model;

/**
 * Holds a connection to a JDBC database.
 */
public class JDBCConnection {

    /** The model used to do the mapping. */
    protected final Model model;

    /** The SQL information. */
    protected final SQLInfo sqlInfo;

    /** The xa datasource. */
    protected final XADataSource xadatasource;

    /** The xa pooled connection. */
    private XAConnection xaconnection;

    /** The actual connection. */
    public Connection connection;

    protected boolean supportsBatchUpdates;

    protected XAResource xaresource;

    protected final JDBCConnectionPropagator connectionPropagator;

    /** If there's a chance the connection may be closed. */
    protected volatile boolean checkConnectionValid;

    // for tests
    public boolean countExecutes;

    // for tests
    public int executeCount;

    // for debug
    private static final AtomicLong instanceCounter = new AtomicLong(0);

    // for debug
    private final long instanceNumber = instanceCounter.incrementAndGet();

    // for debug
    public final JDBCLogger logger = new JDBCLogger(
            String.valueOf(instanceNumber));

    /**
     * Creates a new Mapper.
     *
     * @param model the model
     * @param sqlInfo the sql info
     * @param xadatasource the XA datasource to use to get connections
     */
    public JDBCConnection(Model model, SQLInfo sqlInfo,
            XADataSource xadatasource,
            JDBCConnectionPropagator connectionPropagator)
            throws StorageException {
        this.model = model;
        this.sqlInfo = sqlInfo;
        this.xadatasource = xadatasource;
        this.connectionPropagator = connectionPropagator;
        connectionPropagator.addConnection(this);
        open();
    }

    public Identification getIdentification() {
        return new Identification(null, "" + instanceNumber);
    }

    protected void countExecute() {
        if (countExecutes) {
            executeCount++;
        }
    }

    protected void open() throws StorageException {
        openConnections();
    }

    private void openConnections() throws StorageException {
        try {
            xaconnection = xadatasource.getXAConnection();
            connection = xaconnection.getConnection();
            supportsBatchUpdates = connection.getMetaData().supportsBatchUpdates();
            xaresource = xaconnection.getXAResource();
            sqlInfo.dialect.performPostOpenStatements(connection);
        } catch (SQLException e) {
            throw new StorageException(e);
        }
    }

    public void close() {
        connectionPropagator.removeConnection(this);
        closeConnections();
        xaresource = null;
    }

    private void closeConnections() {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                // ignore, including UndeclaredThrowableException
                checkConnectionValid = true;
            } finally {
                connection = null;
            }
        }
        if (xaconnection != null) {
            try {
                xaconnection.close();
            } catch (SQLException e) {
                checkConnectionValid = true;
            } finally {
                xaconnection = null;
            }
        }
    }

    /**
     * Opens a new connection if the previous ones was broken or timed out.
     */
    protected void resetConnection() throws StorageException {
        logger.error("Resetting connection");
        closeConnections();
        openConnections();
        // we had to reset a connection; notify all the others that they
        // should check their validity proactively
        connectionPropagator.connectionWasReset(this);
    }

    protected void connectionWasReset() {
        checkConnectionValid = true;
    }

    /**
     * Checks that the connection is valid, and tries to reset it if not.
     */
    protected void checkConnectionValid() throws StorageException {
        if (checkConnectionValid) {
            if (connection == null) {
                resetConnection();
            }

            Statement st = null;
            try {
                st = connection.createStatement();
                st.execute(sqlInfo.dialect.getValidationQuery());
            } catch (Exception e) {
                if (sqlInfo.dialect.isConnectionClosedException(e)) {
                    resetConnection();
                } else {
                    throw new StorageException(e);
                }
            } finally {
                if (st != null) {
                    try {
                        st.close();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
            // only if there was no exception set the flag to false
            checkConnectionValid = false;
        }
    }

    /**
     * Checks the SQL error we got and determine if the low level connection has
     * to be reset.
     * <p>
     * Called with a generic Exception and not just SQLException because the
     * PostgreSQL JDBC driver sometimes fails to unwrap properly some
     * InvocationTargetException / UndeclaredThrowableException.
     *
     * @param t the error
     */
    protected void checkConnectionReset(Throwable t) throws StorageException {
        checkConnectionReset(t, false);
    }

    /**
     * Checks the SQL error we got and determine if the low level connection has
     * to be reset.
     * <p>
     * Called with a generic Exception and not just SQLException because the
     * PostgreSQL JDBC driver sometimes fails to unwrap properly some
     * InvocationTargetException / UndeclaredThrowableException.
     *
     * @param t the error
     * @param throwIfReset {@code true} if a {@link ConnectionResetException}
     *            should be thrown when the connection is reset
     * @since 5.6
     */
    protected void checkConnectionReset(Throwable t, boolean throwIfReset)
            throws StorageException, ConnectionResetException {
        if (connection == null
                || sqlInfo.dialect.isConnectionClosedException(t)) {
            resetConnection();
            if (throwIfReset) {
                throw new ConnectionResetException(t);
            }
        }
    }

    /**
     * Checks the XA error we got and determine if the low level connection has
     * to be reset.
     */
    protected void checkConnectionReset(XAException e) {
        if (connection == null
                || sqlInfo.dialect.isConnectionClosedException(e)) {
            try {
                resetConnection();
            } catch (StorageException ee) {
                // swallow, exception already thrown by caller
            }
        }
    }

    protected void closeStatement(Statement s) throws SQLException {
        try {
            s.close();
        } catch (IllegalArgumentException e) {
            // ignore
            // http://bugs.mysql.com/35489 with JDBC 4 and driver <= 5.1.6
        }
    }

}
