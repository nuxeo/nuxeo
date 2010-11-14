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

import java.util.Map;

import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.Context;
import org.nuxeo.ecm.shell.Parameter;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.automation.AutomationShell;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "connect", help = "Connect to a remote automation server")
public class Connect implements Runnable {

    @Context
    protected Shell shell;

    @Argument(name = "url", index = 0, required = false, help = "The url of the automation server")
    protected String url;

    @Parameter(name = "-u", hasValue = true, help = "The url of the automation server")
    protected String username;

    @Parameter(name = "-p", hasValue = true, help = "the password")
    protected String password;

    public void run() {
        Map<String, String> args = (Map<String, String>) shell.getMainArguments();
        if (username == null && args != null) {
            username = args.get("-u");
        }
        if (password == null && args != null) {
            password = args.get("-p");
        }
        if (url == null && args != null) {
            url = args.get("#1");
        }
        if (username != null && password == null) {
            password = shell.getConsole().readLine("Password: ", '*');
        }
        try {
            ((AutomationShell) shell).connect(url, username, password);
        } catch (Exception e) {
            throw new ShellException("Failed to connect to " + url, e);
        }
    }

}
