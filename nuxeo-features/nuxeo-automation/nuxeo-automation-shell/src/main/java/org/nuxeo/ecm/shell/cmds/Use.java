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

import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.CommandRegistry;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Shell;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "use", help = "Switch the current command namespace. If no namespace is specified the current namepsace name is printed.")
public class Use implements Runnable {

    @Context
    protected Shell shell;

    @Argument(name = "name", index = 0, required = false, help = "The command namespace to use")
    protected String name;

    public void run() {
        if (name != null) {
            CommandRegistry old = shell.setActiveRegistry(name);
            if (old != null) {
                shell.getConsole().println(old.getName() + " -> " + name);
            }
        } else {
            shell.getConsole().println(shell.getActiveRegistry().getName());
        }
    }
}
