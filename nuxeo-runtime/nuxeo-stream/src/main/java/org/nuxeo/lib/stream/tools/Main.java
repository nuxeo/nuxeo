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
package org.nuxeo.lib.stream.tools;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.nuxeo.lib.stream.log.LogManager;
import org.nuxeo.lib.stream.log.chronicle.ChronicleLogManager;
import org.nuxeo.lib.stream.tools.command.Command;

/**
 * @since 9.3
 */
public class Main {

    protected final Map<String, Command> commandMap = new HashMap<>();

    protected final Options options = new Options();

    protected String command;

    protected LogManager manager;

    public static void main(final String[] args) throws InterruptedException {
        new Main().run(args);
    }

    public void run(String[] args) {
        Path path = Paths.get(args[0]);
        this.manager = new ChronicleLogManager(path);
        this.command = args[1];
        runWithArgs(Arrays.copyOfRange(args, 2, args.length));
    }

    protected void runWithArgs(String[] args) {
        Command cmd = getCommand();
        cmd.updateOptions(options);
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmdLine = parser.parse(options, args);
            cmd.run(manager, cmdLine);
        } catch (ParseException e) {
            helpAndExit("Parse error: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            helpAndExit("Interrupted: " + e.getMessage());
        }
    }

    protected Command getCommand() {
        if (commandMap.isEmpty()) {
            new CommandRegistry().commands().forEach(cmd -> commandMap.put(cmd.name(), cmd));
        }
        if (!commandMap.containsKey(command)) {
            helpAndExit("Unknown command: " + command);
        }
        return commandMap.get(command);
    }

    protected void helpAndExit(String message) {
        System.err.println(message);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(command != null ? command : "tools", options);
        System.exit(-1);
    }

}
