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
 *     troger
 *
 */

package org.nuxeo.ecm.platform.picture.magick.utils;

import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandException;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.picture.magick.MagickExecutor;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class ImageConverter extends MagickExecutor {

    public static void convert(String inputFilePath, String outputFilePath) throws CommandNotAvailable,
            CommandException {
        CmdParameters params = new CmdParameters();
        params.addNamedParameter("inputFilePath", inputFilePath);
        params.addNamedParameter("outputFilePath", outputFilePath);
        ExecResult res = execCommand("converter", params);
        if (!res.isSuccessful()) {
            throw res.getError();
        }
    }

}
