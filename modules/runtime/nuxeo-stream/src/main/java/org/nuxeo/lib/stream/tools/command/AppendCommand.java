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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.io.DatumReader;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.StreamRuntimeException;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogAppender;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.Name;

/**
 * Appends records from a dump file into an a Log partition.
 *
 * @since 10.2
 */
public class AppendCommand extends Command {
    private static final Log log = LogFactory.getLog(AppendCommand.class);

    protected static final String NAME = "append";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void updateOptions(Options options) {
        options.addOption(Option.builder()
                                .longOpt("input")
                                .desc("Avro file to append to a stream")
                                .hasArg()
                                .required()
                                .argName("INPUT")
                                .build());
        options.addOption(Option.builder("l")
                                .longOpt("log-name")
                                .desc("Log name")
                                .required()
                                .hasArg()
                                .argName("LOG_NAME")
                                .build());
        options.addOption(Option.builder("p")
                                .longOpt("partition")
                                .desc("Write to this partition")
                                .required()
                                .hasArg()
                                .argName("PARTITION")
                                .build());
        options.addOption(Option.builder()
                                .longOpt("codec")
                                .desc("Codec used to write the records, can be: java, avro, avroBinary, avroJson")
                                .required()
                                .hasArg()
                                .argName("CODEC")
                                .build());
    }

    @Override
    public boolean run(LogManager manager, CommandLine cmd) {
        Name name = Name.ofUrn(cmd.getOptionValue("log-name"));
        String codec = cmd.getOptionValue("codec");
        int partition = Integer.parseInt(cmd.getOptionValue("partition"));
        String input = cmd.getOptionValue("input");
        append(manager, name, partition, codec, Paths.get(input));
        return true;
    }

    protected void append(LogManager manager, Name name, int partition, String codec, Path input) {
        log.info(String.format("Append records from %s to stream: %s, partition: %d", input, name, partition));
        Schema schema = ReflectData.get().getSchema(Record.class);
        DatumReader<Record> datumReader = new ReflectDatumReader<>(schema);
        LogAppender<Record> appender = manager.getAppender(name, getRecordCodec(codec));
        int count = 0;
        try (DataFileReader<Record> dataFileReader = new DataFileReader<>(input.toFile(), datumReader)) {
            while (dataFileReader.hasNext()) {
                Record record = dataFileReader.next();
                appender.append(partition, record);
                count++;
            }
        } catch (IOException e) {
            throw new StreamRuntimeException(e);
        }
        log.info(String.format("%d record(s) appended", count));
    }

}
