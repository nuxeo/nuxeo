/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.io.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.SystemPrincipal;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * This service allows the download of blobs to a HTTP response.
 *
 * @since 7.3
 */
public class DownloadServiceImpl extends DefaultComponent implements DownloadService {

    private static final Log log = LogFactory.getLog(DownloadServiceImpl.class);

    protected static final int DOWNLOAD_BUFFER_SIZE = 1024 * 512;

    private static final String NUXEO_VIRTUAL_HOST = "nuxeo-virtual-host";

    private static final String VH_PARAM = "nuxeo.virtual.host";

    private static final String FORCE_NO_CACHE_ON_MSIE = "org.nuxeo.download.force.nocache.msie";

    @Override
    public String getDownloadUrl(DocumentModel doc, String xpath, String filename) {
        return getDownloadUrl(doc.getRepositoryName(), doc.getId(), xpath, filename);
    }

    @Override
    public String getDownloadUrl(String repositoryName, String docId, String xpath, String filename) {
        StringBuilder sb = new StringBuilder();
        sb.append(NXFILE);
        sb.append("/");
        sb.append(repositoryName);
        sb.append("/");
        sb.append(docId);
        if (xpath != null) {
            sb.append("/");
            sb.append(xpath);
            if (filename != null) {
                sb.append("/");
                sb.append(URIUtils.quoteURIPathComponent(filename, true));
            }
        }
        return sb.toString();
    }

    @Override
    public void downloadBlob(HttpServletRequest request, HttpServletResponse response, DocumentModel doc, String xpath,
            Blob blob, String filename, String reason) throws IOException {
        downloadBlob(request, response, doc, xpath, blob, filename, reason, null);
    }

    @Override
    public void downloadBlob(HttpServletRequest request, HttpServletResponse response, DocumentModel doc, String xpath,
            Blob blob, String filename, String reason, Map<String, Serializable> extendedInfos) throws IOException {
        if (blob == null) {
            if (doc == null || xpath == null) {
                throw new IllegalArgumentException("No blob or doc xpath");
            }
            blob = resolveBlob(doc, xpath);
            if (blob == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No blob found");
                return;
            }
        }

        // check Blob Manager download link
        BlobManager blobManager = Framework.getService(BlobManager.class);
        URI uri = blobManager == null ? null : blobManager.getURI(blob, UsageHint.DOWNLOAD, request);
        if (uri != null) {
            try {
                Map<String,Serializable> ei = new HashMap<>();
                if (extendedInfos != null) {
                    ei.putAll(extendedInfos);
                }
                ei.put("redirect", uri.toString());
                logDownload(doc, xpath, filename, reason, ei);
                response.sendRedirect(uri.toString());
            } catch (IOException ioe) {
                DownloadHelper.handleClientDisconnect(ioe);
            }
            return;
        }

        try (InputStream in = blob.getStream()) {
            String etag = '"' + blob.getDigest() + '"'; // with quotes per RFC7232 2.3
            response.setHeader("ETag", etag); // re-send even on SC_NOT_MODIFIED
            addCacheControlHeaders(request, response);

            String ifNoneMatch = request.getHeader("If-None-Match");
            if (ifNoneMatch != null) {
                boolean match = false;
                if (ifNoneMatch.equals("*")) {
                    match = true;
                } else {
                    for (String previousEtag : StringUtils.split(ifNoneMatch, ", ")) {
                        if (previousEtag.equals(etag)) {
                            match = true;
                            break;
                        }
                    }
                }
                if (match) {
                    String method = request.getMethod();
                    if (method.equals("GET") || method.equals("HEAD")) {
                        response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
                    } else {
                        // per RFC7232 3.2
                        response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
                    }
                    return;
                }
            }

            // regular processing

            if (StringUtils.isBlank(filename)) {
                filename = StringUtils.defaultIfBlank(blob.getFilename(), "file");
            }
            response.setHeader("Content-Disposition", DownloadHelper.getRFC2231ContentDisposition(request, filename));
            response.setContentType(blob.getMimeType());
            if (blob.getEncoding() != null) {
                response.setCharacterEncoding(blob.getEncoding());
            }

            long length = blob.getLength();
            response.setHeader("Accept-Ranges", "bytes");
            String range = request.getHeader("Range");
            ByteRange byteRange;
            if (StringUtils.isBlank(range)) {
                byteRange = null;
            } else {
                byteRange = DownloadHelper.parseRange(range, length);
                if (byteRange == null) {
                    log.error("Invalid byte range received: " + range);
                }
            }
            long contentLength = byteRange == null ? length : byteRange.getLength();
            if (contentLength < Integer.MAX_VALUE) {
                response.setContentLength((int) contentLength);
            }

            // (not ours to close)
            @SuppressWarnings("resource")
            OutputStream out = response.getOutputStream();

            logDownload(doc, xpath, filename, reason, extendedInfos);

            BufferingServletOutputStream.stopBuffering(out);
            if (byteRange == null) {
                IOUtils.copy(in, out);
            } else {
                response.setHeader("Content-Range",
                        "bytes " + byteRange.getStart() + "-" + byteRange.getEnd() + "/" + length);
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                IOUtils.copyLarge(in, out, byteRange.getStart(), byteRange.getLength());
            }
            out.flush();
            response.flushBuffer();
        } catch (IOException ioe) {
            DownloadHelper.handleClientDisconnect(ioe);
        }
    }

