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
package org.nuxeo.ecm.platform.pictures.tiles.tilers;

import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;

/**
 * Interface for class that can Tile a Picture
 *
 * @author tiry
 */
public interface PictureTiler {

    boolean needsSync();

    String getName();

    PictureTiles getTilesFromFile(ImageInfo input, String outputDirPath, int tileWidth, int tileHeight, int maxTiles,
            int xCenter, int yCenter, long lastModificationTime, boolean fullGeneration);
}
