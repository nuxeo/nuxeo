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

import java.io.File;

import org.nuxeo.ecm.platform.pictures.tiles.magick.MagickExecutor;

/**
 *
 * Unit command to extract a simplied view of a JPEG file using ImageMagick =
 * extract the needed picture information to reach the target definition level
 *
 * @author tiry
 *
 */
public class JpegSimplifier extends MagickExecutor {

    public static ImageInfo simplify(String inputFile, String outputFile,
            int targetWidth, int targetHeight) throws Exception {
        execCmd(convertCmd() + " -size " + targetWidth + "x" + targetHeight
                + " " + formatFilePath(inputFile) + " "
                + formatFilePath(outputFile));
        if (new File(outputFile).exists()) {
            return ImageIdentifier.getInfo(outputFile);
        } else
            return null;
    }

}
