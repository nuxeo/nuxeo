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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.common.utils.RFC2231;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.utils.DocumentModelUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Simple download servlet used for big files that can not be downloaded from
 * within the JSF context (because of buffered ResponseWrapper).
 *
 * @author tiry
 */
public class DownloadServlet extends HttpServlet {

    protected static final int BUFFER_SIZE = 1024 * 512;

    private static final long serialVersionUID = 986876871L;

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

        String requestURI = req.getRequestURI();
        String filePath = requestURI.replace("/nuxeo/nxbigfile/", "");
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
        InputStream in = null;
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
                    if (bh==null) {
                        return;
                    }
                    String bhPath = fieldPath.replace("/blobholder:", "");
                    if ("".equals(bhPath) || "0".equals(bhPath)) {
                        blob= bh.getBlob();
                    } else {
                        int idxbh = Integer.parseInt(bhPath);
                        blob = bh.getBlobs().get(idxbh);
                    }
                }
                else {
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

            if (fileName == null || fileName.length() == 0) {
                fileName = "file";
            }
            boolean inline = req.getParameter("inline") != null;
            String userAgent = req.getHeader("User-Agent");
            String flash = req.getHeader("x-flash-version");
            if (flash != null && !flash.equals("")) {
                String contentDisposition = RFC2231.encodeContentDisposition(
                        fileName, inline, userAgent);
                resp.setHeader("Content-Disposition", contentDisposition);
            }
            resp.setContentType(blob.getMimeType());

            OutputStream out = resp.getOutputStream();
            in = blob.getStream();
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
                out.flush();
            }
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            if (resp != null) {
                resp.flushBuffer();
            }
            if (session != null) {
                CoreInstance.getInstance().close(session);
            }
            if (in != null) {
                in.close();
            }
        }
    }

}
