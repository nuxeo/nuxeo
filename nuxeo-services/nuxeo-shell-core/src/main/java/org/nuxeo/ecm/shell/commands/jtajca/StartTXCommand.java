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

import org.nuxeo.ecm.shell.CommandLine;
import org.nuxeo.ecm.shell.commands.repository.AbstractCommand;
import org.nuxeo.runtime.transaction.TransactionHelper;
import org.nuxeo.runtime.transaction.TransactionRuntimeException;

/**
 * @author jcarsique
 */
public class StartTXCommand extends AbstractCommand {

    @Override
    public void run(CommandLine cmdLine) {
        if (!TransactionHelper.startTransaction()) {
            throw new TransactionRuntimeException("Couldn't start transaction.");
        }
    }

}
