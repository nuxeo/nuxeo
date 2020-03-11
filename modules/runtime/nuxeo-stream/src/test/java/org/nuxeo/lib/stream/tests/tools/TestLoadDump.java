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
package org.nuxeo.lib.stream.tests.tools;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.junit.Test;
import org.nuxeo.lib.stream.StreamRuntimeException;
import org.nuxeo.lib.stream.computation.Record;

/**
 * Unit test to show how to read a dump file created with the stream.sh dump command. The dump is an Avro file of
 * Record.
 *
 * @since 10.2
 */
public class TestLoadDump {

    private static final String DUMP_FILE = "data/dump.avro";

    private static final String OUTPUT_FILE = "/tmp/out.avro";

    protected DataFileWriter<Record> dataFileWriter;

    protected boolean writeToOuput = false;

    public Path getDumpPath() {
        URL url = getClass().getClassLoader().getResource(DUMP_FILE);
        try {
            return new File(url.toURI()).toPath();
        } catch (URISyntaxException e) {
            return new File(url.getPath()).toPath();
        }
    }

    @Test
    public void testReadDump() throws IOException {
        Path dumpPath = getDumpPath();
        Schema schema = ReflectData.get().getSchema(Record.class);
        DatumReader<Record> datumReader = new ReflectDatumReader<>(schema);
        int count = 0;
        if (writeToOuput) {
            openWriter(OUTPUT_FILE, schema);
        }
        try (DataFileReader<Record> dataFileReader = new DataFileReader<>(dumpPath.toFile(), datumReader)) {
            while (dataFileReader.hasNext()) {
                Record record = dataFileReader.next();
                doSomething(count++, record);
            }
        } catch (IOException e) {
            throw new StreamRuntimeException(e);
        } finally {
            closeWriter();
        }
    }

    protected void openWriter(String outputFile, Schema schema) throws IOException {
        DatumWriter<Record> datumWriter = new ReflectDatumWriter<>(schema);
        dataFileWriter = new DataFileWriter<>(datumWriter);
        dataFileWriter.setCodec(CodecFactory.snappyCodec());
        dataFileWriter.create(schema, new File(outputFile));
    }

    protected void closeWriter() throws IOException {
        if (dataFileWriter != null) {
            dataFileWriter.close();
            dataFileWriter = null;
        }
    }

    protected void doSomething(int i, Record record) throws IOException {
        // System.out.println("in: " + record);
        assertEquals("0", record.getKey());
        assertTrue(record.getData().length > 0);
        if (!writeToOuput) {
            return;
        }
        String message = new String(record.getData(), UTF_8);
        message = message.replace("logEntry", "XXX");
        Record changed = new Record(record.getKey(), message.getBytes(UTF_8), record.getWatermark(),
                record.getFlags());
        // System.out.println("out: " + changed);
        dataFileWriter.append(changed);
    }

}
