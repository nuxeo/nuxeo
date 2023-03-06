/*
 * (C) Copyright 2017-2018 Nuxeo (http://nuxeo.com/) and others.
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
import static java.util.Objects.requireNonNullElse;
import static org.nuxeo.ecm.core.work.BaseOverflowRecordFilter.PREFIX_OPTION;
import static org.nuxeo.ecm.core.work.BaseOverflowRecordFilter.STORE_NAME_OPTION;
import static org.nuxeo.ecm.core.work.BaseOverflowRecordFilter.STORE_TTL_OPTION;
import static org.nuxeo.ecm.core.work.BaseOverflowRecordFilter.THRESHOLD_SIZE_OPTION;
import static org.nuxeo.ecm.core.work.api.WorkManager.Scheduling.CANCEL_SCHEDULED;
import static org.nuxeo.lib.stream.computation.AbstractComputation.INPUT_1;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import javax.naming.NamingException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.core.event.EventServiceComponent;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkQueueDescriptor;
import org.nuxeo.ecm.core.work.api.WorkQueueMetrics;
import org.nuxeo.ecm.core.work.api.WorkSchedulePath;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.ComputationPolicy;
import org.nuxeo.lib.stream.computation.ComputationPolicyBuilder;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.RecordFilter;
import org.nuxeo.lib.stream.computation.RecordFilterChain;
import org.nuxeo.lib.stream.computation.Settings;
import org.nuxeo.lib.stream.computation.StreamManager;
import org.nuxeo.lib.stream.computation.StreamProcessor;
import org.nuxeo.lib.stream.computation.Topology;
import org.nuxeo.lib.stream.computation.internals.RecordFilterChainImpl;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.codec.CodecService;
import org.nuxeo.runtime.metrics.NuxeoMetricSet;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentManager;
import org.nuxeo.runtime.model.Descriptor;
import org.nuxeo.runtime.services.config.ConfigurationService;
import org.nuxeo.runtime.stream.StreamService;
import org.nuxeo.runtime.transaction.TransactionHelper;

import io.dropwizard.metrics5.MetricName;
import io.opencensus.trace.AttributeValue;
import io.opencensus.trace.Span;
import io.opencensus.trace.Tracing;

/**
 * WorkManager impl that appends works into a Log. Works are therefore immutable (no state update) and can not be listed
 * for performance reason.
 *
 * @since 9.3
 */
public class StreamWorkManager extends WorkManagerImpl {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(StreamWorkManager.class);

    public static final String WORK_LOG_CONFIG_PROP = "nuxeo.stream.work.log.config";

    @Deprecated(since = "2021.0")
    public static final String DEFAULT_WORK_LOG_CONFIG = "work";

    public static final String WORK_CODEC_PROP = "nuxeo.stream.work.log.codec";

    public static final String DEFAULT_WORK_CODEC = "legacy";

    public static final String WORK_OVER_PROVISIONING_PROP = "nuxeo.stream.work.over.provisioning.factor";

    public static final String DEFAULT_WORK_OVER_PROVISIONING = "3";

    public static final int DEFAULT_CONCURRENCY = 4;

    // @since 11.1
    protected WorkQueueMetrics lastMetrics;

    protected long lastMetricTime;

    protected long CACHE_LAST_METRIC_DURATION_MS = 1000;

    // @since 11.1
    public static final String NAMESPACE_PREFIX = "work/";

    /**
     * @since 10.2
     */
    public static final String STATETTL_KEY = "nuxeo.stream.work.state.ttl.seconds";

    /**
     * @since 10.2
     */
    public static final String STORESTATE_KEY = "nuxeo.stream.work.storestate.enabled";

    /**
     * @since 10.2
     */
    public static final long STATETTL_DEFAULT_VALUE = 3600;

    /**
     * @since 11.1
     */
    public static final String COMPUTATION_FILTER_CLASS_KEY = "nuxeo.stream.work.computation.filter.class";

    /**
     * @since 11.1
     */
    public static final String COMPUTATION_FILTER_STORE_KEY = "nuxeo.stream.work.computation.filter.storeName";

    /**
     * @since 11.1
     */
    public static final String COMPUTATION_FILTER_STORE_TTL_KEY = "nuxeo.stream.work.computation.filter.storeTTL";

