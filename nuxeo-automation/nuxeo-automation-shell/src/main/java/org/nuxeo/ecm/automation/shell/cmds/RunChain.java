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
package org.nuxeo.ecm.automation.shell.cmds;

import org.nuxeo.ecm.automation.shell.ChainCompletor;
import org.nuxeo.ecm.automation.shell.DocRefCompletor;
import org.nuxeo.ecm.automation.shell.RemoteContext;
import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "run", help = "Run a server automation chain")
public class RunChain implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Argument(name = "chain", index = 0, required = true, completor = ChainCompletor.class, help = "The chain to run")
    protected String chain;

    @Argument(name = "doc", index = 1, required = false, completor = DocRefCompletor.class, help = "A reference to the new context document to use. To use UID references prefix them with 'doc:'.")
    protected String path;

    public void run() {
        // Document doc = ctx.resolveDocument(path);
        // ctx.getSession().newRequest(id)
        // ctx.getSession().execute(request);
    }

}
