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

import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.picture.magick.MagickExecutor;
import org.nuxeo.runtime.api.Framework;

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

        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        CmdParameters params = cles.getDefaultCmdParameters();
        params.addNamedParameter("tileWidth", String.valueOf(tileWidth));
        params.addNamedParameter("tileHeight", String.valueOf(tileHeight));
        params.addNamedParameter("offsetX", String.valueOf(offsetX));
        params.addNamedParameter("offsetY", String.valueOf(offsetY));
        params.addNamedParameter("targetWidth", String.valueOf(targetWidth));
        params.addNamedParameter("targetHeight", String.valueOf(targetHeight));
        params.addNamedParameter("inputFilePath", inputFilePath);
        params.addNamedParameter("outputFilePath", outputFilePath);
        params.addNamedParameter("mapComponents", mapComponents);
        ExecResult res = cles.execCommand("cropAndResize", params);
        if (!res.isSuccessful()) {
            throw res.getError();
        }
    }

}
