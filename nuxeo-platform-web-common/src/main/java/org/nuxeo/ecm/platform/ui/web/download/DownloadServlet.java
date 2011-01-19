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

package org.nuxeo.ecm.platform.ui.web.download;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.RFC2231;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple download servlet used for big files that can not be downloaded from
 * within the JSF context (because of buffered ResponseWrapper).
 *
 * @author tiry
 */
public class DownloadServlet extends HttpServlet {

    protected static final int BUFFER_SIZE = 1024 * 512;

    protected static final int MIN_BUFFER_SIZE = 1024 * 64;

    private static final long serialVersionUID = 986876871L;

    public static final Log log = LogFactory.getLog(DownloadServlet.class);

    private static CoreSession getCoreSession(String repoName) throws Exception {
        RepositoryManager rm = Framework.getService(RepositoryManager.class);
        Repository repo = rm.getRepository(repoName);
        if (repo == null) {
            throw new ClientException("Unable to get " + repoName
                    + " repository");
        }
        return repo.open();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        String requestURI;
        try {
            requestURI = new URI(req.getRequestURI()).getPath();
        } catch (URISyntaxException e1) {
            requestURI = req.getRequestURI();
        }

        if (requestURI.contains("/nxbigfile/")) {
            handleDownloadSingleDocument(req, resp, requestURI);
        }
        if (requestURI.contains("/nxbigzipfile/")) {
            // handle the download for a big zip created in the tmp directory;
            // the name of this zip is sent in the request
            handleDownloadTemporaryZip(req, resp, requestURI);
        }
    }

    private void handleDownloadSingleDocument(HttpServletRequest req,
            HttpServletResponse resp, String requestURI) throws
            ServletException {
        String filePath = requestURI.replace(
                VirtualHostHelper.getContextPath(req) + "/nxbigfile/", "");
        String[] pathParts = filePath.split("/");

        String repoName = pathParts[0];
        String docId = pathParts[1];
        String fieldPath = pathParts[2];
        String fileName = pathParts[3];

        String completePath = filePath.split(docId)[1];
        int idx = completePath.lastIndexOf('/');
        if (idx > 0) {
            fieldPath = completePath.substring(0, idx);
            fileName = completePath.substring(idx + 1);
        }

        CoreSession session = null;
        try {
            session = getCoreSession(repoName);

            DocumentModel doc = session.getDocument(new IdRef(docId));
            Blob blob;
            if (fieldPath != null) {
                // Hack for Flash Url wich doesn't support ':' char
                fieldPath = fieldPath.replace(';', ':');
                // BlobHolder urls
                if (fieldPath.startsWith("/blobholder")) {
                    BlobHolder bh = doc.getAdapter(BlobHolder.class);
                    if (bh == null) {
                        return;
                    }
                    String bhPath = fieldPath.replace("/blobholder:", "");
                    if ("".equals(bhPath) || "0".equals(bhPath)) {
                        blob = bh.getBlob();
                    } else {
                        int idxbh = Integer.parseInt(bhPath);
                        blob = bh.getBlobs().get(idxbh);
                    }
                } else {
                    blob = (Blob) DocumentModelUtils.getPropertyValue(doc,
                            DocumentModelUtils.decodePropertyName(fieldPath));
                    if (blob == null) {
                        // maybe it's a complex property
                        blob = (Blob) DocumentModelUtils.getComplexPropertyValue(
                                doc, fieldPath);
                    }
                }
            } else {
                return;
            }
            downloadBlob(req, resp, blob, fileName);
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            if (session != null) {
                CoreInstance.getInstance().close(session);
            }
        }
    }

    private void downloadBlob(HttpServletRequest req, HttpServletResponse resp,
            Blob blob, String fileName) throws IOException, ServletException {
        InputStream in = null;
        try {

            if (fileName == null || fileName.length() == 0) {
                if (blob.getFilename() != null
                        && blob.getFilename().length() > 0) {
                    fileName = blob.getFilename();
                } else {
                    fileName = "file";
                }
            }
            boolean inline = req.getParameter("inline") != null;
            String userAgent = req.getHeader("User-Agent");
            String contentDisposition = RFC2231.encodeContentDisposition(
                    fileName, inline, userAgent);
            resp.setHeader("Content-Disposition", contentDisposition);
            resp.setContentType(blob.getMimeType());

            long fileSize = blob.getLength();
            if (fileSize > 0) {
                resp.setContentLength((int) fileSize);
            }

            OutputStream out = resp.getOutputStream();
            in = blob.getStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                out.flush();
            }
        } catch (IOException ioe) {
            handleClientDisconnect(ioe);
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            if (resp != null) {
                try {
                    resp.flushBuffer();
                } catch (IOException ioe) {
                    handleClientDisconnect(ioe);
                }
            }
            if (in != null) {
                in.close();
            }
        }
    }

    public void handleClientDisconnect(IOException ioe) throws IOException {
        // handle all IOException that are ClientAbortException by looking at
        // their class name since the package name is not the same for
        // jboss, glassfish, tomcat and jetty and we don't want to add
        // implementation specific build dependencies to this project
        if ("ClientAbortException".equals(ioe.getClass().getSimpleName())) {
            // ignore client disconnections that can happen if the client choose
            // to interrupt the download abruptly, just log the error at the
            // debug level for developers if necessary
            log.debug(ioe, ioe);
        } else {
            // this is a real unexpected problem, let the traditional error
            // management handle this case
            throw ioe;
        }
    }

    private void handleDownloadTemporaryZip(HttpServletRequest req,
            HttpServletResponse resp, String requestURI) throws IOException,
            ServletException {
        String filePath = requestURI.replace(
                VirtualHostHelper.getContextPath(req) + "/nxbigzipfile/", "");
        String[] pathParts = filePath.split("/");
        String tmpFileName = pathParts[0];
        File tmpZip = new File(System.getProperty("java.io.tmpdir") + "/"
                + tmpFileName);
        try {
            FileBlob zipBlob = new FileBlob(tmpZip);
            downloadBlob(req, resp, zipBlob, "clipboard.zip");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
}
