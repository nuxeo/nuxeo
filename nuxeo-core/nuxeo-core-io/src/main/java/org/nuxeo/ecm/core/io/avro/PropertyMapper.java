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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericData.Array;
import org.apache.avro.generic.GenericRecord;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.avro.AvroMapper;
import org.nuxeo.runtime.avro.AvroService;

/**
 * @since 10.2
 */
public class PropertyMapper extends AvroMapper<Property, Object> {

    protected static final String TIMESTAMP_MILLIS = "timestamp-millis";

    protected static final Map<String, Class<?>> MAPPING = Collections.singletonMap("content", BlobProperty.class);

    public PropertyMapper(AvroService service) {
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
                    return service.fromAvro(sub, Property.class, input);
                } catch (NonNullValueException e) {
                    // ignore
                }
            }
            throw new RuntimeServiceException(CANNOT_MAP_FROM + schema.getType());
        case RECORD:
            GenericRecord record = (GenericRecord) input;
            List<Field> fields = schema.getFields();
            Map<String, Object> data = new HashMap<>(fields.size());
            for (Field field : fields) {
                String propertyName = service.decodeName(field.name());
                Class<?> clazz = MAPPING.getOrDefault(propertyName, Property.class);
                Object value = service.fromAvro(field.schema(), clazz, record.get(field.name()));
                data.put(propertyName, value);
            }
            return data;
        case ARRAY:
            GenericData.Array<?> array = (Array<?>) input;
            List<Object> list = new ArrayList<>(array.size());
            for (Object element : array) {
                list.add(service.fromAvro(schema.getElementType(), Property.class, element));
            }
            return list;
        case LONG:
            if (TIMESTAMP_MILLIS.equals(getLogicalType(schema))) {
                return new Date(((Long) input).longValue());
            }
            return input;
        case INT:
        case FLOAT:
        case STRING:
        case DOUBLE:
        case BOOLEAN:
            return input;
        case BYTES:
            return Blobs.createBlob(((ByteBuffer) input).array());
        default:
            throw new RuntimeServiceException(CANNOT_MAP_FROM + schema.getType());
        }
    }

    @Override
    public Object toAvro(Schema schema, Property input) {
        switch (schema.getType()) {
        case NULL:
            if (input.getValue() == null) {
                return null;
            }
            throw new NonNullValueException();
        case UNION:
            for (Schema s : schema.getTypes()) {
                try {
                    return service.toAvro(s, input);
                } catch (NonNullValueException e) {
                    // this exception is thrown when a null value is expected and not found
                    // this happens for schema unions [null, schema]
                }
            }
            throw new RuntimeServiceException(CANNOT_MAP_TO + schema.getType());
        case RECORD:
            if (input.isComplex()) {
                GenericRecord record = new GenericData.Record(schema);
                for (Field f : schema.getFields()) {
                    record.put(f.name(), service.toAvro(f.schema(), input.get(service.decodeName(f.name()))));
                }
                return record;
            }
            throw new RuntimeServiceException(CANNOT_MAP_TO + schema.getType());
        case ARRAY:
            if (input.getType().isListType()) {
                Collection<Object> objects;
                if (((ListType) input.getType()).isArray()) {
                    objects = Arrays.asList((Object[]) input.getValue());
                } else {
                    ListProperty list = (ListProperty) input;
                    objects = list.stream()
                                  .map(p -> service.toAvro(schema.getElementType(), p))
                                  .collect(Collectors.toList());
                }
                return new GenericData.Array<>(schema, objects);
            }
            throw new RuntimeServiceException(CANNOT_MAP_TO + schema.getType());
        case INT:
        case FLOAT:
        case STRING:
        case DOUBLE:
        case BOOLEAN:
            if (input.isScalar()) {
                return input.getValue();
            }
            throw new RuntimeServiceException(CANNOT_MAP_TO + schema.getType());
        case LONG:
            if (input.isScalar()) {
                if (TIMESTAMP_MILLIS.equals(getLogicalType(schema))) {
                    GregorianCalendar cal = (GregorianCalendar) input.getValue();
                    return cal.toInstant().toEpochMilli();
                }
                return input.getValue();
            }
            throw new RuntimeServiceException(CANNOT_MAP_TO + schema.getType());
        default:
            throw new RuntimeServiceException(CANNOT_MAP_TO + schema.getType());
        }
    }

}
