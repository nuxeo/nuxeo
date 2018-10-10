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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.registry.MarshallerRegistry;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;

/**
 * @since 10.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentModelCSVWriter extends AbstractCSVWriter<DocumentModel> {

    public DocumentModelCSVWriter() {
        super(DocumentModel.class);
    }

    @Inject
    protected SchemaManager schemaManager;

    @Inject
    protected MarshallerRegistry registry;

    @Override
    public void write(DocumentModel entity, Class<?> clazz, Type genericType, MediaType mediatype, OutputStream out)
            throws IOException {
        writeSystem(out, entity);
        // schemas are sorted for reproducibility
        List<String> schemas = new ArrayList<>(Arrays.asList(entity.getSchemas()));
        Collections.sort(schemas);
        for (int i = 0; i < schemas.size(); i++) {
            writeSchema(out, entity, schemas.get(i));
            if (i < schemas.size() - 1) {
                writeSeparator(out);
            }
        }
    }

    /**
     * CSV must have consistent column number, null and empy values have to be written.
     */
    protected void writeSystem(OutputStream out, DocumentModel doc) throws IOException {
        writeWithSeparator(out, doc.getId());
        writeWithSeparator(out, doc.getPathAsString());
        writeWithSeparator(out, doc.getRepositoryName());
        writeWithSeparator(out, doc.getType());
        writeWithSeparator(out, doc.getRef() != null ? doc.getCurrentLifeCycleState() : null);
        writeWithSeparator(out, doc.getTitle());
        writeWithSeparator(out, doc.getParentRef() != null ? doc.getParentRef().toString() : null);
        writeWithSeparator(out, Boolean.toString(doc.isTrashed()));
        writeWithSeparator(out, Boolean.toString(doc.isVersion()));
        writeWithSeparator(out, Boolean.toString(doc.isProxy()));
        writeWithSeparator(out, Boolean.toString(doc.isCheckedOut()));
        writeWithSeparator(out, doc.getChangeToken());
        writeWithSeparator(out, doc.getVersionLabel());
        Lock lock = doc.getLockInfo();
        if (lock != null) {
            writeWithSeparator(out, lock.getOwner());
            writeCalendarWithSeparator(out, lock.getCreated());
        } else {
            writeWithSeparator(out, null);
            writeWithSeparator(out, null);
        }
        if (doc.hasSchema("dublincore")) {
            Calendar cal = (Calendar) doc.getPropertyValue("dc:modified");
            if (cal != null) {
                writeCalendarWithSeparator(out, cal);
            }
        } else {
            writeWithSeparator(out, null);
        }
    }

    protected void writeSchema(OutputStream out, DocumentModel entity, String schemaName) throws IOException {
        Writer<Property> propertyWriter = registry.getWriter(null, Property.class, TEXT_CSV_TYPE);
        // provides the current document to the property writer
        Schema schema = schemaManager.getSchema(schemaName);
        List<Field> fields = new ArrayList<>(schema.getFields());
        fields.sort(Comparator.comparing(o -> o.getName().getLocalName()));
        String prefix = schema.getNamespace().prefix;
        if (StringUtils.isBlank(prefix)) {
            prefix = schema.getName();
        }
        prefix += ":";
        // fields are sorted for reproducibility
        for (int i = 0; i < fields.size(); i++) {
            String prefixedName = prefix + fields.get(i).getName().getLocalName();
            Property property = entity.getProperty(prefixedName);
            propertyWriter.write(property, Property.class, Property.class, TEXT_CSV_TYPE, out);
            if (i < fields.size() - 1) {
                writeSeparator(out);
            }
        }
    }

}
