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
package org.nuxeo.ecm.platform.pictures.tiles.gimp.tiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTilesImpl;
import org.nuxeo.ecm.platform.pictures.tiles.gimp.GimpExecutor;
import org.nuxeo.ecm.platform.pictures.tiles.tilers.BasePictureTiler;

/**
 *
 * Gimp based tiler uses a Gimp specific procedure to generate tiles from an
 * picture file Tiles can be generated one by one or 9 by 9
 *
 * @author tiry
 *
 */
public class GimpTiler extends BasePictureTiler {

    public String getName() {
        return "GimpTiler";
    }

    public boolean needsSync() {
        return true;
    }

    public PictureTiles getTilesFromFile(ImageInfo input, String outputDirPath,
            int tileWidth, int tileHeight, int maxTiles, int xCenter,
            int yCenter, long lastModificationTime, boolean fullGeneration) throws ClientException {

        String inputFilePath = input.getFilePath();

        if (fullGeneration) {
            // force full generation
            xCenter = -1;
            yCenter = -1;
        }

        Map<String, String> result = null;

        if (!outputDirPath.endsWith("/"))
            outputDirPath += "/";

        try {
            result = exec("python-fu-nx-tiles", inputFilePath, outputDirPath,
                    tileWidth, tileHeight, maxTiles, xCenter, yCenter);
        } catch (Exception e) {
            throw new ClientException("Error while calling gimp command line",
                    e);
        }

        result.put(PictureTilesImpl.TILE_INPUT_FILE_KEY, inputFilePath);
        result.put(PictureTilesImpl.TILES_WIDTH_KEY, Integer.toString(tileWidth));
        result.put(PictureTilesImpl.TILES_HEIGHT_KEY, Integer.toString(tileHeight));
        result.put(PictureTilesImpl.MAX_TILES_KEY, Integer.toString(maxTiles));
        result.put(PictureTilesImpl.TILE_OUTPUT_DIR_KEY, outputDirPath);
        result.put(PictureTilesImpl.TILES_PREFIX_KEY, "tile");
        result.put(PictureTilesImpl.TILES_SUFFIX_KEY, ".jpg");
        result.put(PictureTilesImpl.PROGRESSIVE_TILING_KEY,
                Boolean.toString(!fullGeneration));

        PictureTiles tiles = null;
        tiles = new PictureTilesImpl(result);
        return tiles;

    }

    protected Map<String, String> exec(String procName, String inputFilePath,
            String outputPath, int tileX, int tileY, int nbTiles,
            int centerXTile, int centerYTile) throws Exception {

        List<Object> params = new ArrayList<Object>();

        params.add(inputFilePath);
        params.add(new Integer(tileX));
        params.add(new Integer(tileY));
        params.add(new Integer(nbTiles));
        params.add(outputPath);
        params.add(new Integer(centerXTile));
        params.add(new Integer(centerYTile));

        return GimpExecutor.exec(procName, params);
    }

}
