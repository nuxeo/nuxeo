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
 * $Id: IODocumentManager.java 30413 2008-02-21 18:38:54Z sfermigier $
 */

package org.nuxeo.ecm.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collection;

import org.nuxeo.ecm.core.api.DocumentRef;

/**
 * Defines functional interface that deals directly with documents import using provided DocumentReader or InputStream
 * as a source and DocumentWriter that knows how the documents will be saved into the repository.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public interface IODocumentManager extends Serializable {

    DocumentTranslationMap importDocuments(InputStream in, String repo, DocumentRef root) throws IOException;

    DocumentTranslationMap importDocuments(InputStream in, DocumentWriter customDocWriter);

    /**
     * @param customDocReader reader from the input stream
     * @param customDocWriter
     */
    DocumentTranslationMap importDocuments(DocumentReader customDocReader, DocumentWriter customDocWriter);

    DocumentTranslationMap exportDocuments(OutputStream out, String repo, Collection<DocumentRef> sources,
            boolean recurse, String format);

    /**
     * Used in pair with importDocuments(... customDocWriter)
     */
    DocumentTranslationMap exportDocuments(OutputStream out, DocumentReader customDocReader, String format);
}
