/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.work;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.work.WorkManagerImpl.MonitoredThreadPoolExecutor;
import org.nuxeo.ecm.core.work.api.WorkQueueDescriptor;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * The implementation of a {@link WorkManager}.
 *
 * @since 5.6
 */
public class WorkManagerImpl extends DefaultComponent implements WorkManager {

    private static final Log log = LogFactory.getLog(WorkManagerImpl.class);

    public static final String DEFAULT_QUEUE_ID = "default";

    public static final String DEFAULT_CATEGORY = "default";

    protected static final String QUEUES_EP = "queues";

    protected WorkQueueDescriptorRegistry workQueueDescriptors;

    @Override
    public void activate(ComponentContext context) throws Exception {
        super.activate(context);
        workQueueDescriptors = new WorkQueueDescriptorRegistry();
        init();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        shutdown(1, TimeUnit.SECONDS);
        workQueueDescriptors = null;
        super.deactivate(context);
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (QUEUES_EP.equals(extensionPoint)) {
            WorkQueueDescriptor workQueueDescriptor = (WorkQueueDescriptor) contribution;
            log.info("Registered work queue " + workQueueDescriptor.id);
            workQueueDescriptors.addContribution(workQueueDescriptor);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (QUEUES_EP.equals(extensionPoint)) {
            WorkQueueDescriptor workQueueDescriptor = (WorkQueueDescriptor) contribution;
            log.info("Unregistered work queue " + workQueueDescriptor.id);
            workQueueDescriptors.removeContribution(workQueueDescriptor);
        }
    }

    @Override
    public List<String> getWorkQueueIds() {
        return workQueueDescriptors.getQueueIds();
    }

    @Override
    public WorkQueueDescriptor getWorkQueueDescriptor(String queueId) {
        return workQueueDescriptors.get(queueId);
    }

    // ----- WorkManager -----

    /**
     * Similar to {@link ThreadPoolExecutor.CallerRunsPolicy} but does the work
     * in the calling thread only if the executor is being shutdown.
     */
    public static class ShutdownPolicy implements RejectedExecutionHandler {

        protected final RejectedExecutionHandler handler;

        public ShutdownPolicy(RejectedExecutionHandler handler) {
            this.handler = handler;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                // normal rejection
                handler.rejectedExecution(r, executor);
            } else {
                // executor is shutdown but a task was added
                // execute it in the calling thread to not lose it
                // TODO suspend/persist it instead
                r.run();
            }
        }
    }

    /**
     * Creates non-daemon threads at normal priority.
     */
    public static class NamedThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger();

        private final ThreadGroup group;

        private final String prefix;

        public NamedThreadFactory(String prefix) {
            SecurityManager sm = System.getSecurityManager();
            group = sm == null ? Thread.currentThread().getThreadGroup()
                    : sm.getThreadGroup();
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            String name = prefix + threadNumber.incrementAndGet();
            Thread thread = new Thread(group, r, name);
            // do not set daemon
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }

    /**
     * A {@link ThreadPoolExecutor} that keeps available the list of scheduled,
     * running and completed tasks.
     * <p>
     * The methods checking the sizes are sure not to lose tasks in transit
     * between the various queues.
     *
     * @since 5.6
     */
    public static class MonitoredThreadPoolExecutor extends ThreadPoolExecutor {

        protected Object monitor = new Object();

        protected List<Work> scheduled;

        protected List<Work> running;

        protected List<Work> completed;

        public MonitoredThreadPoolExecutor(int corePoolSize,
                int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                BlockingQueue<Runnable> queue, ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, queue,
                    threadFactory);
            scheduled = new LinkedList<Work>();
            running = new LinkedList<Work>();
            completed = new LinkedList<Work>();
        }

        @Override
        public void execute(Runnable r) {
            synchronized (monitor) {
                scheduled.add((Work) r);
            }
            super.execute(r);
        }

        /**
         * Requests all running and scheduled work instances to suspend.
         */
        public void suspend() {
            synchronized (monitor) {
                for (Work work : running) {
                    work.suspend();
                }
                for (Work work : scheduled) {
                    work.suspend();
                }
            }
        }

        public void getData(long timeout, TimeUnit unit)
                throws InterruptedException {
            synchronized (monitor) {
                for (Work work : running) {
                    work.getData(timeout, unit);
                    // TODO save
                }
                for (Work work : scheduled) {
                    work.getData(timeout, unit);
                    // TODO save
                }
            }
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            synchronized (monitor) {
                scheduled.remove(r);
                running.add((Work) r);
            }
            super.beforeExecute(t, r);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            synchronized (monitor) {
                running.remove(r);
                completed.add((Work) r);
            }
        }

        /**
         * Gets the scheduled tasks. Returns a copy.
         */
        public List<Work> getScheduled() {
            synchronized (monitor) {
                return new ArrayList<Work>(scheduled);
            }
        }

        /**
         * Gets the running tasks. Returns a copy.
         */
        public List<Work> getRunning() {
            synchronized (monitor) {
                return new ArrayList<Work>(running);
            }
        }

        /**
         * Gets the completed tasks. Returns a copy.
         */
        public List<Work> getCompleted() {
            synchronized (monitor) {
                return new ArrayList<Work>(completed);
            }
        }

