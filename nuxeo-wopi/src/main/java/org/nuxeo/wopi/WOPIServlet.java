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
import static org.nuxeo.wopi.Constants.FILES_ENDPOINT_PATH;
import static org.nuxeo.wopi.Constants.FORM_URL;
import static org.nuxeo.wopi.Constants.WOPI_SRC;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
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

    public static final String WORD_APP_NAME = "Word";

    public static final String WORD_VIEW_URL = "https://word-view.officeapps-df.live.com/wv/wordviewerframe.aspx?";

    public static final String WORD_EDIT_URL = "https://word-edit.officeapps-df.live.com/we/wordeditorframe.aspx?";

    public static final String EXCEL_APP_NAME = "Excel";

    public static final String EXCEL_VIEW_URL = "https://excel.officeapps-df.live.com/x/_layouts/xlviewerinternal.aspx?";

    public static final String EXCEL_EDIT_URL = EXCEL_VIEW_URL + "edit=1&";

    public static final String POWERPOINT_APP_NAME = "PowerPoint";

    public static final String POWERPOINT_VIEW_URL = "https://powerpoint.officeapps-df.live.com/p/PowerPointFrame.aspx?PowerPointView=ReadingView&";

    public static final String POWERPOINT_EDIT_URL = "https://powerpoint.officeapps-df.live.com/p/PowerPointFrame.aspx?PowerPointView=EditView&";

    public static final String WOPITEST_VIEW_URL = "https://onenote.officeapps-df.live.com/hosting/WopiTestFrame.aspx?";

    protected static final Map<Pair<String, String>, Pair<String, String>> ACTIONS_TO_URLS = new HashMap<>();

    static {
        ACTIONS_TO_URLS.put(Pair.of("view", "doc"), Pair.of(WORD_APP_NAME, WORD_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "docm"), Pair.of(WORD_APP_NAME, WORD_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "docx"), Pair.of(WORD_APP_NAME, WORD_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "dot"), Pair.of(WORD_APP_NAME, WORD_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "dotm"), Pair.of(WORD_APP_NAME, WORD_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "docx"), Pair.of(WORD_APP_NAME, WORD_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "odt"), Pair.of(WORD_APP_NAME, WORD_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "rtf"), Pair.of(WORD_APP_NAME, WORD_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("edit", "docm"), Pair.of(WORD_APP_NAME, WORD_EDIT_URL));
        ACTIONS_TO_URLS.put(Pair.of("edit", "docx"), Pair.of(WORD_APP_NAME, WORD_EDIT_URL));
        ACTIONS_TO_URLS.put(Pair.of("edit", "odt"), Pair.of(WORD_APP_NAME, WORD_EDIT_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "csv"), Pair.of(EXCEL_APP_NAME, EXCEL_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "ods"), Pair.of(EXCEL_APP_NAME, EXCEL_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "xls"), Pair.of(EXCEL_APP_NAME, EXCEL_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "xlsb"), Pair.of(EXCEL_APP_NAME, EXCEL_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "xlsm"), Pair.of(EXCEL_APP_NAME, EXCEL_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "xlsx"), Pair.of(EXCEL_APP_NAME, EXCEL_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("edit", "ods"), Pair.of(EXCEL_APP_NAME, EXCEL_EDIT_URL));
        ACTIONS_TO_URLS.put(Pair.of("edit", "xlsb"), Pair.of(EXCEL_APP_NAME, EXCEL_EDIT_URL));
        ACTIONS_TO_URLS.put(Pair.of("edit", "xlsm"), Pair.of(EXCEL_APP_NAME, EXCEL_EDIT_URL));
        ACTIONS_TO_URLS.put(Pair.of("edit", "xlsx"), Pair.of(EXCEL_APP_NAME, EXCEL_EDIT_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "odp"), Pair.of(POWERPOINT_APP_NAME, POWERPOINT_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "pot"), Pair.of(POWERPOINT_APP_NAME, POWERPOINT_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "potm"), Pair.of(POWERPOINT_APP_NAME, POWERPOINT_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "potx"), Pair.of(POWERPOINT_APP_NAME, POWERPOINT_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "pps"), Pair.of(POWERPOINT_APP_NAME, POWERPOINT_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "ppsm"), Pair.of(POWERPOINT_APP_NAME, POWERPOINT_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "ppsx"), Pair.of(POWERPOINT_APP_NAME, POWERPOINT_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "ppt"), Pair.of(POWERPOINT_APP_NAME, POWERPOINT_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "pptm"), Pair.of(POWERPOINT_APP_NAME, POWERPOINT_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("view", "pptx"), Pair.of(POWERPOINT_APP_NAME, POWERPOINT_VIEW_URL));
        ACTIONS_TO_URLS.put(Pair.of("edit", "odp"), Pair.of(POWERPOINT_APP_NAME, POWERPOINT_EDIT_URL));
        ACTIONS_TO_URLS.put(Pair.of("edit", "ppsx"), Pair.of(POWERPOINT_APP_NAME, POWERPOINT_EDIT_URL));
        ACTIONS_TO_URLS.put(Pair.of("edit", "pptx"), Pair.of(POWERPOINT_APP_NAME, POWERPOINT_EDIT_URL));
        // for testing
        ACTIONS_TO_URLS.put(Pair.of("view", "wopitest"), Pair.of("WopiTest", WOPITEST_VIEW_URL));
    }

    public static final String WOPI_JSP = "/wopi.jsp";

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

            String extension = FilenameUtils.getExtension(blob.getFilename());
            Pair<String, String> pair = ACTIONS_TO_URLS.get(Pair.of(action, extension));
            if (pair == null) {
                // TODO http code?
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Cannot open file with Office Online");
                return;
            }
            String url = pair.getRight();

            String token = Helpers.createJWTToken();
            request.setAttribute(ACCESS_TOKEN, token);
            request.setAttribute(ACCESS_TOKEN_TTL, Helpers.getJWTTokenExp(token));
            String baseURL = VirtualHostHelper.getBaseURL(request);
            String fileId = FileInfo.computeFileId(doc, xpath);
            String wopiSrc = URLEncoder.encode(String.format("%s%s%s", baseURL, FILES_ENDPOINT_PATH, fileId),
                    UTF_8.name());
            request.setAttribute(FORM_URL, url + WOPI_SRC + "=" + wopiSrc);
            RequestDispatcher requestDispatcher = request.getRequestDispatcher(WOPI_JSP);
            requestDispatcher.forward(request, response);
        }
    }

}
