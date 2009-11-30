/*
 * (C) Copyright 2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.dam.core.watermark;

import java.io.File;

import org.apache.log4j.Logger;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.picture.magick.MagickExecutor;

/**
 * Unit command to watermark a picture using ImageMagick
 * 
 * @author btatar
 */
public class ImageWatermarker extends MagickExecutor {
    private static Logger log = Logger.getLogger(ImageWatermarker.class);

    public static File watermark(String watermarkFilePath,
            Integer watermarkWidth, Integer watermarkHeight,
            String inputFilePath, String outputFilePath) throws Exception {
        CmdParameters params = new CmdParameters();
        params.addNamedParameter("watermarkFilePath",
                formatFilePath(watermarkFilePath));
        params.addNamedParameter("watermarkWidth",
                String.valueOf(watermarkWidth));
        params.addNamedParameter("watermarkHeight",
                String.valueOf(watermarkHeight));
        params.addNamedParameter("inputFilePath", formatFilePath(inputFilePath));
        params.addNamedParameter("outputFilePath",
                formatFilePath(outputFilePath));
        ExecResult result = execCommand("watermark", params);

        log.error("Error apply watermark", result.getError());
        return result.isSuccessful() ? new File(outputFilePath) : null;
    }
}
