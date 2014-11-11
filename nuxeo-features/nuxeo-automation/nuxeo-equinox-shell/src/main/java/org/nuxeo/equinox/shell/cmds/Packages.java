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
import org.nuxeo.equinox.shell.Connector;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "packages", help = "Display imported/exported package details")
public class Packages implements Runnable {

    @Context
    protected Shell shell;

    @Context
    protected Connector connector;

    @Argument(name = "bundle", index = 0, required = false, help = "bundle name or uid")
    protected String name;

    @Argument(name = "pkgname", index = 1, required = false, help = "package name")
    protected String pckname;

    public void run() {
        String cmd = "packages";
        if (name != null) {
            cmd += " " + name;
        }
        if (pckname != null) {
            cmd += " " + pckname;
        }
        shell.getConsole().println(connector.send(cmd));
    }
}
