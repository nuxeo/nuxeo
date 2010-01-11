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

import org.apache.chemistry.ContentStream;
import org.apache.chemistry.Document;
import org.nuxeo.chemistry.shell.Console;
import org.nuxeo.chemistry.shell.Context;
import org.nuxeo.chemistry.shell.Path;
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
@Cmd(syntax="get|getStream target:item", synopsis="Downloads the stream of the target document")
public class Get extends ChemistryCommand {

    @Override
    protected void execute(ChemistryApp app, CommandLine cmdLine)
            throws Exception {

        CommandParameter param = cmdLine.getParameter("target");

        Context ctx = app.resolveContext(new Path(param.getValue()));
        if (ctx == null) {
            Console.getDefault().warn("Cannot resolve target: " + param.getValue());
            return;
        }

        Document obj = ctx.as(Document.class);
        if (obj == null) {
            Console.getDefault().warn("Your target must be a document");
            return;
        }

        ContentStream cs = new SimplePropertyManager(obj).getStream();
        if (cs == null) {
            Console.getDefault().warn("Your target doesn't have a stream");
            return;
        }

        String name = cs.getFileName();
        InputStream in = cs.getStream();
        File file = app.resolveFile(name);
        FileOutputStream out = new FileOutputStream(file);
        try {
            FileUtils.copy(in, out);
            Console.getDefault().println("Object stream saved to file: " + file);
        } finally {
            out.close();
            in.close();
        }
    }

}
