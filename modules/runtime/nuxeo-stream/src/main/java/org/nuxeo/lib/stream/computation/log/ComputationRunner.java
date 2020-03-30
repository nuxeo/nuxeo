/*
 * (C) Copyright 2017-2019 Nuxeo (http://nuxeo.com/) and others.
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
package org.nuxeo.lib.stream.computation.log;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.computation.Computation;
import org.nuxeo.lib.stream.computation.ComputationMetadataMapping;
import org.nuxeo.lib.stream.computation.ComputationPolicy;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.computation.internals.ComputationContextImpl;
import org.nuxeo.lib.stream.computation.internals.WatermarkMonotonicInterval;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.RebalanceException;
import org.nuxeo.lib.stream.log.RebalanceListener;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.MetricName;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;
import io.dropwizard.metrics5.Timer;

import net.jodah.failsafe.Failsafe;

/**
 * Thread driving a Computation
 *
 * @since 9.3
 */
@SuppressWarnings("EmptyMethod")
public class ComputationRunner implements Runnable, RebalanceListener {
    public static final Duration READ_TIMEOUT = Duration.ofMillis(25);

    protected static final long STARVING_TIMEOUT_MS = 1000;

    protected static final long INACTIVITY_BREAK_MS = 100;

    private static final Log log = LogFactory.getLog(ComputationRunner.class);

    protected final LogStreamManager streamManager;

    protected final ComputationMetadataMapping metadata;

    protected final LogTailer<Record> tailer;

    protected final Supplier<Computation> supplier;

    protected final CountDownLatch assignmentLatch = new CountDownLatch(1);

    protected final WatermarkMonotonicInterval lowWatermark = new WatermarkMonotonicInterval();

    protected final ComputationPolicy policy;

    protected ComputationContextImpl context;

    protected volatile boolean stop;

    protected volatile boolean drain;

    protected Computation computation;

    protected long counter;

    protected long inRecords;

    protected long inCheckpointRecords;

    protected long outRecords;

    protected long lastReadTime = System.currentTimeMillis();

    protected long lastTimerExecution;

    protected String threadName;

    protected List<LogPartition> defaultAssignment;

    // @since 11.1
    // Use the Nuxeo registry name without adding dependency on nuxeo-runtime
    public static final String NUXEO_METRICS_REGISTRY_NAME = "org.nuxeo.runtime.metrics.MetricsService";

    public static final String GLOBAL_FAILURE_COUNT_REGISTRY_NAME = MetricRegistry.name("nuxeo", "streams", "failure")
                                                                                  .getKey();

    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(NUXEO_METRICS_REGISTRY_NAME);

    protected Counter globalFailureCount;

    protected Counter failureCount;

    protected Counter recordSkippedCount;

    protected Counter runningCount;

    protected Timer processRecordTimer;

    protected Timer processTimerTimer;

    // @since 11.1
    protected static AtomicInteger skipFailures = new AtomicInteger(0);

    // @since 11.1
    protected boolean recordActivity;

    @SuppressWarnings("unchecked")
    public ComputationRunner(Supplier<Computation> supplier, ComputationMetadataMapping metadata,
            List<LogPartition> defaultAssignment, LogStreamManager streamManager, ComputationPolicy policy) {
        this.supplier = supplier;
        this.metadata = metadata;
        this.streamManager = streamManager;
        this.policy = policy;
        if (metadata.inputStreams().isEmpty()) {
            this.tailer = null;
            this.context = new ComputationContextImpl(streamManager, metadata, policy, false);
            assignmentLatch.countDown();
        } else if (streamManager.supportSubscribe()) {
            this.tailer = streamManager.subscribe(metadata.name(), metadata.inputStreams(), this);
            // create a spare context until the assignment is done
            this.context = new ComputationContextImpl(streamManager, metadata, policy, true);
        } else {
            this.context = new ComputationContextImpl(streamManager, metadata, policy,  defaultAssignment.isEmpty());
            this.tailer = streamManager.createTailer(metadata.name(), defaultAssignment);
            assignmentLatch.countDown();
        }
        this.defaultAssignment = defaultAssignment;
    }

