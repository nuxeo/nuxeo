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
    DocumentTranslationMap write(Collection<ExportedDocument> docs) throws IOException;

    /**
     * Closes the writer.
     */
    void close();

}
