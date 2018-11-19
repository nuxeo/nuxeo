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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVPrinter;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.io.marshallers.csv.AbstractCSVWriter;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.primitives.BinaryType;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;

/**
 * @since 10.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentPropertyCSVWriter extends AbstractCSVWriter<Property> {

    public static final String LIST_DELIMITER = "\n";

    public static final String LANG_CTX_DATA = "lang";

    public static final String NULL_PROPERTY_LABEL = "null property";

    public DocumentPropertyCSVWriter() {
        super();
    }

    @Override
    protected void write(Property property, CSVPrinter printer) throws IOException {
        if (property == null) {
            printer.print(null);
        } else if (property.isScalar()) {
            writeScalarProperty(property, printer);
        } else if (property.isList()) {
            writeListProperty(property, printer);
        } else {
            writeUnsupported(property.getType(), printer);
        }
    }

    @Override
    protected void writeHeader(Property property, CSVPrinter printer) throws IOException {
        printer.printRecord(property.getXPath());
    }

    protected void writeScalarProperty(Property property, CSVPrinter printer) throws IOException {
        Object value = property.getValue();
        Type type = property.getType();
        if (type instanceof BinaryType) {
            writeUnsupported(type, printer);
        } else {
            String valueAsString = null;
            if (value == null) {
                printer.print(null);
            } else {
                valueAsString = type.encode(value);
                printer.print(valueAsString);
            }
            Directory vocabulary = DocumentModelCSVHelper.getVocabulary(type);
            if (vocabulary != null) {
                writeScalarVocabularyProperty(valueAsString, vocabulary, printer);
            }
        }
    }

    protected void writeScalarVocabularyProperty(String value, Directory vocabulary, CSVPrinter printer)
            throws IOException {
        if (value == null) {
            printer.print(null);
        } else {
            try (Session session = vocabulary.getSession()) {
                String lang = ctx.getParameter(LANG_CTX_DATA);
                printer.print(getTranslatedValue(session, vocabulary.getSchema(), lang, value));
            }
        }
    }

    protected void writeListProperty(Property property, CSVPrinter printer) throws IOException {
        ListType type = (ListType) property.getType();
        if (property instanceof ArrayProperty) {
            Object[] array = (Object[]) property.getValue();
            if (array == null) {
                printer.print(null);
                return;
            }
            Type itemType = type.getFieldType();
            if (itemType instanceof BinaryType) {
                writeUnsupported(type, printer);
            } else {
                String value = Arrays.stream(array).map(itemType::encode).collect(Collectors.joining(LIST_DELIMITER));
                printer.print(value);
                Directory vocabulary = DocumentModelCSVHelper.getVocabulary(itemType);
                if (vocabulary != null) {
                    String[] values = Arrays.stream(array).map(itemType::encode).toArray(String[]::new);
                    writeListVocabularyProperty(values, vocabulary, printer);
                }
            }
        } else {
            writeUnsupported(type, printer);
        }
    }

    protected void writeListVocabularyProperty(String[] values, Directory vocabulary, CSVPrinter printer)
            throws IOException {
        List<String> translated = new ArrayList<>();
        try (Session session = vocabulary.getSession()) {
            String lang = ctx.getParameter(LANG_CTX_DATA);
            for (String value : values) {
                translated.add(getTranslatedValue(session, vocabulary.getSchema(), lang, value));
            }
        }
        printer.print(translated.stream().collect(Collectors.joining(LIST_DELIMITER)));
    }

    protected String getTranslatedValue(Session session, String schema, String lang, String value) {
        DocumentModel entry = session.getEntry(value);
        if (entry == null) {
            return NULL_PROPERTY_LABEL;
        }

        String label;
        try {
            label = (String) entry.getProperty(schema, "label");
        } catch (PropertyNotFoundException e) {
            try {
                // Check if it comes from a l10nxvocabulary, and return this value if it exists,
                // or else return the id
                return (String) entry.getProperty(schema, "label_en");
            } catch (PropertyNotFoundException e1) {
                return value;
            }
        }
        Locale locale = lang != null ? Locale.forLanguageTag(lang) : ctx.getLocale();
        return I18NUtils.getMessageString("messages", label, new Object[0], locale);
    }

    protected void writeUnsupported(Type type, CSVPrinter printer) throws IOException {
        printer.print(String.format("type %s is not supported", type.getName()));
    }

}
