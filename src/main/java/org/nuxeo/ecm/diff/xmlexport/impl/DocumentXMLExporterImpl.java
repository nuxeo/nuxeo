/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     ataillefer
 */
package org.nuxeo.ecm.diff.xmlexport.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.TypedSingleDocumentReader;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentWriter;
import org.nuxeo.ecm.diff.xmlexport.DocumentXMLExporter;
import org.xml.sax.InputSource;

/**
 * Implementation of DocumentXMLExporter.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class DocumentXMLExporterImpl implements DocumentXMLExporter {

    private static final long serialVersionUID = 4086449614391137730L;

    /**
     * {@inheritDoc}
     */
    public InputStream exportXML(DocumentModel doc, CoreSession session)
            throws ClientException {

        byte[] xmlExportByteArray = exportXMLAsByteArray(doc, session);
        return new ByteArrayInputStream(xmlExportByteArray);
    }

    /**
     * {@inheritDoc}
     */
    public InputSource exportXMLAsInputSource(DocumentModel doc,
            CoreSession session) throws ClientException {

        InputStream xmlExportInputStream = exportXML(doc, session);
        return new InputSource(xmlExportInputStream);
    }

    /**
     * Export XML as byte array.
     * 
     * @param doc the doc
     * @param session the session
     * @return the byte[]
     * @throws ClientException the client exception
     */
    public final byte[] exportXMLAsByteArray(DocumentModel doc,
            CoreSession session) throws ClientException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DocumentWriter documentWriter = new XMLDocumentWriter(outputStream);
        DocumentReader documentReader = new TypedSingleDocumentReader(session,
                doc);

        DocumentPipe pipe = new DocumentPipeImpl();
        pipe.setReader(documentReader);
        pipe.setWriter(documentWriter);

        try {
            pipe.run();
        } catch (Exception e) {
            throw new ClientException(
                    "Error while trying to export the document to XML.", e);
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
