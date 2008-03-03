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
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.core.io.impl.plugins.SingleDocumentReader;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentTreeWriter;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentWriter;
import org.restlet.data.MediaType;
import org.restlet.resource.OutputRepresentation;

/**
 * Facelet resource representation that calls a {@link DocumentPipe} using the
 * facelet's output stream for the document writer's output.
 *
 * @author Florent Guillaume
 */
public class ExportRepresentation extends OutputRepresentation {

    private static final Log log = LogFactory.getLog(ExportRepresentation.class);

    protected boolean exportAsTree = false;

    protected boolean exportAsZip = false;

    protected CoreSession documentManager;

    protected DocumentModel root;

    public ExportRepresentation(boolean exportAsTree, boolean exportAsZip,
            CoreSession documentManager, DocumentModel root) {
        super(exportAsZip ? MediaType.APPLICATION_ZIP : MediaType.TEXT_XML);
        this.exportAsTree = exportAsTree;
        this.exportAsZip = exportAsZip;
        this.documentManager = documentManager;
        this.root = root;
    }

    @Override
    public void write(OutputStream outputStream) throws IOException {
        DocumentReader documentReader = null;
        DocumentWriter documentWriter = null;
        try {
            DocumentPipe pipe;
            if (exportAsTree) {
                pipe = new DocumentPipeImpl(10);
            } else {
                pipe = new DocumentPipeImpl();
            }

            if (exportAsTree) {
                documentReader = new DocumentTreeReader(documentManager, root,
                        false);
                if (!exportAsZip) {
                    ((DocumentTreeReader) documentReader).setInlineBlobs(true);
                }
            } else {
                documentReader = new SingleDocumentReader(documentManager, root);
            }
            pipe.setReader(documentReader);

            if (exportAsZip) {
                documentWriter = new NuxeoArchiveWriter(outputStream);
            } else {
                if (exportAsTree) {
                    documentWriter = new XMLDocumentTreeWriter(outputStream);
                } else {
                    documentWriter = new XMLDocumentWriter(outputStream);
                }
            }
            pipe.setWriter(documentWriter);

            pipe.run();

        } catch (Exception e) {
            log.error("Error during export", e);
            throw new IOException(); // stupid IOException has no cause
        } finally {
            if (documentReader != null) {
                documentReader.close();
            }
            if (documentWriter != null) {
                documentWriter.close();
            }
        }
    }

}
