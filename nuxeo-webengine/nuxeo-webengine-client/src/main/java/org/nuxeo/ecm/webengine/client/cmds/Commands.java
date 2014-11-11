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

package org.nuxeo.ecm.webengine.client.cmds;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;

import org.nuxeo.ecm.webengine.client.Client;
import org.nuxeo.ecm.webengine.client.Console;
import org.nuxeo.ecm.webengine.client.command.AnnotatedCommand;
import org.nuxeo.ecm.webengine.client.command.Cmd;
import org.nuxeo.ecm.webengine.client.command.Command;
import org.nuxeo.ecm.webengine.client.command.CommandLine;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Cmd(syntax="cmds|commands", synopsis="List available commands")
public class Commands extends AnnotatedCommand implements Comparator<Command>{

    public int compare(Command o1, Command o2) {
        return o1.getName().compareTo(o2.getName());
    }

    @Override
    public void run(Client client, CommandLine cmdLine) throws Exception {
        Command[] cmds = client.getRegistry().getCommands();
        Arrays.sort(cmds, this);
        HashSet<String> seen = new HashSet<String>();
        StringBuilder buf = new StringBuilder();
        for (Command cmd : cmds) {
            String name = cmd.getName();
            if (seen.contains(name)) continue;
            seen.add(name);
            buf.setLength(0);
            buf.append(name);
            String[] aliases = cmd.getAliases();
            if (aliases.length>1) {
                buf.append(" [");
                for (int i=1; i<aliases.length; i++) {
                    buf.append(aliases[i]).append("|");
                }
                buf.setLength(buf.length()-1);
                buf.append("]");
            }
            buf.append(" - ").append(cmd.getSynopsis());
            Console.getDefault().println(buf.toString());
        }
    }

}
