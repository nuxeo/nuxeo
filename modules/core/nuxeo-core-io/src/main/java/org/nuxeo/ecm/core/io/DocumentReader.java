/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: DocumentReader.java 29029 2008-01-14 18:38:14Z ldoguin $
 */

package org.nuxeo.ecm.core.io;

import java.io.IOException;

/**
 * A document reader. This reader is designed to be accessed remotely (over a network).
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface DocumentReader {

    /**
     * Reads a single document.
     *
     * @return the document read or null if there are no more documents to read
     */
    ExportedDocument read() throws IOException;

    /**
     * Reads next 'count' documents.
     *
     * @param count the number of documents to read
     * @return the array of read documents or null if there are no more documents to read
     */
    ExportedDocument[] read(int count) throws IOException;

    /**
     * Closes the reader.
     */
    void close();

}
