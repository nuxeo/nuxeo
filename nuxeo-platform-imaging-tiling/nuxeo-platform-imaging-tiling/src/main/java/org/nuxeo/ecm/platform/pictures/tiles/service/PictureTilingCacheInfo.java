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
package org.nuxeo.ecm.platform.pictures.tiles.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageIdentifier;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageResizer;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;
import org.nuxeo.ecm.platform.pictures.tiles.helpers.StringMaker;

/**
 * Wraps the needed information about tiling a picture in order to manage cache.
 * This includes : - original image stored on file system - reduced images if
 * any - tiles already generated
 *
 * @author tiry
 *
 */
public class PictureTilingCacheInfo {

    public static int SHRINK_DOWN_LIMIT_PX = 2000;

    private static final Log log = LogFactory.getLog(PictureTilingCacheInfo.class);

    protected String cacheKey;

    protected String workingDir;

    protected ImageInfo originalPictureInfos;

    protected Map<Integer, ImageInfo> shrinkedImages;

    protected List<Integer> shrinkedImagesWidths;

    protected Map<String, PictureTiles> tilesSet;

    protected String syncShrink = "oneOncePerInstance";

    protected Date lastAccessTime;

    protected void updateAccessTime() {
        lastAccessTime = new Date();
    }

    public Date getLastAccessedTime() {
        return lastAccessTime;
    }

    protected long getFileSize(String path) {
        if (path == null)
            return 0;
        File file = new File(path);
        if (file.exists()) {
            return file.length();
        } else
            return 0;
    }

    public long getDiskSpaceUsageInBytes() {
        long diskSpaceUsage = 0;

        // original picture
        diskSpaceUsage += getFileSize(originalPictureInfos.getFilePath());

        // shrinked ones
        for (Integer s : shrinkedImages.keySet()) {
            diskSpaceUsage += getFileSize(shrinkedImages.get(s).getFilePath());
        }

        // tiles
        for (String tileDef : tilesSet.keySet()) {
            PictureTiles tiles = tilesSet.get(tileDef);
            File tileDir = new File(tiles.getTilesPath());
            if (tileDir.exists()) {
                for (File tileFile : tileDir.listFiles()) {
                    diskSpaceUsage += tileFile.length();
                }
            }
        }

        return diskSpaceUsage;
    }

    public PictureTilingCacheInfo(String cacheKey, String workingDir,
            String filePath) throws Exception {
        this.cacheKey = cacheKey;
        this.workingDir = workingDir;
        originalPictureInfos = ImageIdentifier.getInfo(filePath);
        shrinkedImages = new HashMap<Integer, ImageInfo>();
        shrinkedImagesWidths = new ArrayList<Integer>();
        tilesSet = new HashMap<String, PictureTiles>();
        updateAccessTime();
    }

    public void addPictureTilesToCache(PictureTiles tiles) {
        tilesSet.put(tiles.getTileFormatCacheKey(), tiles);
        updateAccessTime();
    }

