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

package org.nuxeo.ecm.platform.commandline.executor.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.commandline.executor.service.cmdtesters.CommandTestResult;
import org.nuxeo.ecm.platform.commandline.executor.service.cmdtesters.CommandTester;
import org.nuxeo.ecm.platform.commandline.executor.service.executors.Executor;
import org.nuxeo.ecm.platform.commandline.executor.service.executors.ShellExecutor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * POJO implementation of the {@link CommandLineExecutorService} interface. Also
 * handles the Extension Point logic.
 *
 * @author tiry
 */
public class CommandLineExecutorComponent extends DefaultComponent implements
        CommandLineExecutorService {

    public static final String EP_ENV = "environment";
    public static final String EP_CMD = "command";
    public static final String EP_CMDTESTER = "commandTester";

    public static final String DEFAULT_TESTER = "SystemPathTester";
    public static final String DEFAULT_EXECUTOR = "ShellExecutor";

    protected static Map<String, CommandLineDescriptor> commandDescriptors = new HashMap<String, CommandLineDescriptor>();

    protected static EnvironementDescriptor env = new EnvironementDescriptor();

    protected static Map<String, CommandTester> testers = new HashMap<String, CommandTester>();

    protected static Map<String, Executor> executors = new HashMap<String, Executor>();

    private static final Log log = LogFactory.getLog(CommandLineExecutorComponent.class);

    @Override
    public void activate(ComponentContext context) throws Exception {
        commandDescriptors = new HashMap<String, CommandLineDescriptor>();
        env = new EnvironementDescriptor();
        testers = new HashMap<String, CommandTester>();
        executors = new HashMap<String, Executor>();
        executors.put(DEFAULT_EXECUTOR, new ShellExecutor());
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        commandDescriptors = null;
        env = null;
        testers = null;
        executors = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        if (EP_ENV.equals(extensionPoint)) {
            env.merge((EnvironementDescriptor) contribution);
        } else if (EP_CMD.equals(extensionPoint)) {
            CommandLineDescriptor desc = (CommandLineDescriptor) contribution;
            String name = desc.getName();

            log.debug("Registering command: " + name);

            if (!desc.isEnabled()) {
                commandDescriptors.remove(name);
                log.info("Command configured to not be enabled: " + name);
                return;
            }

            String testerName = desc.getTester();
            if (testerName == null) {
                testerName = DEFAULT_TESTER;
                log.debug("Using default tester for command: " + name);
            }

            CommandTester tester = testers.get(testerName);
            boolean cmdAvailable = false;
            if (tester == null) {
                log.error("Unable to find tester '" + testerName +
                        "', command will not be avalaible: " + name);
            } else {
                log.debug("Using tester '" + testerName + "' for command: " +
                        name);
                CommandTestResult testResult = tester.test(desc);
                cmdAvailable = testResult.succeed();
                if (cmdAvailable) {
                    log.info("Registered command: " + name);
                } else {
                    log.warn("Command not available: " + name + " (" +
                            testResult.getErrorMessage() + ')');
                    desc.setInstallErrorMessage(testResult.getErrorMessage());
                }
            }
            desc.setAvalaible(cmdAvailable);
            commandDescriptors.put(name, desc);
        } else if (EP_CMDTESTER.equals(extensionPoint)) {
            CommandTesterDescriptor desc = (CommandTesterDescriptor) contribution;
            CommandTester tester = (CommandTester) desc.getTesterClass()
                    .newInstance();
            testers.put(desc.getName(), tester);
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
    }

    /*
     * Service interface
     */
    public ExecResult execCommand(String commandName, CmdParameters params)
            throws CommandNotAvailable {
        CommandAvailability availability = getCommandAvailability(commandName);
        if (!availability.isAvailable()) {
            throw new CommandNotAvailable(availability);
        }

        CommandLineDescriptor cmdDesc = commandDescriptors.get(commandName);

        Executor executor = executors.get(cmdDesc.getExecutor());

        return executor.exec(cmdDesc, params);
    }

    public CommandAvailability getCommandAvailability(String commandName) {
        if (!commandDescriptors.containsKey(commandName)) {
            return new CommandAvailability(commandName
                    + " is not a registred command");
        }

        CommandLineDescriptor desc = commandDescriptors.get(commandName);
        if (desc.isAvalaible()) {
            return new CommandAvailability();
        } else {
            return new CommandAvailability(desc.getInstallationDirective(),
                    desc.getInstallErrorMessage());
        }
    }

    public List<String> getRegistredCommands() {
        List<String> cmds = new ArrayList<String>();
        cmds.addAll(commandDescriptors.keySet());
        return cmds;
    }

    public List<String> getAvailableCommands() {
        List<String> cmds = new ArrayList<String>();

        for (String cmdName : commandDescriptors.keySet()) {
            CommandLineDescriptor cmd = commandDescriptors.get(cmdName);
            if (cmd.isAvalaible()) {
                cmds.add(cmdName);
            }
        }
        return cmds;
    }

    // ******************************************
    // for testing

    public static CommandLineDescriptor getCommandDescriptor(String commandName) {
        return commandDescriptors.get(commandName);
    }

}
