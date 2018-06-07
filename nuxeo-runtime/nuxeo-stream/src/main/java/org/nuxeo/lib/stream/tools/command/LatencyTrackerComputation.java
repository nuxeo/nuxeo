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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

    protected final List<String> logNames;

    protected final int intervalMs;

    protected final int count;

    protected final boolean verbose;

    protected final Codec<Record> codec;

    protected int remaining;

    protected List<LogPartitionGroup> logGroups;

    public LatencyTrackerComputation(LogManager manager, List<String> logNames, String computationName,
            int intervalSecond, int count, boolean verbose, Codec<Record> codec) {
        super(computationName, 1, 1);
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
        info(String.format("Tracking %s, count: %d, interval: %dms", Arrays.toString(logNames.toArray()), count,
                intervalMs));
        logGroups = new ArrayList<>();
        logNames.forEach(name -> {
            for (String group : manager.listConsumerGroups(name)) {
                logGroups.add(new LogPartitionGroup(group, name, 0));
            }
        });
        context.setTimer("tracker", System.currentTimeMillis() + intervalMs);
    }

    @Override
    public void processTimer(ComputationContext context, String key, long timestamp) {
        if (remaining == 0) {
            debug("Exiting after " + count + " captures");
            context.askForTermination();
            return;
        }
        debug(String.format("Tracking latency %d/%d", count - remaining, count));
        for (LogPartitionGroup logGroup : logGroups) {
            List<Latency> latencies = getLatenciesForPartition(logGroup, codec);
            if (latencies.isEmpty()) {
                continue;
            }
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
                    debug("out: " + record);
                }
                context.produceRecord(OUTPUT_STREAM, record);
                context.setSourceLowWatermark(recordWatermark);
            }
        }
        context.askForCheckpoint();
        context.setTimer("tracker", System.currentTimeMillis() + intervalMs);
        remaining--;
    }

    protected byte[] encodeLatency(Latency latency) {
        try {
            return latency.asJson().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Failed to byte encoding " + latency, e);
        }
    }

    @SuppressWarnings("squid:S1193")
    protected List<Latency> getLatenciesForPartition(LogPartitionGroup logGroup, Codec<Record> codec) {
        try {
            return manager.getLatencyPerPartition(logGroup.name, logGroup.group, codec,
                    (rec -> Watermark.ofValue(rec.getWatermark()).getTimestamp()), (Record::getKey));
        } catch (Exception e) {
            if (e.getCause() instanceof ClassNotFoundException || e instanceof IllegalStateException) {
                error("log does not contains Record, remove partition: " + logGroup);
                return Collections.emptyList();
            }
            throw e;
        }
    }

    public static String encodeKey(LogPartitionGroup logGroup, int partition) {
        return String.format("%s:%s:%s", logGroup.group, logGroup.name, partition);
    }

    public static LogPartitionGroup decodeKey(String key) {
        String[] parts = key.split(":");
        return new LogPartitionGroup(parts[0], parts[1], Integer.parseInt(parts[2]));
    }

    @Override
    public void destroy() {
        info("Good bye");
    }

    @Override
    public void processRecord(ComputationContext context, String inputStreamName, Record record) {
        error("Receiving a record is not expected!: " + record);
    }

    protected void debug(String msg) {
        if (verbose) {
            log.info(msg);
        }
    }

    protected void info(String msg) {
        log.info(msg);
    }

    protected void error(String msg) {
        log.error(msg);
    }
}
