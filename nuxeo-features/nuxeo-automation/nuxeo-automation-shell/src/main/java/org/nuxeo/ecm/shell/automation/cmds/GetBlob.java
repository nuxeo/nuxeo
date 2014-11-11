/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
package org.nuxeo.ecm.shell.automation.cmds;

import java.io.File;

import org.nuxeo.ecm.automation.client.jaxrs.model.DocRef;
import org.nuxeo.ecm.automation.client.jaxrs.model.FileBlob;
import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Parameter;
import org.nuxeo.ecm.shell.ShellConsole;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.automation.DocRefCompletor;
import org.nuxeo.ecm.shell.automation.RemoteContext;
import org.nuxeo.ecm.shell.fs.FileSystem;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "getfile", help = "Get a document attached file")
public class GetBlob implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Parameter(name = "-xpath", hasValue = true, help = "The xpath of the blob property to get. Defaults to the one used by the File document type.")
    protected String xpath;

    @Parameter(name = "-todir", hasValue = true, help = "An optional target directory to save the file. The default is the current working directory.")
    protected File todir;

    @Argument(name = "doc", index = 0, required = false, completor = DocRefCompletor.class, help = "The target document. If not specified the current document is used. To use UID references prefix them with 'doc:'.")
    protected String path;

    public void run() {
        ShellConsole console = ctx.getShell().getConsole();
        DocRef doc = ctx.resolveRef(path);
        try {
            FileBlob blob = ctx.getDocumentService().getBlob(doc, xpath);
            String fname = blob.getFileName();
            if (fname == null || fname.length() == 0) {
                fname = "unnamed_blob";
            }
            if (todir == null) {
                todir = ctx.getShell().getContextObject(FileSystem.class).pwd();
            }
            File dst = new File(todir, fname);
            blob.getFile().renameTo(dst);
            console.println("File saved to: " + dst.getAbsolutePath());
        } catch (Exception e) {
            throw new ShellException("Failed to get file from " + doc, e);
        }

    }
}
