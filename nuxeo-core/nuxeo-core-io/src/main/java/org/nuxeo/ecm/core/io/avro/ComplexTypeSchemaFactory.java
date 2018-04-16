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

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.runtime.avro.AvroSchemaFactory;
import org.nuxeo.runtime.avro.AvroSchemaFactoryContext;

/**
 * @since 10.2
 */
public class ComplexTypeSchemaFactory extends AvroSchemaFactory<ComplexType> {

    public ComplexTypeSchemaFactory(AvroSchemaFactoryContext context) {
        super(context);
    }

    @Override
    public Schema createSchema(ComplexType input) {
        Schema schema = Schema.createRecord(getName(input), null, input.getNamespace().prefix, false);
        List<Field> fields = new ArrayList<>(input.getFields().size());
        for (org.nuxeo.ecm.core.schema.types.Field f : context.sort(input.getFields())) {
            String fieldName = context.getService().encodeName(f.getName().getLocalName());
            Schema fieldSchema = context.createSchema(f.getType());
            if (f.isNillable()) {
                fieldSchema = nullable(fieldSchema);
            }
            fields.add(new Field(fieldName, fieldSchema, null, (Object) null));
        }
        schema.setFields(fields);
        return schema;
    }

    @Override
    public String getName(ComplexType input) {
        return context.getService().encodeName(input.getName());
    }

    @Override
    public String getQualifiedName(ComplexType input) {
        return context.getService().encodeName(input.getNamespace().prefix) + ":" + getName(input);
    }

}
