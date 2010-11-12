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
import java.io.FileInputStream;

import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellConsole;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.fs.FileSystem;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "cat", help = "Print the content of a file")
public class Cat implements Runnable {

    @Context
    protected Shell shell;

    @Argument(name = "file", index = 0, required = false, help = "The file to print")
    protected File file;

    public void run() {
        ShellConsole console = shell.getConsole();
        if (file == null) {
            file = shell.getContextObject(FileSystem.class).pwd();
        }
        if (!file.isFile()) {
            throw new ShellException("Not a file!");
        }
        String content = null;
        try {
            FileInputStream in = new FileInputStream(file);
            try {
                content = FileSystem.readContent(in);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            throw new ShellException(e);
        }
        console.println(content);
    }

}
