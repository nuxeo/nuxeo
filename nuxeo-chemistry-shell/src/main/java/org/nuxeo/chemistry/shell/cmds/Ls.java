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

import org.apache.chemistry.CMISObject;
import org.apache.chemistry.Folder;
import org.nuxeo.chemistry.shell.Application;
import org.nuxeo.chemistry.shell.Console;
import org.nuxeo.chemistry.shell.Context;
import org.nuxeo.chemistry.shell.Path;
import org.nuxeo.chemistry.shell.command.AnnotatedCommand;
import org.nuxeo.chemistry.shell.command.Cmd;
import org.nuxeo.chemistry.shell.command.CommandLine;
import org.nuxeo.chemistry.shell.command.CommandParameter;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@Cmd(syntax = "ls [target:item]", synopsis = "List entries in working directory")
public class Ls extends AnnotatedCommand {

    @Override
    public void run(Application app, CommandLine cmdLine) throws Exception {
        CommandParameter param = cmdLine.getLastParameter();

        Context ctx;
        if (param == null || param.getValue() == null) {
            ctx = app.getContext();
        } else {
            ctx = app.resolveContext(new Path(param.getValue()));
            if (ctx == null) {
                Console.getDefault().warn("Cannot resolve "+param.getValue());
                return;
            }
        }
        Folder folder = ctx.as(Folder.class);
        if (folder != null) {
            for (String line : ctx.ls()) {
                Console.getDefault().println(line);
            }
        } else {
            Console.getDefault().println(ctx.as(CMISObject.class).getName());
        }
    }

}
