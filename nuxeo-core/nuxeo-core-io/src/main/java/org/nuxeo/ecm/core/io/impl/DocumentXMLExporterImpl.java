/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.core.io.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.DocumentXMLExporter;
import org.nuxeo.ecm.core.io.impl.plugins.TypedSingleDocumentReader;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentWriter;
import org.xml.sax.InputSource;

/**
 * Default implementation of a {@link DocumentXMLExporter}.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class DocumentXMLExporterImpl implements DocumentXMLExporter {

    private static final long serialVersionUID = 4086449614391137730L;

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream exportXML(DocumentModel doc, CoreSession session) {

        byte[] xmlExportByteArray = exportXMLAsByteArray(doc, session);
        return new ByteArrayInputStream(xmlExportByteArray);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputSource exportXMLAsInputSource(DocumentModel doc, CoreSession session) {

        return new InputSource(exportXML(doc, session));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final byte[] exportXMLAsByteArray(DocumentModel doc, CoreSession session) {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DocumentWriter documentWriter = new XMLDocumentWriter(outputStream);
        DocumentReader documentReader = new TypedSingleDocumentReader(session, doc);

        DocumentPipe pipe = new DocumentPipeImpl();
        pipe.setReader(documentReader);
        pipe.setWriter(documentWriter);

        try {
            pipe.run();
        } catch (IOException e) {
            throw new NuxeoException("Error while trying to export the document to XML.", e);
        } finally {
            if (documentReader != null) {
                documentReader.close();
            }
            if (documentWriter != null) {
                documentWriter.close();
            }
        }

        return outputStream.toByteArray();
    }
}
