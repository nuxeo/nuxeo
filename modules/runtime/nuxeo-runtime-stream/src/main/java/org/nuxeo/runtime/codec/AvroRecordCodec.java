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
package org.nuxeo.runtime.codec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.message.RawMessageDecoder;
import org.apache.avro.message.RawMessageEncoder;
import org.apache.avro.reflect.ReflectData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.lib.stream.StreamRuntimeException;
import org.nuxeo.lib.stream.codec.AvroConfluentCodec;
import org.nuxeo.lib.stream.codec.Codec;
import org.nuxeo.lib.stream.computation.Record;
import org.nuxeo.lib.stream.computation.Watermark;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.serializers.KafkaAvroSerializer;

/**
 * Instead of having an avro Record envelop that contains a data encoded in Avro, this structure is a flat Avro message
 * joining schemas of the Record and data.
 *
 * This encoding can then be read by any Confluent Avro reader.
 *
 * @since 11.4
 */
public class AvroRecordCodec<T extends Record> implements Codec<T> {
    private static final Logger log = LogManager.getLogger(AvroRecordCodec.class);

    public static final String NAME = "avroRecord";

    public static final String RECORD_KEY = "recordKey";

    public static final String RECORD_WATERMARK = "recordWatermark";

    public static final String RECORD_TIMESTAMP = "recordTimestamp";

    public static final String RECORD_FLAG = "recordFlag";

    protected final Schema schema;

    protected final int schemaId;

    protected final Schema messageSchema;

    protected final int messageSchemaId;

    protected final RawMessageDecoder<GenericRecord> messageDecoder;

    protected final RawMessageEncoder<GenericRecord> messageEncoder;

    protected final KafkaAvroSerializer serializer;

    protected final RawMessageEncoder<GenericRecord> encoder;

    protected final SchemaRegistryClient client;

    public AvroRecordCodec(Schema messageSchema, String schemaRegistryUrls) {
        this.messageSchema = messageSchema;
        this.client = AvroConfluentCodec.getRegistryClient(schemaRegistryUrls);
        this.serializer = new KafkaAvroSerializer(client);
        // extends the schema to support record fields
        this.schema = addRecordFieldsToSchema(messageSchema);
        log.trace("msg schema: {}", () -> this.messageSchema.toString(true));
        log.trace("rec + msg schema: {}", () -> this.schema.toString(true));
        // register schemas
        try {
            this.messageSchemaId = client.register(messageSchema.getName(), messageSchema);
            this.schemaId = client.register(schema.getName(), schema);
        } catch (RestClientException | IOException e) {
            throw new StreamRuntimeException(e);
        }
        // create encoder and decoder
        this.encoder = new RawMessageEncoder<>(GenericData.get(), schema);
        this.messageDecoder = new RawMessageDecoder<>(GenericData.get(), messageSchema);
        this.messageEncoder = new RawMessageEncoder<>(GenericData.get(), messageSchema);
    }

    public AvroRecordCodec(String messageClassName, String schemaRegistryUrls) throws ClassNotFoundException {
        this(ReflectData.get().getSchema(Class.forName(messageClassName)), schemaRegistryUrls);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] encode(T record) {
        try {
            // decode the message as generic record
            GenericRecord message = messageDecoder.decode(record.getData(), null);
            // Create a new generic record that contains both record and message fields
            GenericRecord newRecord = createRecordFromMessage(message);
            // populate record fields
            newRecord.put(RECORD_KEY, record.getKey());
            newRecord.put(RECORD_WATERMARK, record.getWatermark());
            newRecord.put(RECORD_TIMESTAMP, Watermark.ofValue(record.getWatermark()).getTimestamp());
            newRecord.put(RECORD_FLAG, Byte.valueOf(record.getFlagsAsByte()).intValue());
            // encode
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(AvroConfluentCodec.MAGIC_BYTE);
            try {
                out.write(ByteBuffer.allocate(AvroConfluentCodec.ID_SIZE).putInt(schemaId).array());
                out.write(encoder.encode(newRecord).array());
            } catch (IOException e) {
                throw new StreamRuntimeException(e);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    protected GenericRecord createRecordFromMessage(GenericRecord message) {
        GenericData.Record ret = new GenericData.Record(schema);
        for (Schema.Field field : message.getSchema().getFields()) {
            Object value = message.get(field.pos());
            ret.put(field.name(), value);
        }
        return ret;
    }

    protected Schema addRecordFieldsToSchema(Schema schema) {
        List<Schema.Field> fields = new ArrayList<>();
        for (Schema.Field field : schema.getFields()) {
            fields.add(new Schema.Field(field.name(), field.schema(), field.doc(), field.defaultVal()));
        }
        fields.add(new Schema.Field(RECORD_KEY, SchemaBuilder.builder().stringType(), "record key", null));
        fields.add(new Schema.Field(RECORD_WATERMARK, SchemaBuilder.builder().longType(), "record watermark", 0L));
        fields.add(new Schema.Field(RECORD_TIMESTAMP, SchemaBuilder.builder().longType(), "record timestamp", 0L));
        fields.add(new Schema.Field(RECORD_FLAG, SchemaBuilder.builder().intType(), "record flags", 0));
        return Schema.createRecord(schema.getName() + "Record", schema.getDoc(), schema.getNamespace(), false, fields);
    }

    @Override
    public T decode(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        if (buffer.get() != AvroConfluentCodec.MAGIC_BYTE) {
            throw new IllegalArgumentException("Invalid Avro Confluent message, expecting magic byte");
        }
        int id = buffer.getInt();
        Schema writeSchema;
        try {
            writeSchema = client.getById(id);
        } catch (IOException | RestClientException e) {
            throw new StreamRuntimeException("Cannot retrieve write schema id: " + id, e);
        }
        RawMessageDecoder<GenericRecord> decoder = new RawMessageDecoder<>(GenericData.get(), writeSchema, schema);
        try {
            GenericRecord rec = decoder.decode(buffer.slice(), null);
            log.trace("GR: {}", rec);
            String key = rec.get(RECORD_KEY).toString();
            long wm = (Long) rec.get(RECORD_WATERMARK);
            int flag = (Integer) rec.get(RECORD_FLAG);
            byte[] msgData = messageEncoder.encode(rec).array();
            Record ret = new Record(key, msgData, wm);
            ret.setFlags((byte) flag);
            return (T) ret;
        } catch (IOException | IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
