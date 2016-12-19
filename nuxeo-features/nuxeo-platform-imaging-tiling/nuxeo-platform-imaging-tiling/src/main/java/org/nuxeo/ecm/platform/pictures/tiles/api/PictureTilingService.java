/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.pictures.tiles.api;

import java.util.Map;

import org.nuxeo.ecm.platform.pictures.tiles.api.imageresource.ImageResource;

/**
 * Interface for the Service that generate the PictureTiles.
 *
 * @author tiry
 */
public interface PictureTilingService {

    PictureTiles completeTiles(PictureTiles existingTiles, int xCenter, int yCenter);

    PictureTiles getTiles(ImageResource resource, int tileWidth, int tileHeight, int maxTiles);

    PictureTiles getTiles(ImageResource resource, int tileWidth, int tileHeight, int maxTiles, int xCenter, int yCenter,
            boolean fullGeneration);

    void setWorkingDirPath(String path);

    Map<String, String> getBlobProperties();

    String getBlobProperty(String docType);

    String getBlobProperty(String docType, String defaultValue);

    void removeCacheEntry(ImageResource resource);

}
