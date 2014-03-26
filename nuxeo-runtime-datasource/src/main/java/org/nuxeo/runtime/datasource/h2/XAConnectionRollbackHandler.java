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
package org.nuxeo.runtime.datasource.h2;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.h2.jdbcx.JdbcXAConnection;
import org.h2.message.Trace;
import org.h2.message.TraceObject;
import org.h2.util.JdbcUtils;

public class XAConnectionRollbackHandler implements
        InvocationHandler {

    protected final JdbcXAConnection proxied;

    protected XAConnectionRollbackHandler(JdbcXAConnection target) {
        proxied = target;
    }

    public static XAConnection newProxy(JdbcXAConnection proxied) {
        InvocationHandler h = new XAConnectionRollbackHandler(
                proxied);
        return (XAConnection) Proxy.newProxyInstance(
                proxied.getClass().getClassLoader(),
                new Class[] { XAConnection.class, XAResource.class }, h);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
            String mname = method.getName();
            if ("rollback".equals(mname)) {
                return rollback(proxied, (Xid) args[0]);
            } else if ("getXAResource".equals(mname)) {
                return proxy;
            }
        return method.invoke(proxied, args);
    }

    protected Field field(Class<?> clazz, String name) {
        try {
            Field f = clazz.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            throw new NoSuchFieldError("Cannot read field " + name);
        }
    }

    protected <V> V getValue(Class<?> clazz, String name, Class<V> rtype) {
        try {
            return rtype.cast(field(clazz, name).get(proxied));
        } catch (Exception e) {
            throw new IncompatibleClassChangeError("Cannot get field " + name + " value ");
        }
    }

    protected <V> void setValue(Class<?> clazz, String name, V value) {
        try {
            field(clazz, name).set(proxied, value);
        } catch (Exception e) {
            throw new RuntimeException("Cannot set " + name + " to " + value);
        }
    }

    protected Object rollback(XAConnection connection, Xid xid) throws XAException {
        Trace trace = getValue(TraceObject.class, "trace", Trace.class);
        if (trace.isDebugEnabled()) {
            trace.debug("rollback(" + xid + ");");
        }
        Connection phycon = getValue(JdbcXAConnection.class,
                "physicalConn", Connection.class);
        try {
                if (getValue(JdbcXAConnection.class, "prepared", Boolean.class).booleanValue()) {
                    Statement stat = null;
                    try {
                        stat = phycon.createStatement();
                        stat.execute("ROLLBACK TRANSACTION "
                                + xid);
                    } finally {
                        JdbcUtils.closeSilently(stat);
                    }
                    setValue(JdbcXAConnection.class, "prepared", false);
                } else {
                    phycon.rollback();
                }
        } catch (SQLException e) {
            XAException xa = new XAException(e.getMessage());
            xa.initCause(e);
            throw xa;
        } finally {
            try {
                phycon.setAutoCommit(true);
            } catch (SQLException e) {
                XAException xa = new XAException(e.getMessage());
                xa.initCause(e);
                throw xa;
            }
        }

        setValue(JdbcXAConnection.class, "currentTransaction", null);
        ;

        return null;
    }

}
