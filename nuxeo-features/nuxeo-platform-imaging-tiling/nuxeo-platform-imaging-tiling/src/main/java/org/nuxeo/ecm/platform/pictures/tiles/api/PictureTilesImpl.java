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
package org.nuxeo.ecm.platform.pictures.tiles.api;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.pictures.tiles.helpers.StringMaker;
import org.nuxeo.runtime.api.Framework;

/**
 *
 * Default implementation for the PictureTiles interface
 *
 * @author tiry
 *
 */
public class PictureTilesImpl implements PictureTiles, Serializable {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static String TILE_OUTPUT_DIR_KEY = "outputDirPath";

    public static String TILE_INPUT_FILE_KEY = "inputFilePath";

    public static String X_TILES_KEY = "XTiles";

    public static String Y_TILES_KEY = "YTiles";

    public static String LAST_MODIFICATION_DATE_KEY = "lastModificationDate";

    // public static String ZoomFactorKey = "ZoomFactor";
    public static String TILES_PREFIX_KEY = "TilesPrefix";

    public static String TILES_SUFFIX_KEY = "TilesSuffix";

    public static String TILES_WIDTH_KEY = "TilesWidth";

    public static String TILES_HEIGHT_KEY = "TilesHeight";

    public static String MAX_TILES_KEY = "MaxTiles";

    public static String PROGRESSIVE_TILING_KEY = "ProgressiveTiling";

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
        long lastModificationTime = Long.parseLong(infoMap.get(
                PictureTilesImpl.LAST_MODIFICATION_DATE_KEY));
        String tileFileName = StringMaker.getTileFileName(x, y,
                infoMap.get(TILES_PREFIX_KEY), infoMap.get(TILES_SUFFIX_KEY), lastModificationTime);
        File imageFile = new File(tilesDirPath + tileFileName);
        return imageFile.exists();
    }

    public Blob getTile(int x, int y) throws Exception {
        String imageFilePath = getTileFilePath(x, y);
        File imageFile = new File(imageFilePath);
        if (imageFile.exists())
            return new FileBlob(imageFile);
        else {
            PictureTilingService pts = Framework.getService(PictureTilingService.class);
            pts.completeTiles(this, x, y);
            imageFile = new File(imageFilePath);
            if (imageFile.exists())
                return new FileBlob(imageFile);
            else
                throw new ClientException("Unable to get Tile");
        }
    }

    public String getTileFilePath(int x, int y) {
        long lastModificationTime = Long.parseLong(infoMap.get(
                PictureTilesImpl.LAST_MODIFICATION_DATE_KEY));
        String tileFileName = StringMaker.getTileFileName(x, y,
                infoMap.get(TILES_PREFIX_KEY), infoMap.get(TILES_SUFFIX_KEY), lastModificationTime);
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
        return tWith / oWith < tHeight / oHeight ? tWith / oWith : tHeight
                / oHeight;
    }

    public void release() {
        long lastModificationTime = Long.parseLong(infoMap.get(
                PictureTilesImpl.LAST_MODIFICATION_DATE_KEY));
        for (int x = 0; x < getXTiles(); x++) {
            for (int y = 0; y < getYTiles(); y++) {
                String tileFileName = StringMaker.getTileFileName(x, y,
                        infoMap.get(TILES_PREFIX_KEY), infoMap.get(TILES_SUFFIX_KEY), lastModificationTime);
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
        return StringMaker.getTileFormatString(getTilesWidth(),
                getTilesHeight(), getMaxTiles());
    }

    public ImageInfo getOriginalImageInfo() {
        return originalImageInfo;
    }

    public void setOriginalImageInfo(ImageInfo imageInfo) {
        originalImageInfo = imageInfo;
    }

}
