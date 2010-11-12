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
package org.nuxeo.ecm.automation.shell.cmds;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.client.jaxrs.model.Blob;
import org.nuxeo.ecm.automation.client.jaxrs.model.FileBlob;
import org.nuxeo.ecm.automation.shell.RemoteContext;
import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.ShellConsole;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.fs.FileSystem;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "script", help = "Run a script on the server")
public class Script implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Argument(name = "file", index = 0, required = true, help = "The script file. Must have a .mvel or .groovy extension")
    protected File file;

    public void run() {
        ShellConsole console = ctx.getShell().getConsole();
        FileBlob blob = new FileBlob(file);
        Map<String, String> args = new HashMap<String, String>();
        // TODO
        try {
            Blob response = (Blob) ctx.getSession().newRequest(
                    "Context.RunInputScript", args).setInput(blob).execute();
            if (response != null) {
                InputStream in = response.getStream();
                String str = null;
                try {
                    str = FileSystem.readContent(in);
                } finally {
                    in.close();
                }
                console.println(str);
                if (response instanceof FileBlob) {
                    ((FileBlob) response).getFile().delete();
                }
            }
        } catch (Exception e) {
            throw new ShellException("Failed to run script", e);
        }
    }

}
