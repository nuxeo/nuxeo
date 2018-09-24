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

import java.nio.file.Paths;
import java.time.Duration;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.tools.renderer.Renderer;

import static org.nuxeo.lib.stream.tools.command.CatCommand.NUXEO_SCHEMA_STORE;

/**
 * Output the last records on a Log.
 *
 * @since 9.3
 */
public class TailCommand extends Command {
    private static final Log log = LogFactory.getLog(TailCommand.class);

    protected static final String NAME = "tail";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void updateOptions(Options options) {
        options.addOption(Option.builder("n")
                                .longOpt("lines")
                                .desc("output the last NUM records")
                                .hasArg()
                                .argName("NUM")
                                .build());
        options.addOption("f", "follow", false, "output appended records");
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
                                .longOpt("codec")
                                .desc("Codec used to read record, can be: java, avro, avroBinary, avroJson")
                                .hasArg()
                                .argName("CODEC")
                                .build());
        options.addOption(
                Option.builder().longOpt("render").desc("Output rendering").hasArg().argName("FORMAT").build());
        options.addOption(Option.builder("t")
                                .longOpt("timeout")
                                .desc("Timeout on follow in second")
                                .hasArg()
                                .argName("TIMEOUT")
                                .build());
        options.addOption(Option.builder()
                .longOpt("schema-store")
                .desc("Set path of a FileAvroSchemaStore to load Avro schemas")
                .hasArg()
                .argName("SCHEMA_STORE_PATH")
                .build());
    }

    @Override
    public boolean run(LogManager manager, CommandLine cmd) throws InterruptedException {
        int lines = Integer.parseInt(cmd.getOptionValue("lines", "10"));
        String name = cmd.getOptionValue("log-name");
        String render = cmd.getOptionValue("render", "default");
        String group = cmd.getOptionValue("group", "tools");
        String codec = cmd.getOptionValue("codec");
        String avroSchemaStorePath  = cmd.getOptionValue("schema-store");
        if (avroSchemaStorePath == null && Paths.get(NUXEO_SCHEMA_STORE).toFile().exists()) {
            avroSchemaStorePath = NUXEO_SCHEMA_STORE;
        }
        int timeout = Integer.parseInt(cmd.getOptionValue("timeout", "120"));
        tail(manager, name, group, lines, getRecordRenderer(render, avroSchemaStorePath), codec);
        if (cmd.hasOption("follow")) {
            follow(manager, name, group, getRecordRenderer(render, avroSchemaStorePath), timeout, codec);
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    protected void tail(LogManager manager, String name, String group, int lines, Renderer render, String codec)
            throws InterruptedException {
        LogRecord<Record>[] records = new LogRecord[lines];
        render.header();
        int count = 0;
        try (LogTailer<Record> tailer = manager.createTailer(group, name, getRecordCodec(codec))) {
            LogRecord<Record> record;
            do {
                record = tailer.read(Duration.ofMillis(500));
                if (record != null) {
                    records[count++ % lines] = record;
                }
            } while (record != null);
        }
        for (int i = count; i < lines + count; i++) {
            LogRecord<Record> record = records[i % lines];
            if (record != null) {
                render.accept(record);
            }
        }
        render.footer();
    }

    protected void follow(LogManager manager, String name, String group, Renderer render, int timeout, String codec)
            throws InterruptedException {
        try (LogTailer<Record> tailer = manager.createTailer(group, name, getRecordCodec(codec))) {
            tailer.toEnd();
            while (true) {
                LogRecord<Record> record = tailer.read(Duration.ofSeconds(timeout));
                if (record == null) {
                    log.error("tail timeout");
                    break;
                }
                render.accept(record);
            }
        }
    }
}
