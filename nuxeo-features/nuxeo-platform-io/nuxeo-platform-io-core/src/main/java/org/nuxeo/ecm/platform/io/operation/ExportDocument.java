/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.io.operation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.core.io.impl.plugins.SingleDocumentReader;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentTreeWriter;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentWriter;
import org.nuxeo.runtime.api.Framework;

/**
 * Operation that export a document.
 *
 * @since 7.2
 */
@Operation(id = ExportDocument.ID, category = Constants.CAT_SERVICES, label = "Document Export", description = "Export the given document.")
public class ExportDocument {

    public static final String ID = "Document.Export";

    @Param(name = "exportAsTree", description = "Export the document and all its children", required = false)
    protected boolean exportAsTree = false;

    @Param(name = "exportAsZip", description = "Create a ZIP of the export. If 'exportAsTree' is true, exportAsZip is force to true", required = false)
    protected boolean exportAsZip = false;

    @Context
    protected CoreSession session;

    @OperationMethod
    public Blob run(DocumentModel doc) throws IOException {
        if (exportAsTree) {
            exportAsZip = true;
        }

        File tempFile = Framework.createTempFilePath(null, null).toFile();
        Framework.trackFile(tempFile, tempFile);

        DocumentReader documentReader = null;
        DocumentWriter documentWriter = null;
        try {
            documentReader = makeDocumentReader(session, doc, exportAsTree, exportAsZip);
            @SuppressWarnings("resource") // closed by documentWriter.close()
            FileOutputStream out = new FileOutputStream(tempFile);
            documentWriter = makeDocumentWriter(out, exportAsTree, exportAsZip);
            DocumentPipe pipe = makePipe(exportAsTree);
            pipe.setReader(documentReader);
            pipe.setWriter(documentWriter);
            pipe.run();
        } finally {
            if (documentReader != null) {
                documentReader.close();
            }
            if (documentWriter != null) {
                documentWriter.close();
            }
        }

        String filename = exportAsZip ? "export.zip" : "document.xml";
        String mimeType = exportAsZip ? "application/zip" : "text/xml";
        return Blobs.createBlob(tempFile, mimeType, null, filename);
    }

    protected DocumentPipe makePipe(boolean exportAsTree) {
        if (exportAsTree) {
            return new DocumentPipeImpl(10);
        } else {
            return new DocumentPipeImpl();
        }
    }

    protected DocumentReader makeDocumentReader(CoreSession session, DocumentModel doc, boolean exportAsTree,
            boolean exportAsZip) {
        DocumentReader documentReader;
        if (exportAsTree) {
            documentReader = new DocumentTreeReader(session, doc, false);
            if (!exportAsZip) {
                ((DocumentTreeReader) documentReader).setInlineBlobs(true);
            }
        } else {
            documentReader = new SingleDocumentReader(session, doc);
        }
        return documentReader;
    }

    protected DocumentWriter makeDocumentWriter(OutputStream outputStream, boolean exportAsTree, boolean exportAsZip)
            throws IOException {
        DocumentWriter documentWriter;
        if (exportAsZip) {
            documentWriter = new NuxeoArchiveWriter(outputStream);
        } else {
            if (exportAsTree) {
                documentWriter = new XMLDocumentTreeWriter(outputStream);
            } else {
                documentWriter = new XMLDocumentWriter(outputStream);
            }
        }
        return documentWriter;
    }
}
