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
package org.nuxeo.ecm.platform.csv.export.io;

import static org.nuxeo.ecm.core.io.registry.reflect.Instantiations.SINGLETON;
import static org.nuxeo.ecm.core.io.registry.reflect.Priorities.REFERENCE;
import static org.nuxeo.ecm.platform.csv.export.io.DocumentModelCSVHelper.getList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.Lock;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.DocumentPartImpl;
import org.nuxeo.ecm.core.api.model.impl.PropertyFactory;
import org.nuxeo.ecm.core.io.marshallers.csv.AbstractCSVWriter;
import org.nuxeo.ecm.core.io.marshallers.csv.OutputStreamWithCSVWriter;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;

/**
 * @since 10.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentModelCSVWriter extends AbstractCSVWriter<DocumentModel> {

    public static final String SCHEMAS_CTX_DATA = "schemas";

    public static final String XPATHS_CTX_DATA = "xpaths";

    public DocumentModelCSVWriter() {
        super();
    }

    @Override
    protected void write(DocumentModel entity, CSVPrinter printer) throws IOException {
        writeSystem(entity, printer);
        for (String schemaName : getList(ctx, SCHEMAS_CTX_DATA)) {
            Schema schema = schemaManager.getSchema(schemaName);
            if (schema != null) {
                writeSchema(entity, schema, printer);
            }
        }
        for (String xpath : getList(ctx, XPATHS_CTX_DATA)) {
            Field field = schemaManager.getField(xpath);
            if (field != null) {
                writeProperty(entity, xpath, printer);
            }
        }
    }

    @Override
    protected void writeHeader(DocumentModel entity, CSVPrinter printer) throws IOException {
        DocumentModelCSVHelper.printSystemPropertiesHeader(printer);
        List<String> schemas = getList(ctx, SCHEMAS_CTX_DATA);
        List<String> xpaths = getList(ctx, XPATHS_CTX_DATA);
        DocumentModelCSVHelper.printPropertiesHeader(schemas, xpaths, printer);
        printer.println();
    }

    /* Make sure this is kept in sync with DocumentModelCSVHelper.SYSTEM_PROPERTIES_HEADER_FIELDS */
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
        printer.print(isVersion || isProxy ? doc.getVersionSeriesId() : null);
        printer.print(doc.getChangeToken());
        printer.print(doc.getRef() != null && doc.isTrashed());
        String sanitizedTitle = DocumentPropertyCSVWriter.removeFirstForbiddenCharacter(doc.getTitle());
        printer.print(sanitizedTitle);
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
        Calendar retainUntil = doc.getRetainUntil();
        printer.print(doc.isRecord());
        if (retainUntil != null) {
            printCalendar(retainUntil, printer);
        } else {
            printer.print(null);
        }
        printer.print(doc.hasLegalHold());
        printer.print(doc.isUnderRetentionOrLegalHold());
    }

    protected void writeSchema(DocumentModel entity, Schema schema, CSVPrinter printer) throws IOException {
        // provides the current document to the property writer
        List<Field> fields = new ArrayList<>(schema.getFields());
        // fields are sorted for reproducibility
        fields.sort(Comparator.comparing(o -> o.getName().getLocalName()));
        String prefix = schema.getNamespace().prefix;
        if (StringUtils.isBlank(prefix)) {
            prefix = schema.getName();
        }
        prefix += ":";
        for (Field field : fields) {
            writeProperty(entity, prefix + field.getName().getLocalName(), printer);
        }
    }

    protected void writeProperty(DocumentModel entity, String xpath, CSVPrinter printer) throws IOException {
        Writer<Property> propertyWriter = registry.getWriter(ctx, Property.class, TEXT_CSV_TYPE);
        Property property;
        try {
            property = entity.getProperty(xpath);
        } catch (PropertyNotFoundException e) {
            // probably mixed content, create a mock
            Field field = schemaManager.getField(xpath);
            property = PropertyFactory.createProperty(new DocumentPartImpl(field.getDeclaringType().getSchema()), field,
                    Property.NONE);
        }
        propertyWriter.write(property, Property.class, Property.class, TEXT_CSV_TYPE,
                new OutputStreamWithCSVWriter(printer));
    }
}
