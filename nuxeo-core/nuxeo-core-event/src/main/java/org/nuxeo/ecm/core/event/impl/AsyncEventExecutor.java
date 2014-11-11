/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.event.impl;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.ReconnectedEventBundle;
import org.nuxeo.ecm.core.event.jmx.EventStatsHolder;
import org.nuxeo.ecm.core.event.tx.EventBundleTransactionHandler;
import org.nuxeo.runtime.api.Framework;

/**
 * ThreadPoolExecutor of listeners for event bundles.
 *
 * @author Bogdan Stefanescu
 * @author Thierry Delprat
 * @author Florent Guillaume
 */
public class AsyncEventExecutor {

    private static final Log log = LogFactory.getLog(AsyncEventExecutor.class);

    public static final int QUEUE_SIZE = Integer.MAX_VALUE;

    protected final ThreadPoolExecutor executor;

    protected final BlockingQueue<Runnable> queue;

    protected final ThreadPoolExecutor mono_executor;

    protected final BlockingQueue<Runnable> mono_queue;

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
        // The following avoids incorrect counts, because ThreadPool workers
        // started normally may be holding on to a firstTask about to start that
        // isn't accounted for anywhere (getActiveCount doesn't return it).
        executor.prestartAllCoreThreads();
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
                this.bundle = new ReconnectedEventBundleImpl(bundle);
            }
        }

        public void run() {
            EventBundleTransactionHandler txh = new EventBundleTransactionHandler();
            try {
                long t0 = System.currentTimeMillis();
                txh.beginNewTransaction(listener.getTransactionTimeout());
                listener.asPostCommitListener().handleEvent(bundle);
                txh.commitOrRollbackTransaction();
                EventStatsHolder.logAsyncExec(listener, System.currentTimeMillis()-t0);
                log.debug("Async listener executed, commited tx");
            } catch (Throwable t) {
                log.error("Failed to execute async event " + bundle.getName()
                        + " on listener " + listener.getName(), t);
                txh.rollbackTransaction();
            } finally {
                bundle.disconnect();
            }

            //Thread.currentThread().interrupt();
        }
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

        public Thread newThread(Runnable r) {
            String name = namePrefix + threadNumber.incrementAndGet();
            Thread t = new Thread(group, r, name);
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

}
