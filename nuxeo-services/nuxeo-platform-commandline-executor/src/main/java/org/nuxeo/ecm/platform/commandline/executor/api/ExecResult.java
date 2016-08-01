/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and others.
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

    public ExecResult(String commandLine, List<String> output, long execTime, int returnCode) {
        this.commandLine = commandLine;
        this.execTime = execTime;
        this.output = output;
        this.returnCode = returnCode;
        success = returnCode == 0;
        if (!success) {
            error = new CommandException(String.format("Error code %d return by command: %s\n%s", returnCode,
                    commandLine, StringUtils.join(output, "\n  ")));
        } else {
            error = null;
        }
    }

    public ExecResult(String commandLine, Exception error) {
        this.commandLine = commandLine;
        execTime = 0;
        output = null;
        returnCode = 1;
        success = false;
        this.error = new CommandException(String.format("Error while running command: %s", commandLine), error);
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
     * Rather rely on {@link #isSuccessful()} to check for the execution success. Note however that since 5.7.3, the
     * {@link #getError()} method cannot return null even if the execution failed (it was not the case before).
     *
     * @return CommandException attached to the result, optionally wrapping the root cause.
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
