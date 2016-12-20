/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.ui.web.restAPI;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.nuxeo.runtime.api.Framework;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * Restlet to help LiveEdit clients download the blob content of a document
 *
 * @author Sun Tan <stan@nuxeo.com>
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
@Name("downloadFileRestlet")
@Scope(EVENT)
public class DownloadFileRestlet extends BaseNuxeoRestlet implements LiveEditConstants, Serializable {

    private static final long serialVersionUID = -2163290273836947871L;

    @In(create = true)
    protected transient NavigationContext navigationContext;

    protected CoreSession documentManager;

    @Override
    public void handle(Request req, Response res) {
        HttpServletRequest request = getHttpRequest(req);
        HttpServletResponse response = getHttpResponse(res);

        String repo = (String) req.getAttributes().get("repo");
        if (repo == null || repo.equals("*")) {
            handleError(res, "you must specify a repository");
            return;
        }

        DocumentModel dm;
        try {
            navigationContext.setCurrentServerLocation(new RepositoryLocation(repo));
            documentManager = navigationContext.getOrCreateDocumentManager();
            String docid = (String) req.getAttributes().get("docid");
            if (docid != null) {
                dm = documentManager.getDocument(new IdRef(docid));
            } else {
                handleError(res, "you must specify a valid document IdRef");
                return;
            }
        } catch (NuxeoException e) {
            handleError(res, e);
            return;
        }

        try {
            String blobPropertyName = getQueryParamValue(req, BLOB_PROPERTY_NAME, null);
            String filenamePropertyName = getQueryParamValue(req, FILENAME_PROPERTY_NAME, null);
            Blob blob;
            String xpath;
            String filename;
            if (blobPropertyName != null && filenamePropertyName != null) {
                filename = (String) dm.getPropertyValue(filenamePropertyName);
                blob = (Blob) dm.getPropertyValue(blobPropertyName);
                xpath = blobPropertyName;
            } else {
                String schemaName = getQueryParamValue(req, SCHEMA, DEFAULT_SCHEMA);
                String blobFieldName = getQueryParamValue(req, BLOB_FIELD, DEFAULT_BLOB_FIELD);
                blob = (Blob) dm.getProperty(schemaName, blobFieldName);
                filename = StringUtils.defaultIfBlank(blob.getFilename(), "file");
                xpath = schemaName + ':' + blobFieldName;
            }

            // trigger download
            String reason = "download";
            Map<String, Serializable> extendedInfos = null;
            DownloadService downloadService = Framework.getService(DownloadService.class);
            downloadService.downloadBlob(request, response, dm, xpath, blob, filename, reason, extendedInfos, null,
                    byteRange -> setEntityToBlobOutput(blob, byteRange, res));
        } catch (IOException | NuxeoException e) {
            handleError(res, e);
        }
    }

}
