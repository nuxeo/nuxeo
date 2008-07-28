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
 * $Id: DocumentReader.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io;

import java.io.IOException;

/**
 * A document reader. This reader is designed to be accessed remotely (over a
 * network).
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface DocumentReader {

    /**
     * Reads a single document.
     *
     * @return the document read or null if there are no more documents to read
     * @throws IOException
     */
    ExportedDocument read() throws IOException;

    /**
     * Reads next 'count' documents.
     *
     * @param count the number of documents to read
     * @return the array of read documents or null if there are no more
     *         documents to read
     * @throws IOException
     */
    ExportedDocument[] read(int count) throws IOException;

    /**
     * Closes the reader.
     */
    void close();

}
