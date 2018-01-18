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

import java.time.DateTimeException;
import java.time.Instant;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogLag;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogOffset;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogTailer;

/**
 * Manipulates the consumer position to the beginning, end or a specific timestamp
 * @since 10.1
 */
public class PositionCommand extends Command {

    protected static final String NAME = "position";

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
        options.addOption(Option.builder()
                .longOpt("reset")
                .desc("Resets all committed positions for the group")
                .build());
        options.addOption(Option.builder()
                .longOpt("to-end")
                .desc("Sets the committed positions to the end of partitions for the group")
                .build());
        options.addOption(Option.builder()
                .longOpt("to-timestamp")
                .desc("Sets the committed positions for the group to a specific timestamp."
                + " The record timestamp used depends on the implementation, for Kafka this is the LogAppendTime. "
                + " The timestamp is specified in ISO-8601 format, eg. " + Instant.now()
                + ". If no record offset is found with an appropriate timestamp then the command fails.")
                .hasArg()
                .argName("TIMESTAMP")
                .build());
    }

    @Override
    public boolean run(LogManager manager, CommandLine cmd) throws InterruptedException {
        String name = cmd.getOptionValue("log-name");
        String group = cmd.getOptionValue("group", "tools");

        if (cmd.hasOption("to-timestamp")) {
            long timestamp = parseTimestamp(cmd.getOptionValue("to-timestamp"));
            if (timestamp < 0) {
                return false;
            }
            return positionToTimestamp(manager, group, name, timestamp);
        } else if (cmd.hasOption("to-end")) {
            toEnd(manager,group,name);
        } else if (cmd.hasOption("reset")) {
            reset(manager,group,name);
        } else {
            System.err.println("Invalid option, try 'help position'");
            return false;
        }

        return true;
    }

    protected void toEnd(LogManager manager, String group, String name) {
        LogLag lag = manager.getLag(name, group);

        try (LogTailer<Record> tailer = manager.createTailer(group, name)) {
            tailer.toEnd();
            tailer.commit();
        }
        System.out.println(String.format("# Moved log %s, group: %s, from: %s to %s", name, group, lag.lower(), lag.upper()));
    }

    protected void reset(LogManager manager, String group, String name) {
        LogLag lag = manager.getLag(name, group);
        long pos = lag.lower();
        try (LogTailer<Record> tailer = manager.createTailer(group, name)) {
            tailer.reset();
        }
        System.out.println(String.format("# Reset log %s, group: %s, from: %s to 0", name, group, pos));
    }

    protected boolean positionToTimestamp(LogManager manager, String group, String name, long timestamp) {

        try (LogTailer<Record> tailer = manager.createTailer(group, name)) {
            int size = manager.getAppender(name).size();
            boolean movedOffset = false;
            for (int partition = 0; partition < size; partition++) {
                LogPartition logPartition = new LogPartition(name, partition);
                LogOffset logOffset = tailer.offsetForTimestamp(logPartition, timestamp);
                if (logOffset != null) {
                    tailer.seek(logOffset);
                    movedOffset = true;
                    System.out.println(
                            String.format("# Set log %s, group: %s, to offset %s", name, group, logOffset.offset()));
                } else {
                    System.err.println(
                            String.format("# Could not find an offset for group: %s, partition: %s", group, logPartition));
                }
            }
            if (movedOffset) {
                tailer.commit();
                return true;
            }
        }

        return false;
    }

    protected long parseTimestamp(String timestamp) {
        try {
            Instant instant = Instant.parse(timestamp);
            return instant.toEpochMilli();
        } catch (DateTimeException e) {
            System.err.println("Failed to read the timeout: " + e.getMessage());
            System.err.println("The timestamp should be in ISO-8601 format, eg. " + Instant.now());
        }
        return -1;
    }

}
