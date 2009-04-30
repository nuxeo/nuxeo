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

package org.nuxeo.ecm.shell.commands.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.client.NuxeoClient;
import org.nuxeo.ecm.shell.Command;
import org.nuxeo.ecm.shell.CommandContext;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.ecm.shell.CommandLineService;
import org.nuxeo.runtime.api.Framework;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AbstractCommand implements Command {

    private static final Log log = LogFactory.getLog(AbstractCommand.class);

    protected final NuxeoClient client = NuxeoClient.getInstance();

    protected CommandLineService cmdService;

    protected CommandContext context;

    protected AbstractCommand() {
        try {
            cmdService = Framework.getLocalService(CommandLineService.class);
            context = cmdService.getCommandContext();
        } catch (Exception e) {
            log.error(e);
        }
    }

    public abstract void run(CommandLine cmdLine) throws Exception;

}
