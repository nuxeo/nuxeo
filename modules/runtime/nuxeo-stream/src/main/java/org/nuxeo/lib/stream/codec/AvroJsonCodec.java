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
package org.nuxeo.lib.stream.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.reflect.ReflectData;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.avro.reflect.ReflectDatumWriter;

/**
 * JSON Avro format for debugging purpose.
 *
 * @since 10.2
 */
public class AvroJsonCodec<T> implements Codec<T> {
    public static final String NAME = "avroJson";

    protected final Class<T> messageClass;

    protected final Schema schema;

    protected final ReflectDatumWriter<T> writer;

    protected final ReflectDatumReader<T> reader;

    public AvroJsonCodec(Class<T> messageClass) {
        this.messageClass = messageClass;
        schema = ReflectData.get().getSchema(messageClass);
        writer = new ReflectDatumWriter<>(schema);
        reader = new ReflectDatumReader<>(schema);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] encode(T object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(schema, baos);
            writer.write(object, jsonEncoder);
            jsonEncoder.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public T decode(byte[] data) {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            JsonDecoder jsonDecoder = DecoderFactory.get().jsonDecoder(schema, bais);
            return reader.read(null, jsonDecoder);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
