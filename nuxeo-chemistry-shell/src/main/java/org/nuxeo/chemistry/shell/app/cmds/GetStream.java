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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import org.apache.chemistry.CMISObject;
import org.apache.chemistry.ContentStream;
import org.nuxeo.chemistry.shell.Console;
import org.nuxeo.chemistry.shell.Context;
import org.nuxeo.chemistry.shell.app.ChemistryApp;
import org.nuxeo.chemistry.shell.app.ChemistryCommand;
import org.nuxeo.chemistry.shell.app.utils.SimplePropertyManager;
import org.nuxeo.chemistry.shell.command.Cmd;
import org.nuxeo.chemistry.shell.command.CommandLine;
import org.nuxeo.chemistry.shell.command.CommandParameter;
import org.nuxeo.chemistry.shell.util.FileUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Cmd(syntax="getStream file:file", synopsis="Export the stream of the current context object to the given file")
public class GetStream extends ChemistryCommand {

    @Override
    protected void execute(ChemistryApp app, CommandLine cmdLine)
            throws Exception {
        Context ctx = app.getContext();
        CMISObject obj = ctx.as(CMISObject.class);
        List<CommandParameter> args = cmdLine.getArguments();
        if (args.size() != 1) {
            Console.getDefault().error("Missing required argument: key");
        }
        if (obj != null) {            
            ContentStream cs = new SimplePropertyManager(obj).getStream();
            String name = cs.getFileName();
            InputStream in = cs.getStream();
            File file = app.resolveFile(args.get(0).getValue());
            file = new File(file, name);
            FileOutputStream out = new FileOutputStream(file);
            try {
                FileUtils.copy(in, out);
            } finally {
                out.close();
                in.close();
            }
        }
    }

}
