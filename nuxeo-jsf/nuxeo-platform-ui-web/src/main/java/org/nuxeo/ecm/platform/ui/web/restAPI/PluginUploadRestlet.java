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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import org.apache.commons.fileupload.FileUploadException;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.platform.filemanager.api.FileManager;
import org.nuxeo.ecm.platform.ui.web.util.FileUploadHelper;
import org.nuxeo.runtime.api.Framework;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CharacterSet;
import org.restlet.data.MediaType;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;

public class PluginUploadRestlet extends BaseNuxeoRestlet implements Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public void handle(Request req, Response res) {
        String repo = (String) req.getAttributes().get("repo");
        String docid = (String) req.getAttributes().get("docid");
        String returnCode = "TRANSF_ERROR";
        String relativePath = "";
        List<String> segments = req.getResourceRef().getSegments();
        int pos = segments.indexOf("restAPI") + 4;
        List<String> pathElements = segments.subList(pos, segments.size() - 1);
        String fileName = segments.get(segments.size() - 1);

        for (String pathElement : pathElements) {
            if (pathElement != null && !pathElement.trim().equals("")) {
                try {
                    relativePath = relativePath + '/' + URLDecoder.decode(pathElement, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }

        try (CloseableCoreSession session = CoreInstance.openCoreSession(repo)) {
            DocumentModel doc = session.getDocument(new IdRef(docid));
            DocumentModel folder = session.getDocument(new PathRef(doc.getPathAsString() + relativePath));
            List<Blob> blobs;
            try {
                blobs = FileUploadHelper.parseRequest(req);
            } catch (FileUploadException | IOException e) {
                handleError(res, e);
                return;
            }

            Blob blob = blobs.get(0);
            try {
                blob.setFilename(fileName);
                returnCode = addBinaryFileFromPlugin(blob, folder);
            } catch (NuxeoException e) {
                handleError(res, e);
                return;
            }
        } catch (NuxeoException | IOException e) {
            handleError(res, e);
            return;
        }

        Representation rep = new StringRepresentation(returnCode, MediaType.TEXT_PLAIN);
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
