/*
 * (C) Copyright 2012-2017 Nuxeo (http://nuxeo.com/) and others.
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

import static org.nuxeo.ecm.core.work.api.WorkQueueDescriptor.ALL_QUEUES;

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
import java.util.stream.Collectors;

import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.logging.SequenceTracer;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.EventServiceComponent;
import org.nuxeo.ecm.core.work.WorkQueuing.Listener;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.Work.State;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.ecm.core.work.api.WorkQueueDescriptor;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;
import org.nuxeo.ecm.core.work.api.WorkQueuingDescriptor;
import org.nuxeo.ecm.core.work.api.WorkSchedulePath;
import org.nuxeo.lib.stream.codec.AvroMessageCodec;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.MetricsService;
import org.nuxeo.runtime.metrics.NuxeoMetricSet;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.transaction.TransactionHelper;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;
import io.dropwizard.metrics5.Timer;

/**
 * The implementation of a {@link WorkManager}. This delegates the queuing implementation to a {@link WorkQueuing}
 * implementation.
 *
 * @since 5.6
 */
public class WorkManagerImpl extends DefaultComponent implements WorkManager {

    private static final Logger log = LogManager.getLogger(WorkManagerImpl.class);

    public static final String NAME = "org.nuxeo.ecm.core.work.service";

    protected static final String QUEUES_EP = "queues";

    protected static final String IMPL_EP = "implementation";

    public static final String DEFAULT_QUEUE_ID = "default";

    public static final String DEFAULT_CATEGORY = "default";

    protected static final String THREAD_PREFIX = "Nuxeo-Work-";

    /**
     * @since 10.2
     */
    public static final String SHUTDOWN_DELAY_MS_KEY = "nuxeo.work.shutdown.delay.ms";

    /**
     * @since 11.1
     */
    public static final String WORKMANAGER_PROCESSING_DISABLE = "nuxeo.work.processing.disable";

    /**
     * The dead letter queue stream name.
     *
     * @since 11.1
     */
    public static final Name DEAD_LETTER_QUEUE = Name.ofUrn("work/dlq");

    // @since 11.1
    public static final Codec<Record> DEAD_LETTER_QUEUE_CODEC = new AvroMessageCodec<>(Record.class);

    // @since 11.1
    protected static final String GLOBAL_METRIC_PREFIX = "nuxeo.works.global.queue";

    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(MetricsService.class.getName());

    // used synchronized
    protected final Map<String, WorkThreadPoolExecutor> executors = new HashMap<>();

    protected final Map<String, String> categoryToQueueId = new HashMap<>();

    protected WorkQueuing queuing;

    protected boolean active = true;

    public WorkManagerImpl() {
        // make subclasses (StreamWorkManager) use the same registry
        super.setName(NAME);
    }

    @Override
    public void setName(String name) {
        // do nothing: name is hardcoded and does not depend on XML configuration
    }

    /**
     * Simple synchronizer to wake up when an in-JVM work is completed. Does not wake up on work completion from another
     * node in cluster mode.
     */
    protected class WorkCompletionSynchronizer {
        protected final ReentrantLock lock = new ReentrantLock();

        protected final Condition condition = lock.newCondition();

