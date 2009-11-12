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
package org.nuxeo.ecm.platform.pictures.tiles.tilers;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;

/**
 *
 * Interface for class that can Tile a Picture
 *
 * @author tiry
 *
 */
public interface PictureTiler {

    boolean needsSync();

    String getName();

    PictureTiles getTilesFromFile(ImageInfo input, String outputDirPath,
            int tileWidth, int tileHeight, int maxTiles, int xCenter,
            int yCenter, long lastModificationTime, boolean fullGeneration) throws ClientException;
}
