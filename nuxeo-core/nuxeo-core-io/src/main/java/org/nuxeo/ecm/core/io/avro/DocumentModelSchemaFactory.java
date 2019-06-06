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

import java.util.Arrays;

import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.apache.avro.Schema.Type;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.avro.AvroSchemaFactory;
import org.nuxeo.runtime.avro.AvroSchemaFactoryContext;

/**
 * @since 10.2
 */
public class DocumentModelSchemaFactory extends AvroSchemaFactory<DocumentModel> {

    public DocumentModelSchemaFactory(AvroSchemaFactoryContext context) {
        super(context);
    }

    @Override
    public Schema createSchema(DocumentModel input) {
        Schema typeSchema = context.createSchema(input.getDocumentType());
        Schema schema = Schema.createRecord(getName(input), null, AvroConstants.ECM, false);
        new LogicalType(AvroConstants.DOCUMENT_MODEL).addToSchema(schema);
        schema.setFields(Arrays.asList(
                // mandatory
                new Field(AvroConstants.UUID, Schema.create(Type.STRING), null, (Object) null),
                new Field(AvroConstants.PATH, Schema.create(Type.STRING), null, (Object) null),
                new Field(AvroConstants.NAME, Schema.create(Type.STRING), null, (Object) null),
                new Field(AvroConstants.TITLE, Schema.create(Type.STRING), null, (Object) null),
                new Field(AvroConstants.REPOSITORY_NAME, Schema.create(Type.STRING), null, (Object) null),
                new Field(AvroConstants.PRIMARY_TYPE, Schema.create(Type.STRING), null, (Object) null),
                new Field(AvroConstants.CHANGE_TOKEN, Schema.create(Type.STRING), null, (Object) null),
                new Field(AvroConstants.CURRENT_LIFE_CYCLE_STATE, Schema.create(Type.STRING), null, (Object) null),
                new Field(AvroConstants.IS_PROXY, Schema.create(Type.BOOLEAN), null, (Object) null),
                new Field(AvroConstants.IS_TRASHED, Schema.create(Type.BOOLEAN), null, (Object) null),
                new Field(AvroConstants.IS_VERSION, Schema.create(Type.BOOLEAN), null, (Object) null),
                new Field(AvroConstants.IS_CHECKEDIN, Schema.create(Type.BOOLEAN), null, (Object) null),
                new Field(AvroConstants.IS_LATEST_VERSION, Schema.create(Type.BOOLEAN), null, (Object) null),
                new Field(AvroConstants.IS_LATEST_MAJOR_VERSION, Schema.create(Type.BOOLEAN), null, (Object) null),
                new Field(AvroConstants.IS_RECORD, Schema.create(Type.BOOLEAN), null, (Object) null),
                new Field(AvroConstants.HAS_LEGAL_HOLD, Schema.create(Type.BOOLEAN), null, (Object) null),
                // nullable
                new Field(AvroConstants.PARENT_ID, nullable(Schema.create(Type.STRING)), null, (Object) null),
                new Field(AvroConstants.VERSION_LABEL, nullable(Schema.create(Type.STRING)), null, (Object) null),
                new Field(AvroConstants.VERSION_VERSIONABLE_ID, nullable(Schema.create(Type.STRING)), null,
                        (Object) null),
                new Field(AvroConstants.POS, nullable(Schema.create(Type.LONG)), null, (Object) null),
                new Field(AvroConstants.MIXIN_TYPES, nullable(Schema.createArray(Schema.create(Type.STRING))), null,
                        (Object) null),
                new Field(AvroConstants.TAGS, nullable(Schema.createArray(Schema.create(Type.STRING))), null,
                        (Object) null),
                new Field(AvroConstants.RETAIN_UNTIL, nullable(Schema.create(Type.LONG)), null, (Object) null),
                new Field(AvroConstants.DOCUMENT_TYPE, typeSchema, null, (Object) null)));
        return schema;
    }

    @Override
    public String getName(DocumentModel input) {
        return context.getService().encodeName(input.getName());
    }

}
