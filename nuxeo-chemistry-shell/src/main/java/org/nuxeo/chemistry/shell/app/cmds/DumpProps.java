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
 */
package org.nuxeo.chemistry.shell.app.cmds;

import org.apache.chemistry.CMISObject;
import org.nuxeo.chemistry.shell.Context;
import org.nuxeo.chemistry.shell.Path;
import org.nuxeo.chemistry.shell.app.ChemistryApp;
import org.nuxeo.chemistry.shell.app.ChemistryCommand;
import org.nuxeo.chemistry.shell.app.utils.SimplePropertyManager;
import org.nuxeo.chemistry.shell.command.Cmd;
import org.nuxeo.chemistry.shell.command.CommandException;
import org.nuxeo.chemistry.shell.command.CommandLine;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
// TODO: remove
@Cmd(syntax="props [item:item]", synopsis="(Obsolete) Print the value of all the properties of the current context object")
public class DumpProps extends ChemistryCommand {

    @Override
    protected void execute(ChemistryApp app, CommandLine cmdLine)
            throws Exception {

        String param = cmdLine.getParameterValue("item");

        Context ctx;
        if (param != null) {
            ctx = app.resolveContext(new Path(param));
            if (ctx == null) {
                throw new CommandException("Cannot resolve "+param);
            }
        } else {
            ctx = app.getContext();
        }

        CMISObject obj = ctx.as(CMISObject.class);
        if (obj != null) {
            new SimplePropertyManager(obj).dumpProperties();
        } else {
            // print server props
            println("Server URL = "+app.getServerUrl());
            println("Host = "+app.getHost());
            println("Username = "+app.getUsername());
            println("Working directory = "+app.getWorkingDirectory());
        }
    }

}
