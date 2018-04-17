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

import java.util.Collections;

import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;
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
        Schema schema = Schema.createRecord(getName(input), null, null, false);
        Field field = new Field("type", context.createSchema(input.getDocumentType()), null, (Object) null);
        // TODO we could handle facets here
        schema.setFields(Collections.singletonList(field));
        return schema;
    }

    @Override
    public String getName(DocumentModel input) {
        return context.replaceForbidden(input.getName());
    }

}
