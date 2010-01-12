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
import org.apache.chemistry.Folder;
import org.nuxeo.chemistry.shell.Context;
import org.nuxeo.chemistry.shell.Path;
import org.nuxeo.chemistry.shell.app.ChemistryApp;
import org.nuxeo.chemistry.shell.app.ChemistryCommand;
import org.nuxeo.chemistry.shell.command.Cmd;
import org.nuxeo.chemistry.shell.command.CommandException;
import org.nuxeo.chemistry.shell.command.CommandLine;
import org.nuxeo.chemistry.shell.command.CommandParameter;
import org.nuxeo.chemistry.shell.util.StringUtils;

import java.util.List;

/**
 * @author <a href="mailto:sf@nuxeo.com">Stefane Fermigier</a>
 *
 */
@Cmd(syntax="rm|del target:item", synopsis="Removes an object of the given name")
public class Remove extends ChemistryCommand {

    @Override
    protected void execute(ChemistryApp app, CommandLine cmdLine)
            throws Exception {
        String param = cmdLine.getParameterValue("target");

        Path path = new Path(param);
        String name = path.getLastSegment();
        Path parent = path.getParent();

        Context ctx = app.resolveContext(parent);
        Folder folder = ctx.as(Folder.class);
        if (folder == null) {
            throw new CommandException(parent+" doesn't exist or is not a folder");
        }

        boolean success = false;
        for (CMISObject child : folder.getChildren()) {
            // TODO: use wildcard but make sure it's safe
            // if (StringUtils.matches(name, child.getName())) {
            if (name.equals(child.getName())) {
                child.delete();
                success = true;
            }
        }
        if (!success) {
            throw new CommandException("No match");
        }
        ctx.reset();
    }

}
