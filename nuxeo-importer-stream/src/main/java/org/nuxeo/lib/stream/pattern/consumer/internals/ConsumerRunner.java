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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.pattern.Message;
import org.nuxeo.lib.stream.pattern.consumer.BatchPolicy;
import org.nuxeo.lib.stream.pattern.consumer.Consumer;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerFactory;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerPolicy;
import org.nuxeo.lib.stream.pattern.consumer.ConsumerStatus;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.RebalanceException;
import org.nuxeo.lib.stream.log.RebalanceListener;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;

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

    protected Timer acceptTimer;

    protected Counter committedCounter;

    protected Timer batchCommitTimer;

    protected Counter batchFailureCount;

    protected Counter consumersCount;

    protected boolean alreadySalted = false;

    public ConsumerRunner(ConsumerFactory<M> factory, ConsumerPolicy policy, LogManager manager,
            List<LogPartition> defaultAssignments) {
        this.factory = factory;
        this.currentBatchPolicy = policy.getBatchPolicy();
        this.policy = policy;
        this.tailer = createTailer(manager, defaultAssignments);
        consumerId = tailer.toString();
        consumersCount = newCounter(MetricRegistry.name("nuxeo", "importer", "queue", "consumers"));
        setTailerPosition(manager);
        log.debug("Consumer thread created tailing on: " + consumerId);
    }

    protected LogTailer<M> createTailer(LogManager manager, List<LogPartition> defaultAssignments) {
        LogTailer<M> tailer;
        if (manager.supportSubscribe()) {
            Set<String> names = defaultAssignments.stream().map(LogPartition::name).collect(Collectors.toSet());
            tailer = manager.subscribe(policy.getName(), names, this);
        } else {
            tailer = manager.createTailer(policy.getName(), defaultAssignments);
        }
        return tailer;
    }

    protected Counter newCounter(String name) {
        registry.remove(name);
        return registry.counter(name);
    }

    protected Timer newTimer(String name) {
        registry.remove(name);
        return registry.timer(name);
    }

    @Override
    public ConsumerStatus call() throws Exception {
        threadName = currentThread().getName();
        setMetrics(threadName);
        consumersCount.inc();
        long start = System.currentTimeMillis();
        consumer = factory.createConsumer(consumerId);
        try {
            consumerLoop();
        } finally {
            consumer.close();
            consumersCount.dec();
            tailer.close();
        }
        return new ConsumerStatus(consumerId, acceptTimer.getCount(), committedCounter.getCount(),
                batchCommitTimer.getCount(), batchFailureCount.getCount(), start, System.currentTimeMillis(), false);
    }

    protected void setMetrics(String name) {
        acceptTimer = newTimer(MetricRegistry.name("nuxeo", "importer", "queue", "consumer", "accepted", name));
        committedCounter = newCounter(MetricRegistry.name("nuxeo", "importer", "queue", "consumer", "committed", name));
        batchFailureCount = newCounter(
                MetricRegistry.name("nuxeo", "importer", "queue", "consumer", "batchFailure", name));
        batchCommitTimer = newTimer(MetricRegistry.name("nuxeo", "importer", "queue", "consumer", "batchCommit", name));
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
                batchFailureCount.inc();
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
        try (Timer.Context ignore = batchCommitTimer.time()) {
            consumer.commit();
            committedCounter.inc(state.getSize());
            if (log.isDebugEnabled()) {
                log.debug(
                        "Commit batch size: " + state.getSize() + ", total committed: " + committedCounter.getCount());
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
            // addSalt(); // do this here so kafka subscription happens concurrently
            message = record.message();
            if (message.poisonPill()) {
                log.warn("Receive a poison pill: " + message);
                batch.last();
            } else {
                try (Timer.Context ignore = acceptTimer.time()) {
                    setThreadName(message);
                    consumer.accept(message);
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
        return batch;
    }

    protected void setThreadName(M message) {
        String name = threadName + "-" + acceptTimer.getCount();
        if (message != null) {
            name += "-" + message.getId();
        } else {
            name += "-null";
        }
        currentThread().setName(name);
    }

    @Override
    public void onPartitionsRevoked(Collection<LogPartition> partitions) {
        // log.info("Partitions revoked: " + partitions);
    }

    @Override
    public void onPartitionsAssigned(Collection<LogPartition> partitions) {
        consumerId = tailer.toString();
        // log.error("Partitions assigned: " + consumerId);
        // partitions are opened on last committed by default
    }
}
