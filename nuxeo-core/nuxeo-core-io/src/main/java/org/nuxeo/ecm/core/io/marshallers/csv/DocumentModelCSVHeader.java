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

package org.nuxeo.ecm.core.io.marshallers.csv;

import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.CHANGE_TOKEN_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.IS_CHECKED_OUT_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.IS_PROXY_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.IS_TRASHED_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.IS_VERSION_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.LAST_MODIFIED_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.LOCK_CREATED_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.LOCK_OWNER_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.PARENT_REF_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.PATH_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.PROXY_TARGET_ID_FIELD;
import static org.nuxeo.ecm.core.io.marshallers.csv.CSVMarshallerConstants.REPOSITORY_FIELD;
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
import java.util.stream.Stream;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.schema.SchemaManager;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;
import org.nuxeo.runtime.api.Framework;

/**
 * Utility class to generate CSV header for document model.
 *
 * @since 10.3
 */
public class DocumentModelCSVHeader {

    public static final String[] SYSTEM_PROPERTIES_HEADER_FIELDS = new String[] { REPOSITORY_FIELD, UID_FIELD,
            PATH_FIELD, TYPE_FIELD, STATE_FIELD, PARENT_REF_FIELD, IS_CHECKED_OUT_FIELD, IS_VERSION_FIELD,
            IS_PROXY_FIELD, PROXY_TARGET_ID_FIELD, VERSIONABLE_ID_FIELD, CHANGE_TOKEN_FIELD, IS_TRASHED_FIELD,
            TITLE_FIELD, VERSION_LABEL_FIELD, LOCK_OWNER_FIELD, LOCK_CREATED_FIELD, LAST_MODIFIED_FIELD };

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
        if (schemas != null) {
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
        if (xpaths != null) {
            Collections.sort(xpaths);
            for (String xpath : xpaths) {
                printer.print(xpath);
            }
        }
    }

    private DocumentModelCSVHeader() {
        // utility class
    }
}
