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
package org.nuxeo.ecm.platform.pictures.tiles.serializer;

import net.sf.json.JSONObject;

import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;

/**
 *
 * JSON serializer for PictureTiles structure
 *
 * @author tiry
 *
 */
public class JSONPictureTilesSerializer implements PictureTilesSerializer {

    public String serialize(PictureTiles tiles) {

        JSONObject mainMap = new JSONObject();
        JSONObject tileInfo = new JSONObject();
        JSONObject orgImgInfo = new JSONObject();
        JSONObject srcImgInfo = new JSONObject();
        JSONObject otherInfo = new JSONObject();

        // tileInfo
        tileInfo.put("maxtiles", tiles.getMaxTiles());
        tileInfo.put("xtiles", tiles.getXTiles());
        tileInfo.put("ytiles", tiles.getYTiles());
        tileInfo.put("tileWidth", tiles.getTilesWidth());
        tileInfo.put("tileHeight", tiles.getTilesHeight());
        tileInfo.put("zoom", tiles.getZoomfactor());
        mainMap.put("tileInfo", tileInfo);

        // orginial Image info
        orgImgInfo.put("format", tiles.getOriginalImageInfo().getFormat());
        orgImgInfo.put("width", tiles.getOriginalImageInfo().getWidth());
        orgImgInfo.put("height", tiles.getOriginalImageInfo().getHeight());
        mainMap.put("originalImage", orgImgInfo);

        // src Image info
        srcImgInfo.put("format", tiles.getSourceImageInfo().getFormat());
        srcImgInfo.put("width", tiles.getSourceImageInfo().getWidth());
        srcImgInfo.put("height", tiles.getSourceImageInfo().getHeight());
        mainMap.put("srcImage", srcImgInfo);

        // misc tiler info
        for (String k : tiles.getInfo().keySet()) {
            otherInfo.put(k, tiles.getInfo().get(k));
        }
        mainMap.put("additionalInfo", otherInfo);

        return mainMap.toString(1);
    }

}
