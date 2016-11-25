/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.ui.web.download;

import static org.nuxeo.ecm.core.io.download.DownloadService.NXBIGBLOB;
import static org.nuxeo.ecm.core.io.download.DownloadService.NXBIGZIPFILE;
import static org.nuxeo.ecm.core.io.download.DownloadService.NXDOWNLOADINFO;
import static org.nuxeo.ecm.core.io.download.DownloadService.NXFILE;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Arrays;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.download.DownloadHelper;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Simple download servlet used for big files that can not be downloaded from within the JSF context (because of
 * buffered ResponseWrapper).
 */
public class DownloadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DownloadServlet.class);

    /** @deprecated since 7.4, use nxfile instead */
    @Deprecated
    public static final String NXBIGFILE = DownloadService.NXBIGFILE;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            handleDownload(req, resp);
        } catch (IOException ioe) {
            DownloadHelper.handleClientDisconnect(ioe);
        }
    }

    protected void handleDownload(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestURI;
        try {
            requestURI = new URI(req.getRequestURI()).getPath();
        } catch (URISyntaxException e) {
            requestURI = req.getRequestURI();
        }
        // remove context
        String context = VirtualHostHelper.getContextPath(req) + "/";
        if (!requestURI.startsWith(context)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid URL syntax");
            return;
        }
        String localURI = requestURI.substring(context.length());
        // find what to do
        int slash = localURI.indexOf('/');
        if (slash < 0) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid URL syntax");
            return;
        }
        String what = localURI.substring(0, slash);
        String path = localURI.substring(slash + 1);
        switch (what) {
        case NXFILE:
        case NXBIGFILE:
            handleDownload(req, resp, path, false);
            break;
        case NXDOWNLOADINFO:
            // used by nxdropout.js
            handleDownload(req, resp, path, true);
            break;
        case NXBIGZIPFILE:
        case NXBIGBLOB:
            Framework.getService(DownloadService.class).downloadBlob(req, resp, path, "download");
            break;
        default:
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid URL syntax");
        }
    }

    protected void handleDownload(HttpServletRequest req, HttpServletResponse resp, String urlPath, boolean info)
            throws IOException {
        String[] parts = urlPath.split("/");
        if (parts.length < 2) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid URL syntax");
            return;
        }
        String repositoryName = parts[0];
        String docId = parts[1];
        boolean tx = false;
        try {
            if (!TransactionHelper.isTransactionActive()) {
                // Manually start and stop a transaction around repository access to be able to release transactional
                // resources without waiting for the download that can take a long time (longer than the transaction
                // timeout) especially if the client or the connection is slow.
                tx = TransactionHelper.startTransaction();
            }
            try (CoreSession session = CoreInstance.openCoreSession(repositoryName)) {
                DocumentRef docRef = new IdRef(docId);
                if (!session.exists(docRef)) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No Blob found");
                    return;
                }
                DocumentModel doc = session.getDocument(docRef);
                Pair<String, String> pair = parsePath(urlPath);
                String xpath = pair.getLeft();
                String filename = pair.getRight();
                DownloadService downloadService = Framework.getService(DownloadService.class);
                if (info) {
                    Blob blob = downloadService.resolveBlob(doc, xpath);
                    if (blob == null) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No blob found");
                        return;
                    }
                    String downloadUrl = VirtualHostHelper.getBaseURL(req)
                            + downloadService.getDownloadUrl(doc, xpath, filename);
                    String result = blob.getMimeType() + ':' + URLEncoder.encode(blob.getFilename(), "UTF-8") + ':'
                            + downloadUrl;
                    resp.setContentType("text/plain");
                    resp.getWriter().write(result);
                    resp.getWriter().flush();

                } else {
                    downloadService.downloadBlob(req, resp, doc, xpath, null, filename, "download");
                }
            }
        } catch (NuxeoException e) {
            if (tx) {
                TransactionHelper.setTransactionRollbackOnly();
            }
            throw new IOException(e);
        } finally {
            if (tx) {
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
    }

    // first two parts are repository name and doc id, already parsed
    protected static Pair<String, String> parsePath(String urlPath) {
        String[] parts = urlPath.split("/");
        int length = parts.length;
        String xpath;
        String filename;
        if (length == 2) {
            xpath = DownloadService.BLOBHOLDER_0;
            filename = null;
        } else if (length == 3) {
            xpath = parts[2];
            filename = null;
        } else {
            xpath = StringUtils.join(Arrays.asList(parts).subList(2, length - 1), "/");
            filename = parts[length - 1];
        }
        return Pair.of(xpath, filename);
    }

    // used by ClipboardActionsBean
    protected void handleDownloadTemporaryZip(HttpServletRequest req, HttpServletResponse resp, String filePath)
            throws IOException {
        String[] pathParts = filePath.split("/");
        String tmpFileName = pathParts[0];
        File tmpZip = new File(Environment.getDefault().getTemp(), tmpFileName);
        try {
            Blob zipBlob = Blobs.createBlob(tmpZip);
            DownloadService downloadService = Framework.getService(DownloadService.class);
            downloadService.downloadBlob(req, resp, null, null, zipBlob, "clipboard.zip", "clipboardZip");
        } finally {
            tmpZip.delete();
        }
    }

}
