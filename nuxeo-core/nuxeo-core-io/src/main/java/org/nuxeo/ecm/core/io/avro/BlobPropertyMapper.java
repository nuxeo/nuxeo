/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     pierre
 */
package org.nuxeo.ecm.core.io.avro;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.avro.AvroMapper;
import org.nuxeo.runtime.avro.AvroService;

/**
 * @since 10.2
 */
public class BlobPropertyMapper extends AvroMapper<BlobProperty, Object> {

    public BlobPropertyMapper(AvroService service) {
        super(service);
    }

    @Override
    public Object fromAvro(Schema schema, Object input) {
        switch (schema.getType()) {
        case NULL:
            if (input == null) {
                return null;
            }
            throw new NonNullValueException();
        case UNION:
            for (Schema sub : schema.getTypes()) {
                try {
                    return service.fromAvro(sub, BlobProperty.class, input);
                } catch (NonNullValueException e) {
                    // this exception is thrown when a null value is expected and not found
                    // this happens for schema unions [null, schema]
                }
            }
            throw new RuntimeServiceException(CANNOT_MAP_FROM + schema.getType());
        case RECORD:
            try {
                GenericRecord record = (GenericRecord) input;
                String mimeType = (String) record.get(service.encodeName("mime-type"));
                String encoding = (String) record.get("encoding");
                byte[] bytes = ((ByteBuffer) record.get("data")).array();
                Blob b = Blobs.createBlob(bytes, mimeType, encoding);
                b.setFilename((String) record.get("name"));
                b.setDigest((String) record.get("digest"));
                return b;
            } catch (IOException e) {
                throw new RuntimeServiceException(CANNOT_MAP_FROM + schema.getType(), e);
            }
        default:
            throw new RuntimeServiceException(CANNOT_MAP_FROM + schema.getType());
        }
    }

    @Override
    public Object toAvro(Schema schema, BlobProperty input) {
        switch (schema.getType()) {
        case NULL:
            if (input == null) {
                return null;
            }
            throw new NonNullValueException();
        case UNION:
            for (Schema s : schema.getTypes()) {
                try {
                    return service.toAvro(s, input);
                } catch (NonNullValueException e) {
                    // ignore
                }
            }
            throw new RuntimeServiceException(CANNOT_MAP_TO + schema.getType());
        case RECORD:
            GenericRecord record = new GenericData.Record(schema);
            for (Field f : schema.getFields()) {
                if ("data".equals(f.name())) {
                    Blob blob = (Blob) input.getValue();
                    try {
                        record.put(f.name(), ByteBuffer.wrap(blob.getByteArray()));
                    } catch (IOException e) {
                        throw new RuntimeServiceException(CANNOT_MAP_TO + schema.getType(), e);
                    }
                } else {
                    record.put(f.name(), service.toAvro(f.schema(), input.get(service.decodeName(f.name()))));
                }
            }
            return record;
        default:
            throw new RuntimeServiceException(CANNOT_MAP_TO + schema.getType());
        }
    }

}
