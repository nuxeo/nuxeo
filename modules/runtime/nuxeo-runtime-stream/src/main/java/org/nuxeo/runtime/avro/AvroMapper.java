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

import org.apache.avro.Schema;

/**
 * The base class for any AvroMapper.<br>
 *
 * @since 10.2
 */
public abstract class AvroMapper<D, M> {

    protected static final String CANNOT_MAP_TO = "Cannot map value to ";

    protected static final String CANNOT_MAP_FROM = "Cannot map from value ";

    protected static final String LOGICAL_TYPE = "logicalType";

    protected final AvroService service;

    protected AvroMapper(AvroService service) {
        this.service = service;
    }

    public abstract Object fromAvro(Schema schema, M input);

    public abstract M toAvro(Schema schema, D input);

    protected String getLogicalType(Schema schema) {
        return schema.getProp(LOGICAL_TYPE);
    }

}
