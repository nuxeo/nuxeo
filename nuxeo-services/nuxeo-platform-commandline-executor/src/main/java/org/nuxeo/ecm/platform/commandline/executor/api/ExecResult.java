/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 */

package org.nuxeo.ecm.platform.commandline.executor.api;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang.StringUtils;

/**
 * Wraps result of the command-line execution:
 * <ul>
 * <li>executed command line,
 * <li>output buffer,
 * <li>return code,
 * <li>java Exception.
 * </ul>
 *
 * @author tiry
 */
public class ExecResult implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String commandLine;

    protected final List<String> output;

    protected final long execTime;

    protected boolean success;

    protected final CommandException error;

    protected int returnCode;

    public ExecResult(String commandLine, List<String> output, long execTime,
            int returnCode) {
        this.commandLine = commandLine;
        this.execTime = execTime;
        this.output = output;
        this.returnCode = returnCode;
        success = returnCode == 0;
        if (!success) {
            this.error = new CommandException(String.format(
                    "Error code %d return by command: %s\n%s", returnCode,
                    commandLine, StringUtils.join(output, "\n  ")));
        } else {
            this.error = null;
        }
    }

    public ExecResult(String commandLine, Exception error) {
        this.commandLine = commandLine;
        execTime = 0;
        output = null;
        success = false;
        this.error = new CommandException(String.format(
                "Error while running command: %s", commandLine), error);
    }

    public List<String> getOutput() {
        return output;
    }

    public long getExecTime() {
        return execTime;
    }

    public boolean isSuccessful() {
        return success;
    }

    /**
     * Rather rely on {@link #isSuccessful()} to check for the execution
     * success. Note however that since 5.7.3, the {@link #getError()} method
     * cannot return null even if the execution failed (it was not the case
     * before).
     *
     * @return CommandException attached to the result, optionally wrapping the
     *         root cause.
     *
     */
    public CommandException getError() {
        return error;
    }

    public int getReturnCode() {
        return returnCode;
    }

    /**
     * @since 5.5
     * @return the executed command line
     */
    public String getCommandLine() {
        return commandLine;
    }

}
