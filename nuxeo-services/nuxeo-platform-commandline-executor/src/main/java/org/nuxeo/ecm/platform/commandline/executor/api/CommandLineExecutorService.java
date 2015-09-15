/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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

import java.util.List;
import java.util.regex.Pattern;

/**
 * Interface for the service that manages commandline execution.
 *
 * @author tiry
 */
public interface CommandLineExecutorService {

    Pattern VALID_PARAMETER_PATTERN = Pattern.compile("[\\p{L}_0-9-.%:=/\\\\ ]+");

    Pattern VALID_PARAMETER_PATTERN_WIN = Pattern.compile("[\\p{L}_0-9-.%:=/\\\\ ()]+");

    CommandAvailability getCommandAvailability(String commandName);

    ExecResult execCommand(String commandName, CmdParameters params) throws CommandNotAvailable;

    List<String> getRegistredCommands();

    List<String> getAvailableCommands();

    /**
     * Returns true if the given {@code parameter} is valid to be used in a command.
     *
     * @since 5.7
     */
    boolean isValidParameter(String parameter);

    /**
     * Checks if the given {@code parameter} is valid to be used in a command.
     * <p>
     * If not, throws an {@code IllegalArgumentException}.
     *
     * @since 5.7
     */
    void checkParameter(String parameter);

    /**
     * @return a new {@link CmdParameters} pre-filled with commonly used parameters such as the tmp dir.
     * @since 7.4
     */
    CmdParameters getDefaultCmdParameters();

}
