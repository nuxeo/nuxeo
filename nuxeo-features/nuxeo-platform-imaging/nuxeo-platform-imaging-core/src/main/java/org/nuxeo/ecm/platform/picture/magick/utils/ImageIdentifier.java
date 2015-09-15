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
        CommandLineExecutorService cles = Framework.getLocalService(CommandLineExecutorService.class);
        CmdParameters params = cles.getDefaultCmdParameters();
        params.addNamedParameter("inputFilePath", inputFilePath);
        return cles.execCommand("identify", params);
    }

}
