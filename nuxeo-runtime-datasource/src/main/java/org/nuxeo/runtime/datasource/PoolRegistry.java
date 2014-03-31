/*******************************************************************************
 * Copyright (c) 2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *******************************************************************************/
package org.nuxeo.runtime.datasource;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.naming.Context;
import javax.naming.Name;
import javax.sql.DataSource;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.datasource.geronimo.PoolingDataSourceFactory;

public class PoolRegistry extends ReentrantReadWriteLock {

    private static final long serialVersionUID = 1L;

    protected final Map<String, DataSource> pools = new HashMap<>();

    protected final PoolingDataSourceFactory poolFactory = new PoolingDataSourceFactory();

    public DataSource getOrCreatePool(Object obj, Name name,
            Context nameCtx, Hashtable<?, ?> env) throws Exception {
        DataSource ds = pools.get(name.toString());
        if (ds != null) {
            return ds;
        }
        return createPool(obj, name, nameCtx, env);
    }

    protected DataSource createPool(Object obj, Name name,
            Context nameCtx, Hashtable<?, ?> env) throws Exception {
        DataSource ds;
        try {
            readLock().lock();
            String nameString = name.toString();
            ds = pools.get(nameString);
            if (ds != null) {
                return ds;
            }
            ds = (DataSource) poolFactory.getObjectInstance(obj, name,
                   nameCtx, env);
            pools.put(nameString, ds);
        } finally {
            readLock().unlock();
        }
        return ds;
    }

    protected void clearPool(String name) {
        DataSource ds = pools.remove(name);
        if (ds == null) {
            LogFactory.getLog(PoolRegistry.class).warn("Cannot find pool " + name);
            return;
        }
    }

 }
