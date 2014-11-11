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

import org.nuxeo.ecm.webengine.client.Client;
import org.nuxeo.ecm.webengine.client.command.AnnotatedCommand;
import org.nuxeo.ecm.webengine.client.command.Cmd;
import org.nuxeo.ecm.webengine.client.command.CommandLine;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Cmd(syntax="test -r [--debug|-d] [--opt|-o:*] [file:file]", synopsis="Test")
public class Test extends AnnotatedCommand {

    @Override
    public void run(Client client, CommandLine cmdLine) throws Exception {
        System.out.println("running test ...");
        System.out.println("--debug: "+cmdLine.getParameter("--debug"));
        System.out.println("--opt: "+cmdLine.getParameter("--opt"));
        System.out.println("file: "+cmdLine.getParameter("file"));
        System.out.println("-o: "+cmdLine.getParameter("-o"));
        System.out.println("-d: "+cmdLine.getParameter("-d"));
    }

}
