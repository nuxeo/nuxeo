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
package org.nuxeo.lib.stream.tools.command;

import static org.nuxeo.lib.stream.tools.command.LatencyTrackerComputation.decodeKey;
import static org.nuxeo.lib.stream.tools.command.PositionCommand.FIRST_READ_TIMEOUT;
import static org.nuxeo.lib.stream.tools.command.PositionCommand.READ_TIMEOUT;
import static org.nuxeo.lib.stream.tools.command.PositionCommand.getTimestampFromDate;
import static org.nuxeo.lib.stream.tools.command.TrackerCommand.ALL_LOGS;
import static org.nuxeo.lib.stream.tools.command.TrackerCommand.DEFAULT_LATENCIES_LOG;
import static org.nuxeo.lib.stream.tools.command.TrackerCommand.INTERNAL_LOG_PREFIX;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.Latency;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.internals.LogPartitionGroup;

/**
 * Restore consumer positions using the latency tracker Log.
 *
 * @since 10.1
 */
public class RestoreCommand extends Command {
    private static final Log log = LogFactory.getLog(RestoreCommand.class);

    protected static final String NAME = "restore";

    protected static final String GROUP = "tools";

    protected boolean verbose = false;

    protected String input;

    protected List<String> logNames;

    protected long date;

    protected boolean dryRun;

    protected String codec;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void updateOptions(Options options) {
        options.addOption(Option.builder("l")
                                .longOpt("log-name")
                                .desc("Restore consumers positions for this LOG, must be a computation Record, "
                                        + "can be a comma separated list of log names or ALL")
                                .required()
                                .hasArg()
                                .argName("LOG_NAME")
                                .build());
        options.addOption(Option.builder("i")
                                .longOpt("log-input")
                                .desc("Log name of the input default to " + DEFAULT_LATENCIES_LOG)
                                .hasArg()
                                .argName("LOG_INPUT")
                                .build());
        options.addOption(Option.builder()
                                .longOpt("to-date")
                                .desc("Sets the committed positions as they where at a specific date."
                                        + " The date is specified in ISO-8601 format, eg. " + Instant.now())
                                .hasArg()
                                .argName("DATE")
                                .build());
        options.addOption(Option.builder()
                                .longOpt("codec")
                                .desc("Codec used to read record, can be: java, avro, avroBinary, avroJson")
                                .hasArg()
                                .argName("CODEC")
                                .build());
        options.addOption(Option.builder().longOpt("verbose").build());
        options.addOption(Option.builder().longOpt("dry-run").desc("Do not change any position").build());
    }

    @Override
    public boolean run(LogManager manager, CommandLine cmd) throws InterruptedException {
        logNames = getLogNames(manager, cmd.getOptionValue("log-name"));
        input = cmd.getOptionValue("log-input", DEFAULT_LATENCIES_LOG);
        date = getTimestampFromDate(cmd.getOptionValue("to-date"));
        verbose = cmd.hasOption("verbose");
        dryRun = cmd.hasOption("dry-run");
        codec = cmd.getOptionValue("codec");
        return restorePosition(manager);
    }

    protected boolean restorePosition(LogManager manager) throws InterruptedException {
        Map<LogPartitionGroup, Latency> latencies = readLatencies(manager);
        Map<LogPartitionGroup, LogOffset> offsets = searchOffsets(manager, latencies);
        if (dryRun) {
            log.info("# Dry run mode returning without doing any changes");
            return true;
        }
        updatePositions(manager, offsets);
        return true;
    }

    protected void updatePositions(LogManager manager, Map<LogPartitionGroup, LogOffset> offsets) {
        log.info("# Update positions");
        offsets.forEach((key, offset) -> updatePosition(manager, key, offset));
    }

    protected void updatePosition(LogManager manager, LogPartitionGroup key, LogOffset offset) {
        if (offset == null) {
            return;
        }
        log.info(key + " new position: " + offset);
        try (LogTailer<Record> tailer = manager.createTailer(key.group, key.getLogPartition(), getRecordCodec(codec))) {
            tailer.seek(offset);
            tailer.commit();
        }
    }

    protected Map<LogPartitionGroup, LogOffset> searchOffsets(LogManager manager,
            Map<LogPartitionGroup, Latency> latencies) throws InterruptedException {
        Map<LogPartitionGroup, LogOffset> ret = new HashMap<>(latencies.size());
        log.info("# Searching records matching the latencies lower timestamp and key");
        for (Map.Entry<LogPartitionGroup, Latency> entry : latencies.entrySet()) {
            ret.put(entry.getKey(), findOffset(manager, entry.getKey(), entry.getValue()));
        }
        return ret;
    }

    protected LogOffset findOffset(LogManager manager, LogPartitionGroup key, Latency latency)
            throws InterruptedException {
        long targetWatermark = latency.lower();
        String targetKey = latency.key();
        try (LogTailer<Record> tailer = manager.createTailer(GROUP, key.getLogPartition(), getRecordCodec(codec))) {
            for (LogRecord<Record> rec = tailer.read(FIRST_READ_TIMEOUT); rec != null; rec = tailer.read(
                    READ_TIMEOUT)) {
                if (targetKey != null && !targetKey.equals(rec.message().getKey())) {
                    continue;
                }
                long timestamp = Watermark.ofValue(rec.message().getWatermark()).getTimestamp();
                if (targetWatermark == timestamp) {
                    log.info(String.format("%s: offset: %s wm: %d key: %s", key, rec.offset(),
                            rec.message().getWatermark(), rec.message().getKey()));
                    return rec.offset().nextOffset();
                }
            }
        }
        log.error("No offset found for: " + key + ", matching: " + latency.asJson());
        return null;
    }

    protected Map<LogPartitionGroup, Latency> readLatencies(LogManager manager) throws InterruptedException {
        Map<LogPartitionGroup, Latency> latencies = new HashMap<>();
        log.info("# Reading latencies log: " + input + ", searching for the higher timestamp <= " + date);
        try (LogTailer<Record> tailer = manager.createTailer(GROUP, input, getRecordCodec(codec))) {
            for (LogRecord<Record> rec = tailer.read(FIRST_READ_TIMEOUT); rec != null; rec = tailer.read(
                    READ_TIMEOUT)) {
                long timestamp = Watermark.ofValue(rec.message().getWatermark()).getTimestamp();
                if (date > 0 && timestamp > date) {
                    continue;
                }
                LogPartitionGroup key = decodeKey(rec.message().getKey());
                if (!logNames.contains(key.name)) {
                    continue;
                }
                Latency latency = decodeLatency(rec.message().getData());
                if (latency != null && latency.lower() > 0) {
                    // we don't want latency.lower = 0, this means either no record either records with unset watermark
                    latencies.put(key, latency);
                }
            }
        }
        log.info("# Latencies found (group:log:partition -> lat)");
        latencies.forEach((key, latency) -> log.info(String.format("%s: %s", key, latency.asJson())));
        return latencies;
    }

    protected Latency decodeLatency(byte[] data) {
        return Latency.fromJson(new String(data, StandardCharsets.UTF_8));
    }

    protected List<String> getLogNames(LogManager manager, String names) {
        if (ALL_LOGS.equalsIgnoreCase(names)) {
            return manager.listAll()
                          .stream()
                          .filter(name -> !name.startsWith(INTERNAL_LOG_PREFIX))
                          .collect(Collectors.toList());
        }
        List<String> ret = Arrays.asList(names.split(","));
        for (String name : ret) {
            if (!manager.exists(name)) {
                throw new IllegalArgumentException("Unknown log name: " + name);
            }
        }
        return ret;
    }

}