        protected boolean waitForCompletedWork(long timeMs) throws InterruptedException {
            lock.lock();
            try {
                return condition.await(timeMs, TimeUnit.MILLISECONDS); // caller is looping
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
    public void registerContribution(Object contribution, String xp, ComponentInstance component) {
        if (QUEUES_EP.equals(xp)) {
            WorkQueueDescriptor descriptor = (WorkQueueDescriptor) contribution;
            if (ALL_QUEUES.equals(descriptor.getId())) {
                Boolean processing = descriptor.processing;
                if (processing == null) {
                    log.error("Ignoring work queue descriptor {} with no processing/queuing", ALL_QUEUES);
                    return;
                }
                log.info("Setting on all work queues:{}",
                        () -> " processing=" + processing + (queuing == null ? "" : " queuing=" + queuing));
                // activate/deactivate processing on all queues
                getDescriptors(QUEUES_EP).forEach(d -> {
                    WorkQueueDescriptor wqd = new WorkQueueDescriptor();
                    wqd.id = d.getId();
                    wqd.processing = processing;
                    register(QUEUES_EP, wqd);
                });
            } else {
                register(QUEUES_EP, descriptor);
            }
        } else {
            super.registerContribution(contribution, xp, component);
        }
    }

    void initializeQueue(WorkQueueDescriptor config) {
        if (ALL_QUEUES.equals(config.id)) {
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
        log.info("Initialized work queue {}, {}", config.id, config);
    }

    void activateQueue(WorkQueueDescriptor config) {
        if (ALL_QUEUES.equals(config.id)) {
            throw new IllegalArgumentException("cannot activate all queues");
        }
        queuing.setActive(config.id, config.isProcessingEnabled());
        log.info("Activated work queue {}, {}", config.id, config);
        // Enable metrics
        if (config.isProcessingEnabled()) {
            activateQueueMetrics(config.id);
        }
    }

    void deactivateQueue(WorkQueueDescriptor config) {
        if (ALL_QUEUES.equals(config.id)) {
            throw new IllegalArgumentException("cannot deactivate all queues");
        }
        // Disable metrics
        if (config.isProcessingEnabled()) {
            deactivateQueueMetrics(config.id);
        }
        queuing.setActive(config.id, false);
        log.info("Deactivated work queue {}", config.id);
    }

    void activateQueueMetrics(String queueId) {
        NuxeoMetricSet queueMetrics = new NuxeoMetricSet(
                MetricName.build(GLOBAL_METRIC_PREFIX).tagged("queue", queueId));
        queueMetrics.putGauge(() -> getMetrics(queueId).scheduled, "scheduled");
        queueMetrics.putGauge(() -> getMetrics(queueId).running, "running");
        queueMetrics.putGauge(() -> getMetrics(queueId).completed, "completed");
        queueMetrics.putGauge(() -> getMetrics(queueId).canceled, "canceled");
        registry.registerAll(queueMetrics);
    }

    void deactivateQueueMetrics(String queueId) {
        String queueMetricsName = MetricName.build(GLOBAL_METRIC_PREFIX).tagged("queue", queueId).getKey();
        registry.removeMatching((name, metric) -> name.getKey().startsWith(queueMetricsName));
    }

    @Override
    public boolean isQueuingEnabled(String queueId) {
        WorkQueueDescriptor wqd = getWorkQueueDescriptor(queueId);
        return wqd != null && wqd.isQueuingEnabled();
    }

    @Override
    public void enableProcessing(boolean value) {
        getDescriptors(QUEUES_EP).forEach(d -> enableProcessing(d.getId(), value));
    }

    @Override
    public void enableProcessing(String queueId, boolean value) {
        WorkQueueDescriptor config = getDescriptor(QUEUES_EP, queueId);
        if (config == null) {
            throw new IllegalArgumentException("no such queue " + queueId);
        }
        if (!value) {
            if (!queuing.supportsProcessingDisabling()) {
                log.error("Attempting to disable works processing on a WorkQueuing instance that does not support it. "
                        + "Works will still be processed. "
                        + "Disabling works processing to manage distribution finely can be done using Redis or Stream implementations.");
            }
            deactivateQueue(config);
        } else {
            activateQueue(config);
        }
    }

    @Override
    public boolean isProcessingEnabled() {
        if (Boolean.parseBoolean(Framework.getProperty(WORKMANAGER_PROCESSING_DISABLE, "false"))) {
            return false;
        }
        for (Descriptor d : getDescriptors(QUEUES_EP)) {
            if (queuing.getQueue(d.getId()).active) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isProcessingEnabled(String queueId) {
        if (Boolean.parseBoolean(Framework.getProperty(WORKMANAGER_PROCESSING_DISABLE, "false"))) {
            return false;
        }
        if (queueId == null) {
            return isProcessingEnabled();
        }
        return queuing.getQueue(queueId).active;
    }

    // ----- WorkManager -----

    @Override
    public List<String> getWorkQueueIds() {
        synchronized (getRegistry()) {
            return getDescriptors(QUEUES_EP).stream().map(Descriptor::getId).collect(Collectors.toList());
        }
    }

    @Override
    public WorkQueueDescriptor getWorkQueueDescriptor(String queueId) {
        synchronized (getRegistry()) {
            return getDescriptor(QUEUES_EP, queueId);
        }
    }

    @Override
    public String getCategoryQueueId(String category) {
        if (category == null) {
            category = DEFAULT_CATEGORY;
        }
        String queueId = categoryToQueueId.get(category);
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
    public void start(ComponentContext context) {
        super.start(context);
        initDeadLetterQueueStream();
        init();
    }

    protected void initDeadLetterQueueStream() {
        StreamService service = Framework.getService(StreamService.class);
        if (service == null) {
            return;
        }
        try {
            org.nuxeo.lib.stream.log.LogManager logManager = service.getLogManager();
            if (!logManager.exists(DEAD_LETTER_QUEUE)) {
                log.info("Initializing dead letter queue to store Work in failure");
                logManager.createIfNotExists(DEAD_LETTER_QUEUE, 1);
            }
            // Needed to initialize the appender with the proper codec
            logManager.getAppender(DEAD_LETTER_QUEUE, DEAD_LETTER_QUEUE_CODEC);
        } catch (IllegalArgumentException e) {
            log.info("No default LogManager found, there will be no dead letter queuing for Work in failure.");
        }
    }

    protected volatile boolean started = false;

    protected volatile boolean shutdownInProgress = false;

    @Override
    public void init() {
        if (started || !active) {
            return;
        }
        synchronized (this) {
            if (started) {
                return;
            }
            WorkQueuingDescriptor d = getDescriptor(IMPL_EP, Descriptor.UNIQUE_DESCRIPTOR_ID);
            try {
                queuing = d.klass.getDeclaredConstructor(Listener.class).newInstance(Listener.lookupListener());
            } catch (ReflectiveOperationException | SecurityException e) {
                throw new RuntimeException(e);
            }
            completionSynchronizer = new WorkCompletionSynchronizer();
            started = true;
            index();
            List<WorkQueueDescriptor> descriptors = getDescriptors(QUEUES_EP);
            for (WorkQueueDescriptor descriptor : descriptors) {
                initializeQueue(descriptor);
            }

            Framework.getRuntime().getComponentManager().addListener(new ComponentManager.Listener() {
                @Override
                public void beforeStop(ComponentManager mgr, boolean isStandby) {
                    List<WorkQueueDescriptor> descriptors = getDescriptors(QUEUES_EP);
                    for (WorkQueueDescriptor descriptor : descriptors) {
                        deactivateQueue(descriptor);
                    }
                    try {
                        if (!shutdown(10, TimeUnit.SECONDS)) {
                            log.error("Some processors are still active");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new NuxeoException("Interrupted while stopping work manager thread pools", e);
                    }
                }

                @Override
                public void afterStart(ComponentManager mgr, boolean isResume) {
                    if (Boolean.parseBoolean(Framework.getProperty(WORKMANAGER_PROCESSING_DISABLE, "false"))) {
                        log.warn("WorkManager processing has been disabled on this node");
                        return;
                    }
                    List<WorkQueueDescriptor> descriptors = getDescriptors(QUEUES_EP);
                    for (WorkQueueDescriptor descriptor : descriptors) {
                        activateQueue(descriptor);
                    }
                }

                @Override
                public void afterStop(ComponentManager mgr, boolean isStandby) {
                    Framework.getRuntime().getComponentManager().removeListener(this);
                }
            });
        }
    }

    protected void index() {
        List<WorkQueueDescriptor> descriptors = getDescriptors(QUEUES_EP);
        descriptors.forEach(d -> d.categories.forEach(c -> {
            categoryToQueueId.computeIfPresent(c, (k, v) -> {
                if (!v.equals(d.getId())) {
                    log.error("Work category '{}' cannot be assigned to work queue '{}'"
                            + " because it is already assigned to work queue '{}'", c, d.getId(), v);
                }
                return v;
            });
            categoryToQueueId.putIfAbsent(c, d.getId());
        }));
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
        synchronized (getRegistry()) {
            workQueueDescriptor = getDescriptor(QUEUES_EP, queueId);
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
            timeout -= System.currentTimeMillis() - t0;
        }
        return true;
    }

    /**
     * @deprecated since 10.2 because unused
     */
    @Deprecated
    protected long remainingMillis(long t0, long delay) {
        long d = System.currentTimeMillis() - t0;
        if (d > delay) {
            return 0;
        }
        return delay - d;
    }

    /**
     * @deprecated since 10.2 because unused
     */
    @Deprecated
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
            thread.setUncaughtExceptionHandler(this::handleUncaughtException);
            return thread;
        }

        protected void handleUncaughtException(Thread t, Throwable e) {
            Log logLocal = LogFactory.getLog(WorkManagerImpl.class);
            if (e instanceof RejectedExecutionException) {
                // we are responsible of this exception, we use it during shutdown phase to not run the task taken just
                // before shutdown due to race condition, so log it as WARN
                logLocal.warn("Rejected execution error on thread " + t.getName(), e);
            } else if (ExceptionUtils.hasInterruptedCause(e)) {
                logLocal.warn("Interrupted error on thread" + t.getName(), e);
            } else {
                logLocal.error(String.format("Uncaught error on thread: %s, "
                        + "current work might be lost, WorkManager metrics might be corrupted.", t.getName()), e);
            }
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
            scheduledCount = registry.counter(
                    MetricName.build("nuxeo.works.queue.scheduled").tagged("queue", queueId));
            runningCount = registry.counter(
                    MetricName.build("nuxeo.works.queue.running").tagged("queue", queueId));
            completedCount = registry.counter(
                    MetricName.build("nuxeo.works.queue.completed").tagged("queue", queueId));
            workTimer = registry.timer(MetricName.build("nuxeo.works.queue.timer").tagged("queue", queueId));
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
         * @deprecated since 10.2 because unused
         */
        @Deprecated
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
                    log.trace("{} is suspending, giving up", work);
                    return;
                }
                if (isShutdown()) {
                    log.trace("rescheduling {}", work.getId(), t);
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
                deactivateQueueMetrics(queueId);
                queuing.setActive(queueId, false);
                // suspend all running work
                boolean hasRunningWork = false;
                for (Work work : running) {
                    work.setWorkInstanceSuspending();
                    log.trace("suspending and rescheduling {}", work.getId());
                    work.setWorkInstanceState(State.SCHEDULED);
                    queuing.workReschedule(queueId, work);
                    hasRunningWork = true;
                }
                if (hasRunningWork) {
                    long shutdownDelay = Framework.getService(ConfigurationService.class)
                                                  .getLong(SHUTDOWN_DELAY_MS_KEY, 0);
                    // sleep for a given amount of time for works to have time to persist their state and stop properly
                    Thread.sleep(shutdownDelay);
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
            WorkStateHelper.setCanceled(work.getId());
            break;
        case IF_NOT_SCHEDULED:
        case IF_NOT_RUNNING_OR_SCHEDULED:
            // TODO disabled for now because hasWorkInState uses isScheduled
            // which is buggy
            boolean disabled = Boolean.TRUE.booleanValue();
            if (!disabled && hasWorkInState(workId, scheduling.state)) {
                log.debug("Canceling schedule because found: {}", scheduling);
                return;

            }
            break;

        }
        if (work.isGroupJoin()) {
            log.debug("Submit Work: {} to GroupJoin: {}", work::getId, work::getPartitionKey);
            WorkStateHelper.addGroupJoinWork(work.getPartitionKey());
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
            log.debug("Not scheduling work after commit because of missing transaction manager: {}", work);
            return false;
        }
        try {
            Transaction transaction = transactionManager.getTransaction();
            if (transaction == null) {
                log.debug("Not scheduling work after commit because of missing transaction: {}", work);
                return false;
            }
            int status = transaction.getStatus();
            if (status == Status.STATUS_ACTIVE) {
                log.debug("Scheduling work after commit: {}", work);
                transaction.registerSynchronization(new WorkScheduling(work, scheduling));
                return true;
            } else if (status == Status.STATUS_COMMITTED) {
                // called in afterCompletion, we can schedule immediately
                log.debug("Scheduling work immediately: {}", work);
                return false;
            } else if (status == Status.STATUS_MARKED_ROLLBACK) {
                log.debug("Cancelling schedule because transaction marked rollback-only: {}", work);
                return true;
            } else {
                log.debug("Not scheduling work after commit because transaction is in status {}: {}", status, work);
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
        SequenceTracer.start("awaitCompletion on " + (queueId == null ? "all queues" : queueId));
        long durationInMs = TimeUnit.MILLISECONDS.convert(duration, unit);
        long deadline = getTimestampAfter(durationInMs);
        int pause = (int) Math.min(durationInMs, 500L);
        log.debug("awaitForCompletion {} ms", durationInMs);
        do {
            if (noScheduledOrRunningWork(queueId)) {
                completionSynchronizer.signalCompletedWork();
                SequenceTracer.stop("done");
                return true;
            }
            completionSynchronizer.waitForCompletedWork(pause);
        } while (System.currentTimeMillis() < deadline);
        log.info("awaitCompletion timeout after {} ms", durationInMs);
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
            log.trace("{} not empty, sched: {}, running: {}", () -> queueId,
                    () -> getQueueSize(queueId, State.SCHEDULED), () -> getQueueSize(queueId, State.RUNNING));
            return false;
        }
        log.trace("{} is completed", queueId);
        return true;
    }

    @Override
    public boolean isStarted() {
        return started && !shutdownInProgress;
    }

    @Override
    public boolean supportsProcessingDisabling() {
        return queuing.supportsProcessingDisabling();
    }

}
