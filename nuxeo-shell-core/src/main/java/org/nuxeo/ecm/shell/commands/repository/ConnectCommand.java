/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Julien Carsique
 *
 * $Id$
 */

package org.nuxeo.ecm.shell.commands.repository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.client.NuxeoClient;
import org.nuxeo.ecm.shell.CommandLine;

/**
 * @author jcarsique
 */
public class ConnectCommand extends AbstractCommand {
    private static final Log log = LogFactory.getLog(ConnectCommand.class);

    @Override
    public void run(CommandLine cmdLine) throws Exception {
        try {
            NuxeoClient.getInstance().tryDisconnect();
        } catch (Exception e) {
            // do nothing
        }
        String[] parameters = cmdLine.getParameters();
        if (parameters.length > 0) {
            context.setUsername(parameters[0]);
            if (parameters.length > 1) {
                context.setPassword(parameters[1]);
            }
        }

        try {
            log.info("First try to connect...");
            context.getRepositoryInstance().getRootDocument();
        } catch (Exception e) {
            log.info("Connection failure with user "+context.getUsername());
            context.setRepositoryInstance(null);
            context.setUsername(null);
            context.setPassword(null);
            try {
                NuxeoClient.getInstance().tryDisconnect();
            } catch (Exception e1) {
                // do nothing
            }
            try {
                log.info("Second try to connect...");
                context.getRepositoryInstance().getRootDocument();
            } catch (Exception e2) {
                log.error("Connection failed: " + e2.getMessage());
                throw e2;
            }
        }
        log.info("Connected to "+context.getHost()+":"+context.getPort());
    }

}
