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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.avro.Schema;
import org.apache.avro.message.RawMessageDecoder;
import org.apache.avro.message.RawMessageEncoder;
import org.apache.avro.reflect.ReflectData;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.lib.stream.StreamRuntimeException;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

/**
 * Use the Confluent Avro encoding which differs from Avro message, the schema store is a REST Confluent Schema
 * Registry.
 *
 * @since 10.3
 */
public class AvroConfluentCodec<T> implements Codec<T> {
    private static final Log log = LogFactory.getLog(AvroConfluentCodec.class);

    public static final String NAME = "avroConfluent";

    protected static final byte MAGIC_BYTE = 0x0;

    protected static final int ID_SIZE = 4;

    protected static final int DEFAULT_IDENTITY_MAP_CAPACITY = 10;

    protected final Class<T> messageClass;

    protected final Schema schema;

    protected final int schemaId;

    protected final String schemaName;

    protected final KafkaAvroSerializer serializer;

    protected final RawMessageEncoder<T> encoder;

    protected final SchemaRegistryClient client;

    /**
     * Create an AvroConfluent codec
     *
     * @param messageClass the class to encode and decode
     * @param schemaRegistryUrls a comma separated list of Confluent Schema Registry URL
     */
    public AvroConfluentCodec(Class<T> messageClass, String schemaRegistryUrls) {
        this.messageClass = messageClass;
        schema = ReflectData.get().getSchema(messageClass);
        schemaName = messageClass.getName();
        if (schemaRegistryUrls.contains(",")) {
            client = new CachedSchemaRegistryClient(Arrays.asList(schemaRegistryUrls.split(",")),
                    DEFAULT_IDENTITY_MAP_CAPACITY);
        } else {
            client = new CachedSchemaRegistryClient(schemaRegistryUrls, DEFAULT_IDENTITY_MAP_CAPACITY);
        }
        try {
            this.schemaId = client.register(messageClass.getName(), schema);
        } catch (RestClientException | IOException e) {
            throw new StreamRuntimeException(e);
        }
        this.serializer = new KafkaAvroSerializer(client);
        this.encoder = new RawMessageEncoder<>(ReflectData.get(), schema);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] encode(T object) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(MAGIC_BYTE);
        try {
            out.write(ByteBuffer.allocate(ID_SIZE).putInt(schemaId).array());
            out.write(encoder.encode(object).array());
        } catch (IOException e) {
            throw new StreamRuntimeException(e);
        }
        return out.toByteArray();
    }

    @Override
    public T decode(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        if (buffer.get() != MAGIC_BYTE) {
            throw new IllegalArgumentException("Invalid Avro Confluent message, expecting magic byte");
        }
        int id = buffer.getInt();
        Schema writeSchema;
        try {
            writeSchema = client.getById(id);
        } catch (IOException e) {
            throw new StreamRuntimeException("Cannot retrieve write schema id: " + id + " on " + messageClass, e);
        } catch (RestClientException e) {
            if (e.getStatus() != 404) {
                throw new StreamRuntimeException("Cannot retrieve write schema id: " + id + " on " + messageClass, e);
            }
            // the write schema is not found, we fallback to read schema
            // this enable to read message that have the same read schema even if we loose the schema registry
            if (log.isWarnEnabled()) {
                log.warn(String.format("Cannot retrieve write schema %d, fallback to read schema: %d for %s", id,
                        schemaId, messageClass));
            }
            writeSchema = schema;
        }
        RawMessageDecoder<T> decoder = new RawMessageDecoder<>(ReflectData.get(), writeSchema, schema);
        try {
            return decoder.decode(buffer.slice(), null);
        } catch (IOException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
