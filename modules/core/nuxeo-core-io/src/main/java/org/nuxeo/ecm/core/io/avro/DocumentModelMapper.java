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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelImpl;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.avro.AvroMapper;
import org.nuxeo.runtime.avro.AvroService;

/**
 * @since 10.2
 */
public class DocumentModelMapper extends AvroMapper<DocumentModel, GenericRecord> {

    public DocumentModelMapper(AvroService service) {
        super(service);
    }

    @Override
    public DocumentModel fromAvro(Schema schema, GenericRecord input) {
        if (!AvroConstants.DOCUMENT_MODEL.equals(getLogicalType(schema))) {
            throw new RuntimeServiceException("Schema does not match DocumentModel");
        }
        DocumentModel doc = documentModelFromAvro(input);
        GenericRecord documentTypeRecord = (GenericRecord) input.get(AvroConstants.DOCUMENT_TYPE);
        for (Field schemaField : documentTypeRecord.getSchema().getFields()) {
            GenericRecord schemaRecord = (GenericRecord) documentTypeRecord.get(schemaField.name());
            List<Field> fields = schemaField.schema().getFields();
            Map<String, Object> data = new HashMap<>(fields.size());
            for (Field field : fields) {
                data.put(service.decodeName(field.name()),
                        service.fromAvro(field.schema(), Property.class, schemaRecord.get(field.name())));
            }
            // set properties with privilege to be able to set secure properties
            Framework.doPrivileged(() -> doc.setProperties(service.decodeName(schemaField.name()), data));
        }
        return doc;
    }

    @Override
    public GenericRecord toAvro(Schema schema, DocumentModel input) {
        GenericRecord record = new GenericData.Record(schema);
        if (AvroConstants.DOCUMENT_MODEL.equals(getLogicalType(schema))) {
            documentModelToAvro(schema, input, record);
        } else if (AvroConstants.DOCUMENT_TYPE.equals(getLogicalType(schema))) {
            for (Field field : schema.getFields()) {
                record.put(field.name(), service.toAvro(field.schema(), input));
            }
        } else {
            for (Field field : schema.getFields()) {
                Property p = input.getProperty(service.decodeName(field.name()));
                record.put(field.name(), service.toAvro(field.schema(), p));
            }
        }
        return record;
    }

    @SuppressWarnings("unchecked")
    protected DocumentModel documentModelFromAvro(GenericRecord input) {
        String path = (String) input.get(AvroConstants.PATH);
        String type = (String) input.get(AvroConstants.PRIMARY_TYPE);
        String uuid = (String) input.get(AvroConstants.UUID);
        String parentId = (String) input.get(AvroConstants.PARENT_ID);
        String repositoryName = (String) input.get(AvroConstants.REPOSITORY_NAME);
        Boolean isProxy = (Boolean) input.get(AvroConstants.IS_PROXY);
        DocumentRef parentRef = parentId == null ? null : new IdRef(parentId);
        Set<String> facets = (Set<String>) input.get(AvroConstants.MIXIN_TYPES);
        DocumentModelImpl doc = new DocumentModelImpl(null, type, uuid, new Path(path), null, parentRef,
                null, facets, null, repositoryName, isProxy);
        doc.setIsVersion((Boolean) input.get(AvroConstants.IS_VERSION));
        doc.prefetchCurrentLifecycleState((String) input.get(AvroConstants.CURRENT_LIFE_CYCLE_STATE));
        Boolean isRecord = (Boolean) input.get(AvroConstants.IS_RECORD);
        if (isRecord) {
            doc.makeRecord();
            Long retainUntilMillis = (Long) input.get(AvroConstants.RETAIN_UNTIL);
            if (retainUntilMillis != null) {
                Calendar retainUntil = Calendar.getInstance();
                retainUntil.setTimeInMillis(retainUntilMillis);
                doc.setRetainUntil(retainUntil);
            }
            Boolean hasLegalHold = (Boolean) input.get(AvroConstants.HAS_LEGAL_HOLD);
            doc.setLegalHold(hasLegalHold);
        }
        return doc;
    }

    protected void documentModelToAvro(Schema schema, DocumentModel doc, GenericRecord record) {
        record.put(AvroConstants.UUID, doc.getId());
        record.put(AvroConstants.NAME, doc.getName());
        record.put(AvroConstants.TITLE, doc.getTitle());
        record.put(AvroConstants.PATH, doc.getPathAsString());
        record.put(AvroConstants.REPOSITORY_NAME, doc.getRepositoryName());
        record.put(AvroConstants.PRIMARY_TYPE, doc.getType());
        DocumentRef parentRef = doc.getParentRef();
        if (parentRef != null) {
            record.put(AvroConstants.PARENT_ID, parentRef.toString());
        }
        record.put(AvroConstants.CURRENT_LIFE_CYCLE_STATE, doc.getCurrentLifeCycleState());
        if (doc.isVersion()) {
            record.put(AvroConstants.VERSION_LABEL, doc.getVersionLabel());
            record.put(AvroConstants.VERSION_VERSIONABLE_ID, doc.getVersionSeriesId());
        }
        record.put(AvroConstants.IS_PROXY, doc.isProxy());
        record.put(AvroConstants.IS_TRASHED, doc.isTrashed());
        record.put(AvroConstants.IS_VERSION, doc.isVersion());
        record.put(AvroConstants.IS_CHECKEDIN, !doc.isCheckedOut());
        record.put(AvroConstants.IS_LATEST_VERSION, doc.isLatestVersion());
        record.put(AvroConstants.IS_LATEST_MAJOR_VERSION, doc.isLatestMajorVersion());
        record.put(AvroConstants.IS_RECORD, doc.isRecord());
        Calendar retainUntil = doc.getRetainUntil();
        if (retainUntil != null) {
            record.put(AvroConstants.RETAIN_UNTIL, retainUntil.toInstant().toEpochMilli());
        }
        record.put(AvroConstants.HAS_LEGAL_HOLD, doc.hasLegalHold());
        record.put(AvroConstants.CHANGE_TOKEN, doc.getChangeToken());
        if (doc.getPos() != null) {
            record.put(AvroConstants.POS, doc.getPos());
        }
        // facets
        record.put(AvroConstants.MIXIN_TYPES, doc.getFacets());
        // document type with schemas
        record.put(AvroConstants.DOCUMENT_TYPE, service.toAvro(schema.getField(AvroConstants.DOCUMENT_TYPE).schema(), doc));
        // INFO \\ tags and acls are ignored for now
    }

}
