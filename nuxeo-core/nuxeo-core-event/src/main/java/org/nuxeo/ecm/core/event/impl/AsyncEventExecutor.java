/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bogdan Stefanescu
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.event.impl;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventStats;
import org.nuxeo.ecm.core.event.ReconnectedEventBundle;
import org.nuxeo.ecm.core.event.tx.EventBundleTransactionHandler;
import org.nuxeo.runtime.api.Framework;

/**
 * ThreadPoolExecutor of listeners for event bundles.
 */
public class AsyncEventExecutor {

    private static final Log log = LogFactory.getLog(AsyncEventExecutor.class);

    public static final int QUEUE_SIZE = Integer.MAX_VALUE;

    protected final ThreadPoolExecutor executor;

    protected final BlockingQueue<Runnable> queue;

    protected final ThreadPoolExecutor mono_executor;

    protected final BlockingQueue<Runnable> mono_queue;

    protected static class ShutdownHandler implements RejectedExecutionHandler {

        protected final RejectedExecutionHandler runningHandler;

        protected ShutdownHandler(RejectedExecutionHandler handler) {
            runningHandler = handler;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                runningHandler.rejectedExecution(r, executor);
            } else {
                r.run();
            }
        }

        public static void install(ThreadPoolExecutor executor) {
            RejectedExecutionHandler runningHandler = executor.getRejectedExecutionHandler();
            RejectedExecutionHandler shutdownHandler = new ShutdownHandler(runningHandler);
            executor.setRejectedExecutionHandler(shutdownHandler);
        }

    }

    public static AsyncEventExecutor create() {
        String val = Framework.getProperty("org.nuxeo.ecm.core.event.async.poolSize");
        int poolSize = val == null ? 4 : Integer.parseInt(val);
        val = Framework.getProperty("org.nuxeo.ecm.core.event.async.maxPoolSize");
        int maxPoolSize = val == null ? 16 : Integer.parseInt(val);
        val = Framework.getProperty("org.nuxeo.ecm.core.event.async.keepAliveTime");
        int keepAliveTime = val == null ? 0 : Integer.parseInt(val);
        val = Framework.getProperty("org.nuxeo.ecm.core.event.async.queueSize");
        int queueSize = val == null ? QUEUE_SIZE : Integer.parseInt(val);
        return new AsyncEventExecutor(poolSize, maxPoolSize, keepAliveTime,
                queueSize);
    }

    public void shutdown() {
        shutdown(0);
    }

    public boolean shutdown(long timeout) {
        // schedule shutdown
        ShutdownHandler.install(executor);
        executor.shutdown();
        ShutdownHandler.install(mono_executor);
        mono_executor.shutdown();

        if (timeout <= 0) {
            return false;
        }

        // wait for asynch executor termination
        long ts = System.currentTimeMillis();
        try {
            boolean terminated =executor.awaitTermination(timeout, TimeUnit.MILLISECONDS);
            if (!terminated) {
                return false;
            }
        } catch (InterruptedException e) {
           return false;
        }

        // check remaining time
        timeout -= System.currentTimeMillis() - ts;
        if (timeout <= 0) {
            if (!mono_executor.isTerminated()) {
                return false;
            }
        }

        // wait for mono executor termination
        try {
            boolean terminated = mono_executor.awaitTermination(timeout, TimeUnit.MILLISECONDS);
            if (!terminated) {
                return false;
            }
        } catch (InterruptedException e) {
           return false;
        }

        return true;
    }

    public AsyncEventExecutor(int poolSize, int maxPoolSize, int keepAliveTime,
            int queueSize) {
        queue = new LinkedBlockingQueue<Runnable>(queueSize);
        mono_queue = new LinkedBlockingQueue<Runnable>(queueSize);
        NamedThreadFactory threadFactory = new NamedThreadFactory("Nuxeo Async Events");
        executor = new ThreadPoolExecutor(poolSize, maxPoolSize, keepAliveTime,
                TimeUnit.SECONDS, queue, threadFactory);
        mono_executor = new ThreadPoolExecutor(1, 1, keepAliveTime,
                TimeUnit.SECONDS, mono_queue, threadFactory);
    }

    public void run(List<EventListenerDescriptor> listeners, EventBundle event) {
        for (EventListenerDescriptor listener : listeners) {
            if (listener.isSingleThreaded()) {
                mono_executor.execute(new Job(listener, event));
            } else {
                executor.execute(new Job(listener, event));
            }
        }
    }

    public int getUnfinishedCount() {
        return executor.getQueue().size() + executor.getActiveCount()
                + mono_executor.getQueue().size() + mono_executor.getActiveCount();
    }

    public int getActiveCount() {
        return executor.getActiveCount() + mono_executor.getActiveCount();
    }

    public int getMaxPoolSize() {
        return executor.getMaximumPoolSize();
    }

    public void setMaxPoolSize(int maxSize) {
        int coreSize = executor.getCorePoolSize();
        if (coreSize > maxSize) {
        }
        executor.getMaximumPoolSize();
    }


    protected static class Job implements Runnable {

        protected final ReconnectedEventBundle bundle;

        protected final EventListenerDescriptor listener;

        public Job(EventListenerDescriptor listener, EventBundle bundle) {
            this.listener = listener;

            if (bundle instanceof ReconnectedEventBundle) {
                this.bundle = (ReconnectedEventBundle) bundle;
            } else {
                this.bundle = new ReconnectedEventBundleImpl(bundle,
                        listener.getName());
            }
        }

        protected EventStats getEventStats() {
            try {
                return Framework.getService(EventStats.class);
            } catch (Exception e) {
                log.warn("Failed to lookup event stats service", e);
            }
            return null;
        }

        @Override
        public void run() {
            EventBundleTransactionHandler txh = new EventBundleTransactionHandler();
            long t0 = System.currentTimeMillis();
            txh.beginNewTransaction(listener.getTransactionTimeout());
            try {
                listener.asPostCommitListener().handleEvent(bundle);
            } catch (Throwable t) {
                log.error("Failed to execute async event " + bundle.getName()
                        + " on listener " + listener.getName(), t);
                txh.setTransactionRollbackOnly();
            } finally {
                bundle.disconnect();
            }
            txh.commitOrRollbackTransaction();
            EventStats stats = getEventStats();
            if (stats != null) {
                stats.logAsyncExec(listener, System.currentTimeMillis() - t0);
            }
            log.debug("Async listener executed, commited tx");
        }

        // Thread.currentThread().interrupt();
    }

    /**
     * Creates non-daemon threads at normal priority.
     */
    public static class NamedThreadFactory implements ThreadFactory {

        private static final AtomicInteger poolNumber = new AtomicInteger();

        private final AtomicInteger threadNumber = new AtomicInteger();

        private final ThreadGroup group;

        private final String namePrefix;

        public NamedThreadFactory(String prefix) {
            SecurityManager sm = System.getSecurityManager();
            group = sm == null ? Thread.currentThread().getThreadGroup()
                    : sm.getThreadGroup();
            namePrefix = prefix + ' ' + poolNumber.incrementAndGet() + '-';
        }

        @Override
        public Thread newThread(Runnable r) {
            String name = namePrefix + threadNumber.incrementAndGet();
            Thread t = new Thread(group, r, name);
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

}
