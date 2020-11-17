/*
 * (C) Copyright 2006-2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 *     Julien Carsique
 */
package org.nuxeo.runtime.jtajca;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.spi.NamingManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.transaction.UserTransaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.geronimo.transaction.manager.NamedXAResourceFactory;
import org.apache.geronimo.transaction.manager.RecoverableTransactionManager;
import org.apache.geronimo.transaction.manager.TransactionImpl;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.apache.geronimo.transaction.manager.XidImpl;
import org.apache.xbean.naming.reference.SimpleReference;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.transaction.TransactionHelper;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;
import io.dropwizard.metrics5.Timer;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.BlankSpan;
import io.opencensus.trace.Span;
import io.opencensus.trace.Status;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.Tracing;

/**
 * Internal helper for the Nuxeo-defined transaction manager and connection manager.
 * <p>
 * This code is called by the factories registered through JNDI, or by unit tests mimicking JNDI bindings.
 */
public class NuxeoContainer {

    protected static final Log log = LogFactory.getLog(NuxeoContainer.class);

    protected static RecoverableTransactionManager tmRecoverable;

    protected static TransactionManager tm;

    protected static TransactionSynchronizationRegistry tmSynchRegistry;

    protected static UserTransaction ut;

    private static volatile InstallContext installContext;

    protected static Context rootContext;

    protected static Context parentContext;

    protected static String jndiPrefix = "java:comp/env/";

    // @since 5.7
    protected static final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    protected static final Counter rollbackCount = registry.counter(
            MetricRegistry.name("nuxeo", "transactions", "rollbacks"));

    protected static final Counter concurrentCount = registry.counter(
            MetricRegistry.name("nuxeo", "transactions", "concurrency"));

    protected static final Counter concurrentMaxCount = registry.counter(
            MetricRegistry.name("nuxeo", "transactions", "concurrency", "max"));

    protected static final Timer transactionTimer = registry.timer(
            MetricRegistry.name("nuxeo", "transactions", "timer"));

    protected static final ConcurrentHashMap<Transaction, Timer.Context> timers = new ConcurrentHashMap<>();

    private NuxeoContainer() {
    }

    public static class InstallContext extends Throwable {
        private static final long serialVersionUID = 1L;

        public final String threadName;

        InstallContext() {
            super("Container installation context (" + Thread.currentThread().getName() + ")");
            threadName = Thread.currentThread().getName();
        }
    }

    /**
     * Install naming and bind transaction and connection management factories "by hand".
     */
    protected static synchronized void install() throws NamingException {
        if (installContext != null) {
            throw new RuntimeException("Nuxeo container already installed");
        }
        installContext = new InstallContext();
        rootContext = new NamingContext();
        parentContext = InitialContextAccessor.getInitialContext();
        if (parentContext != null && parentContext != rootContext) {
            installTransactionManager(parentContext);
        } else {
            addDeepBinding(nameOf("TransactionManager"), new Reference(TransactionManager.class.getName(),
                    NuxeoTransactionManagerFactory.class.getName(), null));
            installTransactionManager(rootContext);
        }
    }

    protected static void installTransactionManager(TransactionManagerConfiguration config) throws NamingException {
        initTransactionManager(config);
        addDeepBinding(rootContext, new CompositeName(nameOf("TransactionManager")), getTransactionManagerReference());
        addDeepBinding(rootContext, new CompositeName(nameOf("UserTransaction")), getUserTransactionReference());
    }

    public static boolean isInstalled() {
        return installContext != null;
    }

    protected static void uninstall() throws NamingException {
        if (installContext == null) {
            throw new RuntimeException("Nuxeo container not installed");
        }
        log.trace("Uninstalling nuxeo container", installContext);
        installContext = null;
        rootContext = null;
        tm = null;
        tmRecoverable = null;
        tmSynchRegistry = null;
        ut = null;
    }

