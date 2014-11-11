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

import org.apache.commons.dbcp.AbandonedConfig;
import org.apache.commons.dbcp.DelegatingConnection;
import org.apache.commons.dbcp.managed.PoolableManagedConnection;
import org.apache.commons.dbcp.managed.TransactionRegistry;
import org.apache.commons.pool.ObjectPool;

/**
 * Patched to have an optimized equals() that also avoids a PostgreSQL driver
 * bug (NXP-6985).
 */
public class PatchedPoolableManagedConnection extends PoolableManagedConnection {

    public PatchedPoolableManagedConnection(
            TransactionRegistry transactionRegistry, Connection conn,
            ObjectPool pool, AbandonedConfig config) {
        super(transactionRegistry, conn, pool, config);

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        Connection delegate = getInnermostDelegateInternal();
        if (delegate == null) {
            return false;
        }
        if (obj instanceof DelegatingConnection) {
            DelegatingConnection c = (DelegatingConnection) obj;
            return c.innermostDelegateEquals(delegate);
        } else {
            // PATCH: check object equality first
            return delegate == obj || delegate.equals(obj);
        }
    }

}
