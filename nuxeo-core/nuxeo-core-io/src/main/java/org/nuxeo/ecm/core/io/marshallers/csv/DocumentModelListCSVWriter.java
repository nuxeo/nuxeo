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
package org.nuxeo.ecm.core.io.marshallers.csv;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;

/**
 * @since 10.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentModelListCSVWriter extends AbstractCSVWriter<DocumentModelList> {

    public DocumentModelListCSVWriter() {
        super(DocumentModelList.class);
    }

    @Inject
    protected SchemaManager schemaManager;

    @Inject
    protected MarshallerRegistry registry;

    @Override
    public void write(DocumentModelList entity, Class<?> clazz, Type genericType, MediaType mediatype, OutputStream out)
            throws IOException {
        Writer<DocumentModel> writer = registry.getWriter(null, DocumentModel.class, TEXT_CSV_TYPE);
        for (DocumentModel doc : entity) {
            writer.write(doc, DocumentModel.class, null, TEXT_CSV_TYPE, out);
            out.write("\n".getBytes(UTF8));
        }
    }

}
