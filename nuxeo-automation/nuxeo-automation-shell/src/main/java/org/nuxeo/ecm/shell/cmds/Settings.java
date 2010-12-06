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
package org.nuxeo.ecm.shell.cmds;

import java.io.File;
import java.io.FileWriter;

import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.fs.cmds.Cat;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "settings", help = "Print or modify the shell settings.")
public class Settings implements Runnable {

    @Context
    protected Shell shell;

    @Argument(name = "name", index = 0, required = false, help = "The variable to print or set.")
    protected String name;

    @Argument(name = "value", index = 1, required = false, help = "The variable value to set.")
    protected String value;

    public void run() {
        File file = shell.getSettingsFile();
        try {
            if (name == null) {
                Cat.cat(shell.getConsole(), file);
            } else if (value == null) {
                shell.getConsole().println(
                        (String) shell.getSetting(name, "NULL"));
            } else {
                shell.getSettings().put(name, value);
                FileWriter writer = new FileWriter(file);
                try {
                    shell.getSettings().store(writer, "generated settings file");
                } finally {
                    writer.close();
                }
            }
        } catch (Exception e) {
            throw new ShellException(e);
        }
    }
}
