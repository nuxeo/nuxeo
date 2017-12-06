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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.magick.MagickExecutor;
import org.nuxeo.runtime.api.Framework;

/**
 * Unit command to extract information from a picture file.
 *
 * @author tiry
 */
public class ImageIdentifier extends MagickExecutor {

    private static final Log log = LogFactory.getLog(ImageIdentifier.class);

    public static ImageInfo getInfo(String inputFilePath) throws CommandNotAvailable, CommandException {

        ExecResult result = getIdentifyResult(inputFilePath);
        if (!result.isSuccessful()) {
            log.debug("identify failed for file: " + inputFilePath);
            throw result.getError();
        }
        String out = result.getOutput().get(result.getOutput().size() > 1 ? result.getOutput().size() - 1 : 0);
        String[] res = out.split(" ");

        return new ImageInfo(res[1], res[2], res[0], res[3], res[4], inputFilePath);
    }

    public static ExecResult getIdentifyResult(String inputFilePath) throws CommandNotAvailable {
        CommandLineExecutorService cles = Framework.getService(CommandLineExecutorService.class);
        CmdParameters params = cles.getDefaultCmdParameters();
        params.addNamedParameter("inputFilePath", inputFilePath);
        return cles.execCommand("identify", params);
    }

}
