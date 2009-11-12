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
import org.nuxeo.ecm.platform.picture.api.ImageInfo;

/**
 * Interface for a collection of Tiles in a given format. Wraps underlying calls
 * to the TilingService
 *
 * @author tiry
 *
 */
public interface PictureTiles {

    Map<String, String> getInfo();

    String getTilesPath();

    Blob getTile(int x, int y) throws Exception;

    float getZoomfactor();

    int getXTiles();

    int getYTiles();

    void release();

    int getTilesHeight();

    int getTilesWidth();

    int getMaxTiles();

    String getCacheKey();

    void setCacheKey(String cacheKey);

    ImageInfo getSourceImageInfo();

    void setSourceImageInfo(ImageInfo imageInfo);

    ImageInfo getOriginalImageInfo();

    void setOriginalImageInfo(ImageInfo imageInfo);

    String getTileFormatCacheKey();

    boolean isTileComputed(int x, int y);

}