    @Override
    public Blob resolveBlob(DocumentModel doc, String xpath) {
        // Hack for Flash Url wich doesn't support ':' char
        xpath = xpath.replace(';', ':');
        Blob blob;
        if (xpath.startsWith(BLOBHOLDER_PREFIX)) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh == null) {
                return null;
            }
            String index = xpath.substring(BLOBHOLDER_PREFIX.length());
            if ("".equals(index) || "0".equals(index)) {
                blob = bh.getBlob();
            } else {
                try {
                    blob = bh.getBlobs().get(Integer.parseInt(index));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        } else {
            try {
                blob = (Blob) doc.getPropertyValue(xpath);
            } catch (PropertyNotFoundException e) {
                log.debug(e.getMessage());
                return null;
            }
        }
        return blob;
    }

    /**
     * Internet Explorer file downloads over SSL do not work with certain HTTP cache control headers
     * <p>
     * See http://support.microsoft.com/kb/323308/
     * <p>
     * What is not mentioned in the above Knowledge Base is that "Pragma: no-cache" also breaks download in MSIE over
     * SSL
     */
    protected void addCacheControlHeaders(HttpServletRequest request, HttpServletResponse response) {
        String userAgent = request.getHeader("User-Agent");
        boolean secure = request.isSecure();
        if (!secure) {
            String nvh = request.getHeader(NUXEO_VIRTUAL_HOST);
            if (nvh == null) {
                nvh = Framework.getProperty(VH_PARAM);
            }
            if (nvh != null) {
                secure = nvh.startsWith("https");
            }
        }
        String cacheControl;
        if (userAgent != null && userAgent.contains("MSIE") && (secure || forceNoCacheOnMSIE())) {
            cacheControl = "max-age=15, must-revalidate";
        } else {
            cacheControl = "private, must-revalidate";
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);
        }
        log.debug("Setting Cache-Control: " + cacheControl);
        response.setHeader("Cache-Control", cacheControl);
    }

    protected static boolean forceNoCacheOnMSIE() {
        // see NXP-7759
        return Framework.isBooleanPropertyTrue(FORCE_NO_CACHE_ON_MSIE);
    }

    @Override
    public void logDownload(DocumentModel doc, String xpath, String filename, String reason,
            Map<String, Serializable> extendedInfos) {
        EventService eventService = Framework.getService(EventService.class);
        if (eventService == null) {
            return;
        }
        EventContext ctx;
        if (doc != null) {
            @SuppressWarnings("resource")
            CoreSession session = doc.getCoreSession();
            Principal principal = session == null ? getPrincipal() : session.getPrincipal();
            ctx = new DocumentEventContext(session, principal, doc);
            ctx.setProperty(CoreEventConstants.REPOSITORY_NAME, doc.getRepositoryName());
            ctx.setProperty(CoreEventConstants.SESSION_ID, doc.getSessionId());
        } else {
            ctx = new EventContextImpl(null, getPrincipal());
        }
        Map<String, Serializable> map = new HashMap<>();
        map.put("blobXPath", xpath);
        map.put("blobFilename", filename);
        map.put("downloadReason", reason);
        if (extendedInfos != null) {
            map.putAll(extendedInfos);
        }
        ctx.setProperty("extendedInfos", (Serializable) map);
        ctx.setProperty("comment", filename);
        Event event = ctx.newEvent(EVENT_NAME);
        eventService.fireEvent(event);
    }

    protected static NuxeoPrincipal getPrincipal() {
        NuxeoPrincipal principal = ClientLoginModule.getCurrentPrincipal();
        if (principal == null) {
            if (!Framework.isTestModeSet()) {
                throw new NuxeoException("Missing security context, login() not done");
            }
            principal = new SystemPrincipal(null);
        }
        return principal;
    }

}
