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

import java.util.List;

/**
 * Interface for the service that manages commandline execution.
 *
 * @author tiry
 */
public interface CommandLineExecutorService {

    CommandAvailability getCommandAvailability(String commandName);

    ExecResult execCommand(String commandName, CmdParameters params) throws CommandNotAvailable;

    List<String> getRegistredCommands();

    List<String> getAvailableCommands();

    /**
     * @return a new {@link CmdParameters} pre-filled with commonly used parameters such as the tmp dir.
     * @since 7.4
     */
    CmdParameters getDefaultCmdParameters();

}
