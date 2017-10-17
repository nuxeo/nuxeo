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
package org.nuxeo.lib.stream.computation.log;

import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.computation.Computation;
import org.nuxeo.lib.stream.computation.ComputationMetadataMapping;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.computation.internals.ComputationContextImpl;
import org.nuxeo.lib.stream.computation.internals.WatermarkMonotonicInterval;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.RebalanceException;
import org.nuxeo.lib.stream.log.RebalanceListener;

/**
 * Thread driving a Computation
 *
 * @since 9.3
 */
@SuppressWarnings("EmptyMethod")
public class ComputationRunner implements Runnable, RebalanceListener {
    public static final Duration READ_TIMEOUT = Duration.ofMillis(25);

    protected static final long STARVING_TIMEOUT_MS = 1000;

    private static final Log log = LogFactory.getLog(ComputationRunner.class);

    protected final LogManager logManager;

    protected final ComputationMetadataMapping metadata;

    protected final LogTailer<Record> tailer;

    protected final Supplier<Computation> supplier;

    protected final CountDownLatch assignmentLatch = new CountDownLatch(1);

    protected final WatermarkMonotonicInterval lowWatermark = new WatermarkMonotonicInterval();

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

    @SuppressWarnings("unchecked")
    public ComputationRunner(Supplier<Computation> supplier, ComputationMetadataMapping metadata,
            List<LogPartition> defaultAssignment, LogManager logManager) {
        this.supplier = supplier;
        this.metadata = metadata;
        this.logManager = logManager;
        this.context = new ComputationContextImpl(metadata);
        if (metadata.inputStreams().isEmpty()) {
            this.tailer = null;
            assignmentLatch.countDown();
        } else if (logManager.supportSubscribe()) {
            this.tailer = logManager.subscribe(metadata.name(), metadata.inputStreams(), this);
        } else {
            this.tailer = logManager.createTailer(metadata.name(), defaultAssignment);
            assignmentLatch.countDown();
        }
    }

    public void stop() {
        log.debug(metadata.name() + ": Receives Stop signal");
        stop = true;
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
        computation = supplier.get();
        log.debug(metadata.name() + ": Init");
        computation.init(context);
        log.debug(metadata.name() + ": Start");
        try {
            processLoop();
        } catch (InterruptedException e) {
            // this is expected when the pool is shutdownNow
            if (log.isTraceEnabled()) {
                log.debug(metadata.name() + ": Interrupted", e);
            } else {
                log.debug(metadata.name() + ": Interrupted");
            }
            // the interrupt flag is set after the tailer are closed
            interrupted = true;
        } catch (Exception e) {
            if (Thread.currentThread().isInterrupted()) {
                // this can happen when pool is shutdownNow throwing ClosedByInterruptException
                log.info(metadata.name() + ": Interrupted", e);
                // TODO: test if we should set interrupted = true to rearm the interrupt flag
            } else {
                log.error(metadata.name() + ": Exception in processLoop: " + e.getMessage(), e);
                throw e;
            }
        } finally {
            try {
                computation.destroy();
                closeTailer();
                log.debug(metadata.name() + ": Exited");
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    protected void closeTailer() {
        if (tailer != null && !tailer.closed()) {
            tailer.close();
        }
    }

    protected void processLoop() throws InterruptedException {
        while (continueLoop()) {
            processTimer();
            processRecord();
            counter++;
            // TODO: add pause for computation without inputs or without timer to prevent CPU hogs
        }
    }

    protected boolean continueLoop() {
        if (stop || Thread.currentThread().isInterrupted()) {
            return false;
        } else if (drain) {
            long now = System.currentTimeMillis();
            // for a source we take lastTimerExecution starvation
            if (metadata.inputStreams().isEmpty()) {
                if (lastTimerExecution > 0 && (now - lastTimerExecution) > STARVING_TIMEOUT_MS) {
                    log.info(metadata.name() + ": End of source drain, last timer " + STARVING_TIMEOUT_MS + " ms ago");
                    return false;
                }
            } else {
                if ((now - lastReadTime) > STARVING_TIMEOUT_MS) {
                    log.info(metadata.name() + ": End of drain no more input after " + (now - lastReadTime) + " ms, "
                            + inRecords + " records read, " + counter + " reads attempt");
                    return false;
                }
            }
        }
        return true;
    }

    protected void processTimer() {
        Map<String, Long> timers = context.getTimers();
        if (timers.isEmpty()) {
            return;
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
            computation.processTimer(context, key, value);
            timerUpdate[0] = true;
        });
        if (timerUpdate[0]) {
            checkSourceLowWatermark();
            lastTimerExecution = now;
            setThreadName("timer");
            checkpointIfNecessary();
        }

    }

    protected void processRecord() throws InterruptedException {
        if (tailer == null) {
            return;
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
            lastReadTime = System.currentTimeMillis();
            inRecords++;
            lowWatermark.mark(record.watermark);
            String from = metadata.reverseMap(logRecord.offset().partition().name());
            // System.out.println(metadata.name() + ": Receive from " + from + " record: " + record);
            computation.processRecord(context, from, record);
            checkRecordFlags(record);
            checkSourceLowWatermark();
            setThreadName("record");
            checkpointIfNecessary();
        }
    }

    protected Duration getTimeoutDuration() {
        // Adapt the duration so we are not throttling when one of the input stream is empty
        return Duration.ofMillis(Math.min(READ_TIMEOUT.toMillis(), System.currentTimeMillis() - lastReadTime));
    }

    protected void checkSourceLowWatermark() {
        long watermark = context.getSourceLowWatermark();
        if (watermark > 0) {
            lowWatermark.mark(Watermark.ofValue(watermark));
            // System.out.println(metadata.name() + ": Set source wm " + lowWatermark);
            context.setSourceLowWatermark(0);
        }
    }

    protected void checkRecordFlags(Record record) {
        if (record.flags.contains(Record.Flag.POISON_PILL)) {
            log.info(metadata.name() + ": Receive POISON PILL");
            context.askForCheckpoint();
            stop = true;
        } else if (record.flags.contains(Record.Flag.COMMIT)) {
            context.askForCheckpoint();
        }
    }

    protected void checkpointIfNecessary() {
        if (context.requireCheckpoint()) {
            boolean completed = false;
            try {
                checkpoint();
                completed = true;
                inCheckpointRecords = inRecords;
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
        // Simulate slow checkpoint
        // try {
        // Thread.sleep(1);
        // } catch (InterruptedException e) {
        // Thread.currentThread().interrupt();
        // throw e;
        // }
        saveOffsets();
        lowWatermark.checkpoint();
        // System.out.println("checkpoint " + metadata.name() + " " + lowWatermark);
        context.removeCheckpointFlag();
        log.debug(metadata.name() + ": checkpoint");
        setThreadName("checkpoint");
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
            LogAppender<Record> appender = logManager.getAppender(stream);
            for (Record record : context.getRecords(stream)) {
                // System.out.println(metadata.name() + " send record to " + stream + " lowWatermark " + lowWatermark);
                if (record.watermark == 0) {
                    // use low watermark when not set
                    record.watermark = lowWatermark.getLow().getValue();
                }
                appender.append(record.key, record);
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
        setThreadName("rebalance assigned");
        // reset the context
        this.context = new ComputationContextImpl(metadata);
        log.debug(metadata.name() + ": Init");
        computation.init(context);
        lastReadTime = System.currentTimeMillis();
        lastTimerExecution = 0;
        assignmentLatch.countDown();
        // what about watermark ?
    }
}
