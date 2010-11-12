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

import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.CommandType;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellConsole;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "commands", aliases = "cmds", help = "Print a list of available commands")
public class Commands implements Runnable {

    @Context
    protected Shell shell;

    public void run() {
        CommandType[] cmds = shell.getActiveRegistry().getCommandTypes();
        ShellConsole console = shell.getConsole();
        for (CommandType type : cmds) {
            console.println(type.getName());
        }
    }

}
