/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.nuxeo.runtime.datasource;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.PooledConnection;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.apache.commons.dbcp.managed.DataSourceXAConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Patched to do proper close. See DBCP-355.
 */
public class PatchedDataSourceXAConnectionFactory extends
        DataSourceXAConnectionFactory {

    private static final Log log = LogFactory.getLog(PatchedDataSourceXAConnectionFactory.class);

    public PatchedDataSourceXAConnectionFactory(
            TransactionManager transactionManager, XADataSource xaDataSource,
            String username, String password) {
        super(transactionManager, xaDataSource, username, password);
    }

    @Override
    public Connection createConnection() throws SQLException {
        // create a new XAConection
        XAConnection xaConnection;
        if (username == null) {
            xaConnection = xaDataSource.getXAConnection();
        } else {
            xaConnection = xaDataSource.getXAConnection(username, password);
        }

        // get the real connection and XAResource from the connection
        Connection connection = xaConnection.getConnection();
        XAResource xaResource = xaConnection.getXAResource();

        // register the xa resource for the connection
        transactionRegistry.registerConnection(connection, xaResource);

        // PATCH: register a ConnectionEventListener for close
        // See DBCP-355
        // The Connection we're returning is a handle on the XAConnection.
        // When the pool calling us closes the Connection, we need to
        // also close the XAConnection that holds the physical connection.
        xaConnection.addConnectionEventListener(new ConnectionEventListener() {
            @Override
            public void connectionClosed(ConnectionEvent event) {
                PooledConnection pc = (PooledConnection) event.getSource();
                pc.removeConnectionEventListener(this);
                try {
                    pc.close();
                } catch (SQLException e) {
                    log.error("Failed to close XAConnection", e);
                }
            }

            @Override
            public void connectionErrorOccurred(ConnectionEvent event) {
                connectionClosed(event);
            }
        });

        return connection;
    }

}
