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
 *     Funsho David
 */

package org.nuxeo.ecm.platform.csv.export.io;

import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.CHANGE_TOKEN_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.HAS_LEGAL_HOLD_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.IS_CHECKED_OUT_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.IS_PROXY_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.IS_RECORD_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.IS_TRASHED_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.IS_UNDER_RETENTION_OR_LEGAL_HOLD_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.IS_VERSION_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.LAST_MODIFIED_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.LOCK_CREATED_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.LOCK_OWNER_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.PARENT_REF_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.PATH_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.PROXY_TARGET_ID_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.REPOSITORY_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.RETAIN_UNTIL_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.STATE_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.TITLE_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.TYPE_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.UID_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.VERSIONABLE_ID_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.VERSION_LABEL_FIELD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.io.registry.context.RenderingContext;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.ListType;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.ecm.core.schema.types.Type;
import org.nuxeo.ecm.core.schema.types.resolver.ObjectResolver;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryEntryResolver;
import org.nuxeo.runtime.api.Framework;

/**
 * Utility class to have helper methods for exporting a document model in a CSV file.
 *
 * @since 10.3
 */
public class DocumentModelCSVHelper {

    /* Make sure this is kept in sync with DocumentModelCSVWriter.writeSystem */
    public static final String[] SYSTEM_PROPERTIES_HEADER_FIELDS = new String[] { REPOSITORY_FIELD, UID_FIELD,
            PATH_FIELD, TYPE_FIELD, STATE_FIELD, PARENT_REF_FIELD, IS_CHECKED_OUT_FIELD, IS_VERSION_FIELD,
            IS_PROXY_FIELD, PROXY_TARGET_ID_FIELD, VERSIONABLE_ID_FIELD, CHANGE_TOKEN_FIELD, IS_TRASHED_FIELD,
            TITLE_FIELD, VERSION_LABEL_FIELD, LOCK_OWNER_FIELD, LOCK_CREATED_FIELD, LAST_MODIFIED_FIELD,
            IS_RECORD_FIELD, RETAIN_UNTIL_FIELD, HAS_LEGAL_HOLD_FIELD, IS_UNDER_RETENTION_OR_LEGAL_HOLD_FIELD };

    public static final List<String> VOCABULARY_TYPES = Arrays.asList("vocabulary", "xvocabulary", "l10nxvocabulary");

    /**
     * Prints the fields for document model system properties.
     */
    public static void printSystemPropertiesHeader(CSVPrinter printer) throws IOException {
        for (String header : SYSTEM_PROPERTIES_HEADER_FIELDS) {
            printer.print(header);
        }
    }

    /**
     * Prints the fields for document model properties of given schemas and xpaths.
     */
    public static void printPropertiesHeader(List<String> schemas, List<String> xpaths, CSVPrinter printer)
            throws IOException {
        SchemaManager schemaManager = Framework.getService(SchemaManager.class);
        for (String schemaName : schemas) {
            Schema schema = schemaManager.getSchema(schemaName);
            if (schema != null) {
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
                    if (isVocabulary(f.getType())) {
                        printer.print(prefixedName + "[label]");
                    }
                }
            }
        }
        for (String xpath : xpaths) {
            Field field = schemaManager.getField(xpath);
            if (field != null) {
                printer.print(xpath);
                if (isVocabulary(field.getType())) {
                    printer.print(xpath + "[label]");
                }
            }
        }
    }

    /**
     * Checks if given type is a vocabulary.
     */
    public static boolean isVocabulary(Type type) {
        if (type instanceof ListType) {
            type = ((ListType) type).getFieldType();
        }
        ObjectResolver resolver = type.getObjectResolver();
        if (resolver instanceof DirectoryEntryResolver) {
            DirectoryEntryResolver directoryEntryResolver = (DirectoryEntryResolver) resolver;
            return VOCABULARY_TYPES.contains(directoryEntryResolver.getDirectory().getSchema());
        }
        return false;
    }

    public static Directory getVocabulary(Type type) {
        if (type instanceof ListType) {
            type = ((ListType) type).getFieldType();
        }
        if (isVocabulary(type)) {
            return ((DirectoryEntryResolver) type.getObjectResolver()).getDirectory();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getList(RenderingContext ctx, String key) {
        Object value = ctx.getParameter(key);
        if (value == null) {
            return Collections.emptyList();
        }
        return ((List<String>) value).stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private DocumentModelCSVHelper() {
        // utility class
    }
}
