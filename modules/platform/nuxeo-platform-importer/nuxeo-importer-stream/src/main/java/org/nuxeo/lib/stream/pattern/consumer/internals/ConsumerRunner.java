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
package org.nuxeo.lib.stream.pattern.consumer.internals;

import static java.lang.Thread.currentThread;
import static org.nuxeo.lib.stream.codec.NoCodec.NO_CODEC;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.RebalanceException;
import org.nuxeo.lib.stream.log.RebalanceListener;
import org.nuxeo.lib.stream.pattern.Message;
import org.nuxeo.lib.stream.pattern.consumer.BatchPolicy;
import org.nuxeo.lib.stream.pattern.consumer.Consumer;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerFactory;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPolicy;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerStatus;

import io.dropwizard.metrics5.Counter;
import io.dropwizard.metrics5.MetricRegistry;
import io.dropwizard.metrics5.SharedMetricRegistries;
import io.dropwizard.metrics5.Timer;

import net.jodah.failsafe.Execution;

/**
 * Read messages from a tailer and drive a consumer according to its policy.
 *
 * @since 9.1
 */
public class ConsumerRunner<M extends Message> implements Callable<ConsumerStatus>, RebalanceListener {
    private static final Log log = LogFactory.getLog(ConsumerRunner.class);

    // This is the registry name used by Nuxeo without adding a dependency nuxeo-runtime
    public static final String NUXEO_METRICS_REGISTRY_NAME = "org.nuxeo.runtime.metrics.MetricsService";

    protected final ConsumerFactory<M> factory;

    protected final ConsumerPolicy policy;

    protected final LogTailer<M> tailer;

    protected String consumerId;

    protected BatchPolicy currentBatchPolicy;

    protected String threadName;

    protected Consumer<M> consumer;

    protected final MetricRegistry registry = SharedMetricRegistries.getOrCreate(NUXEO_METRICS_REGISTRY_NAME);

    protected long acceptCounter;

    protected long committedCounter;

    protected long batchCommitCounter;

    protected long batchFailureCounter;

    protected boolean alreadySalted;

    // Metrics global to all threads, having metrics per thread is way too much
    protected Timer globalAcceptTimer;

    protected Counter globalCommittedCounter;

    protected Timer globalBatchCommitTimer;

    protected Counter globalBatchFailureCounter;

    protected Counter globalConsumersCounter;

    /**
     * @deprecated since 11.1, due to serialization issue with java 11, use
     *             {@link #ConsumerRunner(ConsumerFactory, ConsumerPolicy, LogManager, Codec, List)} which allows to
     *             give a {@link org.nuxeo.lib.stream.codec.Codec codec} to {@link org.nuxeo.lib.stream.log.LogTailer
     *             tailer}.
     */
    @Deprecated
    @SuppressWarnings("unchecked")
    public ConsumerRunner(ConsumerFactory<M> factory, ConsumerPolicy policy, LogManager manager,
            List<LogPartition> defaultAssignments) {
        this(factory, policy, manager, NO_CODEC, defaultAssignments);
    }

    public ConsumerRunner(ConsumerFactory<M> factory, ConsumerPolicy policy, LogManager manager, Codec<M> codec,
            List<LogPartition> defaultAssignments) {
        this.factory = factory;
        this.currentBatchPolicy = policy.getBatchPolicy();
        this.policy = policy;
        this.tailer = createTailer(manager, codec, defaultAssignments);
        consumerId = tailer.toString();
        globalConsumersCounter = registry.counter(MetricRegistry.name("nuxeo", "importer", "stream", "consumers"));
        setTailerPosition(manager);
        log.debug("Consumer thread created tailing on: " + consumerId);
    }

    protected LogTailer<M> createTailer(LogManager manager, Codec<M> codec, List<LogPartition> defaultAssignments) {
        LogTailer<M> tailer;
        if (manager.supportSubscribe()) {
            Set<Name> names = defaultAssignments.stream().map(LogPartition::name).collect(Collectors.toSet());
            tailer = manager.subscribe(Name.ofUrn(policy.getName()), names, this, codec);
        } else {
            tailer = manager.createTailer(Name.ofUrn(policy.getName()), defaultAssignments, codec);
        }
        return tailer;
    }

    @Override
    public ConsumerStatus call() throws Exception {
        threadName = currentThread().getName();
        setMetrics();
        globalConsumersCounter.inc();
        long start = System.currentTimeMillis();
        consumer = factory.createConsumer(consumerId);
        try {
            consumerLoop();
        } finally {
            consumer.close();
            globalConsumersCounter.dec();
            tailer.close();
        }
        return new ConsumerStatus(consumerId, acceptCounter, committedCounter, batchCommitCounter, batchFailureCounter,
                start, System.currentTimeMillis(), false);
    }

    protected void setMetrics() {
        globalAcceptTimer = registry.timer(MetricRegistry.name("nuxeo", "importer", "stream", "consumer", "accepted"));
        globalCommittedCounter = registry.counter(
                MetricRegistry.name("nuxeo", "importer", "stream", "consumer", "committed"));
        globalBatchFailureCounter = registry.counter(
                MetricRegistry.name("nuxeo", "importer", "stream", "consumer", "batchFailure"));
        globalBatchCommitTimer = registry.timer(
                MetricRegistry.name("nuxeo", "importer", "stream", "consumer", "batchCommit"));
    }

