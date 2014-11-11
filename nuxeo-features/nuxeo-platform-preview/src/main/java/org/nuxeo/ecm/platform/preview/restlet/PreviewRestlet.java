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
 */

package org.nuxeo.ecm.platform.preview.restlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.international.LocaleSelector;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;
import org.nuxeo.ecm.platform.preview.api.PreviewException;
import org.nuxeo.ecm.platform.preview.helper.PreviewHelper;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseNuxeoRestlet;
import org.nuxeo.ecm.platform.util.RepositoryLocation;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.OutputRepresentation;

/**
 * Provides a REST API to retrieve the preview of a document.
 *
 * @author tiry
 */
@Name("previewRestlet")
@Scope(ScopeType.EVENT)
public class PreviewRestlet extends BaseNuxeoRestlet {

    private static final Log log = LogFactory.getLog(PreviewRestlet.class);

    @In(create = true)
    protected NavigationContext navigationContext;

    protected CoreSession documentManager;

    protected DocumentModel targetDocument;

    @In(create = true)
    protected transient LocaleSelector localeSelector;

    // cache duration in seconds
    //protected static int MAX_CACHE_LIFE = 60 * 10;

    //protected static final Map<String, PreviewCacheEntry> cachedAdapters = new ConcurrentHashMap<String, PreviewCacheEntry>();

    protected static final List<String> previewInProcessing = Collections.synchronizedList(new ArrayList<String>());

    @Override
    public void handle(Request req, Response res) {

        // Forward locale from HttpRequest to Seam context
        localeSelector.setLocale(getHttpRequest(req).getLocale());

        String repo = (String) req.getAttributes().get("repo");
        String docid = (String) req.getAttributes().get("docid");
        String xpath = (String) req.getAttributes().get("fieldPath");
        xpath = xpath.replace("-", "/");
        List<String> segments = req.getResourceRef().getSegments();
        StringBuilder sb =  new StringBuilder();
        for (int i = 6; i < segments.size(); i++) {
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
        try {
            navigationContext.setCurrentServerLocation(new RepositoryLocation(repo));
            documentManager = navigationContext.getOrCreateDocumentManager();
            targetDocument = documentManager.getDocument(new IdRef(docid));
        } catch (ClientException e) {
            handleError(res, e);
            return;
        }

        List<Blob> previewBlobs;
        try {
            previewBlobs = initCachedBlob(res, xpath, blobPostProcessing);
        } catch (Exception e) {
            handleError(res, "unable to get preview");
            return;
        }
        if (previewBlobs == null || previewBlobs.isEmpty()) {
            // response was already handled by initCachedBlob
            return;
        }
        HttpServletResponse response = getHttpResponse(res);
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");

        try {
            if (subPath == null || "".equals(subPath)) {
                handlePreview(res, previewBlobs.get(0), "text/html");
                return;
            } else {
                for (Blob blob : previewBlobs) {
                    if (subPath.equals(blob.getFilename())) {
                        handlePreview(res, blob, blob.getMimeType());
                        return;
                    }

                }
            }
        } catch (IOException e) {
            handleError(res, e);
        }
    }

    private List<Blob> initCachedBlob(Response res, String xpath, boolean blobPostProcessing)
            throws ClientException {

        HtmlPreviewAdapter preview = null; //getFromCache(targetDocument, xpath);

        if (preview == null) {
            preview = targetDocument.getAdapter(HtmlPreviewAdapter.class);
        }

        if (preview == null) {
            handleNoPreview(res, xpath, null);
            return null;
        }

        List<Blob> previewBlobs = null;
        try {
            if (xpath.equals(PreviewHelper.PREVIEWURL_DEFAULTXPATH)) {
                previewBlobs = preview.getFilePreviewBlobs(blobPostProcessing);
            } else {
                previewBlobs = preview.getFilePreviewBlobs(xpath, blobPostProcessing);
            }
            /*if (preview.cachable()) {
                updateCache(targetDocument, preview, xpath);
            }*/
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
            sb.append("No preview is available for this document</h1>");
        } else {
            sb.append("Preview can not be generated for this document</h1>");
            sb.append("<pre>Blob path: ");
            sb.append(xpath);
            sb.append("</pre>");
            sb.append("<pre>");
            sb.append(e.toString());
            sb.append("</pre>");
        }

        sb.append("</center></body></html>");
        log.error("could not build preview for missing blob at " + xpath, e);

        res.setEntity(sb.toString(), MediaType.TEXT_HTML);
        HttpServletResponse response = getHttpResponse(res);

        response.setHeader("Content-Disposition", "inline");
    }

    protected void handlePreview(Response res, Blob previewBlob, String mimeType)
            throws IOException {
        final File tempfile = File.createTempFile("nuxeo-previewrestlet-tmp",
                "");
        tempfile.deleteOnExit();
        previewBlob.transferTo(tempfile);
        res.setEntity(new OutputRepresentation(null) {
            @Override
            public void write(OutputStream outputStream) throws IOException {
                // the write call happens after the seam conversation is
                // finished which will garbage collect the CoreSession
                // instance, hence we store the blob content in a temporary
                // file
                FileInputStream instream = new FileInputStream(tempfile);
                FileUtils.copy(instream, outputStream);
                instream.close();
                tempfile.delete();
            }
        });
        HttpServletResponse response = getHttpResponse(res);

        response.setHeader("Content-Disposition", "inline");
        response.setContentType(mimeType);
    }

    /*
    protected void updateCache(DocumentModel doc, HtmlPreviewAdapter adapter,
            String xpath) throws ClientException {

        String docKey = doc.getId();
        try {
            Calendar modified = (Calendar) doc.getProperty("dublincore",
                    "modified");
            PreviewCacheEntry entry = new PreviewCacheEntry(modified, adapter,
                    xpath);
            synchronized (cachedAdapters) {
                cachedAdapters.put(docKey, entry);
            }
            cacheGC();
        } finally {
            previewInProcessing.remove(docKey);
        }
    }

    protected void removeFromCache(String key) {
        PreviewCacheEntry entry = cachedAdapters.get(key);
        if (entry != null) {
            entry.getAdapter().cleanup();
        }
        synchronized (cachedAdapters) {
            cachedAdapters.remove(key);
        }
    }

    protected HtmlPreviewAdapter getFromCache(DocumentModel doc, String xpath) throws ClientException {
        String docKey = doc.getId();

        while (previewInProcessing.contains(docKey)) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                log.error(e, e);
            }
        }

        if (cachedAdapters.containsKey(docKey)) {
            Calendar modified = (Calendar) doc.getProperty("dublincore",
                    "modified");
            PreviewCacheEntry entry = cachedAdapters.get(doc.getId());

            if (!entry.getModified().equals(modified)
                    || !xpath.equals(entry.getXpath())) {
                removeFromCache(docKey);
                return null;
            } else {
                return entry.getAdapter();
            }
        } else {
            return null;
        }
    }

    protected void cacheGC() {
        for (String key : cachedAdapters.keySet()) {
            long now = System.currentTimeMillis();
            PreviewCacheEntry entry = cachedAdapters.get(key);
            if ((now - entry.getTimeStamp()) > MAX_CACHE_LIFE * 1000) {
                removeFromCache(key);
            }
        }
    }*/

}
