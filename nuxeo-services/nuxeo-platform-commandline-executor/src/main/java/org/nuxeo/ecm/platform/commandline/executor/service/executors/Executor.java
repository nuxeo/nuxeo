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

package org.nuxeo.ecm.platform.commandline.executor.service.executors;

import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineDescriptor;

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
     */
    ExecResult exec(CommandLineDescriptor cmdDesc, CmdParameters params);

    /**
     * Same method {@link Executor#exec(org.nuxeo.ecm.platform.commandline
     * .executor.service.CommandLineDescriptor, org.nuxeo.ecm.platform
     * .commandline.executor.api.CmdParameters)} but with the parameter
     * quoteParameter.
     *
     * @param cmdDesc        Command to run. Cannot be null.
     * @param params         Parameters passed to the command. Cannot be null.
     * @param quoteParameter Define if parameters should be surrounded by
     *                       quotes or not.
     * @return Result of the execution
     * @since 7.1
     */
    ExecResult exec(CommandLineDescriptor cmdDesc, CmdParameters params, boolean
            quoteParameter);
}
