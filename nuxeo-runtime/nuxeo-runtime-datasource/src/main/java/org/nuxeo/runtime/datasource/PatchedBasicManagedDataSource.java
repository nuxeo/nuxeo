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

import java.lang.reflect.Field;
import java.sql.SQLException;

import javax.sql.XADataSource;
import javax.transaction.TransactionManager;

import org.apache.commons.dbcp.AbandonedConfig;
import org.apache.commons.dbcp.ConnectionFactory;
import org.apache.commons.dbcp.PoolableConnectionFactory;
import org.apache.commons.dbcp.managed.BasicManagedDataSource;
import org.apache.commons.dbcp.managed.LocalXAConnectionFactory;
import org.apache.commons.dbcp.managed.TransactionRegistry;
import org.apache.commons.dbcp.managed.XAConnectionFactory;
import org.apache.commons.pool.KeyedObjectPoolFactory;

/**
 * Patched to use PatchedPoolableManagedConnectionFactory
 * and PatchedDataSourceXAConnectionFactory.
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
    protected ConnectionFactory createConnectionFactory() throws SQLException {
        // locally fetch private fields through getters
        TransactionManager transactionManager = getTransactionManager();
        String xaDataSource = getXADataSource();
        XADataSource xaDataSourceInstance = getXaDataSourceInstance();

        if (transactionManager == null) {
            throw new SQLException("Transaction manager must be set before a connection can be created");
        }

        // If xa data source is not specified a DriverConnectionFactory is created and wrapped with a LocalXAConnectionFactory
        if (xaDataSource == null) {
            ConnectionFactory connectionFactory = super.createConnectionFactory();
            XAConnectionFactory xaConnectionFactory = new LocalXAConnectionFactory(getTransactionManager(), connectionFactory);
            setTransactionRegistry(xaConnectionFactory.getTransactionRegistry());
            return xaConnectionFactory;
        }

        // Create the XADataSource instance using the configured class name if it has not been set
        if (xaDataSourceInstance == null) {
            Class<?> xaDataSourceClass = null;
            try {
                xaDataSourceClass = Class.forName(xaDataSource);
            } catch (ClassNotFoundException t) {
                String message = "Cannot load XA data source class '" + xaDataSource + "'";
                throw new SQLException(message, t);
            }

            try {
                xaDataSourceInstance = (XADataSource) xaDataSourceClass.newInstance();
            } catch (InstantiationException t) {
                String message = "Cannot create XA data source of class '" + xaDataSource + "'";
                throw new SQLException(message, t);
            } catch (IllegalAccessException t) {
                String message = "Cannot create XA data source of class '" + xaDataSource + "'";
                throw new SQLException(message, t);
            }
        }

        // finally, create the XAConectionFactory using the XA data source
        // PATCH: use PatchedDataSourceXAConnectionFactory
        XAConnectionFactory xaConnectionFactory = new PatchedDataSourceXAConnectionFactory(transactionManager, xaDataSourceInstance, username, password);
        setTransactionRegistry(xaConnectionFactory.getTransactionRegistry());
        return xaConnectionFactory;
    }

    // field transactionRegistry is private in the stupid superclass...
    protected void setTransactionRegistry(
            TransactionRegistry transactionRegistry) {
        try {
            Field field = getClass().getSuperclass().getDeclaredField("transactionRegistry");
            field.setAccessible(true);
            field.set(this, transactionRegistry);
        } catch (SecurityException e) {
            throw new RuntimeException(e);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
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
