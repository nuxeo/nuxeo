/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.chemistry.shell.cmds;

import org.apache.chemistry.Folder;
import org.nuxeo.chemistry.shell.Application;
import org.nuxeo.chemistry.shell.Context;
import org.nuxeo.chemistry.shell.Path;
import org.nuxeo.chemistry.shell.command.AnnotatedCommand;
import org.nuxeo.chemistry.shell.command.Cmd;
import org.nuxeo.chemistry.shell.command.CommandException;
import org.nuxeo.chemistry.shell.command.CommandLine;
import org.nuxeo.chemistry.shell.command.CommandParameter;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Cmd(syntax = "cd target:item", synopsis = "Change working item")
public class Cd extends AnnotatedCommand {

    @Override
    public void run(Application app, CommandLine cmdLine) throws Exception {
        ensureConnected(app);

        String param = cmdLine.getParameterValue("target");

        Context ctx = app.resolveContext(new Path(param));
        if (ctx == null) {
            throw new CommandException("Cannot resolve target: " + param);
        }
        Folder folder = ctx.as(Folder.class);
        if (folder != null) {
            app.setContext(ctx);
        } else {
            throw new CommandException("Cannot cd to something that is not a folder");
        }
    }

}
