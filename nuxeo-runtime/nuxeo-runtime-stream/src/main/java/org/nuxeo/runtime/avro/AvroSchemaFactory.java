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
package org.nuxeo.runtime.avro;

import java.util.Arrays;

import org.apache.avro.Schema;

/**
 * The base class for any AvroSchemaFactory.<br>
 *
 * @since 10.2
 */
public abstract class AvroSchemaFactory<T> {

    protected static final Schema NULL_SCHEMA = Schema.create(Schema.Type.NULL);

    protected final AvroSchemaFactoryContext context;

    public AvroSchemaFactory(AvroSchemaFactoryContext context) {
        this.context = context;
    }

    public abstract Schema createSchema(T input);

    public abstract String getName(T input);

    public String getQualifiedName(T input) {
        return getName(input);
    }

    protected Schema nullable(Schema schema) {
        return Schema.createUnion(Arrays.asList(NULL_SCHEMA, schema));
    }

}
