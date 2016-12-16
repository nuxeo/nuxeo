/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Estelle Giuly <egiuly@nuxeo.com>
 */
package org.nuxeo.ecm.platform.ui.web.download;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.io.download.DownloadHelper;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple download servlet used for big files that can not be downloaded from within the JSF context (because of
 * buffered ResponseWrapper).
 */
public class DownloadServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

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
        String baseUrl = VirtualHostHelper.getBaseURL(req);
        String path = requestURI.substring(context.length());
        DownloadService downloadService = Framework.getService(DownloadService.class);
        downloadService.handleDownload(req, resp, baseUrl, path);
    }

    /**
     * @deprecated since 9.1. It was defined for ClipboardActionsBean but it seems not to be used anymore.
     */
    @Deprecated
    protected void handleDownloadTemporaryZip(HttpServletRequest req, HttpServletResponse resp, String filePath)
            throws IOException {
        String[] pathParts = filePath.split("/");
        String tmpFileName = pathParts[0];
        File tmpZip = new File(System.getProperty("java.io.tmpdir") + "/" + tmpFileName);
        try {
            Blob zipBlob = Blobs.createBlob(tmpZip);
            DownloadService downloadService = Framework.getService(DownloadService.class);
            downloadService.downloadBlob(req, resp, null, null, zipBlob, "clipboard.zip", "clipboardZip");
        } finally {
            tmpZip.delete();
        }
    }

}
