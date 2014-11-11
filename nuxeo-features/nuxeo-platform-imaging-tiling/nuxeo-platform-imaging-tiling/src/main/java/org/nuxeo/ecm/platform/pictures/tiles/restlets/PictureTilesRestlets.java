/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 */
package org.nuxeo.ecm.platform.pictures.tiles.restlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletResponse;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;
import org.nuxeo.ecm.platform.pictures.tiles.api.adapter.PictureTilesAdapter;
import org.nuxeo.ecm.platform.pictures.tiles.serializer.JSONPictureTilesSerializer;
import org.nuxeo.ecm.platform.pictures.tiles.serializer.PictureTilesSerializer;
import org.nuxeo.ecm.platform.pictures.tiles.serializer.XMLPictureTilesSerializer;
import org.nuxeo.ecm.platform.ui.web.restAPI.BaseStatelessNuxeoRestlet;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.OutputRepresentation;


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
    public void handle(Request req, Response res) {

        String repo = (String) req.getAttributes().get("repoId");
        String docid = (String) req.getAttributes().get("docId");
        Integer tileWidth = Integer.decode((String) req.getAttributes().get(
                "tileWidth"));
        Integer tileHeight = Integer.decode((String) req.getAttributes().get(
                "tileHeight"));
        Integer maxTiles = Integer.decode((String) req.getAttributes().get(
                "maxTiles"));

        Form form = req.getResourceRef().getQueryAsForm();
        String xpath = (String) form.getFirstValue("fieldPath");
        String x = form.getFirstValue("x");
        String y = form.getFirstValue("y");
        String format = form.getFirstValue("format");

        String test = form.getFirstValue("test");
        if (test != null) {
            try {
                handleSendTest(res, repo, docid, tileWidth, tileHeight,
                        maxTiles);
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
        } catch (ClientException e) {
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
        } catch (ClientException e) {
            handleError(res, e);
        }

        if ((x == null) || (y == null)) {
            handleSendInfo(res, tiles, format);
        } else {
            handleSendImage(res, tiles, Integer.decode(x), Integer.decode(y));
        }
    }

    protected void handleSendTest(Response res, String repoId, String docId,
            Integer tileWidth, Integer tileHeight, Integer maxTiles)
            throws IOException {
        MediaType mt = null;
        mt = MediaType.TEXT_HTML;

        File file = FileUtils.getResourceFileFromContext("testTiling.html");
        String html = FileUtils.readFile(file);

        html = html.replace("$repoId$", repoId);
        html = html.replace("$docId$", docId);
        html = html.replace("$tileWidth$", tileWidth.toString());
        html = html.replace("$tileHeight$", tileHeight.toString());
        html = html.replace("$maxTiles$", maxTiles.toString());

        res.setEntity(html, mt);
    }

    protected void handleSendInfo(Response res, PictureTiles tiles,
            String format) {
        if (format == null) {
            format = "XML";
        }
        MediaType mt = null;
        PictureTilesSerializer serializer = null;

        if (format.equalsIgnoreCase("json")) {
            serializer = new JSONPictureTilesSerializer();
            mt = MediaType.APPLICATION_JSON;
        } else {
            serializer = new XMLPictureTilesSerializer();
            mt = MediaType.TEXT_XML;
        }

        res.setEntity(serializer.serialize(tiles), mt);
        res.getEntity().setCharacterSet(CharacterSet.UTF_8);

        HttpServletResponse response = getHttpResponse(res);
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Pragma", "no-cache");
    }

    protected void handleSendImage(Response res, PictureTiles tiles, Integer x,
            Integer y) {

        Blob image;
        try {
            image = tiles.getTile(x, y);
        } catch (Exception e) {
            handleError(res, e);
            return;
        }

        try {
            final File tempfile = File.createTempFile(
                    "nuxeo-tilingrestlet-tmp", "");
            image.transferTo(tempfile);
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
        } catch (IOException e) {
            handleError(res, e);
        }
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

    protected void updateCache(DocumentModel doc, PictureTilesAdapter adapter,
            String xpath) throws ClientException {

        Calendar modified = (Calendar) doc.getProperty("dublincore", "modified");
        PictureTilesCachedEntry entry = new PictureTilesCachedEntry(modified,
                adapter, xpath);
        synchronized (cachedAdapters) {
            cachedAdapters.put(doc.getId(), entry);
        }
        cacheGC();
    }

    protected void removeFromCache(String key) {
        PictureTilesCachedEntry entry = cachedAdapters.get(key);
        if (entry != null) {
            entry.getAdapter().cleanup();
        }
        synchronized (cachedAdapters) {
            cachedAdapters.remove(key);
        }
    }

    protected PictureTilesAdapter getFromCache(DocumentModel doc, String xpath) throws ClientException {
        if (cachedAdapters.containsKey(doc.getId())) {
            if (xpath == null) {
                xpath = "";
            }
            Calendar modified = (Calendar) doc.getProperty("dublincore",
                    "modified");
            PictureTilesCachedEntry entry = cachedAdapters.get(doc.getId());

            if ((!entry.getModified().equals(modified))
                    || (!xpath.equals(entry.getXpath()))) {
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
