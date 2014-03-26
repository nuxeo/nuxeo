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

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import javax.sql.DataSource;

import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.nuxeo.runtime.datasource.geronimo.PoolingDataSourceFactory;

public class PoolRegistry extends ReentrantReadWriteLock {

    private static final long serialVersionUID = 1L;

    protected final Map<String, DataSource> pools = new HashMap<>();

    protected final ObjectFactory poolFactory = new PoolingDataSourceFactory();

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

    protected ObjectName getObjectName(ConnectionPool pool) {
        try {
            return new ObjectName("org.nuxeo:type=jdbc.pool,name="
                    + pool.getName());
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Cannot build jmx object name for "
                    + pool.getName());
        }
    }

    protected void registerJMX(ConnectionPool pool) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            mbs.registerMBean(pool.getJmxPool(), getObjectName(pool));
        } catch (Exception e) {
            throw new RuntimeException("Cannot publish datasource pool "
                    + pool.getName() + " in platform mbean server", e);
        }
    }

    protected void unregisterJMX(ConnectionPool pool) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            mbs.unregisterMBean(getObjectName(pool));
        } catch (Exception e) {
            throw new RuntimeException("Cannot unpublish datasource pool "
                    + pool.getName() + " in platform mbean server", e);
        }

    }

 }
