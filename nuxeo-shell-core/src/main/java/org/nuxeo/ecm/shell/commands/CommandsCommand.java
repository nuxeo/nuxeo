/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.shell.commands;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.CommandDescriptor;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.ecm.shell.CommandLineService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class CommandsCommand implements Command {
    private static final Log log = LogFactory.getLog(CommandsCommand.class);

    public void run(CommandLine cmdLine) throws Exception {
        CommandLineService service = Framework.getService(CommandLineService.class);
        CommandDescriptor[] cmds;
        String[] elements = cmdLine.getParameters();
        if (elements != null && elements.length == 1) {
            cmds = service.getMatchingCommands(elements[0]);
        } else {
            cmds = service.getSortedCommands();
        }
        for (CommandDescriptor cd : cmds) {
            log.info(cd.getName());
        }
    }

}
