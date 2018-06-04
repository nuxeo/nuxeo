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
package org.nuxeo.lib.stream.tests.codec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;

import org.apache.avro.message.MissingSchemaException;
import org.apache.avro.message.SchemaStore;
import org.apache.avro.reflect.ReflectData;
import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.lib.stream.codec.AvroBinaryCodec;
import org.nuxeo.lib.stream.codec.AvroJsonCodec;
import org.nuxeo.lib.stream.codec.AvroMessageCodec;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.codec.SerializableCodec;
import org.nuxeo.lib.stream.computation.Record;

/**
 * @since 10.2
 */
public class TestCodec {

    protected static final int MAX_DATA_SIZE = 1000;

    @Test
    public void testAvroEvolution() {
        Codec<MessageV1> codec1 = new AvroMessageCodec<>(MessageV1.class);
        Codec<MessageV2> codec2 = new AvroMessageCodec<>(MessageV2.class);
        Codec<MessageV3> codec3 = new AvroMessageCodec<>(MessageV3.class);

        MessageV1 src1 = new MessageV1();
        MessageV2 src2 = new MessageV2();
        MessageV3 src3 = new MessageV3();

        // basic tests
        assertEquals("v1", src1.stringValue);
        assertEquals("new", src2.newString);
        assertEquals("v3", src3.myString);

        testCodec(src1, codec1);
        testCodec(src2, codec2);
        testCodec(src3, codec3);

        // convert MessageV1 to MessageV2 without schema store -> fail
        byte[] data1 = codec1.encode(src1);
        try {
            codec2.decode(data1);
            fail("There is no schema store on codec2 so it can not decode a MessageV1");
        } catch (MissingSchemaException e) {
            // expected
        }

        // project MessageV1 to MessageV2 with a schema store
        SchemaStore.Cache store = new SchemaStore.Cache();
        store.addSchema(ReflectData.get().getSchema(MessageV1.class));
        store.addSchema(ReflectData.get().getSchema(MessageV2.class));
        Codec<MessageV2> codec2Store = new AvroMessageCodec<>(MessageV2.class, store);

        MessageV2 dest2 = codec2Store.decode(data1);
        assertEquals(src1.intValue, dest2.intValue);
        assertEquals(src1.stringValue, dest2.stringValue);
        assertEquals("unknown", dest2.newString);
        assertNull(dest2.anotherNewString);

        // project MessageV1 to MessageV3
        store.addSchema(ReflectData.get().getSchema(MessageV3.class));
        Codec<MessageV3> codec3Store = new AvroMessageCodec<>(MessageV3.class, store);
        MessageV3 dest3 = codec3Store.decode(data1);
        assertEquals(src1.stringValue, dest3.myString);
        assertEquals(src1.intValue, dest3.myInt);
    }

    @Test
    public void testRecordExternalizable() throws Exception {
        Record src = getRecord();
        Codec<Record> codec = new SerializableCodec<>();
        testCodec(src, codec);
        testCodecFromFile("data/record-externalizable.bin", codec);
    }

    @Test
    public void testRecordMessageAvro() throws Exception {
        Record src = getRecord();
        Codec<Record> codec = new AvroMessageCodec<>(Record.class);
        testCodec(src, codec);
        testCodecFromFile("data/record-avro-message.bin", codec);
    }

    @Test
    public void testRecordRawMessageAvro() throws Exception {
        Record src = getRecord();
        Codec<Record> codec = new AvroBinaryCodec<>(Record.class);
        testCodec(src, codec);
        testCodecFromFile("data/record-avro-binary.bin", codec);
    }

    @Test
    public void testRecordJsonAvro() throws Exception {
        Record src = getRecord();
        Codec<Record> codec = new AvroJsonCodec<>(Record.class);
        testCodec(src, codec);
        testCodecFromFile("data/record-avro.json", codec);
    }

    @Test
    public void testInvalidEncoding() throws Exception {
        byte[] data = readFile("data/record-externalizable.bin");
        Codec<Record> codec = new AvroMessageCodec<>(Record.class);
        try {
            codec.decode(data);
            fail("failure expected on decode");
        } catch (IllegalArgumentException e) {
            // expected
        }

        codec = new AvroBinaryCodec<>(Record.class);
        try {
            codec.decode(data);
            fail("failure expected on decode");
        } catch (IllegalArgumentException e) {
            // expected
        }

        codec = new AvroJsonCodec<>(Record.class);
        try {
            codec.decode(data);
            fail("failure expected on decode");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    protected void testCodecFromFile(String path, Codec<Record> codec) throws IOException {
        byte[] data = readFile(path);
        Record record = codec.decode(data);
        testCodec(record, codec);

    }

    protected byte[] readFile(String path) throws IOException {
        byte[] ret = new byte[MAX_DATA_SIZE];
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertTrue(stream.read(ret) > 0);
            assertEquals(-1, stream.read());
        }
        return ret;
    }

    protected <T> void testCodec(T src, Codec<T> codec) {
        byte[] data = codec.encode(src);
        T dest = codec.decode(data);
        assertEquals(src, dest);
        byte[] data2 = codec.encode(dest);
        T dest2 = codec.decode(data2);
        assertEquals(src, dest2);
        assertEquals(String.format("%s\n%s", overview(data), overview(data2)), data.length, data2.length);
        // System.out.println(String.format("Codec: %s, size: %d", codec.getClass().getSimpleName(), data.length));
    }

    protected Record getRecord() throws UnsupportedEncodingException {
        Record src = Record.of("key", "value".getBytes("UTF-8"));
        src.setFlags(EnumSet.of(Record.Flag.COMMIT, Record.Flag.TRACE));
        return src;
    }

    protected String overview(byte[] data) {
        String overview;
        try {
            overview = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            overview = "unsupported encoding";
        }
        return overview;
    }

    @Ignore("Only to generate data file")
    @Test
    public void testWriteRecord() throws Exception {
        Record src = getRecord();

        Codec<Record> codec = new SerializableCodec<>();
        byte[] data = codec.encode(src);
        Path path = Paths.get("/tmp/record-externalizable.bin");
        Files.write(path, data);

        codec = new AvroMessageCodec<>(Record.class);
        data = codec.encode(src);
        path = Paths.get("/tmp/record-avro-message.bin");
        Files.write(path, data);

        codec = new AvroBinaryCodec<>(Record.class);
        data = codec.encode(src);
        path = Paths.get("/tmp/record-avro-binary.bin");
        Files.write(path, data);

        codec = new AvroJsonCodec<>(Record.class);
        data = codec.encode(src);
        path = Paths.get("/tmp/record-avro.json");
        Files.write(path, data);
    }
}
