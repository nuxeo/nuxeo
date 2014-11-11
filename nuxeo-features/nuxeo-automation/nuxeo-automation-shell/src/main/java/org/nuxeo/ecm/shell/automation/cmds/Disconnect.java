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
package org.nuxeo.ecm.shell.automation.cmds;

import org.nuxeo.ecm.automation.client.jaxrs.Session;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.automation.AutomationShell;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "disconnect", help = "Close current connection to server. If not connected nothing is done.")
public class Disconnect implements Runnable {

    @Context
    protected Shell shell;

    public void run() {
        shell.removeContextObject(Session.class);
        ((AutomationShell) shell).disconnect();
    }

}
