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
package org.nuxeo.ecm.platform.pictures.tiles.serializer;

import net.sf.json.JSONObject;

import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;

/**
 * JSON serializer for PictureTiles structure
 *
 * @author tiry
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
