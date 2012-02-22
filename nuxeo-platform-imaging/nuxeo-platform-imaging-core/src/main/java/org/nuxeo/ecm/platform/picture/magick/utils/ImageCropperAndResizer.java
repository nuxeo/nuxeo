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
package org.nuxeo.ecm.platform.picture.magick.utils;

import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.picture.magick.MagickExecutor;

/**
 * Unit command to crop and resize an picture.
 *
 * @author tiry
 */
public class ImageCropperAndResizer extends MagickExecutor {

    public static void cropAndResize(String inputFilePath,
            String outputFilePath, int tileWidth, int tileHeight, int offsetX,
            int offsetY, int targetWidth, int targetHeight) throws Exception {
        CmdParameters params = new CmdParameters();
        params.addNamedParameter("tileWidth", String.valueOf(tileWidth));
        params.addNamedParameter("tileHeight", String.valueOf(tileHeight));
        params.addNamedParameter("offsetX", String.valueOf(offsetX));
        params.addNamedParameter("offsetY", String.valueOf(offsetY));
        params.addNamedParameter("targetWidth", String.valueOf(targetWidth));
        params.addNamedParameter("targetHeight", String.valueOf(targetHeight));
        params.addNamedParameter("inputFilePath", inputFilePath);
        params.addNamedParameter("outputFilePath", outputFilePath);
        execCommand("cropAndResize", params);
    }

}
