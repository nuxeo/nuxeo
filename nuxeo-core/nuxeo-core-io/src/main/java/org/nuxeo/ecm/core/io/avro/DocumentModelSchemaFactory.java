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

import java.util.LinkedList;
import java.util.List;

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
        Schema schema = Schema.createRecord(getName(input), null, "ecm", false);
        new LogicalType(DocumentModelMapper.DOCUMENT_MODEL).addToSchema(schema);
        Schema typeSchema = context.createSchema(input.getDocumentType());
        List<Field> fields = new LinkedList<>();
        fields.add(new Field(DocumentModelMapper.UUID, Schema.create(Type.STRING), null, (Object) null));
        fields.add(new Field(DocumentModelMapper.PATH, Schema.create(Type.STRING), null, (Object) null));
        fields.add(new Field(DocumentModelMapper.PRIMARY_TYPE, typeSchema, null, (Object) null));
        schema.setFields(fields);
        // TODO we could handle facets here
        return schema;
    }

    @Override
    public String getName(DocumentModel input) {
        return context.getService().encodeName(input.getName());
    }

}
