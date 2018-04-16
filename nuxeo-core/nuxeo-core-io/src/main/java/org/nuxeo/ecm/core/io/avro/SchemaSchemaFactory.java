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
import org.apache.avro.Schema;
import org.nuxeo.runtime.avro.AvroSchemaFactory;
import org.nuxeo.runtime.avro.AvroSchemaFactoryContext;

/**
 * @since 10.2
 */
public class SchemaSchemaFactory extends AvroSchemaFactory<org.nuxeo.ecm.core.schema.types.Schema> {

    protected ComplexTypeSchemaFactory complexTypeFactory;

    public SchemaSchemaFactory(AvroSchemaFactoryContext context) {
        super(context);
        complexTypeFactory = new ComplexTypeSchemaFactory(context);
    }

    @Override
    public Schema createSchema(org.nuxeo.ecm.core.schema.types.Schema input) {
        Schema schema = complexTypeFactory.createSchema(input);
        new LogicalType("schema").addToSchema(schema);
        return schema;
    }

    @Override
    public String getName(org.nuxeo.ecm.core.schema.types.Schema input) {
        return complexTypeFactory.getName(input);
    }

}
