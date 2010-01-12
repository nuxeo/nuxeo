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

import org.apache.chemistry.Document;
import org.apache.chemistry.Folder;
import org.nuxeo.chemistry.shell.Console;
import org.nuxeo.chemistry.shell.Context;
import org.nuxeo.chemistry.shell.Path;
import org.nuxeo.chemistry.shell.app.ChemistryApp;
import org.nuxeo.chemistry.shell.app.ChemistryCommand;
import org.nuxeo.chemistry.shell.app.utils.SimplePropertyManager;
import org.nuxeo.chemistry.shell.app.utils.SimpleCreator;
import org.nuxeo.chemistry.shell.command.Cmd;
import org.nuxeo.chemistry.shell.command.CommandLine;
import org.nuxeo.chemistry.shell.command.CommandParameter;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
// TODO: make target optional
@Cmd(syntax="put source:file target:item", synopsis="Uploads the stream of the target document")
public class Put extends ChemistryCommand {

    @Override
    protected void execute(ChemistryApp app, CommandLine cmdLine)
            throws Exception {

        CommandParameter sourceParam = cmdLine.getParameter("source");
        CommandParameter targetParam = cmdLine.getParameter("target");

        Context targetCtx = app.resolveContext(new Path(targetParam.getValue()));

        // Create document if it doesn't exist
        if (targetCtx == null) {
            Context currentCtx = app.getContext();
            Folder folder =  currentCtx.as(Folder.class);
            if (folder != null) {
                new SimpleCreator(folder).createFile(targetParam.getValue());
                currentCtx.reset();
                targetCtx = app.resolveContext(new Path(targetParam.getValue()));
            }
        }
        if (targetCtx == null) {
            Console.getDefault().warn("Cannot create target document");
            return;
        }

        Document obj = targetCtx.as(Document.class);
        if (obj == null) {
            Console.getDefault().warn("Your target must be a document");
            return;
        }

        File file = app.resolveFile(sourceParam.getValue());
        FileInputStream in = new FileInputStream(file);
        try {
            new SimplePropertyManager(obj).setStream(in, file.getName());
        } finally {
            in.close();
        }        
    }

}