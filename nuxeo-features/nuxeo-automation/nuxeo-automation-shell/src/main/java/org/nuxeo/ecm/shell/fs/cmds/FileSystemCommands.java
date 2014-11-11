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
package org.nuxeo.ecm.shell.fs.cmds;

import org.nuxeo.ecm.shell.CommandRegistry;
import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.cmds.GlobalCommands;
import org.nuxeo.ecm.shell.fs.FileSystem;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class FileSystemCommands extends CommandRegistry {

    public final static FileSystemCommands INSTANCE = new FileSystemCommands();

    public FileSystemCommands() {
        super(GlobalCommands.INSTANCE, "local");
        addAnnotatedCommand(Ls.class);
        addAnnotatedCommand(Pwd.class);
        addAnnotatedCommand(Pushd.class);
        addAnnotatedCommand(Popd.class);
        addAnnotatedCommand(Cd.class);
        addAnnotatedCommand(MkDir.class);
        addAnnotatedCommand(Touch.class);
        addAnnotatedCommand(Rm.class);
        addAnnotatedCommand(Cp.class);
        addAnnotatedCommand(Mv.class);
        addAnnotatedCommand(Cat.class);
    }

    public String getPrompt(Shell shell) {
        return System.getProperty("user.name") + ":"
                + shell.getContextObject(FileSystem.class).pwd().getName()
                + "$ ";
    }

}
