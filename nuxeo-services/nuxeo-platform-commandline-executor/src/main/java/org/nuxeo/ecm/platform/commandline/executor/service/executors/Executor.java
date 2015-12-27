/*
 * (C) Copyright 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.commandline.executor.service.executors;

import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineDescriptor;
import org.nuxeo.ecm.platform.commandline.executor.service.EnvironmentDescriptor;

/**
 * Interface for class that provide a way to execute a {@link CommandLineDescriptor}.
 *
 * @author tiry
 */
public interface Executor {

    /**
     * No exception is thrown but the returned {@link ExecResult} contains everything about the command execution,
     * including an optional exception.
     *
     * @param cmdDesc Command to run. Cannot be null.
     * @param params Parameters passed to the command. Cannot be null.
     * @return Result of the execution
     * @deprecated Since 7.4. Prefer use of {@link #exec(CommandLineDescriptor, CmdParameters, EnvironmentDescriptor)}
     */
    @Deprecated
    ExecResult exec(CommandLineDescriptor cmdDesc, CmdParameters params);

    /**
     * No exception is thrown but the returned {@link ExecResult} contains everything about the command execution,
     * including an optional exception.
     *
     * @param cmdDesc Command to run. Cannot be null.
     * @param params Parameters passed to the command. Cannot be null.
     * @param env Environment context (variable and working directory)
     * @return Result of the execution
     * @since 7.4
     */
    ExecResult exec(CommandLineDescriptor cmdDesc, CmdParameters params, EnvironmentDescriptor env);

}
