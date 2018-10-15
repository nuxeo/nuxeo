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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;

/**
 * @since 10.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentModelCSVWriter extends AbstractCSVWriter<DocumentModel> {

    public DocumentModelCSVWriter() {
        super();
    }

    @Override
    protected void write(DocumentModel entity, CSVPrinter printer) throws IOException {
        writeSystem(entity, printer);
        List<String> schemas = new ArrayList<>(Arrays.asList(entity.getSchemas()));
        Collections.sort(schemas);
        for (String schema : schemas) {
            writeSchema(entity, schema, printer);
        }
    }

    @Override
    protected void writeHeader(DocumentModel entity, CSVPrinter printer) throws IOException {
        writeDocumentModelHeader(entity, printer);
        printer.println();
    }

    public void writeDocumentModelHeader(DocumentModel docModel, CSVPrinter printer) throws IOException {
        printer.print("repository");
        printer.print("uid");
        printer.print("path");
        printer.print("type");
        printer.print("state");
        printer.print("parentRef");
        printer.print("isCheckedOut");
        printer.print("isVersion");
        printer.print("isProxy");
        printer.print("proxyTargetId");
        printer.print("versionableId");
        printer.print("changeToken");
        printer.print("isTrashed");
        printer.print("title");
        printer.print("versionLabel");
        printer.print("lockOwner");
        printer.print("lockCreated");
        printer.print("lastModified");
        List<String> schemas = new ArrayList<>(Arrays.asList(docModel.getSchemas()));
        Collections.sort(schemas);
        for (String schemaName : schemas) {
            Schema schema = schemaManager.getSchema(schemaName);
            List<Field> fields = new ArrayList<>(schema.getFields());
            fields.sort(Comparator.comparing(o -> o.getName().getLocalName()));
            String prefix = schema.getNamespace().prefix;
            if (StringUtils.isBlank(prefix)) {
                prefix = schema.getName();
            }
            prefix += ":";
            for (Field f : fields) {
                String prefixedName = prefix + f.getName().getLocalName();
                printer.print(prefixedName);
            }
        }
    }

    protected void writeSystem(DocumentModel doc, CSVPrinter printer) throws IOException {
        printer.print(doc.getRepositoryName());
        printer.print(doc.getId());
        printer.print(doc.getPathAsString());
        printer.print(doc.getType());
        printer.print(doc.getRef() != null ? doc.getCurrentLifeCycleState() : null);
        printer.print(doc.getParentRef() != null ? doc.getParentRef().toString() : null);
        printer.print(doc.isCheckedOut());

        boolean isVersion = doc.isVersion();
        boolean isProxy = doc.isProxy();

        printer.print(doc.isVersion());
        printer.print(isProxy);
        printer.print(isProxy ? doc.getSourceId() : null);
        printer.print((isVersion || isProxy) ? doc.getVersionSeriesId() : null);
        printer.print(doc.getChangeToken());
        printer.print(doc.getRef() != null && doc.isTrashed());
        printer.print(doc.getTitle());
        printer.print(doc.getVersionLabel());
        Lock lock = doc.getLockInfo();
        if (lock != null) {
            printer.print(lock.getOwner());
            printCalendar(lock.getCreated(), printer);
        } else {
            printer.print(null);
            printer.print(null);
        }
        if (doc.hasSchema("dublincore")) {
            Calendar cal = (Calendar) doc.getPropertyValue("dc:modified");
            printCalendar(cal, printer);
        } else {
            printer.print(null);
        }
    }

    protected void writeSchema(DocumentModel entity, String schemaName, CSVPrinter printer) throws IOException {
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
        for (Field field : fields) {
            String prefixedName = prefix + field.getName().getLocalName();
            Property property = entity.getProperty(prefixedName);
            propertyWriter.write(property, Property.class, Property.class, TEXT_CSV_TYPE,
                    new OutputStreamWithCSVWriter(printer));
        }
    }
}
