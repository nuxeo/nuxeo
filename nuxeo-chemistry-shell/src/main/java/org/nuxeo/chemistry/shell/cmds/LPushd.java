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

import java.io.File;
import java.util.Stack;

import org.nuxeo.chemistry.shell.Application;
import org.nuxeo.chemistry.shell.command.AnnotatedCommand;
import org.nuxeo.chemistry.shell.command.Cmd;
import org.nuxeo.chemistry.shell.command.CommandException;
import org.nuxeo.chemistry.shell.command.CommandLine;
import org.nuxeo.chemistry.shell.command.CommandParameter;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Cmd(syntax="lpushd dir:dir", synopsis="Push local directory stack")
@SuppressWarnings("unchecked")
public class LPushd extends AnnotatedCommand {

    @Override
    public void run(Application app, CommandLine cmdLine) throws Exception {
        CommandParameter param = cmdLine.getParameter("dir");

        String path = param.getValue();
        File file = app.resolveFile(path);
        if (!file.isDirectory()) {
            throw new CommandException("Not a directory: " + file);
        }

        Stack<File> stack = (Stack<File>)app.getData(LPopd.WDIR_STACK_KEY);
        if (stack == null) {
            stack = new Stack<File>();
            app.setData(LPopd.WDIR_STACK_KEY, stack);
        }
        stack.push(app.getWorkingDirectory());
        app.setWorkingDirectory(file);
    }

}
