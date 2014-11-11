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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.picture.magick.utils;

import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.picture.magick.MagickExecutor;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class ImageConverter extends MagickExecutor {

    public static void convert(String inputFilePath, String outputFilePath)
            throws Exception {
        CmdParameters params = new CmdParameters();
        params.addNamedParameter("inputFilePath", inputFilePath);
        params.addNamedParameter("outputFilePath", outputFilePath);
        execCommand("converter", params);
    }

}
