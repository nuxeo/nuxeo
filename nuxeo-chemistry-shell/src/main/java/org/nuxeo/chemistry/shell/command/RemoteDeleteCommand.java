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
package org.nuxeo.chemistry.shell.command;

import org.nuxeo.chemistry.shell.Application;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RemoteDeleteCommand extends RemoteCommand {

    public RemoteDeleteCommand(String path, String syntax, String synopsis) {
        super(path, syntax, synopsis);
    }

    @Override
    public void run(Application app, CommandLine cmdLine) throws Exception {
        //TODO
        //app.getContext().delete(path, cmdLine.toMap());
    }

}
