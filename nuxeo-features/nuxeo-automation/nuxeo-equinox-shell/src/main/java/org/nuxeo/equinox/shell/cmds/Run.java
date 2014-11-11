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
package org.nuxeo.equinox.shell.cmds;

import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.cmds.Interactive;
import org.nuxeo.equinox.shell.Connector;
import org.nuxeo.equinox.shell.EquinoxCommandCompletor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "run", help = "Run an Equinox command")
public class Run implements Runnable {

    @Context
    protected Shell shell;

    @Context
    protected Connector connector;

    @Argument(name = "cmd", index = 0, completor = EquinoxCommandCompletor.class, help = "The command to run. If not specified the list of all commands is displayed.")
    protected String cmd;

    @Argument(name = "cmd arg1", index = 1, help = "Command argument")
    protected String arg1;

    @Argument(name = "cmd arg2", index = 2, help = "Command argument")
    protected String arg2;

    @Argument(name = "cmd arg3", index = 3, help = "Command argument")
    protected String arg3;

    @Argument(name = "cmd arg4", index = 4, help = "Command argument")
    protected String arg4;

    @Argument(name = "cmd arg5", index = 5, help = "Command argument")
    protected String arg5;

    @Argument(name = "cmd arg6", index = 6, help = "Command argument")
    protected String arg6;

    public void run() {
        if (cmd == null) {
            cmd = "help";
        } else {
            cmd = Interactive.getCurrentCmdLine().trim();
            cmd = cmd.substring("run".length()).trim();
        }
        shell.getConsole().println(connector.send(cmd));
    }
}
