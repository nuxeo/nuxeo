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

import java.sql.Connection;
import java.util.Collection;

import org.apache.commons.dbcp.AbandonedConfig;
import org.apache.commons.dbcp.PoolingConnection;
import org.apache.commons.dbcp.managed.PoolableManagedConnectionFactory;
import org.apache.commons.dbcp.managed.TransactionRegistry;
import org.apache.commons.dbcp.managed.XAConnectionFactory;
import org.apache.commons.pool.KeyedObjectPool;
import org.apache.commons.pool.KeyedObjectPoolFactory;
import org.apache.commons.pool.ObjectPool;

/**
 * Patched to use PatchedPoolableManagedConnection
 */
public class PatchedPoolableManagedConnectionFactory extends
        PoolableManagedConnectionFactory {

    protected TransactionRegistry tr;

    public PatchedPoolableManagedConnectionFactory(
            XAConnectionFactory connFactory, ObjectPool pool,
            KeyedObjectPoolFactory stmtPoolFactory, String validationQuery,
            int validationQueryTimeout, Collection<?> connectionInitSqls,
            Boolean defaultReadOnly, boolean defaultAutoCommit,
            int defaultTransactionIsolation, String defaultCatalog,
            AbandonedConfig config) {
        super(connFactory, pool, stmtPoolFactory, validationQuery,
                validationQueryTimeout, connectionInitSqls, defaultReadOnly,
                defaultAutoCommit, defaultTransactionIsolation, defaultCatalog,
                config);
        // PATCH: local copy because private in base class
        tr = connFactory.getTransactionRegistry();
    }

    @Override
    synchronized public Object makeObject() throws Exception {
        Connection conn = _connFactory.createConnection();
        if (conn == null) {
            throw new IllegalStateException(
                    "Connection factory returned null from createConnection");
        }
        initializeConnection(conn);
        if (null != _stmtPoolFactory) {
            KeyedObjectPool stmtpool = _stmtPoolFactory.createPool();
            conn = new PoolingConnection(conn, stmtpool);
            stmtpool.setFactory((PoolingConnection) conn);
        }
        // PATCH: use patched class
        return new PatchedPoolableManagedConnection(tr, conn, _pool, _config);
    }

}
