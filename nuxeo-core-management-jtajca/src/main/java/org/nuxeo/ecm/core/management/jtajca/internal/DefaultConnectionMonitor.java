/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.core.management.jtajca.internal;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.spi.ConnectionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.connector.outbound.AbstractConnectionManager;
import org.apache.geronimo.connector.outbound.AbstractConnectionManager.Interceptors;
import org.apache.geronimo.connector.outbound.ConnectionInterceptor;
import org.nuxeo.ecm.core.management.jtajca.ConnectionMonitor;
import org.nuxeo.runtime.jtajca.NuxeoContainer;

/**
 * @author matic
 *
 */
public class DefaultConnectionMonitor implements ConnectionMonitor {

    protected final AbstractConnectionManager cm;

    protected final Log log = LogFactory.getLog(DefaultConnectionMonitor.class);

    protected DefaultConnectionMonitor(AbstractConnectionManager cm) {
        this.cm = enhanceConnectionManager(cm);
    }

    protected static Field field(Class<?> clazz, Object object, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            throw new RuntimeException("Cannot get access to " + clazz + "#"
                    + name + " field");
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T> T fetch(Field field, Object object) {
        try {
            return (T) field.get(object);
        } catch (Exception e) {
            throw new RuntimeException("Cannot get access to field content", e);
        }
    }

    protected static void save(Field field, Object object, Object value) {
        try {
            field.set(object, value);
        } catch (Exception e) {
            throw new RuntimeException("Cannot set field content", e);
        }
    }

    protected AbstractConnectionManager enhanceConnectionManager(
            AbstractConnectionManager cm) {
        if (!log.isTraceEnabled()) {
            return cm;
        }
        Field field = field(AbstractConnectionManager.class, cm, "interceptors");
        Interceptors interceptors = fetch(field, cm);
        interceptors = enhanceInterceptors(interceptors);
        save(field, cm, interceptors);
        return cm;
    }

    protected Interceptors enhanceInterceptors(Interceptors interceptors) {
        Field field = field(interceptors.getClass(), interceptors, "stack");
        ConnectionInterceptor stack = fetch(field, interceptors);
        save(field, interceptors, enhanceStack(stack));
        return interceptors;
    }

    protected ConnectionInterceptor enhanceStack(ConnectionInterceptor stack) {
        try {
            Field field = field(stack.getClass(), stack, "next");
            ConnectionInterceptor next = fetch(field, stack);
            save(field, stack, enhanceStack(next));
        } catch (RuntimeException e) {
            ;
        }
        return (ConnectionInterceptor) Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[] { ConnectionInterceptor.class }, new StackHandler(
                        stack));
    }

    protected class StackHandler implements InvocationHandler {

        protected final ConnectionInterceptor stack;

        public StackHandler(ConnectionInterceptor stack) {
            this.stack = stack;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            if (log.isTraceEnabled()) {
                log.trace("invoked " + stack.getClass().getSimpleName() + "."
                        + method.getName());
            }
            return method.invoke(stack, args);
        }
    }

    protected static ConnectionMonitor monitor;

    public static void install() {
        AbstractConnectionManager cm = lookup();
        monitor = new DefaultConnectionMonitor(cm);
        bindManagementInterface();
    }

    public static void uninstall() throws MBeanRegistrationException,
            InstanceNotFoundException {
        if (monitor == null) {
            return;
        }
        unbindManagementInterface();
        monitor = null;
    }

    public static AbstractConnectionManager lookup() {
        ConnectionManager cm = NuxeoContainer.getConnectionManager();
        if (cm == null) { // try setup through NuxeoConnectionManagerFactory
            try {
                InitialContext ic = new InitialContext();
                cm = (ConnectionManager) ic.lookup("java:comp/env/NuxeoConnectionManager");
            } catch (NamingException cause) {
                throw new RuntimeException("Cannot lookup tx manager", cause);
            }
        }
        if (!(cm instanceof NuxeoContainer.ConnectionManagerWrapper)) {
            throw new RuntimeException("Nuxeo container not installed");
        }
        try {
            Field f = NuxeoContainer.ConnectionManagerWrapper.class.getDeclaredField("cm");
            f.setAccessible(true);
            return (AbstractConnectionManager) f.get(cm);
        } catch (Exception cause) {
            throw new RuntimeException(
                    "Cannot access to geronimo connection manager", cause);
        }
    }

    protected static MBeanServer mbs;

    protected static void bindManagementInterface() {
        try {
            mbs = ManagementFactory.getPlatformMBeanServer();
            mbs.registerMBean(monitor, new ObjectName(ConnectionMonitor.NAME));
        } catch (Exception cause) {
            throw new RuntimeException("Cannot register tx monitor", cause);
        }
    }

    protected static void unbindManagementInterface() {
        try {
            mbs.unregisterMBean(new ObjectName(ConnectionMonitor.NAME));
        } catch (Exception e) {
            throw new RuntimeException("Cannot unregister tx monitor");
        } finally {
            mbs = null;
        }
    }

    @Override
    public int getConnectionCount() {
        return cm.getConnectionCount();
    }

    @Override
    public int getIdleConnectionCount() {
        return cm.getIdleConnectionCount();
    }

    @Override
    public int getBlockingTimeoutMilliseconds() {
        return cm.getBlockingTimeoutMilliseconds();
    }

    @Override
    public int getIdleTimeoutMinutes() {
        return cm.getIdleTimeoutMinutes();
    }

    @Override
    public int getPartitionCount() {
        return cm.getPartitionCount();
    }

    @Override
    public int getPartitionMaxSize() {
        return cm.getPartitionMaxSize();
    }

    @Override
    public void setPartitionMaxSize(int maxSize) throws InterruptedException {
        cm.setPartitionMaxSize(maxSize);
    }

    @Override
    public int getPartitionMinSize() {
        return cm.getPartitionMinSize();
    }

    @Override
    public void setPartitionMinSize(int minSize) {
        cm.setPartitionMinSize(minSize);
    }

    @Override
    public void setBlockingTimeoutMilliseconds(int timeoutMilliseconds) {
        cm.setBlockingTimeoutMilliseconds(timeoutMilliseconds);
    }

    @Override
    public void setIdleTimeoutMinutes(int idleTimeoutMinutes) {
        cm.setIdleTimeoutMinutes(idleTimeoutMinutes);
    }

}
