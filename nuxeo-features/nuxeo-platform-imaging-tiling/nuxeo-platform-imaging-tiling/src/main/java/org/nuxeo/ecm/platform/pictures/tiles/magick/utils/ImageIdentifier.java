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
package org.nuxeo.ecm.platform.pictures.tiles.magick.utils;

import org.nuxeo.ecm.platform.pictures.tiles.magick.ExecResult;
import org.nuxeo.ecm.platform.pictures.tiles.magick.MagickExecutor;

/**
 * Unit command to extract information from a picture file
 *
 *
 * @author tiry
 *
 */
public class ImageIdentifier extends MagickExecutor {

    public static ImageInfo getInfo(String inputFilePath) throws Exception {
        String cmd = " -ping -format '%m %w %h' ";
        if (isWindows()) {
            cmd = " -ping -format \"%m %w %h\" ";
        }
        ExecResult result = execCmd(identifyCmd() + cmd
                + formatFilePath(inputFilePath));

        String out = result.getOutput().get(0);
        String res[] = out.split(" ");

        return new ImageInfo(res[1], res[2], res[0], inputFilePath);
    }

}