        /**
         * Gets the number of non-completed tasks.
         */
        public int getNonCompletedWorkSize() {
            synchronized (monitor) {
                return scheduled.size() + running.size();
            }
        }

        /**
         * Clears the completed tasks.
         */
        public void clearCompleted() {
            synchronized (monitor) {
                completed.clear();
            }
        }

        /**
         * Clears the completed tasks older than the given date.
         */
        public void clearCompleted(long completionTime) {
            if (completionTime <= 0) {
                clearCompleted();
                return;
            }
            synchronized (monitor) {
                for (Iterator<Work> it = completed.iterator(); it.hasNext();) {
                    Work work = it.next();
                    if (work.getCompletionTime() < completionTime) {
                        it.remove();
                    }
                }
            }
        }

    }

    protected static final int DEFAULT_MAX_POOL_SIZE = 4;

    protected MonitoredThreadPoolExecutor executorXXX;

    protected Map<String, MonitoredThreadPoolExecutor> executors;

    @Override
    public void init() {
        executors = new HashMap<String, MonitoredThreadPoolExecutor>();
    }

    protected synchronized MonitoredThreadPoolExecutor getExecutor(
            String queueId) {
        WorkQueueDescriptor workQueueDescriptor = workQueueDescriptors.get(queueId);
        if (workQueueDescriptor == null) {
            throw new IllegalArgumentException("No such work queue: " + queueId);
        }

        MonitoredThreadPoolExecutor executor = executors.get(queueId);
        if (executor == null) {
            ThreadFactory threadFactory = new NamedThreadFactory("Nuxeo-Work-"
                    + queueId + "-");
            int maxPoolSize = workQueueDescriptor.maxThreads;
            if (maxPoolSize <= 0) {
                maxPoolSize = DEFAULT_MAX_POOL_SIZE;
                workQueueDescriptor.maxThreads = maxPoolSize;
            }
            executor = new MonitoredThreadPoolExecutor(maxPoolSize,
                    maxPoolSize, 0, TimeUnit.SECONDS, newBlockingQueue(),
                    threadFactory);
            executors.put(queueId, executor);
        }
        return executor;
    }

    protected BlockingQueue<Runnable> newBlockingQueue() {
        return new LinkedBlockingQueue<Runnable>();
    }

    @Override
    public synchronized boolean shutdown(long timeout, TimeUnit unit)
            throws InterruptedException {
        if (executors == null) {
            return true;
        }
        Map<String, MonitoredThreadPoolExecutor> all = new HashMap<String, MonitoredThreadPoolExecutor>(
                executors);
        executors = null;

        // request orderly shutdown
        for (MonitoredThreadPoolExecutor executor : all.values()) {
            RejectedExecutionHandler old = executor.getRejectedExecutionHandler();
            executor.setRejectedExecutionHandler(new ShutdownPolicy(old));
            executor.shutdown();
        }

        long t0 = System.currentTimeMillis();
        long delay = unit.toMillis(timeout);

        // await normal termination of all work
        List<MonitoredThreadPoolExecutor> todo = new ArrayList<MonitoredThreadPoolExecutor>();
        for (MonitoredThreadPoolExecutor executor : all.values()) {
            if (!executor.awaitTermination(remainingMillis(t0, delay),
                    TimeUnit.MILLISECONDS)) {
                todo.add(executor);
            }
        }
        if (todo.isEmpty()) {
            return true;
        }

        // suspend remaining work instances
        for (MonitoredThreadPoolExecutor executor : todo) {
            executor.suspend();
        }
        for (MonitoredThreadPoolExecutor executor : todo) {
            executor.getData(remainingMillis(t0, delay),
                    TimeUnit.MILLISECONDS);
        }

        return false;
    }

    protected long remainingMillis(long t0, long delay) {
        long d = System.currentTimeMillis() - t0;
        if (d > delay) {
            return 0;
        }
        return delay - d;
    }

    @Override
    public void schedule(Work work) {
        MonitoredThreadPoolExecutor executor = getExecutor(getWorkQueueId(work));
        executor.execute(work);
    }

    protected String getWorkQueueId(Work work) {
        String category = work.getCategory();
        if (category == null) {
            category = DEFAULT_CATEGORY;
        }
        String queueId = workQueueDescriptors.getQueueId(category);
        if (queueId == null) {
            queueId = DEFAULT_QUEUE_ID;
        }
        return queueId;
    }

    @Override
    public List<Work> getScheduledWork(String queueId) {
        return getExecutor(queueId).getScheduled();
    }

    @Override
    public List<Work> getRunningWork(String queueId) {
        return getExecutor(queueId).getRunning();
    }

    @Override
    public List<Work> getCompletedWork(String queueId) {
        return getExecutor(queueId).getCompleted();
    }

    @Override
    public int getNonCompletedWorkSize(String queueId) {
        return getExecutor(queueId).getNonCompletedWorkSize();
    }

    @Override
    public void clearCompletedWork(String queueId) {
        getExecutor(queueId).clearCompleted();
    }

    @Override
    public synchronized void clearCompletedWork(long completionTime) {
        for (MonitoredThreadPoolExecutor executor : executors.values()) {
            executor.clearCompleted(completionTime);
        }
    }

}
