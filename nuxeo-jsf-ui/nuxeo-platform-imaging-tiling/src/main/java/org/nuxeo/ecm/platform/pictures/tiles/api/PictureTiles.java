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

import java.io.IOException;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;

/**
 * Interface for a collection of Tiles in a given format. Wraps underlying calls to the TilingService
 *
 * @author tiry
 */
public interface PictureTiles {

    Map<String, String> getInfo();

    String getTilesPath();

    Blob getTile(int x, int y) throws IOException;

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
