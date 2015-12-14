/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.picture.magick.utils;

import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.picture.magick.MagickExecutor;

/**
 * Unit command to crop and resize an picture.
 *
 * @author tiry
 */
public class ImageCropperAndResizer extends MagickExecutor {

    public static final String DEFAULT_MAP_COMPONENTS = "rgb";

    public static void cropAndResize(String inputFilePath, String outputFilePath, int tileWidth, int tileHeight,
            int offsetX, int offsetY, int targetWidth, int targetHeight) throws CommandNotAvailable, CommandException {
        cropAndResize(inputFilePath, outputFilePath, tileWidth, tileHeight, offsetX, offsetY, targetWidth,
                targetHeight, null);
    }

    /** @since 5.9.5. */
    public static void cropAndResize(String inputFilePath, String outputFilePath, int tileWidth, int tileHeight,
            int offsetX, int offsetY, int targetWidth, int targetHeight, String mapComponents)
            throws CommandNotAvailable, CommandException {

        if (mapComponents == null) {
            mapComponents = DEFAULT_MAP_COMPONENTS;
        }

        CmdParameters params = new CmdParameters();
        params.addNamedParameter("tileWidth", String.valueOf(tileWidth));
        params.addNamedParameter("tileHeight", String.valueOf(tileHeight));
        params.addNamedParameter("offsetX", String.valueOf(offsetX));
        params.addNamedParameter("offsetY", String.valueOf(offsetY));
        params.addNamedParameter("targetWidth", String.valueOf(targetWidth));
        params.addNamedParameter("targetHeight", String.valueOf(targetHeight));
        params.addNamedParameter("inputFilePath", inputFilePath);
        params.addNamedParameter("outputFilePath", outputFilePath);
        params.addNamedParameter("mapComponents", mapComponents);
        ExecResult res = execCommand("cropAndResize", params);
        if (!res.isSuccessful()) {
            throw res.getError();
        }
    }

}
