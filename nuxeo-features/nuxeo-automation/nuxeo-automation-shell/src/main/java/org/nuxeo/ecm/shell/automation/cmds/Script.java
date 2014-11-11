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
import java.util.HashMap;
import java.util.Map;

import jline.ANSIBuffer;

import org.nuxeo.ecm.automation.client.jaxrs.model.FileBlob;
import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Parameter;
import org.nuxeo.ecm.shell.ShellConsole;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.automation.RemoteContext;
import org.nuxeo.ecm.shell.automation.Scripting;
import org.nuxeo.ecm.shell.utils.ANSICodes;
import org.nuxeo.ecm.shell.utils.StringUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "script", help = "Run a script on the server")
public class Script implements Runnable {

    @Context
    protected RemoteContext ctx;

    @Parameter(name = "-ctx", hasValue = true, help = "Use this to set execution context variables. Syntax is: \"k1=v1,k1=v2\"")
    protected String ctxVars;

    @Parameter(name = "-s", hasValue = true, help = "Use this to change the separator used in context variables. THe default is ','")
    protected String sep = ",";

    @Argument(name = "file", index = 0, required = true, help = "The script file. Must have a .mvel or .groovy extension")
    protected File file;

    public void run() {
        ShellConsole console = ctx.getShell().getConsole();
        FileBlob blob = new FileBlob(file);
        Map<String, String> args = new HashMap<String, String>();
        if (ctxVars != null) {
            for (String pair : ctxVars.split(sep)) {
                String[] ar = StringUtils.split(pair, '=', true);
                args.put(ar[0], ar[1]);
            }
        }
        try {
            ANSIBuffer buf = new ANSIBuffer();
            ANSICodes.appendTemplate(buf, Scripting.runScript(ctx, blob, args));
            console.println(buf.toString());
        } catch (Exception e) {
            throw new ShellException("Failed to run script", e);
        }
    }

}
