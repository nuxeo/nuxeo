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
package org.nuxeo.ecm.core.io;

import java.io.InputStream;
import java.io.Serializable;

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
     */
    InputStream exportXML(DocumentModel doc, CoreSession session);

    /**
     * Exports a document to XML as an {@link InputSource}.
     *
     * @param doc the document
     * @param session the core session
     * @return the input source
     */
    InputSource exportXMLAsInputSource(DocumentModel doc, CoreSession session);

    /**
     * Exports a document to XML as a byte array.
     *
     * @param doc the document
     * @param session the core session
     * @return the byte array
     */
    byte[] exportXMLAsByteArray(DocumentModel doc, CoreSession session);
}