    public void stop() {
        log.debug(metadata.name() + ": Receives Stop signal");
        stop = true;
        if (computation != null) {
            computation.signalStop();
        }
    }

    public void drain() {
        log.debug(metadata.name() + ": Receives Drain signal");
        drain = true;
    }

    public boolean waitForAssignments(Duration timeout) throws InterruptedException {
        if (!assignmentLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            log.warn(metadata.name() + ": Timeout waiting for assignment");
            return false;
        }
        return true;
    }

    @Override
    public void run() {
        threadName = Thread.currentThread().getName();
        boolean interrupted = false;
        boolean normalTermination = false;
        computation = supplier.get();
        log.debug(metadata.name() + ": Init");
        registerMetrics();
        try {
            computation.init(context);
            log.debug(metadata.name() + ": Start");
            processLoop();
            normalTermination = true;
        } catch (InterruptedException e) {
            interrupted = true; // Thread.currentThread().interrupt() in finally
            // this is expected when the pool is shutdownNow
            String msg = metadata.name() + ": Interrupted";
            if (log.isTraceEnabled()) {
                log.debug(msg, e);
            } else {
                log.debug(msg);
            }
        } catch (Exception e) {
            if (Thread.currentThread().isInterrupted()) {
                // this can happen when pool is shutdownNow throwing ClosedByInterruptException
                log.info(metadata.name() + ": Interrupted", e);
            } else {
                log.error(metadata.name() + ": Exception in processLoop: " + e.getMessage(), e);
                throw e;
            }
        } finally {
            try {
                computation.destroy();
                closeTailer();
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            if (normalTermination || interrupted) {
                log.debug(metadata.name() + ": Terminated");
            } else {
                // Terminating because of unexpected error in the ComputationRunner code
                log.error(String.format("Terminate computation: %s due to previous failure", metadata.name()));
                globalFailureCount.inc();
                failureCount.inc();
            }
        }
    }

    protected void registerMetrics() {
        globalFailureCount = registry.counter(GLOBAL_FAILURE_COUNT_REGISTRY_NAME);
        runningCount = registry.counter(
                MetricName.build("nuxeo.streams.computation.running").tagged("computation", metadata.name()));
        failureCount = registry.counter(
                MetricName.build("nuxeo.streams.computation.failure").tagged("computation", metadata.name()));
        recordSkippedCount = registry.counter(
                MetricName.build("nuxeo.streams.computation.skippedRecord").tagged("computation", metadata.name()));
        processRecordTimer = registry.timer(
                MetricName.build("nuxeo.streams.computation.processRecord").tagged("computation", metadata.name()));
        processTimerTimer = registry.timer(
                MetricName.build("nuxeo.streams.computation.processTimer").tagged("computation", metadata.name()));
    }

    protected void closeTailer() {
        if (tailer != null && !tailer.closed()) {
            tailer.close();
        }
    }

    protected void processLoop() throws InterruptedException {
        boolean timerActivity;
        while (continueLoop()) {
            timerActivity = processTimer();
            recordActivity = processRecord();
            counter++;
            if (!timerActivity && !recordActivity) {
                // no activity take a break
                Thread.sleep(INACTIVITY_BREAK_MS);
            }
        }
    }

    protected boolean continueLoop() {
        if (stop || Thread.currentThread().isInterrupted()) {
            return false;
        } else if (drain) {
            long now = System.currentTimeMillis();
            if (metadata.inputStreams().isEmpty()) {
                // for a source we take lastTimerExecution starvation
                if (lastTimerExecution > 0 && (now - lastTimerExecution) > STARVING_TIMEOUT_MS) {
                    log.info(metadata.name() + ": End of source drain, last timer " + STARVING_TIMEOUT_MS + " ms ago");
                    return false;
                }
            } else if (!recordActivity && (now - lastReadTime) > STARVING_TIMEOUT_MS) {
                log.info(metadata.name() + ": End of drain no more input after " + (now - lastReadTime) + " ms, "
                            + inRecords + " records read, " + counter + " reads attempt");
                return false;
            }
        }
        return true;
    }

    protected boolean processTimer() {
        Map<String, Long> timers = context.getTimers();
        if (timers.isEmpty()) {
            return false;
        }
        if (tailer != null && tailer.assignments().isEmpty()) {
            // needed to ensure single source across multiple nodes
            return false;
        }
        long now = System.currentTimeMillis();
        final boolean[] timerUpdate = { false };
        // filter and order timers
        LinkedHashMap<String, Long> sortedTimer = timers.entrySet()
                                                        .stream()
                                                        .filter(entry -> entry.getValue() <= now)
                                                        .sorted(Map.Entry.comparingByValue())
                                                        .collect(
                                                                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                                                        (e1, e2) -> e1, LinkedHashMap::new));
        sortedTimer.forEach((key, value) -> {
            context.removeTimer(key);
            processTimerWithRetry(key, value);
            timerUpdate[0] = true;
        });
        if (timerUpdate[0]) {
            checkSourceLowWatermark();
            lastTimerExecution = now;
            setThreadName("timer");
            checkpointIfNecessary();
            if (context.requireTerminate()) {
                stop = true;
            }
            return true;
        }
        return false;
    }

