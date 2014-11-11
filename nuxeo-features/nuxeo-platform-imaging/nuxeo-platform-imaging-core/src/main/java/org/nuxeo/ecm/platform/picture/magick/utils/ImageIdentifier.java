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
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.picture.magick.MagickExecutor;

/**
 * Unit command to extract information from a picture file.
 *
 * @author tiry
 */
public class ImageIdentifier extends MagickExecutor {

    public static ImageInfo getInfo(String inputFilePath)
            throws CommandNotAvailable {
        CmdParameters params = new CmdParameters();
        params.addNamedParameter("inputFilePath", inputFilePath);
        ExecResult result = execCommand("identify", params);

        String out = result.getOutput().get(
                result.getOutput().size() > 1 ? result.getOutput().size() - 1
                        : 0);
        String[] res = out.split(" ");

        return new ImageInfo(res[1], res[2], res[0], res[res.length - 1],
                inputFilePath);
    }

}
