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
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Shell;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "version", help = "Print Nuxeo Shell Version")
public class Version implements Runnable {

    @Context
    Shell shell;

    public void run() {
        shell.getConsole().println(getShellVersionMessage());
        shell.getConsole().println(getServerVersionMessage());
    }

    public static String getShellVersionMessage() {
        return "Nuxeo Shell Version: " + getShellVersion();
    }

    public static String getServerVersionMessage() {
        return "Nuxeo Server Minimal Version: " + getServerVersion();
    }

    public static String getShellVersion() {
        return Shell.class.getPackage().getImplementationVersion();
    }

    public static String getServerVersion() {
        return Shell.class.getPackage().getSpecificationVersion();
    }
}
