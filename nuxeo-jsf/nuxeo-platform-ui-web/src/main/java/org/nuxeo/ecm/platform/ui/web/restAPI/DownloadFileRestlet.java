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

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.io.download.DownloadService.DownloadContext;
import org.nuxeo.ecm.platform.ui.web.tag.fn.LiveEditConstants;
import org.nuxeo.runtime.api.Framework;
import org.restlet.Request;
import org.restlet.Response;

/**
 * Restlet to help LiveEdit clients download the blob content of a document
 *
 * @author Sun Tan <stan@nuxeo.com>
 * @author Olivier Grisel <ogrisel@nuxeo.com>
 */
public class DownloadFileRestlet extends BaseNuxeoRestlet implements LiveEditConstants, Serializable {

    private static final long serialVersionUID = -2163290273836947871L;

    @Override
    public void handle(Request req, Response res) {
        logDeprecation();
        HttpServletRequest request = getHttpRequest(req);
        HttpServletResponse response = getHttpResponse(res);

        String repo = (String) req.getAttributes().get("repo");
        if (repo == null || repo.equals("*")) {
            handleError(res, "you must specify a repository");
            return;
        }

        DocumentModel dm;
        try (CloseableCoreSession documentManager = CoreInstance.openCoreSession(repo)) {
            String docid = (String) req.getAttributes().get("docid");
            if (docid != null) {
                dm = documentManager.getDocument(new IdRef(docid));
            } else {
                handleError(res, "you must specify a valid document IdRef");
                return;
            }

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
            DownloadContext context = DownloadContext.builder(request, response)
                                                     .doc(dm)
                                                     .xpath(xpath)
                                                     .blob(blob)
                                                     .filename(filename)
                                                     .reason("download")
                                                     .blobTransferer(
                                                             byteRange -> setEntityToBlobOutput(blob, byteRange, res))
                                                     .build();
            Framework.getService(DownloadService.class).downloadBlob(context);
        } catch (IOException | NuxeoException e) {
            handleError(res, e);
        }
    }

}
