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

import java.time.Duration;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;

/**
 * Copy a Log to another
 *
 * @since 9.3
 */
public class CopyCommand extends Command {
    private static final Log log = LogFactory.getLog(CopyCommand.class);

    protected static final String NAME = "copy";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void updateOptions(Options options) {
        options.addOption(Option.builder()
                                .longOpt("src")
                                .desc("Source log name")
                                .required()
                                .hasArg()
                                .argName("LOG_NAME")
                                .build());
        options.addOption(Option.builder()
                                .longOpt("srcCodec")
                                .desc("Codec used to read record, can be: java, avro, avroBinary, avroJson")
                                .hasArg()
                                .argName("CODEC")
                                .build());
        options.addOption(Option.builder()
                                .longOpt("dest")
                                .desc("Target log name")
                                .required()
                                .hasArg()
                                .argName("LOG_NAME")
                                .build());
        options.addOption(Option.builder()
                                .longOpt("destCodec")
                                .desc("Codec used to write record, can be: java, avro, avroBinary, avroJson")
                                .hasArg()
                                .argName("CODEC")
                                .build());
        options.addOption(Option.builder("g")
                                .longOpt("group")
                                .desc("Source consumer group to use")
                                .hasArg()
                                .argName("GROUP")
                                .build());
    }

    @Override
    public boolean run(LogManager manager, CommandLine cmd) {
        Name src = Name.ofUrn(cmd.getOptionValue("src"));
        Name dest = Name.ofUrn(cmd.getOptionValue("dest"));
        Name group = Name.ofUrn(cmd.getOptionValue("group", "admin/tools"));
        String srcCodec = cmd.getOptionValue("srcCodec");
        String destCodec = cmd.getOptionValue("destCodec");
        return copy(manager, src, srcCodec, dest, destCodec, group);
    }

    protected boolean copy(LogManager manager, Name src, String srcCodec, Name dest, String destCodec, Name group) {
        log.info(String.format("# Copy %s to %s", src, dest));
        if (!manager.exists(src)) {
            log.error("source log not found: " + src);
            return false;
        }
        if (manager.exists(dest)) {
            log.error("destination log already exists: " + dest);
            return false;
        }
        manager.createIfNotExists(dest, manager.size(src));
        LogAppender<Record> appender = manager.getAppender(dest, getRecordCodec(destCodec));
        try (LogTailer<Record> tailer = manager.createTailer(group, src, getRecordCodec(srcCodec))) {
            while (true) {
                LogRecord<Record> record = tailer.read(Duration.ofSeconds(5));
                if (record == null) {
                    break;
                }
                appender.append(record.message().getKey(), record.message());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted");
            return false;
        }
        return true;
    }

}
