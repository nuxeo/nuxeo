/*
 * (C) Copyright 2012-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */
package org.nuxeo.ecm.core.work;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.logging.SequenceTracer;
import org.nuxeo.ecm.core.event.EventServiceComponent;
import org.nuxeo.ecm.core.work.WorkQueuing.Listener;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkQueueDescriptor;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;
import org.nuxeo.ecm.core.work.api.WorkQueuingDescriptor;
import org.nuxeo.ecm.core.work.api.WorkSchedulePath;
import org.nuxeo.runtime.RuntimeServiceEvent;
import org.nuxeo.runtime.RuntimeServiceListener;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

/**
 * The implementation of a {@link WorkManager}. This delegates the queuing implementation to a {@link WorkQueuing}
 * implementation.
 *
 * @since 5.6
 */
public class WorkManagerImpl extends DefaultComponent implements WorkManager {

    public static final String NAME = "org.nuxeo.ecm.core.work.service";

    private static final Log log = LogFactory.getLog(WorkManagerImpl.class);

    protected static final String QUEUES_EP = "queues";

    protected static final String IMPL_EP = "implementation";

    public static final String DEFAULT_QUEUE_ID = "default";

    public static final String DEFAULT_CATEGORY = "default";

    protected static final String THREAD_PREFIX = "Nuxeo-Work-";

    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    // @GuardedBy("itself")
    protected final WorkQueueRegistry workQueueConfig = new WorkQueueRegistry();

    protected final WorkQueuingRegistry workQueuingConfig = new WorkQueuingRegistry();

    // used synchronized
    protected final Map<String, WorkThreadPoolExecutor> executors = new HashMap<>();

    protected WorkQueuing queuing;

    /**
     * Simple synchronizer to wake up when an in-JVM work is completed. Does not wake up on work completion from another
     * node in cluster mode.
     */
    protected class WorkCompletionSynchronizer {
        final protected ReentrantLock lock = new ReentrantLock();

        final protected Condition condition = lock.newCondition();

        protected boolean waitForCompletedWork(long timeMs) throws InterruptedException {
            lock.lock();
            try {
                return condition.await(timeMs, TimeUnit.MILLISECONDS);
            } finally {
                lock.unlock();
            }
        }

        protected void signalCompletedWork() {
            lock.lock();
            try {
                condition.signalAll();
            } finally {
                lock.unlock();
            }
        }

    }

    protected WorkCompletionSynchronizer completionSynchronizer;

