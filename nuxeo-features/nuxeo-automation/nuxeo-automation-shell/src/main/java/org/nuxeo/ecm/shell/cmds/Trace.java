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
package org.nuxeo.ecm.shell.cmds;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Shell;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "trace", help = "Print the last error stack trace if any")
public class Trace implements Runnable {

    @Context
    protected Shell shell;

    public void run() {
        Throwable t = (Throwable) shell.getProperty("last.error");
        if (t != null) {
            shell.getConsole().println();
            shell.getConsole().println(getStackTrace(t));
        } else {
            shell.getConsole().println("No stack trace to print");
        }
    }

    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        return sw.getBuffer().toString();
    }
}
