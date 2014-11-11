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

import org.nuxeo.ecm.automation.client.jaxrs.model.Document;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Parameter;
import org.nuxeo.ecm.shell.ShellConsole;
import org.nuxeo.ecm.shell.automation.DocumentHelper;
import org.nuxeo.ecm.shell.automation.RemoteContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "pwd", help = "Print the current context document")
public class Pwd implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Parameter(name = "-s", hasValue = false, help = "Use this flag to show the context documents stack")
    protected boolean stack = false;

    public void run() {
        ShellConsole console = ctx.getShell().getConsole();

        if (stack) {
            for (Document doc : ctx.getStack()) {
                DocumentHelper.printPath(console, doc);
            }
        } else {
            DocumentHelper.printPath(console, ctx.getDocument());
        }
    }
}
