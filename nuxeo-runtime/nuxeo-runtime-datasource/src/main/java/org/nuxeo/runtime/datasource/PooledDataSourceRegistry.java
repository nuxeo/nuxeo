/*
 * (C) Copyright 2012-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 */
package org.nuxeo.runtime.datasource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.Reference;
import javax.sql.DataSource;

public class PooledDataSourceRegistry extends ReentrantReadWriteLock {

    private static final long serialVersionUID = 1L;

    public interface PooledDataSource extends DataSource {
        void dispose();

        Connection getConnection(boolean noSharing) throws SQLException;
    }

    protected final Map<String, PooledDataSource> pools = new HashMap<>();

    protected final PooledDataSourceFactory poolFactory = new org.nuxeo.runtime.datasource.PooledDataSourceFactory();

    public <T> T getPool(String name, Class<T> type) {
        return type.cast(pools.get(name));
    }

    public PooledDataSource getOrCreatePool(Object obj, Name objectName, Context nameCtx, Hashtable<?, ?> env) {
        final Reference ref = (Reference) obj;
        String dsName = (String) ref.get("name").getContent();
        PooledDataSource ds = pools.get(dsName);
        if (ds != null) {
            return ds;
        }
        return createPool(dsName, ref, objectName, nameCtx, env);
    }

    protected PooledDataSource createPool(String dsName, Reference ref, Name objectName, Context nameCtx, Hashtable<?, ?> env) {
        PooledDataSource ds;
        try {
            readLock().lock();
            ds = pools.get(dsName);
            if (ds != null) {
                return ds;
            }
            ds = (PooledDataSource) poolFactory.getObjectInstance(ref, objectName, nameCtx, env);
            pools.put(dsName, ds);
        } finally {
            readLock().unlock();
        }
        return ds;
    }

    protected void clearPool(String name) {
        PooledDataSource ds = pools.remove(name);
        if (ds != null) {
            ds.dispose();
        }
    }

    public void createAlias(String name, PooledDataSource pool) {
        pools.put(name, pool);
    }

}
