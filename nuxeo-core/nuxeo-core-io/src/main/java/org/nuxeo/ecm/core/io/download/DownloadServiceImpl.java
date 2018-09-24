/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *     Estelle Giuly <egiuly@nuxeo.com>
 */
package org.nuxeo.ecm.core.io.download;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.Principal;
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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CloseableCoreSession;
import org.nuxeo.ecm.core.api.CoreInstance;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.BlobHolderAdapterService;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.impl.blob.AsyncBlob;
import org.nuxeo.ecm.core.api.local.ClientLoginModule;
import org.nuxeo.ecm.core.api.model.PropertyNotFoundException;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.event.impl.EventContextImpl;
import org.nuxeo.ecm.core.transientstore.api.TransientStore;
import org.nuxeo.ecm.core.transientstore.api.TransientStoreService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * This service allows the download of blobs to a HTTP response.
 *
 * @since 7.3
 */
public class DownloadServiceImpl extends DefaultComponent implements DownloadService {

    private static final Log log = LogFactory.getLog(DownloadServiceImpl.class);

    public static final String XP_PERMISSIONS = "permissions";

    public static final String XP_REDIRECT_RESOLVER = "redirectResolver";

    protected static final int DOWNLOAD_BUFFER_SIZE = 1024 * 512;

    private static final String NUXEO_VIRTUAL_HOST = "nuxeo-virtual-host";

    private static final String VH_PARAM = "nuxeo.virtual.host";

    private static final String FORCE_NO_CACHE_ON_MSIE = "org.nuxeo.download.force.nocache.msie";

    private static final String RUN_FUNCTION = "run";

    private static final Pattern FILENAME_SANITIZATION_REGEX = Pattern.compile(";\\w+=.*");

    protected enum Action {
        DOWNLOAD, DOWNLOAD_FROM_DOC, INFO, BLOBSTATUS
    }

    protected ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    protected RedirectResolver redirectResolver;

    @Override
    public void start(ComponentContext context) {
        super.start(context);
        List<RedirectResolverDescriptor> descriptors = getDescriptors(XP_REDIRECT_RESOLVER);
        if (!descriptors.isEmpty()) {
            RedirectResolverDescriptor descriptor = descriptors.get(descriptors.size() - 1);
            try {
                redirectResolver = descriptor.klass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                getLog().error(e,e);
            }
        }
        if (redirectResolver == null) {
            redirectResolver = new DefaultRedirectResolver();
        }
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        super.stop(context);
        redirectResolver = null;
    }

    /**
     * {@inheritDoc} Multipart download are not yet supported. You can only provide a blob singleton at this time.
     */
    @Override
    public String storeBlobs(List<Blob> blobs) {
        if (blobs.size() > 1) {
            throw new IllegalArgumentException("multipart download not yet implemented");
        }
        TransientStore ts = Framework.getService(TransientStoreService.class).getStore(TRANSIENT_STORE_STORE_NAME);
        String storeKey = UUID.randomUUID().toString();
        ts.putBlobs(storeKey, blobs);
        ts.setCompleted(storeKey, true);
        return storeKey;
    }

    @Override
    public String getDownloadUrl(DocumentModel doc, String xpath, String filename) {
        return getDownloadUrl(doc.getRepositoryName(), doc.getId(), xpath, filename, doc.getChangeToken());
    }

    @Override
    public String getDownloadUrl(String repositoryName, String docId, String xpath, String filename) {
        return getDownloadUrl(repositoryName, docId, xpath, filename, null);
    }

