/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Default implementation of the {@link CommandTester} interface. Simple check for the target command. Checks execution
 * on system with contributed test parameters.
 *
 * @author tiry
 */
public class DefaultCommandTester implements CommandTester {

    @Override
    public CommandTestResult test(CommandLineDescriptor cmdDescriptor) {
        String cmd = cmdDescriptor.getCommand();
        String params = cmdDescriptor.getTestParametersString();
        String[] cmdWithParams = (cmd + " " + params).split(" ");
        try {
            ProcessBuilder builder = new ProcessBuilder(cmdWithParams);
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
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (IOException e) {
            return new CommandTestResult(
                    "command " + cmd + " not found in system path (descriptor " + cmdDescriptor + ")");
        }

        return new CommandTestResult();
    }

}
