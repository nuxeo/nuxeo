/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.runtime.datasource;

import java.sql.SQLException;

import org.apache.commons.dbcp.AbandonedConfig;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.managed.BasicManagedDataSource;
import org.apache.commons.dbcp.managed.XAConnectionFactory;
import org.apache.commons.pool.KeyedObjectPoolFactory;

/**
 * Patched to use PatchedPoolableManagedConnectionFactory
 */
public class PatchedBasicManagedDataSource extends BasicManagedDataSource {

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void createPoolableConnectionFactory(
            ConnectionFactory driverConnectionFactory,
            KeyedObjectPoolFactory statementPoolFactory,
            AbandonedConfig abandonedConfig) throws SQLException {
        try {
            // PATCH: use patched class
            PoolableConnectionFactory connectionFactory = new PatchedPoolableManagedConnectionFactory(
                    (XAConnectionFactory) driverConnectionFactory,
                    connectionPool, statementPoolFactory, validationQuery,
                    validationQueryTimeout, connectionInitSqls,
                    defaultReadOnly, defaultAutoCommit,
                    defaultTransactionIsolation, defaultCatalog,
                    abandonedConfig);
            validateConnectionFactory(connectionFactory);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw (SQLException) new SQLException(
                    "Cannot create PoolableConnectionFactory ("
                            + e.getMessage() + ")").initCause(e);
        }
    }

}
