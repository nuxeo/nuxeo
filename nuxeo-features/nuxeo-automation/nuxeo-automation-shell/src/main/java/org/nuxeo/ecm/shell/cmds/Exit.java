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

import org.nuxeo.ecm.shell.Argument;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.ShellException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
@Command(name = "exit", aliases = "quit", help = "Exit the interactive shell")
public class Exit implements Runnable {

    @Argument(name = "code", index = 0, required = false, help = "The exit code. Must be a positive number otherwise 0 is assumed. Defaults to 0.")
    protected int code = 0;

    public void run() {
        if (code <= 0) {
            code = -1;
        }
        throw new ShellException().setErrorCode(code);
    }
}
