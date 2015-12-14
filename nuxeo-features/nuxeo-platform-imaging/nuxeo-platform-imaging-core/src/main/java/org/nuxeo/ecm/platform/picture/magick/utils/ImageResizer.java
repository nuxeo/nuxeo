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

import java.io.File;

import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.magick.MagickExecutor;

/**
 * Unit command to extract a simplified view of a JPEG file using ImageMagick =
 * extract the needed picture information to reach the target definition level
 *
 * @author tiry
 */
public class ImageResizer extends MagickExecutor {

    public static ImageInfo resize(String inputFile, String outputFile,
            int targetWidth, int targetHeight, int targetDepth)
            throws CommandNotAvailable, CommandException {
        if (targetDepth == -1) {
            targetDepth = ImageIdentifier.getInfo(inputFile).getDepth();
        }
        CmdParameters params = new CmdParameters();
        params.addNamedParameter("targetWidth", String.valueOf(targetWidth));
        params.addNamedParameter("targetHeight", String.valueOf(targetHeight));
        params.addNamedParameter("inputFilePath", inputFile);
        params.addNamedParameter("outputFilePath", outputFile);
        params.addNamedParameter("targetDepth", String.valueOf(targetDepth));
        ExecResult res = execCommand("resizer", params);
        if (!res.isSuccessful()) {
            throw res.getError();
        }
        if (new File(outputFile).exists()) {
            return ImageIdentifier.getInfo(outputFile);
        } else {
            return null;
        }
    }

}