    protected void processTimerWithRetry(String key, Long value) {
        try (Timer.Context ignored = processTimerTimer.time()) {
            Failsafe.with(policy.getRetryPolicy())
                    .onRetry(failure -> computation.processRetry(context, failure))
                    .onFailure(failure -> computation.processFailure(context, failure))
                    .withFallback(() -> processFallback(context))
                    .run(() -> computation.processTimer(context, key, value));
        }
    }

    protected boolean processRecord() throws InterruptedException {
        if (context.requireTerminate()) {
            stop = true;
            return true;
        }
        if (tailer == null) {
            return false;
        }
        Duration timeoutRead = getTimeoutDuration();
        LogRecord<Record> logRecord = null;
        try {
            logRecord = tailer.read(timeoutRead);
        } catch (RebalanceException e) {
            // the revoke has done a checkpoint we can continue
        }
        Record record;
        if (logRecord != null) {
            record = logRecord.message();
            String stream = logRecord.offset().partition().name();
            Record filteredRecord = streamManager.getFilter(stream).afterRead(record, logRecord.offset());
            if (filteredRecord == null) {
                if (log.isDebugEnabled()) {
                    log.debug("Filtering skip record: " + record);
                }
                return false;
            } else if (filteredRecord != record) {
                logRecord = new LogRecord<>(filteredRecord, logRecord.offset());
                record = filteredRecord;
            }
            lastReadTime = System.currentTimeMillis();
            inRecords++;
            lowWatermark.mark(record.getWatermark());
            context.setLastOffset(logRecord.offset());
            String from = metadata.reverseMap(stream);
            processRecordWithRetry(from, record);
            checkRecordFlags(record);
            checkSourceLowWatermark();
            setThreadName("record");
            checkpointIfNecessary();
            return true;
        }
        return false;
    }

    protected void processRecordWithRetry(String from, Record record) {
        runningCount.inc();
        try (Timer.Context ignored = processRecordTimer.time()) {
            Failsafe.with(policy.getRetryPolicy())
                    .onRetry(failure -> computation.processRetry(context, failure))
                    .onFailure(failure -> computation.processFailure(context, failure))
                    .withFallback(() -> processFallback(context))
                    .run(() -> computation.processRecord(context, from, record));
        } finally {
            runningCount.dec();
        }
    }

    protected void processFallback(ComputationContextImpl context) {
        if (policy.continueOnFailure()) {
            log.error(String.format("Skip record after failure: %s", context.getLastOffset()));
            context.askForCheckpoint();
            recordSkippedCount.inc();
        } else if (skipFailureForRecovery()) {
            log.error(String.format("Skip record after failure instead of terminating because of recovery mode: %s",
                    context.getLastOffset()));
            context.askForCheckpoint();
            recordSkippedCount.inc();
        } else {
            log.error(String.format("Terminate computation: %s due to previous failure", metadata.name()));
            context.cancelAskForCheckpoint();
            context.askForTermination();
            globalFailureCount.inc();
            failureCount.inc();
        }
    }

