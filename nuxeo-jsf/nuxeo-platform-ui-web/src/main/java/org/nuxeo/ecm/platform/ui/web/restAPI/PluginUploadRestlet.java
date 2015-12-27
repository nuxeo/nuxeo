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

import static org.jboss.seam.ScopeType.EVENT;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import org.apache.commons.fileupload.FileUploadException;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.SimpleFileManager;
import org.nuxeo.ecm.platform.ui.web.util.FileUploadHelper;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

@Name("pluginUploadRestlet")
@Scope(EVENT)
public class PluginUploadRestlet extends BaseNuxeoRestlet implements Serializable {

    private static final long serialVersionUID = 1L;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    protected CoreSession documentManager;

    @In(create = true)
    protected transient SimpleFileManager FileManageActions;

    @Override
    public void handle(Request req, Response res) {
        String repo = (String) req.getAttributes().get("repo");
        String docid = (String) req.getAttributes().get("docid");
        String returnCode = "TRANSF_ERROR";
        String relativePath = "";
        List<String> segments = req.getResourceRef().getSegments();
        List<String> pathElements = segments.subList(5, segments.size());

        for (String pathElement : pathElements) {
            if (pathElement != null && !pathElement.trim().equals("")) {
                try {
                    relativePath = relativePath + '/' + URLDecoder.decode(pathElement, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }

        DocumentModel currentDocument;

        try {
            if (navigationContext.getCurrentServerLocation() == null) {
                // init context if needed
                navigationContext.setCurrentServerLocation(new RepositoryLocation(repo));
            }

            documentManager = navigationContext.getOrCreateDocumentManager();
            currentDocument = navigationContext.getCurrentDocument();

            if (currentDocument == null || !currentDocument.getRef().toString().equals(docid)) {
                // init context if needed
                currentDocument = documentManager.getDocument(new IdRef(docid));
                navigationContext.setCurrentDocument(currentDocument);
            }
        } catch (NuxeoException e) {
            handleError(res, e);
            return;
        }

        if (currentDocument != null) {
            List<Blob> blobs;
            try {
                blobs = FileUploadHelper.parseRequest(req);
            } catch (FileUploadException | IOException e) {
                handleError(res, e);
                return;
            }

            Blob blob = blobs.get(0);
            try {
                returnCode = FileManageActions.addBinaryFileFromPlugin(blob, blob.getFilename(), relativePath);
            } catch (NuxeoException e) {
                handleError(res, e);
                return;
            }
        }
        res.setEntity(returnCode, MediaType.TEXT_PLAIN);
    }

}