    @Override
    public void activate(ComponentContext context) {
        Framework.addListener(new ShutdownListener());
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (QUEUES_EP.equals(extensionPoint)) {
            registerWorkQueueDescriptor((WorkQueueDescriptor) contribution);
        } else if (IMPL_EP.equals(extensionPoint)) {
            registerWorkQueuingDescriptor((WorkQueuingDescriptor) contribution);
        } else {
            throw new RuntimeException("Unknown extension point: " + extensionPoint);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (QUEUES_EP.equals(extensionPoint)) {
            unregisterWorkQueueDescriptor((WorkQueueDescriptor) contribution);
        } else if (IMPL_EP.equals(extensionPoint)) {
            unregisterWorkQueuingDescriptor((WorkQueuingDescriptor) contribution);
        } else {
            throw new RuntimeException("Unknown extension point: " + extensionPoint);
        }
    }

    void registerWorkQueueDescriptor(WorkQueueDescriptor workQueueDescriptor) {
        String queueId = workQueueDescriptor.id;
        if (WorkQueueDescriptor.ALL_QUEUES.equals(queueId)) {
            Boolean processing = workQueueDescriptor.processing;
            if (processing == null) {
                log.error("Ignoring work queue descriptor " + WorkQueueDescriptor.ALL_QUEUES
                        + " with no processing/queuing");
                return;
            }
            String what = " processing=" + processing;
            what += queuing == null ? "" : (" queuing=" + queuing);
            log.info("Setting on all work queues:" + what);
            // activate/deactivate processing/queuing on all queues
            List<String> queueIds = new ArrayList<>(workQueueConfig.getQueueIds()); // copy
            for (String id : queueIds) {
                // add an updated contribution redefining processing/queuing
                WorkQueueDescriptor wqd = new WorkQueueDescriptor();
                wqd.id = id;
                wqd.processing = processing;
                registerWorkQueueDescriptor(wqd);
            }
            return;
        }
        workQueueConfig.addContribution(workQueueDescriptor);
        WorkQueueDescriptor wqd = workQueueConfig.get(queueId);
        log.info("Registered work queue " + queueId + " " + wqd.toString());
    }

    void unregisterWorkQueueDescriptor(WorkQueueDescriptor workQueueDescriptor) {
        String id = workQueueDescriptor.id;
        if (WorkQueueDescriptor.ALL_QUEUES.equals(id)) {
            return;
        }
        workQueueConfig.removeContribution(workQueueDescriptor);
        log.info("Unregistered work queue " + id);
    }

    void initializeQueue(WorkQueueDescriptor config) {
        if (WorkQueueDescriptor.ALL_QUEUES.equals(config.id)) {
            throw new IllegalArgumentException("cannot initialize all queues");
        }
        if (queuing.getQueue(config.id) != null) {
            throw new IllegalStateException("work queue " + config.id + " is already initialized");
        }
        if (executors.containsKey(config.id)) {
            throw new IllegalStateException("work queue " + config.id + " already have an executor");
        }
        NuxeoBlockingQueue queue = queuing.init(config);
        ThreadFactory threadFactory = new NamedThreadFactory(THREAD_PREFIX + config.id + "-");
        int maxPoolSize = config.getMaxThreads();
        WorkThreadPoolExecutor executor = new WorkThreadPoolExecutor(maxPoolSize, maxPoolSize, 0, TimeUnit.SECONDS,
                queue, threadFactory);
        // prestart all core threads so that direct additions to the queue
        // (from another Nuxeo instance) can be seen
        executor.prestartAllCoreThreads();
        executors.put(config.id, executor);
        log.info("Initialized work queue " + config.id + " " + config.toEffectiveString());
    }

    void activateQueue(WorkQueueDescriptor config) {
        if (WorkQueueDescriptor.ALL_QUEUES.equals(config.id)) {
            throw new IllegalArgumentException("cannot activate all queues");
        }
        queuing.setActive(config.id, config.isProcessingEnabled());
        log.info("Activated work queue " + config.id + " " + config.toEffectiveString());
    }

    void deactivateQueue(WorkQueueDescriptor config) {
        if (WorkQueueDescriptor.ALL_QUEUES.equals(config.id)) {
            throw new IllegalArgumentException("cannot deactivate all queues");
        }
        queuing.setActive(config.id, false);
        log.info("Deactivated work queue " + config.id);
    }

    void registerWorkQueuingDescriptor(WorkQueuingDescriptor descr) {
        workQueuingConfig.addContribution(descr);
    }

    void unregisterWorkQueuingDescriptor(WorkQueuingDescriptor descr) {
        workQueuingConfig.removeContribution(descr);
    }

    protected WorkQueuing newWorkQueuing(Class<? extends WorkQueuing> klass) {
        try {
            return klass.getDeclaredConstructor(Listener.class).newInstance(Listener.lookupListener());
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isQueuingEnabled(String queueId) {
        WorkQueueDescriptor wqd = getWorkQueueDescriptor(queueId);
        return wqd != null && wqd.isQueuingEnabled();
    }

    @Override
    public void enableProcessing(boolean value) {
        for (String queueId : workQueueConfig.getQueueIds()) {
            queuing.getQueue(queueId).setActive(value);
        }
    }

    @Override
    public void enableProcessing(String queueId, boolean value) throws InterruptedException {
        WorkQueueDescriptor config = workQueueConfig.get(queueId);
        if (config == null) {
            throw new IllegalArgumentException("no such queue " + queueId);
        }
        if (!value) {
            deactivateQueue(config);
        } else {
            activateQueue(config);
        }
    }

    @Override
    public boolean isProcessingEnabled() {
        for (String queueId : workQueueConfig.getQueueIds()) {
            if (queuing.getQueue(queueId).active) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isProcessingEnabled(String queueId) {
        if (queueId == null) {
            return isProcessingEnabled();
        }
        return queuing.getQueue(queueId).active;
    }

    // ----- WorkManager -----

    @Override
    public List<String> getWorkQueueIds() {
        synchronized (workQueueConfig) {
            return workQueueConfig.getQueueIds();
        }
    }

    @Override
    public WorkQueueDescriptor getWorkQueueDescriptor(String queueId) {
        synchronized (workQueueConfig) {
            return workQueueConfig.get(queueId);
        }
    }

    @Override
    public String getCategoryQueueId(String category) {
        if (category == null) {
            category = DEFAULT_CATEGORY;
        }
        String queueId = workQueueConfig.getQueueId(category);
        if (queueId == null) {
            queueId = DEFAULT_QUEUE_ID;
        }
        return queueId;
    }

    @Override
    public int getApplicationStartedOrder() {
        return EventServiceComponent.APPLICATION_STARTED_ORDER - 1;
    }

    @Override
    public void applicationStarted(ComponentContext context) {
        init();
    }

    protected volatile boolean started = false;

    protected volatile boolean shutdownInProgress = false;

    @Override
    public void init() {
        if (started) {
            return;
        }
        synchronized (this) {
            if (started) {
                return;
            }
            queuing = newWorkQueuing(workQueuingConfig.klass);
            completionSynchronizer = new WorkCompletionSynchronizer();
            started = true;
            workQueueConfig.index();
            for (String id : workQueueConfig.getQueueIds()) {
                initializeQueue(workQueueConfig.get(id));
            }
            for (String id : workQueueConfig.getQueueIds()) {
                activateQueue(workQueueConfig.get(id));
            }
        }
    }

    protected WorkThreadPoolExecutor getExecutor(String queueId) {
        if (!started) {
            if (Framework.isTestModeSet() && !Framework.getRuntime().isShuttingDown()) {
                LogFactory.getLog(WorkManagerImpl.class).warn("Lazy starting of work manager in test mode");
                init();
            } else {
                throw new IllegalStateException("Work manager not started, could not access to executors");
            }
        }
        WorkQueueDescriptor workQueueDescriptor;
        synchronized (workQueueConfig) {
            workQueueDescriptor = workQueueConfig.get(queueId);
        }
        if (workQueueDescriptor == null) {
            throw new IllegalArgumentException("No such work queue: " + queueId);
        }

        return executors.get(queueId);
    }

    @Override
    public boolean shutdownQueue(String queueId, long timeout, TimeUnit unit) throws InterruptedException {
        WorkThreadPoolExecutor executor = getExecutor(queueId);
        return shutdownExecutors(Collections.singleton(executor), timeout, unit);
    }

    protected boolean shutdownExecutors(Collection<WorkThreadPoolExecutor> list, long timeout, TimeUnit unit)
            throws InterruptedException {
        // mark executors as shutting down
        for (WorkThreadPoolExecutor executor : list) {
            executor.shutdownAndSuspend();
        }
        timeout = TimeUnit.MILLISECONDS.convert(timeout, unit);
        // wait until threads termination
        for (WorkThreadPoolExecutor executor : list) {
            long t0 = System.currentTimeMillis();
            if (!executor.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                return false;
            }
            timeout -= unit.convert(System.currentTimeMillis() - t0, TimeUnit.MILLISECONDS);
        }
        return true;
    }

    protected long remainingMillis(long t0, long delay) {
        long d = System.currentTimeMillis() - t0;
        if (d > delay) {
            return 0;
        }
        return delay - d;
    }

    protected synchronized void removeExecutor(String queueId) {
        executors.remove(queueId);
    }

    @Override
    public boolean shutdown(long timeout, TimeUnit unit) throws InterruptedException {
        shutdownInProgress = true;
        try {
            return shutdownExecutors(new ArrayList<>(executors.values()), timeout, unit);
        } finally {
            shutdownInProgress = false;
            started = false;
        }
    }

    protected class ShutdownListener implements RuntimeServiceListener {
        @Override
        public void handleEvent(RuntimeServiceEvent event) {
            if (RuntimeServiceEvent.RUNTIME_ABOUT_TO_STOP != event.id) {
                return;
            }
            Framework.removeListener(this);
            try {
                if (!shutdown(10, TimeUnit.SECONDS)) {
                    log.error("Some processors are still active");
                }
            } catch (InterruptedException cause) {
                Thread.currentThread().interrupt();
                log.error("Interrupted during works manager shutdown, continuing runtime shutdown", cause);
            }
        }
    }

    /**
     * A work instance and how to schedule it, for schedule-after-commit.
     *
     * @since 5.8
     */
    public class WorkScheduling implements Synchronization {
        public final Work work;

        public final Scheduling scheduling;

        public WorkScheduling(Work work, Scheduling scheduling) {
            this.work = work;
            this.scheduling = scheduling;
        }

        @Override
        public void beforeCompletion() {
        }

        @Override
        public void afterCompletion(int status) {
            if (status == Status.STATUS_COMMITTED) {
                schedule(work, scheduling, false);
            } else if (status == Status.STATUS_ROLLEDBACK) {
                work.setWorkInstanceState(State.UNKNOWN);
            } else {
                throw new IllegalArgumentException("Unsupported transaction status " + status);
            }
        }
    }

    /**
     * Creates non-daemon threads at normal priority.
     */
    private static class NamedThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger();

        private final ThreadGroup group;

        private final String prefix;

        public NamedThreadFactory(String prefix) {
            SecurityManager sm = System.getSecurityManager();
            group = sm == null ? Thread.currentThread().getThreadGroup() : sm.getThreadGroup();
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            String name = prefix + threadNumber.incrementAndGet();
            Thread thread = new Thread(group, r, name);
            // do not set daemon
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.setUncaughtExceptionHandler((t,
                    e) -> LogFactory.getLog(WorkManagerImpl.class).error("Uncaught error on thread " + t.getName(), e));
            return thread;
        }
    }

    /**
     * A {@link ThreadPoolExecutor} that keeps available the list of running tasks.
     * <p>
     * Completed tasks are passed to another queue.
     * <p>
     * The scheduled queue and completed list are passed as arguments and can have different implementations (in-memory,
     * persisted, etc).
     *
     * @since 5.6
     */
    protected class WorkThreadPoolExecutor extends ThreadPoolExecutor {

        protected final String queueId;

        /**
         * List of running Work instances, in order to be able to interrupt them if requested.
         */
        // @GuardedBy("itself")
        protected final ConcurrentLinkedQueue<Work> running;

        // metrics, in cluster mode these counters must be aggregated, no logic should rely on them
        // Number of work scheduled by this instance
        protected final Counter scheduledCount;

        // Number of work currently running on this instance
        protected final Counter runningCount;

        // Number of work completed by this instance
        protected final Counter completedCount;

        protected final Timer workTimer;

        protected WorkThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                NuxeoBlockingQueue queue, ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, queue, threadFactory);
            queueId = queue.queueId;
            running = new ConcurrentLinkedQueue<>();
            // init metrics
            scheduledCount = registry.counter(MetricRegistry.name("nuxeo", "works", queueId, "scheduled", "count"));
            runningCount = registry.counter(MetricRegistry.name("nuxeo", "works", queueId, "running"));
            completedCount = registry.counter(MetricRegistry.name("nuxeo", "works", queueId, "completed"));
            workTimer = registry.timer(MetricRegistry.name("nuxeo", "works", queueId, "total"));
        }

        public int getScheduledOrRunningSize() {
            int ret = 0;
            for (String queueId : getWorkQueueIds()) {
                ret += getQueueSize(queueId, null);
            }
            return ret;
        }

        @Override
        public void execute(Runnable r) {
            throw new UnsupportedOperationException("use other api");
        }

        /**
         * Executes the given task sometime in the future.
         *
         * @param work the work to execute
         * @see #execute(Runnable)
         */
        public void execute(Work work) {
            scheduledCount.inc();
            submit(work);
        }

        /**
         * go through the queue instead of using super.execute which may skip the queue and hand off to a thread
         * directly
         */
        protected void submit(Work work) throws RuntimeException {
            queuing.workSchedule(queueId, work);
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            Work work = WorkHolder.getWork(r);
            if (isShutdown()) {
                work.setWorkInstanceState(State.SCHEDULED);
                queuing.workReschedule(queueId, work);
                throw new RejectedExecutionException(queueId + " was shutdown, rescheduled " + work);
            }
            work.setWorkInstanceState(State.RUNNING);
            queuing.workRunning(queueId, work);
            running.add(work);
            runningCount.inc();
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            Work work = WorkHolder.getWork(r);
            try {
                if (work.isSuspending()) {
                    log.trace(work + " is suspending, giving up");
                    return;
                }
                if (isShutdown()) {
                    log.trace("rescheduling " + work.getId(), t);
                    work.setWorkInstanceState(State.SCHEDULED);
                    queuing.workReschedule(queueId, work);
                    return;
                }
                work.setWorkInstanceState(State.UNKNOWN);
                queuing.workCompleted(queueId, work);
            } finally {
                running.remove(work);
                runningCount.dec();
                completedCount.inc();
                workTimer.update(work.getCompletionTime() - work.getStartTime(), TimeUnit.MILLISECONDS);
                completionSynchronizer.signalCompletedWork();
            }
        }

        /**
         * Initiates a shutdown of this executor and asks for work instances to suspend themselves.
         *
         * @throws InterruptedException
         */
        public void shutdownAndSuspend() throws InterruptedException {
            try {
                // don't consume the queue anymore
                queuing.setActive(queueId, false);
                // suspend all running work
                for (Work work : running) {
                    work.setWorkInstanceSuspending();
                    log.trace("suspending and rescheduling " + work.getId());
                    work.setWorkInstanceState(State.SCHEDULED);
                    queuing.workReschedule(queueId, work);
                }
                shutdownNow();
            } finally {
                executors.remove(queueId);
            }
        }

        public void removeScheduled(String workId) {
            queuing.removeScheduled(queueId, workId);
        }

    }

    @Override
    public void schedule(Work work) {
        schedule(work, Scheduling.ENQUEUE, false);
    }

    @Override
    public void schedule(Work work, boolean afterCommit) {
        schedule(work, Scheduling.ENQUEUE, afterCommit);
    }

    @Override
    public void schedule(Work work, Scheduling scheduling) {
        schedule(work, scheduling, false);
    }

    @Override
    public void schedule(Work work, Scheduling scheduling, boolean afterCommit) {
        String workId = work.getId();
        String queueId = getCategoryQueueId(work.getCategory());
        if (!isQueuingEnabled(queueId)) {
            return;
        }
        if (afterCommit && scheduleAfterCommit(work, scheduling)) {
            return;
        }
        work.setWorkInstanceState(State.SCHEDULED);
        WorkSchedulePath.newInstance(work);
        switch (scheduling) {
        case ENQUEUE:
            break;
        case CANCEL_SCHEDULED:
            getExecutor(queueId).removeScheduled(workId);
            break;
        case IF_NOT_SCHEDULED:
        case IF_NOT_RUNNING_OR_SCHEDULED:
            // TODO disabled for now because hasWorkInState uses isScheduled
            // which is buggy
            boolean disabled = Boolean.TRUE.booleanValue();
            if (!disabled && hasWorkInState(workId, scheduling.state)) {
                if (log.isDebugEnabled()) {
                    log.debug("Canceling schedule because found: " + scheduling);
                }
                return;

            }
            break;

        }
        queuing.workSchedule(queueId, work);
    }

    /**
     * Schedule after commit. Returns {@code false} if impossible (no transaction or transaction manager).
     *
     * @since 5.8
     */
    protected boolean scheduleAfterCommit(Work work, Scheduling scheduling) {
        TransactionManager transactionManager;
        try {
            transactionManager = TransactionHelper.lookupTransactionManager();
        } catch (NamingException e) {
            transactionManager = null;
        }
        if (transactionManager == null) {
            if (log.isDebugEnabled()) {
                log.debug("Not scheduling work after commit because of missing transaction manager: " + work);
            }
            return false;
        }
        try {
            Transaction transaction = transactionManager.getTransaction();
            if (transaction == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Not scheduling work after commit because of missing transaction: " + work);
                }
                return false;
            }
            int status = transaction.getStatus();
            if (status == Status.STATUS_ACTIVE) {
                if (log.isDebugEnabled()) {
                    log.debug("Scheduling work after commit: " + work);
                }
                transaction.registerSynchronization(new WorkScheduling(work, scheduling));
                return true;
            } else if (status == Status.STATUS_COMMITTED) {
                // called in afterCompletion, we can schedule immediately
                if (log.isDebugEnabled()) {
                    log.debug("Scheduling work immediately: " + work);
                }
                return false;
            } else if (status == Status.STATUS_MARKED_ROLLBACK) {
                if (log.isDebugEnabled()) {
                    log.debug("Cancelling schedule because transaction marked rollback-only: " + work);
                }
                return true;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Not scheduling work after commit because transaction is in status " + status + ": "
                            + work);
                }
                return false;
            }
        } catch (SystemException | RollbackException e) {
            log.error("Cannot schedule after commit", e);
            return false;
        }
    }

    @Override
    public Work find(String workId, State state) {
        return queuing.find(workId, state);
    }

    /**
     * @param state SCHEDULED, RUNNING or null for both
     */
    protected boolean hasWorkInState(String workId, State state) {
        return queuing.isWorkInState(workId, state);
    }

    @Override
    public State getWorkState(String workId) {
        return queuing.getWorkState(workId);
    }

    @Override
    public List<Work> listWork(String queueId, State state) {
        // don't return scheduled after commit
        return queuing.listWork(queueId, state);
    }

    @Override
    public List<String> listWorkIds(String queueId, State state) {
        return queuing.listWorkIds(queueId, state);
    }

    @Override
    public WorkQueueMetrics getMetrics(String queueId) {
        return queuing.metrics(queueId);
    }

    @Override
    public int getQueueSize(String queueId, State state) {
        WorkQueueMetrics metrics = getMetrics(queueId);
        if (state == null) {
            return metrics.scheduled.intValue() + metrics.running.intValue();
        }
        if (state == State.SCHEDULED) {
            return metrics.scheduled.intValue();
        } else if (state == State.RUNNING) {
            return metrics.running.intValue();
        } else {
            throw new IllegalArgumentException(String.valueOf(state));
        }
    }

    @Override
    public boolean awaitCompletion(long duration, TimeUnit unit) throws InterruptedException {
        return awaitCompletion(null, duration, unit);
    }

    @Override
    public boolean awaitCompletion(String queueId, long duration, TimeUnit unit) throws InterruptedException {
        if (!isStarted()) {
            return true;
        }
        SequenceTracer.start("awaitCompletion on " + ((queueId == null) ? "all queues" : queueId));
        long durationInMs = TimeUnit.MILLISECONDS.convert(duration, unit);
        long deadline = getTimestampAfter(durationInMs);
        int pause = (int) Math.min(duration, 500L);
        log.debug("awaitForCompletion " + durationInMs + " ms");
        do {
            if (noScheduledOrRunningWork(queueId)) {
                completionSynchronizer.signalCompletedWork();
                SequenceTracer.stop("done");
                return true;
            }
            completionSynchronizer.waitForCompletedWork(pause);
        } while (System.currentTimeMillis() < deadline);
        log.info("awaitCompletion timeout after " + durationInMs + " ms");
        SequenceTracer.destroy("timeout after " + durationInMs + " ms");
        return false;
    }

    protected long getTimestampAfter(long durationInMs) {
        long ret = System.currentTimeMillis() + durationInMs;
        if (ret < 0) {
            ret = Long.MAX_VALUE;
        }
        return ret;
    }

    protected boolean noScheduledOrRunningWork(String queueId) {
        if (queueId == null) {
            for (String id : getWorkQueueIds()) {
                if (!noScheduledOrRunningWork(id)) {
                    return false;
                }
            }
            return true;
        }
        if (!isProcessingEnabled(queueId)) {
            return getExecutor(queueId).runningCount.getCount() == 0L;
        }
        if (getQueueSize(queueId, null) > 0) {
            if (log.isTraceEnabled()) {
                log.trace(queueId + " not empty, sched: " + getQueueSize(queueId, State.SCHEDULED) + ", running: "
                        + getQueueSize(queueId, State.RUNNING));
            }
            return false;
        }
        if (log.isTraceEnabled()) {
            log.trace(queueId + " is completed");
        }
        return true;
    }

    @Override
    public boolean isStarted() {
        return started && !shutdownInProgress;
    }

}
