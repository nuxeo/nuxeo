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

import static java.util.concurrent.Executors.newFixedThreadPool;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Computation;
import org.nuxeo.lib.stream.computation.ComputationMetadataMapping;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogPartition;

/**
 * Pool of ComputationRunner
 *
 * @since 9.3
 */
public class ComputationPool {
    private static final Log log = LogFactory.getLog(ComputationPool.class);

    protected final ComputationMetadataMapping metadata;

    protected final int threads;

    protected final LogManager manager;

    protected final Supplier<Computation> supplier;

    protected final List<List<LogPartition>> defaultAssignments;

    protected final List<ComputationRunner> runners;

    protected final Codec<Record> inputCodec;

    protected final Codec<Record> outputCodec;

    protected ExecutorService threadPool;

    public ComputationPool(Supplier<Computation> supplier, ComputationMetadataMapping metadata,
            List<List<LogPartition>> defaultAssignments, LogManager manager, Codec<Record> inputCodec,
            Codec<Record> outputCodec) {
        Objects.requireNonNull(inputCodec);
        Objects.requireNonNull(outputCodec);
        this.supplier = supplier;
        this.manager = manager;
        this.metadata = metadata;
        this.threads = defaultAssignments.size();
        this.inputCodec = inputCodec;
        this.outputCodec = outputCodec;
        this.defaultAssignments = defaultAssignments;
        this.runners = new ArrayList<>(threads);
    }

    public String getComputationName() {
        return metadata.name();
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public void start() {
        log.info(metadata.name() + ": Starting pool");
        threadPool = newFixedThreadPool(threads, new NamedThreadFactory(metadata.name() + "Pool"));
        defaultAssignments.forEach(assignments -> {
            ComputationRunner runner = new ComputationRunner(supplier, metadata, assignments, manager, inputCodec,
                    outputCodec);
            threadPool.submit(runner);
            runners.add(runner);
        });
        // close the pool no new admission
        threadPool.shutdown();
        log.debug(metadata.name() + ": Pool started, threads: " + threads);
    }

    public boolean isTerminated() {
        return threadPool.isTerminated();
    }

    public boolean waitForAssignments(Duration timeout) throws InterruptedException {
        log.info(metadata.name() + ": Wait for partitions assignments");
        if (threadPool == null || threadPool.isTerminated()) {
            return true;
        }
        for (ComputationRunner runner : runners) {
            if (!runner.waitForAssignments(timeout)) {
                return false;
            }
        }
        return true;
    }

    public boolean drainAndStop(Duration timeout) {
        if (threadPool == null || threadPool.isTerminated()) {
            return true;
        }
        log.info(metadata.name() + ": Draining");
        runners.forEach(ComputationRunner::drain);
        boolean ret = awaitPoolTermination(timeout);
        stop(Duration.ofSeconds(1));
        return ret;
    }

    public boolean stop(Duration timeout) {
        if (threadPool == null || threadPool.isTerminated()) {
            return true;
        }
        log.info(metadata.name() + ": Stopping");
        runners.forEach(ComputationRunner::stop);
        boolean ret = awaitPoolTermination(timeout);
        shutdown();
        return ret;
    }

    public void shutdown() {
        if (threadPool != null && !threadPool.isTerminated()) {
            log.info(metadata.name() + ": Shutting down");
            threadPool.shutdownNow();
            // give a chance to end threads with valid tailer when shutdown is followed by streams.close()
            try {
                threadPool.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn(metadata.name() + ": Interrupted in shutdown");
            }
        }
        runners.clear();
        threadPool = null;
    }

    protected boolean awaitPoolTermination(Duration timeout) {
        try {
            if (!threadPool.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                log.warn(metadata.name() + ": Timeout on wait for pool termination");
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn(metadata.name() + ": Interrupted while waiting for pool termination");
            return false;
        }
        return true;
    }

    public long getLowWatermark() {
        // Collect all the low watermark of the pool, filtering 0 (or 1 which is completed of 0)
        Set<Watermark> watermarks = runners.stream()
                                           .map(ComputationRunner::getLowWatermark)
                                           .filter(wm -> wm.getValue() > 1)
                                           .collect(Collectors.toSet());
        // Take the lowest watermark of unprocessed (not completed) records
        long ret = watermarks.stream().filter(wm -> !wm.isCompleted()).mapToLong(Watermark::getValue).min().orElse(0);
        boolean pending = true;
        if (ret == 0) {
            pending = false;
            // There is no known pending records we take the max completed low watermark
            ret = watermarks.stream().filter(Watermark::isCompleted).mapToLong(Watermark::getValue).max().orElse(0);
        }
        if (log.isTraceEnabled() && ret > 0)
            log.trace(metadata.name() + ": low: " + ret + " " + (pending ? "Pending" : "Completed"));
        return ret;
    }

    protected static class NamedThreadFactory implements ThreadFactory {
        protected final AtomicInteger count = new AtomicInteger(0);

        protected final String prefix;

        public NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @SuppressWarnings("NullableProblems")
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, String.format("%s-%02d", prefix, count.getAndIncrement()));
            t.setUncaughtExceptionHandler((t1, e) -> log.error("Uncaught exception: " + e.getMessage(), e));
            return t;
        }
    }

}