    protected boolean skipFailureForRecovery() {
        if (policy.getSkipFirstFailures() > 0) {
            if (skipFailures.incrementAndGet() <= policy.getSkipFirstFailures()) {
                return true;
            }
        }
        return false;
    }

    protected Duration getTimeoutDuration() {
        // lastReadTime could have been updated by another thread calling onPartitionsAssigned when doing minus
        // no need to synchronize it, we don't want an accurate value there
        long adaptedReadTimeout = Math.max(0, System.currentTimeMillis() - lastReadTime);
        // Adapt the duration so we are not throttling when one of the input stream is empty
        return Duration.ofMillis(Math.min(READ_TIMEOUT.toMillis(), adaptedReadTimeout));
    }

    protected void checkSourceLowWatermark() {
        long watermark = context.getSourceLowWatermark();
        if (watermark > 0) {
            lowWatermark.mark(Watermark.ofValue(watermark));
            context.setSourceLowWatermark(0);
        }
    }

    protected void checkRecordFlags(Record record) {
        if (record.getFlags().contains(Record.Flag.POISON_PILL)) {
            log.info(metadata.name() + ": Receive POISON PILL");
            context.askForCheckpoint();
            stop = true;
        } else if (record.getFlags().contains(Record.Flag.COMMIT)) {
            context.askForCheckpoint();
        }
    }

    protected void checkpointIfNecessary() {
        if (context.requireCheckpoint()) {
            boolean completed = false;
            try {
                checkpoint();
                completed = true;
            } finally {
                if (!completed) {
                    log.error(metadata.name() + ": CHECKPOINT FAILURE: Resume may create duplicates.");
                }
            }
        }
    }

    protected void checkpoint() {
        sendRecords();
        saveTimers();
        saveState();
        // To Simulate slow checkpoint add a Thread.sleep(1)
        saveOffsets();
        lowWatermark.checkpoint();
        context.removeCheckpointFlag();
        inCheckpointRecords = inRecords;
        setThreadName("checkpoint");
        log.debug(metadata.name() + ": checkpoint done");
    }

    protected void saveTimers() {
        // TODO: save timers in the key value store NXP-22112
    }

    protected void saveState() {
        // TODO: save key value store NXP-22112
    }

    protected void saveOffsets() {
        if (tailer != null) {
            tailer.commit();
        }
    }

    protected void sendRecords() {
        for (String stream : metadata.outputStreams()) {
            for (Record record : context.getRecords(stream)) {
                if (record.getWatermark() == 0) {
                    // use low watermark when not set
                    record.setWatermark(lowWatermark.getLow().getValue());
                }
                streamManager.append(stream, record);
                outRecords++;
            }
            context.getRecords(stream).clear();
        }
    }

    public Watermark getLowWatermark() {
        return lowWatermark.getLow();
    }

    protected void setThreadName(String message) {
        String name = threadName + ",in:" + inRecords + ",inCheckpoint:" + inCheckpointRecords + ",out:" + outRecords
                + ",lastRead:" + lastReadTime + ",lastTimer:" + lastTimerExecution + ",wm:"
                + lowWatermark.getLow().getValue() + ",loop:" + counter;
        if (message != null) {
            name += "," + message;
        }
        Thread.currentThread().setName(name);
    }

    @Override
    public void onPartitionsRevoked(Collection<LogPartition> partitions) {
        setThreadName("rebalance revoked");
    }

    @Override
    public void onPartitionsAssigned(Collection<LogPartition> partitions) {
        lastReadTime = System.currentTimeMillis();
        boolean isSpare = partitions.isEmpty();
        setThreadName("rebalance assigned");
        // reset the context
        this.context = new ComputationContextImpl(streamManager, metadata, policy, partitions.isEmpty());
        log.debug(metadata.name() + ": Init isSpare=" + isSpare);
        computation.init(context);
        lastReadTime = System.currentTimeMillis();
        lastTimerExecution = 0;
        assignmentLatch.countDown();
        // what about watermark ?
    }
}
