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
package org.nuxeo.ecm.platform.pictures.tiles.helpers;

/**
 *
 * Simple helper to generate the directory names in the cache structure
 *
 * @author tiry
 *
 */
public class StringMaker {

    public static String getTileFormatString(int tileWidth, int tileHeight,
            int maxTiles) {
        return tileWidth + "x" + tileHeight + "x" + maxTiles;
    }

    public static String getTileFileName(int x, int y, String prefix,
            String suffix, long lastModificationTime) {
        if (prefix == null) {
            prefix = "tile";
        }
        if (suffix == null) {
            suffix = ".jpg";
        }

        return prefix + x + "-" + y + '-' + lastModificationTime + suffix;
    }

    public static String getTileFileName(int x, int y, long lastModificationDate) {
        return getTileFileName(x, y, "tile", ".jpg", lastModificationDate);
    }

}
