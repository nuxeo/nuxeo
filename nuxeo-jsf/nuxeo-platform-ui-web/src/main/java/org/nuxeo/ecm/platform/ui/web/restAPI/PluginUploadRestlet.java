/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.restAPI;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.api.SimpleFileManager;
import org.nuxeo.ecm.platform.ui.web.util.FileUploadHelper;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

@Name("pluginUploadRestlet")
@Scope(EVENT)
public class PluginUploadRestlet extends BaseNuxeoRestlet implements
        Serializable {

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
                    relativePath = relativePath + '/'
                            + URLDecoder.decode(pathElement, "utf-8");
                } catch (UnsupportedEncodingException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }

        DocumentModel currentDocument;

        try {
            if (navigationContext.getCurrentServerLocation() == null) {
                // init context if needed
                navigationContext.setCurrentServerLocation(new RepositoryLocation(
                        repo));
            }

            documentManager = navigationContext.getOrCreateDocumentManager();
            currentDocument = navigationContext.getCurrentDocument();

            if (currentDocument == null
                    || !currentDocument.getRef().toString().equals(docid)) {
                // init context if needed
                currentDocument = documentManager.getDocument(new IdRef(docid));
                navigationContext.setCurrentDocument(currentDocument);
            }
        } catch (ClientException e) {
            handleError(res, e);
            return;
        }

        if (currentDocument != null) {
            List<Blob> blobs;
            try {
                blobs = FileUploadHelper.parseRequest(req);

            } catch (Exception e) {
                handleError(res, e);
                return;
            }

            Blob blob = blobs.get(0);
            try {
                returnCode = FileManageActions.addBinaryFileFromPlugin(blob,
                        blob.getFilename(), relativePath);
            } catch (ClientException e) {
                handleError(res, e);
                return;
            }
        }
        res.setEntity(returnCode, MediaType.TEXT_PLAIN);
    }

}
