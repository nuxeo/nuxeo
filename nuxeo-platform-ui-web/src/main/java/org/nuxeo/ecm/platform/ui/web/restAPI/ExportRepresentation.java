/*
 * (C) Copyright 2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 *
 * $Id: ExportRepresentation.java 30251 2008-02-18 19:17:33Z fguillaume $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.restlet.data.MediaType;
import org.restlet.resource.OutputRepresentation;

/**
 * Facelet resource representation that calls a {@link DocumentPipe} using the
 * facelet's output stream for the document writer's output.
 * <p>
 * This abstract method must be subclassed to implement {@link #makePipe},
 * {@link #makeDocumentReader} and {@link #makeDocumentWriter}.
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

    protected ExportRepresentation(MediaType mediaType, DocumentModel root,
            boolean unrestricted) {
        super(mediaType);
        repositoryName = root.getRepositoryName();
        rootId = root.getId();
        isUnrestricted = unrestricted;
    }

    /**
     * Create a {@link DocumentPipe} adequate for the number of documents needed
     * by the export.
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
     * @throws Exception
     */
    protected abstract DocumentReader makeDocumentReader(
            CoreSession documentManager, DocumentModel root) throws Exception;

    /**
     * Create a {@link DocumentWriter} for the export.
     *
     * @param outputStream the stream to use
     * @return the document writer
     * @throws Exception
     */
    protected abstract DocumentWriter makeDocumentWriter(
            OutputStream outputStream) throws Exception;

    @Override
    public void write(OutputStream outputStream) throws IOException {
        CoreSession session;
        try {
            if (isUnrestricted) {
                session = CoreInstance.openCoreSessionSystem(repositoryName);
            } else {
                session = CoreInstance.openCoreSession(repositoryName);
            }
        } catch (ClientException e) {
            throw new IOException(e);
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
            } catch (Exception e) {
                log.error("Error during export", e);
                throw new IOException("Error during export", e);
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