    protected static String detectJNDIPrefix(Context context) {
        String name = context.getClass().getName();
        if ("org.jnp.interfaces.NamingContext".equals(name)) { // JBoss
            return "java:";
        } else if ("org.jboss.as.naming.InitialContext".equals(name)) { // Wildfly
            return "java:jboss/";
        } else if ("org.mortbay.naming.local.localContextRoot".equals(name)) { // Jetty
            return "jdbc/";
        }
        // Standard JEE containers (Nuxeo-Embedded, Tomcat, GlassFish,
        // ...
        return "java:comp/env/";
    }

    public static String nameOf(String name) {
        return jndiPrefix.concat(name);
    }

    /**
     * Exposes the {@link #rootContext}.
     *
     * @since 5.7
     * @see <a href="https://jira.nuxeo.com/browse/NXP-10331">NXP-10331</a>
     */
    public static Context getRootContext() {
        return rootContext;
    }

    /**
     * Bind object in root context. Create needed sub contexts. since 5.6
     */
    public static void addDeepBinding(String name, Object obj) throws NamingException {
        addDeepBinding(rootContext, new CompositeName(name), obj);
    }

    protected static void addDeepBinding(Context dir, CompositeName comp, Object obj) throws NamingException {
        Name name = comp.getPrefix(1);
        if (comp.size() == 1) {
            addBinding(dir, name, obj);
            return;
        }
        Context subdir;
        try {
            subdir = (Context) dir.lookup(name);
        } catch (NamingException e) {
            subdir = dir.createSubcontext(name);
        }
        addDeepBinding(subdir, (CompositeName) comp.getSuffix(1), obj);
    }

    protected static void addBinding(Context dir, Name name, Object obj) throws NamingException {
        try {
            dir.rebind(name, obj);
        } catch (NamingException e) {
            dir.bind(name, obj);
        }
    }

    protected static void removeBinding(String name) throws NamingException {
        rootContext.unbind(name);
    }

    /**
     * Gets the transaction manager used by the container.
     *
     * @return the transaction manager
     */
    public static TransactionManager getTransactionManager() {
        return tm;
    }

