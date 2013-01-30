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

import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.DataSource;

public class DataSourceRegistry extends ReentrantReadWriteLock {

    private static final long serialVersionUID = 1L;

    protected final Map<Name, DataSource> datasources = new HashMap<Name, DataSource>();

    protected final org.apache.tomcat.jdbc.pool.DataSourceFactory delegate = new org.apache.tomcat.jdbc.pool.DataSourceFactory();

    public DataSource getOrCreateDatasource(Object obj, Name name,
            Context nameCtx, Hashtable<?, ?> env) throws Exception {
        DataSource ds = datasources.get(name);
        if (ds != null) {
            return ds;
        }
        return createDatasource(obj, name, nameCtx, env);
    }

    protected DataSource createDatasource(Object obj, Name name,
            Context nameCtx, Hashtable<?, ?> env) throws Exception {
        DataSource ds;
        try {
            readLock().lock();
            ds = datasources.get(name);
            if (ds != null) {
                return ds;
            }
            ds = (DataSource) delegate.getObjectInstance(obj, name,
                   nameCtx, env);
            datasources.put(name, ds);
        } finally {
            readLock().unlock();
        }
        registerJMX(ds.getPool());
        return ds;
    }

    public void clearDatasource(Name name) {
        DataSource ds = datasources.remove(name);
        if (ds != null) {
            unregisterJMX(ds.getPool());
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
            throw new RuntimeException("Cannot publish jdbc pool "
                    + pool.getName() + " in platform mbean server", e);
        }
    }

    protected void unregisterJMX(ConnectionPool pool) {
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            mbs.unregisterMBean(getObjectName(pool));
        } catch (Exception e) {
            throw new RuntimeException("Cannot unpublish jdbc pool "
                    + pool.getName() + " in platform mbean server", e);
        }

    }

 }
