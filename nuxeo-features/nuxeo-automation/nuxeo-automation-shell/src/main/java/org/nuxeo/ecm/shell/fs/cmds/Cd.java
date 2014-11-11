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

import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.fs.FileSystem;
import org.nuxeo.ecm.shell.fs.FolderCompletor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "cd", help = "Change the local working directory")
public class Cd implements Runnable {

    @Context
    protected Shell shell;

    @Argument(name = "file", index = 0, required = true, completor = FolderCompletor.class, help = "A local directory to change to")
    protected File file;

    public void run() {
        if (!file.isDirectory()) {
            shell.getConsole().println("Not a directory!");
        } else {
            shell.getContextObject(FileSystem.class).cd(file);
        }
    }
}
