/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.chemistry.shell.cmds;

import org.nuxeo.chemistry.shell.Application;
import org.nuxeo.chemistry.shell.Console;
import org.nuxeo.chemistry.shell.command.AnnotatedCommand;
import org.nuxeo.chemistry.shell.command.Cmd;
import org.nuxeo.chemistry.shell.command.CommandException;
import org.nuxeo.chemistry.shell.command.CommandLine;


/**
 * @author Stefane Fermigier
 */
@Cmd(syntax = "match pattern", synopsis = "Fails if last command result doesn't match the pattern")
public class Match extends AnnotatedCommand {

    @Override
    public void run(Application app, CommandLine cmdLine) throws Exception {
        ensureConnected(app);

        String pattern = cmdLine.getParameterValue("pattern");

        String[] lines = Console.getDefault().getLastResult().split("\n");
        boolean success = false;
        for (String line : lines) {
            if (line.matches(pattern)) {
                success = true;
            }
        }
        if (!success) {
            throw new CommandException("Match failed: pattern \""
                    + pattern + "\" doesn't match last result");
        }
    }

}
