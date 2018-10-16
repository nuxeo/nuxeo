/*
 * (C) Copyright 2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *
 * $Id: ExportRepresentation.java 30251 2008-02-18 19:17:33Z fguillaume $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.restlet.data.MediaType;
import org.restlet.representation.OutputRepresentation;

/**
 * Facelet resource representation that calls a {@link DocumentPipe} using the facelet's output stream for the document
 * writer's output.
 * <p>
 * This abstract method must be subclassed to implement {@link #makePipe}, {@link #makeDocumentReader} and
 * {@link #makeDocumentWriter}.
 *
 * @author Florent Guillaume
 */
public abstract class ExportRepresentation extends OutputRepresentation {

    private static final Log log = LogFactory.getLog(ExportRepresentation.class);

    protected final String repositoryName;

    protected final String rootId;

    protected final boolean isUnrestricted;

    protected ExportRepresentation(MediaType mediaType, DocumentModel root) {
        this(mediaType, root, false);
    }

    protected ExportRepresentation(MediaType mediaType, DocumentModel root, boolean unrestricted) {
        super(mediaType);
        repositoryName = root.getRepositoryName();
        rootId = root.getId();
        isUnrestricted = unrestricted;
    }

    /**
     * Create a {@link DocumentPipe} adequate for the number of documents needed by the export.
     *
     * @return the document pipe.
     */
    protected abstract DocumentPipe makePipe();

    /**
     * Create a {@link DocumentReader} for the export.
     *
     * @param documentManager a session
     * @param root the root of the export
     * @return the document reader
     */
    protected abstract DocumentReader makeDocumentReader(CoreSession documentManager, DocumentModel root);

    /**
     * Create a {@link DocumentWriter} for the export.
     *
     * @param outputStream the stream to use
     * @return the document writer
     */
    protected abstract DocumentWriter makeDocumentWriter(OutputStream outputStream) throws IOException;

    @Override
    public void write(OutputStream outputStream) throws IOException {
        CloseableCoreSession session;
        if (isUnrestricted) {
            session = CoreInstance.openCoreSessionSystem(repositoryName);
        } else {
            session = CoreInstance.openCoreSession(repositoryName);
        }
        try {
            DocumentReader documentReader = null;
            DocumentWriter documentWriter = null;
            try {
                DocumentModel root = session.getDocument(new IdRef(rootId));
                documentReader = makeDocumentReader(session, root);
                documentWriter = makeDocumentWriter(outputStream);
                DocumentPipe pipe = makePipe();
                pipe.setReader(documentReader);
                pipe.setWriter(documentWriter);
                pipe.run();
            } finally {
                if (documentReader != null) {
                    documentReader.close();
                }
                if (documentWriter != null) {
                    documentWriter.close();
                }
            }
        } finally {
            session.close();
        }
    }

}
