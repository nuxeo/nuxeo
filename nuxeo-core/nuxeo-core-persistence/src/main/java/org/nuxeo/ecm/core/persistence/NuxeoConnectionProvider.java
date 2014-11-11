/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.connection.ConnectionProvider;
import org.hibernate.connection.DatasourceConnectionProvider;
import org.nuxeo.runtime.api.ConnectionHelper;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * ConnectionProvider for Hibernate that looks up the connection in a
 * thread-local location, in order to share all connections to the database and
 * to avoid the need for XA datasources.
 *
 * @since 5.7
 */
public class NuxeoConnectionProvider implements ConnectionProvider {

    /**
     * Delegate to do a standard Hibernate ConnectionProvider when no Nuxeo
     * connection is available.
     */
    protected DatasourceConnectionProvider dscp;

    /**
     * The application-server-specific JNDI name for the datasource.
     */
    protected String dataSourceName;

    /**
     * Whether we have switched the connection autoCommit=false and must commit
     * it on release.
     */
    protected boolean began;

    @Override
    public void configure(Properties props) throws HibernateException {
        dscp = new DatasourceConnectionProvider();
        dscp.configure(props);
        dataSourceName = props.getProperty(Environment.DATASOURCE);
    }

    @Override
    public Connection getConnection() throws SQLException {
        // try single-datasource non-XA mode
        Connection connection = ConnectionHelper.getConnection(dataSourceName);
        if (connection == null) {
            // standard datasource usage
            connection = dscp.getConnection();
        }
        // begin(connection);
        return connection;
    }

    @Override
    public void closeConnection(Connection connection) throws SQLException {
        try {
            // commit(connection);
        } finally {
            connection.close();
        }
  }

    /**
     * If there is a transaction active, make the connection use it by switching
     * to autoCommit=false
     */
    private void begin(Connection connection) throws SQLException {
        began = false;
        if (!connection.getAutoCommit()) {
            // setAutoCommit(false) already done by container
            // so presumably on connection close the container
            // will do the right thing and commit
            return;
        }
        try {
            Transaction transaction = TransactionHelper.lookupTransactionManager().getTransaction();
            if (transaction != null
                    && transaction.getStatus() == Status.STATUS_ACTIVE) {
                connection.setAutoCommit(false);
                began = true;
            }
        } catch (NamingException e) {
            // ignore
        } catch (SystemException e) {
            // ignore
        }
    }

    /**
     * If we previously switched to autoCommit=false, then now is the time to
     * commit.
     */
    private void commit(Connection connection) throws SQLException {
        if (began) {
            began = false;
            connection.commit();
        }
    }

    @Override
    public void close() throws HibernateException {
    }

    @Override
    public boolean supportsAggressiveRelease() {
        // close the connection after each statement
        return true;
    }

}
