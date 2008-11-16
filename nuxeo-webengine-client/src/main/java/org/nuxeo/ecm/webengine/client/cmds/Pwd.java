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
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.client.cmds;

import org.nuxeo.ecm.webengine.client.command.AnnotatedCommand;
import org.nuxeo.ecm.webengine.client.command.Cmd;
import org.nuxeo.ecm.webengine.client.command.CommandLine;
import org.nuxeo.ecm.webengine.client.http.HttpClient;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Cmd(syntax="connect|open", synopsis="Open a new session")
public class Pwd extends AnnotatedCommand {

    @Override
    public void run(HttpClient client, CommandLine cmdLine) throws Exception {
        System.out.println("TODO: Open session ...");
    }
    
}
