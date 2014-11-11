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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.core.io;

import java.io.InputStream;
import java.io.Serializable;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.xml.sax.InputSource;

/**
 * Handles the XML export of a document.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public interface DocumentXMLExporter extends Serializable {

    /**
     * Exports a document to XML as an {@link InputStream}.
     *
     * @param doc the document
     * @param session the core session
     * @return the input stream
     * @throws ClientException if an error occurs while exporting the document
     *             to XML
     */
    InputStream exportXML(DocumentModel doc, CoreSession session)
            throws ClientException;

    /**
     * Exports a document to XML as an {@link InputSource}.
     *
     * @param doc the document
     * @param session the core session
     * @return the input source
     * @throws ClientException if an error occurs while exporting the document
     *             to XML
     */
    InputSource exportXMLAsInputSource(DocumentModel doc, CoreSession session)
            throws ClientException;

    /**
     * Exports a document to XML as a byte array.
     *
     * @param doc the document
     * @param session the core session
     * @return the byte array
     * @throws ClientException if an error occurs while exporting the document
     *             to XML
     */
    byte[] exportXMLAsByteArray(DocumentModel doc, CoreSession session)
            throws ClientException;
}
