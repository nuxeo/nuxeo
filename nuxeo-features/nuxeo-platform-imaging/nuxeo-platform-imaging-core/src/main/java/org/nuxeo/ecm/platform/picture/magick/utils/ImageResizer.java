/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.picture.magick.utils;

import static org.nuxeo.ecm.platform.picture.api.ImagingConvertConstants.JPEG_CONVERSATION_FORMAT;

import java.io.File;

import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.magick.MagickExecutor;
import org.nuxeo.runtime.api.Framework;

/**
 * Unit command to extract a simplified view of a JPEG file using ImageMagick = extract the needed picture information
 * to reach the target definition level
 *
 * @author tiry
 */
public class ImageResizer extends MagickExecutor {

    public static ImageInfo resize(String inputFile, String outputFile, int targetWidth, int targetHeight,
            int targetDepth) throws CommandNotAvailable, CommandException {
        if (targetDepth == -1) {
            targetDepth = ImageIdentifier.getInfo(inputFile).getDepth();
        }
        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        CmdParameters params = cles.getDefaultCmdParameters();
        params.addNamedParameter("targetWidth", String.valueOf(targetWidth));
        params.addNamedParameter("targetHeight", String.valueOf(targetHeight));
        params.addNamedParameter("inputFilePath", inputFile);
        params.addNamedParameter("outputFilePath", outputFile);
        params.addNamedParameter("targetDepth", String.valueOf(targetDepth));
        String commandName = "resizer";
        // hack to manage jpeg default background
        if (outputFile.endsWith(JPEG_CONVERSATION_FORMAT)) {
            commandName = "jpegResizer";
        }
        ExecResult res = cles.execCommand(commandName, params);
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
