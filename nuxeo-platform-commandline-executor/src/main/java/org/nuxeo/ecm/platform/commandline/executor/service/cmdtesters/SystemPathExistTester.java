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

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.nuxeo.common.utils.Path;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineDescriptor;

/**
 * Default implementation of the {@link CommandTester} interface.
 * Simple check for the target command in the system path.
 *
 * @author tiry
 */
public class SystemPathExistTester implements CommandTester {

    public SystemPathExistTester() {
        // NOP
    }

    protected List<String> getSystemPaths() {
        String pathStr = System.getenv("PATH");
        String[] paths = pathStr.split(":");
        return Arrays.asList(paths);
    }

    public CommandTestResult test(CommandLineDescriptor cmdDescriptor) {
        String cmd = cmdDescriptor.getCommand();
        if (new File(cmd).exists()) {
            return new CommandTestResult();
        }
        for (String path : getSystemPaths()) {
            String cmdPath = new Path(path).append(cmd).toString();
            if (new File(cmdPath).exists()) {
                return new CommandTestResult();
            }
        }
        return new CommandTestResult("command " + cmd + " not found in system path");
    }

}
