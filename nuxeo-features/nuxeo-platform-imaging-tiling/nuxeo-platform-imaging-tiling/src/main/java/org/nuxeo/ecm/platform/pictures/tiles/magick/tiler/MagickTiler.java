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
package org.nuxeo.ecm.platform.pictures.tiles.magick.tiler;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageCropperAndResizer;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilesImpl;
import org.nuxeo.ecm.platform.pictures.tiles.helpers.StringMaker;
import org.nuxeo.ecm.platform.pictures.tiles.tilers.PictureTiler;

/**
 *
 * ImageMagic based Tiler Uses several ImageMagick command lines to extract a
 * tile form a picture file
 *
 * @author tiry
 *
 */
public class MagickTiler implements PictureTiler {

    private static final Log log = LogFactory.getLog(MagickTiler.class);

    public boolean needsSync() {
        return false;
    }

    public String getName() {
        return "MagicTiler";
    }

    static int[] computeCropCoords(ImageInfo input, int maxTiles, int tileWidth,
            int tileHeight, int xCenter, int yCenter) {
        int startX = 0;
        int startY = 0;
        int cropWidth = 0;
        int cropHeight = 0;
        int ntx = 0;
        int nty = 0;

        if (input.getWidth() > input.getHeight()) {
            cropWidth = input.getWidth() / maxTiles;
            if ((input.getWidth() % maxTiles) > 0) {
                cropWidth += 1;
            }
            Double exactCropHeight = (new Double(tileHeight)) / tileWidth
                    * cropWidth;
            if (exactCropHeight - exactCropHeight.intValue() > 0) {
                cropHeight = exactCropHeight.intValue() + 1;
            } else {
                cropHeight = exactCropHeight.intValue();
            }
            ntx = maxTiles;
            nty = input.getHeight() / cropHeight;
            if ((input.getHeight() % cropHeight) > 0) {
                nty += 1;
            }
        } else {
            cropHeight = input.getHeight() / maxTiles;
            if ((input.getHeight() % maxTiles) > 0) {
                cropHeight += 1;
            }
            Double exactCropWidth = (new Double(tileWidth)) / tileHeight
                    * cropHeight;
            if (exactCropWidth - exactCropWidth.intValue() > 0) {
                cropWidth = exactCropWidth.intValue() + 1;
            } else {
                cropWidth = exactCropWidth.intValue();
            }
            nty = maxTiles;
            ntx = input.getWidth() / cropWidth;
            if ((input.getWidth() % cropWidth) > 0) {
                ntx += 1;
            }
        }

        startX = xCenter * cropWidth;
        startY = yCenter * cropHeight;

        double widthRatio = new Double(tileWidth) / cropWidth;
        double heightRatio = new Double(tileHeight) / cropHeight;

        if (xCenter == ntx - 1) {
            cropWidth = input.getWidth() - (xCenter) * cropWidth;
            tileWidth = (int) Math.round(cropWidth * widthRatio);
        }
        if (yCenter == nty - 1) {
            cropHeight = input.getHeight() - (yCenter) * cropHeight;
            tileHeight = (int) Math.round(cropHeight * heightRatio);
        }

        int[] result = { startX, startY, cropWidth, cropHeight, ntx, nty,
                tileWidth, tileHeight };

        return result;
    }

    public PictureTiles getTilesFromFile(ImageInfo input, String outputDirPath,
            int tileWidth, int tileHeight, int maxTiles, int xCenter,
            int yCenter, long lastModificationTime, boolean fullGeneration) throws ClientException {

        int[] cropCoords = computeCropCoords(input, maxTiles, tileWidth,
                tileHeight, xCenter, yCenter);

        String fileName = StringMaker.getTileFileName(xCenter, yCenter, lastModificationTime);
        String outputFilePath = new Path(outputDirPath).append(fileName).toString();

        try {
            ImageCropperAndResizer.cropAndResize(input.getFilePath(),
                    outputFilePath, cropCoords[2], cropCoords[3],
                    cropCoords[0], cropCoords[1], cropCoords[6], cropCoords[7]);
        } catch (Exception e) {
            throw new ClientException(e);
        }

        Map<String, String> infoMap = new HashMap<String, String>();
        infoMap.put(PictureTilesImpl.TILE_OUTPUT_DIR_KEY, outputDirPath);
        infoMap.put(PictureTilesImpl.X_TILES_KEY, Integer.toString(cropCoords[4]));
        infoMap.put(PictureTilesImpl.Y_TILES_KEY, Integer.toString(cropCoords[5]));

        return new PictureTilesImpl(infoMap);
    }

}
