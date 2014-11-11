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
import java.io.FileOutputStream;
import java.io.IOException;

import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("append")
public class AppendOperation {

    @XNode("@path")
    protected String path;

    @XNode("@target")
    protected String target;

    @XNode("@appendNewLine")
    protected boolean appendNewLine = true;

    public void run(Installer installer, File bundleDir, File installDir) throws IOException {
        //ctx.getBundle().getEntryPaths(path);
        File src = new File(bundleDir, path);
        if (src.isFile()) {
            String text = FileUtils.readFile(src);
            if (appendNewLine) {
                String crlf = System.getProperty("line.separator");
                text = crlf + text + crlf;
            }
            File file = new File(installDir, target);
            File parent = file.getParentFile();
            if (!parent.isDirectory()) {
                parent.mkdirs();
            }
            boolean append = file.exists();
            FileOutputStream out = new FileOutputStream(file, append);
            try {
                out.write(text.getBytes());
            } finally {
                out.close();
            }
        } else {
            installer.logWarning("Could not find path: "+path+" to append");
        }
    }

}
