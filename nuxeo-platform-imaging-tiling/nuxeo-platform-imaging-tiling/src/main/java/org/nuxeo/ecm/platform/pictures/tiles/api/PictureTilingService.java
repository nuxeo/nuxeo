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

import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.pictures.tiles.api.imageresource.ImageResource;

/**
 * Interface for the Service that generate the PictureTiles.
 *
 * @author tiry
 */
public interface PictureTilingService {

    /**
     * Gets the picture tiles from a blob.
     *
     * @param blob
     * @param tileWidth
     * @param tileHeight
     * @param maxTiles
     * @return
     * @throws ClientException
     */
    @Deprecated
    PictureTiles getTilesFromBlob(Blob blob, int tileWidth, int tileHeight,
            int maxTiles) throws ClientException;

    /**
     * Gets the picture tiles from a blob Tiles are lazily generated.
     *
     * @param blob
     * @param tileWidth
     * @param tileHeight
     * @param maxTiles
     * @param progressive
     * @return
     * @throws ClientException
     */
    @Deprecated
    PictureTiles getTilesFromBlob(Blob blob, int tileWidth, int tileHeight,
            int maxTiles, int xCenter, int yCenter, boolean fullGeneration)
            throws ClientException;

    PictureTiles completeTiles(PictureTiles existingTiles, int xCenter,
            int yCenter) throws ClientException;

    PictureTiles getTiles(ImageResource resource, int tileWidth,
            int tileHeight, int maxTiles) throws ClientException;

    PictureTiles getTiles(ImageResource resource, int tileWidth,
            int tileHeight, int maxTiles, int xCenter, int yCenter,
            boolean fullGeneration) throws ClientException;

    void setWorkingDirPath(String path);

    Map<String, String> getBlobProperties();

    String getBlobProperty(String docType);

    String getBlobProperty(String docType, String defaultValue);

    void removeCacheEntry(ImageResource resource) throws ClientException;

}
