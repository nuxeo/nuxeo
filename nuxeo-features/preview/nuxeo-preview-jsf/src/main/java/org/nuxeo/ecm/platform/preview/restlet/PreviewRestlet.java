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
 *     Thomas Roger
 *
 */

package org.nuxeo.ecm.platform.preview.restlet;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.DocumentBlobHolder;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobManager.UsageHint;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.core.io.download.DownloadService.DownloadContext;
import org.nuxeo.ecm.platform.preview.adapter.base.ConverterBasedHtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.NothingToPreviewException;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseStatelessNuxeoRestlet;
import org.nuxeo.runtime.api.Framework;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Status;

/**
 * Provides a REST API to retrieve the preview of a document.
 *
 * @author tiry
 * @since 10.3
 * @deprecated since 10.3
 */
public class PreviewRestlet extends BaseStatelessNuxeoRestlet {

    private static final Log log = LogFactory.getLog(PreviewRestlet.class);

    public static final String PREVIEWURL_DEFAULTXPATH = "default";

    // cache duration in seconds
    // protected static int MAX_CACHE_LIFE = 60 * 10;

    // protected static final Map<String, PreviewCacheEntry> cachedAdapters =
    // new ConcurrentHashMap<String, PreviewCacheEntry>();

    protected static final List<String> previewInProcessing = Collections.synchronizedList(new ArrayList<String>());

    @Override
    public void doHandleStatelessRequest(Request req, Response res) {
        logDeprecation();
        HttpServletRequest request = getHttpRequest(req);
        HttpServletResponse response = getHttpResponse(res);

        String repo = (String) req.getAttributes().get("repo");
        String docid = (String) req.getAttributes().get("docid");
        String xpath = (String) req.getAttributes().get("fieldPath");
        xpath = xpath.replace("-", "/");
        List<String> segments = req.getResourceRef().getSegments();
        StringBuilder sb = new StringBuilder();
        int pos = segments.indexOf("restAPI") + 5;
        for (int i = pos; i < segments.size(); i++) {
            sb.append(segments.get(i));
            sb.append("/");
        }
        String subPath = sb.substring(0, sb.length() - 1);

        try {
            xpath = URLDecoder.decode(xpath, "UTF-8");
            subPath = URLDecoder.decode(subPath, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error(e);
        }

        String blobPostProcessingParameter = getQueryParamValue(req, "blobPostProcessing", "false");
        boolean blobPostProcessing = Boolean.parseBoolean(blobPostProcessingParameter);

        if (repo == null || repo.equals("*")) {
            handleError(res, "you must specify a repository");
            return;
        }
        if (docid == null || repo.equals("*")) {
            handleError(res, "you must specify a documentId");
            return;
        }

        boolean initOk = initRepositoryAndTargetDocument(res, repo, docid);
        if (!initOk) {
            return;
        }

        // if it's a managed blob try to use the embed uri for preview
        Blob blobToPreview = getBlobToPreview(xpath);
        BlobManager blobManager = Framework.getService(BlobManager.class);
        try {
            URI uri = blobManager.getURI(blobToPreview, UsageHint.EMBED, null);
            if (uri != null) {
                res.redirectSeeOther(uri.toString());
                return;
            }
        } catch (IOException e) {
            handleError(res, e);
            return;
        }

        List<Blob> previewBlobs = initCachedBlob(res, xpath, blobPostProcessing);
        if (previewBlobs == null || previewBlobs.isEmpty()) {
            // response was already handled by initCachedBlob
            return;
        }

        // find blob
        Blob blob = null;
        if (StringUtils.isEmpty(subPath)) {
            blob = previewBlobs.get(0);
            blob.setMimeType("text/html");
        } else {
            for (Blob b : previewBlobs) {
                if (subPath.equals(b.getFilename())) {
                    blob = b;
                    break;
                }
            }
        }
        if (blob == null) {
            res.setStatus(Status.CLIENT_ERROR_NOT_FOUND);
            return;
        }

        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");

        final Blob fblob = blob;
        if (xpath == null || "default".equals(xpath)) {
            xpath = DownloadService.BLOBHOLDER_0;
        }
        Map<String, Serializable> extendedInfos = Collections.singletonMap("subPath", subPath);
        try {
            DownloadContext context = DownloadContext.builder(request, response)
                                                     .doc(targetDocument)
                                                     .xpath(xpath)
                                                     .blob(blob)
                                                     .reason("preview")
                                                     .extendedInfos(extendedInfos)
                                                     .inline(true)
                                                     .blobTransferer(
                                                             byteRange -> setEntityToBlobOutput(fblob, byteRange, res))
                                                     .build();
            Framework.getService(DownloadService.class).downloadBlob(context);
        } catch (IOException e) {
            handleError(res, e);
        }
    }

    /**
     * @since 7.3
     */
    private Blob getBlobToPreview(String xpath) {
        BlobHolder bh;
        if ((xpath == null) || ("default".equals(xpath))) {
            bh = targetDocument.getAdapter(BlobHolder.class);
        } else {
            bh = new DocumentBlobHolder(targetDocument, xpath);
        }
        return bh.getBlob();
    }

    private List<Blob> initCachedBlob(Response res, String xpath, boolean blobPostProcessing) {

        HtmlPreviewAdapter preview = null; // getFromCache(targetDocument,
        // xpath);

        targetDocument.putContextData(ConverterBasedHtmlPreviewAdapter.OLD_PREVIEW_PROPERTY, true);
        // if (preview == null) {
        preview = targetDocument.getAdapter(HtmlPreviewAdapter.class);
        // }

        if (preview == null) {
            handleNoPreview(res, xpath, null);
            return null;
        }

        List<Blob> previewBlobs = null;
        try {
            if (xpath.equals(PREVIEWURL_DEFAULTXPATH)) {
                previewBlobs = preview.getFilePreviewBlobs(blobPostProcessing);
            } else {
                previewBlobs = preview.getFilePreviewBlobs(xpath, blobPostProcessing);
            }
            /*
             * if (preview.cachable()) { updateCache(targetDocument, preview, xpath); }
             */
        } catch (PreviewException e) {
            previewInProcessing.remove(targetDocument.getId());
            handleNoPreview(res, xpath, e);
            return null;
        }

        if (previewBlobs == null || previewBlobs.size() == 0) {
            handleNoPreview(res, xpath, null);
            return null;
        }
        return previewBlobs;
    }

    protected void handleNoPreview(Response res, String xpath, Exception e) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><body><center><h1>");
        if (e == null) {
            sb.append("No preview is available for this document." + "</h1>");
        } else {
            sb.append("Preview cannot be generated for this document." + "</h1>");
            sb.append("<pre>Technical issue:</pre>");
            sb.append("<pre>Blob path: ");
            sb.append(xpath);
            sb.append("</pre>");
            sb.append("<pre>");
            sb.append(e.toString());
            sb.append("</pre>");
        }

        sb.append("</center></body></html>");
        if (e instanceof NothingToPreviewException) {
            // Not an error, don't log
        } else {
            log.error("Could not build preview for missing blob at " + xpath, e);
        }

        res.setEntity(sb.toString(), MediaType.TEXT_HTML);
        HttpServletResponse response = getHttpResponse(res);

        response.setHeader("Content-Disposition", "inline");
    }

}
