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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.avro.AvroMapper;
import org.nuxeo.runtime.avro.AvroService;

/**
 * @since 10.2
 */
public class DocumentModelMapper extends AvroMapper<DocumentModel, GenericRecord> {

    public static final String UUID = "uuid";

    public static final String PATH = "path";

    public static final String PRIMARY_TYPE = "primaryType";

    public static final String DOCUMENT_TYPE = "documentType";

    public static final String DOCUMENT_MODEL = "documentModel";

    public DocumentModelMapper(AvroService service) {
        super(service);
    }

    @Override
    public DocumentModel fromAvro(Schema schema, GenericRecord input) {
        if (!DOCUMENT_MODEL.equals(getLogicalType(schema))) {
            throw new RuntimeServiceException("Schema does not match DocumentModel");
        }
        String path = (String) input.get(PATH);
        GenericRecord documentTypeRecord = (GenericRecord) input.get(PRIMARY_TYPE);
        DocumentModel doc = new DocumentModelImpl((String) null, path, documentTypeRecord.getSchema().getName());
        for (Field schemaField : documentTypeRecord.getSchema().getFields()) {
            GenericRecord schemaRecord = (GenericRecord) documentTypeRecord.get(schemaField.name());
            List<Field> fields = schemaField.schema().getFields();
            Map<String, Object> data = new HashMap<>(fields.size());
            for (Field field : fields) {
                data.put(service.decodeName(field.name()),
                        service.fromAvro(field.schema(), Property.class, schemaRecord.get(field.name())));
            }
            doc.setProperties(service.decodeName(schemaField.name()), data);
        }
        return doc;
    }

    @Override
    public GenericRecord toAvro(Schema schema, DocumentModel input) {
        String logicalType = getLogicalType(schema);
        GenericRecord record = new GenericData.Record(schema);
        if (DOCUMENT_MODEL.equals(logicalType)) {
            record.put(PATH, input.getPathAsString());
            record.put(UUID, input.getId());
        }
        for (Field field : schema.getFields()) {
            if (field.name().equals(UUID) || field.name().equals(PATH)) {
                continue;
            }
            if (field.schema().getType() == Type.RECORD
                    && (DOCUMENT_MODEL.equals(logicalType) || DOCUMENT_TYPE.equals(logicalType))) {
                record.put(field.name(), service.toAvro(field.schema(), input));
            } else {
                Property p = input.getProperty(service.decodeName(field.name()));
                record.put(field.name(), service.toAvro(field.schema(), p));
            }
        }
        return record;
    }

}
