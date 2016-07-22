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
import javax.resource.spi.ManagedConnection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.connector.outbound.AbstractConnectionManager;
import org.apache.geronimo.connector.outbound.AbstractConnectionManager.Interceptors;
import org.apache.geronimo.connector.outbound.ConnectionInfo;
import org.apache.geronimo.connector.outbound.ConnectionInterceptor;
import org.apache.log4j.MDC;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.management.jtajca.ConnectionPoolMonitor;
import org.nuxeo.ecm.core.storage.sql.Mapper;
import org.nuxeo.ecm.core.storage.sql.Mapper.Identification;
import org.nuxeo.ecm.core.storage.sql.SessionImpl;
import org.nuxeo.ecm.core.storage.sql.SoftRefCachingMapper;
import org.nuxeo.ecm.core.storage.sql.ra.ManagedConnectionImpl;
import org.nuxeo.runtime.metrics.MetricsService;
import org.tranql.connector.AbstractManagedConnection;

import com.codahale.metrics.JmxAttributeGauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;

/**
 * @author matic
 */
public class DefaultConnectionPoolMonitor implements ConnectionPoolMonitor {

    private static final Log log = LogFactory.getLog(DefaultConnectionPoolMonitor.class);

    // @since 5.7.2
    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected final String name;

    protected AbstractConnectionManager cm;

    protected DefaultConnectionPoolMonitor(String mame, AbstractConnectionManager cm) {
        name = mame;
        this.cm = enhanceConnectionManager(cm);
    }

    protected static Field field(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot get access to " + clazz + "#" + name + " field");
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T> T fetch(Field field, Object object) {
        try {
            return (T) field.get(object);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot get access to field content", e);
        }
    }

    protected static void save(Field field, Object object, Object value) {
        try {
            field.set(object, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot set field content", e);
        }
    }

    protected AbstractConnectionManager enhanceConnectionManager(AbstractConnectionManager cm) {
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
        } catch (RuntimeException e) {;
        }
        return (ConnectionInterceptor) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[] { ConnectionInterceptor.class }, new StackHandler(stack));
    }

    protected class StackHandler implements InvocationHandler {

        protected final ConnectionInterceptor stack;

        public StackHandler(ConnectionInterceptor stack) {
            this.stack = stack;
        }

        protected void traceInvoke(Method m, Object[] args) {
            Throwable stackTrace = null;
            if (ConnectionInterceptor.class.isAssignableFrom(m.getDeclaringClass())) {
                stackTrace = new Throwable("debug stack trace");
            }
            log.trace("invoked " + stack.getClass().getSimpleName() + "." + m.getName(), stackTrace);
        }

        IdProvider midProvider;

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(stack, args);
            } finally {
                String name = method.getName();
                traceInvoke(method, args);
                if (args != null && args.length > 0) {
                    ConnectionInfo info = (ConnectionInfo) args[0];
                    ManagedConnection connection = info.getManagedConnectionInfo().getManagedConnection();
                    if (connection != null) {
                        if (midProvider == null) {
                            midProvider = guessProvider(connection);
                        }
                        if (name.startsWith("get")) {
                            MDC.put(midProvider.key(), midProvider.id(connection));
                        } else if (name.startsWith("return")) {
                            MDC.remove(midProvider.key());
                        }
                    }
                }
            }
        }

        protected IdProvider guessProvider(ManagedConnection connection) {
            if (connection instanceof ManagedConnectionImpl) {
                return new IdProvider() {

                    @Override
                    public String key() {
                        return "vcs";
                    }

                    @Override
                    public Object id(ManagedConnection connection) {
                        return mapperId(connection);
                    }

                };
            }
            if (connection instanceof AbstractManagedConnection) {
                return new IdProvider() {

                    @Override
                    public String key() {
                        return "db";
                    }

                    @Override
                    public Object id(ManagedConnection connection) {
                        return ((AbstractManagedConnection<?,?>) connection).getPhysicalConnection();
                    }

                };
            }
            throw new IllegalArgumentException("unknown connection type of " + connection.getClass());
        }
    }

    interface IdProvider {
        String key();

        Object id(ManagedConnection connection);
    }

    private static final Field SESSION_FIELD = field(ManagedConnectionImpl.class, "session");

    private static final Field WRAPPED_FIELD = field(SoftRefCachingMapper.class, "mapper");

    protected Identification mapperId(ManagedConnection connection) {
        SessionImpl session = fetch(SESSION_FIELD, connection);
        Mapper mapper = session.getMapper();
        if (mapper instanceof SoftRefCachingMapper) {
            mapper = fetch(WRAPPED_FIELD, mapper);
        }
        try {
            return mapper.getIdentification();
        } catch (NuxeoException e) {
            log.error("Cannot fetch mapper identification", e);
            return null;
        }
    }

    protected ObjectInstance self;

    @Override
    public void install() {
        self = DefaultMonitorComponent.bind(this, name);
        registry.register(MetricRegistry.name("nuxeo", "repositories", name, "connections", "count"),
                new JmxAttributeGauge(self.getObjectName(), "ConnectionCount"));
        registry.register(MetricRegistry.name("nuxeo", "repositories", name, "connections", "idle"),
                new JmxAttributeGauge(self.getObjectName(), "IdleConnectionCount"));
    }

    @Override
    public void uninstall() {
        DefaultMonitorComponent.unbind(self);
        registry.remove(MetricRegistry.name("nuxeo", "repositories", name, "connections", "count"));
        registry.remove(MetricRegistry.name("nuxeo", "repositories", name, "connections", "idle"));
        self = null;
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

    /**
     * @since 5.8
     */
    public void handleNewConnectionManager(AbstractConnectionManager cm) {
        this.cm = enhanceConnectionManager(cm);
    }

}
