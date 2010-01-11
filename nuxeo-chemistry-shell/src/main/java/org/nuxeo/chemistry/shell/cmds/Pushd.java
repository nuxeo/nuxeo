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

import java.util.Stack;

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
 *
 */
@Cmd(syntax="pushd [target:item]", synopsis="Push directory stack")
public class Pushd extends AnnotatedCommand {

    @Override
    @SuppressWarnings("unchecked")
    public void run(Application app, CommandLine cmdLine) throws Exception {
        CommandParameter param = cmdLine.getParameter("target");

        String path = param.getValue();
        Context ctx = app.resolveContext(new Path(path));
        if (ctx != null) {
            Stack<Context> stack = (Stack<Context>) app.getData(Popd.CTX_STACK_KEY);
            if (stack == null) {
                stack = new Stack<Context>();
                app.setData(Popd.CTX_STACK_KEY, stack);
            }
            stack.push(ctx);
        } else {
            Console.getDefault().warn("No such object: "+path);
        }
        Console.getDefault().updatePrompt();
    }

}
