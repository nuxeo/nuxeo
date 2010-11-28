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
import org.nuxeo.ecm.shell.Parameter;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.equinox.shell.BundleNameCompletor;
import org.nuxeo.equinox.shell.Connector;
import org.nuxeo.equinox.shell.StateCompletor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "ss", help = "Display installed bundles (short status)")
public class Ss implements Runnable {

    @Context
    protected Shell shell;

    @Context
    protected Connector connector;

    @Parameter(name = "-s", hasValue = true, completor = StateCompletor.class, help = "comma separated list of bundle states")
    protected String status;

    @Argument(name = "name", index = 0, completor = BundleNameCompletor.class, help = "segment of bundle symbolic name. If not specified all bundles are listed")
    protected String name;

    public void run() {
        String cmd = "ss";
        if (status != null) {
            cmd += " -s " + status;
        }
        if (name != null) {
            cmd += " " + name;
        }
        shell.getConsole().println(connector.send(cmd));
    }
}
