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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.Environment;
import org.nuxeo.common.xmap.registry.MapRegistry;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.commandline.executor.service.cmdtesters.CommandTestResult;
import org.nuxeo.ecm.platform.commandline.executor.service.cmdtesters.CommandTester;
import org.nuxeo.ecm.platform.commandline.executor.service.executors.Executor;
import org.nuxeo.ecm.platform.commandline.executor.service.executors.ShellExecutor;
import org.nuxeo.runtime.RuntimeMessage.Level;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * POJO implementation of the {@link CommandLineExecutorService} interface. Also handles the Extension Point logic.
 *
 * @author tiry
 */
public class CommandLineExecutorComponent extends DefaultComponent implements CommandLineExecutorService {

    private static final Logger log = LogManager.getLogger(CommandLineExecutorComponent.class);

    public static final String EP_ENV = "environment";

    public static final String EP_CMD = "command";

    public static final String EP_CMDTESTER = "commandTester";

    public static final String DEFAULT_TESTER = "DefaultCommandTester";

    public static final String DEFAULT_EXECUTOR = "ShellExecutor";

    protected static final Executor DEFAULT_EXECUTOR_INSTANCE = new ShellExecutor();

    protected EnvironmentDescriptor env = new EnvironmentDescriptor();

    protected Map<String, EnvironmentDescriptor> envDescriptors = new HashMap<>();

    protected Map<String, CommandAvailability> unavailableCommands;

    @Override
    public void activate(ComponentContext context) {
        env = new EnvironmentDescriptor();
    }

    @Override
    public void deactivate(ComponentContext context) {
        env = null;
    }

    @Override
    public void start(ComponentContext context) {
        // compute testers before handling command registrations
        Map<String, CommandTester> testers = new HashMap<>();
        this.<CommandTesterDescriptor> getRegistryContributions(EP_CMDTESTER).forEach(desc -> {
            try {
                var tester = desc.getTesterClass().getDeclaredConstructor().newInstance();
                testers.put(desc.getName(), tester);
            } catch (ReflectiveOperationException e) {
                log.error(e, e);
                addRuntimeMessage(Level.ERROR, e.getMessage());
            }
        });

        unavailableCommands = new HashMap<>();
        this.<CommandLineDescriptor> getRegistryContributions(EP_CMD).forEach(desc -> {
            String name = desc.getName();

            String testerName = desc.getTester();
            if (testerName == null) {
                testerName = DEFAULT_TESTER;
                log.debug("Using default tester for command: {}", name);
            }

            // check tester exists for executor
            CommandTester tester = testers.get(testerName);
            if (tester == null) {
                String error = String.format("Unable to find tester '%s', command will not be available: '%s'",
                        testerName, name);
                addRuntimeMessage(Level.WARNING, error);
                log.error(error);
                unavailableCommands.put(name, new CommandAvailability(desc.getInstallationDirective(), error));
            } else {
                log.debug("Using tester '{}' for command: {}", testerName, name);
                CommandTestResult testResult = tester.test(desc);
                if (testResult.succeed()) {
                    log.info("Registered command: {}", name);
                } else {
                    String error = testResult.getErrorMessage();
                    String warn = String.format("Command not available: %s (%s. %s)", name, error,
                            desc.getInstallationDirective());
                    log.warn(warn);
                    unavailableCommands.put(name, new CommandAvailability(desc.getInstallationDirective(), error));
                }
            }
        });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        unavailableCommands = null;
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
        }
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
        var cmdDesc = this.<CommandLineDescriptor> getRegistryContribution(EP_CMD, commandName)
                          // should not happen
                          .orElseThrow(() -> new RuntimeException("Command " + commandName + " is not available"));
        EnvironmentDescriptor environment = new EnvironmentDescriptor().merge(
                env).merge(envDescriptors.getOrDefault(commandName, envDescriptors.get(cmdDesc.getCommand())));
        return DEFAULT_EXECUTOR_INSTANCE.exec(cmdDesc, params, environment);
    }

    @Override
    public CommandAvailability getCommandAvailability(String commandName) {
        CommandAvailability avail = unavailableCommands.get(commandName);
        if (avail != null) {
            return avail;
        }
        return this.<CommandLineDescriptor> getRegistryContribution(EP_CMD, commandName)
                   .map(desc -> new CommandAvailability())
                   .orElse(new CommandAvailability(commandName + " is not a registered command"));
    }

    @Override
    public List<String> getRegistredCommands() {
        return new ArrayList<>(this.<MapRegistry> getExtensionPointRegistry(EP_CMD).getContributions().keySet());
    }

    @Override
    public List<String> getAvailableCommands() {
        return getRegistredCommands().stream()
                                     .filter(Predicate.not(unavailableCommands::containsKey))
                                     .collect(Collectors.toList());
    }

    @Override
    public CommandLineDescriptor getCommandLineDescriptor(String commandName) {
        return this.<CommandLineDescriptor> getRegistryContribution(EP_CMD, commandName).orElse(null);
    }

    // ******************************************
    // for testing

    /** @deprecated since 11.4, use instance method {@link #getCommandLineDescriptor} instead */
    @Deprecated(since = "11.4")
    public static CommandLineDescriptor getCommandDescriptor(String commandName) {
        return Framework.getService(CommandLineExecutorService.class).getCommandLineDescriptor(commandName);
    }

    @Override
    public CmdParameters getDefaultCmdParameters() {
        CmdParameters params = new CmdParameters();
        params.addNamedParameter("java.io.tmpdir", System.getProperty("java.io.tmpdir"));
        params.addNamedParameter(Environment.NUXEO_TMP_DIR, Environment.getDefault().getTemp().getPath());
        return params;
    }

}
