/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.shell.fs.cmds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Shell;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "touch", help = "Touch a file")
public class Touch implements Runnable {

    @Context
    protected Shell shell;

    @Argument(name = "file", index = 0, required = true, help = "The file to touch")
    protected File file;

    public void run() {
        file.getParentFile().mkdirs();
        if (!file.exists()) {
            OutputStream out = null;
            try {
                try {
                    out = new FileOutputStream(file);
                } finally {
                    if (out != null) {
                        out.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            file.setLastModified(System.currentTimeMillis());
        }
    }

}
