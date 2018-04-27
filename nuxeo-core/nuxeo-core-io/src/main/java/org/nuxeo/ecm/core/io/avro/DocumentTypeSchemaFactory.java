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

import java.util.ArrayList;
import java.util.List;

import org.apache.avro.LogicalType;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.nuxeo.ecm.core.schema.DocumentType;
import org.nuxeo.runtime.avro.AvroSchemaFactory;
import org.nuxeo.runtime.avro.AvroSchemaFactoryContext;

/**
 * @since 10.2
 */
public class DocumentTypeSchemaFactory extends AvroSchemaFactory<DocumentType> {

    public DocumentTypeSchemaFactory(AvroSchemaFactoryContext context) {
        super(context);
    }

    @Override
    public Schema createSchema(DocumentType input) {
        Schema schema = Schema.createRecord(getName(input), null, input.getNamespace().prefix, false);
        new LogicalType(AvroConstants.DOCUMENT_TYPE).addToSchema(schema);
        List<Field> fields = new ArrayList<>(input.getSchemas().size());
        for (org.nuxeo.ecm.core.schema.types.Schema s : context.sort(input.getSchemas())) {
            fields.add(new Field(s.getName(), context.createSchema(s), null, (Object) null));
        }
        schema.setFields(fields);
        return schema;
    }

    @Override
    public String getName(DocumentType input) {
        return context.getService().encodeName(input.getName());
    }

}
