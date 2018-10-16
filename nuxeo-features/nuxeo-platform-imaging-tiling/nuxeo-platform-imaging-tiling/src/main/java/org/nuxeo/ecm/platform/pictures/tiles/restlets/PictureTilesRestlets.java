/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 *
 */
package org.nuxeo.ecm.platform.pictures.tiles.restlets;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.io.download.DownloadService;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;
import org.nuxeo.ecm.platform.pictures.tiles.api.adapter.PictureTilesAdapter;
import org.nuxeo.ecm.platform.pictures.tiles.serializer.JSONPictureTilesSerializer;
import org.nuxeo.ecm.platform.pictures.tiles.serializer.PictureTilesSerializer;
import org.nuxeo.ecm.platform.pictures.tiles.serializer.XMLPictureTilesSerializer;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseStatelessNuxeoRestlet;
import org.nuxeo.runtime.api.Framework;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;

/**
 * Restlet to provide a REST API on top of the PictureTilingService.
 *
 * @author tiry
 */
public class PictureTilesRestlets extends BaseStatelessNuxeoRestlet {

    // cache duration in seconds
    protected static int MAX_CACHE_LIFE = 60 * 10;

    protected static Map<String, PictureTilesCachedEntry> cachedAdapters = new ConcurrentHashMap<String, PictureTilesCachedEntry>();

    @Override
    public void doHandleStatelessRequest(Request req, Response res) {
        HttpServletRequest request = getHttpRequest(req);
        HttpServletResponse response = getHttpResponse(res);

        String repo = (String) req.getAttributes().get("repoId");
        String docid = (String) req.getAttributes().get("docId");
        Integer tileWidth = Integer.decode((String) req.getAttributes().get("tileWidth"));
        Integer tileHeight = Integer.decode((String) req.getAttributes().get("tileHeight"));
        Integer maxTiles = Integer.decode((String) req.getAttributes().get("maxTiles"));

        Form form = req.getResourceRef().getQueryAsForm();
        String xpath = form.getFirstValue("fieldPath");
        String x = form.getFirstValue("x");
        String y = form.getFirstValue("y");
        String format = form.getFirstValue("format");

        String test = form.getFirstValue("test");
        if (test != null) {
            try {
                handleSendTest(res, repo, docid, tileWidth, tileHeight, maxTiles);
                return;
            } catch (IOException e) {
                handleError(res, e);
                return;
            }
        }

        if (repo == null || repo.equals("*")) {
            handleError(res, "you must specify a repository");
            return;
        }
        if (docid == null || repo.equals("*")) {
            handleError(res, "you must specify a documentId");
            return;
        }
        Boolean init = initRepositoryAndTargetDocument(res, repo, docid);

        if (!init) {
            handleError(res, "unable to init repository connection");
            return;
        }

        PictureTilesAdapter adapter;
        try {
            adapter = getFromCache(targetDocument, xpath);
            if (adapter == null) {
                adapter = targetDocument.getAdapter(PictureTilesAdapter.class);
                if ((xpath != null) && (!"".equals(xpath))) {
                    adapter.setXPath(xpath);
                }
                updateCache(targetDocument, adapter, xpath);
            }
        } catch (NuxeoException e) {
            handleError(res, e);
            return;
        }

        if (adapter == null) {
            handleNoTiles(res, null);
            return;
        }

        PictureTiles tiles = null;
        try {
            tiles = adapter.getTiles(tileWidth, tileHeight, maxTiles);
        } catch (NuxeoException e) {
            handleError(res, e);
            return;
        }

        if ((x == null) || (y == null)) {
            handleSendInfo(res, tiles, format);
            return;
        }

        final Blob image;
        try {
            image = tiles.getTile(Integer.decode(x), Integer.decode(y));
        } catch (NuxeoException | IOException e) {
            handleError(res, e);
            return;
        }

        String reason = "tile";
        Boolean inline = Boolean.TRUE;
        Map<String, Serializable> extendedInfos = new HashMap<>();
        extendedInfos.put("x", x);
        extendedInfos.put("y", y);
        DownloadService downloadService = Framework.getService(DownloadService.class);
        try {
            downloadService.downloadBlob(request, response, targetDocument, xpath, image, image.getFilename(), reason,
                    extendedInfos, inline, byteRange -> setEntityToBlobOutput(image, byteRange, res));
        } catch (IOException e) {
            handleError(res, e);
        }
    }

