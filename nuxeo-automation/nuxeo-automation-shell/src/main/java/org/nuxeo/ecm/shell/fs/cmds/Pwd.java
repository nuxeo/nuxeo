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

import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Parameter;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.fs.FileSystem;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "pwd", help = "Print the local working directory")
public class Pwd implements Runnable {

    @Context
    protected Shell shell;

    @Parameter(name = "-s", hasValue = false, help = "Use this flag to show the working directory stack")
    protected boolean stack = false;

    public void run() {
        FileSystem fs = shell.getContextObject(FileSystem.class);
        if (stack) {
            for (File file : fs.getStack()) {
                shell.getConsole().println(file.getAbsolutePath());
            }
        } else {
            shell.getConsole().println(fs.pwd().getAbsolutePath());
        }
    }
}
