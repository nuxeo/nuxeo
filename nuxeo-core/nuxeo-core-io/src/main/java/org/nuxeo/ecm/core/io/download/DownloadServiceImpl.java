/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Estelle Giuly <egiuly@nuxeo.com>
 */
package org.nuxeo.ecm.core.io.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
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
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;
import org.nuxeo.runtime.transaction.TransactionHelper;

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

    private static final String XP = "permissions";

    private static final String RUN_FUNCTION = "run";

    protected static enum Action {DOWNLOAD, DOWNLOAD_FROM_DOC, INFO};

    private static final Pattern FILENAME_SANITIZATION_REGEX = Pattern.compile(";\\w+=.*");

    private DownloadPermissionRegistry registry = new DownloadPermissionRegistry();

    private ScriptEngineManager scriptEngineManager;

    public static class DownloadPermissionRegistry extends SimpleContributionRegistry<DownloadPermissionDescriptor> {

        @Override
        public String getContributionId(DownloadPermissionDescriptor contrib) {
            return contrib.getName();
        }

        @Override
        public boolean isSupportingMerge() {
            return true;
        }

        @Override
        public DownloadPermissionDescriptor clone(DownloadPermissionDescriptor orig) {
            return new DownloadPermissionDescriptor(orig);
        }

        @Override
        public void merge(DownloadPermissionDescriptor src, DownloadPermissionDescriptor dst) {
            dst.merge(src);
        }

        public DownloadPermissionDescriptor getDownloadPermissionDescriptor(String id) {
            return getCurrentContribution(id);
        }

        /** Returns descriptors sorted by name. */
        public List<DownloadPermissionDescriptor> getDownloadPermissionDescriptors() {
            List<DownloadPermissionDescriptor> descriptors = new ArrayList<>(currentContribs.values());
            Collections.sort(descriptors);
            return descriptors;
        }
    }

    public DownloadServiceImpl() {
        scriptEngineManager = new ScriptEngineManager();
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (!XP.equals(extensionPoint)) {
            throw new UnsupportedOperationException(extensionPoint);
        }
        DownloadPermissionDescriptor descriptor = (DownloadPermissionDescriptor) contribution;
        registry.addContribution(descriptor);
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        DownloadPermissionDescriptor descriptor = (DownloadPermissionDescriptor) contribution;
        registry.removeContribution(descriptor);
    }

    /**
     * {@inheritDoc}
     *
     * Multipart download are not yet supported. You can only provide
     * a blob singleton at this time.
     */
    @Override
    public String storeBlobs(List<Blob> blobs) {
        if (blobs.size() > 1) {
            throw new IllegalArgumentException("multipart download not yet implemented");
        }
        TransientStore ts = Framework.getService(TransientStoreService.class).getStore("download");
        String storeKey = UUID.randomUUID().toString();
        ts.putBlobs(storeKey, blobs);
        return storeKey;
    }



    @Override
    public String getDownloadUrl(DocumentModel doc, String xpath, String filename) {
        return getDownloadUrl(doc.getRepositoryName(), doc.getId(), xpath, filename);
    }

    @Override
    public String getDownloadUrl(String repositoryName, String docId, String xpath, String filename) {
        StringBuilder sb = new StringBuilder();
        sb.append(NXFILE);
        sb.append("/").append(repositoryName);
        sb.append("/").append(docId);
        if (xpath != null) {
            sb.append("/").append(xpath);
            if (filename != null) {
                // make sure filename doesn't contain path separators
                filename = getSanitizedFilenameWithoutPath(filename);
                sb.append("/").append(URIUtils.quoteURIPathComponent(filename, true));
            }
        }
        return sb.toString();
    }

    protected String getSanitizedFilenameWithoutPath(String filename) {
        int sep = Math.max(filename.lastIndexOf('\\'), filename.lastIndexOf('/'));
        if (sep != -1) {
            filename = filename.substring(sep + 1);
        }

        return FILENAME_SANITIZATION_REGEX.matcher(filename).replaceAll("");
    }

    @Override
    public String getDownloadUrl(String storeKey) {
        return NXBIGBLOB + "/" + storeKey;
    }

    /**
     * Gets the download path and action of the URL to use to download blobs. For instance, from the path
     * "nxfile/default/3727ef6b-cf8c-4f27-ab2c-79de0171a2c8/files:files/0/file/image.png", the pair
     * ("default/3727ef6b-cf8c-4f27-ab2c-79de0171a2c8/files:files/0/file/image.png", Action.DOWNLOAD_FROM_DOC) is returned.
     *
     * @param path the path of the URL to use to download blobs
     * @return the pair download path and action
     * @since 9.1
     */
    protected Pair<String, Action> getDownloadPathAndAction(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        int slash = path.indexOf('/');
        if (slash < 0) {
            return null;
        }
        String type = path.substring(0, slash);
        String downloadPath = path.substring(slash + 1);
        switch (type) {
        case NXDOWNLOADINFO:
            // used by nxdropout.js
            return Pair.of(downloadPath, Action.INFO);
        case NXFILE:
        case NXBIGFILE:
            return Pair.of(downloadPath, Action.DOWNLOAD_FROM_DOC);
        case NXBIGZIPFILE:
        case NXBIGBLOB:
            return Pair.of(downloadPath, Action.DOWNLOAD);
        default:
            return null;
        }
    }

    /**
     * Gets a document from a repository name and a docId.
     *
     * @param repository the repository name
     * @param docId the document id
     * @return the DocumentModel
     * @since 9.1
     */
    protected DocumentModel getDownloadDocument(String repository, String docId) {
        try (CoreSession session = CoreInstance.openCoreSession(repository)) {
            DocumentRef docRef = new IdRef(docId);
            if (!session.exists(docRef)) {
                return null;
            }
            return session.getDocument(docRef);
        }
    }

    @Override
    public Blob resolveBlobFromDownloadUrl(String url) {
        String nuxeoUrl = Framework.getProperty("nuxeo.url");
        if (!url.startsWith(nuxeoUrl)) {
            return null;
        }
        String path = url.substring(nuxeoUrl.length() + 1);
        Pair<String, Action> pair = getDownloadPathAndAction(path);
        if (pair == null) {
            return null;
        }
        String downloadPath = pair.getLeft();
        try {
            DownloadBlobInfo downloadBlobInfo = new DownloadBlobInfo(downloadPath);
            DocumentModel doc = getDownloadDocument(downloadBlobInfo.repository, downloadBlobInfo.docId);
            if (doc == null) {
                return null;
            }
            return resolveBlob(doc, downloadBlobInfo.xpath);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
    public void handleDownload(HttpServletRequest req, HttpServletResponse resp, String baseUrl, String path)
            throws IOException {
        Pair<String, Action> pair = getDownloadPathAndAction(path);
        if (pair == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid URL syntax");
            return;
        }
        String downloadPath = pair.getLeft();
        Action action = pair.getRight();
        switch (action) {
        case INFO:
            handleDownload(req, resp, downloadPath, baseUrl, true);
            break;
        case DOWNLOAD_FROM_DOC:
            handleDownload(req, resp, downloadPath, baseUrl, false);
            break;
        case DOWNLOAD:
            downloadBlob(req, resp, downloadPath, "download");
            break;
        default:
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid URL syntax");
        }
    }

    protected void handleDownload(HttpServletRequest req, HttpServletResponse resp, String downloadPath, String baseUrl,
            boolean info) throws IOException {
        boolean tx = false;
        DownloadBlobInfo downloadBlobInfo;
        try {
            downloadBlobInfo = new DownloadBlobInfo(downloadPath);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Invalid URL syntax");
            return;
        }
        try {
            if (!TransactionHelper.isTransactionActive()) {
                // Manually start and stop a transaction around repository access to be able to release transactional
                // resources without waiting for the download that can take a long time (longer than the transaction
                // timeout) especially if the client or the connection is slow.
                tx = TransactionHelper.startTransaction();
            }
            String xpath = downloadBlobInfo.xpath;
            String filename = FILENAME_SANITIZATION_REGEX.matcher(downloadBlobInfo.filename).replaceAll("");
            DocumentModel doc = getDownloadDocument(downloadBlobInfo.repository, downloadBlobInfo.docId);
            if (doc == null) {
                // Send a security exception to force authentication, if the current user is anonymous
                Principal principal = req.getUserPrincipal();
                if (principal instanceof NuxeoPrincipal) {
                    NuxeoPrincipal nuxeoPrincipal = (NuxeoPrincipal) principal;
                    if (nuxeoPrincipal.isAnonymous()) {
                        throw new DocumentSecurityException("Authentication is needed for downloading the blob");
                    }
                }
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No document found");
                return;
            }
            if (info) {
                Blob blob = resolveBlob(doc, xpath);
                if (blob == null) {
                    resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No blob found");
                    return;
                }
                String downloadUrl = baseUrl + getDownloadUrl(doc, xpath, filename);
                String result = blob.getMimeType() + ':' + URLEncoder.encode(blob.getFilename(), "UTF-8") + ':'
                        + downloadUrl;
                resp.setContentType("text/plain");
                resp.getWriter().write(result);
                resp.getWriter().flush();
            } else {
                downloadBlob(req, resp, doc, xpath, null, filename, "download");
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

    @Override
    public void downloadBlob(HttpServletRequest request, HttpServletResponse response, String key, String reason) throws IOException {
        TransientStore ts = Framework.getService(TransientStoreService.class).getStore("download");
        try {
            List<Blob> blobs = ts.getBlobs(key);
            if (blobs == null) {
                throw new IllegalArgumentException("no such blobs referenced with " + key);
            }
            if (blobs.size() > 1) {
                throw new IllegalArgumentException("multipart download not yet implemented");
            }
            Blob blob = blobs.get(0);
            downloadBlob(request, response, null, null, blob, blob.getFilename(), reason);
        } finally {
            ts.remove(key);
        }
    }

    @Override
    public void downloadBlob(HttpServletRequest request, HttpServletResponse response, DocumentModel doc, String xpath,
            Blob blob, String filename, String reason) throws IOException {
        downloadBlob(request, response, doc, xpath, blob, filename, reason, Collections.emptyMap());
    }

    @Override
    public void downloadBlob(HttpServletRequest request, HttpServletResponse response, DocumentModel doc, String xpath,
            Blob blob, String filename, String reason, Map<String, Serializable> extendedInfos) throws IOException {
        if (blob == null) {
            if (doc == null || xpath == null) {
                throw new NuxeoException("No blob or doc xpath");
            }
            blob = resolveBlob(doc, xpath);
            if (blob == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No blob found");
                return;
            }
        }
        final Blob fblob = blob;
        downloadBlob(request, response, doc, xpath, blob, filename, reason, extendedInfos, null,
                byteRange -> transferBlobWithByteRange(fblob, byteRange, response));
    }

    @Override
    public void downloadBlob(HttpServletRequest request, HttpServletResponse response, DocumentModel doc, String xpath,
            Blob blob, String filename, String reason, Map<String, Serializable> extendedInfos, Boolean inline,
            Consumer<ByteRange> blobTransferer) throws IOException {
        Objects.requireNonNull(blob);
        // check blob permissions
        if (!checkPermission(doc, xpath, blob, reason, extendedInfos)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Permission denied");
            return;
        }

        // check Blob Manager download link
        BlobManager blobManager = Framework.getService(BlobManager.class);
        URI uri = blobManager == null ? null : blobManager.getURI(blob, UsageHint.DOWNLOAD, request);
        if (uri != null) {
            try {
                Map<String, Serializable> ei = new HashMap<>();
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

        try {
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
            String contentDisposition = DownloadHelper.getRFC2231ContentDisposition(request, filename, inline);
            response.setHeader("Content-Disposition", contentDisposition);
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
                } else {
                    response.setHeader("Content-Range",
                            "bytes " + byteRange.getStart() + "-" + byteRange.getEnd() + "/" + length);
                    response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                }
            }
            long contentLength = byteRange == null ? length : byteRange.getLength();
            if (contentLength < Integer.MAX_VALUE) {
                response.setContentLength((int) contentLength);
            }

            logDownload(doc, xpath, filename, reason, extendedInfos);

            // execute the final download
            blobTransferer.accept(byteRange);
        } catch (UncheckedIOException e) {
            DownloadHelper.handleClientDisconnect(e.getCause());
        } catch (IOException ioe) {
            DownloadHelper.handleClientDisconnect(ioe);
        }
    }

    protected void transferBlobWithByteRange(Blob blob, ByteRange byteRange, HttpServletResponse response)
            throws UncheckedIOException {
        transferBlobWithByteRange(blob, byteRange, () -> {
            try {
                return response.getOutputStream();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        try {
            response.flushBuffer();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void transferBlobWithByteRange(Blob blob, ByteRange byteRange, Supplier<OutputStream> outputStreamSupplier)
            throws UncheckedIOException {
        try (InputStream in = blob.getStream()) {
            @SuppressWarnings("resource")
            OutputStream out = outputStreamSupplier.get(); // not ours to close
            BufferingServletOutputStream.stopBuffering(out);
            if (byteRange == null) {
                IOUtils.copy(in, out);
            } else {
                IOUtils.copyLarge(in, out, byteRange.getStart(), byteRange.getLength());
            }
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected String fixXPath(String xpath) {
        // Hack for Flash Url wich doesn't support ':' char
        return xpath == null ? null : xpath.replace(';', ':');
    }

    @Override
    public Blob resolveBlob(DocumentModel doc, String xpath) {
        xpath = fixXPath(xpath);
        Blob blob;
        if (xpath.startsWith(BLOBHOLDER_PREFIX)) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh == null) {
                log.debug("Not a BlobHolder");
                return null;
            }
            String suffix = xpath.substring(BLOBHOLDER_PREFIX.length());
            int index;
            try {
                index = Integer.parseInt(suffix);
            } catch (NumberFormatException e) {
                log.debug(e.getMessage());
                return null;
            }
            if (!suffix.equals(Integer.toString(index))) {
                // attempt to use a non-canonical integer, could be used to bypass
                // a permission function checking just "blobholder:1" and receiving "blobholder:01"
                log.debug("Non-canonical index: " + suffix);
                return null;
            }
            if (index == 0) {
                blob = bh.getBlob();
            } else {
                blob = bh.getBlobs().get(index);
            }
        } else {
            if (!xpath.contains(":")) {
                // attempt to use a xpath not prefix-qualified, could be used to bypass
                // a permission function checking just "file:content" and receiving "content"
                log.debug("Non-canonical xpath: " + xpath);
                return null;
            }
            try {
                blob = (Blob) doc.getPropertyValue(xpath);
            } catch (PropertyNotFoundException e) {
                log.debug(e.getMessage());
                return null;
            }
        }
        return blob;
    }

    @Override
    public boolean checkPermission(DocumentModel doc, String xpath, Blob blob, String reason,
            Map<String, Serializable> extendedInfos) {
        List<DownloadPermissionDescriptor> descriptors = registry.getDownloadPermissionDescriptors();
        if (descriptors.isEmpty()) {
            return true;
        }
        xpath = fixXPath(xpath);
        Map<String, Object> context = new HashMap<>();
        Map<String, Serializable> ei = extendedInfos == null ? Collections.emptyMap() : extendedInfos;
        NuxeoPrincipal currentUser = ClientLoginModule.getCurrentPrincipal();
        context.put("Document", doc);
        context.put("XPath", xpath);
        context.put("Blob", blob);
        context.put("Reason", reason);
        context.put("Infos", ei);
        context.put("Rendition", ei.get("rendition"));
        context.put("CurrentUser", currentUser);
        for (DownloadPermissionDescriptor descriptor : descriptors) {
            ScriptEngine engine = scriptEngineManager.getEngineByName(descriptor.getScriptLanguage());
            if (engine == null) {
                throw new NuxeoException("Engine not found for language: " + descriptor.getScriptLanguage()
                        + " in permission: " + descriptor.getName());
            }
            if (!(engine instanceof Invocable)) {
                throw new NuxeoException("Engine " + engine.getClass().getName() + " not Invocable for language: "
                        + descriptor.getScriptLanguage() + " in permission: " + descriptor.getName());
            }
            Object result;
            try {
                engine.eval(descriptor.getScript());
                engine.getBindings(ScriptContext.ENGINE_SCOPE).putAll(context);
                result = ((Invocable) engine).invokeFunction(RUN_FUNCTION);
            } catch (NoSuchMethodException e) {
                throw new NuxeoException("Script does not contain function: " + RUN_FUNCTION + "() in permission: "
                        + descriptor.getName(), e);
            } catch (ScriptException e) {
                log.error("Failed to evaluate script: " + descriptor.getName(), e);
                continue;
            }
            if (!(result instanceof Boolean)) {
                log.error("Failed to get boolean result from permission: " + descriptor.getName() + " (" + result + ")");
                continue;
            }
            boolean allow = ((Boolean) result).booleanValue();
            if (!allow) {
                return false;
            }
        }
        return true;
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
        return ClientLoginModule.getCurrentPrincipal();
    }

}
