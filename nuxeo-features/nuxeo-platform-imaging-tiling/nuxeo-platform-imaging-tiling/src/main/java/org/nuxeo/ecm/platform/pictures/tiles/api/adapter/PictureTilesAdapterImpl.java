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
package org.nuxeo.ecm.platform.pictures.tiles.api.adapter;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilingService;
import org.nuxeo.ecm.platform.pictures.tiles.api.imageresource.DocumentImageResource;
import org.nuxeo.ecm.platform.pictures.tiles.api.imageresource.ImageResource;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation for the PictureTilesAdapter. This implementation
 * simply uses a xPath to get the target blob.
 *
 * @author tiry
 */
public class PictureTilesAdapterImpl implements PictureTilesAdapter {

    protected String xPath;

    protected DocumentModel doc;

    protected String fileName;

    protected Map<String, PictureTiles> tiles = new HashMap<String, PictureTiles>();

    protected static PictureTilingService pts;

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

    public PictureTiles getTiles(int tileWidth, int tileHeight, int maxTiles)
            throws ClientException {

        String key = tileWidth + "-" + tileHeight + "-" + maxTiles;

        if (!tiles.containsKey(key)) {
            PictureTiles tile = getService().getTiles(getResource(), tileWidth,
                    tileHeight, maxTiles, 0, 0, false);
            tiles.put(key, tile);
        }
        return tiles.get(key);
    }

    public void cleanup() {
        for (String k : tiles.keySet()) {
            tiles.get(k).release();
        }
        tiles = null;

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

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

}
