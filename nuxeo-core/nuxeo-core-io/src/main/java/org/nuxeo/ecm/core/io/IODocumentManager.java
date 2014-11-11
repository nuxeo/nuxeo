/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: IODocumentManager.java 30413 2008-02-21 18:38:54Z sfermigier $
 */

package org.nuxeo.ecm.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.io.exceptions.ExportDocumentException;
import org.nuxeo.ecm.core.io.exceptions.ImportDocumentException;

/**
 * Defines functional interface that deals directly with documents import using
 * provided DocumentReader or InputStream as a source and DocumentWriter that
 * knows how the documents will be saved into the repository.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public interface IODocumentManager extends Serializable {

    DocumentTranslationMap importDocuments(InputStream in, String repo,
            DocumentRef root) throws ImportDocumentException, ClientException,
            IOException;

    DocumentTranslationMap importDocuments(InputStream in,
            DocumentWriter customDocWriter) throws ImportDocumentException;

    /**
     * @param customDocReader reader from the input stream
     * @param customDocWriter
     */
    DocumentTranslationMap importDocuments(DocumentReader customDocReader,
            DocumentWriter customDocWriter) throws ImportDocumentException;

    DocumentTranslationMap exportDocuments(OutputStream out, String repo,
            Collection<DocumentRef> sources, boolean recurse, String format)
            throws ExportDocumentException, ClientException;

    /**
     * Used in pair with importDocuments(... customDocWriter)
     */
    DocumentTranslationMap exportDocuments(OutputStream out,
            DocumentReader customDocReader, String format)
            throws ExportDocumentException;
}
