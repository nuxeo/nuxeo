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
package org.nuxeo.ecm.platform.pictures.tiles.api.adapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilingService;
import org.nuxeo.ecm.platform.pictures.tiles.api.imageresource.DocumentImageResource;
import org.nuxeo.ecm.platform.pictures.tiles.api.imageresource.ImageResource;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation for the PictureTilesAdapter. This implementation simply uses a xPath to get the target blob.
 *
 * @author tiry
 */
public class PictureTilesAdapterImpl implements PictureTilesAdapter {

    protected String xPath;

    protected DocumentModel doc;

    protected String fileName;

    protected Map<String, PictureTiles> tiles = new ConcurrentHashMap<>();

    protected static PictureTilingService pts;

    protected static final Log log = LogFactory.getLog(PictureTilesAdapterImpl.class);

    public PictureTilesAdapterImpl(DocumentModel doc, String xPath) {
        this.xPath = xPath;
        this.doc = doc;
    }

    public PictureTilesAdapterImpl(DocumentModel doc) {
        this(doc, null);
    }

    protected ImageResource getResource() {
        DocumentImageResource res = new DocumentImageResource(doc, xPath);
        if (fileName != null) {
            res.setFileName(fileName);
        }
        return res;
    }

    protected PictureTilingService getService() {
        if (pts == null) {
            pts = Framework.getLocalService(PictureTilingService.class);
        }
        return pts;
    }

    public PictureTiles getTiles(int tileWidth, int tileHeight, int maxTiles) {

        String key = tileWidth + "-" + tileHeight + "-" + maxTiles;

        if (!tiles.containsKey(key)) {
            PictureTiles tile = getService().getTiles(getResource(), tileWidth, tileHeight, maxTiles, 0, 0, false);
            tiles.put(key, tile);
        }
        return tiles.get(key);
    }

    public void cleanup() {
        if (tiles == null) {
            return;
        }
        for (String k : tiles.keySet()) {
            tiles.get(k).release();
        }
        tiles = new ConcurrentHashMap<>();
    }

    public String getXPath() {
        return xPath;
    }

    public void setXPath(String path) {
        xPath = path;
    }

    public DocumentModel getDoc() {
        return doc;
    }

    public void setDoc(DocumentModel doc) {
        this.doc = doc;
    }

    /**
     * @deprecated since 9.1 as filename is now hold by blob
     */
    @Deprecated
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

}
