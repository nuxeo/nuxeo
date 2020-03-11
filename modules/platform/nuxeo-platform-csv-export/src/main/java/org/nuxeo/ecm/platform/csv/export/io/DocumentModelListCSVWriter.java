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
import static org.nuxeo.ecm.platform.csv.export.io.DocumentModelCSVWriter.SCHEMAS_CTX_DATA;
import static org.nuxeo.ecm.platform.csv.export.io.DocumentModelCSVWriter.XPATHS_CTX_DATA;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.csv.CSVPrinter;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.marshallers.csv.AbstractCSVWriter;
import org.nuxeo.ecm.core.io.marshallers.csv.OutputStreamWithCSVWriter;
import org.nuxeo.ecm.core.io.registry.Writer;
import org.nuxeo.ecm.core.io.registry.reflect.Setup;

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
        Writer<DocumentModel> writer = registry.getWriter(ctx, DocumentModel.class, TEXT_CSV_TYPE);
        for (DocumentModel doc : entity) {
            try (OutputStream out = new OutputStreamWithCSVWriter(printer)) {
                writer.write(doc, DocumentModel.class, DocumentModel.class, TEXT_CSV_TYPE, out);
            }
            printer.println();
        }
    }

    @Override
    protected void writeHeader(List<DocumentModel> entity, CSVPrinter printer) throws IOException {
        DocumentModelCSVHelper.printSystemPropertiesHeader(printer);
        List<String> schemas = getList(ctx, SCHEMAS_CTX_DATA);
        List<String> xpaths = getList(ctx, XPATHS_CTX_DATA);
        DocumentModelCSVHelper.printPropertiesHeader(schemas, xpaths, printer);
        printer.println();
    }

}
