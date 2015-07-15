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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicLong;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.JDBCUtils;
import org.nuxeo.ecm.core.api.ConcurrentUpdateException;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.storage.sql.Mapper.Identification;
import org.nuxeo.ecm.core.storage.sql.Model;
import org.nuxeo.ecm.core.storage.sql.jdbc.dialect.Dialect;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.datasource.ConnectionHelper;

/**
 * Holds a connection to a JDBC database.
 */
public class JDBCConnection {

    private static final Log log = LogFactory.getLog(JDBCConnection.class);

    /** JDBC application name parameter for setClientInfo. */
    private static final String APPLICATION_NAME = "ApplicationName";

    private static final String SET_CLIENT_INFO_PROP = "org.nuxeo.vcs.setClientInfo";

    private static final String SET_CLIENT_INFO_DEFAULT = "false";

    /** The model used to do the mapping. */
    protected final Model model;

    /** The SQL information. */
    protected final SQLInfo sqlInfo;

    /** The dialect. */
    protected final Dialect dialect;

    /** The xa datasource. */
    protected final XADataSource xadatasource;

    /** The xa pooled connection. */
    private XAConnection xaconnection;

    /** The actual connection. */
    public Connection connection;

    protected boolean supportsBatchUpdates;

    protected XAResource xaresource = new XAResourceConnectionAdapter(this);

    /** Whether this connection must never be shared (long-lived). */
    protected final boolean noSharing;

    // for tests
    public boolean countExecutes;

    // for tests
    public int executeCount;

    // for debug
    private static final AtomicLong instanceCounter = new AtomicLong(0);

    // for debug
    private final long instanceNumber = instanceCounter.incrementAndGet();

    // for debug
    public final JDBCLogger logger = new JDBCLogger(String.valueOf(instanceNumber));

    protected boolean setClientInfo;

    /**
     * Creates a new Mapper.
     *
     * @param model the model
     * @param sqlInfo the sql info
     * @param xadatasource the XA datasource to use to get connections
     */
    public JDBCConnection(Model model, SQLInfo sqlInfo, XADataSource xadatasource, boolean noSharing) {
        this.model = model;
        this.sqlInfo = sqlInfo;
        this.xadatasource = xadatasource;
        this.noSharing = noSharing;
        dialect = sqlInfo.dialect;
        setClientInfo = Boolean.parseBoolean(Framework.getProperty(SET_CLIENT_INFO_PROP, SET_CLIENT_INFO_DEFAULT));
    }

    /**
     * for tests only
     *
     * @since 5.9.3
     */
    public JDBCConnection() {
        xadatasource = null;
        sqlInfo = null;
        noSharing = false;
        model = null;
        dialect = null;
    }

    public Identification getIdentification() {
        return new Identification(null, "" + instanceNumber);
    }

    protected void countExecute() {
        if (countExecutes) {
            executeCount++;
        }
    }

    protected void openConnections() {
        try {
            openBaseConnection();
            supportsBatchUpdates = connection.getMetaData().supportsBatchUpdates();
            dialect.performPostOpenStatements(connection);
        } catch (SQLException cause) {
            throw new NuxeoException("Cannot connect to database: " + model.getRepositoryDescriptor().name, cause);
        }
    }

    protected void openBaseConnection() throws SQLException {
        // try single-datasource non-XA mode
        String repositoryName = model.getRepositoryDescriptor().name;
        String dataSourceName = ConnectionHelper.getPseudoDataSourceNameForRepository(repositoryName);
        connection = ConnectionHelper.getConnection(dataSourceName, noSharing);
        if (connection == null) {
            // standard XA mode
            xaconnection = JDBCUtils.getXAConnection(xadatasource);
            connection = xaconnection.getConnection();
            xaresource = xaconnection.getXAResource();
        } else {
            // single-datasource non-XA mode
            xaconnection = null;
        }
        if (setClientInfo) {
            // log the mapper number (m=123)
            connection.setClientInfo(APPLICATION_NAME, "nuxeo m=" + instanceNumber);
        }
    }

    public void close() {
        closeConnections();
    }

    public void closeConnections() {
        if (connection != null) {
            try {
                try {
                    if (setClientInfo) {
                        // connection will become idle in the pool
                        connection.setClientInfo(APPLICATION_NAME, "nuxeo");
                    }
                } finally {
                    connection.close();
                }
            } catch (SQLException e) {
                log.error(e, e);
            } finally {
                connection = null;
            }
        }
        if (xaconnection != null) {
            try {
                xaconnection.close();
            } catch (SQLException e) {
                log.error(e, e);
            } finally {
                xaconnection = null;
            }
        }
    }

    /**
     * Checks the SQL error we got and determine if a concurrent update happened. Throws if that's the case.
     *
     * @param e the exception
     * @since 5.8
     */
    protected void checkConcurrentUpdate(Throwable e) throws ConcurrentUpdateException {
        if (dialect.isConcurrentUpdateException(e)) {
            throw new ConcurrentUpdateException(e);
        }
    }

    protected void closeStatement(Statement s) throws SQLException {
        try {
            if (s != null) {
                s.close();
            }
        } catch (IllegalArgumentException e) {
            // ignore
            // http://bugs.mysql.com/35489 with JDBC 4 and driver <= 5.1.6
        }
    }

    protected void closeStatement(Statement s, ResultSet r) throws SQLException {
        try {
            if (r != null) {
                r.close();
            }
            if (s != null) {
                s.close();
            }
        } catch (IllegalArgumentException e) {
            // ignore
            // http://bugs.mysql.com/35489 with JDBC 4 and driver <= 5.1.6
        }
    }

}