    protected static Reference getTransactionManagerReference() {
        return new SimpleReference() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getContent() throws NamingException {
                return NuxeoContainer.getTransactionManager();
            }
        };
    }

    /**
     * Gets the user transaction used by the container.
     *
     * @return the user transaction
     */
    public static UserTransaction getUserTransaction() {
        return ut;
    }

    protected static Reference getUserTransactionReference() {
        return new SimpleReference() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getContent() throws NamingException {
                return getUserTransaction();
            }
        };
    }

    protected static synchronized TransactionManager initTransactionManager(TransactionManagerConfiguration config) {
        TransactionManagerImpl impl = createTransactionManager(config);
        tm = impl;
        tmRecoverable = impl;
        tmSynchRegistry = impl;
        ut = new UserTransactionImpl(tm);
        return tm;
    }

    protected static TransactionManagerWrapper wrapTransactionManager(TransactionManager tm) {
        if (tm == null) {
            return null;
        }
        if (tm instanceof TransactionManagerWrapper) {
            return (TransactionManagerWrapper) tm;
        }
        return new TransactionManagerWrapper(tm);
    }

    public static <T> T lookup(String name, Class<T> type) throws NamingException {
        if (rootContext == null) {
            throw new NamingException("no naming context available");
        }
        return lookup(rootContext, name, type);
    }

    public static <T> T lookup(Context context, String name, Class<T> type) throws NamingException {
        Object resolved;
        try {
            resolved = context.lookup(detectJNDIPrefix(context).concat(name));
        } catch (NamingException cause) {
            if (parentContext == null) {
                throw cause;
            }
            return type.cast(parentContext.lookup(detectJNDIPrefix(parentContext).concat(name)));
        }
        if (resolved instanceof Reference) {
            try {
                resolved = NamingManager.getObjectInstance(resolved, new CompositeName(name), rootContext, null);
            } catch (NamingException e) {
                throw e;
            } catch (Exception e) { // stupid JNDI API throws Exception
                throw ExceptionUtils.runtimeException(e);
            }
        }
        return type.cast(resolved);
    }

    protected static void installTransactionManager(Context context) throws NamingException {
        TransactionManager actual = lookup(context, "TransactionManager", TransactionManager.class);
        if (tm != null) {
            return;
        }
        tm = actual;
        tmRecoverable = wrapTransactionManager(tm);
        ut = new UserTransactionImpl(tm);
        tmSynchRegistry = (TransactionSynchronizationRegistry) tm;
    }

    protected static TransactionManagerImpl createTransactionManager(TransactionManagerConfiguration config) {
        if (config == null) {
            config = new TransactionManagerConfiguration();
        }
        try {
            return new TransactionManagerImpl(config.transactionTimeoutSeconds);
        } catch (XAException e) {
            // failed in recovery somewhere
            throw new RuntimeException(e.toString(), e);
        }
    }

    /**
     * User transaction that uses this container's transaction manager.
     *
     * @since 5.6
     */
    public static class UserTransactionImpl implements UserTransaction {

        protected final TransactionManager transactionManager;

        public UserTransactionImpl(TransactionManager manager) {
            transactionManager = manager;
        }

        @Override
        public int getStatus() throws SystemException {
            return transactionManager.getStatus();
        }

        @Override
        public void setRollbackOnly() throws IllegalStateException, SystemException {
            transactionManager.setRollbackOnly();
        }

        @Override
        public void setTransactionTimeout(int seconds) throws SystemException {
            transactionManager.setTransactionTimeout(seconds);
        }

        @Override
        public void begin() throws NotSupportedException, SystemException {
            transactionManager.begin();
            Tracer tracer = Tracing.getTracer();
            Span span = tracer.getCurrentSpan();
            if (!(span instanceof BlankSpan)) {
                HashMap<String, AttributeValue> map = new HashMap<>();
                map.put("tx.thread", AttributeValue.stringAttributeValue(Thread.currentThread().getName()));
                map.put("tx.id", AttributeValue.stringAttributeValue(getTransactionId()));
                span.addAnnotation("tx.begin", map);
            }
            timers.put(transactionManager.getTransaction(), transactionTimer.time());
            concurrentCount.inc();
            if (concurrentCount.getCount() > concurrentMaxCount.getCount()) {
                concurrentMaxCount.inc();
            }
        }

        protected String getTransactionId() {
            return transactionKeyAsString(((TransactionManagerImpl) transactionManager).getTransactionKey());
        }

        protected static String transactionKeyAsString(Object key) {
            if (key instanceof XidImpl) {
                byte[] globalId = ((XidImpl) key).getGlobalTransactionId();
                StringBuilder buffer = new StringBuilder();
                for (byte aGlobalId : globalId) {
                    buffer.append(Integer.toHexString(aGlobalId));
                }
                String stringKey = buffer.toString();
                // remove trailing 0
                for (int index = stringKey.length() - 1; index >= 0; index--) {
                    if (stringKey.charAt(index) != '0') {
                        return stringKey.substring(0, index + 1);
                    }
                }
                return stringKey;
            }
            return key.toString();
        }

        @Override
        public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException,
                RollbackException, SecurityException, SystemException {
            Span span = Tracing.getTracer().getCurrentSpan();
            span.addAnnotation("tx.committing");
            Transaction transaction = transactionManager.getTransaction();
            if (transaction == null) {
                throw new IllegalStateException("No transaction associated with current thread");
            }
            @SuppressWarnings("resource")
            Timer.Context timerContext = timers.remove(transaction);
            transactionManager.commit();
            if (timerContext != null) {
                long elapsed = timerContext.stop();

                HashMap<String, AttributeValue> map = new HashMap<>();
                map.put("tx.duration_ms", AttributeValue.longAttributeValue(elapsed / 1000_000));
                span.addAnnotation("tx.commited", map);
            }
            concurrentCount.dec();
            span.setStatus(Status.OK);
        }

        @Override
        public void rollback() throws IllegalStateException, SecurityException, SystemException {
            Span span = Tracing.getTracer().getCurrentSpan();
            span.addAnnotation("tx.rollbacking");
            Transaction transaction = transactionManager.getTransaction();
            if (transaction == null) {
                throw new IllegalStateException("No transaction associated with current thread");
            }
            @SuppressWarnings("resource")
            Timer.Context timerContext = timers.remove(transaction);
            transactionManager.rollback();
            concurrentCount.dec();
            rollbackCount.inc();
            if (timerContext != null) {
                long elapsed = timerContext.stop();
                span.addAnnotation("tx.rollbacked " + elapsed / 1000000 + "ms");
            } else {
                span.addAnnotation("tx.rollbacked");
            }
            span.setStatus(Status.UNKNOWN);
        }
    }

    public static class TransactionManagerConfiguration {
        public int transactionTimeoutSeconds = 600;

        public void setTransactionTimeoutSeconds(int transactionTimeoutSeconds) {
            this.transactionTimeoutSeconds = transactionTimeoutSeconds;
        }
    }

    /**
     * Wraps a transaction manager for providing a dummy recoverable interface.
     *
     * @author matic
     */
    public static class TransactionManagerWrapper implements RecoverableTransactionManager {

        protected TransactionManager tm;

        public TransactionManagerWrapper(TransactionManager tm) {
            this.tm = tm;
        }

        @Override
        public Transaction suspend() throws SystemException {
            return tm.suspend();
        }

        @Override
        public void setTransactionTimeout(int seconds) throws SystemException {
            tm.setTransactionTimeout(seconds);
        }

        @Override
        public void setRollbackOnly() throws IllegalStateException, SystemException {
            tm.setRollbackOnly();
        }

        @Override
        public void rollback() throws IllegalStateException, SecurityException, SystemException {
            tm.rollback();
        }

        @Override
        public void resume(Transaction tobj)
                throws IllegalStateException, InvalidTransactionException, SystemException {
            tm.resume(tobj);
        }

        @Override
        public int getStatus() throws SystemException {
            return tm.getStatus();
        }

        @Override
        public void commit() throws HeuristicMixedException, HeuristicRollbackException, IllegalStateException,
                RollbackException, SecurityException, SystemException {
            tm.commit();
        }

        @Override
        public void begin() throws SystemException {
            try {
                tm.begin();
            } catch (javax.transaction.NotSupportedException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void recoveryError(Exception e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void registerNamedXAResourceFactory(NamedXAResourceFactory factory) {
            if (!RecoverableTransactionManager.class.isAssignableFrom(tm.getClass())) {
                throw new UnsupportedOperationException();
            }
            ((RecoverableTransactionManager) tm).registerNamedXAResourceFactory(factory);
        }

        @Override
        public void unregisterNamedXAResourceFactory(String factory) {
            if (!RecoverableTransactionManager.class.isAssignableFrom(tm.getClass())) {
                throw new UnsupportedOperationException();
            }
            ((RecoverableTransactionManager) tm).unregisterNamedXAResourceFactory(factory);
        }

        @Override
        public Transaction getTransaction() throws SystemException {
            final Transaction tx = tm.getTransaction();
            if (tx instanceof TransactionImpl) {
                return tx;
            }
            return new TransactionImpl(null, null) {
                @Override
                public void commit() throws HeuristicMixedException, HeuristicRollbackException, RollbackException,
                        SecurityException, SystemException {
                    tx.commit();
                }

                @Override
                public void rollback() throws IllegalStateException, SystemException {
                    tx.rollback();
                }

                @Override
                public synchronized boolean enlistResource(XAResource xaRes)
                        throws IllegalStateException, RollbackException, SystemException {
                    return tx.enlistResource(xaRes);
                }

                @Override
                public synchronized boolean delistResource(XAResource xaRes, int flag)
                        throws IllegalStateException, SystemException {
                    return super.delistResource(xaRes, flag);
                }

                @Override
                public synchronized void setRollbackOnly() throws IllegalStateException {
                    try {
                        tx.setRollbackOnly();
                    } catch (SystemException e) {
                        throw new IllegalStateException(e);
                    }
                }

                @Override
                public void registerInterposedSynchronization(javax.transaction.Synchronization synchronization) {
                    try {
                        TransactionHelper.lookupSynchronizationRegistry()
                                         .registerInterposedSynchronization(synchronization);
                    } catch (NamingException e) {
                    }
                }
            };
        }
    }

    public static TransactionSynchronizationRegistry getTransactionSynchronizationRegistry() {
        return tmSynchRegistry;
    }

}