    /**
     * @since 11.1
     */
    public static final String COMPUTATION_FILTER_THRESHOLD_SIZE_KEY = "nuxeo.stream.work.computation.filter.thresholdSize";

    /**
     * @since 11.1
     */
    public static final String COMPUTATION_FILTER_PREFIX_KEY = "nuxeo.stream.work.computation.filter.storeKeyPrefix";

    protected Topology topology;

    protected Topology topologyDisabled;

    protected Settings settings;

    protected StreamProcessor streamProcessor;

    protected LogManager logManager;

    protected StreamManager streamManager;

    protected boolean storeState;

    protected long stateTTL;

    protected int getOverProvisioningFactor() {
        // Enable over provisioning only if the log can be distributed
        if (getLogManager().supportSubscribe(Name.ofUrn(NAMESPACE_PREFIX + "default"))) {
            return Integer.parseInt(Framework.getProperty(WORK_OVER_PROVISIONING_PROP, DEFAULT_WORK_OVER_PROVISIONING));
        }
        return 1;
    }

    protected String getCodecName() {
        return Framework.getProperty(WORK_CODEC_PROP, DEFAULT_WORK_CODEC);
    }

    protected Codec<Record> getCodec() {
        return Framework.getService(CodecService.class).getCodec(getCodecName(), Record.class);
    }

    @Override
    public void schedule(Work work, Scheduling scheduling, boolean afterCommit) {
        String queueId = getCategoryQueueId(work.getCategory());
        log.debug("Scheduling: workId: {}, category: {}, queue: {}, scheduling: {}, afterCommit: {}, work: {}",
                work.getId(), work.getCategory(), queueId, scheduling, afterCommit, work);
        if (!isQueuingEnabled(queueId)) {
            log.info("Queue disabled, scheduling canceled: {}", queueId);
            return;
        }
        if (CANCEL_SCHEDULED.equals(scheduling)) {
            if (storeState) {
                if (WorkStateHelper.getState(work.getId()) != null) {
                    WorkStateHelper.setCanceled(work.getId());
                }
            } else {
                log.warn("Canceling a work is only supported if '{}' is true. Skipping work: {}", STORESTATE_KEY, work);
            }
            return;
        }
        if (afterCommit && scheduleAfterCommit(work, scheduling)) {
            return;
        }
        if (storeState) {
            WorkStateHelper.setState(work.getId(), Work.State.SCHEDULED, stateTTL);
        }
        WorkSchedulePath.newInstance(work);
        String key = work.getPartitionKey();
        LogOffset offset;
        try {
            offset = streamManager.append(NAMESPACE_PREFIX + queueId, Record.of(key, WorkComputation.serialize(work)));
            Span span = Tracing.getTracer().getCurrentSpan();
            Map<String, AttributeValue> map = new HashMap<>();
            map.put("work", AttributeValue.stringAttributeValue(work.getId()));
            map.put("class", AttributeValue.stringAttributeValue(work.getClass().getSimpleName()));
            map.put("category", AttributeValue.stringAttributeValue(work.getCategory()));
            map.put("key", AttributeValue.stringAttributeValue(key));
            map.put("offset", AttributeValue.stringAttributeValue(offset.toString()));
            span.addAnnotation("WorkManager#schedule", map);
        } catch (IllegalArgumentException e) {
            log.error("Not scheduled work, unknown category: {}, mapped to {}", work.getCategory(),
                    NAMESPACE_PREFIX + queueId);
            return;
        }
        if (work.isCoalescing()) {
            WorkStateHelper.setLastOffset(work.getId(), offset.offset(), stateTTL);
        }
        if (work.isGroupJoin()) {
            log.debug("Submit Work: {} to GroupJoin: {}, offset: {}", work.getId(), work.getPartitionKey(), offset);
            WorkStateHelper.addGroupJoinWork(work.getPartitionKey());
        }
    }

