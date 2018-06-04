/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Gethin James
 */
package org.nuxeo.lib.stream.tools.command;

import java.io.Externalizable;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;

/**
 * Manipulates the consumer position to the beginning, end or a specific timestamp
 *
 * @since 10.1
 */
public class PositionCommand extends Command {
    private static final Log log = LogFactory.getLog(PositionCommand.class);

    public static final Duration FIRST_READ_TIMEOUT = Duration.ofMillis(1000);

    public static final Duration READ_TIMEOUT = Duration.ofMillis(100);

    protected static final String NAME = "position";

    public static final String AFTER_DATE_OPT = "after-date";

    public static final String TO_WATERMARK_OPT = "to-watermark";

    protected static long getTimestampFromDate(String dateIso8601) {
        if (dateIso8601 == null || dateIso8601.isEmpty()) {
            return -1;
        }
        try {
            Instant instant = Instant.parse(dateIso8601);
            return instant.toEpochMilli();
        } catch (DateTimeException e) {
            log.error("Failed to read the timeout: " + e.getMessage());
            log.error("The timestamp should be in ISO-8601 format, eg. " + Instant.now());
        }
        return -1;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void updateOptions(Options options) {
        options.addOption(Option.builder("l")
                                .longOpt("log-name")
                                .desc("Log name")
                                .required()
                                .hasArg()
                                .argName("LOG_NAME")
                                .build());
        options.addOption(
                Option.builder("g").longOpt("group").desc("Consumer group").hasArg().argName("GROUP").build());
        options.addOption(
                Option.builder().longOpt("reset").desc("Resets all committed positions for the group").build());
        options.addOption(Option.builder()
                                .longOpt("to-end")
                                .desc("Sets the committed positions to the end of partitions for the group")
                                .build());
        options.addOption(
                Option.builder()
                      .longOpt(AFTER_DATE_OPT)
                      .desc("Sets the committed positions for the group to a specific date."
                              + " The date used to find the offset depends on the implementation, for Kafka this is the"
                              + " LogAppendTime. The position is set to the earliest offset whose timestamp is greater than or equal to the given date."
                              + " The date is specified in ISO-8601 format, eg. " + Instant.now()
                              + ". If no record offset is found with an appropriate timestamp then the command fails.")
                      .hasArg()
                      .argName("DATE")
                      .build());
        options.addOption(
                Option.builder()
                      .longOpt(TO_WATERMARK_OPT)
                      .desc("Sets the committed positions for the group to a specific date."
                              + " The date used to find the offset is contained in a record watermark. "
                              + " This means that the LOG_NAME is expected to be a computation stream with records with populated watermark."
                              + " The position is set to the biggest record offset with a watermark date inferior or equals to the given date.\""
                              + " The date is specified in ISO-8601 format, eg. " + Instant.now()
                              + ". If no record offset is found with an appropriate timestamp then the command fails.")
                      .hasArg()
                      .argName("DATE")
                      .build());
    }

    @Override
    public boolean run(LogManager manager, CommandLine cmd) throws InterruptedException {
        String name = cmd.getOptionValue("log-name");
        String group = cmd.getOptionValue("group", "tools");

        if (cmd.hasOption(AFTER_DATE_OPT)) {
            long timestamp = getTimestampFromDate(cmd.getOptionValue(AFTER_DATE_OPT));
            if (timestamp >= 0) {
                return positionAfterDate(manager, group, name, timestamp);
            }
        } else if (cmd.hasOption(TO_WATERMARK_OPT)) {
            long timestamp = getTimestampFromDate(cmd.getOptionValue(TO_WATERMARK_OPT));
            if (timestamp >= 0) {
                return positionToWatermark(manager, group, name, timestamp);
            }
        } else if (cmd.hasOption("to-end")) {
            return toEnd(manager, group, name);
        } else if (cmd.hasOption("reset")) {
            return reset(manager, group, name);
        } else {
            log.error("Invalid option, try 'help position'");
        }
        return false;
    }

    protected boolean toEnd(LogManager manager, String group, String name) {
        LogLag lag = manager.getLag(name, group);
        try (LogTailer<Externalizable> tailer = manager.createTailer(group, name)) {
            tailer.toEnd();
            tailer.commit();
        }
        log.info(String.format("# Moved log %s, group: %s, from: %s to %s", name, group, lag.lower(), lag.upper()));
        return true;
    }

    protected boolean reset(LogManager manager, String group, String name) {
        LogLag lag = manager.getLag(name, group);
        long pos = lag.lower();
        try (LogTailer<Externalizable> tailer = manager.createTailer(group, name)) {
            tailer.reset();
        }
        log.info(String.format("# Reset log %s, group: %s, from: %s to 0", name, group, pos));
        return true;
    }

    protected boolean positionAfterDate(LogManager manager, String group, String name, long timestamp) {
        try (LogTailer<Externalizable> tailer = manager.createTailer(group, name)) {
            boolean movedOffset = false;
            for (int partition = 0; partition < manager.size(name); partition++) {
                LogPartition logPartition = new LogPartition(name, partition);
                LogOffset logOffset = tailer.offsetForTimestamp(logPartition, timestamp);
                if (logOffset == null) {
                    log.error(String.format("# Could not find an offset for group: %s, partition: %s", group,
                            logPartition));
                    continue;
                }
                tailer.seek(logOffset);
                movedOffset = true;
                log.info(String.format("# Set log %s, group: %s, to offset %s", name, group, logOffset.offset()));
            }
            if (movedOffset) {
                tailer.commit();
                return true;
            }
        }
        log.error("No offset found for the specified date");
        return false;
    }

    protected boolean positionToWatermark(LogManager manager, String group, String name, long timestamp)
            throws InterruptedException {
        String newGroup = "tools";
        int size = manager.size(name);
        List<LogOffset> offsets = new ArrayList<>(size);
        List<LogLag> lags = manager.getLagPerPartition(name, newGroup);
        int partition = 0;
        // find the offsets first
        for (LogLag lag : lags) {
            if (lag.lag() == 0) {
                // empty partition nothing to do
                offsets.add(null);
            } else {
                try (LogTailer<Record> tailer = manager.createTailer(newGroup, new LogPartition(name, partition))) {
                    offsets.add(searchWatermarkOffset(tailer, timestamp));
                }
            }
            partition++;
        }
        if (offsets.stream().noneMatch(Objects::nonNull)) {
            if (LogLag.of(lags).upper() == 0) {
                log.error("No offsets found because log is empty");
                return false;
            }
            log.error("Timestamp: " + timestamp + " is earlier as any records, resetting positions");
            return reset(manager, group, name);
        }
        try (LogTailer<Externalizable> tailer = manager.createTailer(group, name)) {
            offsets.stream().filter(Objects::nonNull).forEach(tailer::seek);
            tailer.commit();
            offsets.stream().filter(Objects::nonNull).forEach(offset -> log.info("# Moving consumer to: " + offset));
        }
        return true;
    }

    protected LogOffset searchWatermarkOffset(LogTailer<Record> tailer, long timestamp) throws InterruptedException {
        LogOffset lastOffset = null;
        for (LogRecord<Record> rec = tailer.read(FIRST_READ_TIMEOUT); rec != null; rec = tailer.read(READ_TIMEOUT)) {
            long recTimestamp = Watermark.ofValue(rec.message().getWatermark()).getTimestamp();
            if (recTimestamp == timestamp) {
                return rec.offset();
            } else if (recTimestamp > timestamp) {
                return lastOffset;
            }
            if (recTimestamp == 0) {
                throw new IllegalArgumentException("Cannot find position because Record has empty watermark: " + rec);
            }
            lastOffset = rec.offset();
        }
        // not found returns last offset of partition
        return lastOffset;
    }

}
