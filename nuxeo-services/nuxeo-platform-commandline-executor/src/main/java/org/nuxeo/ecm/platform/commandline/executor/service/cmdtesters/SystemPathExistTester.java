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

import java.io.IOException;
import java.io.InputStream;

import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineDescriptor;

/**
 * Default implementation of the {@link CommandTester} interface. Simple check for the target command in the system
 * path.
 *
 * @author tiry
 */
public class SystemPathExistTester implements CommandTester {

    @Override
    public CommandTestResult test(CommandLineDescriptor cmdDescriptor) {
        String cmd = cmdDescriptor.getCommand();
        try {
            ProcessBuilder builder = new ProcessBuilder(cmd);
            // make sure we have only one InputStream to read to avoid parallelism/deadlock issues
            builder.redirectErrorStream(true);
            Process process = builder.start();
            // close process input immediately
            process.getOutputStream().close();
            // consume all process output
            try (InputStream in = process.getInputStream()) {
                byte[] bytes = new byte[4096];
                while (in.read(bytes) != -1) {
                    // loop
                }
            }
            // wait for process termination
            process.waitFor();
        } catch (InterruptedException e) {
            ExceptionUtils.checkInterrupt(e);
        } catch (IOException e) {
            return new CommandTestResult(
                    "command " + cmd + " not found in system path (descriptor " + cmdDescriptor + ")");
        }

        return new CommandTestResult();
    }

}