    protected void handleSendTest(Response res, String repoId, String docId, Integer tileWidth, Integer tileHeight,
            Integer maxTiles) throws IOException {
        MediaType mt;
        mt = MediaType.TEXT_HTML;

        File file = FileUtils.getResourceFileFromContext("testTiling.html");
        String html = org.apache.commons.io.FileUtils.readFileToString(file, UTF_8);

        html = html.replace("$repoId$", repoId);
        html = html.replace("$docId$", docId);
        html = html.replace("$tileWidth$", tileWidth.toString());
        html = html.replace("$tileHeight$", tileHeight.toString());
        html = html.replace("$maxTiles$", maxTiles.toString());

        res.setEntity(html, mt);
    }

    protected void handleSendInfo(Response res, PictureTiles tiles, String format) {
        if (format == null) {
            format = "XML";
        }
        MediaType mt;
        PictureTilesSerializer serializer;

        if (format.equalsIgnoreCase("json")) {
            serializer = new JSONPictureTilesSerializer();
            mt = MediaType.APPLICATION_JSON;
        } else {
            serializer = new XMLPictureTilesSerializer();
            mt = MediaType.APPLICATION_XML;
        }

        res.setEntity(serializer.serialize(tiles), mt);
        res.getEntity().setCharacterSet(CharacterSet.UTF_8);

        HttpServletResponse response = getHttpResponse(res);
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");
    }

    protected void handleNoTiles(Response res, Exception e) {
        StringBuilder sb = new StringBuilder();

        sb.append("<html><body><center><h1>");
        if (e == null) {
            sb.append("No Tiling is available for this document</h1>");
        } else {
            sb.append("Picture Tiling can not be generated for this document</h1>");
            sb.append("<br/><pre>");
            sb.append(e.toString());
            sb.append("</pre>");
        }

        sb.append("</center></body></html>");

        res.setEntity(sb.toString(), MediaType.TEXT_HTML);
        HttpServletResponse response = getHttpResponse(res);
        response.setHeader("Content-Disposition", "inline");
    }

    protected void updateCache(DocumentModel doc, PictureTilesAdapter adapter, String xpath) {

        Calendar modified = (Calendar) doc.getProperty("dublincore", "modified");
        PictureTilesCachedEntry entry = new PictureTilesCachedEntry(modified, adapter, xpath);
        cachedAdapters.put(doc.getId(), entry);
        cacheGC();
    }

    protected void removeFromCache(String key) {
        PictureTilesCachedEntry entry = cachedAdapters.get(key);
        if (entry != null) {
            entry.getAdapter().cleanup();
        }
        cachedAdapters.remove(key);
    }

    protected boolean isSameDate(Calendar d1, Calendar d2) {

        // because one of the date is stored in the repository
        // the date may be 'rounded'
        // so compare
        long t1 = d1.getTimeInMillis() / 1000;
        long t2 = d2.getTimeInMillis() / 1000;
        return Math.abs(t1 - t2) <= 1;
    }

    protected PictureTilesAdapter getFromCache(DocumentModel doc, String xpath) {
        if (cachedAdapters.containsKey(doc.getId())) {
            if (xpath == null) {
                xpath = "";
            }
            Calendar modified = (Calendar) doc.getProperty("dublincore", "modified");
            PictureTilesCachedEntry entry = cachedAdapters.get(doc.getId());

            if ((!isSameDate(entry.getModified(), modified)) || (!xpath.equals(entry.getXpath()))) {
                removeFromCache(doc.getId());
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
            PictureTilesCachedEntry entry = cachedAdapters.get(key);
            if ((now - entry.getTimeStamp()) > MAX_CACHE_LIFE * 1000) {
            }
        }
    }

}
