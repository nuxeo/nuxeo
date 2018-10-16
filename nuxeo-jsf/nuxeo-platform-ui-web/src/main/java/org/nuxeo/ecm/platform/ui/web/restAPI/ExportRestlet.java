/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Florent Guillaume
 *
 * $Id: ExportRestlet.java 30251 2008-02-18 19:17:33Z fguillaume $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.SecurityConstants;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentTreeReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.core.io.impl.plugins.SingleDocumentReader;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentTreeWriter;
import org.nuxeo.ecm.core.io.impl.plugins.XMLDocumentWriter;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.representation.Representation;

/**
 * @deprecated since 7.2. Exports are now exposed directly as renditions on the document. Exports can be generated
 *             through the {@code ExportDocument} operation. See NXP-16585.
 */
@Deprecated
public class ExportRestlet extends BaseStatelessNuxeoRestlet implements Serializable {

    private static final long serialVersionUID = 7831287875548588711L;

    @Override
    protected void doHandleStatelessRequest(Request req, Response res) {
        boolean exportAsTree;
        boolean exportAsZip;
        String action = req.getResourceRef().getSegments().get(4);
        if (action.equals("exportTree")) {
            exportAsTree = true;
            exportAsZip = true;
        } else {
            // "export", "exportSingle"
            exportAsTree = false;
            String format = req.getResourceRef().getQueryAsForm().getFirstValue("format");
            if (format != null) {
                format = format.toLowerCase();
            } else {
                format = "xml";
            }
            exportAsZip = "zip".equals(format);
        }

        String repo = (String) req.getAttributes().get("repo");
        if (repo == null || repo.equals("*")) {
            handleError(res, "you must specify a repository");
            return;
        }

        DocumentModel root;
        String docid = (String) req.getAttributes().get("docid");
        boolean needUnrestricted = false;

        try {
            boolean init = initRepository(res, repo);
            if (!init) {
                handleError(res, "Unable to init repository");
                return;
            }
            if (docid == null || docid.equals("*")) {
                root = session.getRootDocument();
            } else if (session.hasPermission(new IdRef(docid), SecurityConstants.READ)) {
                root = session.getDocument(new IdRef(docid));
            } else {
                UnrestrictedVersionExporter runner = new UnrestrictedVersionExporter(session, docid);
                runner.runUnrestricted();
                root = runner.root;
                needUnrestricted = true;

                // if user can't read version, export is authorized
                // if he can at least read a proxy pointing to this version
                if (!root.isVersion()) {
                    throw new DocumentSecurityException("Not enough rights to export " + root.getPathAsString());
                }
                DocumentModelList docs = session.getProxies(root.getRef(), null);
                boolean hasReadableProxy = false;
                for (DocumentModel doc : docs) {
                    if (session.hasPermission(doc.getRef(), SecurityConstants.READ)) {
                        hasReadableProxy = true;
                        break;
                    }
                }
                if (!hasReadableProxy) {
                    throw new DocumentSecurityException(
                            "Current user doesn't have access to any proxy pointing to version "
                                    + root.getPathAsString());
                }
            }
        } catch (NuxeoException e) {
            handleError(res, e);
            return;
        }

        if (exportAsZip) {
            // set the content disposition and file name
            String FILENAME = "export.zip";

            // use the Facelets APIs to set a new header
            Map<String, Object> attributes = res.getAttributes();
            Form headers = (Form) attributes.get(HeaderConstants.ATTRIBUTE_HEADERS);
            if (headers == null) {
                headers = new Form();
            }
            headers.add("Content-Disposition", String.format("attachment; filename=\"%s\";", FILENAME));
            attributes.put(HeaderConstants.ATTRIBUTE_HEADERS, headers);
        }

        MediaType mediaType = exportAsZip ? MediaType.APPLICATION_ZIP : MediaType.TEXT_XML;
        Representation entity = makeRepresentation(mediaType, root, exportAsTree, exportAsZip, needUnrestricted);

        res.setEntity(entity);
        if (mediaType == MediaType.TEXT_XML) {
            res.getEntity().setCharacterSet(CharacterSet.UTF_8);
        }
    }

    protected Representation makeRepresentation(MediaType mediaType, DocumentModel root, final boolean exportAsTree,
            final boolean exportAsZip, final boolean isUnrestricted) {

        return new ExportRepresentation(mediaType, root, isUnrestricted) {

            @Override
            protected DocumentPipe makePipe() {
                if (exportAsTree) {
                    return new DocumentPipeImpl(10);
                } else {
                    return new DocumentPipeImpl();
                }
            }

            @Override
            protected DocumentReader makeDocumentReader(CoreSession documentManager, DocumentModel root)
                    {
                DocumentReader documentReader;
                if (exportAsTree) {
                    documentReader = new DocumentTreeReader(documentManager, root, false);
                    if (!exportAsZip) {
                        ((DocumentTreeReader) documentReader).setInlineBlobs(true);
                    }
                } else {
                    documentReader = new SingleDocumentReader(documentManager, root);
                }
                return documentReader;
            }

            @Override
            protected DocumentWriter makeDocumentWriter(OutputStream outputStream) throws IOException {
                DocumentWriter documentWriter;
                if (exportAsZip) {
                    documentWriter = new NuxeoArchiveWriter(outputStream);
                } else {
                    if (exportAsTree) {
                        documentWriter = new XMLDocumentTreeWriter(outputStream);
                    } else {
                        documentWriter = new XMLDocumentWriter(outputStream);
                    }
                }
                return documentWriter;
            }
        };
    }

    protected static class UnrestrictedVersionExporter extends UnrestrictedSessionRunner {

        private final String docid;

        public DocumentModel root;

        protected UnrestrictedVersionExporter(CoreSession session, String docId) {
            super(session);
            docid = docId;
        }

        @Override
        public void run() {
            root = session.getDocument(new IdRef(docid));
        }

    }

}
