/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.lib.stream.tools.command;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.AbstractComputation;
import org.nuxeo.lib.stream.computation.ComputationContext;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.Latency;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;
import org.nuxeo.lib.stream.log.internals.LogPartitionGroup;

/**
 * A computation that sends periodically latencies information into a Log.
 *
 * @since 10.1
 */
public class LatencyTrackerComputation extends AbstractComputation {
    private static final Log log = LogFactory.getLog(LatencyTrackerComputation.class);

    protected static final String OUTPUT_STREAM = "o1";

    protected final LogManager manager;

    protected final List<Name> logNames;

    protected final int intervalMs;

    protected final int count;

    protected final boolean verbose;

    protected final Codec<Record> codec;

    protected int remaining;

    protected final List<LogPartitionGroup> logGroups = new ArrayList<>();

    protected int refreshGroupCounter;

    public LatencyTrackerComputation(LogManager manager, List<Name> logNames, String computationName,
            int intervalSecond, int count, boolean verbose, Codec<Record> codec, int outputStream) {
        super(computationName, 1, outputStream);
        this.manager = manager;
        this.logNames = logNames;
        this.intervalMs = 1000 * intervalSecond;
        this.count = count;
        this.remaining = count;
        this.verbose = verbose;
        this.codec = codec;
    }

    @Override
    public void init(ComputationContext context) {
        log.info(String.format("Tracking %d streams: %s, count: %d, interval: %dms", logNames.size(),
                Arrays.toString(logNames.toArray()), count,
                intervalMs));
        context.setTimer("tracker", System.currentTimeMillis() + intervalMs);
    }

    @Override
    public void processTimer(ComputationContext context, String key, long timestamp) {
        if (remaining == 0) {
            if (verbose) {
                log.info("Exiting after " + count + " captures");
            }
            context.askForTermination();
            return;
        }
        if (verbose) {
            log.info(String.format("Tracking latency %d/%d", count - remaining, count));
        }
        List<LogPartitionGroup> toRemove = new ArrayList<>();
        for (LogPartitionGroup logGroup : getLogGroup()) {
            try {
                List<Latency> latencies = manager.getLatencyPerPartition(logGroup.name, logGroup.group, codec,
                        (rec -> Watermark.ofValue(rec.getWatermark()).getTimestamp()), (Record::getKey));
                if (!latencies.isEmpty()) {
                    processLatencies(context, logGroup, latencies);
                }
            } catch (Exception e) {
                if (e.getCause() instanceof ClassNotFoundException || e.getCause() instanceof ClassCastException
                        || e instanceof IllegalStateException || e instanceof IllegalArgumentException) {
                    log.warn("log does not contains computation Record, removing partition: " + logGroup);
                    toRemove.add(logGroup);
                    continue;
                }
                throw e;
            }
        }
        context.askForCheckpoint();
        context.setTimer("tracker", System.currentTimeMillis() + intervalMs);
        remaining--;
        if (!toRemove.isEmpty()) {
            logGroups.removeAll(toRemove);
            if (logGroups.isEmpty()) {
                log.error("Exiting because all logs have been skipped");
                context.askForTermination();
            }
        }
    }

    protected List<LogPartitionGroup> getLogGroup() {
        if (logGroups.isEmpty() || refreshGroup()) {
            logGroups.clear();
            logNames.forEach(name -> {
                for (Name group : manager.listConsumerGroups(name)) {
                    logGroups.add(new LogPartitionGroup(group, name, 0));
                }
            });
            if (verbose) {
                log.info("Update list of consumers: " + Arrays.toString(logGroups.toArray()));
            }
        }
        return logGroups;
    }

    protected boolean refreshGroup() {
        refreshGroupCounter += 1;
        return (refreshGroupCounter % 5) == 0;
    }

    protected void processLatencies(ComputationContext context, LogPartitionGroup logGroup, List<Latency> latencies) {
        for (int partition = 0; partition < latencies.size(); partition++) {
            Latency latency = latencies.get(partition);
            if (latency.lower() <= 0) {
                // lower is the watermark timestamp for the latest processed record, without this info we cannot do
                // anything
                continue;
            }
            // upper is the time when the latency has been measured it is used as the watermark
            long recordWatermark = Watermark.ofTimestamp(latency.upper()).getValue();
            String recordKey = encodeKey(logGroup, partition);
            byte[] recordValue = encodeLatency(latency);
            Record record = new Record(recordKey, recordValue, recordWatermark);
            if (verbose) {
                log.info("out: " + record);
            }
            context.produceRecord(OUTPUT_STREAM, record);
            context.setSourceLowWatermark(recordWatermark);
        }
    }

    protected byte[] encodeLatency(Latency latency) {
        return latency.asJson().getBytes(StandardCharsets.UTF_8);
    }

    public static String encodeKey(LogPartitionGroup logGroup, int partition) {
        return String.format("%s:%s:%s", logGroup.group.getId(), logGroup.name.getId(), partition);
    }

    public static LogPartitionGroup decodeKey(String key) {
        String[] parts = key.split(":");
        return new LogPartitionGroup(Name.ofId(parts[0]), Name.ofId(parts[1]), Integer.parseInt(parts[2]));
    }

    @Override
    public void destroy() {
        log.info("Good bye");
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        log.error("Receiving a record is not expected: " + record);
    }

}
