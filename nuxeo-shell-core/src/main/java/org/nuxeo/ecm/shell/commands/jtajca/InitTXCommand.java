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

package org.nuxeo.ecm.shell.commands.jtajca;

import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.jndi.NamingContextFactory;
import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.ecm.shell.commands.repository.AbstractCommand;
import org.nuxeo.runtime.jtajca.NuxeoContainer;

/**
 * @author jcarsique Command for initializing transaction management
 *
 */
public class InitTXCommand extends AbstractCommand {
    private static final Log log = LogFactory.getLog(InitTXCommand.class);

    @Override
    public void run(CommandLine cmdLine) throws NamingException {
        try {
            NamingContextFactory.setAsInitial();
            NuxeoContainer.initTransactionManagement();
        } catch (NamingException e) {
            throw e;
        }
    }

}
