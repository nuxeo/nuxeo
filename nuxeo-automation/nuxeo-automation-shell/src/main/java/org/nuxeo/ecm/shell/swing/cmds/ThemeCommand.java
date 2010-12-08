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
package org.nuxeo.ecm.shell.swing.cmds;

import jline.SimpleCompletor;

import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.swing.Console;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "theme", help = "Modify the theme used by the shell. This command is available only in UI mode.")
public class ThemeCommand implements Runnable {

    @Context
    protected Shell shell;

    @Context
    protected Console console;

    @Argument(name = "name", index = 0, required = false, completor = ThemeCompletor.class, help = "The theme name to set. If not specified the current theme is printed.")
    protected String name;

    public void run() {
        try {
            if (name != null) {
                shell.setSetting("theme", name);
                console.loadDefaultTheme(shell);
            } else {
                shell.getConsole().println(shell.getSetting("theme", "Default"));
            }
        } catch (Exception e) {
            throw new ShellException(e);
        }
    }

    public static class ThemeCompletor extends SimpleCompletor {
        public ThemeCompletor() {
            super(new String[] { "Default", "Linux", "White", "Custom" });
        }
    }
}
