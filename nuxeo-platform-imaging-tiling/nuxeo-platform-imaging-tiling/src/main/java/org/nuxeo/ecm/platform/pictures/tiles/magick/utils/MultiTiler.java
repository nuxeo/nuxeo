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
package org.nuxeo.ecm.platform.pictures.tiles.magick.utils;

import org.nuxeo.ecm.platform.pictures.tiles.magick.MagickExecutor;

/**
 * Unit command to generate several tiles at once using ImageMagick
 *
 * @author tiry
 *
 */
public class MultiTiler extends MagickExecutor {

    public static void tile(String inputFilePath, String outputPath,
            int tileWidth, int tileHeight) throws Exception {
        StringBuffer sb = new StringBuffer();
        sb.append(convertCmd());
        sb.append(" ");
        sb.append(formatFilePath(inputFilePath));
        sb.append(" -crop ");
        sb.append(tileWidth);
        sb.append("x");
        sb.append(tileHeight);
        sb.append(" +repage ");
        sb.append(formatFilePath(outputPath + "tiles_%02d.jpeg"));
        execCmd(sb.toString());
    }
}
