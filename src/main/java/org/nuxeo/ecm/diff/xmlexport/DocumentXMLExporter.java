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
package org.nuxeo.ecm.diff.xmlexport;

import java.io.InputStream;
import java.io.Serializable;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.xml.sax.InputSource;

/**
 * Handles XML export of a document.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public interface DocumentXMLExporter extends Serializable {

    /**
     * Export document XML as an InputStream.
     * 
     * @param doc the doc
     * @param session the session
     * @return the input stream
     * @throws ClientException the client exception
     */
    InputStream exportXML(DocumentModel doc, CoreSession session)
            throws ClientException;

    /**
     * Export docupent xml as an InputSource.
     * 
     * @param doc the doc
     * @param session the session
     * @return the input source
     * @throws ClientException the client exception
     */
    InputSource exportXMLAsInputSource(DocumentModel doc, CoreSession session)
            throws ClientException;

    /**
     * Export document XML as a byte array.
     * 
     * @param doc the doc
     * @param session the session
     * @return the byte[]
     * @throws ClientException the client exception
     */
    byte[] exportXMLAsByteArray(DocumentModel doc, CoreSession session)
            throws ClientException;

}
