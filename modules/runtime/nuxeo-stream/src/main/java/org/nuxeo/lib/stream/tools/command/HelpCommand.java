/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.lib.stream.tools.command;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.tools.CommandRegistry;

/**
 * @since 9.3
 */
public class HelpCommand extends Command {

    protected static final String NAME = "help";

    protected Options options;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void updateOptions(Options options) {
        this.options = options;
    }

    @Override
    public boolean run(LogManager manager, CommandLine cmd) {
        List<Command> commands = new CommandRegistry().commands();
        displayCommonOptions();
        if (cmd.getArgList().isEmpty()) {
            commands.stream().filter(command -> !(command instanceof HelpCommand)).forEach(this::displayHelp);
        } else {
            String name = cmd.getArgList().get(0);
            commands.stream().filter(command -> name.equals(command.name())).forEach(this::displayHelp);
        }
        return true;
    }

    protected void displayCommonOptions() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setSyntaxPrefix("");
        formatter.printHelp("Usage: stream.sh [COMMAND] [Options]\nCommon options:", options);
    }

    protected void displayHelp(Command command) {
        Options cmdOptions = new Options();
        command.updateOptions(cmdOptions);
        HelpFormatter formatter = new HelpFormatter();
        formatter.setSyntaxPrefix("Command: ");
        formatter.printHelp(command.name(), cmdOptions);
    }
}
