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
 *     bstefanescu, jcarsique
 *
 * $Id$
 */

package org.nuxeo.ecm.shell;

import java.text.ParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.repository.RepositoryInstance;
import org.nuxeo.ecm.core.client.NuxeoClient;
import org.nuxeo.ecm.shell.commands.system.LogCommand;
import org.nuxeo.runtime.api.Framework;

/**
 * Should be used with the nuxeo launcher
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Main {

    private static final Log log = LogFactory.getLog(Main.class);

    private Main() {
    }

    public static void main(String[] args) {

        CommandLine cmdLine;
        CommandLineService service = Framework.getRuntime().getService(
                CommandLineService.class);
        try {
            cmdLine = service.parse(args, true);
        } catch (ParseException e) {
            log.error(e);
//            System.exit(1);
            return;
        }

        String cmdName = cmdLine.getCommand();
        if (cmdName == null) {
            cmdName = "interactive";
            cmdLine.addCommand(cmdName);
        }

        CommandContext cmdContext = service.getCommandContext();
        cmdContext.setCommandLine(cmdLine);

        String host = cmdLine.getOption(Options.HOST);
        String port = cmdLine.getOption(Options.PORT);

        // this logic would be duplicated in a "connect" command
        if (host != null) {
            cmdContext.setCandidateHosts(IPHelper.findCandidateIPs(host));
            cmdContext.setPort(port == null ? 0 : Integer.parseInt(port));
        } else { // a local connection ?
            // do nothing
        }
        cmdContext.setUsername(cmdLine.getOption(Options.USERNAME));
        cmdContext.setPassword(cmdLine.getOption(Options.PASSWORD));

        boolean debugMode = cmdLine.getOption(Options.DEBUG)!=null;
        if (debugMode) {
            LogCommand.setDebug(true);
        }

        CommandDescriptor cd = service.getCommand(cmdName);

        if (cd == null) {
            log.error("No such command was registered:  " + cmdName);
            System.exit(1);
        }

        int rcmds = args.length - 1;
        String[] newArgs;
        if (rcmds > 0) {
            newArgs = new String[rcmds];
            System.arraycopy(args, 1, newArgs, 0, rcmds);
        } else {
            newArgs = new String[0];
        }

        try {
            service.runCommand(cd, cmdLine);
        } catch (Throwable e) {
            log.error(e);
            System.exit(2);
        } finally {
            try {
                if (cmdContext.isCurrentRepositorySet()) {
                    RepositoryInstance repo = cmdContext.getRepositoryInstance();
                    if (repo != null) {
                        repo.close();
                    }
                }
                NuxeoClient.getInstance().tryDisconnect();
                log.info("Bye.");
                Framework.shutdown();
                System.exit(0);
            } catch (Exception e) {
                log.error("Failed to Disconnect.",e);
            }
        }
    }

}