    @Override
    public String getDownloadUrl(String repositoryName, String docId, String xpath, String filename,
            String changeToken) {
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
        if (StringUtils.isNotEmpty(changeToken)) {
            try {
                sb.append("?")
                  .append(CoreSession.CHANGE_TOKEN)
                  .append("=")
                  .append(URLEncoder.encode(changeToken, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                log.error("Cannot append changeToken", e);
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
     * ("default/3727ef6b-cf8c-4f27-ab2c-79de0171a2c8/files:files/0/file/image.png", Action.DOWNLOAD_FROM_DOC) is
     * returned.
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
        case NXBLOBSTATUS:
            return Pair.of(downloadPath, Action.BLOBSTATUS);
        default:
            return null;
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
            try (CloseableCoreSession session = CoreInstance.openCoreSession(downloadBlobInfo.repository)) {
                DocumentRef docRef = new IdRef(downloadBlobInfo.docId);
                if (!session.exists(docRef)) {
                    return null;
                }
                DocumentModel doc = session.getDocument(docRef);
                return resolveBlob(doc, downloadBlobInfo.xpath);
            }
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
        case BLOBSTATUS:
            downloadBlobStatus(req, resp, downloadPath, "download");
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
            String filename = downloadBlobInfo.filename;
            try (CloseableCoreSession session = CoreInstance.openCoreSession(downloadBlobInfo.repository)) {
                DocumentRef docRef = new IdRef(downloadBlobInfo.docId);
                if (!session.exists(docRef)) {
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
                DocumentModel doc = session.getDocument(docRef);
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
    public void downloadBlobStatus(HttpServletRequest request, HttpServletResponse response, String key, String reason)
            throws IOException {
        this.downloadBlob(request, response, key, reason, true);
    }

    @Override
    public void downloadBlob(HttpServletRequest request, HttpServletResponse response, String key, String reason)
            throws IOException {
        this.downloadBlob(request, response, key, reason, false);
    }

    protected void downloadBlob(HttpServletRequest request, HttpServletResponse response, String key, String reason,
            boolean status) throws IOException {
        TransientStore ts = Framework.getService(TransientStoreService.class).getStore(TRANSIENT_STORE_STORE_NAME);
        if (!ts.exists(key)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        List<Blob> blobs = ts.getBlobs(key);
        if (blobs == null || blobs.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        if (blobs.size() > 1) {
            throw new IllegalArgumentException("multipart download not yet implemented");
        }
        if (ts.getParameter(key, TRANSIENT_STORE_PARAM_ERROR) != null) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    (String) ts.getParameter(key, TRANSIENT_STORE_PARAM_ERROR));
        } else {
            boolean isCompleted = ts.isCompleted(key);
            if (!status && !isCompleted) {
                response.setStatus(HttpServletResponse.SC_ACCEPTED);
                return;
            }
            Blob blob;
            if (status) {
                Serializable progress = ts.getParameter(key, TRANSIENT_STORE_PARAM_PROGRESS);
                blob = new AsyncBlob(key, isCompleted, progress != null ? (int) progress : -1);
            } else {
                blob = blobs.get(0);
            }
            try {
                downloadBlob(request, response, null, null, blob, blob.getFilename(), reason);
            } finally {
                if (!status) {
                    ts.remove(key);
                }
            }
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
        downloadBlob(request, response, doc, xpath, blob, filename, reason, extendedInfos, null);
    }

    @Override
    public void downloadBlob(HttpServletRequest request, HttpServletResponse response, DocumentModel doc, String xpath,
            Blob blob, String filename, String reason, Map<String, Serializable> extendedInfos, Boolean inline)
            throws IOException {
        if (blob == null) {
            if (doc == null) {
                throw new NuxeoException("No doc specified");
            }
            blob = resolveBlob(doc, xpath);
            if (blob == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No blob found");
                return;
            }
        }
        final Blob fblob = blob;
        downloadBlob(request, response, doc, xpath, blob, filename, reason, extendedInfos, inline,
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

        // check Blob Manager external download link
        URI uri = redirectResolver.getURI(blob, UsageHint.DOWNLOAD, request);
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
            String digest = blob.getDigest();
            if (digest == null) {
                digest = DigestUtils.md5Hex(blob.getStream());
            }
            String etag = '"' + digest + '"'; // with quotes per RFC7232 2.3
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
            if (StringUtils.isNotBlank(blob.getEncoding())) {
                try {
                    response.setCharacterEncoding(blob.getEncoding());
                } catch (IllegalArgumentException e) {
                    // ignore invalid encoding
                }
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
                    getLog().error("Invalid byte range received: " + range);
                } else {
                    response.setHeader("Content-Range",
                            "bytes " + byteRange.getStart() + "-" + byteRange.getEnd() + "/" + length);
                    response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                }
            }
            long contentLength = byteRange == null ? length : byteRange.getLength();
            response.setContentLengthLong(contentLength);

            // log the download but not if it's a random byte range
            if (byteRange == null || byteRange.getStart() == 0) {
                logDownload(doc, xpath, filename, reason, extendedInfos);
            }

            // execute the final download
            blobTransferer.accept(byteRange);
        } catch (UncheckedIOException e) {
            DownloadHelper.handleClientDisconnect(e.getCause());
        } catch (IOException ioe) {
            DownloadHelper.handleClientDisconnect(ioe);
        }
    }

    protected void transferBlobWithByteRange(Blob blob, ByteRange byteRange, HttpServletResponse response) {
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
    public void transferBlobWithByteRange(Blob blob, ByteRange byteRange, Supplier<OutputStream> outputStreamSupplier) {
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
    public Blob resolveBlob(DocumentModel doc) {
        BlobHolderAdapterService blobHolderAdapterService = Framework.getService(BlobHolderAdapterService.class);
        return blobHolderAdapterService.getBlobHolderAdapter(doc, "download").getBlob();
    }

    @Override
    public Blob resolveBlob(DocumentModel doc, String xpath) {
        if (xpath == null) {
            return resolveBlob(doc);
        }
        xpath = fixXPath(xpath);
        Blob blob;
        if (xpath.startsWith(BLOBHOLDER_PREFIX)) {
            BlobHolder bh = doc.getAdapter(BlobHolder.class);
            if (bh == null) {
                getLog().debug("Not a BlobHolder");
                return null;
            }
            String suffix = xpath.substring(BLOBHOLDER_PREFIX.length());
            int index;
            try {
                index = Integer.parseInt(suffix);
            } catch (NumberFormatException e) {
                getLog().debug(e.getMessage());
                return null;
            }
            if (!suffix.equals(Integer.toString(index))) {
                // attempt to use a non-canonical integer, could be used to bypass
                // a permission function checking just "blobholder:1" and receiving "blobholder:01"
                getLog().debug("Non-canonical index: " + suffix);
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
                getLog().debug("Non-canonical xpath: " + xpath);
                return null;
            }
            try {
                blob = (Blob) doc.getPropertyValue(xpath);
            } catch (PropertyNotFoundException e) {
                getLog().debug(e.getMessage());
                return null;
            }
        }
        return blob;
    }

    @Override
    public boolean checkPermission(DocumentModel doc, String xpath, Blob blob, String reason,
            Map<String, Serializable> extendedInfos) {
        List<DownloadPermissionDescriptor> descriptors = getDescriptors(XP_PERMISSIONS);
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
                        + " in permission: " + descriptor.name);
            }
            if (!(engine instanceof Invocable)) {
                throw new NuxeoException("Engine " + engine.getClass().getName() + " not Invocable for language: "
                        + descriptor.getScriptLanguage() + " in permission: " + descriptor.name);
            }
            Object result;
            try {
                engine.eval(descriptor.script);
                engine.getBindings(ScriptContext.ENGINE_SCOPE).putAll(context);
                result = ((Invocable) engine).invokeFunction(RUN_FUNCTION);
            } catch (NoSuchMethodException e) {
                throw new NuxeoException("Script does not contain function: " + RUN_FUNCTION + "() in permission: "
                        + descriptor.name, e);
            } catch (ScriptException e) {
                getLog().error("Failed to evaluate script: " + descriptor.name, e);
                continue;
            }
            if (!(result instanceof Boolean)) {
                getLog().error(
                        "Failed to get boolean result from permission: " + descriptor.name + " (" + result + ")");
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
        if (userAgent != null && userAgent.contains("MSIE") && (secure || forceNoCacheOnMSIE())) {
            String cacheControl = "max-age=15, must-revalidate";
            getLog().debug("Setting Cache-Control: " + cacheControl);
            response.setHeader("Cache-Control", cacheControl);
        }
    }

    protected static boolean forceNoCacheOnMSIE() {
        // see NXP-7759
        return Framework.isBooleanPropertyTrue(FORCE_NO_CACHE_ON_MSIE);
    }

    @Override
    public void logDownload(DocumentModel doc, String xpath, String filename, String reason,
            Map<String, Serializable> extendedInfos) {
        if ("webengine".equals(reason)) {
            // don't log JSON operation results as downloads
            return;
        }
        EventService eventService = Framework.getService(EventService.class);
        if (eventService == null) {
            return;
        }
        EventContext ctx;
        if (doc != null) {
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
