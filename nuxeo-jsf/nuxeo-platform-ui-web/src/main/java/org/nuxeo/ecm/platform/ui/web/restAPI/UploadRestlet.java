/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMDocumentFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.ui.web.util.FileUploadHelper;
import org.nuxeo.runtime.api.Framework;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

/**
 * Restlet to import files as nuxeo documents using the pluggable FileManager service. This restlet is mainly used for
 * desktop integration with drag and drop browser plugins.
 *
 * @author tdelprat
 */
public class UploadRestlet extends BaseNuxeoRestlet implements Serializable {

    private static final Log log = LogFactory.getLog(UploadRestlet.class);

    private static final long serialVersionUID = -7858792615823015193L;

    @Override
    public void handle(Request req, Response res) {
        logDeprecation();
        String repo = (String) req.getAttributes().get("repo");
        String docid = (String) req.getAttributes().get("docid");
        String fileName = (String) req.getAttributes().get("filename");

        DOMDocumentFactory domFactory = new DOMDocumentFactory();
        DOMDocument result = (DOMDocument) domFactory.createDocument();

        DocumentModel targetContainer;
        try (CloseableCoreSession session = CoreInstance.openCoreSession(repo)) {
            try {
                targetContainer = session.getDocument(new IdRef(docid));
            } catch (NuxeoException e) {
                handleError(res, e);
                return;
            }

            List<Blob> blobs = null;
            try {
                blobs = FileUploadHelper.parseRequest(req);
            } catch (FileUploadException | IOException e) {
                handleError(res, e);
                return;
            }

            if (!FileUploadHelper.isMultipartRequest(req)) {
                // mono import
                String outcome;
                try {
                    Blob inputBlob = blobs.get(0);
                    inputBlob.setFilename(fileName);
                    outcome = addBinaryFileFromPlugin(inputBlob, targetContainer);
                } catch (NuxeoException | IOException e) {
                    outcome = "ERROR : " + e.getMessage();
                }
                result.addElement("upload").setText(outcome);
            } else {
                // multiple file upload
                Element uploads = result.addElement("uploads");
                for (Blob blob : blobs) {
                    String outcome;
                    try {
                        outcome = addBinaryFileFromPlugin(blob, targetContainer);
                    } catch (NuxeoException | IOException e) {
                        log.error("error importing " + blob.getFilename() + ": " + e.getMessage(), e);
                        outcome = "ERROR : " + e.getMessage();
                    }
                    uploads.addElement("upload").setText(outcome);
                }
            }
        }
        Representation rep = new StringRepresentation(result.asXML(), MediaType.APPLICATION_XML);
        rep.setCharacterSet(CharacterSet.UTF_8);
        res.setEntity(rep);
    }

    protected String addBinaryFileFromPlugin(Blob blob, DocumentModel folder) throws IOException {
        FileManager fileManager = Framework.getService(FileManager.class);
        DocumentModel doc = fileManager.createDocumentFromBlob(folder.getCoreSession(), blob, folder.getPathAsString(),
                true, blob.getFilename());
        return doc.getName();
    }

}
