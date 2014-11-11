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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.management.ObjectInstance;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ManagedConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.connector.outbound.AbstractConnectionManager;
import org.apache.geronimo.connector.outbound.AbstractConnectionManager.Interceptors;
import org.apache.geronimo.connector.outbound.ConnectionInfo;
import org.apache.geronimo.connector.outbound.ConnectionInterceptor;
import org.apache.log4j.MDC;
import org.nuxeo.ecm.core.management.jtajca.ConnectionMonitor;
import org.nuxeo.ecm.core.storage.StorageException;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Mapper.Identification;
import org.nuxeo.ecm.core.storage.sql.SessionImpl;
import org.nuxeo.ecm.core.storage.sql.SoftRefCachingMapper;
import org.nuxeo.ecm.core.storage.sql.ra.ManagedConnectionImpl;
import org.nuxeo.runtime.jtajca.NuxeoContainer;

/**
 * @author matic
 *
 */
public class DefaultConnectionMonitor implements ConnectionMonitor {

    private static final Log log = LogFactory.getLog(DefaultConnectionMonitor.class);

    private static final String NUXEO_CONNECTION_MANAGER_PREFIX = "java:comp/env/NuxeoConnectionManager/";

    protected String repositoryName;

    protected AbstractConnectionManager cm;

    protected DefaultConnectionMonitor(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    protected static Field field(Class<?> clazz, String name) {
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
        Field field = field(AbstractConnectionManager.class, "interceptors");
        Interceptors interceptors = fetch(field, cm);
        interceptors = enhanceInterceptors(interceptors);
        save(field, cm, interceptors);
        return cm;
    }

    protected Interceptors enhanceInterceptors(Interceptors interceptors) {
        Field field = field(interceptors.getClass(), "stack");
        ConnectionInterceptor stack = fetch(field, interceptors);
        save(field, interceptors, enhanceStack(stack));
        return interceptors;
    }

    protected ConnectionInterceptor enhanceStack(ConnectionInterceptor stack) {
        try {
            Field field = field(stack.getClass(), "next");
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

        protected void traceInvoke(Method m, Object[] args) {
            if (!log.isTraceEnabled()) {
                return;
            }
            log.trace("invoked " + stack.getClass().getSimpleName() + "."
                    + m.getName());
        }

        protected void putMDC(ConnectionInfo info) {
            MDC.put("mid", mapperId(info));
        }

        protected void popMDC(ConnectionInfo info) {
            MDC.remove("mid");
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
                throws Throwable {
            try {
                return method.invoke(stack, args);
            } finally {
                String name = method.getName();
                traceInvoke(method, args);
                if (name.startsWith("get")) {
                    MDC.put("mid", mapperId((ConnectionInfo) args[0]));
                } else if (name.startsWith("return")) {
                    MDC.remove("mid");
                }
            }
        }
    }

    private static final Field SESSION_FIELD = field(
            ManagedConnectionImpl.class, "session");

    private static final Field WRAPPED_FIELD = field(SoftRefCachingMapper.class,
            "mapper");

    protected Identification mapperId(ConnectionInfo info) {
        ManagedConnection connection = info.getManagedConnectionInfo().getManagedConnection();
        if (connection == null) {
            return null;
        }
        SessionImpl session = fetch(SESSION_FIELD, connection);
        Mapper mapper = session.getMapper();
        if (mapper instanceof SoftRefCachingMapper) {
            mapper = fetch(WRAPPED_FIELD, mapper);
        }
        try {
            return mapper.getIdentification();
        } catch (StorageException e) {
            log.error("Cannot fetch mapper identification", e);
            return null;
        }
    }

    protected ObjectInstance self;

    protected void install() {
        cm = lookup(repositoryName);
        cm = enhanceConnectionManager(cm);
        self = DefaultMonitorComponent.bind(this, repositoryName);
    }

    protected void uninstall() {
        DefaultMonitorComponent.unbind(self);
        self = null;
        cm = null;
    }

    protected AbstractConnectionManager lookup(String repositoryName) {
        ConnectionManager cm = NuxeoContainer.getConnectionManager(repositoryName);
        if (cm == null) { // try setup through NuxeoConnectionManagerFactory
            try {
                InitialContext ic = new InitialContext();
                cm = (ConnectionManager) ic.lookup(NUXEO_CONNECTION_MANAGER_PREFIX
                        + repositoryName);
            } catch (NamingException cause) {
                throw new RuntimeException("Cannot lookup connection manager", cause);
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