    @Override
    public int getApplicationStartedOrder() {
        // start before the WorkManagerImpl
        return EventServiceComponent.APPLICATION_STARTED_ORDER - 2;
    }

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        ConfigurationService configuration = Framework.getService(ConfigurationService.class);
        storeState = configuration.isBooleanTrue(STORESTATE_KEY);
        stateTTL = configuration.getLong(STATETTL_KEY, STATETTL_DEFAULT_VALUE);
    }

    protected RecordFilterChain getRecordFilter() {
        String filterClass = getRecordFilterClass();
        if (filterClass == null) {
            return null;
        }
        RecordFilterChain filter = new RecordFilterChainImpl();
        Class<? extends RecordFilter> klass;
        try {
            klass = (Class<RecordFilter>) Class.forName(filterClass);
            if (!RecordFilter.class.isAssignableFrom(klass)) {
                throw new IllegalArgumentException("Invalid class for RecordFilter: " + filterClass);
            }
            RecordFilter ret = klass.getDeclaredConstructor().newInstance();
            ret.init(getRecordFilterOptions());
            filter.addFilter(ret);
        } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException("Invalid class for RecordFilter: " + filterClass, e);
        }
        return filter;
    }

    protected Map<String, String> getRecordFilterOptions() {
        Map<String, String> ret = new HashMap<>();
        ConfigurationService configuration = Framework.getService(ConfigurationService.class);
        configuration.getString(COMPUTATION_FILTER_STORE_KEY).ifPresent(value -> ret.put(STORE_NAME_OPTION, value));
        configuration.getString(COMPUTATION_FILTER_PREFIX_KEY).ifPresent(value -> ret.put(PREFIX_OPTION, value));
        configuration.getInteger(COMPUTATION_FILTER_THRESHOLD_SIZE_KEY)
                     .ifPresent(value -> ret.put(THRESHOLD_SIZE_OPTION, value.toString()));
        configuration.getString(COMPUTATION_FILTER_STORE_TTL_KEY).ifPresent(value -> ret.put(STORE_TTL_OPTION, value));
        return ret;
    }

    protected String getRecordFilterClass() {
        ConfigurationService configuration = Framework.getService(ConfigurationService.class);
        return configuration.getString(COMPUTATION_FILTER_CLASS_KEY).orElse(null);
    }

    @Override
    public void init() {
        if (started) {
            return;
        }
        WorkManagerImpl wmi = (WorkManagerImpl) Framework.getRuntime().getComponent("org.nuxeo.ecm.core.work.service");
        wmi.active = false;
        log.debug("Initializing");
        synchronized (this) {
            if (started) {
                return;
            }
            getDescriptors(QUEUES_EP).forEach(d -> categoryToQueueId.put(d.getId(), d.getId()));
            index();
            initTopology();
            logManager = getLogManager();
            streamManager = getStreamManager();
            streamManager.register("StreamWorkManagerDisable", topologyDisabled, settings);
            streamManager.register("StreamWorkManager", topology, settings);
            started = true;
            new ComponentListener().install();
            log.info("Initialized");
        }
    }

    class ComponentListener implements ComponentManager.Listener {
        @Override
        public void beforeStop(ComponentManager mgr, boolean isStandby) {
            if (!shutdown(10, TimeUnit.SECONDS)) {
                log.error("Some processors are still active");
            }
        }

        @Override
        public void afterStart(ComponentManager mgr, boolean isResume) {
            if (isProcessingDisabled()) {
                log.warn("WorkManager processing has been disabled on this node");
                return;
            }
            streamProcessor = streamManager.createStreamProcessor("StreamWorkManager");
            streamProcessor.start();
            for (Descriptor d : getDescriptors(QUEUES_EP)) {
                activateQueueMetrics(d.getId());
            }
        }

        @Override
        public void afterStop(ComponentManager mgr, boolean isStandby) {
            Framework.getRuntime().getComponentManager().removeListener(this);
            for (Descriptor d : getDescriptors(QUEUES_EP)) {
                deactivateQueueMetrics(d.getId());
            }
        }
    }

    protected LogManager getLogManager() {
        StreamService service = Framework.getService(StreamService.class);
        return service.getLogManager();
    }

    protected StreamManager getStreamManager() {
        StreamService service = Framework.getService(StreamService.class);
        return service.getStreamManager();
    }

    @Deprecated(since = "2021.0")
    protected String getLogConfig() {
        return Framework.getProperty(WORK_LOG_CONFIG_PROP, DEFAULT_WORK_LOG_CONFIG);
    }

    @Override
    public boolean isProcessingEnabled(String queueId) {
        WorkQueueDescriptor wqd = getWorkQueueDescriptor(queueId);
        return wqd != null && wqd.isProcessingEnabled();
    }

    protected void initTopology() {
        List<WorkQueueDescriptor> descriptors = getDescriptors(QUEUES_EP);
        // create the single topology with one root per work pool
        Topology.Builder builder = Topology.builder();
        descriptors.stream()
                   .filter(WorkQueueDescriptor::isProcessingEnabled)
                   .forEach(d -> builder.addComputation(() -> new WorkComputation(NAMESPACE_PREFIX + d.getId()),
                           Collections.singletonList(INPUT_1 + ":" + NAMESPACE_PREFIX + d.getId())));
        topology = builder.build();
        // create a topology for the disabled work pools in order to init their input streams
        Topology.Builder builderDisabled = Topology.builder();
        descriptors.stream()
                   .filter(Predicate.not(WorkQueueDescriptor::isProcessingEnabled))
                   .forEach(d -> builderDisabled.addComputation(() -> new WorkComputation(d.getId()),
                           Collections.singletonList(INPUT_1 + ":" + NAMESPACE_PREFIX + d.getId())));
        topologyDisabled = builderDisabled.build();
        // The retry policy is handled at AbstractWork level, but we want to skip failure
        ComputationPolicy policy = new ComputationPolicyBuilder().continueOnFailure(true).build();
        RecordFilterChain filter = getRecordFilter();
        settings = new Settings(DEFAULT_CONCURRENCY, getPartitions(DEFAULT_CONCURRENCY), getCodec(), policy, filter);
        descriptors.forEach(item -> settings.setConcurrency(NAMESPACE_PREFIX + item.getId(), item.getMaxThreads()));
        descriptors.forEach(
                item -> settings.setPartitions(NAMESPACE_PREFIX + item.getId(), getPartitions(item.getMaxThreads())));
    }

    protected int getPartitions(int maxThreads) {
        if (maxThreads == 1) {
            // Special case where one thread means no concurrency at cluster level
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

        @Override
        public void beforeCompletion() {
        }

        @Override
        public void afterCompletion(int status) {
            if (status == Status.STATUS_COMMITTED) {
                StreamWorkManager.this.schedule(work, scheduling, false);
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
        log.info("Activated queue " + config.id + " " + config.toString());
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
        log.info("Deactivated work queue not supported: {}", config.id);
    }

    @Override
    protected void activateQueueMetrics(String queueId) {
        NuxeoMetricSet queueMetrics = new NuxeoMetricSet(
                MetricName.build("nuxeo.works.global.queue").tagged("queue", queueId));
        queueMetrics.putGauge(() -> getMetricsWithNuxeoClassLoader(queueId).scheduled, "scheduled");
        queueMetrics.putGauge(() -> getMetricsWithNuxeoClassLoader(queueId).running, "running");
        queueMetrics.putGauge(() -> getMetricsWithNuxeoClassLoader(queueId).completed, "completed");
        queueMetrics.putGauge(() -> getMetricsWithNuxeoClassLoader(queueId).canceled, "canceled");
        registry.registerAll(queueMetrics);
    }

    @Override
    protected void deactivateQueueMetrics(String queueId) {
        String queueMetricsName = MetricName.build("nuxeo.works.global.queue").tagged("queue", queueId).getKey();
        registry.removeMatching((name, metric) -> name.getKey().startsWith(queueMetricsName));
    }

    @Override
    public boolean shutdownQueue(String queueId, long timeout, TimeUnit unit) {
        log.warn("Shutdown a queue is not supported with computation implementation");
        return false;
    }

    @Override
    public boolean shutdown(long timeout, TimeUnit timeUnit) {
        if (streamProcessor == null) {
            return true;
        }
        log.info("Shutdown WorkManager in {}ms", () -> timeUnit.toMillis(timeout));
        shutdownInProgress = true;
        try {
            long shutdownDelay = Framework.getService(ConfigurationService.class).getLong(SHUTDOWN_DELAY_MS_KEY, 0);
            boolean ret = streamProcessor.stop(Duration.ofMillis(Math.max(timeUnit.toMillis(timeout), shutdownDelay)));
            if (!ret) {
                log.error("Not able to stop worker pool within the timeout.");
            }
            return ret;
        } finally {
            shutdownInProgress = false;
        }
    }

    protected WorkQueueMetrics getMetricsWithNuxeoClassLoader(String queueId) {
        long now = System.currentTimeMillis();
        if (lastMetrics != null && lastMetrics.queueId == queueId
                && (now - lastMetricTime) < CACHE_LAST_METRIC_DURATION_MS) {
            return lastMetrics;
        }
        // JMX threads have distinct class loader that need to be changed to get metrics
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(Framework.class.getClassLoader());
            lastMetrics = getMetrics(queueId);
            lastMetricTime = System.currentTimeMillis();
            return lastMetrics;
        } finally {
            Thread.currentThread().setContextClassLoader(classLoader);
        }
    }

    @Override
    public WorkQueueMetrics getMetrics(String queueId) {
        Name queue = Name.ofUrn(NAMESPACE_PREFIX + queueId);
        LogLag lag = logManager.getLag(queue, queue);
        long running = 0;
        if (lag.lag() > 0) {
            // we don't have the exact running metric
            // give an approximation that can be higher that actual one because of the over provisioning
            running = min(lag.lag(), settings.getPartitions(queue));
        }
        return new WorkQueueMetrics(queueId, lag.lag(), running, lag.lower(), 0);
    }

    @Override
    public boolean awaitCompletion(String queueId, long duration, TimeUnit unit) throws InterruptedException {
        if (queueId != null) {
            return awaitCompletionOnQueue(queueId, duration, unit);
        }
        for (Descriptor item : getDescriptors(QUEUES_EP)) {
            if (!awaitCompletionOnQueue(item.getId(), duration, unit)) {
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
                log.debug("awaitCompletion for {} completed {}", () -> queueId, () -> getMetrics(queueId));
                return true;
            }
            log.debug("awaitCompletion for {} not completed {}", () -> queueId, () -> getMetrics(queueId));
        }
        log.warn(String.format("%s timeout after: %.2fs, %s", queueId, durationMs / 1000.0, getMetrics(queueId)));
        return false;
    }

    /**
     * @deprecated since 10.2 because unused
     */
    @Deprecated
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
                log.debug("awaitCompletion for {} completed {}", () -> requireNonNullElse(queueId, "all"), () -> wm);
                return true;
            }
            var lowWatermarkF = lowWatermark;
            log.debug("awaitCompletion low wm for {}: {} diff: {}", () -> requireNonNullElse(queueId, "all"), () -> wm,
                    () -> wm - lowWatermarkF);
            lowWatermark = wm;
        }
        log.warn(String.format("%s timeout after: %.2fs", queueId, durationMs / 1000.0));
        return false;
    }

    protected long getLowWaterMark(String queueId) {
        if (streamProcessor == null) {
            return -1L;
        }
        if (queueId != null) {
            return streamProcessor.getLowWatermark(queueId);
        }
        return streamProcessor.getLowWatermark();
    }

    @Override
    public Work.State getWorkState(String workId) {
        if (!storeState) {
            return null;
        }
        return WorkStateHelper.getState(workId);
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
            log.warn("Not scheduled work after commit because of missing transaction manager: {}", work::getId);
            return false;
        }
        try {
            Transaction transaction = transactionManager.getTransaction();
            if (transaction == null) {
                log.debug("Not scheduled work after commit because of missing transaction: {}", work::getId);
                return false;
            }
            int status = transaction.getStatus();
            if (status == Status.STATUS_ACTIVE) {
                log.debug("Scheduled after commit: {}", work::getId);
                transaction.registerSynchronization(new StreamWorkManager.WorkScheduling(work, scheduling));
                return true;
            } else if (status == Status.STATUS_COMMITTED) {
                // called in afterCompletion, we can schedule immediately
                log.debug("Scheduled immediately: {}", work::getId);
                return false;
            } else if (status == Status.STATUS_MARKED_ROLLBACK) {
                log.debug("Cancelling schedule because transaction marked rollback-only: {}", work::getId);
                return true;
            } else {
                log.debug("Not scheduling work after commit because transaction is in status {}: {}", status,
                        work.getId());
                return false;
            }
        } catch (SystemException | RollbackException e) {
            log.error("Cannot schedule after commit", e);
            return false;
        }
    }

    @Override
    public boolean supportsProcessingDisabling() {
        return true;
    }

}
