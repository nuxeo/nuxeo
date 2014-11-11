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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.install;

import java.io.File;
import java.io.IOException;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("copy")
public class CopyOperation {

    @XNode("@path")
    protected String path;

    @XNode("@target")
    protected String target;

    public void run(Installer installer, File bundleDir, File installDir) throws IOException {
        File dest = new File(installDir, target);
        if (path.endsWith("/*")) {
            dest.mkdirs();
            File file = new File(bundleDir, path.substring(0, path.length()-1));
            FileUtils.copy(file.listFiles(), dest);
        } else {
            File file = new File(bundleDir, path);
            FileUtils.copy(file, dest);
        }
    }

}
