/*
 * (C) Copyright 2006-2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Delprat
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.download;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Arrays;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

    public static final String NXBIGFILE_PREFIX = DownloadService.NXBIGFILE;

    public static final String NXDOWNLOADINFO_PREFIX = DownloadService.NXDOWNLOADINFO;

    public static final String NXBIGBLOB_PREFIX = DownloadService.NXBIGBLOB;

    public static final String NXBIGZIPFILE_PREFIX = DownloadService.NXBIGZIPFILE;

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
        if (requestURI.contains("/" + NXBIGFILE_PREFIX + "/")) {
            handleDownloadBlob(req, resp, requestURI);
        } else if (requestURI.contains("/" + NXDOWNLOADINFO_PREFIX + "/")) {
            handleGetDownloadInfo(req, resp, requestURI);
        } else if (requestURI.contains("/" + NXBIGZIPFILE_PREFIX + "/")) {
            // handle the download for a big zip created in the tmp directory;
            // the name of this zip is sent in the request
            handleDownloadTemporaryZip(req, resp, requestURI);
        } else if (requestURI.contains("/" + NXBIGBLOB_PREFIX + "/")) {
            // handle the download of a Blob referenced in HTTP Request or Session
            handleDownloadSessionBlob(req, resp, requestURI);
        }
    }

    // used by nxdropout.js
    protected void handleGetDownloadInfo(HttpServletRequest req, HttpServletResponse resp, String requestURI)
            throws IOException {
        String downloadUrl = requestURI.replace(NXDOWNLOADINFO_PREFIX, NXBIGFILE_PREFIX);
        downloadBlob(req, resp, downloadUrl, true);
    }

    // regular download from xpath or blobholder
    protected void handleDownloadBlob(HttpServletRequest req, HttpServletResponse resp, String requestURI)
            throws IOException {
        downloadBlob(req, resp, requestURI, false);
    }

    protected void downloadBlob(HttpServletRequest req, HttpServletResponse resp, String requestURI, boolean info)
            throws IOException {
        String urlPath = requestURI.replace(VirtualHostHelper.getContextPath(req) + "/" + NXBIGFILE_PREFIX + "/", "");
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
                    String result = blob.getMimeType() + ':' + URLEncoder.encode(blob.getFilename(), "UTF-8") + ':'
                            + VirtualHostHelper.getServerURL(req) + requestURI.substring(1);
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

    // used by DownloadFile operation
    protected void handleDownloadSessionBlob(HttpServletRequest req, HttpServletResponse resp, String requestURI)
            throws IOException {
        String blobId = requestURI.replace(VirtualHostHelper.getContextPath(req) + "/" + NXBIGBLOB_PREFIX + "/", "");
        Blob blob = (Blob) req.getAttribute(blobId);
        if (blob != null) {
            req.removeAttribute(blobId);
        } else {
            HttpSession session = req.getSession(false);
            if (session == null) {
                log.error("Unable to download blob " + blobId + " since the holding http session does not exist");
                return;
            }
            blob = (Blob) session.getAttribute(blobId);
            if (blob == null) {
                return;
            }
            session.removeAttribute(blobId);
        }
        DownloadService downloadService = Framework.getService(DownloadService.class);
        downloadService.downloadBlob(req, resp, null, null, blob, null, "DownloadFile");
    }

    // used by ClipboardActionsBean
    protected void handleDownloadTemporaryZip(HttpServletRequest req, HttpServletResponse resp, String requestURI)
            throws IOException {
        String filePath = requestURI.replace(VirtualHostHelper.getContextPath(req) + "/" + NXBIGZIPFILE_PREFIX + "/",
                "");
        String[] pathParts = filePath.split("/");
        String tmpFileName = pathParts[0];
        File tmpZip = new File(System.getProperty("java.io.tmpdir") + "/" + tmpFileName);
        try {
            Blob zipBlob = Blobs.createBlob(tmpZip);
            DownloadService downloadService = Framework.getService(DownloadService.class);
            downloadService.downloadBlob(req, resp, null, null, zipBlob, "clipboard.zip", "ZipExport");
        } finally {
            tmpZip.delete();
        }
    }

}