    public PictureTiles getCachedPictureTiles(int tileWidth, int tileHeight,
            int maxTiles) {
        String ptKey = StringMaker.getTileFormatString(tileWidth, tileHeight,
                maxTiles);
        updateAccessTime();
        return tilesSet.get(ptKey);
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public String getOriginalPicturePath() {
        return originalPictureInfos.getFilePath();
    }

    public String getTilingDir(int tileWidth, int tileHeight, int maxTiles) {
        String dirPath = "tiles-" + tileWidth + "-" + tileHeight + "-"
                + maxTiles;
        dirPath = new Path(workingDir).append(dirPath).toString();

        log.debug("Target tiling dir=" + dirPath);
        File dir = new File(dirPath);

        if (!dir.exists()) {
            dir.mkdir();
            dir.deleteOnExit();
        }
        return dirPath;
    }

    public ImageInfo getBestSourceImage(int tileWidth, int tileHeight,
            int maxTiles) {
        updateAccessTime();
        if ("JPEG".equals(originalPictureInfos.getFormat())) {
            // since JPEG supports it we may strip it down

            if ((originalPictureInfos.getHeight() > SHRINK_DOWN_LIMIT_PX)
                    || (originalPictureInfos.getWidth() > SHRINK_DOWN_LIMIT_PX)) {
                int neededWidth = tileWidth * maxTiles;
                int neededHeight = tileHeight * maxTiles;
                int shrinkedWidth = 0;

                // JPG simplification work with 2 factor
                if ((neededHeight > (originalPictureInfos.getHeight() / 2))
                        || (neededWidth > (originalPictureInfos.getWidth() / 2))) {
                    return originalPictureInfos;
                }

                // avoid multiple shrink processing of the same image
                synchronized (syncShrink) {
                    for (Integer swidth : shrinkedImagesWidths) {
                        if (swidth >= neededWidth)
                            shrinkedWidth = swidth;
                        else
                            break;
                    }

                    if (shrinkedWidth > 0) {
                        return shrinkedImages.get(new Integer(shrinkedWidth));
                    } else {
                        String shrinkedImagePath = new Path(workingDir).append(
                                "reduced-" + neededWidth + "x" + neededHeight
                                        + ".jpg").toString();
                        try {
                            ImageInfo shrinked = ImageResizer.resize(
                                    originalPictureInfos.getFilePath(),
                                    shrinkedImagePath, neededWidth,
                                    neededHeight, -1);

                            shrinkedImagesWidths.add(new Integer(
                                    shrinked.getWidth()));
                            Collections.sort(shrinkedImagesWidths);
                            Collections.reverse(shrinkedImagesWidths);

                            shrinkedImages.put(
                                    new Integer(shrinked.getWidth()), shrinked);

                            return shrinked;
                        } catch (Exception e) {
                            return originalPictureInfos;
                        }
                    }
                }
            } else
                return originalPictureInfos;
        } else
            return originalPictureInfos;
    }

    public ImageInfo getOriginalPictureInfos() {
        updateAccessTime();
        return originalPictureInfos;
    }

    public void cleanUp() {
        // original picture
        File orgFile = new File(originalPictureInfos.getFilePath());
        if (orgFile.exists())
            orgFile.delete();

        // shrinked ones
        for (Integer s : shrinkedImages.keySet()) {
            File skFile = new File(shrinkedImages.get(s).getFilePath());
            if (skFile.exists())
                skFile.delete();
        }

        // tiles
        for (String tileDef : tilesSet.keySet()) {
            PictureTiles tiles = tilesSet.get(tileDef);
            File tileDir = new File(tiles.getTilesPath());
            if (tileDir.exists()) {
                for (File tileFile : tileDir.listFiles()) {
                    tileFile.delete();
                }
            }
        }
    }

    public void partialCleanUp(long targetDeltaInKB) {
        long deletedKB = 0;
        // tiles
        for (String tileDef : tilesSet.keySet()) {
            PictureTiles tiles = tilesSet.get(tileDef);
            File tileDir = new File(tiles.getTilesPath());
            if (tileDir.exists()) {
                for (File tileFile : tileDir.listFiles()) {
                    deletedKB += tileFile.length() / 1000;
                    tileFile.delete();
                    if (deletedKB > targetDeltaInKB)
                        return;
                }
            }
        }

        // shrinked ones
        for (Integer s : shrinkedImages.keySet()) {
            File skFile = new File(shrinkedImages.get(s).getFilePath());
            if (skFile.exists()) {
                deletedKB += skFile.length() / 1000;
                skFile.delete();
                if (deletedKB > targetDeltaInKB)
                    return;
            }
        }

        // original picture
        File orgFile = new File(originalPictureInfos.getFilePath());
        if (orgFile.exists())
            orgFile.delete();

    }

}
