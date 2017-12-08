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
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;

/**
 * @since 9.3
 */
public class CopyCommand extends Command {

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
                                .longOpt("dest")
                                .desc("Target log name")
                                .required()
                                .hasArg()
                                .argName("LOG_NAME")
                                .build());
        options.addOption(Option.builder("g")
                                .longOpt("group")
                                .desc("Source consumer group to use")
                                .hasArg()
                                .argName("GROUP")
                                .build());
    }

    @Override
    public boolean run(LogManager manager, CommandLine cmd) throws InterruptedException {
        String src = cmd.getOptionValue("src");
        String dest = cmd.getOptionValue("dest");
        String group = cmd.getOptionValue("group", "tools");
        return copy(manager, src, dest, group);
    }

    protected boolean copy(LogManager manager, String src, String dest, String group) {
        System.out.println(String.format("# Copy %s to %s", src, dest));
        if (!manager.exists(src)) {
            System.err.println("source log not found: " + src);
            return false;
        }
        if (manager.exists(dest)) {
            System.err.println("destination log already exists: " + dest);
            return false;
        }
        manager.createIfNotExists(dest, manager.getAppender(src).size());
        LogAppender<Record> appender = manager.getAppender(dest);
        try (LogTailer<Record> tailer = manager.createTailer(group, src)) {
            while (true) {
                LogRecord<Record> record = tailer.read(Duration.ofSeconds(5));
                if (record == null) {
                    break;
                }
                appender.append(record.message().key, record.message());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Interrupted");
            return false;
        }
        return true;
    }

}
