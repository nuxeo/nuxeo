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
import java.time.Duration;

import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.StreamRuntimeException;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.LogPartition;
import org.nuxeo.lib.stream.log.LogRecord;
import org.nuxeo.lib.stream.log.LogTailer;
import org.nuxeo.lib.stream.log.Name;

/**
 * Dump records from a Log into an Avro file.
 *
 * @since 10.2
 */
public class DumpCommand extends Command {
    private static final Log log = LogFactory.getLog(DumpCommand.class);

    protected static final String NAME = "dump";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void updateOptions(Options options) {
        options.addOption(Option.builder("n")
                                .longOpt("count")
                                .desc("Dump the first N records into a file")
                                .hasArg()
                                .argName("N")
                                .build());
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
        options.addOption(Option.builder("p")
                                .longOpt("partition")
                                .desc("Read only this partition")
                                .hasArg()
                                .argName("PARTITION")
                                .build());
        options.addOption(Option.builder()
                                .longOpt("output")
                                .desc("Avro file path to dump the records")
                                .hasArg()
                                .required()
                                .argName("OUTPUT")
                                .build());
    }

    @Override
    public boolean run(LogManager manager, CommandLine cmd) throws InterruptedException {
        int limit = Integer.parseInt(cmd.getOptionValue("count", "-1"));
        Name name = Name.ofUrn(cmd.getOptionValue("log-name"));
        Name group = Name.ofUrn(cmd.getOptionValue("group", "admin/tools"));
        String codec = cmd.getOptionValue("codec");
        int partition = Integer.parseInt(cmd.getOptionValue("partition", "-1"));
        String output = cmd.getOptionValue("output");
        dump(manager, name, partition, group, limit, codec, Paths.get(output));
        return true;
    }

    protected void dump(LogManager manager, Name name, int partition, Name group, int limit, String codec,
            Path output) throws InterruptedException {
        log.info("Dump record to file: " + output);
        Schema schema = ReflectData.get().getSchema(Record.class);
        DatumWriter<Record> datumWriter = new ReflectDatumWriter<>(schema);
        int count = 0;
        try (DataFileWriter<Record> dataFileWriter = new DataFileWriter<>(datumWriter)) {
            dataFileWriter.setCodec(CodecFactory.snappyCodec());
            dataFileWriter.create(schema, output.toFile());
            try (LogTailer<Record> tailer = getTailer(manager, name, partition, group, codec)) {
                do {
                    LogRecord<Record> record = tailer.read(Duration.ofMillis(1000));
                    if (record == null) {
                        break;
                    }
                    dataFileWriter.append(record.message());
                    count++;
                } while (limit < 0 || (count < limit));
            }
        } catch (IOException e) {
            throw new StreamRuntimeException(e);
        }
        log.info(String.format("%d record(s) dumped", count));
    }

    protected LogTailer<Record> getTailer(LogManager manager, Name name, int partition, Name group, String codec) {
        if (partition >= 0) {
            return manager.createTailer(group, new LogPartition(name, partition), getRecordCodec(codec));
        }
        return manager.createTailer(group, name, getRecordCodec(codec));
    }
}
