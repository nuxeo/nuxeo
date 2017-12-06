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
 *     btatar
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
 * Unit command to rotate a picture.
 *
 * @author btatar
 */
public class ImageRotater extends MagickExecutor {

    public static void rotate(String inputFile, String outputFile, int angle) throws CommandNotAvailable,
            CommandException {
        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        CmdParameters params = cles.getDefaultCmdParameters();
        params.addNamedParameter("angle", String.valueOf(angle));
        params.addNamedParameter("inputFilePath", inputFile);
        params.addNamedParameter("outputFilePath", outputFile);
        ExecResult result = cles.execCommand("rotate", params);
        if (!result.isSuccessful()) {
            throw result.getError();
        }
    }

}
