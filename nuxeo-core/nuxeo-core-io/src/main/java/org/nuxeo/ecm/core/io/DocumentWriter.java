/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: DocumentWriter.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io;

import java.io.IOException;
import java.util.Collection;

/**
 * A document writer.
 * <p>
 * This writer is designed to be accessible remotely (over a network).
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface DocumentWriter {

    /**
     * Writes a single document.
     *
     * @param doc the document to write
     * @return the translation map.
     * @throws IOException
     */
    DocumentTranslationMap write(ExportedDocument doc) throws IOException;

    /**
     * Writes an array of documents.
     *
     * @param docs the array to write
     * @return the translation map.
     * @throws IOException
     */
    DocumentTranslationMap write(ExportedDocument[] docs) throws IOException;

    /**
     * Writes documents from the given collection.
     *
     * @param docs the documents to write
     * @return the translation map.
     * @throws IOException
     */
    DocumentTranslationMap write(Collection<ExportedDocument> docs)
            throws IOException;

    /**
     * Closes the writer.
     */
    void close();

}
