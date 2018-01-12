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
package org.nuxeo.ecm.platform.pictures.tiles.api;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.pictures.tiles.helpers.StringMaker;
import org.nuxeo.runtime.api.Framework;

/**
 * Default implementation for the PictureTiles interface
 *
 * @author tiry
 */
public class PictureTilesImpl implements PictureTiles, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static final String TILE_OUTPUT_DIR_KEY = "outputDirPath";

    public static final String TILE_INPUT_FILE_KEY = "inputFilePath";

    public static final String X_TILES_KEY = "XTiles";

    public static final String Y_TILES_KEY = "YTiles";

    public static final String LAST_MODIFICATION_DATE_KEY = "lastModificationDate";

    public static final String TILES_PREFIX_KEY = "TilesPrefix";

    public static final String TILES_SUFFIX_KEY = "TilesSuffix";

    public static final String TILES_WIDTH_KEY = "TilesWidth";

    public static final String TILES_HEIGHT_KEY = "TilesHeight";

    public static final String MAX_TILES_KEY = "MaxTiles";

    public static final String PROGRESSIVE_TILING_KEY = "ProgressiveTiling";

    protected Map<String, String> infoMap;

    protected String tilesDirPath;

    protected String cacheKey;

    protected ImageInfo sourceImageInfo;

    protected ImageInfo originalImageInfo;

    public PictureTilesImpl(String tilesDirPath) {
        this.tilesDirPath = tilesDirPath;
    }

    public PictureTilesImpl(Map<String, String> info) {
        infoMap = info;
        tilesDirPath = info.get(TILE_OUTPUT_DIR_KEY);
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public Map<String, String> getInfo() {
        return infoMap;
    }

    public boolean isTileComputed(int x, int y) {
        long lastModificationTime = Long.parseLong(infoMap.get(PictureTilesImpl.LAST_MODIFICATION_DATE_KEY));
        String tileFileName = StringMaker.getTileFileName(x, y, infoMap.get(TILES_PREFIX_KEY),
                infoMap.get(TILES_SUFFIX_KEY), lastModificationTime);
        File imageFile = new File(tilesDirPath + tileFileName);
        return imageFile.exists();
    }

    public Blob getTile(int x, int y) throws IOException {
        String imageFilePath = getTileFilePath(x, y);
        File imageFile = new File(imageFilePath);
        if (imageFile.exists())
            return Blobs.createBlob(imageFile);
        else {
            PictureTilingService pts = Framework.getService(PictureTilingService.class);
            pts.completeTiles(this, x, y);
            imageFile = new File(imageFilePath);
            if (imageFile.exists())
                return Blobs.createBlob(imageFile);
            else
                throw new NuxeoException("Unable to get Tile");
        }
    }

    public String getTileFilePath(int x, int y) {
        long lastModificationTime = Long.parseLong(infoMap.get(PictureTilesImpl.LAST_MODIFICATION_DATE_KEY));
        String tileFileName = StringMaker.getTileFileName(x, y, infoMap.get(TILES_PREFIX_KEY),
                infoMap.get(TILES_SUFFIX_KEY), lastModificationTime);
        String imageFilePath = new Path(tilesDirPath).append(tileFileName).toString();
        return imageFilePath;
    }

    public int getMaxTiles() {
        String MT = infoMap.get(MAX_TILES_KEY);
        if (MT == null) {
            return 0;
        }
        return Integer.parseInt(MT);
    }

    public int getTilesWidth() {
        String TW = infoMap.get(TILES_WIDTH_KEY);
        if (TW == null) {
            return 0;
        }
        return Integer.parseInt(TW);
    }

    public int getTilesHeight() {
        String TH = infoMap.get(TILES_HEIGHT_KEY);
        if (TH == null) {
            return 0;
        }
        return Integer.parseInt(TH);
    }

    public String getTilesPath() {
        return tilesDirPath;
    }

    public int getXTiles() {
        String XT = infoMap.get(X_TILES_KEY);
        if (XT == null) {
            return 0;
        }
        return Integer.parseInt(XT);
    }

    public int getYTiles() {
        String YT = infoMap.get(Y_TILES_KEY);
        if (YT == null) {
            return 0;
        }
        return Integer.parseInt(YT);
    }

    public float getZoomfactor() {

        float oWith = originalImageInfo.getWidth();
        float tWith = getXTiles() * getTilesWidth();
        float oHeight = originalImageInfo.getHeight();
        float tHeight = getYTiles() * getTilesHeight();
        return tWith / oWith < tHeight / oHeight ? tWith / oWith : tHeight / oHeight;
    }

    public void release() {
        long lastModificationTime = Long.parseLong(infoMap.get(PictureTilesImpl.LAST_MODIFICATION_DATE_KEY));
        for (int x = 0; x < getXTiles(); x++) {
            for (int y = 0; y < getYTiles(); y++) {
                String tileFileName = StringMaker.getTileFileName(x, y, infoMap.get(TILES_PREFIX_KEY),
                        infoMap.get(TILES_SUFFIX_KEY), lastModificationTime);
                File img = new File(tilesDirPath + tileFileName);
                if (img.exists())
                    img.delete();
            }
        }
    }

    public ImageInfo getSourceImageInfo() {
        return sourceImageInfo;
    }

    public void setSourceImageInfo(ImageInfo imageInfo) {
        this.sourceImageInfo = imageInfo;
    }

    public String getTileFormatCacheKey() {
        return StringMaker.getTileFormatString(getTilesWidth(), getTilesHeight(), getMaxTiles());
    }

    public ImageInfo getOriginalImageInfo() {
        return originalImageInfo;
    }

    public void setOriginalImageInfo(ImageInfo imageInfo) {
        originalImageInfo = imageInfo;
    }

}
