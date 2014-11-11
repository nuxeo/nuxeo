/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
