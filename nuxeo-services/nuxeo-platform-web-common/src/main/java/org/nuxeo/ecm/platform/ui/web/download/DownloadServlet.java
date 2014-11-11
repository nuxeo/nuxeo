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
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.core.storage.sql.coremodel.SQLBlob;
import org.nuxeo.ecm.platform.web.common.ServletHelper;
import org.nuxeo.ecm.platform.web.common.exceptionhandling.ExceptionHelper;
import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.BufferingServletOutputStream;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Simple download servlet used for big files that can not be downloaded from
 * within the JSF context (because of buffered ResponseWrapper).
 *
 * @author tiry
 */
public class DownloadServlet extends HttpServlet {

    private static final String NXDOWNLOADINFO_PREFIX = "nxdownloadinfo";

    private static final String NXBIGFILE_PREFIX = "nxbigfile";

    protected static final int BUFFER_SIZE = 1024 * 512;

    protected static final int MIN_BUFFER_SIZE = 1024 * 64;

    protected static final Blob BLOB_NOT_FOUND = new StringBlob("404");

    private static final long serialVersionUID = 986876871L;

    private static final Log log = LogFactory.getLog(DownloadServlet.class);

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

        if (requestURI.contains("/" + NXBIGFILE_PREFIX + "/")) {
            handleDownloadSingleDocument(req, resp, requestURI);
        } else if (requestURI.contains("/" + NXDOWNLOADINFO_PREFIX + "/")) {
            handleGetDownloadInfo(req, resp, requestURI);
        } else if (requestURI.contains("/nxbigzipfile/")) {
            // handle the download for a big zip created in the tmp directory;
            // the name of this zip is sent in the request
            handleDownloadTemporaryZip(req, resp, requestURI);
        }
    }

    private Blob resolveBlob(HttpServletRequest req, HttpServletResponse resp,
            String requestURI) throws ServletException {

        String filePath = requestURI.replace(
                VirtualHostHelper.getContextPath(req) + "/" + NXBIGFILE_PREFIX
                        + "/", "");
        String[] pathParts = filePath.split("/");

        String repoName = pathParts[0];
        String docId = pathParts[1];

        String fieldPath = "blobholder:0";
        if (pathParts.length > 2) {
            fieldPath = pathParts[2];
        }

        String fileName = null;
        if (pathParts.length > 3) {
            fileName = pathParts[3];
        }

        // alternate decoding
        String[] alternatePath = filePath.split(docId);
        if (alternatePath.length > 1) {
            String completePath = alternatePath[1];
            int idx = completePath.lastIndexOf('/');
            if (idx > 0) {
                fieldPath = completePath.substring(1, idx);
                fileName = completePath.substring(idx + 1);
            }
        }

        CoreSession session = null;
        boolean tx = false;
        try {
            if (!TransactionHelper.isTransactionActive()) {
                // manually start and stop a transaction around repository
                // access to be able to release transactional resources without
                // waiting for the download that can take a long time (longer
                // than the transaction timeout) especially if the client or the
                // connection is slow.
                tx = TransactionHelper.startTransaction();
            }
            session = getCoreSession(repoName);

            DocumentModel doc = session.getDocument(new IdRef(docId));
            Blob blob = null;
            if (fieldPath != null) {
                // Hack for Flash Url wich doesn't support ':' char
                fieldPath = fieldPath.replace(';', ':');
                // BlobHolder urls
                if (fieldPath.startsWith("blobholder")) {
                    BlobHolder bh = doc.getAdapter(BlobHolder.class);
                    if (bh == null) {
                        return null;
                    }
                    String bhPath = fieldPath.replace("blobholder:", "");
                    if ("".equals(bhPath) || "0".equals(bhPath)) {
                        blob = bh.getBlob();
                    } else {
                        int idxbh = Integer.parseInt(bhPath);
                        blob = bh.getBlobs().get(idxbh);
                    }
                } else {
                    try {
                        blob = (Blob) doc.getPropertyValue(fieldPath);
                    } catch (PropertyNotFoundException e) {
                        log.debug(e.getMessage());
                        return BLOB_NOT_FOUND;
                    } catch (ClientException e) {
                        log.debug(e.getMessage());
                        return null;
                    }
                }
            }

            if (fileName != null && !fileName.isEmpty()) {
                blob.setFilename(fileName);
            }
            return blob;
        } catch (Exception e) {
            if (tx) {
                TransactionHelper.setTransactionRollbackOnly();
            }
            throw new ServletException(e);
        } finally {
            if (session != null) {
                CoreInstance.getInstance().close(session);
            }
            if (tx) {
                TransactionHelper.commitOrRollbackTransaction();
            }
        }
    }

    private void handleGetDownloadInfo(HttpServletRequest req,
            HttpServletResponse resp, String requestURI)
            throws ServletException {

        String downloadUrl = requestURI.replace(NXDOWNLOADINFO_PREFIX,
                NXBIGFILE_PREFIX);
        Blob blob = resolveBlob(req, resp, downloadUrl);
        if (!isBlobFound(blob, resp)) {
            return;
        }

        StringBuffer sb = new StringBuffer();

        resp.setContentType("text/plain");
        try {
            sb.append(blob.getMimeType());
            sb.append(":");
            sb.append(URLEncoder.encode(blob.getFilename(), "UTF-8"));
            sb.append(":");
            sb.append(VirtualHostHelper.getServerURL(req));
            sb.append(downloadUrl.substring(1));
            resp.getWriter().write(sb.toString());
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    private boolean isBlobFound(Blob blob, HttpServletResponse resp)
            throws ServletException {
        if (blob == null) {
            try {
                resp.sendError(HttpServletResponse.SC_NO_CONTENT,
                        "No Blob found");
                return false;
            } catch (IOException e) {
                throw new ServletException(e);
            }
        } else if (BLOB_NOT_FOUND.equals(blob)) {
            try {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND,
                        "No Blob found");
                return false;
            } catch (IOException e) {
                throw new ServletException(e);
            }
        }
        return true;
    }

    private void handleDownloadSingleDocument(HttpServletRequest req,
            HttpServletResponse resp, String requestURI)
            throws ServletException {
        Blob blob = resolveBlob(req, resp, requestURI);
        if (!isBlobFound(blob, resp)) {
            return;
        }

        try {
            downloadBlob(req, resp, blob, null);
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    void downloadBlob(HttpServletRequest req, HttpServletResponse resp,
            Blob blob, String fileName) throws IOException, ServletException {
        InputStream in = blob.getStream();
        OutputStream out = resp.getOutputStream();
        try {

            String digest = null;
            if (blob instanceof SQLBlob) {
                digest = ((SQLBlob) blob).getBinary().getDigest();
            }

            String previousToken = req.getHeader("If-None-Match");
            if (previousToken != null && previousToken.equals(digest)) {
                resp.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            } else {
                resp.setHeader("ETag", digest);
                if (fileName == null || fileName.length() == 0) {
                    if (blob.getFilename() != null
                            && blob.getFilename().length() > 0) {
                        fileName = blob.getFilename();
                    } else {
                        fileName = "file";
                    }
                }

                resp.setHeader("Content-Disposition",
                        ServletHelper.getRFC2231ContentDisposition(req,
                                fileName));
                resp.setContentType(blob.getMimeType());

                long fileSize = blob.getLength();
                if (fileSize > 0) {
                    String range = req.getHeader("Range");
                    ByteRange byteRange = null;
                    if (range != null) {
                        try {
                            byteRange = parseRange(range, fileSize);
                        } catch (ClientException e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                    if (byteRange != null) {
                        resp.setHeader("Accept-Ranges", "bytes");
                        resp.setHeader("Content-Range",
                                "bytes " + byteRange.getStart() + "-"
                                        + byteRange.getEnd() + "/" + fileSize);
                        long length = byteRange.getLength();
                        if (length < Integer.MAX_VALUE) {
                            resp.setContentLength((int) length);
                        }
                        resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                        writeStream(in, out, byteRange);
                    } else {
                        if (fileSize < Integer.MAX_VALUE) {
                            resp.setContentLength((int) fileSize);
                        }
                        writeStream(in, out, new ByteRange(0, fileSize - 1));
                    }
                }
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
        if (ExceptionHelper.isClientAbortError(ioe)) {
            log.debug("Client disconnected: " + ioe.getMessage());
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
        FileBlob zipBlob = new FileBlob(tmpZip);
        try {
            downloadBlob(req, resp, zipBlob, "clipboard.zip");
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            tmpZip.delete();
        }
    }

    public static void writeStream(InputStream in, OutputStream out,
            ByteRange range) throws IOException {
        BufferingServletOutputStream.stopBuffering(out);
        byte[] buffer = new byte[BUFFER_SIZE];
        long read;
        long offset = range.getStart();
        in.skip(offset);
        while (offset <= range.getEnd() && (read = in.read(buffer)) != -1) {
            read = Math.min(read, range.getEnd() - offset + 1);
            out.write(buffer, 0, (int) read);
            out.flush();
            offset += read;
        }

    }

    public static ByteRange parseRange(String range, long fileSize)
            throws ClientException {
        // Do no support multiple ranges
        if (!range.startsWith("bytes=") || range.indexOf(',') >= 0) {
            throw new ClientException("Cannot parse range : " + range);
        }
        int sepIndex = range.indexOf('-', 6);
        if (sepIndex < 0) {
            throw new ClientException("Cannot parse range : " + range);
        }
        String start = range.substring(6, sepIndex).trim();
        String end = range.substring(sepIndex + 1).trim();
        long rangeStart = 0;
        long rangeEnd = fileSize - 1;
        if (start.isEmpty()) {
            if (end.isEmpty()) {
                throw new ClientException("Cannot parse range : " + range);
            }
            rangeStart = fileSize - Integer.parseInt(end);
            if (rangeStart < 0) {
                rangeStart = 0;
            }
        } else {
            rangeStart = Integer.parseInt(start);
            if (!end.isEmpty()) {
                rangeEnd = Integer.parseInt(end);
            }
        }
        if (rangeStart > rangeEnd) {
            throw new ClientException("Cannot parse range : " + range);
        }

        return new ByteRange(rangeStart, rangeEnd);
    }

    public static class ByteRange {

        private long start;

        private long end;

        public ByteRange(long rangeStart, long rangeEnd) {
            start = rangeStart;
            end = rangeEnd;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

        public long getLength() {
            return end - start + 1;
        }
    }

}
