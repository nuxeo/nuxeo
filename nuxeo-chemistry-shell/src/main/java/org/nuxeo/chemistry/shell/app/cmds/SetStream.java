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

import java.io.File;
import java.io.FileInputStream;

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
@Cmd(syntax="setstream target filename:file", synopsis="Set the given file content as a stream on the current context object")
public class SetStream extends ChemistryCommand {

    @Override
    protected void execute(ChemistryApp app, CommandLine cmdLine)
            throws Exception {

        String target = cmdLine.getParameterValue("target");
        String filename = cmdLine.getParameterValue("filename");

        Context ctx = app.resolveContext(new Path(target));
        CMISObject obj = ctx.as(CMISObject.class);

        if (obj == null) {
            throw new CommandException("Cannot resolve "+target);
        }

        File file = app.resolveFile(filename);
        FileInputStream in = new FileInputStream(file);
        try {
            new SimplePropertyManager(obj).setStream(in, file.getName());
        } finally {
            in.close();
        }
    }

}
