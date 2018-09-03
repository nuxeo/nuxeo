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
import static org.nuxeo.wopi.Constants.ACCESS_TOKEN;
import static org.nuxeo.wopi.Constants.ACCESS_TOKEN_TTL;
import static org.nuxeo.wopi.Constants.FILE_CONTENT_PROPERTY;
import static org.nuxeo.wopi.Constants.FORM_URL;
import static org.nuxeo.wopi.Constants.WOPI_SRC;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

/**
 * @since 10.3
 */
public class WOPIServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String WORD_VIEW_URL = "https://word-view.officeapps-df.live.com/wv/wordviewerframe.aspx?";

    public static final String WORD_EDIT_URL = "https://word-edit.officeapps-df.live.com/we/wordeditorframe.aspx?";

    public static final String EXCEL_VIEW_URL = "https://excel.officeapps-df.live.com/x/_layouts/xlviewerinternal.aspx?";

    public static final String EXCEL_EDIT_URL = EXCEL_VIEW_URL + "edit=1&";

    public static final String POWERPOINT_VIEW_URL = "https://powerpoint.officeapps-df.live.com/p/PowerPointFrame.aspx?PowerPointView=ReadingView&";

    public static final String POWERPOINT_EDIT_URL = "https://powerpoint.officeapps-df.live.com/p/PowerPointFrame.aspx?PowerPointView=EditView&";

    public static final String WOPITEST_VIEW_URL = "https://onenote.officeapps-df.live.com/hosting/WopiTestFrame.aspx?";

    public static final Map<Pair<String, String>, String> ACTIONS_TO_URLS = new HashMap<>();

    static {
        ACTIONS_TO_URLS.put(Pair.of("view", "doc"), WORD_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "docm"), WORD_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "docx"), WORD_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "dot"), WORD_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "dotm"), WORD_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "docx"), WORD_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "odt"), WORD_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "rtf"), WORD_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("edit", "docm"), WORD_EDIT_URL);
        ACTIONS_TO_URLS.put(Pair.of("edit", "docx"), WORD_EDIT_URL);
        ACTIONS_TO_URLS.put(Pair.of("edit", "odt"), WORD_EDIT_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "csv"), EXCEL_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "ods"), EXCEL_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "xls"), EXCEL_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "xlsb"), EXCEL_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "xlsm"), EXCEL_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "xlsx"), EXCEL_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("edit", "ods"), EXCEL_EDIT_URL);
        ACTIONS_TO_URLS.put(Pair.of("edit", "xlsb"), EXCEL_EDIT_URL);
        ACTIONS_TO_URLS.put(Pair.of("edit", "xlsm"), EXCEL_EDIT_URL);
        ACTIONS_TO_URLS.put(Pair.of("edit", "xlsx"), EXCEL_EDIT_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "odp"), POWERPOINT_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "pot"), POWERPOINT_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "potm"), POWERPOINT_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "potx"), POWERPOINT_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "pps"), POWERPOINT_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "ppsm"), POWERPOINT_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "ppsx"), POWERPOINT_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "ppt"), POWERPOINT_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "pptm"), POWERPOINT_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("view", "pptx"), POWERPOINT_VIEW_URL);
        ACTIONS_TO_URLS.put(Pair.of("edit", "odp"), POWERPOINT_EDIT_URL);
        ACTIONS_TO_URLS.put(Pair.of("edit", "ppsx"), POWERPOINT_EDIT_URL);
        ACTIONS_TO_URLS.put(Pair.of("edit", "pptx"), POWERPOINT_EDIT_URL);
        // for testing
        ACTIONS_TO_URLS.put(Pair.of("view", "wopitest"), WOPITEST_VIEW_URL);
    }

    public static final String WOPI_JSP = "/wopi.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getPathInfo();
        if (path == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path: " + path);
            return;
        }
        // remove first /
        path = path.substring(1, path.length());
        String[] parts = path.split("/");
        int length = parts.length;
        if (length < 3) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path: " + path);
            return;
        }

        String action = parts[0];
        String repository = parts[1];
        String docId = parts[2];
        String xpath = parts.length == 4 ? parts[3] : FILE_CONTENT_PROPERTY;
        try (CloseableCoreSession session = CoreInstance.openCoreSession(repository)) {
            DocumentRef ref = new IdRef(docId);
            if (!session.exists(ref)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Document not found");
                return;
            }

            DocumentModel doc = session.getDocument(ref);
            Blob blob = getBlob(doc, xpath);
            if (blob == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No blob on document");
                return;
            }

            String extension = FilenameUtils.getExtension(blob.getFilename());
            String url = ACTIONS_TO_URLS.get(Pair.of(action, extension));
            if (url == null) {
                // TODO http code?
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Cannot open file with Office Online");
                return;
            }

            String token = Helpers.createJWTToken();
            request.setAttribute(ACCESS_TOKEN, token);
            request.setAttribute(ACCESS_TOKEN_TTL, Helpers.getJWTTokenExp(token));
            String baseURL = VirtualHostHelper.getBaseURL(request);
            String fileId = FileInfo.computeFileId(doc, xpath);
            String wopiSrc = URLEncoder.encode(String.format("%ssite/wopi/files/%s", baseURL, fileId), UTF_8.name());
            request.setAttribute(FORM_URL, url + WOPI_SRC + "=" + wopiSrc);
            RequestDispatcher requestDispatcher = request.getRequestDispatcher(WOPI_JSP);
            requestDispatcher.forward(request, response);
        }
    }

    protected Blob getBlob(DocumentModel doc, String xpath) {
        // TODO check cloud services blob provider?
        return (Blob) doc.getPropertyValue(xpath);
    }
}