    protected void addSalt() throws InterruptedException {
        if (alreadySalted) {
            return;
        }
        // this random delay prevent consumers to be too much synchronized
        if (policy.isSalted()) {
            long randomDelay = ThreadLocalRandom.current()
                                                .nextLong(policy.getBatchPolicy().getTimeThreshold().toMillis());
            Thread.sleep(randomDelay);
        }
        alreadySalted = true;
    }

    protected void setTailerPosition(LogManager manager) {
        ConsumerPolicy.StartOffset seekPosition = policy.getStartOffset();
        if (manager.supportSubscribe() && seekPosition != ConsumerPolicy.StartOffset.LAST_COMMITTED) {
            throw new UnsupportedOperationException(
                    "Tailer startOffset to " + seekPosition + " is not supported in subscribe mode");
        }
        switch (policy.getStartOffset()) {
        case BEGIN:
            tailer.toStart();
            break;
        case END:
            tailer.toEnd();
            break;
        default:
            tailer.toLastCommitted();
        }
    }

    protected void consumerLoop() throws InterruptedException {
        boolean end = false;
        while (!end) {
            Execution execution = new Execution(policy.getRetryPolicy());
            end = processBatchWithRetry(execution);
            if (execution.getLastFailure() != null) {
                if (policy.continueOnFailure()) {
                    log.error("Skip message on failure after applying the retry policy: ", execution.getLastFailure());
                } else {
                    log.error("Abort on Failure after applying the retry policy: ", execution.getLastFailure());
                    end = true;
                }
            }
        }
    }

    protected boolean processBatchWithRetry(Execution execution) throws InterruptedException {
        boolean end = false;
        while (!execution.isComplete()) {
            try {
                end = processBatch();
                tailer.commit();
                execution.complete();
            } catch (Throwable t) {
                globalBatchFailureCounter.inc();
                batchFailureCounter += 1;
                if (t instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    throw t;
                }
                if (t instanceof RebalanceException) {
                    log.info("Rebalance");
                    // the current batch is rollback because of this exception
                    // we continue with the new tailer assignment
                } else if (execution.canRetryOn(t)) {
                    setBatchRetryPolicy();
                    tailer.toLastCommitted();
                } else {
                    throw t;
                }
            }
            restoreBatchPolicy();
        }
        return end;
    }

    protected void setBatchRetryPolicy() {
        currentBatchPolicy = BatchPolicy.NO_BATCH;
    }

    protected void restoreBatchPolicy() {
        currentBatchPolicy = policy.getBatchPolicy();
    }

    protected boolean processBatch() throws InterruptedException {
        boolean end = false;
        beginBatch();
        try {
            BatchState state = acceptBatch();
            commitBatch(state);
            if (state.getState() == BatchState.State.LAST) {
                log.info("No more message on tailer: " + tailer);
                end = true;
            }
        } catch (Exception e) {
            try {
                rollbackBatch(e);
            } catch (Exception rollbackException) {
                log.error("Exception on rollback invocation", rollbackException);
                // we propagate the initial error.
            }
            throw e;
        }
        return end;
    }

    protected void beginBatch() {
        consumer.begin();
    }

    protected void commitBatch(BatchState state) {
        try (Timer.Context ignore = globalBatchCommitTimer.time()) {
            consumer.commit();
            committedCounter += state.getSize();
            globalCommittedCounter.inc(state.getSize());
            batchCommitCounter += 1;
            if (log.isDebugEnabled()) {
                log.debug("Commit batch size: " + state.getSize() + ", total committed: " + committedCounter);
            }
        }
    }

    protected void rollbackBatch(Exception e) {
        if (e instanceof RebalanceException) {
            log.warn("Rollback current batch because of consumer rebalancing");
        } else {
            log.warn("Rollback batch", e);
        }
        consumer.rollback();
    }

    protected BatchState acceptBatch() throws InterruptedException {
        BatchState batch = new BatchState(currentBatchPolicy);
        batch.start();
        LogRecord<M> record;
        M message;
        while ((record = tailer.read(policy.getWaitMessageTimeout())) != null) {
            addSalt(); // do this here so kafka subscription happens concurrently
            message = record.message();
            if (message.poisonPill()) {
                log.warn("Receive a poison pill: " + message);
                batch.last();
            } else {
                try (Timer.Context ignore = globalAcceptTimer.time()) {
                    setThreadName(message.getId());
                    consumer.accept(message);
                    acceptCounter += 1;
                }
                batch.inc();
                if (message.forceBatch()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Force end of batch: " + message);
                    }
                    batch.force();
                }
            }
            if (batch.getState() != BatchState.State.FILLING) {
                return batch;
            }
        }
        batch.last();
        log.info(String.format("No record after: %ds on %s, terminating", policy.getWaitMessageTimeout().getSeconds(),
                consumerId));
        return batch;
    }

    protected void setThreadName(String message) {
        String name = threadName + "-" + acceptCounter + "-" + message;
        currentThread().setName(name);
    }

    @Override
    public void onPartitionsRevoked(Collection<LogPartition> partitions) {
        // log.info("Partitions revoked: " + partitions);
    }

    @Override
    public void onPartitionsAssigned(Collection<LogPartition> partitions) {
        consumerId = tailer.toString();
        setThreadName("rebalance-" + consumerId);
        // log.error("Partitions assigned: " + consumerId);
        // partitions are opened on last committed by default
    }
}
