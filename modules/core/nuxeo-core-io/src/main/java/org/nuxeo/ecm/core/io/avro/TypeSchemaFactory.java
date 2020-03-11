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

import org.apache.avro.LogicalType;
import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.commons.lang3.NotImplementedException;
import org.nuxeo.ecm.core.schema.types.ComplexType;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.core.schema.types.primitives.BooleanType;
import org.nuxeo.ecm.core.schema.types.primitives.DateType;
import org.nuxeo.ecm.core.schema.types.primitives.DoubleType;
import org.nuxeo.ecm.core.schema.types.primitives.IntegerType;
import org.nuxeo.ecm.core.schema.types.primitives.LongType;
import org.nuxeo.ecm.core.schema.types.primitives.StringType;
import org.nuxeo.runtime.avro.AvroSchemaFactory;
import org.nuxeo.runtime.avro.AvroSchemaFactoryContext;

/**
 * @since 10.2
 */
public class TypeSchemaFactory extends AvroSchemaFactory<Type> {

    protected ComplexTypeSchemaFactory complexTypeFactory;

    public TypeSchemaFactory(AvroSchemaFactoryContext context) {
        super(context);
        complexTypeFactory = new ComplexTypeSchemaFactory(context);
    }

    @Override
    public Schema createSchema(Type input) {
        if (input.isSimpleType()) {
            if (input == IntegerType.INSTANCE) {
                return Schema.create(org.apache.avro.Schema.Type.INT);
            } else if (input == LongType.INSTANCE) {
                return Schema.create(org.apache.avro.Schema.Type.LONG);
            } else if (input == DoubleType.INSTANCE) {
                return Schema.create(org.apache.avro.Schema.Type.DOUBLE);
            } else if (input == StringType.INSTANCE) {
                return Schema.create(org.apache.avro.Schema.Type.STRING);
            } else if (input == BooleanType.INSTANCE) {
                return Schema.create(org.apache.avro.Schema.Type.BOOLEAN);
            } else if (input == BinaryType.INSTANCE) {
                return Schema.create(org.apache.avro.Schema.Type.BYTES);
            } else if (input == DateType.INSTANCE) {
                return LogicalTypes.timestampMillis().addToSchema(Schema.create(org.apache.avro.Schema.Type.LONG));
            } else if (input.getSuperType() != null && input.getSuperType().isSimpleType()) {
                return new LogicalType(getName(input)).addToSchema(createSchema(input.getSuperType()));
            }
        } else if (input.isListType()) {
            Schema array = Schema.createArray(context.createSchema(((ListType) input).getFieldType()));
            String logicalType = ((ListType) input).isArray() ? "array" : "list";
            new LogicalType(logicalType).addToSchema(array);
            return array;
        } else if (input.isComplexType()) {
            return complexTypeFactory.createSchema((ComplexType) input);
        } else if (input.isCompositeType()) {
            throw new NotImplementedException("Composite types are not supported yet");
        }
        throw new NotImplementedException("Cannot create Avro type schema for nuxeo type: " + input);
    }

    @Override
    public String getName(Type input) {
        return context.getService().encodeName(input.getName());
    }

}
