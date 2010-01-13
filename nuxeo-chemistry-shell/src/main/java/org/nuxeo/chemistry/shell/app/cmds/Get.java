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
import org.nuxeo.chemistry.shell.Context;
import org.nuxeo.chemistry.shell.Path;
import org.nuxeo.chemistry.shell.app.ChemistryApp;
import org.nuxeo.chemistry.shell.app.ChemistryCommand;
import org.nuxeo.chemistry.shell.app.utils.SimplePropertyManager;
import org.nuxeo.chemistry.shell.command.Cmd;
import org.nuxeo.chemistry.shell.command.CommandException;
import org.nuxeo.chemistry.shell.command.CommandLine;
import org.nuxeo.chemistry.shell.util.FileUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
// TODO: add an optional local name for the file
@Cmd(syntax="get|getstream target:item", synopsis="Downloads the stream of the target document")
public class Get extends ChemistryCommand {

    @Override
    protected void execute(ChemistryApp app, CommandLine cmdLine)
            throws Exception {

        String target = cmdLine.getParameterValue("target");

        Context ctx = app.resolveContext(new Path(target));
        if (ctx == null) {
            throw new CommandException("Cannot resolve "+target);
        }

        Document obj = ctx.as(Document.class);
        if (obj == null) {
            throw new CommandException("Your target must be a document");
        }

        ContentStream cs = new SimplePropertyManager(obj).getStream();
        if (cs == null) {
            throw new CommandException("Your target doesn't have a stream");
        }

        String name = cs.getFileName();
        InputStream in = cs.getStream();
        File file = app.resolveFile(name);
        FileOutputStream out = new FileOutputStream(file);
        try {
            FileUtils.copy(in, out);
            println("Object stream saved to local file: " + file);
        } finally {
            out.close();
            in.close();
        }
    }

}
