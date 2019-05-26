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
package org.nuxeo.ecm.platform.pictures.tiles.helpers;

/**
 * Simple helper to generate the directory names in the cache structure
 *
 * @author tiry
 */
public class StringMaker {

    public static String getTileFormatString(int tileWidth, int tileHeight, int maxTiles) {
        return tileWidth + "x" + tileHeight + "x" + maxTiles;
    }

    public static String getTileFileName(int x, int y, String prefix, String suffix, long lastModificationTime) {
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
