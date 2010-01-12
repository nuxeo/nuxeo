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

import org.nuxeo.chemistry.shell.Application;
import org.nuxeo.chemistry.shell.command.AnnotatedCommand;
import org.nuxeo.chemistry.shell.command.Cmd;
import org.nuxeo.chemistry.shell.command.Command;
import org.nuxeo.chemistry.shell.command.CommandException;
import org.nuxeo.chemistry.shell.command.CommandLine;
import org.nuxeo.chemistry.shell.command.CommandParameter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Cmd(syntax="help [command:command]", synopsis="Help")
public class Help extends AnnotatedCommand {

    @Override
    public void run(Application app, CommandLine cmdLine) throws Exception {
        String param = cmdLine.getParameterValue("command");
        if (param != null) {
            Command cmd = app.getCommandRegistry().getCommand(param);
            if (cmd != null) {
                println(cmd.getHelp(app));
            } else {
                throw new CommandException("Unknown command: " + param);
            }
        } else {
            println(getHelp(app));

            Command[] cmds = app.getCommandRegistry().getCommands();
            Arrays.sort(cmds, new CommandComparator());
            Set<String> seen = new HashSet<String>();
            StringBuilder buf = new StringBuilder();
            for (Command cmd : cmds) {
                String name = cmd.getName();
                if (seen.contains(name)) {
                    continue;
                }
                seen.add(name);
                buf.setLength(0);
                buf.append(name);
                String[] aliases = cmd.getAliases();
                if (aliases.length > 1) {
                    buf.append(" [");
                    for (int i=1; i<aliases.length; i++) {
                        buf.append(aliases[i]).append("|");
                    }
                    buf.setLength(buf.length()-1);
                    buf.append("]");
                }
                buf.append(" - ").append(cmd.getSynopsis());
                println(buf.toString());
            }
        }
    }

    private static class CommandComparator implements Comparator<Command> {
        public int compare(Command o1, Command o2) {
            return o1.getName().compareTo(o2.getName());
        }
    }

}
