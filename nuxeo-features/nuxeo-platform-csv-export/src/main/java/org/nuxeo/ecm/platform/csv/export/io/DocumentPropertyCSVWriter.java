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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVPrinter;
import org.nuxeo.common.utils.i18n.I18NUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.BlobProperty.ScalarMemberProperty;
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

    public static final String LIST_DELIMITER = " | ";

    /** @since 11.5 */
    public static final String NEWLINE_REPLACEMENT_CTX_DATA = "newlineReplacement";

    public static final String LANG_CTX_DATA = "lang";

    public static final String UNKNOWN_TRANSLATED_VALUE_LABEL = "unknown translated value";

    /** @since 2021.10 */
    public static final Pattern FORBIDDEN_CHARACTERS = Pattern.compile("^(=|\\+|-|@|\\t|\\r)+");

    protected static final Pattern NEWLINE = Pattern.compile("\\R");

    /** @since 2021.10 */
    protected Function<String, String> sanitizer = Function.<String> identity()
                                                           .andThen(this::replaceNewline)
                                                           .andThen(
                                                                   DocumentPropertyCSVWriter::removeFirstForbiddenCharacter);

    public DocumentPropertyCSVWriter() {
        super();
    }

    @Override
    protected void write(Property property, CSVPrinter printer) throws IOException {
        if (property == null) {
            printer.print(null);
        } else if (property.isScalar()) {
            if (property instanceof ScalarMemberProperty && property.getParent().getValue() == null) {
                printer.print(null);
                return;
            }
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
            if (value == null) {
                printer.print(null);
            } else {
                String valueAsString = type.encode(value);
                valueAsString = sanitize(valueAsString);
                printer.print(valueAsString);
            }
            Directory vocabulary = DocumentModelCSVHelper.getVocabulary(type);
            if (vocabulary != null) {
                String valueAsString = value == null ? null : type.encode(value);
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
            Type itemType = type.getFieldType();
            Directory vocabulary = DocumentModelCSVHelper.getVocabulary(itemType);
            if (array == null) {
                printer.print(null);
                if (vocabulary != null) {
                    printer.print(null);
                }
                return;
            }
            if (itemType instanceof BinaryType) {
                writeUnsupported(type, printer);
            } else {
                String value = Arrays.stream(array)
                                     .map(itemType::encode)
                                     .map(this::sanitize)
                                     .collect(Collectors.joining(LIST_DELIMITER));
                printer.print(value);
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
        if (value.contains("/")) {
            value = value.substring(value.lastIndexOf("/") + 1);
        }
        DocumentModel entry = session.getEntry(value);
        if (entry == null) {
            return UNKNOWN_TRANSLATED_VALUE_LABEL;
        }

        String label;
        try {
            label = (String) entry.getProperty(schema, "label");
        } catch (PropertyNotFoundException e) {
            try {
                // Check if it comes from a l10nxvocabulary, and return this value if it exists,
                // or else return the id
                return sanitize((String) entry.getProperty(schema, "label_en"));
            } catch (PropertyNotFoundException e1) {
                return sanitize(value);
            }
        }
        Locale locale = lang != null ? Locale.forLanguageTag(lang) : ctx.getLocale();
        return sanitize(I18NUtils.getMessageString("messages", label, new Object[0], locale));
    }

    protected void writeUnsupported(Type type, CSVPrinter printer) throws IOException {
        printer.print(String.format("type %s is not supported", type.getName()));
    }

    protected String replaceNewline(String value) {
        String replacement = ctx.getParameter(NEWLINE_REPLACEMENT_CTX_DATA);
        if (value == null || replacement == null) {
            return value;
        }
        Matcher m = NEWLINE.matcher(value);
        if (m.find()) {
            value = m.replaceAll(Matcher.quoteReplacement(replacement));
        }
        return value;
    }

    /** @since 2021.10 */
    public static String removeFirstForbiddenCharacter(String value) {
        return FORBIDDEN_CHARACTERS.matcher(value).replaceFirst("");
    }

    /** @since 2021.10 */
    protected String sanitize(String value) {
        return sanitizer.apply(value);
    }

}
