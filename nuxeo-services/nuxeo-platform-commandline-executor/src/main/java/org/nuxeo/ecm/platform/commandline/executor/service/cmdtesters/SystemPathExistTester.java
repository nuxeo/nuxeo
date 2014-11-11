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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 *
 */

package org.nuxeo.ecm.platform.commandline.executor.service.cmdtesters;

import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineDescriptor;

/**
 * Default implementation of the {@link CommandTester} interface. Simple check
 * for the target command in the system path.
 *
 * @author tiry
 */
public class SystemPathExistTester implements CommandTester {

    public CommandTestResult test(CommandLineDescriptor cmdDescriptor) {
        String cmd = cmdDescriptor.getCommand();
        try {
            Runtime.getRuntime().exec(cmd);
        } catch (Exception e) {
            return new CommandTestResult("command " + cmd
                    + " not found in system path");
        }

        return new CommandTestResult();
    }

}
