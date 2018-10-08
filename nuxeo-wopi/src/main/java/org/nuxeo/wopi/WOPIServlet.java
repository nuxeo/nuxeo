/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 *     Thomas Roger
 */

package org.nuxeo.wopi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.nuxeo.wopi.Constants.ACCESS_TOKEN_ATTRIBUTE;
import static org.nuxeo.wopi.Constants.ACCESS_TOKEN_TTL_ATTRIBUTE;
import static org.nuxeo.wopi.Constants.FILES_ENDPOINT_PATH;
import static org.nuxeo.wopi.Constants.FORM_URL;
import static org.nuxeo.wopi.Constants.WOPI_JSP;
import static org.nuxeo.wopi.Constants.WOPI_SRC;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 10.3
 */
public class WOPIServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if (path == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Null path");
            return;
        }
        // remove first /
        path = path.substring(1);
        String[] parts = path.split("/");
        int length = parts.length;
        if (length < 4) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path: " + path);
            return;
        }

        WOPIService wopiService = Framework.getService(WOPIService.class);
        if (!wopiService.isEnabled()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "WOPI not enabled");
            return;
        }

        String action = parts[0];
        String repository = parts[1];
        String docId = parts[2];
        String xpath = String.join("/", Arrays.asList(parts).subList(3, length));
        try (CloseableCoreSession session = CoreInstance.openCoreSession(repository)) {
            DocumentRef ref = new IdRef(docId);
            if (!session.exists(ref)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Document not found");
                return;
            }

            DocumentModel doc = session.getDocument(ref);
            Blob blob = Helpers.getEditableBlob(doc, xpath);
            if (blob == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No editable blob on document");
                return;
            }

            String actionURL = wopiService.getActionURL(blob, action);
            if (actionURL == null) {
                // TODO http code?
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Cannot open file with Office Online");
                return;
            }

            String token = Helpers.createJWTToken();
            request.setAttribute(ACCESS_TOKEN_ATTRIBUTE, token);
            request.setAttribute(ACCESS_TOKEN_TTL_ATTRIBUTE, Helpers.getJWTTokenExp(token));
            String baseURL = VirtualHostHelper.getBaseURL(request);
            String fileId = FileInfo.computeFileId(doc, xpath);
            String wopiSrc = URLEncoder.encode(String.format("%s%s%s", baseURL, FILES_ENDPOINT_PATH, fileId),
                    UTF_8.name());
            request.setAttribute(FORM_URL, String.format("%s%s=%s", actionURL, WOPI_SRC, wopiSrc));
            RequestDispatcher requestDispatcher = request.getRequestDispatcher(WOPI_JSP);
            requestDispatcher.forward(request, response);
        }
    }

}
