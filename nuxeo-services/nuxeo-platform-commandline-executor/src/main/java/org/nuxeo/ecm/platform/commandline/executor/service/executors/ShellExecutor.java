/*
 * (C) Copyright 2006-2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tdelprat, jcarsique
 *
 */

package org.nuxeo.ecm.platform.commandline.executor.service.executors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.SimpleLog;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineDescriptor;
import org.nuxeo.log4j.ThreadedStreamGobbler;

/**
 * Default implementation of the {@link Executor} interface. Use simple shell
 * exec.
 *
 * @author tiry
 */
public class ShellExecutor extends AbstractExecutor implements Executor {

    private static final Log log = LogFactory.getLog(ShellExecutor.class);

    @Override
    public ExecResult exec(CommandLineDescriptor cmdDesc, CmdParameters params) {

        long t0 = System.currentTimeMillis();
        List<String> output = new ArrayList<String>();

        String[] cmd;
        if (isWindows()) {
            String[] paramsArray = getParametersArray(cmdDesc, params);
            cmd = new String[] { "cmd", "/C", cmdDesc.getCommand() };
            cmd = (String[]) ArrayUtils.addAll(cmd, paramsArray);
            cmd = (String[]) ArrayUtils.addAll(cmd, new String[] { "2>&1" });
        } else {
            String paramsString = getParametersString(cmdDesc, params)
                    + " 2>&1";
            cmd = new String[] { "/bin/sh", "-c",
                    cmdDesc.getCommand() + " " + paramsString };
        }
        String commandLine = StringUtils.join(cmd, " ");

        Process p1;
        try {
            if (log.isDebugEnabled()) {
                log.debug("Running system command: "
                        + commandLine);
            }
            p1 = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            return new ExecResult(commandLine, e);
        }

        ThreadedStreamGobbler out = new ThreadedStreamGobbler(
                p1.getInputStream(), cmdDesc.getReadOutput() ? output : null);
        ThreadedStreamGobbler err = new ThreadedStreamGobbler(
                p1.getErrorStream(), SimpleLog.LOG_LEVEL_ERROR);

        err.start();
        out.start();

        int exitCode = 0;
        try {
            exitCode = p1.waitFor();
            out.join();
            err.join();
        } catch (InterruptedException e) {
            return new ExecResult(commandLine, e);
        }

        long t1 = System.currentTimeMillis();
        return new ExecResult(commandLine, output, t1 - t0,
                exitCode);
    }

}
