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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;
import org.nuxeo.ecm.core.schema.types.Field;
import org.nuxeo.ecm.core.schema.types.Schema;

/**
 * @since 10.3
 */
@Setup(mode = SINGLETON, priority = REFERENCE)
public class DocumentModelListCSVWriter extends AbstractCSVWriter<List<DocumentModel>> {

    public DocumentModelListCSVWriter() {
        super();
    }

    @Override
    protected void write(List<DocumentModel> entity, CSVPrinter printer) throws IOException {
        Writer<DocumentModel> writer = registry.getWriter(null, DocumentModel.class, TEXT_CSV_TYPE);
        for (DocumentModel doc : entity) {
            writer.write(doc, DocumentModel.class, DocumentModel.class, TEXT_CSV_TYPE,
                    new OutputStreamWithCSVWriter(printer));
            printer.println();
        }
    }

    @Override
    protected void writeHeader(List<DocumentModel> entity, CSVPrinter printer) throws IOException {
        writeDocumentModelListHeader(entity, printer);
        printer.println();
    }

    protected void writeDocumentModelListHeader(List<DocumentModel> list, CSVPrinter printer) throws IOException {
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
        List<String> schemas = new ArrayList<>(Arrays.asList(list.get(0).getSchemas()));
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

}
