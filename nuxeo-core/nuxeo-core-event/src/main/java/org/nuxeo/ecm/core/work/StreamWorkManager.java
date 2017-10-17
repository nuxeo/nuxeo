/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.ecm.core.work;

import static java.lang.Math.min;
import static org.nuxeo.ecm.core.work.api.WorkManager.Scheduling.CANCEL_SCHEDULED;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.event.EventServiceComponent;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkQueueDescriptor;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;
import org.nuxeo.ecm.core.work.api.WorkSchedulePath;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.computation.log.LogStreamProcessor;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.metrics.NuxeoMetricSet;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.transaction.TransactionHelper;

import com.codahale.metrics.MetricRegistry;

/**
 * WorkManager impl that appends works into a Log. Works are therefore immutable (no state update) and can not be listed
 * for performance reason.
 *
 * @since 9.3
 */
public class StreamWorkManager extends WorkManagerImpl {
    protected static final Log log = LogFactory.getLog(StreamWorkManager.class);

    public static final String WORKMANAGER_CONFIG_PROP = "nuxeo.stream.work.config";

    public static final String DEFAULT_WORKMANAGER_CONFIG = "work";

    public static final String WORKMANAGER_OVERPROVISIONING_PROP = "nuxeo.stream.work.over.provisioning";

    public static final String DEFAULT_WORKMANAGER_OVERPROVISIONING = "3";

    public static final int DEFAULT_CONCURRENCY = 4;

    protected Topology topology;

    protected Settings settings;

    protected StreamProcessor streamProcessor;

    protected LogManager logManager;

    protected final Set<String> streamIds = new HashSet<>();

    protected int getOverProvisioningFactor() {
        return Integer.parseInt(
                Framework.getProperty(WORKMANAGER_OVERPROVISIONING_PROP, DEFAULT_WORKMANAGER_OVERPROVISIONING));
    }

    @Override
    public void schedule(Work work, Scheduling scheduling, boolean afterCommit) {
        String queueId = getStreamForCategory(work.getCategory());
        if (log.isDebugEnabled()) {
            log.debug(String.format(
                    "Scheduling: workId: %s, category: %s, queue: %s, scheduling: %s, afterCommit: %s, work: %s",
                    work.getId(), work.getCategory(), queueId, scheduling, afterCommit, work));
        }
        if (!isQueuingEnabled(queueId)) {
            log.info("Queue disabled, scheduling canceled: " + queueId);
            return;
        }
        if (CANCEL_SCHEDULED.equals(scheduling)) {
            log.warn("Canceling a work is not supported by this impl, skipping work: " + work);
            return;
        }
        if (afterCommit && scheduleAfterCommit(work, scheduling)) {
            return;
        }
        WorkSchedulePath.newInstance(work);
        // TODO: may be choose a key with a transaction id so all jobs from the same tx are ordered ?
        String key = work.getId();
        LogAppender<Record> appender = logManager.getAppender(getStreamForCategory(work.getCategory()));
        if (appender == null) {
            log.error(String.format("Not scheduled work, unknown category: %s, mapped to %s", work.getCategory(),
                    getStreamForCategory(work.getCategory())));
            return;
        }
        appender.append(key, new Record(key, WorkComputation.serialize(work),
                Watermark.ofTimestamp(System.currentTimeMillis()).getValue(), null));
    }

    protected String getStreamForCategory(String category) {
        if (category != null && streamIds.contains(category)) {
            return category;
        }
        return DEFAULT_CATEGORY;
    }

    @Override
    public int getApplicationStartedOrder() {
        // start before the WorkManagerImpl
        return EventServiceComponent.APPLICATION_STARTED_ORDER - 2;
    }

    @Override
    public void start(ComponentContext context) {
        init();
    }

    public void init() {
        if (started) {
            return;
        }
        log.debug("Initializing");
        synchronized (this) {
            if (started) {
                return;
            }
            supplantWorkManagerImpl();
            workQueueConfig.index();
            initTopology();
            this.logManager = getLogManager();
            this.streamProcessor = new LogStreamProcessor(logManager);
            streamProcessor.init(topology, settings);
            started = true;
            Framework.getRuntime().getComponentManager().addListener(new ComponentListener());
            log.info("Initialized");
        }
    }

