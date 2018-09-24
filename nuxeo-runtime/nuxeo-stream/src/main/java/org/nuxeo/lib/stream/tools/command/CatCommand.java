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
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.tools.renderer.Renderer;

/**
 * Output records to stdout.
 *
 * @since 9.3
 */
public class CatCommand extends Command {

    protected static final String NUXEO_SCHEMA_STORE = "nxserver/data/avro";

    protected static final String NAME = "cat";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void updateOptions(Options options) {
        options.addOption(
                Option.builder("n").longOpt("lines").desc("Render the first N records").hasArg().argName("N").build());
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
        options.addOption(Option.builder()
                .longOpt("schema-store")
                .desc("Set path of a FileAvroSchemaStore to load Avro schemas")
                .hasArg()
                .argName("SCHEMA_STORE_PATH")
                .build());
    }

    @Override
    public boolean run(LogManager manager, CommandLine cmd) throws InterruptedException {
        int limit = Integer.parseInt(cmd.getOptionValue("lines", "-1"));
        String name = cmd.getOptionValue("log-name");
        String render = cmd.getOptionValue("render", "default");
        String group = cmd.getOptionValue("group", "tools");
        String codec = cmd.getOptionValue("codec");
        String avroSchemaStorePath  = cmd.getOptionValue("schema-store");
        if (avroSchemaStorePath == null && Paths.get(NUXEO_SCHEMA_STORE).toFile().exists()) {
            avroSchemaStorePath = NUXEO_SCHEMA_STORE;
        }
        cat(manager, name, group, limit, getRecordRenderer(render, avroSchemaStorePath), codec);
        return true;
    }

    protected void cat(LogManager manager, String name, String group, int limit, Renderer render, String codec)
            throws InterruptedException {
        render.header();
        try (LogTailer<Record> tailer = manager.createTailer(group, name, getRecordCodec(codec))) {
            int count = 0;
            do {
                LogRecord<Record> record = tailer.read(Duration.ofMillis(1000));
                if (record == null) {
                    break;
                }
                count++;
                render.accept(record);
            } while (limit < 0 || (count < limit));
        }
        render.footer();
    }
}
