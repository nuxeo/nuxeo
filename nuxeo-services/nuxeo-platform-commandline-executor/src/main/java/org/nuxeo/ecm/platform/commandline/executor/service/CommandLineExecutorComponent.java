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

package org.nuxeo.ecm.platform.commandline.executor.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.nuxeo.common.Environment;
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
 * POJO implementation of the {@link CommandLineExecutorService} interface. Also handles the Extension Point logic.
 *
 * @author tiry
 */
public class CommandLineExecutorComponent extends DefaultComponent implements CommandLineExecutorService {

    public static final String EP_ENV = "environment";

    public static final String EP_CMD = "command";

    public static final String EP_CMDTESTER = "commandTester";

    public static final String DEFAULT_TESTER = "DefaultCommandTester";

    public static final String DEFAULT_EXECUTOR = "ShellExecutor";

    protected static Map<String, CommandLineDescriptor> commandDescriptors = new HashMap<>();

    protected static EnvironmentDescriptor env = new EnvironmentDescriptor();

    protected static Map<String, EnvironmentDescriptor> envDescriptors = new HashMap<>();

    protected static Map<String, CommandTester> testers = new HashMap<>();

    protected static Map<String, Executor> executors = new HashMap<>();

    private static final Log log = LogFactory.getLog(CommandLineExecutorComponent.class);

    @Override
    public void activate(ComponentContext context) {
        commandDescriptors = new HashMap<>();
        env = new EnvironmentDescriptor();
        testers = new HashMap<>();
        executors = new HashMap<>();
        executors.put(DEFAULT_EXECUTOR, new ShellExecutor());
    }

    @Override
    public void deactivate(ComponentContext context) {
        commandDescriptors = null;
        env = null;
        testers = null;
        executors = null;
    }

    @Override
    public void registerContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
        if (EP_ENV.equals(extensionPoint)) {
            EnvironmentDescriptor desc = (EnvironmentDescriptor) contribution;
            String name = desc.getName();
            if (name == null) {
                env.merge(desc);
            } else {
                for (String envName : name.split(",")) {
                    if (envDescriptors.containsKey(envName)) {
                        envDescriptors.get(envName).merge(desc);
                    } else {
                        envDescriptors.put(envName, desc);
                    }
                }
            }
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
                log.error("Unable to find tester '" + testerName + "', command will not be available: " + name);
            } else {
                log.debug("Using tester '" + testerName + "' for command: " + name);
                CommandTestResult testResult = tester.test(desc);
                cmdAvailable = testResult.succeed();
                if (cmdAvailable) {
                    log.info("Registered command: " + name);
                } else {
                    desc.setInstallErrorMessage(testResult.getErrorMessage());
                    log.warn("Command not available: " + name + " (" + desc.getInstallErrorMessage() + ". "
                            + desc.getInstallationDirective() + ')');
                }
            }
            desc.setAvailable(cmdAvailable);
            commandDescriptors.put(name, desc);
        } else if (EP_CMDTESTER.equals(extensionPoint)) {
            CommandTesterDescriptor desc = (CommandTesterDescriptor) contribution;
            CommandTester tester;
            try {
                tester = (CommandTester) desc.getTesterClass().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
            testers.put(desc.getName(), tester);
        }
    }

    @Override
    public void unregisterContribution(Object contribution, String extensionPoint, ComponentInstance contributor) {
    }

    /*
     * Service interface
     */
    @Override
    public ExecResult execCommand(String commandName, CmdParameters params) throws CommandNotAvailable {
        CommandAvailability availability = getCommandAvailability(commandName);
        if (!availability.isAvailable()) {
            throw new CommandNotAvailable(availability);
        }

        CommandLineDescriptor cmdDesc = commandDescriptors.get(commandName);
        Executor executor = executors.get(cmdDesc.getExecutor());
        EnvironmentDescriptor environment = new EnvironmentDescriptor().merge(env).merge(
                envDescriptors.getOrDefault(commandName, envDescriptors.get(cmdDesc.getCommand())));
        return executor.exec(cmdDesc, params, environment);
    }

    @Override
    public CommandAvailability getCommandAvailability(String commandName) {
        if (!commandDescriptors.containsKey(commandName)) {
            return new CommandAvailability(commandName + " is not a registered command");
        }

        CommandLineDescriptor desc = commandDescriptors.get(commandName);
        if (desc.isAvailable()) {
            return new CommandAvailability();
        } else {
            return new CommandAvailability(desc.getInstallationDirective(), desc.getInstallErrorMessage());
        }
    }

    @Override
    public List<String> getRegistredCommands() {
        List<String> cmds = new ArrayList<>();
        cmds.addAll(commandDescriptors.keySet());
        return cmds;
    }

    @Override
    public List<String> getAvailableCommands() {
        List<String> cmds = new ArrayList<>();

        for (String cmdName : commandDescriptors.keySet()) {
            CommandLineDescriptor cmd = commandDescriptors.get(cmdName);
            if (cmd.isAvailable()) {
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

    @Override
    public CmdParameters getDefaultCmdParameters() {
        CmdParameters params = new CmdParameters();
        params.addNamedParameter("java.io.tmpdir", System.getProperty("java.io.tmpdir"));
        params.addNamedParameter(Environment.NUXEO_TMP_DIR, Environment.getDefault().getTemp().getPath());
        return params;
    }

}