    class ComponentListener extends ComponentManager.LifeCycleHandler {
        @Override
        public void beforeStop(ComponentManager mgr, boolean isStandby) {
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
            streamProcessor.start();
            for (String id : workQueueConfig.getQueueIds()) {
                activateQueueMetrics(id);
            }
        }

        @Override
        public void afterStop(ComponentManager mgr, boolean isStandby) {
            Framework.getRuntime().getComponentManager().removeListener(this);
            for (String id : workQueueConfig.getQueueIds()) {
                deactivateQueueMetrics(id);
            }
        }
    }

    protected LogManager getLogManager() {
        String config = getLogConfig();
        log.info("Init StreamWorkManager with Log configuration: " + config);
        StreamService service = Framework.getService(StreamService.class);
        return service.getLogManager(getLogConfig());
    }

    protected String getLogConfig() {
        return Framework.getProperty(WORKMANAGER_CONFIG_PROP, DEFAULT_WORKMANAGER_CONFIG);
    }

    @Override
    public boolean isProcessingEnabled(String queueId) {
        WorkQueueDescriptor wqd = getWorkQueueDescriptor(queueId);
        return wqd != null && wqd.isProcessingEnabled();
    }

    /**
     * Hack to steal the WorkManagerImpl queue contributions.
     */
    protected void supplantWorkManagerImpl() {
        WorkManagerImpl wmi = (WorkManagerImpl) Framework.getRuntime().getComponent("org.nuxeo.ecm.core.work.service");
        Class clazz = WorkManagerImpl.class;
        Field workQueueConfigField;
        try {
            workQueueConfigField = clazz.getDeclaredField("workQueueConfig");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        workQueueConfigField.setAccessible(true);
        final WorkQueueRegistry wqr;
        try {
            wqr = (WorkQueueRegistry) workQueueConfigField.get(wmi);
            log.debug("Remove contributions from WorkManagerImpl");
            // Removes the WorkManagerImpl so it does not create any worker pool
            workQueueConfigField.set(wmi, new WorkQueueRegistry());
            // TODO: should we remove workQueuingConfig registry as well ?
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        wqr.getQueueIds().forEach(id -> workQueueConfig.addContribution(wqr.get(id)));
        streamIds.addAll(workQueueConfig.getQueueIds());
        workQueueConfig.getQueueIds().forEach(id -> log.info("Registering : " + id));
    }

    protected void initTopology() {
        // create a single topology with one root per work pool
        Topology.Builder builder = Topology.builder();
        workQueueConfig.getQueueIds().stream().filter(item -> workQueueConfig.get(item).isProcessingEnabled()).forEach(
                item -> builder.addComputation(() -> new WorkComputation(item),
                        Collections.singletonList("i1:" + item)));
        this.topology = builder.build();
        this.settings = new Settings(DEFAULT_CONCURRENCY, getPartitions(DEFAULT_CONCURRENCY));
        workQueueConfig.getQueueIds()
                       .forEach(item -> settings.setConcurrency(item, workQueueConfig.get(item).getMaxThreads()));
        workQueueConfig.getQueueIds().forEach(
                item -> settings.setPartitions(item, getPartitions(workQueueConfig.get(item).getMaxThreads())));
    }

    protected int getPartitions(int maxThreads) {
        if (maxThreads == 1) {
            // when the pool size is one the we don't want any concurrency
            return 1;
        }
        return getOverProvisioningFactor() * maxThreads;
    }

    public class WorkScheduling implements Synchronization {
        public final Work work;

        public final Scheduling scheduling;

        public WorkScheduling(Work work, Scheduling scheduling) {
            this.work = work;
            this.scheduling = scheduling;
        }

        public void beforeCompletion() {
        }

        public void afterCompletion(int status) {
            if (status == Status.STATUS_COMMITTED) {
                StreamWorkManager.this.schedule(this.work, this.scheduling, false);
            } else {
                if (status != Status.STATUS_ROLLEDBACK) {
                    throw new IllegalArgumentException("Unsupported transaction status " + status);
                }
            }

        }
    }

    @Override
    void activateQueue(WorkQueueDescriptor config) {
        // queue processing is activated only from component listener afterStart
        if (WorkQueueDescriptor.ALL_QUEUES.equals(config.id)) {
            throw new IllegalArgumentException("cannot activate all queues");
        }
        log.info("Activated queue " + config.id + " " + config.toEffectiveString());
        if (config.isProcessingEnabled()) {
            activateQueueMetrics(config.id);
        }
    }

    @Override
    void deactivateQueue(WorkQueueDescriptor config) {
        // queue processing is deactivated only on shutdown
        if (WorkQueueDescriptor.ALL_QUEUES.equals(config.id)) {
            throw new IllegalArgumentException("cannot deactivate all queues");
        }
        if (config.isProcessingEnabled()) {
            deactivateQueueMetrics(config.id);
        }
        log.info("Deactivated work queue not supported: " + config.id);
    }

    @Override
    protected void activateQueueMetrics(String queueId) {
        NuxeoMetricSet queueMetrics = new NuxeoMetricSet("nuxeo", "works", "total", queueId);
        queueMetrics.putGauge(() -> getMetricsWithNuxeoClassLoader(queueId).scheduled, "scheduled");
        queueMetrics.putGauge(() -> getMetricsWithNuxeoClassLoader(queueId).running, "running");
        queueMetrics.putGauge(() -> getMetricsWithNuxeoClassLoader(queueId).completed, "completed");
        queueMetrics.putGauge(() -> getMetricsWithNuxeoClassLoader(queueId).canceled, "canceled");
        registry.registerAll(queueMetrics);
    }

    @Override
    protected void deactivateQueueMetrics(String queueId) {
        String queueMetricsName = MetricRegistry.name("nuxeo", "works", "total", queueId);
        registry.removeMatching((name, metric) -> name.startsWith(queueMetricsName));
    }

    @Override
    public boolean shutdownQueue(String queueId, long timeout, TimeUnit unit) throws InterruptedException {
        log.warn("Shutdown a queue is not supported with computation implementation");
        return false;
    }

    @Override
    public boolean shutdown(long timeout, TimeUnit timeUnit) throws InterruptedException {
        log.info("Shutdown WorkManager in " + timeUnit.toMillis(timeout) + " ms");
        shutdownInProgress = true;
        try {
            boolean ret = streamProcessor.stop(Duration.ofMillis(timeUnit.toMillis(timeout)));
            if (!ret) {
                log.error("Not able to stop worker pool within the timeout.");
            }
            return ret;
        } finally {
            shutdownInProgress = false;
        }
    }

    @Override
    public int getQueueSize(String queueId, Work.State state) {
        switch (state) {
        case SCHEDULED:
            return getMetrics(queueId).getScheduled().intValue();
        case RUNNING:
            return getMetrics(queueId).getRunning().intValue();
        }
        return 0;
    }

    protected WorkQueueMetrics getMetricsWithNuxeoClassLoader(String queueId) {
        // JMX threads have distinct class loader that need to be changed to get metrics
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(Framework.class.getClassLoader());
            return getMetrics(queueId);
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    @Override
    public WorkQueueMetrics getMetrics(String queueId) {
        LogLag lag = logManager.getLag(queueId, queueId);
        long running = 0;
        if (lag.lag() > 0) {
            // we don't have the exact running metric
            // give an approximation that can be higher that actual one because of the over provisioning
            running = min(lag.lag(), settings.getPartitions(queueId));
        }
        return new WorkQueueMetrics(queueId, lag.lag(), running, lag.lower(), 0);
    }

    @Override
    public boolean awaitCompletion(String queueId, long duration, TimeUnit unit) throws InterruptedException {
        if (queueId != null) {
            return awaitCompletionOnQueue(queueId, duration, unit);
        }
        for (String item : workQueueConfig.getQueueIds()) {
            if (!awaitCompletionOnQueue(item, duration, unit)) {
                return false;
            }
        }
        return true;
    }

    protected boolean awaitCompletionOnQueue(String queueId, long duration, TimeUnit unit) throws InterruptedException {
        if (!isStarted()) {
            return true;
        }
        log.debug("awaitCompletion " + queueId + " starting");
        // wait for the lag to be null
        long durationMs = min(unit.toMillis(duration), TimeUnit.DAYS.toMillis(1)); // prevent overflow
        long deadline = System.currentTimeMillis() + durationMs;
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
            int lag = getMetrics(queueId).getScheduled().intValue();
            if (lag == 0) {
                if (log.isDebugEnabled()) {
                    log.debug("awaitCompletion for " + queueId + " completed " + getMetrics(queueId));
                }
                return true;
            }
            if (!log.isDebugEnabled()) {
                log.debug("awaitCompletion for " + queueId + " not completed " + getMetrics(queueId));
            }
        }
        log.warn(String.format("%s timeout after: %.2fs, %s", queueId, durationMs / 1000.0, getMetrics(queueId)));
        return false;
    }

    public boolean awaitCompletionWithWaterMark(String queueId, long duration, TimeUnit unit)
            throws InterruptedException {
        if (!isStarted()) {
            return true;
        }
        // wait that the low watermark get stable
        long durationMs = min(unit.toMillis(duration), TimeUnit.DAYS.toMillis(1)); // prevent overflow
        long deadline = System.currentTimeMillis() + durationMs;
        long lowWatermark = getLowWaterMark(queueId);
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
            long wm = getLowWaterMark(queueId);
            if (wm == lowWatermark) {
                log.debug("awaitCompletion for " + ((queueId == null) ? "all" : queueId) + " completed " + wm);
                return true;
            }
            if (log.isDebugEnabled()) {
                log.debug("awaitCompletion low wm  for " + ((queueId == null) ? "all" : queueId) + ":" + wm + " diff: "
                        + (wm - lowWatermark));
            }
            lowWatermark = wm;
        }
        log.warn(String.format("%s timeout after: %.2fs", queueId, durationMs / 1000.0));
        return false;
    }

    protected long getLowWaterMark(String queueId) {
        if (queueId != null) {
            return streamProcessor.getLowWatermark(queueId);
        }
        return streamProcessor.getLowWatermark();
    }

    @Override
    public Work.State getWorkState(String s) {
        // always not found
        return null;
    }

    @Override
    public Work find(String s, Work.State state) {
        // always not found
        return null;
    }

    @Override
    public List<Work> listWork(String s, Work.State state) {
        return Collections.emptyList();
    }

    @Override
    public List<String> listWorkIds(String s, Work.State state) {
        return Collections.emptyList();
    }

    @Override
    protected boolean scheduleAfterCommit(Work work, Scheduling scheduling) {
        TransactionManager transactionManager;
        try {
            transactionManager = TransactionHelper.lookupTransactionManager();
        } catch (NamingException e) {
            transactionManager = null;
        }
        if (transactionManager == null) {
            log.warn("Not scheduled work after commit because of missing transaction manager: " + work.getId());
            return false;
        }
        try {
            Transaction transaction = transactionManager.getTransaction();
            if (transaction == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Not scheduled work after commit because of missing transaction: " + work.getId());
                }
                return false;
            }
            int status = transaction.getStatus();
            if (status == Status.STATUS_ACTIVE) {
                if (log.isDebugEnabled()) {
                    log.debug("Scheduled after commit: " + work.getId());
                }
                transaction.registerSynchronization(new StreamWorkManager.WorkScheduling(work, scheduling));
                return true;
            } else if (status == Status.STATUS_COMMITTED) {
                // called in afterCompletion, we can schedule immediately
                if (log.isDebugEnabled()) {
                    log.debug("Scheduled immediately: " + work.getId());
                }
                return false;
            } else if (status == Status.STATUS_MARKED_ROLLBACK) {
                if (log.isDebugEnabled()) {
                    log.debug("Cancelling schedule because transaction marked rollback-only: " + work.getId());
                }
                return true;
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Not scheduling work after commit because transaction is in status " + status + ": "
                            + work.getId());
                }
                return false;
            }
        } catch (SystemException | RollbackException e) {
            log.error("Cannot schedule after commit", e);
            return false;
        }
    }

}
